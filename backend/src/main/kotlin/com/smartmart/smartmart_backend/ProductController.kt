
package com.smartmart.smartmart_backend

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val jdbcTemplate: JdbcTemplate
) {

    @GetMapping
    fun getAllProducts(): List<Map<String, Any?>> {
        val sql = """
            SELECT
                ProductID AS productId,
                CategoryID AS categoryId,
                SKU AS sku,
                ProductName AS productName,
                Description AS description,
                Unit AS unit,
                Price AS price,
                CostPrice AS costPrice,
                IsActive AS isActive
            FROM dbo.Products
            ORDER BY ProductID
        """.trimIndent()

        return jdbcTemplate.queryForList(sql)
    }
}
