package com.example.smartmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smartmart.domain.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** SWA-36 — Lê Tuấn Thành. Owns request lifetime; the screen only renders state. */
class ProductDetailViewModel(
    productId: Long,
    private val repository: ProductRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private var currentProductId = productId
    private var loadJob: Job? = null

    init {
        loadProduct(productId)
    }

    /** Call from the main thread; a new request cancels the previous request. */
    fun loadProduct(productId: Long) {
        currentProductId = productId
        loadJob?.cancel()

        if (productId <= 0) {
            _uiState.value = ProductDetailUiState.NotFound
            return
        }

        _uiState.value = ProductDetailUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val product = withContext(ioDispatcher) {
                    repository.getProductById(productId)
                }
                ensureActive()
                _uiState.value = product?.let { ProductDetailUiState.Success(it) }
                    ?: ProductDetailUiState.NotFound
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ensureActive()
                Timber.e(exception, "Product detail load failed")
                _uiState.value = ProductDetailUiState.Error(
                    "⚠️ Không thể tải dữ liệu sản phẩm",
                )
            }
        }
    }

    fun retry() {
        loadProduct(currentProductId)
    }

    companion object {
        fun factory(
            productId: Long,
            repository: ProductRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProductDetailViewModel(productId, repository)
            }
        }
    }
}
