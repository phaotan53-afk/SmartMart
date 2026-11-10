
package com.example.smartmart.ui.product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.smartmart.data.model.Product
import com.example.smartmart.data.repository.ProductRepository
import kotlinx.coroutines.CancellationException

@Composable
fun ProductListRoute() {

    val repository = remember {
        ProductRepository()
    }

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var retryCount by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(retryCount) {

        isLoading = true
        error = null

        try {
            products = repository.getProducts()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.localizedMessage
                ?: "Không thể tải sản phẩm"

            products = emptyList()
        } finally {
            isLoading = false
        }
    }

    ProductListScreen(
        products = products,
        isLoading = isLoading,
        error = error,
        onRetry = {
            retryCount++
        }
    )
}
