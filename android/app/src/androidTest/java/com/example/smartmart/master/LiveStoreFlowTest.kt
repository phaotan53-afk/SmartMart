package com.example.smartmart.master

import android.graphics.Bitmap
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.example.smartmart.BuildConfig
import com.example.smartmart.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Bổ sung đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * Android thật → Spring Boot → SQL Server thật. Không có fixture HTTP/H2.
 * Tạo tài khoản kiểm thử riêng; không gọi dịch vụ thanh toán hoặc giao dịch thật.
 */
class LiveStoreFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun api(method: String, path: String, body: JSONObject? = null, token: String? = null): String {
        val connection = URL(BuildConfig.API_BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.requestMethod = method; connection.connectTimeout = 10000; connection.readTimeout = 20000
        connection.setRequestProperty("Accept", "application/json")
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        if (body != null) { connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) } }
        return try {
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream).bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue("Live backend HTTP $status for $path", status in 200..299); text
        } finally { connection.disconnect() }
    }
    private fun waitTag(tag: String) = compose.waitUntil(45_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes().toString(Charsets.UTF_8) }
    private fun screenshot(name: String) {
        // Window-manager resize crossfade is outside Compose idling; capture the settled surface.
        instrumentation.waitForIdleSync(); android.os.SystemClock.sleep(1200); instrumentation.waitForIdleSync()
        shell("mkdir -p /sdcard/smartmart-master-screenshots")
        shell("screencap -p /sdcard/smartmart-master-screenshots/$name.png")
    }
    private fun type(tag: String, value: String) { compose.onNodeWithTag(tag).performScrollTo().performTextInput(value); Espresso.closeSoftKeyboard() }
    private fun allowLocalNetwork() { if (Build.VERSION.SDK_INT >= 37) instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, "android.permission.ACCESS_LOCAL_NETWORK") }
    private fun waitText(text: String) = compose.waitUntil(30_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun openProductUi(id: Long, name: String) {
        compose.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("product_$id")); compose.onNodeWithText(name).performClick(); waitTag("master_detail_success")
    }

    @Test fun nativeRegisterProfileFavoriteCartChangesAndSearchFiltersPersist() {
        allowLocalNetwork()
        val email = "android-register-${UUID.randomUUID()}@example.invalid"; val password = "Ui-${UUID.randomUUID()}"
        val product = JSONObject(api("GET", "/api/v1/products?size=8&sort=name")).getJSONArray("items").getJSONObject(0)
        val id = product.getLong("id"); val name = product.getString("name")
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                waitTag("catalog_list"); compose.onNodeWithContentDescription("Tài khoản").performClick(); waitTag("auth_register_toggle")
                compose.onNodeWithTag("auth_register_toggle").performScrollTo().performClick(); type("auth_name", "UI Register Customer")
                type("auth_email", email); type("auth_password", password); compose.onNodeWithTag("auth_submit").performScrollTo().performClick(); waitTag("catalog_list")
                val token = JSONObject(api("POST", "/api/auth/login", JSONObject().put("email", email).put("password", password))).getString("token")
                compose.onNodeWithContentDescription("Tài khoản").performClick(); waitTag("profile_name")
                compose.onNodeWithTag("profile_name").performTextReplacement("UI Profile Updated")
                val newEmail = "android-profile-${UUID.randomUUID()}@example.invalid"
                compose.onNodeWithTag("profile_email").performTextReplacement(newEmail); Espresso.closeSoftKeyboard(); compose.onNodeWithTag("profile_save").performScrollTo().performClick()
                compose.waitUntil(30_000) { JSONObject(api("GET", "/api/me", token = token)).getString("email") == newEmail }
                assertEquals("UI Profile Updated", JSONObject(api("GET", "/api/me", token = token)).getString("fullName"))
                compose.onNodeWithText("Trang chủ").performClick(); waitTag("catalog_list"); compose.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("product_$id"))
                compose.onNodeWithContentDescription("Yêu thích $name").performClick(); compose.waitUntil(30_000) { JSONArray(api("GET", "/api/favorites", token = token)).length() == 1 }
                compose.onNodeWithContentDescription("Yêu thích").performClick(); waitText("Sản phẩm yêu thích")
                compose.onNodeWithContentDescription("Bỏ yêu thích $name").performScrollTo().performClick(); compose.waitUntil(30_000) { JSONArray(api("GET", "/api/favorites", token = token)).length() == 0 }
                compose.onNodeWithText("Trang chủ").performClick(); waitTag("catalog_list"); openProductUi(id, name)
                compose.onNodeWithTag("master_detail_add").performScrollTo().performClick(); compose.waitUntil(30_000) { JSONObject(api("GET", "/api/cart", token = token)).getInt("itemCount") == 1 }
                compose.onNodeWithTag("open_cart").performClick(); waitTag("cart_screen")
                compose.onNodeWithContentDescription("Tăng số lượng").performScrollTo().performClick(); compose.waitUntil(30_000) { JSONObject(api("GET", "/api/cart", token = token)).getInt("itemCount") == 2 }
                compose.waitUntil(15_000) { runCatching { compose.onNodeWithTag("quantity_value").assertTextEquals("2"); true }.getOrDefault(false) }
                compose.onNodeWithContentDescription("Giảm số lượng").performClick(); compose.waitUntil(30_000) { JSONObject(api("GET", "/api/cart", token = token)).getInt("itemCount") == 1 }
                compose.waitUntil(15_000) { runCatching { compose.onNodeWithTag("quantity_value").assertTextEquals("1"); true }.getOrDefault(false) }
                compose.onNodeWithContentDescription("Xóa $name").performClick(); compose.waitUntil(30_000) { JSONObject(api("GET", "/api/cart", token = token)).getInt("itemCount") == 0 }; waitText("Giỏ hàng đang trống.")
                compose.onNodeWithText("Trang chủ").performClick(); waitTag("catalog_list")
                compose.onNodeWithTag("catalog_search").performTextReplacement("no-result-${UUID.randomUUID()}"); Espresso.closeSoftKeyboard(); compose.onNodeWithContentDescription("Tìm kiếm").performClick(); waitText("Không có sản phẩm phù hợp.")
                screenshot("narrow-search-empty-live"); compose.onNodeWithText("Xóa bộ lọc").performScrollTo().performClick()
                compose.waitUntil(30_000) { compose.onAllNodesWithTag("product_$id").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithContentDescription("Mở bộ lọc").performClick(); waitTag("filter_min")
                val price = product.get("price").toString().substringBefore('.')
                type("filter_min", price); type("filter_max", price); compose.onNodeWithTag("filter_apply_price").performScrollTo().performClick()
                compose.waitUntil(30_000) { compose.onAllNodesWithText("1 sản phẩm").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("product_$id")); compose.onNodeWithText(name).assertIsDisplayed(); screenshot("narrow-price-filter-live")
                openProductUi(id, name)
                compose.onNodeWithTag("master_detail_success").performScrollToNode(hasText("Đánh giá (", substring = true))
                compose.onNodeWithText("Đánh giá (", substring = true).performClick(); waitTag("review_order")
                val before = JSONObject(api("GET", "/api/products/$id/reviews")).getLong("reviewCount")
                compose.onNodeWithText("4★").performScrollTo().performClick(); type("review_order", "999999999"); type("review_comment", "UI ineligible review test")
                compose.onNodeWithTag("review_submit").performScrollTo().performClick(); waitText("Tài khoản không có quyền thực hiện thao tác này.")
                assertEquals(before, JSONObject(api("GET", "/api/products/$id/reviews")).getLong("reviewCount")); screenshot("narrow-review-ineligible-live")
                compose.onNodeWithContentDescription("Tài khoản").performClick(); waitTag("profile_name"); compose.onNodeWithText("Đăng xuất").performScrollTo().performClick()
                waitTag("catalog_list"); compose.onNodeWithContentDescription("Tài khoản").performClick(); waitTag("auth_submit")
            } finally { screenshot("interaction-final-frame") }
        }
    }

    @Test fun referenceScreensAreNativeNarrowWideAndLargeFont() {
        allowLocalNetwork()
        val apple = JSONObject(api("GET", "/api/v1/products?query=" + java.net.URLEncoder.encode("Táo", "UTF-8"))).getJSONArray("items").getJSONObject(0)
        val id = apple.getLong("id"); val name = apple.getString("name")
        try {
            shell("wm size 1080x2400"); shell("wm density 420"); shell("settings put system font_scale 1.0")
            ActivityScenario.launch(MainActivity::class.java).use {
                waitTag("catalog_list"); compose.waitUntil(30_000) { compose.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Ảnh sản phẩm đã tải")).fetchSemanticsNodes().isNotEmpty() }
                screenshot("narrow-home-clean"); openProductUi(id, name)
                compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("master_product_image").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Ảnh sản phẩm đã tải")); true }.getOrDefault(false) }
                screenshot("narrow-apple-detail-clean")
                assertTrue("Demo apple gallery must have at least two real images", JSONObject(api("GET", "/api/products/$id")).getJSONArray("images").length() >= 2)
                compose.onNodeWithTag("product_thumbnail_1").performScrollTo().performClick(); compose.onNodeWithTag("product_thumbnail_1").assertIsSelected()
                compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("master_product_image").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Ảnh sản phẩm đã tải")); true }.getOrDefault(false) }
                screenshot("narrow-apple-thumbnail-selected")
                compose.onNodeWithTag("product_thumbnail_0").performClick(); compose.onNodeWithTag("product_thumbnail_0").assertIsSelected()
                compose.onNodeWithTag("master_detail_add").performScrollTo().assertIsDisplayed().assertIsEnabled(); screenshot("narrow-apple-actions-clean")
                shell("wm size 1800x1200"); shell("wm density 160")
                compose.waitUntil(30_000) { instrumentation.targetContext.resources.configuration.screenWidthDp >= 840 }; waitTag("master_detail_success")
                compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("master_product_image").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Ảnh sản phẩm đã tải")); true }.getOrDefault(false) }
                compose.onNodeWithText(name).assertExists()
                compose.onNodeWithTag("master_detail_success").performScrollToIndex(0); screenshot("wide-apple-detail-clean")
                compose.onNodeWithContentDescription("Quay lại").performClick(); waitTag("catalog_list"); compose.onNodeWithTag("catalog_list").performScrollToIndex(0); screenshot("wide-home-clean")
                openProductUi(id, name); shell("wm size 1080x2400"); shell("wm density 420"); shell("settings put system font_scale 1.3")
                compose.waitUntil(30_000) { instrumentation.targetContext.resources.configuration.fontScale >= 1.29f }; waitTag("master_detail_success")
                compose.onNodeWithText(name).assertExists()
                compose.onNodeWithTag("master_detail_add").performScrollTo().assertIsDisplayed().assertIsEnabled(); screenshot("narrow-apple-actions-font130")
            }
        } finally { shell("settings put system font_scale 1.0"); shell("wm density reset"); shell("wm size reset") }
    }

    @Test fun nativeCatalogDetailQuantityCartAddressQuoteAndCodPersistOnRealSql() {
        if (Build.VERSION.SDK_INT >= 37) instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, "android.permission.ACCESS_LOCAL_NETWORK")
        val email = "android-ui-${UUID.randomUUID()}@example.invalid"
        val password = "Ui-${UUID.randomUUID()}"
        val session = JSONObject(api("POST", "/api/auth/register", JSONObject().put("fullName", "[UI TEST] Android").put("email", email).put("password", password)))
        val token = session.getString("token")
        val page = JSONObject(api("GET", "/api/v1/products?size=8&sort=name"))
        val selected = page.getJSONArray("items").getJSONObject(0)
        val productId = selected.getLong("id"); val productName = selected.getString("name")
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
            waitTag("catalog_list"); compose.waitUntil(45_000) { compose.onAllNodesWithTag("product_$productId").fetchSemanticsNodes().isNotEmpty() }
            screenshot("narrow-home-live")
            compose.onNodeWithContentDescription("Tài khoản").performClick(); waitTag("auth_email")
            compose.onNodeWithTag("auth_email").performTextInput(email); compose.onNodeWithTag("auth_password").performTextInput(password); Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("auth_submit").performClick(); waitTag("catalog_list")
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Đăng nhập thành công.").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("product_$productId"))
            compose.onNodeWithText(productName).performClick(); waitTag("master_detail_success")
            screenshot("narrow-detail-live")
            compose.onNodeWithTag("master_detail_success").performScrollToNode(hasTestTag("master_detail_add"))
            compose.onNodeWithContentDescription("Tăng số lượng").performScrollTo().performClick()
            compose.onNodeWithTag("quantity_value").assertTextEquals("2")
            compose.onNodeWithTag("master_detail_add").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            try { compose.waitUntil(30_000) { JSONObject(api("GET", "/api/cart", token = token)).getInt("itemCount") == 2 } }
            catch (error: AssertionError) { screenshot("cart-write-failure"); throw AssertionError("Cart after native action: " + api("GET", "/api/cart", token = token), error) }
            compose.waitUntil(15_000) { compose.onAllNodes(hasText("Đã lưu 2", substring = true)).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("open_cart").performClick(); waitTag("cart_screen")
            compose.onNodeWithTag("cart_screen").performScrollToNode(hasTestTag("checkout_open")); screenshot("narrow-cart-live")
            compose.onNodeWithTag("checkout_open").performClick(); waitTag("checkout_screen")
            compose.onNodeWithText("Thêm địa chỉ").performScrollTo().performClick()
            type("address_recipient", "Nguoi Nhan UI"); type("address_phone", "0901234567"); type("address_line", "1 Duong Kiem Thu")
            type("address_district", "Quan Kiem Thu"); type("address_province", "Thanh Pho Kiem Thu")
            compose.onNodeWithTag("address_save").performScrollTo().assertIsDisplayed().assertIsEnabled()
            screenshot("narrow-address-before-save")
            compose.onNodeWithTag("address_save").performClick()
            compose.waitUntil(30_000) { JSONArray(api("GET", "/api/addresses", token = token)).length() == 1 }
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Đã lưu địa chỉ nhận hàng.").fetchSemanticsNodes().isEmpty() }
            compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("place_order").assertIsEnabled(); true }.getOrDefault(false) }
            compose.onNodeWithTag("place_order").performScrollTo().assertIsDisplayed()
            screenshot("narrow-checkout-live")
            compose.onNodeWithTag("place_order").performClick()
            compose.waitUntil(30_000) { JSONArray(api("GET", "/api/orders", token = token)).length() == 1 }
            val order = JSONArray(api("GET", "/api/orders", token = token)).getJSONObject(0)
            assertEquals("UNPAID", order.getString("paymentStatus")); assertEquals("COD", order.getString("paymentMethod"))
            val detail = JSONObject(api("GET", "/api/orders/${order.getLong("id")}", token = token))
            assertEquals(productId, detail.getJSONArray("items").getJSONObject(0).getLong("productId")); assertEquals(2, detail.getJSONArray("items").getJSONObject(0).getInt("quantity"))
            assertEquals(0, JSONObject(api("GET", "/api/cart", token = token)).getInt("itemCount"))
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Trạng thái: Chờ xác nhận").fetchSemanticsNodes().isNotEmpty() }
            screenshot("narrow-order-live")
            compose.onNodeWithContentDescription("Quay lại").performClick(); waitText("Đơn hàng của tôi")
            compose.onNodeWithText(order.getString("orderCode")).assertIsDisplayed().performClick(); waitText("Trạng thái: Chờ xác nhận")
            compose.onNodeWithText("Hủy đơn").performScrollTo().performClick(); waitText("Hủy đơn hàng?")
            compose.onAllNodesWithText("Hủy đơn").onLast().performClick()
            compose.waitUntil(30_000) { JSONObject(api("GET", "/api/orders/${order.getLong("id")}", token = token)).getString("orderStatus") == "CANCELLED" }
            compose.waitUntil(30_000) { JSONObject(api("GET", "/api/products/$productId")).getInt("quantityOnHand") == selected.getInt("quantityOnHand") }
            waitText("Trạng thái: Đã hủy"); screenshot("narrow-order-cancelled-live")
            } finally { screenshot("live-final-frame") }
        }
    }
}
