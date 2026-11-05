package com.example.smartmart.data.api

import com.example.smartmart.data.model.ProductResponse
import com.example.smartmart.data.model.CatalogProductResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductApiService {
    @GET("api/products")
    suspend fun getProducts(): List<CatalogProductResponse>

    @GET("api/products/{id}")
    suspend fun getProductById(@Path("id") productId: Long): Response<ProductResponse>
}
