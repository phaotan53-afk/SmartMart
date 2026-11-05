
package com.example.smartmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.example.smartmart.ui.theme.SmartMartTheme

data class Product(
    val productId: Long,
    val productName: String,
    val sku: String,
    val unit: String,
    val price: Double,
    val isActive: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && timber.log.Timber.treeCount == 0) timber.log.Timber.plant(timber.log.Timber.DebugTree())
        // Bổ sung tích hợp: đăng ký network fetcher rõ ràng; ảnh dùng client không gắn Bearer.
        coil3.SingletonImageLoader.setSafe { context ->
            coil3.ImageLoader.Builder(context)
                .components { add(coil3.network.okhttp.OkHttpNetworkFetcherFactory()) }
                .build()
        }
        enableEdgeToEdge()

        setContent {
            SmartMartTheme {
                SmartMartScreen()
            }
        }
    }
}

/**
 * Lê Tuấn Thành — bổ sung tích hợp: chuyển gọi HTTP trực tiếp trong Composable sang
 * StoreRoot → ViewModel → Repository. Giữ Activity, package, model và theme gốc của nhóm.
 * UI/catalog/checkout mới: bổ sung đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 */
@Composable
fun SmartMartScreen() {
    com.example.smartmart.master.StoreRoot()
}
