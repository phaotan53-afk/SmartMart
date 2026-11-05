package com.example.smartmart.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smartmart.data.api.ProductApiService
import com.example.smartmart.data.model.CatalogProductResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/** Existing collection bridge. SWA-35 can replace this host without changing SWA-36. */
sealed interface CatalogState {
    data object Loading : CatalogState
    data class Success(val products: List<CatalogProductResponse>) : CatalogState
    data object Error : CatalogState
}

class CatalogViewModel(private val api: ProductApiService) : ViewModel() {
    private val mutableState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val state = mutableState.asStateFlow()
    private var request: Job? = null

    init { retry() }

    fun retry() {
        request?.cancel()
        request = viewModelScope.launch {
            mutableState.value = CatalogState.Loading
            try {
                val products = api.getProducts()
                ensureActive()
                mutableState.value = CatalogState.Success(products)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                // A canceled source may finish or throw late; it must not overwrite a retry.
                ensureActive()
                Timber.e(exception, "Catalog request failed")
                mutableState.value = CatalogState.Error
            }
        }
    }

    companion object {
        fun factory(api: ProductApiService): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CatalogViewModel::class.java))
                return CatalogViewModel(api) as T
            }
        }
    }
}
