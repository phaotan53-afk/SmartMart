package com.example.smartmart.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

// Nullable wire fields let the repository validate incomplete JSON before it reaches the UI.
data class ProductResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("price") val price: BigDecimal? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("stockStatus") val stockStatus: String? = null,
    @SerializedName("unit") val unit: String? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val brand: String? = null,
    val quantityOnHand: Int? = null,
    val averageRating: BigDecimal? = null,
    val reviewCount: Int? = null,
    val images: List<ProductImageResponse>? = null,
    val specifications: Map<String, String>? = null,
)

data class ProductImageResponse(val url: String? = null, val altText: String? = null)
