
package com.example.smartmart.data.remote

import com.example.smartmart.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.net.URLEncoder
import java.io.IOException

class ProductHttpException(val statusCode: Int) : IOException("HTTP $statusCode")

object ProductApi {

    private const val PRODUCTS_URL =
        "http://127.0.0.1:8080/api/products"

    // SWA-35: Lấy danh sách tất cả sản phẩm
    suspend fun getProducts(): List<Product> =
        withContext(Dispatchers.IO) {
            val response = request("$PRODUCTS_URL")
            parseProducts(JSONArray(response))
        }

    // SWA-17: Tìm kiếm và lọc sản phẩm
    suspend fun searchProducts(
        keyword: String = "",
        categoryId: Long? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        page: Int = 0,
        pageSize: Int = 10
    ): List<Product> = withContext(Dispatchers.IO) {

        val parameters = mutableListOf<String>()

        if (keyword.isNotBlank()) {
            val encoded = URLEncoder.encode(
                keyword.trim(),
                "UTF-8"
            )
            parameters.add("tuKhoa=$encoded")
        }

        categoryId?.let {
            parameters.add("maDanhMuc=$it")
        }

        minPrice?.let {
            parameters.add("giaToiThieu=$it")
        }

        maxPrice?.let {
            parameters.add("giaToiDa=$it")
        }

        parameters.add("trang=$page")
        parameters.add("kichThuoc=$pageSize")

        val url = "$PRODUCTS_URL/search?" +
                parameters.joinToString("&")

        val response = request(url)
        val jsonObject = JSONObject(response)

        parseProducts(jsonObject.getJSONArray("content"))
    }

    // SWA-36: Lấy chi tiết sản phẩm theo ID
    suspend fun getProductById(
        productId: Long
    ): Product = withContext(Dispatchers.IO) {
        require(productId > 0) { "Mã sản phẩm không hợp lệ" }
        val response = request("$PRODUCTS_URL/$productId")
        parseDetailProduct(response, productId)
    }

    // SWA-147: Không hiển thị nhầm sản phẩm nếu hợp đồng API trả sai ID/dữ liệu.
    internal fun parseDetailProduct(response: String, expectedId: Long): Product {
        val item = JSONObject(response)
        val product = parseProduct(item)
        require(product.productId == expectedId && product.productName.isNotBlank())
        require(item.has("price") && !item.isNull("price"))
        val actualPrice = item.getDouble("price")
        require(actualPrice.isFinite() && actualPrice >= 0)
        return product
    }

    // Hàm dùng chung để gọi HTTP GET
    private fun request(url: String): String {

        val connection = URL(url)
            .openConnection(Proxy.NO_PROXY) as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val statusCode = connection.responseCode

            if (statusCode !in 200..299) {
                throw ProductHttpException(statusCode)
            }

            return connection.inputStream
                .bufferedReader()
                .use { it.readText() }

        } finally {
            connection.disconnect()
        }
    }

    // Chuyển JSON array thành danh sách Product
    private fun parseProducts(
        jsonArray: JSONArray
    ): List<Product> {

        return List(jsonArray.length()) { index ->
            parseProduct(jsonArray.getJSONObject(index))
        }
    }

    // Chuyển JSON object thành Product

    private fun parseProduct(
        item: JSONObject
    ): Product {
        return Product(
            productId = item.getLong("productId"),
            productName = item.optString(
                "productName",
                "Chưa có tên"
            ),
            sku = item.optString("sku", ""),
            unit = item.optString("unit", ""),
            price = item.optDouble("price", 0.0),
            isActive = item.optBoolean("isActive", true),
            categoryId = if (item.isNull("categoryId")) {
                null
            } else {
                item.optLong("categoryId")
            },
            description = item.optString("description", "")
        )
    }

}
