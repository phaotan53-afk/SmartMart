
package com.example.smartmart.data.repository

import com.example.smartmart.data.api.ProductApi
import com.example.smartmart.data.model.Product

class ProductRepository {

    suspend fun getProducts(): List<Product> {
        return ProductApi.getProducts()
    }
}
