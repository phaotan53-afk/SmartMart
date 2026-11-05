package com.example.smartmart.domain.model

import java.math.BigDecimal

data class Product(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val description: String?,
    val imageUrl: String?,
    val stockStatus: StockStatus,
    /** Optional for older API servers. Never infer a sales unit from a product name. */
    val unit: String? = null,
    // Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val brand: String? = null,
    val quantityOnHand: Int? = null,
    val averageRating: BigDecimal? = null,
    val reviewCount: Int? = null,
    val images: List<String> = emptyList(),
    val specifications: Map<String, String> = emptyMap(),
)

enum class StockStatus {
    IN_STOCK,
    OUT_OF_STOCK,
    UNAVAILABLE,
}
