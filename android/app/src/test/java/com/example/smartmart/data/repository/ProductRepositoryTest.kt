package com.example.smartmart.data.repository

import com.example.smartmart.data.api.ProductApiService
import com.example.smartmart.data.model.ProductResponse
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus
import com.example.smartmart.domain.repository.ProductServiceException
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal

class ProductRepositoryTest {
    private val api = mockk<ProductApiService>()
    private val repository = ProductRepositoryImpl(api)
    private val response = ProductResponse(
        id = 42L,
        name = "Fresh milk",
        price = BigDecimal("1234567890123.45"),
        description = "No added sugar.",
        imageUrl = "https://images.example.com/milk.jpg",
        stockStatus = "IN_STOCK",
    )

    @Test
    fun `successful response maps full product without losing decimal precision`() = runTest {
        coEvery { api.getProductById(42L) } returns Response.success(response)

        val result = repository.getProductById(42L)

        assertEquals(
            Product(
                id = 42L,
                name = "Fresh milk",
                price = BigDecimal("1234567890123.45"),
                description = "No added sugar.",
                imageUrl = "https://images.example.com/milk.jpg",
                stockStatus = StockStatus.IN_STOCK,
            ),
            result,
        )
        coVerify(exactly = 1) { api.getProductById(42L) }
    }

    @Test
    fun `HTTP 404 is the missing product result`() = runTest {
        coEvery { api.getProductById(42L) } returns errorResponse(404)

        assertNull(repository.getProductById(42L))
    }

    @Test
    fun `HTTP 500 remains a service failure rather than a missing product`() = runTest {
        coEvery { api.getProductById(42L) } returns errorResponse(500)

        expectFailure<ProductServiceException> { repository.getProductById(42L) }
    }

    @Test
    fun `HTTP 204 without a product fails the detail response contract`() = runTest {
        coEvery { api.getProductById(42L) } returns Response.success<ProductResponse>(204, null)

        expectFailure<ProductServiceException> { repository.getProductById(42L) }
    }

    @Test
    fun `response with missing price cannot create a purchasable domain product`() = runTest {
        coEvery { api.getProductById(42L) } returns Response.success(response.copy(price = null))

        expectFailure<ProductServiceException> { repository.getProductById(42L) }
    }

    @Test
    fun `response for a different product ID fails instead of displaying the wrong item`() = runTest {
        coEvery { api.getProductById(42L) } returns Response.success(response.copy(id = 43L))

        expectFailure<ProductServiceException> { repository.getProductById(42L) }
    }

    @Test
    fun `invalid product ID is rejected without making an API request`() = runTest {
        expectFailure<IllegalArgumentException> { repository.getProductById(0L) }

        coVerify(exactly = 0) { api.getProductById(any()) }
    }

    @Test
    fun `unknown stock status maps to unavailable rather than allowing purchases`() = runTest {
        coEvery { api.getProductById(42L) } returns Response.success(
            response.copy(stockStatus = "DISCONTINUED"),
        )

        assertEquals(StockStatus.UNAVAILABLE, repository.getProductById(42L)?.stockStatus)
    }

    @Test
    fun `Spring numeric JSON price retains its exact decimal value through domain mapping`() = runTest {
        val json = """
            {
                "id": 42,
                "name": "Fresh milk",
                "price": 1234567890123.45,
                "description": null,
                "imageUrl": null,
                "stockStatus": "OUT_OF_STOCK"
            }
        """.trimIndent()
        val parsed = Gson().fromJson(json, ProductResponse::class.java)
        coEvery { api.getProductById(42L) } returns Response.success(parsed)

        val product = repository.getProductById(42L)

        assertEquals(BigDecimal("1234567890123.45"), product?.price)
        assertEquals(StockStatus.OUT_OF_STOCK, product?.stockStatus)
        assertNull(product?.description)
        assertNull(product?.imageUrl)
    }

    @Test
    fun `unit from backend is retained without making up a default`() = runTest {
        coEvery { api.getProductById(42L) } returns Response.success(response.copy(unit = " kg "))
        assertEquals("kg", repository.getProductById(42L)?.unit)

        coEvery { api.getProductById(42L) } returns Response.success(response.copy(unit = "  "))
        assertNull(repository.getProductById(42L)?.unit)

        coEvery { api.getProductById(42L) } returns Response.success(response.copy(unit = null))
        assertNull(repository.getProductById(42L)?.unit)
    }

    @Test
    fun `negative price is rejected at the API boundary`() = runTest {
        coEvery { api.getProductById(42L) } returns Response.success(
            response.copy(price = BigDecimal("-1")),
        )
        expectFailure<ProductServiceException> { repository.getProductById(42L) }
    }

    @Test
    fun `missing name or stock cannot create an apparently purchasable product`() = runTest {
        for (invalid in listOf(response.copy(name = " "), response.copy(stockStatus = null))) {
            coEvery { api.getProductById(42L) } returns Response.success(invalid)
            expectFailure<ProductServiceException> { repository.getProductById(42L) }
        }
    }

    @Test
    fun `transport failure maps to domain exception and preserves its cause`() = runTest {
        val transportFailure = IOException("offline")
        coEvery { api.getProductById(42L) } throws transportFailure

        val failure = expectFailure<ProductServiceException> { repository.getProductById(42L) }

        assertSame(transportFailure, failure.cause)
    }

    @Test
    fun `repository never converts cancellation into a load failure`() = runTest {
        val cancellation = CancellationException("destination left")
        coEvery { api.getProductById(42L) } throws cancellation

        val result = expectFailure<CancellationException> { repository.getProductById(42L) }

        assertSame(cancellation, result)
    }

    private fun errorResponse(code: Int): Response<ProductResponse> = Response.error(
        code,
        "{}".toResponseBody("application/json".toMediaType()),
    )

    private suspend inline fun <reified T : Throwable> expectFailure(
        block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (failure: Throwable) {
            assertTrue("Expected ${T::class.java.name}, got ${failure.javaClass.name}", failure is T)
            return failure as T
        }
        fail("Expected ${T::class.java.name}")
        error("Unreachable after JUnit assertion failure")
    }
}
