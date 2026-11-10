
package com.example.smartmart.ui.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmart.data.model.Product
import com.example.smartmart.data.remote.ProductApi
import com.example.smartmart.ui.components.ProductCard
import com.example.smartmart.ui.components.SmartMartBackButton

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

    // SWA-36: Sản phẩm đang được chọn
    var selectedProductId by remember {
        mutableStateOf<Long?>(null)
    }

    // SWA-17: Dữ liệu đang nhập
    var keyword by remember { mutableStateOf("") }
    var minPriceText by remember { mutableStateOf("") }
    var maxPriceText by remember { mutableStateOf("") }

    // SWA-17: Bộ lọc đã áp dụng
    var appliedKeyword by remember { mutableStateOf("") }

    var appliedMinPrice by remember {
        mutableStateOf<Double?>(null)
    }

    var appliedMaxPrice by remember {
        mutableStateOf<Double?>(null)
    }

    var isFilterApplied by remember {
        mutableStateOf(false)
    }

    var validationMessage by remember {
        mutableStateOf<String?>(null)
    }

    // Tải sản phẩm từ Backend
    LaunchedEffect(refreshKey) {
        isLoading = true
        errorMessage = null

        try {
            products = if (isFilterApplied) {
                ProductApi.searchProducts(
                    keyword = appliedKeyword,
                    minPrice = appliedMinPrice,
                    maxPrice = appliedMaxPrice
                )
            } else {
                ProductApi.getProducts()
            }
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Không thể kết nối đến máy chủ"
        } finally {
            isLoading = false
        }
    }

    // SWA-36: Hiển thị chi tiết khi chọn sản phẩm
    if (selectedProductId != null) {
        CustomerProductDetailScreen(
            productId = selectedProductId!!,
            onBackClick = {
                selectedProductId = null
            }
        )
        return
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

                        Spacer(Modifier.width(8.dp))

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
                        onClick = {
                            refreshKey++
                        }
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

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Khám phá sản phẩm",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Danh sách sản phẩm tại SmartMart",
                fontSize = 13.sp,
                color = TextGray
            )

            Spacer(Modifier.height(16.dp))

            // SWA-17: Ô tìm kiếm
            OutlinedTextField(
                value = keyword,
                onValueChange = {
                    keyword = it
                    validationMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Tìm kiếm sản phẩm")
                },
                placeholder = {
                    Text("Nhập tên sản phẩm hoặc SKU")
                },
                singleLine = true,
                leadingIcon = {
                    Text("⌕", fontSize = 22.sp)
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(10.dp))

            // SWA-17: Bộ lọc giá
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = minPriceText,
                    onValueChange = {
                        minPriceText = it
                        validationMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    label = {
                        Text("Giá từ")
                    },
                    placeholder = {
                        Text("0")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = maxPriceText,
                    onValueChange = {
                        maxPriceText = it
                        validationMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    label = {
                        Text("Giá đến")
                    },
                    placeholder = {
                        Text("100000")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (validationMessage != null) {
                Spacer(Modifier.height(6.dp))

                Text(
                    text = validationMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // SWA-17: Nút tìm kiếm và xóa lọc
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = {
                        val minPrice = minPriceText
                            .trim()
                            .takeIf { it.isNotEmpty() }
                            ?.toDoubleOrNull()

                        val maxPrice = maxPriceText
                            .trim()
                            .takeIf { it.isNotEmpty() }
                            ?.toDoubleOrNull()

                        val invalidMin =
                            minPriceText.isNotBlank() &&
                                    minPrice == null

                        val invalidMax =
                            maxPriceText.isNotBlank() &&
                                    maxPrice == null

                        validationMessage = when {
                            invalidMin || invalidMax ->
                                "Giá phải là số hợp lệ"

                            minPrice != null && minPrice < 0 ->
                                "Giá không được âm"

                            maxPrice != null && maxPrice < 0 ->
                                "Giá không được âm"

                            minPrice != null &&
                                    maxPrice != null &&
                                    minPrice > maxPrice ->
                                "Giá từ không được lớn hơn giá đến"

                            else -> null
                        }

                        if (validationMessage == null) {
                            appliedKeyword = keyword.trim()
                            appliedMinPrice = minPrice
                            appliedMaxPrice = maxPrice
                            isFilterApplied = true
                            refreshKey++
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmartGreen
                    )
                ) {
                    Text("Tìm kiếm")
                }

                OutlinedButton(
                    onClick = {
                        keyword = ""
                        minPriceText = ""
                        maxPriceText = ""

                        appliedKeyword = ""
                        appliedMinPrice = null
                        appliedMaxPrice = null
                        isFilterApplied = false

                        validationMessage = null
                        refreshKey++
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Xóa lọc",
                        color = DarkGreen
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Hiển thị trạng thái dữ liệu
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
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.Center
                    ) {
                        Text(
                            text = "Không thể tải sản phẩm",
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = errorMessage ?: "",
                            color = TextGray
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                refreshKey++
                            },
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
                            text = if (isFilterApplied) {
                                "Không tìm thấy sản phẩm phù hợp"
                            } else {
                                "Hiện chưa có sản phẩm nào"
                            },
                            color = TextGray
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Hiển thị ${products.size} sản phẩm",
                        fontSize = 13.sp,
                        color = DarkGreen,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            bottom = 24.dp
                        )
                    ) {
                        items(
                            items = products,
                            key = { it.productId }
                        ) { product ->

                            // SWA-36: Nhấn để mở chi tiết
                            ProductCard(
                                product = product,
                                onClick = {
                                    selectedProductId =
                                        product.productId
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
