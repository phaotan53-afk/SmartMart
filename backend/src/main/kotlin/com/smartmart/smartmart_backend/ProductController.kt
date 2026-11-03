
package com.smartmart.smartmart_backend

import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val jdbcTemplate: JdbcTemplate
) {

    // Các trường được phép trả về cho API công khai.
    // Không trả về CostPrice (giá vốn nội bộ).
    private val productColumns = """
        ProductID AS productId,
        CategoryID AS categoryId,
        SKU AS sku,
        ProductName AS productName,
        Description AS description,
        Unit AS unit,
        Price AS price,
        IsActive AS isActive
    """.trimIndent()

    // SWA-35: Danh sách sản phẩm
    @GetMapping
    fun getAllProducts(): List<Map<String, Any?>> {
        val sql = """
            SELECT $productColumns
            FROM dbo.Products
            ORDER BY ProductID
        """.trimIndent()

        return jdbcTemplate.queryForList(sql)
    }

    // SWA-17: Tìm kiếm, lọc và phân trang
    @GetMapping("/search")
    fun searchProducts(
        @RequestParam(required = false) tuKhoa: String?,
        @RequestParam(required = false) maDanhMuc: Long?,
        @RequestParam(required = false) giaToiThieu: Double?,
        @RequestParam(required = false) giaToiDa: Double?,
        @RequestParam(defaultValue = "0") trang: Int,
        @RequestParam(defaultValue = "10") kichThuoc: Int
    ): ResponseEntity<Any> {

        if (trang < 0 || kichThuoc !in 1..100) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "message" to "Tham số phân trang không hợp lệ"
                )
            )
        }

        if (
            (giaToiThieu != null &&
                    (!giaToiThieu.isFinite() || giaToiThieu < 0)) ||
            (giaToiDa != null &&
                    (!giaToiDa.isFinite() || giaToiDa < 0)) ||
            (giaToiThieu != null &&
                    giaToiDa != null &&
                    giaToiThieu > giaToiDa)
        ) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "message" to "Khoảng giá không hợp lệ"
                )
            )
        }

        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (!tuKhoa.isNullOrBlank()) {
            // Escape các ký tự đặc biệt của SQL LIKE.
            val escapedKeyword = tuKhoa.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("[", "\\[")

            conditions.add(
                "(ProductName LIKE ? ESCAPE '\\' " +
                        "OR SKU LIKE ? ESCAPE '\\')"
            )

            val keyword = "%$escapedKeyword%"
            params.add(keyword)
            params.add(keyword)
        }

        if (maDanhMuc != null) {
            conditions.add("CategoryID = ?")
            params.add(maDanhMuc)
        }

        if (giaToiThieu != null) {
            conditions.add("Price >= ?")
            params.add(giaToiThieu)
        }

        if (giaToiDa != null) {
            conditions.add("Price <= ?")
            params.add(giaToiDa)
        }

        val whereClause = if (conditions.isEmpty()) {
            ""
        } else {
            "WHERE " + conditions.joinToString(" AND ")
        }

        val countSql = """
            SELECT COUNT(*)
            FROM dbo.Products
            $whereClause
        """.trimIndent()

        val totalElements = jdbcTemplate.queryForObject(
            countSql,
            Long::class.java,
            *params.toTypedArray()
        ) ?: 0L

        val offset = trang.toLong() * kichThuoc

        val searchSql = """
            SELECT $productColumns
            FROM dbo.Products
            $whereClause
            ORDER BY ProductID
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
        """.trimIndent()

        val queryParams = params.toMutableList()
        queryParams.add(offset)
        queryParams.add(kichThuoc)

        val products = jdbcTemplate.queryForList(
            searchSql,
            *queryParams.toTypedArray()
        )

        val totalPages = if (totalElements == 0L) {
            0L
        } else {
            (totalElements - 1) / kichThuoc + 1
        }

        val result = mapOf(
            "content" to products,
            "totalElements" to totalElements,
            "totalPages" to totalPages,
            "number" to trang,
            "size" to kichThuoc,
            "numberOfElements" to products.size,
            "first" to (trang == 0),
            "last" to (trang.toLong() + 1 >= totalPages),
            "empty" to products.isEmpty()
        )

        return ResponseEntity.ok(result)
    }

    // SWA-36: Chi tiết sản phẩm
    @GetMapping("/{id}")
    fun getProductById(
        @PathVariable id: Long
    ): ResponseEntity<Map<String, Any?>> {

        val sql = """
            SELECT $productColumns
            FROM dbo.Products
            WHERE ProductID = ?
        """.trimIndent()

        val products = jdbcTemplate.queryForList(sql, id)

        return if (products.isEmpty()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(products.first())
        }
    }
}
