package com.example.smartmart.ui.screen.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus
import com.example.smartmart.ui.theme.SmartMartTheme
import com.example.smartmart.viewmodel.ProductDetailUiState
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** UI acceptance tests — no live backend or network images required. */
class ProductDetailScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val product = Product(
        id = 42L,
        name = "Táo đỏ",
        price = BigDecimal("35000"),
        description = "Táo đỏ giòn ngọt.",
        imageUrl = null,
        stockStatus = StockStatus.IN_STOCK,
        unit = "kg",
    )

    @Test fun priceUsesVietnameseGroupingAndOnlyProvidedUnit() {
        assertEquals("35.000đ/kg", formatProductPrice(BigDecimal("35000"), "kg"))
        assertEquals("35.000đ", formatProductPrice(BigDecimal("35000"), null))
        assertEquals("35.001đ/hộp", formatProductPrice(BigDecimal("35000.5"), " hộp "))
    }

    @Test fun loadingShowsProgressWithoutPurchaseAction() {
        composeRule.setContent {
            SmartMartTheme { ProductDetailScreen(ProductDetailUiState.Loading, {}, {}) }
        }
        composeRule.onNodeWithTag("detail_loading").assertIsDisplayed()
        composeRule.onNodeWithTag("detail_add_to_cart").assertDoesNotExist()
    }

    @Test fun successShowsSelectedProductAndDispatchesExactProduct() {
        var selected: Product? = null
        composeRule.setContent {
            SmartMartTheme {
                ProductDetailScreen(ProductDetailUiState.Success(product), {}, { selected = it })
            }
        }
        composeRule.onNodeWithText("Táo đỏ").assertIsDisplayed()
        composeRule.onNodeWithTag("detail_add_to_cart").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(product, selected) }
    }

    @Test fun errorRetryDispatchesActionWithoutCart() {
        var retries = 0
        composeRule.setContent {
            SmartMartTheme {
                ProductDetailScreen(ProductDetailUiState.Error("⚠️ Không thể tải dữ liệu sản phẩm"), { retries++ }, {})
            }
        }
        composeRule.onNodeWithTag("detail_error").assertIsDisplayed()
        composeRule.onNodeWithTag("detail_retry").performClick()
        composeRule.runOnIdle { assertEquals(1, retries) }
        composeRule.onNodeWithTag("detail_add_to_cart").assertDoesNotExist()
    }

    @Test fun notFoundShowsVietnameseEmptyState() {
        composeRule.setContent {
            SmartMartTheme { ProductDetailScreen(ProductDetailUiState.NotFound, {}, {}) }
        }
        composeRule.onNodeWithText("🔍 Sản phẩm không tồn tại hoặc đã bị xóa").assertIsDisplayed()
        composeRule.onNodeWithTag("detail_add_to_cart").assertDoesNotExist()
    }

    @Test fun outOfStockCannotBeAddedToCart() {
        composeRule.setContent {
            SmartMartTheme {
                ProductDetailScreen(ProductDetailUiState.Success(product.copy(stockStatus = StockStatus.OUT_OF_STOCK)), {}, {})
            }
        }
        composeRule.onNodeWithTag("detail_add_to_cart").assertIsNotEnabled()
    }

    @Test fun unknownAvailabilityCannotBeAddedToCart() {
        composeRule.setContent {
            SmartMartTheme {
                ProductDetailScreen(ProductDetailUiState.Success(product.copy(stockStatus = StockStatus.UNAVAILABLE)), {}, {})
            }
        }
        composeRule.onNodeWithTag("detail_add_to_cart").assertIsNotEnabled()
    }

    @Test fun navigateBackDispatchesHostAction() {
        var backs = 0
        composeRule.setContent {
            SmartMartTheme {
                ProductDetailScreen(ProductDetailUiState.NotFound, {}, {}, onNavigateBack = { backs++ })
            }
        }
        composeRule.onNodeWithText("SmartMart").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Quay lại danh sách sản phẩm").performClick()
        composeRule.runOnIdle { assertEquals(1, backs) }
    }
}
