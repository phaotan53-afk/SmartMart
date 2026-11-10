
package com.example.smartmart.domain.product

import com.example.smartmart.data.model.Product
import java.util.Locale

fun filterProducts(
    products: List<Product>,
    keyword: String,
    activeOnly: Boolean
): List<Product> {

    val searchText = keyword.trim().lowercase(Locale.ROOT)

    return products.filter { product ->

        // Tìm kiếm theo tên sản phẩm hoặc SKU
        val matchesKeyword =
            searchText.isEmpty() ||
                    product.productName.lowercase(Locale.ROOT)
                        .contains(searchText) ||
                    product.sku.lowercase(Locale.ROOT)
                        .contains(searchText)

        // Lọc sản phẩm đang kinh doanh
        val matchesStatus =
            !activeOnly || product.isActive

        matchesKeyword && matchesStatus
    }
}
