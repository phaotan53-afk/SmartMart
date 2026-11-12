package com.example.smartmart

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smartmart.data.remote.ProductApi
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Test tích hợp thật: cần Backend + SQL Server + adb reverse 8080 đang hoạt động. */
class ProductDetailLiveIntegrationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun actualAppLoadsDetailFromRunningBackendAndSqlServer() {
        val expected = runBlocking { ProductApi.getProducts().first() }
        rule.onNodeWithText("Khách hàng").performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithText("Mã: ${expected.sku}").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Mã: ${expected.sku}").performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithTag("detail_content").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(expected.productName).assertIsDisplayed()
        rule.onNodeWithText(expected.sku).performScrollTo().assertIsDisplayed()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "swa147-evidence")
        directory.mkdirs()
        File(directory, "live-01-backend-sql-detail.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        rule.onNodeWithText("←").performClick()
        rule.onNodeWithTag("detail_content").assertDoesNotExist()
        rule.onNodeWithText("Mã: ${expected.sku}").assertIsDisplayed()
    }
}
