
package com.example.smartmart.data.model

data class Product(
    val productId: Long,
    val productName: String,
    val sku: String,
    val unit: String,
    val price: Double,
    val isActive: Boolean,
    val categoryId: Long? = null,
    val description: String = ""
)
