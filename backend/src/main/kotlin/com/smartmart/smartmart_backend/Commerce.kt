package com.smartmart.smartmart_backend

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * Mọi thao tác ghi dùng session thật và kiểm tra sở hữu. Quantity INT theo schema nhóm.
 */
data class AddressRequest(
    @field:NotBlank @field:Size(max=120) val recipientName: String,
    @field:NotBlank @field:Size(max=20) @field:Pattern(regexp="[+]?[0-9][0-9 .()-]{6,18}[0-9]") val phone: String,
    @field:NotBlank @field:Size(max=250) val addressLine: String,
    @field:Size(max=100) val ward: String? = null,
    @field:NotBlank @field:Size(max=100) val district: String,
    @field:NotBlank @field:Size(max=100) val province: String,
    val isDefault: Boolean = false
)
data class AddCartRequest(@field:Positive val productId: Long, @field:Min(1) @field:Max(999) val quantity: Int)
data class QuantityRequest(@field:Min(1) @field:Max(999) val quantity: Int)
data class CartItem(val productId: Long, val name: String, val price: BigDecimal, val unit: String?, val imageUrl: String?,
    val quantity: Int, val lineTotal: BigDecimal, val stockStatus: String, val quantityOnHand: Int?)
data class CartResponse(val items: List<CartItem>, val total: BigDecimal, val itemCount: Int)
data class CheckoutRequest(@field:Positive val addressId: Long, val paymentMethod: String = "COD",
    @field:Size(max=40) val voucherCode: String? = null, @field:Size(max=500) val customerNote: String? = null)
data class CheckoutQuote(val subtotal: BigDecimal,val discountAmount: BigDecimal,val shippingFee: BigDecimal,val totalAmount: BigDecimal)
data class ReviewRequest(@field:Positive val orderId: Long, @field:Min(1) @field:Max(5) val rating: Int,
    @field:Size(max=1000) val comment: String? = null)

/** A configured provider must verify authenticated webhook data before it can settle a payment. */
interface OnlinePaymentProvider {
    fun requireConfigured(method: String)
    fun createSession(orderId: Long, method: String): Map<String, Any?>
    fun verifyWebhook(payload: ByteArray, signature: String?): VerifiedPaymentEvent
}
data class VerifiedPaymentEvent(val eventId: String, val transactionRef: String, val amount: BigDecimal, val succeeded: Boolean)
@Service
class UnconfiguredPaymentProvider : OnlinePaymentProvider {
    override fun requireConfigured(method: String) {
        throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_PROVIDER_UNCONFIGURED", "Thanh toán ví điện tử và thẻ chưa được cấu hình. Vui lòng chọn COD")
    }
    override fun createSession(orderId: Long, method: String): Map<String, Any?> { requireConfigured(method); return emptyMap() }
    override fun verifyWebhook(payload: ByteArray, signature: String?): VerifiedPaymentEvent {
        requireConfigured("WEBHOOK"); error("Unreachable")
    }
}

