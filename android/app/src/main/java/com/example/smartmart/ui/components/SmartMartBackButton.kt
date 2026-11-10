
package com.example.smartmart.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val SmartGreen = Color(0xFF16A34A)

@Composable
fun SmartMartBackButton(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = SmartGreen
        )
    ) {
        // Dùng ký tự mũi tên để không cần
        // cài thêm thư viện Material Icons.
        androidx.compose.material3.Text(
            text = "←",
            color = SmartGreen,
            style = androidx.compose.material3.MaterialTheme
                .typography.headlineMedium
        )
    }
}
