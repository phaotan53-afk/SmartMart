package com.example.smartmart.data.repository

import com.example.smartmart.data.api.ProductApiService
import com.example.smartmart.data.model.ProductResponse
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus
import com.example.smartmart.domain.repository.ProductRepository
import com.example.smartmart.domain.repository.ProductServiceException
import kotlinx.coroutines.CancellationException

/** SWA-36 — Lê Tuấn Thành. Owns HTTP semantics and validates the API boundary. */
class ProductRepositoryImpl(
    private val api: ProductApiService,
) : ProductRepository {
    override suspend fun getProductById(productId: Long): Product? {
        require(productId > 0) { "Product ID must be positive" }
        try {
            val response = api.getProductById(productId)
            if (response.code() == 404) {
                response.errorBody()?.close()
                return null
            }
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                throw ProductServiceException()
            }
            // Empty 2xx responses are invalid, not a missing product.
            return checkNotNull(response.body()) { "Missing product response body" }
                .toDomain(productId)
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (failure: ProductServiceException) {
            throw failure
        } catch (failure: Exception) {
            // Retrofit, decoding and validation details never become UI messages.
            throw ProductServiceException(failure)
        }
    }
}

internal fun ProductResponse.toDomain(expectedId: Long): Product {
    val validatedId = checkNotNull(id) { "Missing product ID" }
    check(validatedId == expectedId) { "Product ID does not match the request" }
    val validatedName = checkNotNull(name?.trim()?.takeIf(String::isNotEmpty)) {
        "Missing product name"
    }
    val validatedPrice = checkNotNull(price) { "Missing product price" }
    check(validatedPrice.signum() >= 0) { "Negative product price" }
    val status = checkNotNull(stockStatus?.trim()?.takeIf(String::isNotEmpty)) {
        "Missing product stock status"
    }
    return Product(
        id = validatedId,
        name = validatedName,
        price = validatedPrice,
        description = description?.trim()?.takeIf(String::isNotEmpty),
        imageUrl = imageUrl?.trim()?.takeIf(String::isNotEmpty),
        stockStatus = StockStatus.entries.firstOrNull { it.name == status }
            ?: StockStatus.UNAVAILABLE,
        unit = unit?.trim()?.takeIf(String::isNotEmpty),
        categoryId = categoryId,
        categoryName = categoryName,
        brand = brand,
        quantityOnHand = quantityOnHand?.takeIf { it >= 0 },
        averageRating = averageRating?.takeIf { it >= java.math.BigDecimal.ZERO && it <= java.math.BigDecimal(5) },
        reviewCount = reviewCount?.takeIf { it >= 0 },
        images = images.orEmpty().mapNotNull { it.url?.takeIf(String::isNotBlank) },
        specifications = specifications.orEmpty(),
    )
}
