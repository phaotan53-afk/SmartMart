
package com.example.smartmart.ui.warehouse

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SmartGreen = Color(0xFF16A34A)
private val Gray = Color(0xFF94A3B8)

@Composable
fun WarehouseBottomBar() {

    val tabs = listOf(
        "▦" to "Tổng quan",
        "▣" to "Sản phẩm",
        "▤" to "Kho",
        "⚙" to "Cài đặt"
    )

    NavigationBar(
        containerColor = Color.White
    ) {
        tabs.forEachIndexed { index, tab ->

            val isProductTab = index == 1

            NavigationBarItem(
                selected = isProductTab,
                enabled = isProductTab,
                onClick = {
                    // Đang ở màn hình Sản phẩm
                },
                icon = {
                    Text(
                        text = tab.first,
                        fontSize = 23.sp,
                        color = if (isProductTab) {
                            SmartGreen
                        } else {
                            Gray
                        }
                    )
                },
                label = {
                    Text(
                        text = tab.second,
                        fontSize = 10.sp,
                        fontWeight = if (isProductTab) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SmartGreen,
                    selectedTextColor = SmartGreen,
                    indicatorColor = Color(0xFFDCFCE7),
                    disabledIconColor = Gray,
                    disabledTextColor = Gray
                )
            )
        }
    }
}
