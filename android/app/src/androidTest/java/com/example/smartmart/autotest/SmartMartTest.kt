package autotest

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


@RunWith(AndroidJUnit4::class)
class ProductListE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()


    
    private fun waitForProducts() {
     
        composeRule.onNodeWithText("Khách hàng").performClick()

        
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Mã: ", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun A5_productList_showsBasicInfo() {
        waitForProducts()

        composeRule.onNodeWithText("Danh sách sản phẩm tại SmartMart").assertIsDisplayed()
        composeRule.onNodeWithText("Nước suối 500ml").assertIsDisplayed()
        composeRule.onNodeWithText("Mã: TEST001").assertIsDisplayed()
        composeRule.onNodeWithText("5.000 đ").assertIsDisplayed()
        composeRule.onNodeWithText("Sữa tươi 1 lít").assertIsDisplayed()
    }

    @Test
    fun A6_inactiveProduct_isNotDisplayed() {
        waitForProducts()

        composeRule.onNodeWithText("Nước suối 500ml").assertDoesNotExist()
        composeRule.onAllNodesWithText("Ngừng kinh doanh").assertCountEquals(0)
    }
}
