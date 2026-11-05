package com.example.smartmart.master

import com.example.smartmart.BuildConfig
import com.example.smartmart.data.api.ProductApiService
import com.example.smartmart.data.repository.ProductRepositoryImpl
import com.example.smartmart.data.repository.toDomain
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.repository.ProductRepository
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class StoreFailure(val userMessage: String, val code: String = "UNKNOWN") : Exception(userMessage)

/** Business contract used by the store ViewModel and test doubles. */
interface StoreRepository {
    suspend fun catalog(query: CatalogQuery): ProductPage
    suspend fun filters(): Filters
    suspend fun login(email: String, password: String): User
    suspend fun register(name: String, email: String, password: String, phone: String?): User
    suspend fun logout()
    suspend fun profile(name: String, phone: String?, email: String?): User
    suspend fun cart(): Cart
    suspend fun add(productId: Long, quantity: Int): Cart
    suspend fun quantity(productId: Long, quantity: Int): Cart
    suspend fun remove(productId: Long): Cart
    suspend fun addresses(): List<Address>
    suspend fun saveAddress(address: Address): Address
    suspend fun removeAddress(id: Long)
    suspend fun orders(): List<Order>
    suspend fun order(id: Long): Order
    suspend fun checkout(key: String, request: CheckoutRequest): Order
    suspend fun quote(request: CheckoutRequest): CheckoutQuote
    suspend fun tracking(id: Long): ShipmentTracking
    suspend fun trackingLocation(id: Long): ReportedLocation
    suspend fun shipment(id: Long): Shipment
    suspend fun cancel(id: Long): Order
    suspend fun reviews(id: Long, rating: Int?, page: Int): ReviewPage
    suspend fun review(id: Long, orderId: Long, rating: Int, comment: String?)
    suspend fun favorites(): List<Product>
    suspend fun favorite(id: Long, enabled: Boolean)
    suspend fun management(role: String): ManagementState
    suspend fun adminAction(action: String, id: Long?, body: Map<String, Any?>)
    suspend fun users(query: String?): List<UserStatus>
    suspend fun report(from: String?, to: String?): Map<String, Any?>
}

/**
 * Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * Stores no password/card data. Token lives only for this app process; cart/orders live in SQL.
 */
