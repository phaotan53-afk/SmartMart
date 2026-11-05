package com.smartmart.smartmart_backend

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * API quản trị dùng role trong session và soft-delete để giữ FK/lịch sử nhóm.
 */
data class ProductWriteRequest(@field:Positive val categoryId: Int,@field:NotBlank @field:Size(max=40) val sku: String,
    @field:NotBlank @field:Size(max=180) val name: String,@field:Size(max=5000) val description: String?=null,
    @field:NotBlank @field:Size(max=30) val unit: String,@field:DecimalMin("0.00") @field:DecimalMax("9999999999.99") val price: BigDecimal,
    @field:DecimalMin("0.00") @field:DecimalMax("9999999999.99") val costPrice: BigDecimal?=null,
    @field:Size(max=100) val brand: String?=null,val isActive: Boolean=true,@field:Size(max=1000) val imageUrl: String?=null)
data class CategoryWriteRequest(@field:NotBlank @field:Size(max=100) val name: String,@field:Size(max=500) val description: String?=null,val isActive: Boolean=true)
data class InventoryAdjustment(val quantityChange: Int,val type: String="ADJUSTMENT",@field:NotBlank @field:Size(max=300) val note: String)
data class InventoryCount(@field:Min(0) val quantityOnHand: Int,@field:Min(0) val reorderLevel: Int,@field:NotBlank @field:Size(max=300) val note: String)
data class OrderStatusRequest(val status: String)
data class CodCollection(@field:DecimalMin("0.00") val amount: BigDecimal)
data class RefundRequest(@field:NotBlank @field:Size(max=500) val reason: String)
data class CashReturnedRequest(val cashReturned: Boolean)
data class ShipperAssignment(@field:Positive val shipperId: Long)
data class UserStatusRequest(val isActive: Boolean)
data class VoucherWriteRequest(@field:NotBlank @field:Pattern(regexp="[A-Za-z0-9_-]{1,40}") val code: String,
    @field:Size(max=300) val description: String?=null,val discountType: String,
    @field:DecimalMin(value="0.00",inclusive=false) @field:DecimalMax("9999999999.99") val discountValue: BigDecimal,
    @field:DecimalMin("0.00") @field:DecimalMax("9999999999.99") val minOrderValue: BigDecimal=BigDecimal.ZERO,
    val startAt: LocalDateTime,val endAt: LocalDateTime,@field:Min(1) val usageLimit: Int?=null,val isActive: Boolean=true)
data class VoucherValidation(@field:NotBlank val code: String,@field:DecimalMin("0.00") val subtotal: BigDecimal)
data class ShipperLocation(@field:DecimalMin("-90") @field:DecimalMax("90") val latitude: BigDecimal,@field:DecimalMin("-180") @field:DecimalMax("180") val longitude: BigDecimal)

