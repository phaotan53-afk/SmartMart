
package com.example.smartmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smartmart.ui.product.ProductListRoute
import com.example.smartmart.ui.theme.SmartMartTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartMartTheme {
                ProductListRoute()
            }
        }
    }
}
