package com.smartmart.smartmart_backend

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.sql.ResultSet

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * DTO công khai không chứa CostPrice; id/name ở detail được giữ tương thích Demo.
 */
data class ProductSummary(
    val id: Long, val name: String, val price: BigDecimal, val description: String?, val unit: String?,
    val imageUrl: String?, val stockStatus: String, val categoryId: Int, val categoryName: String,
    val brand: String?, val quantityOnHand: Int?, val averageRating: BigDecimal?, val reviewCount: Long
)
data class ProductImageResponse(val url: String, val altText: String?)
data class ProductDetailResponse(
    val id: Long, val name: String, val price: BigDecimal, val description: String?, val imageUrl: String?,
    val stockStatus: String, val unit: String? = null, val categoryId: Int = 0, val categoryName: String = "",
    val brand: String? = null, val quantityOnHand: Int? = null, val averageRating: BigDecimal? = null, val reviewCount: Long = 0,
    val images: List<ProductImageResponse> = emptyList(), val specifications: Map<String, String> = emptyMap(),
    val relatedProducts: List<ProductSummary> = emptyList(), val soldCount: Long = 0
)
data class CatalogPage(val items: List<ProductSummary>, val page: Int, val size: Int, val totalItems: Long, val totalPages: Int)
data class CatalogFilter(val query: String?, val categoryId: Int?, val minPrice: BigDecimal?, val maxPrice: BigDecimal?,
    val minRating: BigDecimal?, val inStock: Boolean?, val brand: String?, val sort: String, val page: Int, val size: Int) {
    fun validate() {
        if (page !in 0..100_000 || size !in 1..100 || query.orEmpty().length > 100 || brand.orEmpty().length > 100) badRequest("Điều kiện phân trang hoặc tìm kiếm không hợp lệ")
        if (categoryId != null && categoryId <= 0) badRequest("Danh mục không hợp lệ")
        if (minPrice != null && minPrice < BigDecimal.ZERO || maxPrice != null && maxPrice < BigDecimal.ZERO || minPrice != null && maxPrice != null && minPrice > maxPrice) badRequest("Khoảng giá không hợp lệ")
        if (minRating != null && (minRating < BigDecimal.ZERO || minRating > BigDecimal(5))) badRequest("Điểm đánh giá phải từ 0 đến 5")
        if (sort !in SORTS) badRequest("Cách sắp xếp không hợp lệ")
    }
    companion object { val SORTS = mapOf("newest" to "p.ProductID DESC", "price_asc" to "p.Price,p.ProductID", "price_desc" to "p.Price DESC,p.ProductID", "name" to "p.ProductName,p.ProductID", "rating" to "rating.averageRating DESC,p.ProductID") }
}

@Repository
class ProductQueryRepository(private val jdbc: JdbcTemplate) {
    // Selective reuse: Demo's parameterized detail query and deterministic primary-image ordering.
    private val joins = """
        FROM dbo.Products p JOIN dbo.Categories c ON c.CategoryID=p.CategoryID
        LEFT JOIN dbo.Inventory inventory ON inventory.ProductID=p.ProductID
        OUTER APPLY (SELECT TOP(1) pi.ImageUrl FROM dbo.ProductImages pi WHERE pi.ProductID=p.ProductID
            ORDER BY pi.IsPrimary DESC,pi.DisplayOrder,pi.ProductImageID) image
        OUTER APPLY (SELECT CAST(AVG(CAST(r.Rating AS DECIMAL(4,2))) AS DECIMAL(4,2)) AS averageRating,COUNT_BIG(*) AS reviewCount
            FROM dbo.Reviews r WHERE r.ProductID=p.ProductID) rating
    """.trimIndent()
    private val columns = """
        SELECT p.ProductID AS id,p.ProductName AS name,p.Price AS price,p.Unit AS unit,p.Description AS description,
        image.ImageUrl AS imageUrl,CASE WHEN p.IsActive=0 OR inventory.QuantityOnHand IS NULL THEN 'UNAVAILABLE'
        WHEN inventory.QuantityOnHand>0 THEN 'IN_STOCK' ELSE 'OUT_OF_STOCK' END AS stockStatus,
        p.CategoryID AS categoryId,c.CategoryName AS categoryName,p.Brand AS brand,inventory.QuantityOnHand AS quantityOnHand,
        rating.averageRating AS averageRating,rating.reviewCount AS reviewCount
    """.trimIndent()
    private fun map(row: ResultSet, ignored: Int) = ProductSummary(row.getLong("id"), row.getString("name"), row.getBigDecimal("price"), row.getString("description"),
        row.getString("unit"), row.getString("imageUrl"), row.getString("stockStatus"), row.getInt("categoryId"), row.getString("categoryName"), row.getString("brand"),
        (row.getObject("quantityOnHand") as Number?)?.toInt(), row.getBigDecimal("averageRating"), row.getLong("reviewCount"))

    fun one(productId: Long): ProductSummary? = jdbc.query("$columns $joins WHERE p.ProductID = ?", ::map, productId).firstOrNull()

