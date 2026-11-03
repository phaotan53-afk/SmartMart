
package com.example.smartmart.data.remote

import com.example.smartmart.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

object ProductApi {

    private const val PRODUCTS_URL =
        "http://127.0.0.1:8080/api/products"

    suspend fun getProducts(): List<Product> =
        withContext(Dispatchers.IO) {

            val connection = URL(PRODUCTS_URL)
                .openConnection(Proxy.NO_PROXY) as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val statusCode = connection.responseCode

                if (statusCode !in 200..299) {
                    throw IllegalStateException(
                        "Server trả về mã lỗi HTTP $statusCode"
                    )
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val jsonArray = JSONArray(response)

                List(jsonArray.length()) { index ->
                    val item = jsonArray.getJSONObject(index)

                    Product(
                        productId = item.getLong("productId"),
                        productName = item.optString(
                            "productName", "Chưa có tên"
                        ),
                        sku = item.optString("sku", ""),
                        unit = item.optString("unit", ""),
                        price = item.optDouble("price", 0.0),
                        isActive = item.optBoolean(
                            "isActive", true
                        )
                    )
                }
            } finally {
                connection.disconnect()
            }
        }
}
