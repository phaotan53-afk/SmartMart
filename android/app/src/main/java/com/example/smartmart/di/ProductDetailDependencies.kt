package com.example.smartmart.di

import com.example.smartmart.data.api.ProductApiClient
import com.example.smartmart.data.repository.ProductRepositoryImpl
import com.example.smartmart.domain.repository.ProductRepository

/** Create once in the application's composition root and share across detail destinations. */
class ProductDetailDependencies(baseUrl: String = ProductApiClient.EMULATOR_BASE_URL) {
    val api = ProductApiClient.create(baseUrl)
    val repository: ProductRepository = ProductRepositoryImpl(api)
}
