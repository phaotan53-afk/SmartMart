
package com.example.smartmart.ui.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartmart.data.model.Product
import com.example.smartmart.domain.product.filterProducts
import com.example.smartmart.utils.formatPrice

private val Green = Color(0xFF16A34A)
private val LightGreen = Color(0xFFDCFCE7)
private val Background = Color(0xFFF8FAFC)
private val TextDark = Color(0xFF1F2937)
private val TextGray = Color(0xFF64748B)
private val BorderGray = Color(0xFFE2E8F0)

@Composable
fun ProductListScreen(
    products: List<Product>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var activeOnly by remember { mutableStateOf(false) }

    val filteredProducts = remember(
        products,
        keyword,
        activeOnly
    ) {
        filterProducts(
            products = products,
            keyword = keyword,
            activeOnly = activeOnly
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "SmartMart",
                        color = Green,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Mua sắm tiện lợi mỗi ngày",
                        color = TextGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Danh sách sản phẩm",
                color = TextDark,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Tìm kiếm theo tên hoặc SKU
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                label = {
                    Text("Tìm kiếm sản phẩm...")
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green,
                    unfocusedBorderColor = BorderGray,
                    focusedLabelColor = Green,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    cursorColor = Green,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Lọc trạng thái sản phẩm
            FilterChip(
                selected = activeOnly,
                onClick = {
                    activeOnly = !activeOnly
                },
                label = {
                    Text(
                        if (activeOnly) "Đang kinh doanh"
                        else "Tất cả / Chỉ đang kinh doanh"
                    )
                },
                modifier = Modifier.testTag("active_filter"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Green,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = TextDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = activeOnly,
                    borderColor = BorderGray,
                    selectedBorderColor = Green
                )
            )

            when {

                // Loading
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Green)

                        Text(
                            text = "Đang tải sản phẩm...",
                            color = TextGray,
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .testTag("loading_state")
                        )
                    }
                }

                // Lỗi API
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error,
                            color = TextDark,
                            modifier = Modifier.testTag("error_state")
                        )

                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green
                            ),
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .testTag("retry_button")
                        ) {
                            Text("Thử lại")
                        }
                    }
                }

                // Danh sách rỗng
                products.isEmpty() -> {
                    Text(
                        text = "Chưa có sản phẩm nào.",
                        color = TextGray,
                        modifier = Modifier
                            .padding(16.dp)
                            .testTag("empty_state")
                    )
                }

                // Không tìm thấy kết quả
                filteredProducts.isEmpty() -> {
                    Text(
                        text = "Không tìm thấy sản phẩm phù hợp.",
                        color = TextGray,
                        modifier = Modifier.testTag("no_result_state")
                    )
                }

                // Hiển thị danh sách
                else -> {
                    Text(
                        text = "${filteredProducts.size} sản phẩm",
                        color = TextGray,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("product_list"),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredProducts,
                            key = { it.productId }
                        ) { product ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("product_${product.productId}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                border = BorderStroke(1.dp, BorderGray),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 1.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = product.productName,
                                        color = TextDark,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Mã sản phẩm: ${product.sku}",
                                        color = TextGray
                                    )

                                    Text(
                                        text = "Đơn vị: ${product.unit}",
                                        color = TextGray
                                    )

                                    Text(
                                        text = "Giá: ${formatPrice(product.price)}",
                                        color = Green,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Surface(
                                        color = if (product.isActive) {
                                            LightGreen
                                        } else {
                                            BorderGray
                                        },
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (product.isActive) {
                                                "Đang kinh doanh"
                                            } else {
                                                "Ngừng kinh doanh"
                                            },
                                            color = if (product.isActive) {
                                                Green
                                            } else {
                                                TextGray
                                            },
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 5.dp
                                            ),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
