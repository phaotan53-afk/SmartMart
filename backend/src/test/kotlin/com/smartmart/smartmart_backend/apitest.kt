
package com.smartmart.smartmart_backend

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class apitest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun A1_getProducts_returnsListWithFields() {

        val dbProducts = jdbcTemplate.queryForList(
            "SELECT TOP 1 * FROM Products"
        )

        if (dbProducts.isEmpty()) {
            println("Database chưa có sản phẩm để kiểm tra A1")
            return
        }

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun A2_getProducts_empty_returnsEmptyArray() {

        val dbProducts = jdbcTemplate.queryForList(
            "SELECT TOP 1 * FROM Products"
        )

        if (dbProducts.isNotEmpty()) {
            println("Bỏ qua A2: Database hiện có sản phẩm thật.")
            return
        }

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }


    @Test
    fun A3_getProducts_sqlFiltersActiveOnly() {

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.isActive == false)]").doesNotExist())
    }


    @Test
    fun A4_health_returnsUp() {

        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
}
