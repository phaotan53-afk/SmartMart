
package com.example.smartmart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.smartmart.ui.customer.CustomerProductScreen
import com.example.smartmart.ui.warehouse.WarehouseProductScreen

private val SmartGreen = Color(0xFF16A34A)
private val Background = Color(0xFFF8FAF9)
private val Border = Color(0xFFE5E7EB)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF64748B)

// Chỉ dùng cho demo giao diện.
// Không phải phân quyền người dùng thật.
private enum class DemoRole {
    CUSTOMER,
    WAREHOUSE
}

@Composable
fun SmartMartApp() {

    var selectedRole by remember {
        mutableStateOf<DemoRole?>(null)
    }

    // Khi đang xem một vai trò, nhấn Back
    // để quay lại màn hình chọn vai trò.
    BackHandler(enabled = selectedRole != null) {
        selectedRole = null
    }

    when (selectedRole) {
        DemoRole.CUSTOMER -> {
            CustomerProductScreen(
                onBackClick = {
                    selectedRole = null
                }
            )
        }

        DemoRole.WAREHOUSE -> {
            WarehouseProductScreen(
                onBackClick = {
                    selectedRole = null
                }
            )
        }

        null -> {
            RoleSelectionScreen(
                onCustomerClick = {
                    selectedRole = DemoRole.CUSTOMER
                },
                onWarehouseClick = {
                    selectedRole = DemoRole.WAREHOUSE
                }
            )
        }
    }
}

@Composable
private fun RoleSelectionScreen(
    onCustomerClick: () -> Unit,
    onWarehouseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Color(0xFFDCFCE7),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🛒",
                fontSize = 36.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "SmartMart",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = SmartGreen
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Hệ thống quản lý siêu thị thông minh",
            fontSize = 13.sp,
            color = GrayText
        )

        Spacer(Modifier.height(36.dp))

        Text(
            text = "Chọn giao diện trải nghiệm",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Lựa chọn vai trò để xem danh sách sản phẩm",
            fontSize = 13.sp,
            color = GrayText
        )

        Spacer(Modifier.height(24.dp))

        DemoRoleCard(
            emoji = "🛍️",
            title = "Khách hàng",
            description = "Xem danh sách và giá sản phẩm",
            iconBackground = Color(0xFFEFF6FF),
            onClick = onCustomerClick
        )

        Spacer(Modifier.height(14.dp))

        DemoRoleCard(
            emoji = "🏬",
            title = "Quản lý kho",
            description = "Xem SKU, giá và trạng thái sản phẩm",
            iconBackground = Color(0xFFF0FDF4),
            onClick = onWarehouseClick
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = "CHẾ ĐỘ DEMO • CHƯA XÁC THỰC VAI TRÒ",
            fontSize = 10.sp,
            color = GrayText
        )
    }
}

@Composable
private fun DemoRoleCard(
    emoji: String,
    title: String,
    description: String,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Border,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        iconBackground,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 26.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = GrayText
                )
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
