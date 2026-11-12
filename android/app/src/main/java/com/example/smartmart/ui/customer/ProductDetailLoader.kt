package com.example.smartmart.ui.customer

import com.example.smartmart.data.model.Product
import com.example.smartmart.data.remote.ProductHttpException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data object NotFound : ProductDetailUiState
    data class Success(val product: Product) : ProductDetailUiState
    data class Error(val message: String) : ProductDetailUiState
}

// SWA-147: Tách việc tải dữ liệu khỏi Compose để kiểm tra được cả lỗi và thử lại.
class ProductDetailLoader(private val fetchProduct: suspend (Long) -> Product) {
    suspend fun load(productId: Long, emit: (ProductDetailUiState) -> Unit) {
        emit(ProductDetailUiState.Loading)
        if (productId <= 0) {
            emit(ProductDetailUiState.NotFound)
            return
        }
        val result = try {
            val product = fetchProduct(productId)
            require(product.productId == productId && product.productName.isNotBlank())
            require(product.price.isFinite() && product.price >= 0)
            ProductDetailUiState.Success(product)
        } catch (e: CancellationException) {
            // Khi rời màn hình phải hủy coroutine, không biến việc hủy thành lỗi mạng.
            throw e
        } catch (e: ProductHttpException) {
            if (e.statusCode == 404) ProductDetailUiState.NotFound
            else ProductDetailUiState.Error("Máy chủ chưa thể tải sản phẩm. Vui lòng thử lại.")
        } catch (e: SocketTimeoutException) {
            ProductDetailUiState.Error("Kết nối quá thời gian chờ. Vui lòng thử lại.")
        } catch (e: IOException) {
            ProductDetailUiState.Error("Không thể kết nối máy chủ. Kiểm tra mạng và thử lại.")
        } catch (e: Exception) {
            ProductDetailUiState.Error("Dữ liệu sản phẩm không hợp lệ. Vui lòng thử lại.")
        }
        emit(result)
    }
}
