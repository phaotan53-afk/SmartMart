package com.example.smartmart.master

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smartmart.domain.model.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Phụ trách: Lê Tuấn Thành — FE/Integration theo tài liệu phân công.
 * Chức năng: danh sách → PRODUCT_DETAIL → giỏ → đơn → thanh toán và đánh giá.
 * Nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * Vai trò: điều phối state và repository; mọi quyền sở hữu được backend xác minh lại.
 */
class StoreViewModel(
    private val repository: StoreRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val initialQuery = CatalogQuery(
        query = saved["query"] ?: "", categoryId = saved["category"],
        minPrice = saved["minPrice"] ?: "", maxPrice = saved["maxPrice"] ?: "",
        minRating = saved["minRating"], inStock = saved["inStock"], brand = saved["brand"],
        sort = saved["sort"] ?: "name", page = saved["page"] ?: 0,
    )
    private val mutable = MutableStateFlow(StoreState(
        query = initialQuery,
        destination = saved.get<String>("destination")?.let { runCatching { Destination.valueOf(it) }.getOrNull() }
            ?.takeIf { it == Destination.HOME || it == Destination.DETAIL } ?: Destination.HOME,
        selectedProductId = saved["productId"],
    ))
    val state = mutable.asStateFlow()
    private var catalogJob: Job? = null
    private var detailExtrasJob: Job? = null
    private var mutation: Job? = null
    private var quoteJob: Job? = null

    init {
        loadCatalog()
        viewModelScope.launch {
            try { val filters = repository.filters(); mutable.update { it.copy(filters = filters) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: StoreFailure) { /* Catalog remains usable; retry reloads metadata. */ }
        }
        state.value.selectedProductId?.let(::loadDetailExtras)
    }

    fun setQuery(query: CatalogQuery) {
        saved["query"] = query.query; saved["category"] = query.categoryId
        saved["minPrice"] = query.minPrice; saved["maxPrice"] = query.maxPrice
        saved["minRating"] = query.minRating; saved["inStock"] = query.inStock
        saved["brand"] = query.brand; saved["sort"] = query.sort; saved["page"] = query.page
        mutable.update { it.copy(query = query) }; loadCatalog()
    }
    fun retryCatalog() { loadCatalog(); viewModelScope.launch {
        try { val filters = repository.filters(); mutable.update { it.copy(filters = filters) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: StoreFailure) { }
    } }
    private fun loadCatalog() {
        catalogJob?.cancel()
        val query = state.value.query
        catalogJob = viewModelScope.launch {
            mutable.update { it.copy(catalogLoading = true, catalogError = null) }
            try {
                val page = repository.catalog(query); ensureActive()
                mutable.update { it.copy(catalog = page, catalogLoading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: StoreFailure) { ensureActive(); mutable.update { it.copy(catalogLoading = false, catalogError = failure.userMessage) } }
        }
    }

    fun navigate(destination: Destination) {
        if (destination in setOf(Destination.CART, Destination.CHECKOUT, Destination.ORDERS, Destination.FAVORITES, Destination.MANAGEMENT) && state.value.user == null) {
            setDestination(Destination.ACCOUNT); mutable.update { it.copy(message = "Vui lòng đăng nhập để tiếp tục.") }; return
        }
        setDestination(destination)
        when (destination) {
            Destination.CART -> runAction { val cart = repository.cart(); mutable.update { it.copy(cart = cart) } }
            Destination.CHECKOUT -> runAction { val addresses = repository.addresses(); val cart = repository.cart(); mutable.update { it.copy(addresses = addresses, cart = cart, quote = null, quoteSignature = null) } }
            Destination.ORDERS -> runAction { val orders = repository.orders(); mutable.update { it.copy(orders = orders) } }
            Destination.FAVORITES -> runAction { val favorites = repository.favorites(); mutable.update { it.copy(favorites = favorites) } }
            Destination.ACCOUNT -> if (state.value.user?.role == "CUSTOMER") runAction { val addresses = repository.addresses(); mutable.update { it.copy(addresses = addresses) } }
            Destination.MANAGEMENT -> refreshManagement()
            else -> Unit
        }
    }
    private fun setDestination(destination: Destination) {
        saved["destination"] = destination.name
        mutable.update { it.copy(destination = destination, pageError = null) }
    }
    fun back() { navigate(when (state.value.destination) {
        Destination.ORDER_DETAIL -> Destination.ORDERS
        Destination.CHECKOUT -> Destination.CART
        else -> Destination.HOME
    }) }
    fun openProduct(id: Long) {
        saved["productId"] = id
        mutable.update { it.copy(selectedProductId = id, reviews = ReviewPage(), reviewRating = null, related = emptyList()) }
        setDestination(Destination.DETAIL); loadDetailExtras(id)
    }
    private fun loadDetailExtras(id: Long) {
        detailExtrasJob?.cancel()
        detailExtrasJob = viewModelScope.launch {
            try {
                val reviews = repository.reviews(id, null, 0); ensureActive()
                mutable.update { if (it.selectedProductId == id) it.copy(reviews = reviews) else it }
                val product = state.value.catalog.items.firstOrNull { it.id == id }
                val related = repository.catalog(CatalogQuery(categoryId = product?.categoryId)).items.filter { it.id != id }.take(4)
                ensureActive(); mutable.update { if (it.selectedProductId == id) it.copy(related = related) else it }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: StoreFailure) { /* Optional panels never replace the validated product/error state. */ }
        }
    }
    fun loadReviews(rating: Int?, page: Int = 0) {
        val id = state.value.selectedProductId ?: return
        runAction {
            val reviews = repository.reviews(id, rating, page)
            mutable.update { if (it.selectedProductId == id) it.copy(reviews = reviews, reviewRating = rating) else it }
        }
    }

    fun authenticate(register: Boolean, name: String, email: String, password: String, phone: String) = runAction {
        if (email.isBlank() || password.isBlank() || (register && name.isBlank())) throw StoreFailure("Vui lòng nhập đủ thông tin bắt buộc.", "VALIDATION")
        val user = if (register) repository.register(name, email, password, phone.takeIf(String::isNotBlank)) else repository.login(email, password)
        mutable.update { it.copy(user = user, message = if (register) "Đăng ký thành công." else "Đăng nhập thành công.") }
        if (user.role == "CUSTOMER") {
            val cart = repository.cart(); val favorites = repository.favorites()
            mutable.update { it.copy(cart = cart, favorites = favorites) }
        }
        setDestination(if (user.role == "CUSTOMER") Destination.HOME else Destination.MANAGEMENT)
        if (user.role != "CUSTOMER") { val management = repository.management(user.role); mutable.update { it.copy(management = management) } }
    }
    fun logout() = runAction {
        try { repository.logout() } finally {
            mutable.update { StoreState(catalog = it.catalog, query = it.query, filters = it.filters, catalogLoading = false, message = "Đã đăng xuất.") }
            saved["destination"] = Destination.HOME.name
        }
    }
    fun updateProfile(name: String, phone: String, email: String) = runAction {
        val user = repository.profile(name, phone.takeIf(String::isNotBlank), email); mutable.update { it.copy(user = user, message = "Đã cập nhật hồ sơ.") }
    }
    fun addToCart(product: Product, quantity: Int) {
        if (state.value.user == null) { navigate(Destination.ACCOUNT); mutable.update { it.copy(message = "Đăng nhập để thêm sản phẩm vào giỏ.") }; return }
        runAction {
            val cart = repository.add(product.id, quantity)
            mutable.update { it.copy(cart = cart, message = "Đã lưu $quantity ${product.unit.orEmpty()} ${product.name} vào giỏ.") }
        }
    }
    fun changeQuantity(id: Long, quantity: Int) = runAction {
        val cart = repository.quantity(id, quantity); mutable.update { it.copy(cart = cart) }
    }
    fun removeCartItem(id: Long) = runAction { val cart = repository.remove(id); mutable.update { it.copy(cart = cart, message = "Đã xóa sản phẩm khỏi giỏ.") } }
    fun saveAddress(address: Address) = runAction {
        repository.saveAddress(address); val addresses = repository.addresses()
        mutable.update { it.copy(addresses = addresses, message = "Đã lưu địa chỉ nhận hàng.") }
    }
    fun removeAddress(id: Long) = runAction { repository.removeAddress(id); val addresses = repository.addresses(); mutable.update { it.copy(addresses = addresses) } }
    fun quote(addressId: Long?, voucher: String) {
        quoteJob?.cancel()
        mutable.update { it.copy(quote = null, quoteSignature = null, quoteLoading = addressId != null) }
        if (addressId == null) return
        val signature = quoteSignature(addressId, voucher)
        quoteJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            try {
                val quote = repository.quote(CheckoutRequest(addressId, "COD", voucher.trim().takeIf(String::isNotBlank), null))
                ensureActive(); mutable.update { it.copy(quote = quote, quoteSignature = signature, quoteLoading = false, pageError = null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: StoreFailure) { ensureActive(); mutable.update { it.copy(quoteLoading = false, pageError = failure.userMessage) } }
        }
    }
    fun checkout(addressId: Long, method: String, voucher: String, note: String) = runAction {
        val signature = "$addressId|$method|${voucher.trim()}|${note.trim()}"
        if (saved.get<String>("checkoutSignature") != signature) { saved["checkoutKey"] = UUID.randomUUID().toString(); saved["checkoutSignature"] = signature }
        val key = saved.get<String>("checkoutKey") ?: UUID.randomUUID().toString().also { saved["checkoutKey"] = it }
        val order = repository.checkout(key, CheckoutRequest(addressId, method, voucher.trim().takeIf(String::isNotBlank), note.trim().takeIf(String::isNotBlank)))
        saved.remove<String>("checkoutKey"); saved.remove<String>("checkoutSignature")
        mutable.update { it.copy(selectedOrder = order, cart = Cart(), quote = null, quoteSignature = null, message = "Đã tạo đơn ${order.orderCode}. Thanh toán: ${statusLabel(order.paymentStatus)}.") }
        setDestination(Destination.ORDER_DETAIL)
    }
    fun openOrder(id: Long) = runAction {
        val order = repository.order(id); mutable.update { it.copy(selectedOrder = order, tracking = null, trackingLocation = null) }; setDestination(Destination.ORDER_DETAIL)
        if (state.value.user?.role == "CUSTOMER") try { val tracking = repository.tracking(id); mutable.update { it.copy(tracking = tracking) } } catch (_: StoreFailure) { }
    }
    fun openShipment(id: Long) = runAction {
        val shipment = repository.shipment(id)
        mutable.update { it.copy(management = it.management.copy(shipments = it.management.shipments.map { current -> if (current.id == id) shipment else current })) }
    }
    fun trackLocation(id: Long) = runAction {
        val location = repository.trackingLocation(id)
        mutable.update { it.copy(trackingLocation = location, message = "Đây là vị trí shipper đã báo, không phải dữ liệu theo dõi thời gian thực.") }
    }
    fun cancelOrder(id: Long) = runAction { val order = repository.cancel(id); mutable.update { it.copy(selectedOrder = order, message = "Đã hủy đơn hàng.") } }
    fun review(id: Long, orderId: Long, rating: Int, comment: String) = runAction {
        repository.review(id, orderId, rating, comment.trim().takeIf(String::isNotBlank))
        val reviews = repository.reviews(id, null, 0)
        // A completed write for a previous product must not replace the new product's review panel.
        mutable.update { if (it.selectedProductId == id) it.copy(reviews = reviews, reviewRating = null, message = "Đã lưu đánh giá. Cảm ơn bạn!") else it.copy(message = "Đã lưu đánh giá. Cảm ơn bạn!") }; loadCatalog()
    }
    fun toggleFavorite(id: Long) {
        if (state.value.user == null) { navigate(Destination.ACCOUNT); return }
        val enabled = state.value.favorites.none { it.id == id }
        runAction { repository.favorite(id, enabled); val favorites = repository.favorites(); mutable.update { it.copy(favorites = favorites, message = if (enabled) "Đã thêm vào yêu thích." else "Đã bỏ yêu thích.") } }
    }
    fun refreshManagement() = runAction {
        val role = state.value.user?.role ?: return@runAction
        val management = repository.management(role); mutable.update { it.copy(management = management) }
    }
    fun adminAction(action: String, id: Long?, body: Map<String, Any?>) = runAction {
        val signature = "$action|$id|$body"
        val isStockMutation = action in setOf("inventory", "count")
        if (isStockMutation && saved.get<String>("stockMutationSignature") != signature) {
            saved["stockMutationSignature"] = signature; saved["stockMutationKey"] = UUID.randomUUID().toString()
        }
        val request = if (isStockMutation) body + ("_idempotencyKey" to saved.get<String>("stockMutationKey")) else body
        repository.adminAction(action, id, request)
        if (isStockMutation) { saved.remove<String>("stockMutationSignature"); saved.remove<String>("stockMutationKey") }
        val management = repository.management(state.value.user?.role.orEmpty())
        mutable.update { it.copy(management = management, message = "Đã lưu thay đổi.") }; loadCatalog()
    }
    fun searchUsers(query: String) = runAction { val users = repository.users(query); mutable.update { it.copy(management = it.management.copy(users = users)) } }
    fun reportRange(from: String, to: String) = runAction {
        if (runCatching { java.time.LocalDate.parse(from); java.time.LocalDate.parse(to) }.isFailure) throw StoreFailure("Ngày cần có dạng yyyy-MM-dd.", "VALIDATION")
        val report = repository.report(from, to); mutable.update { it.copy(management = it.management.copy(report = report)) }
    }
    fun messageConsumed() { mutable.update { it.copy(message = null) } }
    private fun runAction(block: suspend () -> Unit) {
        if (mutation?.isActive == true) return
        mutation = viewModelScope.launch {
            mutable.update { it.copy(busy = true, pageError = null) }
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: StoreFailure) { mutable.update { it.copy(message = failure.userMessage, pageError = failure.userMessage) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    companion object { val Factory = viewModelFactory { initializer { StoreViewModel(StoreGraph.repository, createSavedStateHandle()) } } }
}

fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "Chờ xác nhận"; "CONFIRMED" -> "Đã xác nhận"; "PROCESSING" -> "Đang xử lý"
    "PREPARING" -> "Đang chuẩn bị"; "SHIPPING", "IN_TRANSIT" -> "Đang giao"; "DELIVERED" -> "Đã giao"; "CANCELLED" -> "Đã hủy"
    "ASSIGNED" -> "Đã phân công"; "PICKED_UP" -> "Đã nhận đơn giao"; "FAILED" -> "Giao không thành công"
    "UNPAID" -> "Chưa thanh toán"; "PAID" -> "Đã thanh toán"; "REFUNDED" -> "Đã hoàn tiền"
    else -> status
}
internal fun quoteSignature(addressId: Long, voucher: String) = "$addressId|COD|${voucher.trim().uppercase()}"
