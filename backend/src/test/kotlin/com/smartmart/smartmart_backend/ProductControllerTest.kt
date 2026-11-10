
package com.smartmart.smartmart_backend

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductControllerTest {

    private val jdbcTemplate = mock(JdbcTemplate::class.java)
    private val controller = ProductController(jdbcTemplate)

    @Test
    fun `TC01 - Tra ve danh sach khi co san pham`() {
        // Arrange: Giả lập dữ liệu từ SQL Server
        val products = listOf(
            mapOf<String, Any>(
                "productId" to 1L,
                "productName" to "But bi"
            ),
            mapOf<String, Any>(
                "productId" to 2L,
                "productName" to "Vo hoc sinh"
            )
        )

        Mockito.`when`(
            jdbcTemplate.queryForList(Mockito.anyString())
        ).thenReturn(products)

        // Act: Gọi phương thức cần kiểm thử
        val result = controller.getAllProducts()

        // Assert: Kiểm tra kết quả
        assertEquals(2, result.size)
        assertEquals("But bi", result[0]["productName"])
        assertEquals("Vo hoc sinh", result[1]["productName"])

        Mockito.verify(jdbcTemplate)
            .queryForList(Mockito.anyString())
    }

    @Test
    fun `TC02 - Tra ve danh sach rong khi khong co san pham`() {
        // Arrange
        Mockito.`when`(
            jdbcTemplate.queryForList(Mockito.anyString())
        ).thenReturn(emptyList())

        // Act
        val result = controller.getAllProducts()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `TC03 - Tra ve dung thong tin san pham`() {
        // Arrange
        val products = listOf(
            mapOf<String, Any>(
                "productId" to 1L,
                "categoryId" to 2L,
                "sku" to "SP001",
                "productName" to "But bi",
                "description" to "But bi mau xanh",
                "unit" to "Cay",
                "price" to 5000.0,
                "isActive" to true
            )
        )

        Mockito.`when`(
            jdbcTemplate.queryForList(Mockito.anyString())
        ).thenReturn(products)

        // Act
        val result = controller.getAllProducts()

        // Assert
        assertEquals(1, result.size)
        assertEquals(1L, result[0]["productId"])
        assertEquals("SP001", result[0]["sku"])
        assertEquals("But bi", result[0]["productName"])
        assertEquals(5000.0, result[0]["price"])
        assertEquals(true, result[0]["isActive"])
    }

    @Test
    fun `TC04 - Xu ly khi database phat sinh loi`() {
        // Arrange
        Mockito.`when`(
            jdbcTemplate.queryForList(Mockito.anyString())
        ).thenThrow(
            org.springframework.dao.DataAccessResourceFailureException(
                "Khong the ket noi database"
            )
        )

        // Act & Assert
        kotlin.test.assertFailsWith<
                org.springframework.dao.DataAccessResourceFailureException
                > {
            controller.getAllProducts()
        }
    }
}
