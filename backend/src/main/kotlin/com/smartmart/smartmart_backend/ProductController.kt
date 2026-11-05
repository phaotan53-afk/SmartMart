
package com.smartmart.smartmart_backend

import org.springframework.jdbc.core.JdbcTemplate
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val jdbcTemplate: JdbcTemplate,
    private val catalog: CatalogService,
    private val auth: AuthService
) {

    // Lê Tuấn Thành — bổ sung tích hợp: giữ contract detail id/name từ Demo.
    // Nghiệp vụ mới được tách vào service/repository; không gán tác giả backend nhóm cho FE.
    @GetMapping("/{id}")
    fun getProductById(@PathVariable("id") productId: Long) = catalog.detail(productId)

    @GetMapping
    fun getAllProducts(request: HttpServletRequest): List<Map<String, Any?>> {
        // Giữ query và collection fields của nhóm; CostPrice chỉ dành cho ADMIN.
        // Client công khai chuyển sang /api/v1/products với DTO an toàn và phân trang.
        auth.roles(request, "ADMIN")
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