class ApiStoreRepository(private val api: StoreApi, private val session: SessionToken) : StoreRepository {
    override suspend fun catalog(query: CatalogQuery): ProductPage = safe {
        val min = query.minPrice.takeIf(String::isNotBlank)?.toBigDecimalOrNull()
        val max = query.maxPrice.takeIf(String::isNotBlank)?.toBigDecimalOrNull()
        if ((query.minPrice.isNotBlank() && min == null) || (query.maxPrice.isNotBlank() && max == null) ||
            (min != null && min.signum() < 0) || (max != null && max.signum() < 0) ||
            (min != null && max != null && min > max)) throw StoreFailure("Khoảng giá không hợp lệ.", "VALIDATION")
        val page = api.products(query.query.trim().takeIf(String::isNotBlank), query.categoryId, min, max, query.minRating,
            query.inStock, query.brand, query.sort, query.page)
        ProductPage(page.items.map { it.toDomain(checkNotNull(it.id)) }, page.page, page.totalPages, page.totalItems)
    }
    override suspend fun filters() = safe { api.filters() }
    override suspend fun login(email: String, password: String) = safe {
        api.login(LoginRequest(email.trim(), password)).let { session.value = it.token; session.role = it.user.role; it.user }
    }
    override suspend fun register(name: String, email: String, password: String, phone: String?) = safe {
        api.register(RegisterRequest(name.trim(), email.trim(), password, phone)).let { session.value = it.token; session.role = it.user.role; it.user }
    }
    override suspend fun logout() { try { safe { api.logout() } } finally { session.value = null; session.role = null } }
    override suspend fun profile(name: String, phone: String?, email: String?) = safe { api.updateProfile(ProfileRequest(name.trim(), phone, email?.trim())) }
    override suspend fun cart() = safe { api.cart() }
    override suspend fun add(productId: Long, quantity: Int) = safe {
        require(quantity in 1..999)
        api.addToCart(CartRequest(productId, quantity))
    }
    override suspend fun quantity(productId: Long, quantity: Int) = safe {
        require(quantity in 1..999)
        api.changeQuantity(productId, QuantityRequest(quantity))
    }
    override suspend fun remove(productId: Long) = safe { api.removeCartItem(productId) }
    override suspend fun addresses() = safe { api.addresses() }
    override suspend fun saveAddress(address: Address) = safe {
        val request = AddressWriteRequest(address.recipientName.trim(), address.phone.trim(), address.addressLine.trim(), address.ward, address.district.trim(), address.province.trim(), address.isDefault)
        if (address.id == 0L) api.saveAddress(request) else api.updateAddress(address.id, request)
    }
    override suspend fun removeAddress(id: Long) = safe { api.removeAddress(id) }
    override suspend fun orders() = safe { api.orders() }
    override suspend fun order(id: Long) = safe { if (session.role == "ADMIN") api.adminOrder(id) else api.order(id) }
    override suspend fun checkout(key: String, request: CheckoutRequest) = safe { api.checkout(key, request) }
    override suspend fun quote(request: CheckoutRequest) = safe { api.quote(request) }
    override suspend fun tracking(id: Long) = safe { api.tracking(id) }
    override suspend fun trackingLocation(id: Long) = safe { api.trackingLocation(id) }
    override suspend fun shipment(id: Long) = safe { api.shipment(id) }
    override suspend fun cancel(id: Long) = safe { api.cancel(id); api.order(id) }
    override suspend fun reviews(id: Long, rating: Int?, page: Int) = safe { api.reviews(id, rating, page) }
    override suspend fun review(id: Long, orderId: Long, rating: Int, comment: String?) = safe {
        api.addReview(id, ReviewRequest(orderId, rating, comment))
    }
    override suspend fun favorites() = safe { api.favorites().map { it.toDomain(checkNotNull(it.id)) } }
    override suspend fun favorite(id: Long, enabled: Boolean) = safe { if (enabled) api.favorite(id) else api.unfavorite(id) }
    override suspend fun management(role: String) = safe {
        if (role == "SHIPPER") return@safe ManagementState(shipments = api.shipments())
        ManagementState(
            products = api.adminProducts(), categories = api.adminCategories(),
            inventory = api.inventory(), orders = if (role == "ADMIN") api.adminOrders() else emptyList(),
            vouchers = if (role == "ADMIN") api.vouchers() else emptyList(),
            report = if (role == "ADMIN") api.report() else emptyMap(),
            shippers = if (role == "ADMIN") api.shippers() else emptyList(),
            users = if (role == "ADMIN") api.users(null) else emptyList(),
        )
    }
    override suspend fun adminAction(action: String, id: Long?, body: Map<String, Any?>) = safe {
        val requestBody = body.filterKeys { !it.startsWith("_") }
        when (action) {
            "product" -> if (id == null) api.createProduct(requestBody) else api.updateProduct(id, requestBody)
            "category" -> if (id == null) api.createCategory(requestBody) else api.updateCategory(id, requestBody)
            "category_disable" -> api.disableCategory(checkNotNull(id))
            "inventory" -> api.adjustInventory(checkNotNull(id), checkNotNull(body["_idempotencyKey"]).toString(), requestBody)
            "count" -> api.countInventory(checkNotNull(id), checkNotNull(body["_idempotencyKey"]).toString(), requestBody)
            "order" -> api.orderStatus(checkNotNull(id), body.mapValues { it.value.toString() })
            "refund" -> api.refund(checkNotNull(id), body.mapValues { it.value.toString() })
            "cod" -> api.collectCod(checkNotNull(id), mapOf("amount" to checkNotNull(body["amount"]).toString().toBigDecimal()))
            "voucher" -> if (id == null) api.createVoucher(requestBody) else api.updateVoucher(id, requestBody)
            "voucher_disable" -> api.disableVoucher(checkNotNull(id))
            "refund_cash" -> api.confirmCashRefund(checkNotNull(id), mapOf("cashReturned" to true))
            "user_status" -> api.userStatus(checkNotNull(id), mapOf("isActive" to (body["isActive"] == true)))
            "assign_shipper" -> api.assignShipper(checkNotNull(id), mapOf("shipperId" to body["shipperId"].toString().toLong()))
            "shipment_accept" -> api.acceptShipment(checkNotNull(id))
            "shipment_status" -> api.shipmentStatus(checkNotNull(id), mapOf("status" to body["status"].toString()))
            "location" -> api.reportLocation(mapOf("latitude" to body["latitude"].toString().toBigDecimal(), "longitude" to body["longitude"].toString().toBigDecimal()))
            else -> throw StoreFailure("Thao tác không hợp lệ.", "VALIDATION")
        }
        Unit
    }
    override suspend fun users(query: String?) = safe { api.users(query) }
    override suspend fun report(from: String?, to: String?) = safe { api.report(from, to) }
}

