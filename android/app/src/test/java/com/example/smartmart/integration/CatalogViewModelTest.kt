package com.example.smartmart.integration

import com.example.smartmart.data.api.ProductApiService
import com.example.smartmart.data.model.CatalogProductResponse
import com.example.smartmart.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = mockk<ProductApiService>()
    private val product = CatalogProductResponse(
        productId = 36L,
        productName = "Táo đỏ",
        sku = "TAO-DO",
        unit = "kg",
        price = BigDecimal("35000"),
        isActive = true,
    )

    @Test
    fun `initial catalog loading becomes success`() = runTest {
        coEvery { api.getProducts() } returns listOf(product)
        val viewModel = CatalogViewModel(api)
        assertEquals(CatalogState.Loading, viewModel.state.value)

        advanceUntilIdle()

        assertEquals(CatalogState.Success(listOf(product)), viewModel.state.value)
    }

    @Test
    fun `empty collection remains a successful empty result`() = runTest {
        coEvery { api.getProducts() } returns emptyList()
        val viewModel = CatalogViewModel(api)

        advanceUntilIdle()

        assertEquals(CatalogState.Success(emptyList()), viewModel.state.value)
    }

    @Test
    fun `retry recovers from a service failure`() = runTest {
        coEvery { api.getProducts() } throws IOException("offline")
        val viewModel = CatalogViewModel(api)
        advanceUntilIdle()
        assertEquals(CatalogState.Error, viewModel.state.value)

        coEvery { api.getProducts() } returns listOf(product)
        viewModel.retry()
        runCurrent()

        assertEquals(CatalogState.Success(listOf(product)), viewModel.state.value)
        coVerify(exactly = 2) { api.getProducts() }
    }

    @Test
    fun `cancellation does not become an error`() = runTest {
        coEvery { api.getProducts() } throws CancellationException()
        val viewModel = CatalogViewModel(api)

        advanceUntilIdle()

        assertEquals(CatalogState.Loading, viewModel.state.value)
    }

    @Test
    fun `late success from a canceled request cannot overwrite retry result`() = runTest {
        val releaseOld = CompletableDeferred<Unit>()
        var requestCount = 0
        coEvery { api.getProducts() } coAnswers {
            requestCount++
            if (requestCount == 1) {
                withContext(NonCancellable) { releaseOld.await() }
                listOf(product.copy(productId = 1L))
            } else {
                listOf(product)
            }
        }
        val viewModel = CatalogViewModel(api)
        runCurrent()
        viewModel.retry()
        runCurrent()
        assertEquals(CatalogState.Success(listOf(product)), viewModel.state.value)

        releaseOld.complete(Unit)
        advanceUntilIdle()

        assertEquals(CatalogState.Success(listOf(product)), viewModel.state.value)
    }

    @Test
    fun `late error from a canceled request cannot overwrite retry result`() = runTest {
        val releaseOld = CompletableDeferred<Unit>()
        var requestCount = 0
        coEvery { api.getProducts() } coAnswers {
            requestCount++
            if (requestCount == 1) {
                withContext(NonCancellable) { releaseOld.await() }
                throw IOException("old failed request")
            }
            listOf(product)
        }
        val viewModel = CatalogViewModel(api)
        runCurrent()
        viewModel.retry()
        runCurrent()
        assertEquals(CatalogState.Success(listOf(product)), viewModel.state.value)

        releaseOld.complete(Unit)
        advanceUntilIdle()

        assertEquals(CatalogState.Success(listOf(product)), viewModel.state.value)
    }
}
