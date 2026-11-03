
package com.example.smartmart.ui.customer
import com.example.smartmart.ui.components.SmartMartBackButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmart.data.model.Product
import com.example.smartmart.data.remote.ProductApi
import com.example.smartmart.ui.components.ProductCard

private val SmartGreen = Color(0xFF16A34A)
private val DarkGreen = Color(0xFF15803D)
private val ScreenBackground = Color(0xFFF8FAF9)
private val TextDark = Color(0xFF111827)
private val TextGray = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProductScreen(
    onBackClick: () -> Unit
) {

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorMessage = null

        try {
            products = ProductApi.getProducts()
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Không thể kết nối đến máy chủ"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = ScreenBackground,
        bottomBar = {
            CustomerBottomBar()
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
                            text = "🛒",
                            fontSize = 22.sp
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "SmartMart",
                            fontWeight = FontWeight.Bold,
                            color = SmartGreen,
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Khám phá sản phẩm",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Danh sách sản phẩm tại SmartMart",
                fontSize = 13.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Thanh giới thiệu tính năng tìm kiếm
            // Chưa có thao tác tìm kiếm trong SWA-35
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        Color(0xFFE5E7EB),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⌕",
                    fontSize = 22.sp,
                    color = TextGray
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Tìm kiếm sản phẩm (sắp ra mắt)",
                    fontSize = 13.sp,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SmartGreen
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Không thể tải danh sách sản phẩm",
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = errorMessage ?: "",
                            color = TextGray
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Button(
                            onClick = { refreshKey++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SmartGreen
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
                            text = "Hiện chưa có sản phẩm nào",
                            color = TextGray
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Tổng số: ${products.size} sản phẩm",
                        fontSize = 13.sp,
                        color = DarkGreen,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

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
                            ProductCard(product = product)
                        }
                    }
                }
            }
        }
    }
}