@Service
class AdminService(private val jdbc: JdbcTemplate,private val commerce: CommerceService) {
    fun products()=jdbc.queryForList("SELECT p.ProductID AS id,p.CategoryID AS categoryId,p.SKU AS sku,p.ProductName AS name,p.Description AS description,p.Unit AS unit,p.Price AS price,p.CostPrice AS costPrice,p.Brand AS brand,p.IsActive AS isActive,i.QuantityOnHand AS quantityOnHand FROM dbo.Products p LEFT JOIN dbo.Inventory i ON i.ProductID=p.ProductID ORDER BY p.ProductID DESC")
    @Transactional
    fun saveProduct(id: Long?,body: ProductWriteRequest): Map<String,Any?> {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Categories WHERE CategoryID=? AND IsActive=1",Int::class.java,body.categoryId)!! == 0) badRequest("Danh mục không tồn tại hoặc đã ngừng sử dụng")
        body.imageUrl?.let { if(!it.startsWith("/assets/") && !it.startsWith("https://")) badRequest("Ảnh phải dùng HTTPS hoặc tài nguyên /assets/") }
        val savedId=if(id==null) {
            val newId=jdbc.queryForObject("INSERT dbo.Products(CategoryID,SKU,ProductName,Description,Unit,Price,CostPrice,Brand,IsActive) OUTPUT INSERTED.ProductID VALUES(?,?,?,?,?,?,?,?,?)",Long::class.java,body.categoryId,body.sku.trim(),body.name.trim(),body.description,body.unit.trim(),body.price,body.costPrice,body.brand?.trim(),body.isActive)!!
            jdbc.update("INSERT dbo.Inventory(ProductID) VALUES(?)",newId);newId
        } else {
            if(jdbc.update("UPDATE dbo.Products SET CategoryID=?,SKU=?,ProductName=?,Description=?,Unit=?,Price=?,CostPrice=?,Brand=?,IsActive=?,UpdatedAt=SYSDATETIME() WHERE ProductID=?",body.categoryId,body.sku.trim(),body.name.trim(),body.description,body.unit.trim(),body.price,body.costPrice,body.brand?.trim(),body.isActive,id)!=1) notFound("Không tìm thấy sản phẩm")
            id
        }
        if(!body.imageUrl.isNullOrBlank()) {
            jdbc.update("UPDATE dbo.ProductImages SET IsPrimary=0 WHERE ProductID=?",savedId)
            jdbc.update("INSERT dbo.ProductImages(ProductID,ImageUrl,AltText,IsPrimary) SELECT ?,?,?,1 WHERE NOT EXISTS(SELECT 1 FROM dbo.ProductImages WHERE ProductID=? AND ImageUrl=?)",savedId,body.imageUrl,body.name,savedId,body.imageUrl)
            jdbc.update("UPDATE dbo.ProductImages SET IsPrimary=1 WHERE ProductID=? AND ImageUrl=?",savedId,body.imageUrl)
        }
        return products().first { (it["id"] as Number).toLong()==savedId }
    }
    fun disableProduct(id: Long) { if(jdbc.update("UPDATE dbo.Products SET IsActive=0,UpdatedAt=SYSDATETIME() WHERE ProductID=?",id)!=1) notFound("Không tìm thấy sản phẩm") }
    fun categories()=jdbc.queryForList("SELECT CategoryID AS id,CategoryName AS name,Description AS description,IsActive AS isActive FROM dbo.Categories ORDER BY CategoryID")
    fun saveCategory(id: Int?,body: CategoryWriteRequest): Map<String,Any?> {
        val saved=if(id==null) jdbc.queryForObject("INSERT dbo.Categories(CategoryName,Description,IsActive) OUTPUT INSERTED.CategoryID VALUES(?,?,?)",Int::class.java,body.name.trim(),body.description,body.isActive)!!
        else { if(jdbc.update("UPDATE dbo.Categories SET CategoryName=?,Description=?,IsActive=? WHERE CategoryID=?",body.name.trim(),body.description,body.isActive,id)!=1) notFound("Không tìm thấy danh mục");id }
        return categories().first { (it["id"] as Number).toInt()==saved }
    }
    fun disableCategory(id: Int) { if(jdbc.update("UPDATE dbo.Categories SET IsActive=0 WHERE CategoryID=?",id)!=1) notFound("Không tìm thấy danh mục") }
    fun inventory()=jdbc.queryForList("SELECT p.ProductID AS productId,p.ProductName AS name,i.QuantityOnHand AS quantityOnHand,i.ReorderLevel AS reorderLevel,CAST(CASE WHEN i.QuantityOnHand<=i.ReorderLevel THEN 1 ELSE 0 END AS BIT) AS lowStock FROM dbo.Inventory i JOIN dbo.Products p ON p.ProductID=i.ProductID ORDER BY p.ProductID")
    private fun mutation(actor: Long,key: String?,type: String,content: String): Boolean {
        if(key==null) return true
        if(!key.matches(Regex("[A-Za-z0-9-]{16,100}"))) badRequest("Idempotency-Key không hợp lệ")
        jdbc.queryForObject("SELECT UserID FROM dbo.Users WITH(UPDLOCK,HOLDLOCK) WHERE UserID=?",Long::class.java,actor)
        val digest=Passwords.digest(content)
        val previous=jdbc.queryForList("SELECT RequestHash FROM dbo.MutationKeys WHERE UserID=? AND OperationType=? AND RequestKey=?",String::class.java,actor,type,key).firstOrNull()
        if(previous!=null) { if(previous!=digest) conflict("IDEMPOTENCY_CONFLICT","Mã yêu cầu đã dùng cho nội dung khác");return false }
        jdbc.update("INSERT dbo.MutationKeys(UserID,OperationType,RequestKey,RequestHash) VALUES(?,?,?,?)",actor,type,key,digest)
        return true
    }
    @Transactional
    fun adjust(actor: Long,productId: Long,body: InventoryAdjustment,key: String?): List<Map<String,Any?>> {
        if(body.quantityChange==0 || body.type !in listOf("IMPORT","ADJUSTMENT","DAMAGE") || body.type=="IMPORT" && body.quantityChange<1 || body.type=="DAMAGE" && body.quantityChange>0) badRequest("Loại điều chỉnh và số lượng không hợp lệ")
        if(!mutation(actor,key,"INVENTORY_ADJUST","$productId|$body")) return inventory()
        val row=jdbc.queryForList("SELECT InventoryID AS id,QuantityOnHand AS quantity FROM dbo.Inventory WITH(UPDLOCK,HOLDLOCK) WHERE ProductID=?",productId).firstOrNull() ?: notFound("Không tìm thấy tồn kho")
        val quantity=(row["quantity"] as Number).toLong()+body.quantityChange.toLong()
        if(quantity !in 0..Int.MAX_VALUE.toLong()) conflict("INVALID_STOCK","Điều chỉnh làm tồn kho âm hoặc vượt giới hạn")
        jdbc.update("UPDATE dbo.Inventory SET QuantityOnHand=?,UpdatedAt=SYSDATETIME() WHERE ProductID=?",quantity,productId)
        jdbc.update("INSERT dbo.InventoryTransactions(InventoryID,UserID,TransactionType,QuantityChange,Note) VALUES(?,?,?,?,?)",row["id"],actor,body.type,body.quantityChange,body.note.trim())
        return inventory()
    }
    @Transactional
    fun count(actor: Long,productId: Long,body: InventoryCount,key: String?): List<Map<String,Any?>> {
        if(!mutation(actor,key,"INVENTORY_COUNT","$productId|$body")) return inventory()
        val row=jdbc.queryForList("SELECT InventoryID AS id,QuantityOnHand AS quantity FROM dbo.Inventory WITH(UPDLOCK,HOLDLOCK) WHERE ProductID=?",productId).firstOrNull() ?: notFound("Không tìm thấy tồn kho")
        val delta=body.quantityOnHand-(row["quantity"] as Number).toInt()
        jdbc.update("UPDATE dbo.Inventory SET QuantityOnHand=?,ReorderLevel=?,UpdatedAt=SYSDATETIME() WHERE ProductID=?",body.quantityOnHand,body.reorderLevel,productId)
        if(delta!=0) jdbc.update("INSERT dbo.InventoryTransactions(InventoryID,UserID,TransactionType,QuantityChange,Note) VALUES(?,?,'ADJUSTMENT',?,?)",row["id"],actor,delta,"Kiểm kê: "+body.note.take(290))
        return inventory()
    }
    fun inventoryHistory(productId: Long?)=if(productId==null) jdbc.queryForList("SELECT TOP(200) t.InventoryTransactionID AS id,i.ProductID AS productId,t.TransactionType AS type,t.QuantityChange AS quantityChange,t.Note AS note,t.CreatedAt AS createdAt FROM dbo.InventoryTransactions t JOIN dbo.Inventory i ON i.InventoryID=t.InventoryID ORDER BY t.InventoryTransactionID DESC") else jdbc.queryForList("SELECT TOP(200) t.InventoryTransactionID AS id,i.ProductID AS productId,t.TransactionType AS type,t.QuantityChange AS quantityChange,t.Note AS note,t.CreatedAt AS createdAt FROM dbo.InventoryTransactions t JOIN dbo.Inventory i ON i.InventoryID=t.InventoryID WHERE i.ProductID=? ORDER BY t.InventoryTransactionID DESC",productId)
    fun orders()=jdbc.queryForList("SELECT o.OrderID AS id,o.OrderCode AS orderCode,o.CustomerID AS customerId,u.FullName AS customerName,o.OrderStatus AS orderStatus,o.PaymentStatus AS paymentStatus,o.TotalAmount AS totalAmount,o.CreatedAt AS createdAt FROM dbo.Orders o JOIN dbo.Users u ON u.UserID=o.CustomerID ORDER BY o.OrderID DESC")
    @Transactional
    fun orderStatus(actor: Long,id: Long,target: String): Map<String,Any?> {
        val order=jdbc.queryForList("SELECT OrderStatus AS status,CustomerID AS customerId FROM dbo.Orders WITH(UPDLOCK,HOLDLOCK) WHERE OrderID=?",id).firstOrNull() ?: notFound("Không tìm thấy đơn hàng")
        val old=order["status"] as String
        if(old==target) return commerce.order(actor,id,true)
        val allowed=mapOf("PENDING" to setOf("CONFIRMED","CANCELLED"),"CONFIRMED" to setOf("PREPARING","CANCELLED"),"PREPARING" to setOf("SHIPPING","CANCELLED"),"SHIPPING" to setOf("DELIVERED"))
        if(target !in allowed[old].orEmpty()) conflict("ORDER_STATE","Chuyển trạng thái đơn hàng không hợp lệ")
        if(target=="CANCELLED") {
            commerce.restoreInventory(id,actor)
            jdbc.update("UPDATE dbo.Payments SET PaymentStatus='FAILED' WHERE OrderID=? AND PaymentStatus='PENDING'",id)
            jdbc.update("DELETE dbo.VoucherRedemptions WHERE OrderID=?",id)
        }
        jdbc.update("UPDATE dbo.Orders SET OrderStatus=? WHERE OrderID=?",target,id)
        jdbc.update("INSERT dbo.OrderStatusHistory(OrderID,ChangedByUserID,OldStatus,NewStatus,Note) VALUES(?,?,?,?,N'Quản trị cập nhật trạng thái')",id,actor,old,target)
        if(target=="SHIPPING") jdbc.update("UPDATE dbo.Shipments SET ShipmentStatus='IN_TRANSIT',ShippedAt=COALESCE(ShippedAt,SYSDATETIME()) WHERE OrderID=?",id)
        if(target=="DELIVERED") jdbc.update("UPDATE dbo.Shipments SET ShipmentStatus='DELIVERED',DeliveredAt=COALESCE(DeliveredAt,SYSDATETIME()) WHERE OrderID=?",id)
        return commerce.order(actor,id,true)
    }
    @Transactional
    fun collect(actor: Long,id: Long,amount: BigDecimal): Map<String,Any?> {
        val row=jdbc.queryForList("SELECT OrderStatus AS status,PaymentStatus AS paymentStatus,TotalAmount AS amount FROM dbo.Orders WITH(UPDLOCK,HOLDLOCK) WHERE OrderID=?",id).firstOrNull() ?: notFound("Không tìm thấy đơn hàng")
        if(row["status"]!="DELIVERED") conflict("ORDER_STATE","Chỉ ghi nhận tiền COD sau khi đã giao thành công")
        if(amount.compareTo(row["amount"] as BigDecimal)!=0) badRequest("Số tiền thu phải khớp tổng đơn hàng")
        if(row["paymentStatus"]=="PAID") return commerce.order(actor,id,true)
        if(row["paymentStatus"]!="UNPAID") conflict("PAYMENT_STATE","Trạng thái thanh toán không hợp lệ")
        if(jdbc.update("UPDATE dbo.Payments SET PaymentStatus='SUCCESS',PaidAt=SYSDATETIME() WHERE OrderID=? AND PaymentMethod='COD' AND PaymentStatus='PENDING'",id)!=1) conflict("PAYMENT_STATE","Không có thanh toán COD chờ thu")
        jdbc.update("UPDATE dbo.Orders SET PaymentStatus='PAID' WHERE OrderID=?",id)
        return commerce.order(actor,id,true)
    }
    @Transactional
    fun requestRefund(actor: Long,id: Long,body: RefundRequest): Map<String,Any?> {
        val row=jdbc.queryForList("SELECT PaymentStatus AS paymentStatus,TotalAmount AS amount FROM dbo.Orders WITH(UPDLOCK,HOLDLOCK) WHERE OrderID=?",id).firstOrNull() ?: notFound("Không tìm thấy đơn hàng")
        if(row["paymentStatus"]!="PAID") conflict("PAYMENT_STATE","Chỉ tạo yêu cầu hoàn cho đơn đã thanh toán")
        val existing=jdbc.queryForList("SELECT RefundID AS id,RefundStatus AS status,Amount AS amount,Reason AS reason FROM dbo.RefundRequests WHERE OrderID=?",id).firstOrNull()
        if(existing!=null) return existing
        jdbc.update("INSERT dbo.RefundRequests(OrderID,RequestedBy,Amount,Reason) VALUES(?,?,?,?)",id,actor,row["amount"],body.reason.trim())
        return jdbc.queryForList("SELECT RefundID AS id,RefundStatus AS status,Amount AS amount,Reason AS reason FROM dbo.RefundRequests WHERE OrderID=?",id).first()
    }
    @Transactional
    fun confirmCashRefund(actor: Long,id: Long,body: CashReturnedRequest): Map<String,Any?> {
        if(!body.cashReturned) badRequest("Cần xác nhận tiền mặt thực tế đã hoàn")
        val row=jdbc.queryForList("SELECT r.RefundStatus AS status,p.PaymentMethod AS method FROM dbo.RefundRequests r WITH(UPDLOCK,HOLDLOCK) JOIN dbo.Payments p ON p.OrderID=r.OrderID WHERE r.OrderID=?",id).firstOrNull() ?: notFound("Không tìm thấy yêu cầu hoàn tiền")
        if(row["method"]!="COD") throw ApiException(HttpStatus.SERVICE_UNAVAILABLE,"PAYMENT_PROVIDER_UNCONFIGURED","Chưa cấu hình nhà cung cấp hoàn tiền online")
        if(row["status"]=="SUCCEEDED") return commerce.order(actor,id,true)
        jdbc.update("UPDATE dbo.RefundRequests SET RefundStatus='SUCCEEDED',CompletedAt=SYSDATETIME() WHERE OrderID=?",id)
        jdbc.update("UPDATE dbo.Payments SET PaymentStatus='REFUNDED' WHERE OrderID=?",id)
        jdbc.update("UPDATE dbo.Orders SET PaymentStatus='REFUNDED',OrderStatus='REFUNDED' WHERE OrderID=?",id)
        return commerce.order(actor,id,true)
    }
    fun vouchers()=jdbc.queryForList("SELECT VoucherID AS id,Code AS code,Description AS description,DiscountType AS discountType,DiscountValue AS discountValue,MinOrderValue AS minOrderValue,StartAt AS startAt,EndAt AS endAt,UsageLimit AS usageLimit,IsActive AS isActive,(SELECT COUNT(*) FROM dbo.VoucherRedemptions r WHERE r.VoucherID=v.VoucherID) AS usedCount FROM dbo.Vouchers v ORDER BY VoucherID DESC")
    fun saveVoucher(id: Long?,body: VoucherWriteRequest): Map<String,Any?> {
        if(body.discountType !in listOf("PERCENT","AMOUNT") || body.discountType=="PERCENT" && body.discountValue>BigDecimal(100) || body.endAt<=body.startAt) badRequest("Loại, giá trị hoặc thời hạn voucher không hợp lệ")
        val saved=if(id==null) jdbc.queryForObject("INSERT dbo.Vouchers(Code,Description,DiscountType,DiscountValue,MinOrderValue,StartAt,EndAt,UsageLimit,IsActive) OUTPUT INSERTED.VoucherID VALUES(?,?,?,?,?,?,?,?,?)",Long::class.java,body.code.uppercase(),body.description,body.discountType,body.discountValue,body.minOrderValue,Timestamp.valueOf(body.startAt),Timestamp.valueOf(body.endAt),body.usageLimit,body.isActive)!!
        else { if(jdbc.update("UPDATE dbo.Vouchers SET Code=?,Description=?,DiscountType=?,DiscountValue=?,MinOrderValue=?,StartAt=?,EndAt=?,UsageLimit=?,IsActive=? WHERE VoucherID=?",body.code.uppercase(),body.description,body.discountType,body.discountValue,body.minOrderValue,Timestamp.valueOf(body.startAt),Timestamp.valueOf(body.endAt),body.usageLimit,body.isActive,id)!=1) notFound("Không tìm thấy voucher");id }
        return vouchers().first { (it["id"] as Number).toLong()==saved }
    }
    fun disableVoucher(id: Long) { if(jdbc.update("UPDATE dbo.Vouchers SET IsActive=0 WHERE VoucherID=?",id)!=1) notFound("Không tìm thấy voucher") }
    fun validateVoucher(body: VoucherValidation): Map<String,Any?> {
        val row=jdbc.queryForList("SELECT VoucherID AS id,Code AS code,DiscountType AS type,DiscountValue AS value,MinOrderValue AS minimum,UsageLimit AS limit FROM dbo.Vouchers WHERE Code=? AND IsActive=1 AND StartAt<=SYSDATETIME() AND EndAt>SYSDATETIME()",body.code.trim().uppercase()).firstOrNull() ?: conflict("INVALID_VOUCHER","Mã giảm giá không hợp lệ hoặc đã hết hạn")
        if(body.subtotal<(row["minimum"] as BigDecimal)) conflict("VOUCHER_MINIMUM","Đơn hàng chưa đạt giá trị tối thiểu")
        val used=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.VoucherRedemptions WHERE VoucherID=?",Int::class.java,row["id"])!!
        if((row["limit"] as Number?)?.toInt()?.let { used>=it }==true) conflict("VOUCHER_EXHAUSTED","Mã đã hết lượt sử dụng")
        val discount=(if(row["type"]=="PERCENT") body.subtotal*(row["value"] as BigDecimal)/BigDecimal(100) else row["value"] as BigDecimal).min(body.subtotal).setScale(2,java.math.RoundingMode.HALF_UP)
        return mapOf("valid" to true,"code" to row["code"],"discountAmount" to discount,"message" to "Mã có thể áp dụng; điều kiện sẽ được kiểm tra lại khi đặt hàng")
    }
    fun users(query: String?): List<Map<String,Any?>> {
        if(query.orEmpty().length>100) badRequest("Từ khóa quá dài")
        val like="%"+query.orEmpty().trim().replace("[","[[]").replace("%","[%]").replace("_","[_]")+"%"
        return jdbc.queryForList("SELECT UserID AS id,FullName AS fullName,Email AS email,Phone AS phone,Role AS role,IsActive AS isActive FROM dbo.Users WHERE FullName LIKE ? OR Email LIKE ? ORDER BY UserID DESC",like,like)
    }
    @Transactional
    fun userStatus(actor: Long,id: Long,active: Boolean) {
        if(actor==id) badRequest("Không thể tự khóa tài khoản quản trị đang dùng")
        if(jdbc.update("UPDATE dbo.Users SET IsActive=? WHERE UserID=?",active,id)!=1) notFound("Không tìm thấy người dùng")
        if(!active) jdbc.update("DELETE dbo.SessionTokens WHERE UserID=?",id)
    }
    fun reports(from: LocalDate?,to: LocalDate?): Map<String,Any?> {
        val start=from ?: LocalDate.now().minusDays(30);val end=to ?: LocalDate.now()
        if(end<start || java.time.temporal.ChronoUnit.DAYS.between(start,end)>3660) badRequest("Khoảng thời gian không hợp lệ hoặc quá 10 năm")
        val args=arrayOf<Any>(Timestamp.valueOf(start.atStartOfDay()),Timestamp.valueOf(end.plusDays(1).atStartOfDay()))
        val summary=jdbc.queryForList("SELECT COUNT_BIG(*) AS orderCount,COALESCE(SUM(CASE WHEN PaymentStatus='PAID' THEN TotalAmount ELSE 0 END),0) AS paidRevenue,COALESCE(SUM(CASE WHEN PaymentStatus='UNPAID' THEN TotalAmount ELSE 0 END),0) AS unpaidAmount FROM dbo.Orders WHERE CreatedAt>=? AND CreatedAt<?",*args).first()
        val daily=jdbc.queryForList("SELECT CAST(CreatedAt AS DATE) AS date,COUNT_BIG(*) AS orderCount,SUM(CASE WHEN PaymentStatus='PAID' THEN TotalAmount ELSE 0 END) AS paidRevenue FROM dbo.Orders WHERE CreatedAt>=? AND CreatedAt<? GROUP BY CAST(CreatedAt AS DATE) ORDER BY date",*args)
        val statuses=jdbc.queryForList("SELECT OrderStatus AS status,COUNT_BIG(*) AS count FROM dbo.Orders WHERE CreatedAt>=? AND CreatedAt<? GROUP BY OrderStatus",*args)
        val best=jdbc.queryForList("SELECT TOP(10) p.ProductID AS productId,p.ProductName AS name,SUM(CAST(oi.Quantity AS BIGINT)) AS quantitySold,SUM(oi.LineTotal) AS grossProductRevenue FROM dbo.OrderItems oi JOIN dbo.Orders o ON o.OrderID=oi.OrderID JOIN dbo.Products p ON p.ProductID=oi.ProductID WHERE o.CreatedAt>=? AND o.CreatedAt<? AND o.OrderStatus='DELIVERED' GROUP BY p.ProductID,p.ProductName ORDER BY quantitySold DESC",*args)
        val delivery=jdbc.queryForList("SELECT u.UserID AS shipperId,u.FullName AS name,COUNT_BIG(*) AS shipmentCount,SUM(CASE WHEN s.ShipmentStatus='DELIVERED' THEN 1 ELSE 0 END) AS deliveredCount FROM dbo.Shipments s JOIN dbo.Users u ON u.UserID=s.ShipperID JOIN dbo.Orders o ON o.OrderID=s.OrderID WHERE o.CreatedAt>=? AND o.CreatedAt<? GROUP BY u.UserID,u.FullName",*args)
        return mapOf("from" to start,"to" to end,"summary" to summary,"daily" to daily,"ordersByStatus" to statuses,"bestSellers" to best,"deliveryPerformance" to delivery)
    }
}

