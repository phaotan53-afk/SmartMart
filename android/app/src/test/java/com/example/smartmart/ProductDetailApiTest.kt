package com.example.smartmart

import com.example.smartmart.data.remote.ProductApi
import org.junit.Assert.*
import org.junit.Test

/** SWA-147: dùng bộ giải mã JSON thật mà getProductById gọi. */
class ProductDetailApiTest {
    private val validJson = """{"productId":17,"productName":"Sữa tươi","sku":"MILK017","unit":"Hộp","price":25000,"isActive":true,"categoryId":2,"description":"Mô tả có dấu"}"""

    @Test fun decodesActualEnglishApiContract() {
        val product = ProductApi.parseDetailProduct(validJson, 17)
        assertEquals(17L, product.productId)
        assertEquals("Sữa tươi", product.productName)
        assertEquals("MILK017", product.sku)
        assertEquals("Hộp", product.unit)
        assertEquals(25000.0, product.price, 0.0)
        assertEquals(2L, product.categoryId)
        assertEquals("Mô tả có dấu", product.description)
        assertTrue(product.isActive)
    }

    @Test fun nullableCategoryIsSupported() {
        assertNull(ProductApi.parseDetailProduct(validJson.replace("\"categoryId\":2", "\"categoryId\":null"), 17).categoryId)
    }

    @Test fun missingOptionalFieldsUseDefaults() {
        val product = ProductApi.parseDetailProduct("""{"productId":17,"productName":"Sản phẩm","price":0}""", 17)
        assertEquals("", product.description)
        assertEquals("", product.sku)
        assertNull(product.categoryId)
    }

    @Test fun inactiveProductStatusIsPreserved() {
        assertFalse(ProductApi.parseDetailProduct(validJson.replace("\"isActive\":true", "\"isActive\":false"), 17).isActive)
    }

    @Test fun unexpectedProductIdIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProductApi.parseDetailProduct(validJson, 99) }
    }

    @Test fun missingProductIdIsRejected() {
        assertThrows(Exception::class.java) { ProductApi.parseDetailProduct("""{"productName":"A","price":1}""", 17) }
    }

    @Test fun malformedJsonIsRejected() {
        assertThrows(Exception::class.java) { ProductApi.parseDetailProduct("not json", 17) }
    }

    @Test fun missingPriceIsRejectedInsteadOfShowingZero() {
        assertThrows(IllegalArgumentException::class.java) { ProductApi.parseDetailProduct("""{"productId":17,"productName":"A"}""", 17) }
    }

    @Test fun negativePriceIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProductApi.parseDetailProduct(validJson.replace("25000", "-1"), 17) }
    }

    @Test fun nullPriceIsRejectedInsteadOfShowingZero() {
        assertThrows(IllegalArgumentException::class.java) { ProductApi.parseDetailProduct(validJson.replace("25000", "null"), 17) }
    }

    @Test fun nonNumericPriceIsRejectedInsteadOfShowingZero() {
        assertThrows(Exception::class.java) { ProductApi.parseDetailProduct(validJson.replace("25000", "\"invalid\""), 17) }
    }

    @Test fun blankProductNameIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProductApi.parseDetailProduct(validJson.replace("Sữa tươi", "   "), 17) }
    }
}
