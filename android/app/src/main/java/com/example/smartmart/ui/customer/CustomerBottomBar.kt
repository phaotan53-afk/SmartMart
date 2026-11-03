
package com.example.smartmart.ui.customer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SmartGreen = Color(0xFF16A34A)
private val Gray = Color(0xFF94A3B8)

@Composable
fun CustomerBottomBar() {

    val tabs = listOf(
        "⌂" to "Sản phẩm",
        "▦" to "Danh mục",
        "🛒" to "Giỏ hàng",
        "♙" to "Tài khoản"
    )

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.White
    ) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = index == 0,
                onClick = {
                    // Chưa điều hướng sang màn hình khác
                },
                icon = {
                    Text(
                        text = tab.first,
                        fontSize = 23.sp,
                        color = if (index == 0) SmartGreen else Gray
                    )
                },
                label = {
                    Text(
                        text = tab.second,
                        fontSize = 10.sp,
                        fontWeight = if (index == 0) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            )
        }
    }
}