@RestController
class AdminController(private val auth: AuthService,private val service: AdminService,private val commerce: CommerceService) {
    private fun admin(request: HttpServletRequest)=auth.roles(request,"ADMIN").id
    private fun warehouse(request: HttpServletRequest)=auth.roles(request,"ADMIN","WAREHOUSE_MANAGER").id
    @GetMapping("/api/admin/products") fun products(request: HttpServletRequest): List<Map<String,Any?>> { warehouse(request);return service.products() }
    @PostMapping("/api/admin/products") @ResponseStatus(HttpStatus.CREATED) fun addProduct(request: HttpServletRequest,@Valid @RequestBody body: ProductWriteRequest): Map<String,Any?> { warehouse(request);return service.saveProduct(null,body) }
    @PutMapping("/api/admin/products/{id}") fun product(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: ProductWriteRequest): Map<String,Any?> { warehouse(request);return service.saveProduct(id,body) }
    @DeleteMapping("/api/admin/products/{id}") fun removeProduct(request: HttpServletRequest,@PathVariable id: Long): Map<String,Boolean> { warehouse(request);service.disableProduct(id);return mapOf("success" to true) }
    @GetMapping("/api/admin/categories") fun categories(request: HttpServletRequest): List<Map<String,Any?>> { warehouse(request);return service.categories() }
    @PostMapping("/api/admin/categories") @ResponseStatus(HttpStatus.CREATED) fun addCategory(request: HttpServletRequest,@Valid @RequestBody body: CategoryWriteRequest): Map<String,Any?> { warehouse(request);return service.saveCategory(null,body) }
    @PutMapping("/api/admin/categories/{id}") fun category(request: HttpServletRequest,@PathVariable id: Int,@Valid @RequestBody body: CategoryWriteRequest): Map<String,Any?> { warehouse(request);return service.saveCategory(id,body) }
    @DeleteMapping("/api/admin/categories/{id}") fun removeCategory(request: HttpServletRequest,@PathVariable id: Int): Map<String,Boolean> { warehouse(request);service.disableCategory(id);return mapOf("success" to true) }
    @GetMapping("/api/admin/inventory") fun inventory(request: HttpServletRequest): List<Map<String,Any?>> { warehouse(request);return service.inventory() }
    @PostMapping("/api/admin/inventory/{id}/adjust") fun adjust(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: InventoryAdjustment,@RequestHeader(value="Idempotency-Key",required=false) key: String?)=service.adjust(warehouse(request),id,body,key)
    @PutMapping("/api/admin/inventory/{id}/count") fun count(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: InventoryCount,@RequestHeader(value="Idempotency-Key",required=false) key: String?)=service.count(warehouse(request),id,body,key)
    @GetMapping("/api/admin/inventory/transactions") fun history(request: HttpServletRequest,@RequestParam(required=false) productId: Long?): List<Map<String,Any?>> { warehouse(request);return service.inventoryHistory(productId) }
    @GetMapping("/api/admin/orders") fun orders(request: HttpServletRequest): List<Map<String,Any?>> { admin(request);return service.orders() }
    @GetMapping("/api/admin/orders/{id}") fun order(request: HttpServletRequest,@PathVariable id: Long)=commerce.order(admin(request),id,true)
    @PutMapping("/api/admin/orders/{id}/status") fun status(request: HttpServletRequest,@PathVariable id: Long,@RequestBody body: OrderStatusRequest)=service.orderStatus(admin(request),id,body.status)
    @PostMapping("/api/admin/orders/{id}/collect-cod") fun collect(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: CodCollection)=service.collect(admin(request),id,body.amount)
    @PostMapping("/api/admin/orders/{id}/refund") fun refund(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: RefundRequest)=service.requestRefund(admin(request),id,body)
    @PostMapping("/api/admin/orders/{id}/confirm-cash-refund") fun confirmRefund(request: HttpServletRequest,@PathVariable id: Long,@RequestBody body: CashReturnedRequest)=service.confirmCashRefund(admin(request),id,body)
    @GetMapping("/api/admin/vouchers") fun vouchers(request: HttpServletRequest): List<Map<String,Any?>> { admin(request);return service.vouchers() }
    @PostMapping("/api/admin/vouchers") @ResponseStatus(HttpStatus.CREATED) fun addVoucher(request: HttpServletRequest,@Valid @RequestBody body: VoucherWriteRequest): Map<String,Any?> { admin(request);return service.saveVoucher(null,body) }
    @PutMapping("/api/admin/vouchers/{id}") fun voucher(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: VoucherWriteRequest): Map<String,Any?> { admin(request);return service.saveVoucher(id,body) }
    @DeleteMapping("/api/admin/vouchers/{id}") fun removeVoucher(request: HttpServletRequest,@PathVariable id: Long): Map<String,Boolean> { admin(request);service.disableVoucher(id);return mapOf("success" to true) }
    @PostMapping("/api/vouchers/validate") fun validateVoucher(request: HttpServletRequest,@Valid @RequestBody body: VoucherValidation): Map<String,Any?> { auth.roles(request,"CUSTOMER");return service.validateVoucher(body) }
    @GetMapping("/api/admin/users") fun users(request: HttpServletRequest,@RequestParam(required=false) query: String?): List<Map<String,Any?>> { admin(request);return service.users(query) }
    @PutMapping("/api/admin/users/{id}/status") fun userStatus(request: HttpServletRequest,@PathVariable id: Long,@RequestBody body: UserStatusRequest): Map<String,Boolean> { service.userStatus(admin(request),id,body.isActive);return mapOf("success" to true) }
    @DeleteMapping("/api/admin/users/{id}") fun deleteUser(request: HttpServletRequest,@PathVariable id: Long): Map<String,Boolean> { service.userStatus(admin(request),id,false);return mapOf("success" to true) }
    @GetMapping("/api/admin/reports") fun report(request: HttpServletRequest,@RequestParam(required=false) from: LocalDate?,@RequestParam(required=false) to: LocalDate?): Map<String,Any?> { admin(request);return service.reports(from,to) }
}
