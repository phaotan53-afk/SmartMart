package com.example.smartmart.domain.repository

import com.example.smartmart.domain.model.Product

/** SWA-36 — Lê Tuấn Thành. UI and tests depend on this contract, never on Retrofit. */
interface ProductRepository {
    /**
     * Returns null only when the requested product does not exist.
     * Throws [ProductServiceException] for transport or invalid response failures.
     * Coroutine cancellation propagates unchanged.
     */
    suspend fun getProductById(productId: Long): Product?
}
