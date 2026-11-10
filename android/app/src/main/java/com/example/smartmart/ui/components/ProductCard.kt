
package com.example.smartmart.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmart.data.model.Product
import java.text.NumberFormat
import java.util.Locale

private val SmartGreen = Color(0xFF16A34A)
private val LightBorder = Color(0xFFE5E7EB)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)



@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit = {}
) {

    val formattedPrice = NumberFormat
        .getNumberInstance(Locale.forLanguageTag("vi-VN"))
        .format(product.price)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = LightBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Hình minh họa sản phẩm
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = getProductIconBackground(product.productName),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getProductEmoji(product.productName),
                    fontSize = 26.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = product.productName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Mã: ${product.sku}",
                    fontSize = 12.sp,
                    color = GrayText
                )

                Text(
                    text = "$formattedPrice đ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SmartGreen
                )
            }

            Text(
                text = "›",
                fontSize = 26.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
