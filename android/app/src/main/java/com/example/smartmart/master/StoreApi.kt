package com.example.smartmart.master

import com.example.smartmart.data.model.ProductResponse
import retrofit2.http.*
import java.math.BigDecimal

/** DTOs are transport only; repository validates products before UI sees them. */
data class ProductPageResponse(val items: List<ProductResponse>, val page: Int, val totalPages: Int, val totalItems: Long)
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val fullName: String, val email: String, val password: String, val phone: String?)
data class CartRequest(val productId: Long, val quantity: Int)
data class QuantityRequest(val quantity: Int)
data class CheckoutRequest(val addressId: Long, val paymentMethod: String, val voucherCode: String?, val customerNote: String?)
data class ReviewRequest(val orderId: Long, val rating: Int, val comment: String?)
data class ProfileRequest(val fullName: String, val phone: String?, val email: String? = null)
data class AddressWriteRequest(val recipientName: String, val phone: String, val addressLine: String, val ward: String?, val district: String, val province: String, val isDefault: Boolean)

/**
 * Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * API authenticated actions derive ownership from the Bearer session on the server.
 */
interface StoreApi {
    @GET("api/v1/products") suspend fun products(
        @Query("query") query: String?, @Query("categoryId") categoryId: Long?,
        @Query("minPrice") minPrice: BigDecimal?, @Query("maxPrice") maxPrice: BigDecimal?,
        @Query("minRating") minRating: Int?, @Query("inStock") inStock: Boolean?,
        @Query("brand") brand: String?, @Query("sort") sort: String,
        @Query("page") page: Int, @Query("size") size: Int = 8,
    ): ProductPageResponse
    @GET("api/catalog/filters") suspend fun filters(): Filters
    @POST("api/auth/login") suspend fun login(@Body request: LoginRequest): Session
    @POST("api/auth/register") suspend fun register(@Body request: RegisterRequest): Session
    @POST("api/auth/logout") suspend fun logout()
    @GET("api/me") suspend fun profile(): User
    @PUT("api/me") suspend fun updateProfile(@Body request: ProfileRequest): User
    @GET("api/cart") suspend fun cart(): Cart
    @POST("api/cart/items") suspend fun addToCart(@Body request: CartRequest): Cart
    @PUT("api/cart/items/{id}") suspend fun changeQuantity(@Path("id") id: Long, @Body request: QuantityRequest): Cart
    @DELETE("api/cart/items/{id}") suspend fun removeCartItem(@Path("id") id: Long): Cart
    @GET("api/addresses") suspend fun addresses(): List<Address>
    @POST("api/addresses") suspend fun saveAddress(@Body address: AddressWriteRequest): Address
    @PUT("api/addresses/{id}") suspend fun updateAddress(@Path("id") id: Long, @Body address: AddressWriteRequest): Address
    @DELETE("api/addresses/{id}") suspend fun removeAddress(@Path("id") id: Long)
    @GET("api/orders") suspend fun orders(): List<Order>
    @GET("api/orders/{id}") suspend fun order(@Path("id") id: Long): Order
    @POST("api/orders") suspend fun checkout(@Header("Idempotency-Key") key: String, @Body request: CheckoutRequest): Order
    @POST("api/checkout/quote") suspend fun quote(@Body request: CheckoutRequest): CheckoutQuote
    @POST("api/orders/{id}/cancel") suspend fun cancel(@Path("id") id: Long)
    @GET("api/products/{id}/reviews") suspend fun reviews(@Path("id") id: Long, @Query("rating") rating: Int?, @Query("page") page: Int, @Query("size") size: Int = 5): ReviewPage
    @POST("api/products/{id}/reviews") suspend fun addReview(@Path("id") id: Long, @Body request: ReviewRequest)
    @GET("api/favorites") suspend fun favorites(): List<ProductResponse>
    @PUT("api/favorites/{id}") suspend fun favorite(@Path("id") id: Long)
    @DELETE("api/favorites/{id}") suspend fun unfavorite(@Path("id") id: Long)
    @GET("api/admin/products") suspend fun adminProducts(): List<AdminProduct>
    @POST("api/admin/products") suspend fun createProduct(@Body body: Map<String, @JvmSuppressWildcards Any?>): AdminProduct
    @PUT("api/admin/products/{id}") suspend fun updateProduct(@Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): AdminProduct
    @GET("api/admin/categories") suspend fun adminCategories(): List<Category>
    @POST("api/admin/categories") suspend fun createCategory(@Body body: Map<String, @JvmSuppressWildcards Any?>): Category
    @PUT("api/admin/categories/{id}") suspend fun updateCategory(@Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): Category
    @GET("api/admin/inventory") suspend fun inventory(): List<InventoryItem>
    @POST("api/admin/inventory/{id}/adjust") suspend fun adjustInventory(@Path("id") id: Long, @Header("Idempotency-Key") key: String, @Body body: Map<String, @JvmSuppressWildcards Any?>)
    @PUT("api/admin/inventory/{id}/count") suspend fun countInventory(@Path("id") id: Long, @Header("Idempotency-Key") key: String, @Body body: Map<String, @JvmSuppressWildcards Any?>)
    @GET("api/admin/orders") suspend fun adminOrders(): List<Order>
    @GET("api/admin/orders/{id}") suspend fun adminOrder(@Path("id") id: Long): Order
    @PUT("api/admin/orders/{id}/status") suspend fun orderStatus(@Path("id") id: Long, @Body body: Map<String, String>)
    @POST("api/admin/orders/{id}/refund") suspend fun refund(@Path("id") id: Long, @Body body: Map<String, String>)
    @POST("api/admin/orders/{id}/collect-cod") suspend fun collectCod(@Path("id") id: Long, @Body body: Map<String, BigDecimal>)
    @GET("api/admin/vouchers") suspend fun vouchers(): List<Voucher>
    @POST("api/admin/vouchers") suspend fun createVoucher(@Body body: Map<String, @JvmSuppressWildcards Any?>): Voucher
    @GET("api/admin/reports") suspend fun report(@Query("from") from: String? = null, @Query("to") to: String? = null): Map<String, Any?>
    @GET("api/admin/users") suspend fun users(@Query("query") query: String?): List<UserStatus>
    @PUT("api/admin/users/{id}/status") suspend fun userStatus(@Path("id") id: Long, @Body body: Map<String, Boolean>)
    @GET("api/admin/shippers") suspend fun shippers(): List<Shipper>
    @POST("api/admin/orders/{id}/assign-shipper") suspend fun assignShipper(@Path("id") id: Long, @Body body: Map<String, Long>)
    @POST("api/admin/orders/{id}/confirm-cash-refund") suspend fun confirmCashRefund(@Path("id") id: Long, @Body body: Map<String, Boolean>)
    @DELETE("api/admin/categories/{id}") suspend fun disableCategory(@Path("id") id: Long)
    @PUT("api/admin/vouchers/{id}") suspend fun updateVoucher(@Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): Voucher
    @DELETE("api/admin/vouchers/{id}") suspend fun disableVoucher(@Path("id") id: Long)
    @GET("api/shipper/shipments") suspend fun shipments(): List<Shipment>
    @GET("api/shipper/shipments/{id}") suspend fun shipment(@Path("id") id: Long): Shipment
    @POST("api/shipper/shipments/{id}/accept") suspend fun acceptShipment(@Path("id") id: Long): Shipment
    @PUT("api/shipper/shipments/{id}/status") suspend fun shipmentStatus(@Path("id") id: Long, @Body body: Map<String, String>): Shipment
    @POST("api/shipper/locations") suspend fun reportLocation(@Body body: Map<String, BigDecimal>): ReportedLocation
    @GET("api/orders/{id}/shipment") suspend fun tracking(@Path("id") id: Long): ShipmentTracking
    @GET("api/orders/{id}/shipment/location") suspend fun trackingLocation(@Path("id") id: Long): ReportedLocation
}
