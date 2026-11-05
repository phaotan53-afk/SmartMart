package com.example.smartmart.master

import com.example.smartmart.domain.model.Product
import java.math.BigDecimal

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI. */
data class CatalogQuery(
    val query: String = "", val categoryId: Long? = null,
    val minPrice: String = "", val maxPrice: String = "", val minRating: Int? = null,
    val inStock: Boolean? = null, val brand: String? = null, val sort: String = "name", val page: Int = 0,
)
data class ProductPage(val items: List<Product> = emptyList(), val page: Int = 0, val totalPages: Int = 0, val totalItems: Long = 0)
data class User(val id: Long, val fullName: String, val email: String, val phone: String? = null, val role: String)
data class Session(val token: String, val user: User)
data class CartItem(
    val productId: Long, val name: String, val price: BigDecimal, val unit: String? = null,
    val imageUrl: String? = null, val quantity: Int, val lineTotal: BigDecimal,
    val stockStatus: String = "UNAVAILABLE", val quantityOnHand: Int? = null,
)
data class Cart(val items: List<CartItem> = emptyList(), val total: BigDecimal = BigDecimal.ZERO, val itemCount: Int = 0)
data class CheckoutQuote(val subtotal: BigDecimal, val discountAmount: BigDecimal, val shippingFee: BigDecimal, val totalAmount: BigDecimal)
data class Address(
    val id: Long = 0, val recipientName: String, val phone: String, val addressLine: String,
    val ward: String? = null, val district: String, val province: String, val isDefault: Boolean = false,
)
data class OrderItem(
    val productId: Long, val productName: String? = null, val name: String? = null,
    val quantity: Int, val unitPrice: BigDecimal? = null, val price: BigDecimal? = null,
    val lineTotal: BigDecimal? = null,
)
data class OrderHistory(val status: String, val note: String?, val changedAt: String)
data class Order(
    val id: Long, val orderCode: String, val orderStatus: String, val paymentStatus: String,
    val subtotal: BigDecimal, val discountAmount: BigDecimal, val shippingFee: BigDecimal,
    val totalAmount: BigDecimal, val createdAt: String, val items: List<OrderItem> = emptyList(),
    val paymentMethod: String? = null, val addressId: Long? = null,
    val addressSnapshot: String? = null,
    val history: List<OrderHistory> = emptyList(),
)
data class Review(val id: Long, val customerName: String, val rating: Int, val comment: String?, val createdAt: String)
data class ReviewPage(
    val items: List<Review> = emptyList(), val averageRating: BigDecimal? = null,
    val reviewCount: Int = 0, val page: Int = 0, val totalPages: Int = 0,
)
data class Category(val id: Long, val name: String, val isActive: Boolean = true)
data class Shipper(val id: Long, val name: String)
data class UserStatus(val id: Long, val fullName: String, val email: String, val phone: String?, val role: String, val isActive: Boolean)
data class ReportedLocation(val latitude: BigDecimal, val longitude: BigDecimal, val recordedAt: String, val realtime: Boolean = false)
data class ShipmentTracking(val id: Long, val status: String, val trackingCode: String?, val shipperName: String?, val assignedAt: String?, val shippedAt: String?, val deliveredAt: String?)
data class Shipment(val id: Long, val orderId: Long, val shipperId: Long?, val status: String, val trackingCode: String?, val orderCode: String,
    val addressSnapshot: String?, val totalAmount: BigDecimal, val paymentStatus: String, val orderStatus: String,
    val assignedAt: String? = null, val shippedAt: String? = null, val deliveredAt: String? = null, val order: Order? = null)
data class Filters(val categories: List<Category> = emptyList(), val brands: List<String> = emptyList())
enum class Destination { HOME, DETAIL, CART, ACCOUNT, CHECKOUT, ORDERS, ORDER_DETAIL, FAVORITES, MANAGEMENT }

data class StoreState(
    val destination: Destination = Destination.HOME, val selectedProductId: Long? = null,
    val selectedOrder: Order? = null, val user: User? = null,
    val catalog: ProductPage = ProductPage(), val query: CatalogQuery = CatalogQuery(),
    val catalogLoading: Boolean = true, val catalogError: String? = null,
    val filters: Filters = Filters(), val cart: Cart = Cart(), val addresses: List<Address> = emptyList(),
    val orders: List<Order> = emptyList(), val favorites: List<Product> = emptyList(),
    val reviews: ReviewPage = ReviewPage(), val related: List<Product> = emptyList(),
    val reviewRating: Int? = null, val busy: Boolean = false, val message: String? = null,
    val pageError: String? = null, val management: ManagementState = ManagementState(),
    val quote: CheckoutQuote? = null, val quoteSignature: String? = null, val quoteLoading: Boolean = false,
    val tracking: ShipmentTracking? = null, val trackingLocation: ReportedLocation? = null,
)
data class ManagementState(
    val products: List<AdminProduct> = emptyList(), val categories: List<Category> = emptyList(),
    val orders: List<Order> = emptyList(), val inventory: List<InventoryItem> = emptyList(),
    val vouchers: List<Voucher> = emptyList(), val report: Map<String, Any?> = emptyMap(),
    val shipments: List<Shipment> = emptyList(), val shippers: List<Shipper> = emptyList(), val users: List<UserStatus> = emptyList(),
)
data class AdminProduct(val id: Long, val categoryId: Long, val sku: String, val name: String, val description: String?, val unit: String, val price: BigDecimal, val costPrice: BigDecimal?, val brand: String?, val isActive: Boolean, val quantityOnHand: Int)
data class InventoryItem(val productId: Long, @com.google.gson.annotations.SerializedName(value = "name", alternate = ["productName"]) val name: String, val quantityOnHand: Int, val reorderLevel: Int? = null, val lowStock: Boolean = false)
data class Voucher(val id: Long, val code: String, val discountType: String, val discountValue: BigDecimal, val isActive: Boolean = true,
    val description: String? = null, val minOrderValue: BigDecimal = BigDecimal.ZERO, val startAt: String? = null, val endAt: String? = null, val usageLimit: Int? = null)
