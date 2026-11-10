
package com.example.smartmart.data.api

import com.example.smartmart.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object ProductApi {

    private const val API_URL =
        "http://10.0.2.2:8080/api/products"

    suspend fun getProducts(): List<Product> =
        withContext(Dispatchers.IO) {

            val connection =
                URL(API_URL).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode

                if (responseCode !in 200..299) {
                    val message = connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()

                    throw IOException(
                        "Server trả về HTTP $responseCode: $message"
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
                        productName = item.optString("productName", "Chưa có tên"),
                        sku = item.optString("sku", ""),
                        unit = item.optString("unit", ""),
                        price = item.optDouble("price", 0.0),
                        isActive = item.optBoolean("isActive", true)
                    )
                }

            } finally {
                connection.disconnect()
            }
        }
}