@Service
class CommerceService(private val jdbc: JdbcTemplate, private val catalog: CatalogService, private val onlinePayment: OnlinePaymentProvider,
    private val checkoutTransactions: CheckoutTransactions,
    @Value("\${smartmart.shipping-fee:0}") private val shippingFee: BigDecimal) {
    private val addressColumns = "AddressID AS id,RecipientName AS recipientName,Phone AS phone,AddressLine AS addressLine,Ward AS ward,District AS district,Province AS province,IsDefault AS isDefault"
    fun addresses(userId: Long) = jdbc.queryForList("SELECT $addressColumns FROM dbo.Addresses WHERE UserID=? ORDER BY IsDefault DESC,AddressID", userId)
    fun address(userId: Long, id: Long) = jdbc.queryForList("SELECT $addressColumns FROM dbo.Addresses WHERE UserID=? AND AddressID=?", userId, id).firstOrNull() ?: notFound("Không tìm thấy địa chỉ")
    private fun lockUser(userId: Long) {
        jdbc.queryForObject("SELECT UserID FROM dbo.Users WITH(UPDLOCK,HOLDLOCK) WHERE UserID=?", Long::class.java, userId) ?: notFound()
    }
    @Transactional
    fun saveAddress(userId: Long, id: Long?, body: AddressRequest): Map<String, Any?> {
        lockUser(userId)
        if (id != null) address(userId, id)
        if (body.isDefault) jdbc.update("UPDATE dbo.Addresses SET IsDefault=0 WHERE UserID=?", userId)
        val savedId = if (id == null) jdbc.queryForObject("INSERT dbo.Addresses(UserID,RecipientName,Phone,AddressLine,Ward,District,Province,IsDefault) OUTPUT INSERTED.AddressID VALUES(?,?,?,?,?,?,?,?)",
            Long::class.java,userId,body.recipientName.trim(),body.phone.trim(),body.addressLine.trim(),body.ward?.trim(),body.district.trim(),body.province.trim(),body.isDefault)!!
        else { jdbc.update("UPDATE dbo.Addresses SET RecipientName=?,Phone=?,AddressLine=?,Ward=?,District=?,Province=?,IsDefault=? WHERE UserID=? AND AddressID=?",
            body.recipientName.trim(),body.phone.trim(),body.addressLine.trim(),body.ward?.trim(),body.district.trim(),body.province.trim(),body.isDefault,userId,id); id }
        return address(userId, savedId)
    }
    @Transactional
    fun deleteAddress(userId: Long, id: Long) {
        lockUser(userId); address(userId,id)
        if (jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Orders WHERE AddressID=?",Int::class.java,id)!! > 0)
            conflict("ADDRESS_IN_USE", "Địa chỉ đã được sử dụng trong đơn hàng và cần được giữ để đối soát")
        jdbc.update("DELETE dbo.Addresses WHERE UserID=? AND AddressID=?",userId,id)
    }

    fun cart(userId: Long): CartResponse {
        val items = jdbc.query("""SELECT ci.ProductID AS productId,p.ProductName AS name,p.Price AS price,p.Unit AS unit,
            ci.Quantity AS quantity,inventory.QuantityOnHand AS quantityOnHand,p.IsActive AS isActive,image.ImageUrl AS imageUrl
            FROM dbo.CartItems ci JOIN dbo.Carts c ON c.CartID=ci.CartID JOIN dbo.Products p ON p.ProductID=ci.ProductID
            LEFT JOIN dbo.Inventory inventory ON inventory.ProductID=p.ProductID
            OUTER APPLY(SELECT TOP(1) ImageUrl FROM dbo.ProductImages pi WHERE pi.ProductID=p.ProductID ORDER BY IsPrimary DESC,DisplayOrder,ProductImageID) image
            WHERE c.UserID=? ORDER BY ci.CartItemID""", { row, _ ->
                val quantity = row.getInt("quantity"); val price = row.getBigDecimal("price")
                val stock = (row.getObject("quantityOnHand") as Number?)?.toInt()
                CartItem(row.getLong("productId"),row.getString("name"),price,row.getString("unit"),row.getString("imageUrl"),quantity,price.multiply(quantity.toBigDecimal()),
                    if (!row.getBoolean("isActive") || stock==null) "UNAVAILABLE" else if (stock>0) "IN_STOCK" else "OUT_OF_STOCK",stock)
            },userId)
        return CartResponse(items,items.fold(BigDecimal.ZERO) { sum,it -> sum+it.lineTotal },items.sumOf { it.quantity })
    }

    @Transactional
    fun setCart(userId: Long, productId: Long, quantity: Int, add: Boolean): CartResponse {
        if (quantity !in 1..999) badRequest("Số lượng phải từ 1 đến 999")
        lockUser(userId)
        var cartId = jdbc.queryForList("SELECT CartID FROM dbo.Carts WHERE UserID=?",Long::class.java,userId).firstOrNull()
        if (cartId==null) cartId = jdbc.queryForObject("INSERT dbo.Carts(UserID) OUTPUT INSERTED.CartID VALUES(?)",Long::class.java,userId)!!
        val old = jdbc.queryForList("SELECT Quantity FROM dbo.CartItems WHERE CartID=? AND ProductID=?",Int::class.java,cartId,productId).firstOrNull()
        if (!add && old==null) notFound("Sản phẩm chưa có trong giỏ hàng")
        val desired = quantity + if (add) old ?: 0 else 0
        if (desired>999) badRequest("Tối đa 999 đơn vị mỗi sản phẩm")
        val product = jdbc.queryForList("SELECT p.IsActive AS active,i.QuantityOnHand AS stock FROM dbo.Products p WITH(UPDLOCK,HOLDLOCK) LEFT JOIN dbo.Inventory i WITH(UPDLOCK,HOLDLOCK) ON i.ProductID=p.ProductID WHERE p.ProductID=?",productId).firstOrNull() ?: notFound("Không tìm thấy sản phẩm")
        val stock = (product["stock"] as Number?)?.toInt()
        if (product["active"] != true || stock==null) conflict("PRODUCT_UNAVAILABLE","Sản phẩm hiện không thể đặt mua")
        if (desired>stock) conflict("INSUFFICIENT_STOCK","Số lượng vượt quá tồn kho hiện tại")
        if (old==null) jdbc.update("INSERT dbo.CartItems(CartID,ProductID,Quantity) VALUES(?,?,?)",cartId,productId,desired)
        else jdbc.update("UPDATE dbo.CartItems SET Quantity=? WHERE CartID=? AND ProductID=?",desired,cartId,productId)
        jdbc.update("UPDATE dbo.Carts SET UpdatedAt=SYSDATETIME() WHERE CartID=?",cartId)
        return cart(userId)
    }

    @Transactional
    fun removeCart(userId: Long, productId: Long): CartResponse {
        lockUser(userId)
        jdbc.update("DELETE ci FROM dbo.CartItems ci JOIN dbo.Carts c ON c.CartID=ci.CartID WHERE c.UserID=? AND ci.ProductID=?",userId,productId)
        return cart(userId)
    }

    // The retry boundary owns each complete transaction, including commit/rollback.
    // Business errors are not retried; the same key protects retries from duplicate orders.
    fun checkout(userId: Long, key: String, body: CheckoutRequest): Map<String, Any?> =
        checkoutTransactions.execute { checkoutOnce(userId,key,body) }

    private fun checkoutOnce(userId: Long, key: String, body: CheckoutRequest): Map<String, Any?> {
        if (!key.matches(Regex("[A-Za-z0-9-]{16,100}"))) badRequest("Idempotency-Key phải gồm 16 đến 100 chữ, số hoặc dấu gạch nối")
        if (body.paymentMethod !in listOf("COD","EWALLET","CARD")) badRequest("Phương thức thanh toán không hợp lệ")
        val fingerprint = Passwords.digest(listOf(body.addressId.toString(),body.paymentMethod,body.voucherCode?.trim()?.uppercase().orEmpty(),body.customerNote.orEmpty()).joinToString("|") { "${it.length}:$it" })
        lockUser(userId)
        val existing = jdbc.queryForList("SELECT OrderID AS id,CheckoutFingerprint AS fingerprint FROM dbo.Orders WHERE CustomerID=? AND IdempotencyKey=?",userId,key).firstOrNull()
        if (existing!=null) {
            if (existing["fingerprint"] != fingerprint) conflict("IDEMPOTENCY_CONFLICT","Mã yêu cầu đã được dùng cho nội dung đặt hàng khác")
            return order(userId,(existing["id"] as Number).toLong())
        }
        // Fail before any cart/stock/order mutation when provider credentials are absent.
        if (body.paymentMethod!="COD") onlinePayment.requireConfigured(body.paymentMethod)
        val address = address(userId,body.addressId)
        val items = jdbc.queryForList("""SELECT ci.ProductID AS productId,ci.Quantity AS quantity,p.Price AS price,
            p.ProductName AS name,p.Unit AS unit,p.IsActive AS active,i.QuantityOnHand AS stock,i.InventoryID AS inventoryId
            FROM dbo.CartItems ci WITH(UPDLOCK,HOLDLOCK) JOIN dbo.Carts c ON c.CartID=ci.CartID
            JOIN dbo.Products p WITH(UPDLOCK,HOLDLOCK) ON p.ProductID=ci.ProductID
            LEFT JOIN dbo.Inventory i WITH(UPDLOCK,HOLDLOCK) ON i.ProductID=p.ProductID
            WHERE c.UserID=? ORDER BY ci.ProductID""",userId)
        if (items.isEmpty()) conflict("EMPTY_CART","Giỏ hàng đang trống")
        var subtotal=BigDecimal.ZERO
        for (item in items) {
            val quantity=(item["quantity"] as Number).toInt(); val stock=(item["stock"] as Number?)?.toInt()
            if (item["active"]!=true || stock==null) conflict("PRODUCT_UNAVAILABLE","Có sản phẩm trong giỏ không thể đặt mua")
            if (quantity !in 1..999 || quantity>stock) conflict("INSUFFICIENT_STOCK","Tồn kho đã thay đổi. Vui lòng kiểm tra giỏ hàng")
            subtotal += (item["price"] as BigDecimal)*quantity.toBigDecimal()
        }
        val voucher = findVoucher(subtotal,body.voucherCode,true)
        val quote = totals(subtotal,voucher)
        val discount = quote.discountAmount
        val deliveryFee = quote.shippingFee
        val total = quote.totalAmount
        val code="SM-"+UUID.randomUUID().toString().replace("-","").take(24)
        val snapshot=listOf("recipientName","phone","addressLine","ward","district","province").mapNotNull { address[it] }.joinToString(", ").take(1000)
        val orderId=jdbc.queryForObject("""INSERT dbo.Orders(OrderCode,CustomerID,AddressID,VoucherID,Subtotal,DiscountAmount,ShippingFee,TotalAmount,CustomerNote,IdempotencyKey,CheckoutFingerprint,AddressSnapshot)
            OUTPUT INSERTED.OrderID VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""",Long::class.java,code,userId,body.addressId,voucher?.get("id"),subtotal,discount,deliveryFee,total,body.customerNote,key,fingerprint,snapshot)!!
        for (item in items) {
            val quantity=(item["quantity"] as Number).toInt()
            val changed=jdbc.update("UPDATE dbo.Inventory SET QuantityOnHand=QuantityOnHand-?,UpdatedAt=SYSDATETIME() WHERE ProductID=? AND QuantityOnHand>=?",quantity,item["productId"],quantity)
            if (changed!=1) conflict("INSUFFICIENT_STOCK","Tồn kho đã thay đổi. Vui lòng thử lại")
            jdbc.update("INSERT dbo.OrderItems(OrderID,ProductID,Quantity,UnitPrice,ProductNameSnapshot,UnitSnapshot) VALUES(?,?,?,?,?,?)",orderId,item["productId"],quantity,item["price"],item["name"],item["unit"])
            jdbc.update("INSERT dbo.InventoryTransactions(InventoryID,UserID,TransactionType,QuantityChange,Note) VALUES(?,?,'SALE',?,?)",item["inventoryId"],userId,-quantity,"Order $code")
        }
        jdbc.update("INSERT dbo.Payments(OrderID,PaymentMethod,Amount,PaymentStatus,TransactionRef) VALUES(?,'COD',?,'PENDING',?)",orderId,total,"COD-"+UUID.randomUUID())
        jdbc.update("INSERT dbo.Shipments(OrderID,TrackingCode) VALUES(?,?)",orderId,"SM-"+UUID.randomUUID().toString().replace("-", ""))
        jdbc.update("INSERT dbo.OrderStatusHistory(OrderID,ChangedByUserID,NewStatus,Note) VALUES(?,?,'PENDING',N'Khách hàng tạo đơn')",orderId,userId)
        if (voucher!=null) jdbc.update("INSERT dbo.VoucherRedemptions(VoucherID,CustomerID,OrderID) VALUES(?,?,?)",voucher["id"],userId,orderId)
        jdbc.update("DELETE ci FROM dbo.CartItems ci JOIN dbo.Carts c ON c.CartID=ci.CartID WHERE c.UserID=?",userId)
        return order(userId,orderId)
    }

    private fun findVoucher(subtotal: BigDecimal,code: String?,lock: Boolean): Map<String,Any?>? = code?.trim()?.takeIf(String::isNotBlank)?.let {
        val locking = if(lock) " WITH(UPDLOCK,HOLDLOCK)" else ""
        val row=jdbc.queryForList("SELECT VoucherID AS id,DiscountType AS type,DiscountValue AS value,MinOrderValue AS minimum,UsageLimit AS limit FROM dbo.Vouchers$locking WHERE Code=? AND IsActive=1 AND StartAt<=SYSDATETIME() AND EndAt>SYSDATETIME()",it.uppercase()).firstOrNull()
            ?: conflict("INVALID_VOUCHER","Mã giảm giá không tồn tại hoặc đã hết hạn")
        if(subtotal<(row["minimum"] as BigDecimal)) conflict("VOUCHER_MINIMUM","Đơn hàng chưa đạt giá trị tối thiểu của mã giảm giá")
        val limit=(row["limit"] as Number?)?.toInt()
        val used=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.VoucherRedemptions WHERE VoucherID=?",Int::class.java,row["id"])!!
        if(limit!=null && used>=limit) conflict("VOUCHER_EXHAUSTED","Mã giảm giá đã hết lượt sử dụng")
        row
    }
    private fun totals(subtotal: BigDecimal,voucher: Map<String,Any?>?): CheckoutQuote {
        val discount=(if(voucher==null) BigDecimal.ZERO else if(voucher["type"]=="PERCENT") subtotal*(voucher["value"] as BigDecimal)/BigDecimal(100) else voucher["value"] as BigDecimal).min(subtotal).setScale(2,RoundingMode.HALF_UP)
        val fee=shippingFee.max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP)
        val total=subtotal-discount+fee
        if(total>BigDecimal("9999999999.99") || subtotal>BigDecimal("9999999999.99")) badRequest("Tổng giá trị đơn hàng vượt giới hạn")
        return CheckoutQuote(subtotal,discount,fee,total)
    }
    fun quote(userId: Long,body: CheckoutRequest): CheckoutQuote {
        if(body.paymentMethod !in listOf("COD","EWALLET","CARD")) badRequest("Phương thức thanh toán không hợp lệ")
        if(body.paymentMethod!="COD") onlinePayment.requireConfigured(body.paymentMethod)
        address(userId,body.addressId)
        val cart=cart(userId)
        if(cart.items.isEmpty()) conflict("EMPTY_CART","Giỏ hàng đang trống")
        if(cart.items.any { it.stockStatus!="IN_STOCK" || it.quantity>it.quantityOnHand!! }) conflict("INSUFFICIENT_STOCK","Tồn kho đã thay đổi. Vui lòng kiểm tra giỏ hàng")
        return totals(cart.total,findVoucher(cart.total,body.voucherCode,false))
    }

    private val orderColumns="o.OrderID AS id,o.OrderCode AS orderCode,o.OrderStatus AS orderStatus,o.PaymentStatus AS paymentStatus,o.Subtotal AS subtotal,o.DiscountAmount AS discountAmount,o.ShippingFee AS shippingFee,o.TotalAmount AS totalAmount,o.CreatedAt AS createdAt,o.AddressID AS addressId,o.AddressSnapshot AS addressSnapshot,o.CustomerNote AS customerNote,(SELECT TOP(1) PaymentMethod FROM dbo.Payments p WHERE p.OrderID=o.OrderID ORDER BY p.PaymentID) AS paymentMethod"
    fun orders(userId: Long): List<Map<String,Any?>> = jdbc.queryForList("SELECT $orderColumns FROM dbo.Orders o WHERE o.CustomerID=? ORDER BY o.OrderID DESC",userId)
    fun order(userId: Long,id: Long,admin: Boolean=false): Map<String,Any?> {
        val row=jdbc.queryForList("SELECT $orderColumns FROM dbo.Orders o WHERE o.OrderID=?"+if(admin) "" else " AND o.CustomerID=?",*if(admin) arrayOf<Any>(id) else arrayOf<Any>(id,userId)).firstOrNull() ?: notFound("Không tìm thấy đơn hàng")
        val items=jdbc.queryForList("SELECT oi.ProductID AS productId,COALESCE(oi.ProductNameSnapshot,p.ProductName) AS name,COALESCE(oi.UnitSnapshot,p.Unit) AS unit,oi.Quantity AS quantity,oi.UnitPrice AS price,oi.LineTotal AS lineTotal FROM dbo.OrderItems oi JOIN dbo.Products p ON p.ProductID=oi.ProductID WHERE oi.OrderID=? ORDER BY oi.OrderItemID",id)
        val history=jdbc.queryForList("SELECT NewStatus AS status,Note AS note,ChangedAt AS changedAt FROM dbo.OrderStatusHistory WHERE OrderID=? ORDER BY OrderStatusHistoryID",id)
        return row+mapOf("items" to items,"history" to history)
    }

    @Transactional
    fun cancel(userId: Long,id: Long) {
        lockUser(userId)
        val row=jdbc.queryForList("SELECT OrderStatus AS status FROM dbo.Orders WITH(UPDLOCK,HOLDLOCK) WHERE OrderID=? AND CustomerID=?",id,userId).firstOrNull() ?: notFound("Không tìm thấy đơn hàng")
        if (row["status"]=="CANCELLED") return
        if (row["status"]!="PENDING") conflict("ORDER_STATE","Chỉ có thể hủy đơn đang chờ xác nhận")
        restoreInventory(id,userId)
        jdbc.update("UPDATE dbo.Orders SET OrderStatus='CANCELLED' WHERE OrderID=?",id)
        jdbc.update("UPDATE dbo.Payments SET PaymentStatus='FAILED' WHERE OrderID=? AND PaymentStatus='PENDING'",id)
        jdbc.update("DELETE dbo.VoucherRedemptions WHERE OrderID=?",id)
        jdbc.update("INSERT dbo.OrderStatusHistory(OrderID,ChangedByUserID,OldStatus,NewStatus,Note) VALUES(?,?,'PENDING','CANCELLED',N'Khách hàng hủy đơn')",id,userId)
    }
    fun restoreInventory(orderId: Long, actor: Long) {
        val items=jdbc.queryForList("SELECT ProductID AS id,Quantity AS quantity FROM dbo.OrderItems WHERE OrderID=? ORDER BY ProductID",orderId)
        for(item in items) {
            jdbc.update("UPDATE dbo.Inventory SET QuantityOnHand=QuantityOnHand+?,UpdatedAt=SYSDATETIME() WHERE ProductID=?",item["quantity"],item["id"])
            jdbc.update("INSERT dbo.InventoryTransactions(InventoryID,UserID,TransactionType,QuantityChange,Note) SELECT InventoryID,?,'RETURN',?,? FROM dbo.Inventory WHERE ProductID=?",actor,item["quantity"],"Cancel order $orderId",item["id"])
        }
    }

    fun reviews(productId: Long,rating: Int?,page: Int,size: Int): Map<String,Any?> {
        catalog.summary(productId)
        if(page !in 0..100_000 || size !in 1..100 || rating!=null && rating !in 1..5) badRequest("Điều kiện đánh giá không hợp lệ")
        val filter=if(rating==null) "" else " AND r.Rating=?"
        val args=if(rating==null) arrayOf<Any>(productId) else arrayOf<Any>(productId,rating)
        val count=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.Reviews r WHERE r.ProductID=?$filter",Long::class.java,*args)!!
        val summary=catalog.summary(productId)
        val items=jdbc.queryForList("SELECT r.ReviewID AS id,u.FullName AS customerName,r.Rating AS rating,r.Comment AS comment,r.CreatedAt AS createdAt FROM dbo.Reviews r JOIN dbo.Users u ON u.UserID=r.CustomerID WHERE r.ProductID=?$filter ORDER BY r.ReviewID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",*(args.toList()+listOf(page*size,size)).toTypedArray())
        return mapOf("items" to items,"averageRating" to summary.averageRating,"reviewCount" to summary.reviewCount,"page" to page,"totalPages" to ((count+size-1)/size),"totalItems" to count)
    }
    @Transactional
    fun review(userId: Long,productId: Long,body: ReviewRequest): Map<String,Any?> {
        lockUser(userId); catalog.summary(productId)
        val eligible=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Orders o JOIN dbo.OrderItems oi ON oi.OrderID=o.OrderID WHERE o.OrderID=? AND o.CustomerID=? AND o.OrderStatus='DELIVERED' AND oi.ProductID=?",Int::class.java,body.orderId,userId,productId)!!
        if(eligible==0) forbidden()
        if(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Reviews WITH(UPDLOCK,HOLDLOCK) WHERE ProductID=? AND CustomerID=? AND OrderID=?",Int::class.java,productId,userId,body.orderId)!!>0) conflict("REVIEW_EXISTS","Bạn đã đánh giá sản phẩm trong đơn hàng này")
        val id=jdbc.queryForObject("INSERT dbo.Reviews(ProductID,CustomerID,OrderID,Rating,Comment) OUTPUT INSERTED.ReviewID VALUES(?,?,?,?,?)",Long::class.java,productId,userId,body.orderId,body.rating,body.comment?.trim())!!
        return jdbc.queryForList("SELECT r.ReviewID AS id,u.FullName AS customerName,r.Rating AS rating,r.Comment AS comment,r.CreatedAt AS createdAt FROM dbo.Reviews r JOIN dbo.Users u ON u.UserID=r.CustomerID WHERE r.ReviewID=?",id).first()
    }
    fun favorites(userId: Long) = jdbc.queryForList("SELECT ProductID FROM dbo.Favorites WHERE CustomerID=? ORDER BY FavoriteID DESC",Long::class.java,userId).map { catalog.summary(it!!) }
    @Transactional
    fun favorite(userId: Long,productId: Long,add: Boolean) {
        lockUser(userId); catalog.summary(productId)
        if(add) jdbc.update("INSERT dbo.Favorites(CustomerID,ProductID) SELECT ?,? WHERE NOT EXISTS(SELECT 1 FROM dbo.Favorites WHERE CustomerID=? AND ProductID=?)",userId,productId,userId,productId)
        else jdbc.update("DELETE dbo.Favorites WHERE CustomerID=? AND ProductID=?",userId,productId)
    }
}

@RestController
class CommerceController(private val auth: AuthService,private val service: CommerceService,private val online: OnlinePaymentProvider) {
    private fun user(request: HttpServletRequest)=auth.roles(request,"CUSTOMER").id
    @GetMapping("/api/addresses") fun addresses(request: HttpServletRequest)=service.addresses(user(request))
    @PostMapping("/api/addresses") @ResponseStatus(HttpStatus.CREATED)
    fun addAddress(request: HttpServletRequest,@Valid @RequestBody body: AddressRequest)=service.saveAddress(user(request),null,body)
    @PutMapping("/api/addresses/{id}") fun updateAddress(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: AddressRequest)=service.saveAddress(user(request),id,body)
    @DeleteMapping("/api/addresses/{id}") fun removeAddress(request: HttpServletRequest,@PathVariable id: Long): Map<String,Boolean> { service.deleteAddress(user(request),id);return mapOf("success" to true) }
    @GetMapping("/api/cart") fun cart(request: HttpServletRequest)=service.cart(user(request))
    @PostMapping("/api/cart/items") fun addCart(request: HttpServletRequest,@Valid @RequestBody body: AddCartRequest)=service.setCart(user(request),body.productId,body.quantity,true)
    @PutMapping("/api/cart/items/{id}") fun updateCart(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: QuantityRequest)=service.setCart(user(request),id,body.quantity,false)
    @DeleteMapping("/api/cart/items/{id}") fun removeCart(request: HttpServletRequest,@PathVariable id: Long)=service.removeCart(user(request),id)
    @GetMapping("/api/orders") fun orders(request: HttpServletRequest)=service.orders(user(request))
    @PostMapping("/api/checkout/quote") fun quote(request: HttpServletRequest,@Valid @RequestBody body: CheckoutRequest)=service.quote(user(request),body)
    @PostMapping("/api/orders") @ResponseStatus(HttpStatus.CREATED)
    fun checkout(request: HttpServletRequest,@RequestHeader("Idempotency-Key") key: String,@Valid @RequestBody body: CheckoutRequest)=service.checkout(user(request),key,body)
    @GetMapping("/api/orders/{id}") fun order(request: HttpServletRequest,@PathVariable id: Long)=service.order(user(request),id)
    @PostMapping("/api/orders/{id}/cancel") fun cancel(request: HttpServletRequest,@PathVariable id: Long): Map<String,Any?> { val userId=user(request);service.cancel(userId,id);return service.order(userId,id) }
    @GetMapping("/api/products/{id}/reviews") fun reviews(@PathVariable id: Long,@RequestParam(required=false) rating: Int?,@RequestParam(defaultValue="0") page: Int,@RequestParam(defaultValue="10") size: Int)=service.reviews(id,rating,page,size)
    @PostMapping("/api/products/{id}/reviews") @ResponseStatus(HttpStatus.CREATED)
    fun review(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: ReviewRequest)=service.review(user(request),id,body)
    @GetMapping("/api/favorites") fun favorites(request: HttpServletRequest)=service.favorites(user(request))
    @PutMapping("/api/favorites/{id}") fun favorite(request: HttpServletRequest,@PathVariable id: Long): Map<String,Boolean> { service.favorite(user(request),id,true);return mapOf("success" to true) }
    @DeleteMapping("/api/favorites/{id}") fun unfavorite(request: HttpServletRequest,@PathVariable id: Long): Map<String,Boolean> { service.favorite(user(request),id,false);return mapOf("success" to true) }
    @GetMapping("/api/payment-methods") fun methods()=listOf(mapOf("method" to "COD","available" to true),mapOf("method" to "EWALLET","available" to false),mapOf("method" to "CARD","available" to false))
    @PostMapping("/api/payments/{id}/session") fun session(request: HttpServletRequest,@PathVariable id: Long): Map<String,Any?> { val order=service.order(user(request),id);return online.createSession(id,order["paymentMethod"] as String) }
    @PostMapping("/api/payments/webhook") fun webhook(@RequestBody payload: ByteArray,@RequestHeader(value="X-Payment-Signature",required=false) signature: String?): Map<String,Boolean> { online.verifyWebhook(payload,signature); return mapOf("accepted" to false) }
}
