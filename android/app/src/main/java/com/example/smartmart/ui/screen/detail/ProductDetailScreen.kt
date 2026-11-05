package com.example.smartmart.ui.screen.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.smartmart.R
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus
import com.example.smartmart.ui.theme.SmartMartTheme
import com.example.smartmart.viewmodel.ProductDetailUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** SWA-36 — Lê Tuấn Thành. Pure UI: state in, user actions out; no API or ViewModel access. */
@Composable
fun ProductDetailScreen(
    state: ProductDetailUiState,
    onRetry: () -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailHeader(onNavigateBack) },
        bottomBar = {
            if (state is ProductDetailUiState.Success) {
                PurchaseBar(state.data, onAddToCart)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (state) {
                ProductDetailUiState.Loading -> LoadingContent()
                is ProductDetailUiState.Success -> ProductDetails(state.data)
                is ProductDetailUiState.Error -> ErrorContent(state.message, onRetry)
                ProductDetailUiState.NotFound -> NotFoundContent()
            }
        }
    }
}

/** Kept as a pure-content entry point for existing previews/tests and team integration. */
@Composable
fun ProductDetailContent(
    state: ProductDetailUiState,
    onRetry: () -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
) = ProductDetailScreen(state, onRetry, onAddToCart, modifier)

@Composable
private fun DetailHeader(onNavigateBack: (() -> Unit)?) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painterResource(R.drawable.ic_detail_back),
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    painterResource(R.drawable.ic_detail_cart),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.detail_brand),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun LoadingContent() {
    val description = stringResource(R.string.detail_loading)
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).testTag("detail_loading"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { contentDescription = description },
        )
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProductDetails(product: Product) {
    Column(
        modifier = Modifier.widthIn(max = 1080.dp).fillMaxSize()
            .verticalScroll(rememberScrollState()).padding(16.dp).testTag("detail_success"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.detail_breadcrumb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DetailCard {
            BoxWithConstraints {
                if (maxWidth >= 720.dp) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        ProductImage(product, Modifier.weight(1f))
                        ProductSummary(product, Modifier.weight(1f))
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        ProductImage(product, Modifier.fillMaxWidth())
                        ProductSummary(product, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        DetailCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(width = 4.dp, height = 24.dp)
                            .clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.detail_description),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    product.description?.takeIf(String::isNotBlank)
                        ?: stringResource(R.string.detail_no_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProductImage(product: Product, modifier: Modifier) {
    val placeholder = painterResource(R.drawable.product_image_placeholder)
    AsyncImage(
        model = product.imageUrl,
        contentDescription = product.name,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = ContentScale.Fit,
        modifier = modifier.aspectRatio(1.2f).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background).testTag("detail_product_image"),
    )
}

@Composable
private fun ProductSummary(product: Product, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            product.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        StockBadge(product.stockStatus)
        ProductPrice(product)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.detail_quick_info),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                InfoRow(stringResource(R.string.detail_product_code), product.id.toString())
                product.unit?.takeIf(String::isNotBlank)?.let {
                    InfoRow(stringResource(R.string.detail_unit), it)
                }
                InfoRow(stringResource(R.string.detail_stock), stockLabel(product.stockStatus))
            }
        }
        Text(
            stringResource(
                if (product.stockStatus == StockStatus.IN_STOCK) R.string.detail_one_item
                else R.string.detail_unavailable_hint,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StockBadge(stock: StockStatus) {
    val available = stock == StockStatus.IN_STOCK
    Surface(
        shape = CircleShape,
        color = if (available) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
        contentColor = if (available) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(6.dp).background(if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
            Text(stockLabel(stock), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun stockLabel(stock: StockStatus): String = stringResource(
    when (stock) {
        StockStatus.IN_STOCK -> R.string.detail_in_stock
        StockStatus.OUT_OF_STOCK -> R.string.detail_out_of_stock
        StockStatus.UNAVAILABLE -> R.string.detail_unavailable
    },
)

@Composable
private fun ProductPrice(product: Product) {
    val price = remember(product.price, product.unit) { formatProductPrice(product.price, product.unit) }
    Text(price, style = MaterialTheme.typography.titleLarge, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

/** VNĐ has no fractional display; the optional unit must come from the API, never inferred. */
internal fun formatProductPrice(price: BigDecimal, unit: String?): String {
    val amount = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
        .format(price.setScale(0, RoundingMode.HALF_UP))
    return amount + "đ" + (unit?.trim()?.takeIf(String::isNotEmpty)?.let { "/$it" } ?: "")
}

@Composable
private fun PurchaseBar(product: Product, onAddToCart: (Product) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 1048.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.detail_price_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ProductPrice(product)
                }
                DetailPrimaryButton(
                    onClick = { onAddToCart(product) },
                    enabled = product.stockStatus == StockStatus.IN_STOCK,
                    modifier = Modifier.fillMaxWidth().testTag("detail_add_to_cart"),
                ) {
                    Icon(painterResource(R.drawable.ic_detail_cart), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.detail_add_to_cart))
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("detail_error"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite; error(message) },
        ) {
            Text(message, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        }
        Text(stringResource(R.string.detail_error_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        DetailPrimaryButton(onClick = onRetry, modifier = Modifier.testTag("detail_retry")) {
            Text(stringResource(R.string.detail_retry))
        }
    }
}

@Composable
private fun NotFoundContent() {
    Column(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("detail_not_found"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.detail_empty_product), contentDescription = null, modifier = Modifier.size(160.dp))
        Text(stringResource(R.string.detail_not_found), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.detail_not_found_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Shared button geometry and interaction colors from the SmartMart master design. */
@Composable
private fun DetailPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactions,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed || hovered) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.height(40.dp),
        content = content,
    )
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

private val previewProduct = Product(
    id = 1L,
    name = "Táo đỏ",
    price = BigDecimal("35000"),
    description = "Táo đỏ giòn ngọt tự nhiên. Phù hợp để ăn trực tiếp, làm salad hoặc chế biến món ăn.",
    imageUrl = null,
    stockStatus = StockStatus.IN_STOCK,
    unit = "kg",
)

@Preview(name = "Chi tiết — Đang tải", showBackground = true, locale = "vi")
@Composable
private fun LoadingPreview() = SmartMartTheme {
    ProductDetailScreen(ProductDetailUiState.Loading, {}, {})
}

@Preview(name = "Chi tiết — Thành công", showBackground = true, locale = "vi")
@Composable
private fun SuccessPreview() = SmartMartTheme {
    ProductDetailScreen(ProductDetailUiState.Success(previewProduct), {}, {}, onNavigateBack = {})
}

@Preview(name = "Chi tiết — Lỗi", showBackground = true, locale = "vi")
@Composable
private fun ErrorPreview() = SmartMartTheme {
    ProductDetailScreen(ProductDetailUiState.Error("⚠️ Không thể tải dữ liệu sản phẩm"), {}, {})
}

@Preview(name = "Chi tiết — Không tìm thấy", showBackground = true, locale = "vi")
@Composable
private fun NotFoundPreview() = SmartMartTheme {
    ProductDetailScreen(ProductDetailUiState.NotFound, {}, {}, onNavigateBack = {})
}

@Preview(name = "Chi tiết — Máy tính bảng", widthDp = 1000, heightDp = 900, showBackground = true, locale = "vi")
@Composable
private fun TabletPreview() = SmartMartTheme {
    ProductDetailScreen(ProductDetailUiState.Success(previewProduct), {}, {}, onNavigateBack = {})
}
