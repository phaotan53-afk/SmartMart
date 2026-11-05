package com.example.smartmart.viewmodel

import app.cash.turbine.test
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus
import com.example.smartmart.domain.repository.ProductRepository
import com.example.smartmart.domain.repository.ProductServiceException
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
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ProductRepository>()
    private val product = Product(
        id = 36L,
        name = "Fresh milk",
        price = BigDecimal("32500.00"),
        description = "One litre",
        imageUrl = "https://example.com/milk.jpg",
        stockStatus = StockStatus.IN_STOCK,
    )

    @Test
    fun `initial load emits Loading then Success`() = runTest {
        coEvery { repository.getProductById(product.id) } returns product
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(ProductDetailUiState.Success(product), awaitItem())
            expectNoEvents()
        }
        coVerify(exactly = 1) { repository.getProductById(product.id) }
    }

    @Test
    fun `connection failure emits Loading then safe Error`() = runTest {
        coEvery { repository.getProductById(product.id) } throws IOException("private host")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(
                ProductDetailUiState.Error(
                    "⚠️ Không thể tải dữ liệu sản phẩm",
                ),
                awaitItem(),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `service failure emits Loading then safe Error`() = runTest {
        coEvery { repository.getProductById(product.id) } throws ProductServiceException()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(
                ProductDetailUiState.Error("⚠️ Không thể tải dữ liệu sản phẩm"),
                awaitItem(),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `missing product emits Loading then NotFound`() = runTest {
        coEvery { repository.getProductById(product.id) } returns null
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(ProductDetailUiState.NotFound, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `retry emits Loading then Success after Error`() = runTest {
        coEvery { repository.getProductById(product.id) } throws IOException("offline")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(
                ProductDetailUiState.Error(
                    "⚠️ Không thể tải dữ liệu sản phẩm",
                ),
                awaitItem(),
            )

            coEvery { repository.getProductById(product.id) } returns product
            viewModel.retry()
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(ProductDetailUiState.Success(product), awaitItem())
            expectNoEvents()
        }
        coVerify(exactly = 2) { repository.getProductById(product.id) }
    }

    @Test
    fun `cancellation does not become Error`() = runTest {
        coEvery { repository.getProductById(product.id) } throws CancellationException()
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            expectNoEvents()
        }
        assertFalse(viewModel.uiState.value is ProductDetailUiState.Error)
    }

    @Test
    fun `nonpositive product IDs become NotFound without a request`() = runTest {
        for (invalidId in listOf(0L, -1L)) {
            val viewModel = createViewModel(invalidId)
            viewModel.uiState.test {
                assertEquals(ProductDetailUiState.NotFound, awaitItem())
                advanceUntilIdle()
                expectNoEvents()
            }
        }
        coVerify(exactly = 0) { repository.getProductById(any()) }
    }

    @Test
    fun `late result from canceled load cannot replace the current product`() = runTest {
        val oldResult = CompletableDeferred<Product>()
        val nextProduct = product.copy(id = 37L, name = "Bread")
        coEvery { repository.getProductById(product.id) } coAnswers {
            // Simulate a data source that finishes even after its caller is canceled.
            withContext(NonCancellable) { oldResult.await() }
        }
        coEvery { repository.getProductById(nextProduct.id) } returns nextProduct
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            viewModel.loadProduct(nextProduct.id)
            runCurrent()
            assertEquals(ProductDetailUiState.Success(nextProduct), awaitItem())

            oldResult.complete(product)
            advanceUntilIdle()
            expectNoEvents()
            assertEquals(ProductDetailUiState.Success(nextProduct), viewModel.uiState.value)
        }
        coVerify(exactly = 1) { repository.getProductById(product.id) }
        coVerify(exactly = 1) { repository.getProductById(nextProduct.id) }
    }

    @Test
    fun `retry uses the most recently selected product ID`() = runTest {
        val nextProduct = product.copy(id = 37L)
        coEvery { repository.getProductById(product.id) } returns product
        coEvery { repository.getProductById(nextProduct.id) } throws IOException("offline")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(ProductDetailUiState.Success(product), awaitItem())

            viewModel.loadProduct(nextProduct.id)
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(
                ProductDetailUiState.Error(
                    "⚠️ Không thể tải dữ liệu sản phẩm",
                ),
                awaitItem(),
            )

            coEvery { repository.getProductById(nextProduct.id) } returns nextProduct
            viewModel.retry()
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(ProductDetailUiState.Success(nextProduct), awaitItem())
        }
        coVerify(exactly = 1) { repository.getProductById(product.id) }
        coVerify(exactly = 2) { repository.getProductById(nextProduct.id) }
    }

    @Test
    fun `invalid selection cancels a pending success and stays NotFound`() = runTest {
        val pendingProduct = CompletableDeferred<Product>()
        coEvery { repository.getProductById(product.id) } coAnswers {
            withContext(NonCancellable) { pendingProduct.await() }
        }
        val viewModel = createViewModel()
        runCurrent()

        viewModel.loadProduct(0L)
        assertEquals(ProductDetailUiState.NotFound, viewModel.uiState.value)
        pendingProduct.complete(product)
        advanceUntilIdle()

        assertEquals(ProductDetailUiState.NotFound, viewModel.uiState.value)
        coVerify(exactly = 0) { repository.getProductById(0L) }
    }

    @Test
    fun `late error from canceled request cannot replace a newer success`() = runTest {
        val releaseOldRequest = CompletableDeferred<Unit>()
        val nextProduct = product.copy(id = 37L)
        coEvery { repository.getProductById(product.id) } coAnswers {
            withContext(NonCancellable) { releaseOldRequest.await() }
            throw ProductServiceException()
        }
        coEvery { repository.getProductById(nextProduct.id) } returns nextProduct
        val viewModel = createViewModel()
        runCurrent()

        viewModel.loadProduct(nextProduct.id)
        runCurrent()
        assertEquals(ProductDetailUiState.Success(nextProduct), viewModel.uiState.value)
        releaseOldRequest.complete(Unit)
        advanceUntilIdle()

        assertEquals(ProductDetailUiState.Success(nextProduct), viewModel.uiState.value)
    }

    @Test
    fun `unexpected failure emits a safe message`() = runTest {
        coEvery { repository.getProductById(product.id) } throws
            IllegalStateException("sensitive internal details")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            runCurrent()
            assertEquals(
                ProductDetailUiState.Error("⚠️ Không thể tải dữ liệu sản phẩm"),
                awaitItem(),
            )
            expectNoEvents()
        }
    }

    private fun createViewModel(productId: Long = product.id) = ProductDetailViewModel(
        productId = productId,
        repository = repository,
        ioDispatcher = mainDispatcherRule.dispatcher,
    )

}
