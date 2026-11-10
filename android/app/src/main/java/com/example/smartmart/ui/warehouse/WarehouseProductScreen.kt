
package com.example.smartmart.ui.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.NumberFormat
import java.util.Locale
import com.example.smartmart.ui.components.getProductEmoji
import com.example.smartmart.ui.components.getProductIconBackground
import com.example.smartmart.ui.components.SmartMartBackButton



private val Green = Color(0xFF16A34A)
private val DarkGreen = Color(0xFF15803D)
private val Background = Color(0xFFF8FAF9)
private val Border = Color(0xFFE5E7EB)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseProductScreen(
    onBackClick: () -> Unit
) {

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null

        try {
            products = ProductApi.getProducts()
        } catch (e: Exception) {
            error = e.message ?: "Không thể tải sản phẩm"
        } finally {
            loading = false
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            WarehouseBottomBar()
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    SmartMartBackButton(
                        onClick = onBackClick
                    )
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "▣",
                            color = Green,
                            fontSize = 24.sp
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "SmartMart",
                            color = Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { refreshKey++ }
                    ) {
                        Text(
                            text = "Làm mới",
                            color = DarkGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Quản lý sản phẩm",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Danh sách sản phẩm trong hệ thống",
                fontSize = 13.sp,
                color = GrayText
            )

            Spacer(Modifier.height(18.dp))

            // Thanh tìm kiếm minh họa.
            // Chức năng tìm kiếm thuộc SWA-17.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        Border,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⌕",
                    fontSize = 22.sp,
                    color = GrayText
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "Tìm mã hoặc tên (sắp ra mắt)",
                    fontSize = 13.sp,
                    color = GrayText
                )
            }

            Spacer(Modifier.height(18.dp))

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Green
                        )
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Không thể tải danh sách sản phẩm",
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = error ?: "",
                            color = GrayText
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { refreshKey++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green
                            )
                        ) {
                            Text("Thử lại")
                        }
                    }
                }

                products.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có sản phẩm nào",
                            color = GrayText
                        )
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tổng: ${products.size} sản phẩm",
                            fontSize = 13.sp,
                            color = GrayText
                        )

                        Text(
                            text = "Quản lý kho",
                            fontSize = 12.sp,
                            color = DarkGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            bottom = 24.dp
                        )
                    ) {
                        items(
                            items = products,
                            key = { it.productId }
                        ) { product ->
                            WarehouseProductItem(product)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseProductItem(product: Product) {

    val price = NumberFormat
        .getNumberInstance(Locale.forLanguageTag("vi-VN"))
        .format(product.price)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Border,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = getProductIconBackground(
                                product.productName
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getProductEmoji(
                            product.productName
                        ),
                        fontSize = 24.sp
                    )
                }


                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = product.productName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = DarkText
                    )

                    Text(
                        text = "SKU: ${product.sku}",
                        fontSize = 12.sp,
                        color = GrayText
                    )

                    Text(
                        text = "$price đ / ${product.unit}",
                        fontSize = 13.sp,
                        color = DarkGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFF1F5F9)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (product.isActive) {
                        "● Đang kinh doanh"
                    } else {
                        "● Ngừng kinh doanh"
                    },
                    fontSize = 12.sp,
                    color = if (product.isActive) {
                        DarkGreen
                    } else {
                        Color(0xFFDC2626)
                    }
                )

                Text(
                    text = "Mã #${product.productId}",
                    fontSize = 12.sp,
                    color = GrayText
                )
            }
        }
    }
}
