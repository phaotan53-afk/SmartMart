package com.example.smartmart

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smartmart.data.model.Product
import com.example.smartmart.data.remote.ProductHttpException
import com.example.smartmart.ui.customer.CustomerProductDetailContent
import com.example.smartmart.ui.customer.CustomerProductDetailScreen
import com.example.smartmart.ui.customer.CustomerProductScreen
import com.example.smartmart.ui.customer.ProductDetailUiState
import com.example.smartmart.ui.theme.SmartMartTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** UI của SWA-147 dùng dữ liệu kiểm thử xác định; không tuyên bố đây là test SQL Server. */
class ProductDetailScreenTest {
    @get:Rule val rule = createComposeRule()
    private val product = Product(17, "Sữa tươi kiểm thử", "MILK017", "Hộp", 25000.0, true, 2, "Mô tả kiểm thử")

    private fun screenshot(name: String) {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "swa147-evidence")
        directory.mkdirs()
        File(directory, "fixture-$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun detailShowsProductFromInjectedApi() {
        rule.setContent { SmartMartTheme { CustomerProductDetailScreen(17, {}, { product }) } }
        rule.onNodeWithText(product.productName).assertIsDisplayed()
        rule.onNodeWithText("25.000 đ").assertIsDisplayed()
        rule.onNodeWithText("MILK017").performScrollTo().assertIsDisplayed()
        screenshot("01-detail-success")
    }

    @Test fun pendingRequestShowsLoading() {
        val response = CompletableDeferred<Product>()
        rule.setContent { SmartMartTheme { CustomerProductDetailScreen(17, {}, { response.await() }) } }
        rule.onNodeWithTag("detail_loading").assertIsDisplayed()
        screenshot("02-loading")
    }

    @Test fun missingProductShowsNotFound() {
        rule.setContent { SmartMartTheme { CustomerProductDetailScreen(999, {}, { throw ProductHttpException(404) }) } }
        rule.onNodeWithText("Không tìm thấy sản phẩm").assertIsDisplayed()
        rule.onNodeWithTag("detail_error").assertDoesNotExist()
        screenshot("03-not-found")
    }

    @Test fun networkFailureShowsFriendlyErrorAndRetry() {
        rule.setContent { SmartMartTheme { CustomerProductDetailScreen(17, {}, { throw IOException("internal stack localhost") }) } }
        rule.onNodeWithTag("detail_error").assertIsDisplayed()
        rule.onNodeWithTag("detail_retry").assertIsDisplayed()
        rule.onNodeWithText("internal stack localhost", substring = true).assertDoesNotExist()
        screenshot("04-network-error")
    }

    @Test fun retryExecutesAnotherRequestAndDisplaysSuccess() {
        val calls = AtomicInteger()
        rule.setContent {
            SmartMartTheme {
                CustomerProductDetailScreen(17, {}, { if (calls.incrementAndGet() == 1) throw IOException() else product })
            }
        }
        rule.onNodeWithTag("detail_retry").performClick()
        rule.onNodeWithText(product.productName).assertIsDisplayed()
        assertEquals(2, calls.get())
        screenshot("05-retry-success")
    }

    @Test fun actualCustomerListOpensSelectedProductId() {
        var selectedId = 0L
        val second = product.copy(productId = 18, productName = "Nước suối kiểm thử", sku = "WATER018")
        rule.setContent {
            SmartMartTheme {
                CustomerProductScreen({}, { listOf(product, second) }, { id -> selectedId = id; second })
            }
        }
        rule.onNodeWithText(second.productName).performScrollTo().performClick()
        rule.onNodeWithTag("detail_content").assertExists()
        rule.onNodeWithText(second.productName).assertIsDisplayed()
        assertEquals(18L, selectedId)
        screenshot("06-customer-list-to-detail")
    }

    @Test fun systemBackReturnsToListBeforeRoleSelection() {
        rule.setContent { SmartMartTheme { SmartMartApp({ listOf(product) }, { product }) } }
        rule.onNodeWithText("Khách hàng").performClick()
        rule.onNodeWithText(product.productName).performClick()
        rule.onNodeWithTag("detail_content").assertExists()
        Espresso.pressBack()
        rule.onNodeWithTag("detail_content").assertDoesNotExist()
        rule.onNodeWithText(product.productName).assertIsDisplayed()
        rule.onNodeWithText("Chọn giao diện trải nghiệm").assertDoesNotExist()
        screenshot("07-system-back-to-list")
        Espresso.pressBack()
        rule.onNodeWithText("Chọn giao diện trải nghiệm").assertIsDisplayed()
    }

    @Test fun toolbarBackReturnsToCustomerList() {
        rule.setContent { SmartMartTheme { CustomerProductScreen({}, { listOf(product) }, { product }) } }
        rule.onNodeWithText(product.productName).performClick()
        rule.onNodeWithTag("detail_content").assertExists()
        rule.onNodeWithText("←").performClick()
        rule.onNodeWithTag("detail_content").assertDoesNotExist()
        rule.onNodeWithText(product.productName).assertIsDisplayed()
    }

    @Test fun blankDescriptionUsesFallbackAndInactiveStatusIsShown() {
        rule.setContent {
            SmartMartTheme { CustomerProductDetailContent(ProductDetailUiState.Success(product.copy(description = "", isActive = false)), {}, {}) }
        }
        rule.onNodeWithText("Chưa có mô tả").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Ngừng kinh doanh").performScrollTo().assertIsDisplayed()
        screenshot("08-empty-description-inactive")
    }

    @Test fun notFoundReturnButtonCallsNavigation() {
        var backCalled = false
        rule.setContent { SmartMartTheme { CustomerProductDetailContent(ProductDetailUiState.NotFound, { backCalled = true }, {}) } }
        rule.onNodeWithText("Về danh sách").performClick()
        assertTrue(backCalled)
    }
}