    fun page(filter: CatalogFilter): CatalogPage {
        filter.validate()
        val conditions = mutableListOf("p.IsActive=1", "c.IsActive=1")
        val args = mutableListOf<Any>()
        filter.query?.trim()?.takeIf(String::isNotBlank)?.let {
            conditions += "(p.ProductName LIKE ? ESCAPE '\\' OR p.Description LIKE ? ESCAPE '\\' OR p.SKU LIKE ? ESCAPE '\\')"
            val like = "%" + it.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[") + "%"
            args.addAll(listOf(like, like, like))
        }
        filter.categoryId?.let { conditions += "p.CategoryID=?"; args += it }
        filter.minPrice?.let { conditions += "p.Price>=?"; args += it }
        filter.maxPrice?.let { conditions += "p.Price<=?"; args += it }
        filter.minRating?.let { conditions += "rating.averageRating>=?"; args += it }
        filter.brand?.trim()?.takeIf(String::isNotBlank)?.let { conditions += "p.Brand=?"; args += it }
        filter.inStock?.let { conditions += if (it) "inventory.QuantityOnHand>0" else "inventory.QuantityOnHand=0" }
        val where = " WHERE " + conditions.joinToString(" AND ")
        val total = jdbc.queryForObject("SELECT COUNT_BIG(*) $joins $where", Long::class.java, *args.toTypedArray())!!
        val items = jdbc.query("$columns $joins $where ORDER BY ${CatalogFilter.SORTS.getValue(filter.sort)} OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", ::map, *(args + listOf(filter.page * filter.size, filter.size)).toTypedArray())
        return CatalogPage(items, filter.page, filter.size, total, ((total + filter.size - 1) / filter.size).toInt())
    }
    fun images(id: Long): List<ProductImageResponse> = jdbc.query("SELECT ImageUrl,AltText FROM dbo.ProductImages WHERE ProductID=? ORDER BY IsPrimary DESC,DisplayOrder,ProductImageID", { row, _ -> ProductImageResponse(row.getString(1), row.getString(2)) }, id)
    fun specifications(id: Long): Map<String, String> = jdbc.queryForList("SELECT SpecificationName AS name,SpecificationValue AS value FROM dbo.ProductSpecifications WHERE ProductID=? ORDER BY SpecificationName", id).associate { it["name"] as String to it["value"] as String }
    fun sold(id: Long): Long = jdbc.queryForObject("SELECT COALESCE(SUM(CAST(oi.Quantity AS BIGINT)),0) FROM dbo.OrderItems oi JOIN dbo.Orders o ON o.OrderID=oi.OrderID WHERE oi.ProductID=? AND o.OrderStatus='DELIVERED'", Long::class.java, id)!!
    fun categories() = jdbc.queryForList("SELECT CategoryID AS id,CategoryName AS name,Description AS description FROM dbo.Categories WHERE IsActive=1 ORDER BY CategoryName")
    fun brands() = jdbc.queryForList("SELECT DISTINCT Brand FROM dbo.Products WHERE IsActive=1 AND Brand IS NOT NULL ORDER BY Brand", String::class.java)
}

@Service
class CatalogService(private val repository: ProductQueryRepository) {
    fun page(filter: CatalogFilter) = repository.page(filter)
    fun detail(id: Long): ProductDetailResponse {
        if (id <= 0) notFound("Không tìm thấy sản phẩm")
        val item = repository.one(id) ?: notFound("Không tìm thấy sản phẩm")
        val related = repository.page(CatalogFilter(null, item.categoryId, null, null, null, null, null, "newest", 0, 5)).items.filter { it.id != id }.take(4)
        return ProductDetailResponse(item.id,item.name,item.price,item.description,item.imageUrl,item.stockStatus,item.unit,item.categoryId,item.categoryName,item.brand,item.quantityOnHand,item.averageRating,item.reviewCount,repository.images(id),repository.specifications(id),related,repository.sold(id))
    }
    fun summary(id: Long) = repository.one(id) ?: notFound("Không tìm thấy sản phẩm")
    fun filters() = mapOf("categories" to repository.categories(), "brands" to repository.brands())
    fun categories() = repository.categories()
}

@RestController
class CatalogController(private val catalog: CatalogService) {
    @GetMapping("/api/v1/products")
    fun list(@RequestParam(required=false) query: String?, @RequestParam(required=false) categoryId: Int?,
        @RequestParam(required=false) minPrice: BigDecimal?, @RequestParam(required=false) maxPrice: BigDecimal?,
        @RequestParam(required=false) minRating: BigDecimal?, @RequestParam(required=false) inStock: Boolean?, @RequestParam(required=false) brand: String?,
        @RequestParam(defaultValue="newest") sort: String, @RequestParam(defaultValue="0") page: Int, @RequestParam(defaultValue="12") size: Int) =
        catalog.page(CatalogFilter(query,categoryId,minPrice,maxPrice,minRating,inStock,brand,sort,page,size))
    @GetMapping("/api/categories") fun categories() = catalog.categories()
    @GetMapping("/api/catalog/filters") fun filters() = catalog.filters()
}