/** Never expose raw Retrofit/SQL exception text. Codes map to reviewed Vietnamese UI text. */
internal suspend fun <T> safe(block: suspend () -> T): T = try { block() }
catch (cancelled: CancellationException) { throw cancelled }
catch (failure: StoreFailure) { throw failure }
catch (failure: HttpException) {
    val code = try { failure.response()?.errorBody()?.use { Gson().fromJson(it.string(), Map::class.java)["code"]?.toString() } } catch (_: Exception) { null }
    timber.log.Timber.w("Store API rejected request: HTTP %d, code=%s", failure.code(), code ?: "UNKNOWN")
    val message = when {
        code == "PAYMENT_PROVIDER_UNCONFIGURED" -> "Chưa cấu hình nhà cung cấp thanh toán. Hãy chọn COD hoặc thử lại sau."
        code == "INSUFFICIENT_STOCK" || code == "OUT_OF_STOCK" -> "Số lượng hàng hiện tại không đủ. Vui lòng cập nhật giỏ."
        code == "REVIEW_NOT_ELIGIBLE" || code == "PURCHASE_REQUIRED" -> "Chỉ được đánh giá sản phẩm trong đơn hàng đã giao của bạn."
        failure.code() == 401 -> "Vui lòng đăng nhập lại để tiếp tục."
        failure.code() == 403 -> "Tài khoản không có quyền thực hiện thao tác này."
        failure.code() == 404 -> "Không tìm thấy dữ liệu yêu cầu."
        failure.code() == 409 -> "Dữ liệu đã thay đổi hoặc thao tác bị trùng. Vui lòng tải lại."
        failure.code() == 400 -> "Thông tin chưa hợp lệ. Vui lòng kiểm tra các trường đã nhập."
        failure.code() == 503 -> "Dịch vụ hiện chưa sẵn sàng. Vui lòng thử lại sau."
        else -> "Không thể hoàn tất yêu cầu. Vui lòng thử lại."
    }
    throw StoreFailure(message, code ?: failure.code().toString())
} catch (failure: Exception) {
    timber.log.Timber.e("Store request failed (%s)", failure.javaClass.simpleName)
    throw StoreFailure("Không thể kết nối hoặc đọc dữ liệu máy chủ. Vui lòng thử lại.", "NETWORK")
}

class SessionToken { @Volatile var value: String? = null; @Volatile var role: String? = null }

/** Shared composition root: one Retrofit/client per process, no client creation on recomposition. */
object StoreGraph {
    private val session = SessionToken()
    private val client = OkHttpClient.Builder().retryOnConnectionFailure(false).connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS).addInterceptor { chain ->
            val request = chain.request().newBuilder()
            session.value?.let { request.header("Authorization", "Bearer $it") }
            chain.proceed(request.build())
        }.build()
    private val retrofit = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val repository: StoreRepository = ApiStoreRepository(retrofit.create(StoreApi::class.java), session)
    val detail: ProductRepository = ProductRepositoryImpl(retrofit.create(ProductApiService::class.java))
    fun image(url: String?): String? = url?.let { if (it.startsWith("/")) BuildConfig.API_BASE_URL.trimEnd('/') + it else it }
}
