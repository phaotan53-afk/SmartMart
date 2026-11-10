
package com.example.smartmart

import com.example.smartmart.data.model.Product
import com.example.smartmart.domain.product.filterProducts
import com.example.smartmart.utils.formatPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFilterTest {

    private val products = listOf(
        Product(1, "Sua tuoi", "MILK001", "Hop", 25000.0, true),
        Product(2, "Nuoc suoi", "WATER001", "Chai", 10000.0, false),
        Product(3, "Sua chua", "YOGURT001", "Hop", 12000.0, true)
    )

    // Test 1: Tìm kiếm theo tên
    @Test
    fun searchByProductName() {
        val result = filterProducts(products, "Sua tuoi", false)

        assertEquals(1, result.size)
        assertEquals("Sua tuoi", result[0].productName)
    }

    // Test 2: Tìm kiếm theo SKU
    @Test
    fun searchBySku() {
        val result = filterProducts(products, "WATER001", false)

        assertEquals(1, result.size)
        assertEquals("WATER001", result[0].sku)
    }

    // Test 3: Không phân biệt chữ hoa và chữ thường
    @Test
    fun searchCaseInsensitive() {
        val result = filterProducts(products, "sUa", false)

        assertEquals(2, result.size)
    }

    // Test 4: Lọc sản phẩm đang kinh doanh
    @Test
    fun filterActiveProducts() {
        val result = filterProducts(products, "", true)

        assertEquals(2, result.size)
        assertTrue(result.all { it.isActive })
    }

    // Test 5: Kết hợp tìm kiếm và lọc
    @Test
    fun combineSearchAndFilter() {
        val result = filterProducts(products, "Sua", true)

        assertEquals(2, result.size)
    }

    // Test 6: Không tìm thấy sản phẩm
    @Test
    fun searchNoResult() {
        val result = filterProducts(
            products,
            "KhongTonTai",
            false
        )

        assertTrue(result.isEmpty())
    }

    // Test 7: Danh sách sản phẩm rỗng
    @Test
    fun emptyProductList() {
        val result = filterProducts(
            emptyList(),
            "",
            false
        )

        assertTrue(result.isEmpty())
    }

    // Test 8: Định dạng giá sản phẩm
    @Test
    fun formatProductPrice() {
        assertEquals("25.000 đ", formatPrice(25000.0))
    }
}
