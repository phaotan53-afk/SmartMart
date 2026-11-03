
package com.example.smartmart.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmart.data.model.Product
import com.example.smartmart.data.remote.ProductApi
import com.example.smartmart.ui.components.SmartMartBackButton
import com.example.smartmart.ui.components.getProductEmoji
import com.example.smartmart.ui.components.getProductIconBackground
import java.text.NumberFormat
import java.util.Locale

private val DetailGreen = Color(0xFF16A34A)
private val DetailBackground = Color(0xFFF8FAF9)
private val DetailText = Color(0xFF111827)
private val DetailGray = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProductDetailScreen(
    productId: Long,
    onBackClick: () -> Unit
) {
    var product by remember(productId) {
        mutableStateOf<Product?>(null)
    }

    var isLoading by remember(productId) {
        mutableStateOf(true)
    }

    var errorMessage by remember(productId) {
        mutableStateOf<String?>(null)
    }

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(productId, refreshKey) {
        isLoading = true
        errorMessage = null
        product = null

        try {
            product = ProductApi.getProductById(productId)
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Không thể tải chi tiết sản phẩm"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = DetailBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiết sản phẩm",
                        fontWeight = FontWeight.Bold,
                        color = DetailText
                    )
                },
                navigationIcon = {
                    SmartMartBackButton(
                        onClick = onBackClick
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            CustomerBottomBar()
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = DetailGreen
                    )
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Không thể tải chi tiết sản phẩm",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = errorMessage ?: "",
                            color = DetailGray
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { refreshKey++ }
                        ) {
                            Text("Thử lại")
                        }
                    }
                }

                product != null -> {
                    val currentProduct = product!!

                    val formattedPrice = NumberFormat
                        .getNumberInstance(
                            Locale.forLanguageTag("vi-VN")
                        )
                        .format(currentProduct.price)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(
                                    getProductIconBackground(
                                        currentProduct.productName
                                    ),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getProductEmoji(
                                    currentProduct.productName
                                ),
                                fontSize = 80.sp
                            )
                        }

                        Text(
                            text = currentProduct.productName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DetailText
                        )

                        Text(
                            text = "$formattedPrice đ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DetailGreen
                        )

                        HorizontalDivider()

                        DetailInfoRow(
                            label = "Mã sản phẩm",
                            value = currentProduct.sku
                        )

                        DetailInfoRow(
                            label = "Đơn vị tính",
                            value = currentProduct.unit
                        )

                        DetailInfoRow(
                            label = "Danh mục",
                            value = currentProduct.categoryId
                                ?.toString() ?: "Chưa xác định"
                        )

                        DetailInfoRow(
                            label = "Trạng thái",
                            value = if (currentProduct.isActive) {
                                "Đang kinh doanh"
                            } else {
                                "Ngừng kinh doanh"
                            }
                        )

                        HorizontalDivider()

                        Text(
                            text = "Mô tả sản phẩm",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DetailText
                        )

                        Text(
                            text = currentProduct.description
                                .ifBlank { "Chưa có mô tả" },
                            fontSize = 14.sp,
                            color = DetailGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = DetailGray,
            fontSize = 14.sp
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = value,
            color = DetailText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
