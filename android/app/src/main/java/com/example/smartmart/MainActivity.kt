
package com.example.smartmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartmart.ui.theme.SmartMartTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Product(
    val productId: Long,
    val productName: String,
    val sku: String,
    val unit: String,
    val price: Double,
    val isActive: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartMartTheme {
                SmartMartScreen()
            }
        }
    }
}

@Composable
fun SmartMartScreen() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null

        try {
            products = withContext(Dispatchers.IO) {
                val connection = URL(
                    "http://127.0.0.1:8080/api/products"
                ).openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    if (connection.responseCode !in 200..299) {
                        error = "Server trả về mã lỗi: ${connection.responseCode}"
                        emptyList()
                    } else {
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
                                isActive = item.optBoolean("isActive", true)
                            )
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            error = "Không thể tải sản phẩm: ${e.localizedMessage}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SmartMart",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Danh sách sản phẩm",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) { innerPadding ->
        when {
            loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                    Text("Đang tải sản phẩm...")
                }
            }

            error != null -> {
                Text(
                    text = error ?: "Đã xảy ra lỗi",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                )
            }

            products.isEmpty() -> {
                Text(
                    text = "Chưa có sản phẩm nào.",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = { it.productId }) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = product.productName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text("Mã sản phẩm: ${product.sku}")
                                Text("Đơn vị: ${product.unit}")
                                Text(
                                    text = "Giá: ${
                                        "%,.0f".format(
                                            java.util.Locale("vi", "VN"),
                                            product.price
                                        )
                                    } đ"
                                )
                                Text(
                                    text = if (product.isActive) {
                                        "Đang kinh doanh"
                                    } else {
                                        "Ngừng kinh doanh"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
