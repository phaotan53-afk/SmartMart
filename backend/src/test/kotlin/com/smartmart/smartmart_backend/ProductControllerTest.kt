package com.smartmart.smartmart_backend

import java.math.BigDecimal
import java.sql.ResultSet
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

/** Tái sử dụng chọn lọc 4 test từ Demo/SmartMart/Code/SmartMart-main.
 * Giữ các assertion nghiệp vụ; chuyển điểm kiểm tra từ Controller sang Repository/Service
 * vì SQL detail đã được tách khỏi Controller trong đợt hợp nhất. Không phải test SQL thật.
 */
class ProductControllerTest {
    @Test
    fun `detail maps SQL values and passes the ID as a bound parameter`() {
        val row=mock(ResultSet::class.java)
        `when`(row.getLong("id")).thenReturn(42L)
        `when`(row.getString("name")).thenReturn("Sữa tươi")
        `when`(row.getBigDecimal("price")).thenReturn(BigDecimal("32000.50"))
        `when`(row.getString("description")).thenReturn("Sữa tươi tiệt trùng")
        `when`(row.getString("imageUrl")).thenReturn("https://example.com/milk.png")
        `when`(row.getString("stockStatus")).thenReturn("IN_STOCK")
        `when`(row.getString("unit")).thenReturn("hộp")
        `when`(row.getString("categoryName")).thenReturn("Sữa")
        var queryExecuted=false
        val jdbc=mock(JdbcTemplate::class.java) { invocation ->
            if(invocation.method.name=="query") {
                queryExecuted=true
                val sql=invocation.getArgument<String>(0)
                assertTrue(sql.contains("WHERE p.ProductID = ?"))
                assertTrue(sql.contains("p.Unit AS unit"))
                assertTrue(sql.replace(Regex("\\s+"),"").contains("ORDERBYpi.IsPrimaryDESC,pi.DisplayOrder,pi.ProductImageID"))
                assertEquals(listOf(42L),(invocation.rawArguments[2] as Array<*>).toList())
                val mapper=invocation.getArgument<RowMapper<ProductSummary>>(1)
                listOf(mapper.mapRow(row,0))
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val response=ProductQueryRepository(jdbc).one(42L)!!
        assertTrue(queryExecuted)
        assertEquals(42L,response.id)
        assertEquals("Sữa tươi",response.name)
        assertEquals(BigDecimal("32000.50"),response.price)
        assertEquals("Sữa tươi tiệt trùng",response.description)
        assertEquals("https://example.com/milk.png",response.imageUrl)
        assertEquals("IN_STOCK",response.stockStatus)
        assertEquals("hộp",response.unit)
    }

    @Test
    fun `detail preserves absent optional description image and unit`() {
        val row=mock(ResultSet::class.java)
        `when`(row.getLong("id")).thenReturn(42L)
        `when`(row.getString("name")).thenReturn("Gạo")
        `when`(row.getBigDecimal("price")).thenReturn(BigDecimal("95000.00"))
        `when`(row.getString("stockStatus")).thenReturn("UNAVAILABLE")
        `when`(row.getString("categoryName")).thenReturn("Thực phẩm")
        val jdbc=mock(JdbcTemplate::class.java) { invocation ->
            if(invocation.method.name=="query") {
                val mapper=invocation.getArgument<RowMapper<ProductSummary>>(1)
                listOf(mapper.mapRow(row,0))
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val response=ProductQueryRepository(jdbc).one(42L)!!
        assertNull(response.description);assertNull(response.imageUrl);assertNull(response.unit)
        assertNull(response.quantityOnHand)
        assertEquals("UNAVAILABLE",response.stockStatus)
    }

    @Test
    fun `detail returns 404 when the database has no matching product`() {
        val jdbc=mock(JdbcTemplate::class.java) { invocation ->
            if(invocation.method.name=="query") emptyList<ProductSummary>()
            else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val error=assertThrows(ApiException::class.java) { CatalogService(ProductQueryRepository(jdbc)).detail(404L) }
        assertEquals(HttpStatus.NOT_FOUND,error.status)
    }

    @Test
    fun `nonpositive IDs return 404 without querying the database`() {
        val jdbc=mock(JdbcTemplate::class.java)
        val service=CatalogService(ProductQueryRepository(jdbc))
        for(id in listOf(0L,-1L,Long.MIN_VALUE)) {
            val error=assertThrows(ApiException::class.java) { service.detail(id) }
            assertEquals(HttpStatus.NOT_FOUND,error.status)
        }
        verifyNoInteractions(jdbc)
    }
}
