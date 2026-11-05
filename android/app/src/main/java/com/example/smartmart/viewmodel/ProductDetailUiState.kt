package com.example.smartmart.viewmodel

import com.example.smartmart.domain.model.Product

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState

    data class Success(val data: Product) : ProductDetailUiState

    data class Error(val message: String) : ProductDetailUiState

    data object NotFound : ProductDetailUiState
}
