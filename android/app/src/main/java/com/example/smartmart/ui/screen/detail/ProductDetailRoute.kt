package com.example.smartmart.ui.screen.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.repository.ProductRepository
import com.example.smartmart.viewmodel.ProductDetailViewModel

/**
 * SWA-36 — Lê Tuấn Thành. Bind the selected SWA-35/SWA-17 product ID to its own
 * ViewModel. The host owns navigation and cart integration; the screen stays stateless.
 */
@Composable
fun ProductDetailRoute(
    productId: Long,
    repository: ProductRepository,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    val factory = remember(productId, repository) {
        ProductDetailViewModel.factory(productId, repository)
    }
    val viewModel: ProductDetailViewModel = viewModel(
        key = "product-detail-$productId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductDetailScreen(
        state = state,
        onRetry = viewModel::retry,
        onAddToCart = onAddToCart,
        modifier = modifier,
        onNavigateBack = onNavigateBack,
    )
}
