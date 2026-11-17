package com.smartmart.smartmart_backend

import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import kotlin.test.assertTrue

/**
 * API tests cho backend (KHÔNG cần SQL Server: JdbcTemplate được giả lập).
 * Đặt tại: backend/src/test/kotlin/com/smartmart/smartmart_backend/
 * Chạy:    .\gradlew.bat test --tests "*ProductControllerTest"
 *
 * Nếu build báo không tìm thấy import (Spring Boot 4 đổi tên package),
 * dùng Alt+Enter trong IntelliJ để sửa import @WebMvcTest / @MockitoBean.
 */
@WebMvcTest
@TestPropertySource(properties = ["DB_USERNAME=test", "DB_PASSWORD=test"])
class ProductControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var jdbcTemplate: JdbcTemplate

    private fun sampleRow(id: Long, name: String, price: String) = mapOf<String, Any>(
        "productId" to id,
        "categoryId" to 1,
        "sku" to "TEST00$id",
        "productName" to name,
        "unit" to "chai",
        "price" to BigDecimal(price),
        "isActive" to true
    )

    // A1 - TC-35-01: trả 200, là mảng JSON, có đủ field
    @Test
    fun A1_getProducts_returnsListWithFields() {
        `when`(jdbcTemplate.queryForList(anyString()))
            .thenReturn(listOf(sampleRow(1, "Nước suối 500ml", "5000.00")))

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].productId").value(1))
            .andExpect(jsonPath("$[0].productName").value("Nước suối 500ml"))
            .andExpect(jsonPath("$[0].price").exists())
            .andExpect(jsonPath("$[0].isActive").value(true))
    }

    // A2 - TC-35-03: không có sản phẩm -> 200 và mảng rỗng (không lỗi 500)
    @Test
    fun A2_getProducts_empty_returnsEmptyArray() {
        `when`(jdbcTemplate.queryForList(anyString())).thenReturn(emptyList())

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    // A3 - TC-35-02 (AC3): câu SQL phải chỉ lấy sản phẩm đang kinh doanh.
    // LƯU Ý: trên nhánh main test này DỰ KIẾN FAIL vì SQL chưa lọc IsActive.
    // Đó là bug thật cần ghi vào Bug Log (nếu nhánh bạn test chưa sửa).
    @Test
    fun A3_getProducts_sqlFiltersActiveOnly() {
        `when`(jdbcTemplate.queryForList(anyString())).thenReturn(emptyList())

        mockMvc.perform(get("/api/products")).andExpect(status().isOk)

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(jdbcTemplate).queryForList(captor.capture())
        val sql = captor.value.replace("\\s+".toRegex(), " ").lowercase()
        assertTrue(
            sql.contains("isactive = 1") || sql.contains("isactive=1"),
            "SQL không lọc IsActive = 1, sản phẩm ngừng kinh doanh sẽ bị trả về. SQL: $sql"
        )
    }

    // A4 - smoke: health check
    @Test
    fun A4_health_returnsUp() {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
}
