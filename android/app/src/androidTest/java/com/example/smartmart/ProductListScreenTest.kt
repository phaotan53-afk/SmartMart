
package com.example.smartmart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.smartmart.data.model.Product
import com.example.smartmart.ui.product.ProductListScreen
import com.example.smartmart.ui.theme.SmartMartTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProductListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleProducts = listOf(
        Product(1, "Sua tuoi", "MILK001", "Hop", 25000.0, true),
        Product(2, "Nuoc suoi", "WATER001", "Chai", 10000.0, false)
    )

    private fun showScreen(
        products: List<Product> = sampleProducts,
        loading: Boolean = false,
        error: String? = null,
        onRetry: () -> Unit = {}
    ) {
        composeRule.setContent {
            SmartMartTheme {
                ProductListScreen(
                    products = products,
                    isLoading = loading,
                    error = error,
                    onRetry = onRetry
                )
            }
        }
    }

    // Test 1: Hiển thị danh sách
    @Test
    fun displayProductList() {
        showScreen()

        composeRule.onNodeWithText("Sua tuoi").assertIsDisplayed()
        composeRule.onNodeWithText("Nuoc suoi").assertIsDisplayed()
    }

    // Test 2: Hiển thị loading
    @Test
    fun displayLoadingState() {
        showScreen(loading = true)

        composeRule
            .onNodeWithTag("loading_state")
            .assertIsDisplayed()
    }

    // Test 3: Hiển thị danh sách rỗng
    @Test
    fun displayEmptyState() {
        showScreen(products = emptyList())

        composeRule
            .onNodeWithTag("empty_state")
            .assertIsDisplayed()
    }

    // Test 4: Hiển thị lỗi API
    @Test
    fun displayApiError() {
        showScreen(error = "Không thể kết nối Backend")

        composeRule
            .onNodeWithTag("error_state")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("retry_button")
            .assertIsDisplayed()
    }

    // Test 5: Nút thử lại
    @Test
    fun retryButtonCallsCallback() {
        var retryCalled = false

        showScreen(
            error = "Lỗi API",
            onRetry = { retryCalled = true }
        )

        composeRule
            .onNodeWithTag("retry_button")
            .performClick()

        assertTrue(retryCalled)
    }

    // Test 6: Hiển thị giá
    @Test
    fun displayCorrectPrice() {
        showScreen()

        composeRule
            .onNodeWithText("Giá: 25.000 đ")
            .assertIsDisplayed()
    }

    // Test 7: Tìm theo tên

    @Test
    fun searchByName() {
        showScreen()

        composeRule
            .onNodeWithTag("search_input")
            .performTextInput("Sua tuoi")

        // Kiểm tra sản phẩm phù hợp được hiển thị
        composeRule
            .onNodeWithTag("product_1")
            .assertIsDisplayed()

        // Kiểm tra sản phẩm không phù hợp bị loại khỏi danh sách
        composeRule
            .onNodeWithTag("product_2")
            .assertDoesNotExist()
    }


    // Test 8: Tìm theo SKU
    @Test
    fun searchBySku() {
        showScreen()

        composeRule
            .onNodeWithTag("search_input")
            .performTextInput("WATER001")

        composeRule.onNodeWithText("Nuoc suoi").assertIsDisplayed()
        composeRule.onNodeWithText("Sua tuoi").assertDoesNotExist()
    }

    // Test 9: Lọc sản phẩm đang kinh doanh
    @Test
    fun filterActiveProducts() {
        showScreen()

        composeRule
            .onNodeWithTag("active_filter")
            .performClick()

        composeRule.onNodeWithText("Sua tuoi").assertIsDisplayed()
        composeRule.onNodeWithText("Nuoc suoi").assertDoesNotExist()
    }

    // Test 10: Không tìm thấy sản phẩm
    @Test
    fun showNoSearchResult() {
        showScreen()

        composeRule
            .onNodeWithTag("search_input")
            .performTextInput("KHONGTONTAI")

        composeRule
            .onNodeWithTag("no_result_state")
            .assertIsDisplayed()
    }
}
