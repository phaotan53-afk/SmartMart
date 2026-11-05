package com.example.smartmart.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** Existing SWA-35 collection contract used by the host to open the SWA-36 destination. */
data class CatalogProductResponse(
    @SerializedName("productId") val productId: Long,
    @SerializedName("productName") val productName: String,
    @SerializedName("sku") val sku: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("price") val price: BigDecimal,
    @SerializedName("isActive") val isActive: Boolean,
)
