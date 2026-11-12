package com.example.smartmart

import com.example.smartmart.data.model.Product
import com.example.smartmart.data.remote.ProductHttpException
import com.example.smartmart.ui.customer.ProductDetailLoader
import com.example.smartmart.ui.customer.ProductDetailUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/** SWA-147: kiểm tra trạng thái và lỗi của đúng bộ tải màn hình chi tiết. */
class ProductDetailLoaderTest {
    private val product = Product(17, "Sữa tươi", "MILK017", "Hộp", 25000.0, true, 2, "Mô tả")

    private fun load(id: Long = 17, fetch: suspend (Long) -> Product): List<ProductDetailUiState> = runBlocking {
        val states = mutableListOf<ProductDetailUiState>()
        ProductDetailLoader(fetch).load(id, states::add)
        states
    }

    @Test fun loadingIsEmittedBeforeResult() {
        val states = load { product }
        assertEquals(listOf(ProductDetailUiState.Loading, ProductDetailUiState.Success(product)), states)
    }

    @Test fun selectedIdIsPassedToApi() {
        var requestedId = 0L
        load { requestedId = it; product }
        assertEquals(17L, requestedId)
    }

    @Test fun http404BecomesNotFound() {
        assertEquals(ProductDetailUiState.NotFound, load { throw ProductHttpException(404) }.last())
    }

    @Test fun zeroIdDoesNotCallApi() {
        var called = false
        val result = load(0) { called = true; product }
        assertFalse(called)
        assertEquals(ProductDetailUiState.NotFound, result.last())
    }

    @Test fun negativeIdDoesNotCallApi() {
        var called = false
        val result = load(-2) { called = true; product }
        assertFalse(called)
        assertEquals(ProductDetailUiState.NotFound, result.last())
    }

    @Test fun timeoutHasFriendlyMessage() {
        val state = load { throw SocketTimeoutException("socket internal details") }.last() as ProductDetailUiState.Error
        assertTrue(state.message.contains("quá thời gian chờ"))
        assertFalse(state.message.contains("socket"))
    }

    @Test fun networkFailureDoesNotExposeTechnicalDetails() {
        val state = load { throw IOException("localhost:8080 stack trace") }.last() as ProductDetailUiState.Error
        assertTrue(state.message.contains("Kiểm tra mạng"))
        assertFalse(state.message.contains("localhost"))
    }

    @Test fun serverErrorIsRetryableErrorNotNotFound() {
        val state = load { throw ProductHttpException(500) }.last()
        assertTrue(state is ProductDetailUiState.Error)
    }

    @Test fun cancellationPropagatesWhenLeavingScreen() = runBlocking {
        val cancellation = CancellationException("screen closed")
        val states = mutableListOf<ProductDetailUiState>()
        try {
            ProductDetailLoader { throw cancellation }.load(17, states::add)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        assertEquals(listOf(ProductDetailUiState.Loading), states)
    }

    @Test fun wrongProductIsRejected() {
        assertTrue(load { product.copy(productId = 99) }.last() is ProductDetailUiState.Error)
    }

    @Test fun invalidPriceIsRejected() {
        assertTrue(load { product.copy(price = -1.0) }.last() is ProductDetailUiState.Error)
        assertTrue(load { product.copy(price = Double.NaN) }.last() is ProductDetailUiState.Error)
    }

    @Test fun retryLoadsAgainAndReplacesErrorWithProduct() = runBlocking {
        var calls = 0
        val loader = ProductDetailLoader { if (++calls == 1) throw IOException("offline") else product }
        val states = mutableListOf<ProductDetailUiState>()
        loader.load(17, states::add)
        loader.load(17, states::add)
        assertEquals(2, calls)
        assertEquals(ProductDetailUiState.Loading, states[2])
        assertEquals(ProductDetailUiState.Success(product), states.last())
    }
}
