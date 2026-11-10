package com.example.smartmart

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test end-to-end (Jetpack Compose, chạy trên emulator).
 * Đặt tại: android/app/src/androidTest/java/com/example/smartmart/
 *
 * Màn SmartMartApp khởi đầu ở RoleSelectionScreen nên test sẽ click "Khách hàng".
 * TRƯỚC KHI CHẠY:
 *   1. Backend đang chạy (bootRun) và database có dữ liệu Seed_Data.sql
 *   2. Emulator đang chạy và kết nối được 10.0.2.2:8080 (hoặc adb reverse tcp:8080 tcp:8080)
 */
@RunWith(AndroidJUnit4::class)
class ProductListE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()


    // Bấm chọn vai trò "Khách hàng" và chờ đến khi danh sách sản phẩm tải xong (tối đa 15 giây)
    private fun waitForProducts() {
        // Bấm chọn vai trò Khách hàng từ RoleSelectionScreen
        composeRule.onNodeWithText("Khách hàng").performClick()

        // Chờ sản phẩm hiển thị dựa theo chuỗi "Mã: "
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Mã: ", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // A5 - TC-35-01: hiển thị tiêu đề và thông tin cơ bản của sản phẩm
    // Dữ liệu mong đợi lấy từ Seed_Data.sql (TEST001, TEST002).
    @Test
    fun A5_productList_showsBasicInfo() {
        waitForProducts()

        composeRule.onNodeWithText("Danh sách sản phẩm tại SmartMart").assertIsDisplayed()
        composeRule.onNodeWithText("Nước suối 500ml").assertIsDisplayed()
        composeRule.onNodeWithText("Mã: TEST001").assertIsDisplayed()
        composeRule.onNodeWithText("5.000 đ").assertIsDisplayed()
        composeRule.onNodeWithText("Sữa tươi 1 lít").assertIsDisplayed()
    }

    // A6 - TC-35-02 (AC3): sản phẩm ngừng kinh doanh không được hiển thị.
    // ĐIỀU KIỆN TRƯỚC KHI CHẠY (SSMS):
    //   UPDATE dbo.Products SET IsActive = 0 WHERE SKU = 'TEST001';
    // SAU KHI CHẠY, khôi phục:
    //   UPDATE dbo.Products SET IsActive = 1;
    @Test
    fun A6_inactiveProduct_isNotDisplayed() {
        waitForProducts()

        composeRule.onNodeWithText("Nước suối 500ml").assertDoesNotExist()
        composeRule.onAllNodesWithText("Ngừng kinh doanh").assertCountEquals(0)
    }
}
