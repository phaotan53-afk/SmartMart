package com.example.smartmart.master

import com.example.smartmart.domain.model.Product

/** Native Compose UI emits intent only; ViewModel executes all repository work. */
sealed interface StoreEvent {
    data class Navigate(val destination: Destination) : StoreEvent
    data object Back : StoreEvent
    data object Retry : StoreEvent
    data class Query(val query: CatalogQuery) : StoreEvent
    data class OpenProduct(val id: Long) : StoreEvent
    data class Add(val product: Product, val quantity: Int) : StoreEvent
    data class Quantity(val id: Long, val quantity: Int) : StoreEvent
    data class Remove(val id: Long) : StoreEvent
    data class Auth(val register: Boolean, val name: String, val email: String, val password: String, val phone: String) : StoreEvent
    data object Logout : StoreEvent
    data class Profile(val name: String, val phone: String, val email: String) : StoreEvent
    data class SaveAddress(val address: Address) : StoreEvent
    data class RemoveAddress(val id: Long) : StoreEvent
    data class Checkout(val addressId: Long, val method: String, val voucher: String, val note: String) : StoreEvent
    data class Quote(val addressId: Long?, val voucher: String) : StoreEvent
    data class TrackLocation(val orderId: Long) : StoreEvent
    data class OpenOrder(val id: Long) : StoreEvent
    data class OpenShipment(val id: Long) : StoreEvent
    data class CancelOrder(val id: Long) : StoreEvent
    data class Reviews(val rating: Int?, val page: Int = 0) : StoreEvent
    data class ReviewProduct(val id: Long, val orderId: Long, val rating: Int, val comment: String) : StoreEvent
    data class Favorite(val id: Long) : StoreEvent
    data object RefreshManagement : StoreEvent
    data class UserSearch(val query: String) : StoreEvent
    data class ReportRange(val from: String, val to: String) : StoreEvent
    data class Admin(val action: String, val id: Long?, val body: Map<String, Any?>) : StoreEvent
}

fun StoreViewModel.dispatch(event: StoreEvent) {
    when (event) {
        is StoreEvent.Navigate -> navigate(event.destination)
        StoreEvent.Back -> back(); StoreEvent.Retry -> retryCatalog()
        is StoreEvent.Query -> setQuery(event.query)
        is StoreEvent.OpenProduct -> openProduct(event.id)
        is StoreEvent.Add -> addToCart(event.product, event.quantity)
        is StoreEvent.Quantity -> changeQuantity(event.id, event.quantity)
        is StoreEvent.Remove -> removeCartItem(event.id)
        is StoreEvent.Auth -> authenticate(event.register, event.name, event.email, event.password, event.phone)
        StoreEvent.Logout -> logout()
        is StoreEvent.Profile -> updateProfile(event.name, event.phone, event.email)
        is StoreEvent.SaveAddress -> saveAddress(event.address)
        is StoreEvent.RemoveAddress -> removeAddress(event.id)
        is StoreEvent.Checkout -> checkout(event.addressId, event.method, event.voucher, event.note)
        is StoreEvent.Quote -> quote(event.addressId, event.voucher)
        is StoreEvent.TrackLocation -> trackLocation(event.orderId)
        is StoreEvent.OpenOrder -> openOrder(event.id)
        is StoreEvent.OpenShipment -> openShipment(event.id)
        is StoreEvent.CancelOrder -> cancelOrder(event.id)
        is StoreEvent.Reviews -> loadReviews(event.rating, event.page)
        is StoreEvent.ReviewProduct -> review(event.id, event.orderId, event.rating, event.comment)
        is StoreEvent.Favorite -> toggleFavorite(event.id)
        StoreEvent.RefreshManagement -> refreshManagement()
        is StoreEvent.UserSearch -> searchUsers(event.query)
        is StoreEvent.ReportRange -> reportRange(event.from, event.to)
        is StoreEvent.Admin -> adminAction(event.action, event.id, event.body)
    }
}
