
package com.example.smartmart.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun formatPrice(price: Double): String {

    val symbols = DecimalFormatSymbols(
        Locale("vi", "VN")
    ).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    val formatter = DecimalFormat("#,##0", symbols)

    return "${formatter.format(price)} đ"
}
