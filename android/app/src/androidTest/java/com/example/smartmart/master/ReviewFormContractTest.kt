package com.example.smartmart.master

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus
import com.example.smartmart.ui.theme.SmartMartTheme
import com.example.smartmart.viewmodel.ProductDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

/** Positive FE contract only. No credential, admin operation or claim of live SQL integration. */
class ReviewFormContractTest {
    @get:Rule val compose = createComposeRule()
    @Test fun deliveredOrderReviewFormDispatchesExactProductOrderRatingAndComment() {
        val product = Product(41, "Sản phẩm kiểm thử", BigDecimal(10000), null, null, StockStatus.IN_STOCK)
        val order = Order(71, "UI-COMPONENT", "DELIVERED", "UNPAID", BigDecimal(10000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal(10000), "2026-11-05T08:00:00", items = listOf(OrderItem(41, name = product.name, quantity = 1)))
        val state = StoreState(destination = Destination.DETAIL, selectedProductId = 41, selectedOrder = order, user = User(1, "Khách kiểm thử", "ui@example.invalid", role = "CUSTOMER"), catalogLoading = false)
        var submitted: StoreEvent.ReviewProduct? = null
        compose.setContent { SmartMartTheme { StoreApp(state, ProductDetailUiState.Success(product), { if (it is StoreEvent.ReviewProduct) submitted = it }, {}) } }
        compose.onNodeWithTag("master_detail_success").performScrollToNode(hasText("Đánh giá (0)"))
        compose.onNodeWithText("Đánh giá (0)").performClick()
        compose.onNodeWithTag("review_order").performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("71")))
        compose.onNodeWithContentDescription("4 sao").performScrollTo().performClick()
        compose.onNodeWithTag("review_comment").performScrollTo().performTextInput("Tươi ngon"); Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("review_submit").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(StoreEvent.ReviewProduct(41, 71, 4, "Tươi ngon"), submitted) }
    }
}
