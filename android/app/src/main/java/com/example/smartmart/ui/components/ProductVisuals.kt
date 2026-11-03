
package com.example.smartmart.ui.components

import androidx.compose.ui.graphics.Color

fun getProductEmoji(productName: String): String {
    val name = productName.lowercase()

    return when {
        "sữa" in name || "sua" in name -> "🥛"
        "mì" in name || "mi " in name -> "🍜"
        "nước" in name || "nuoc" in name -> "💧"
        "gạo" in name || "gao" in name -> "🍚"
        "bánh quy" in name -> "🍪"
        "bánh mì" in name -> "🍞"
        "trứng" in name -> "🥚"
        "dầu ăn" in name -> "🫒"
        "đường" in name -> "🍬"
        "cà phê" in name -> "☕"
        "trà" in name -> "🍵"
        "rau" in name -> "🥬"
        "thịt" in name -> "🥩"
        "cá" in name -> "🐟"
        "táo" in name -> "🍎"
        "chuối" in name -> "🍌"
        else -> "🛍️"
    }
}

fun getProductIconBackground(productName: String): Color {
    return when (getProductEmoji(productName)) {
        "🥛", "💧" -> Color(0xFFEFF6FF)
        "🍜", "🍞" -> Color(0xFFFFF7ED)
        "🍚", "🥬" -> Color(0xFFF0FDF4)
        "🍪", "🍬" -> Color(0xFFFFFBEB)
        "☕", "🍵" -> Color(0xFFFEF3C7)
        else -> Color(0xFFF3F4F6)
    }
}
