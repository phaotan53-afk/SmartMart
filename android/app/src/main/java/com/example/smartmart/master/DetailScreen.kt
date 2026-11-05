package com.example.smartmart.master

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus
import com.example.smartmart.viewmodel.ProductDetailUiState

/**
 * Phụ trách: Lê Tuấn Thành — FE/Integration; PRODUCT_DETAIL, SWA-36 theo Sprint.
 * Nguồn: UI bổ sung trong đợt hợp nhất, có hỗ trợ AI; giữ trạng thái và repository Demo.
 * Tích hợp: nhận đúng productId; số lượng và thao tác giỏ phát event cho host.
 */
@Composable
fun DetailScreen(detail: ProductDetailUiState, state: StoreState, wide: Boolean, onRetry: () -> Unit, onEvent: (StoreEvent) -> Unit) {
    when (detail) {
        ProductDetailUiState.Loading -> Box(Modifier.fillMaxSize().testTag("detail_loading"), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ProductDetailUiState.NotFound -> EmptyState("Không tìm thấy sản phẩm.", "Về trang chủ") { onEvent(StoreEvent.Navigate(Destination.HOME)) }
        is ProductDetailUiState.Error -> EmptyState(detail.message, "Thử lại", onRetry)
        is ProductDetailUiState.Success -> {
            val product = detail.data
            var tab by rememberSaveable(product.id) { mutableIntStateOf(0) }
            LazyColumn(Modifier.fillMaxSize().testTag("master_detail_success"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item { Text("Trang chủ  ›  ${product.categoryName ?: "Sản phẩm"}  ›  ${product.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            DetailImages(product, Modifier.weight(1f))
                            DetailSummary(product, state, onEvent, Modifier.weight(1f))
                        } else Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            DetailImages(product, Modifier.fillMaxWidth())
                            DetailSummary(product, state, onEvent, Modifier.fillMaxWidth())
                        }
                    }
                }
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Mô tả", "Thông số", "Đánh giá (${state.reviews.reviewCount})").forEachIndexed { index, title ->
                                TextButton(onClick = { tab = index }, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = if (tab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(title, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal) }
                            }
                        }
                        HorizontalDivider()
                        when (tab) {
                            0 -> {
                                SectionTitle("Mô tả sản phẩm")
                                Text(product.description ?: "Chưa có mô tả sản phẩm.", style = MaterialTheme.typography.bodyLarge)
                                if (wide) QuickInfo(product) else QuickInfo(product)
                            }
                            1 -> {
                                SectionTitle("Thông số sản phẩm")
                                if (product.specifications.isEmpty()) Text("Chưa có thông số bổ sung.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                product.specifications.forEach { (key, value) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) { Text(key, Modifier.weight(1f)); Text(value, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                                QuickInfo(product)
                            }
                            else -> ReviewsContent(product, state, onEvent)
                        }
                    }
                }
                if (state.related.isNotEmpty()) {
                    item { SectionTitle("Sản phẩm liên quan") }
                    item { LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.related, key = { it.id }) { related -> ProductCard(related, state.favorites.any { it.id == related.id }, state.busy, onEvent, Modifier.width(if (wide) 220.dp else 180.dp)) }
                    } }
                }
            }
        }
    }
}

@Composable
private fun DetailImages(product: Product, modifier: Modifier) {
    val images = remember(product) { (listOfNotNull(product.imageUrl) + product.images).distinct() }
    var selected by rememberSaveable(product.id) { mutableIntStateOf(0) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProductPhoto(images.getOrNull(selected) ?: product.imageUrl, product.name, Modifier.fillMaxWidth().aspectRatio(1.2f).testTag("master_product_image"))
        if (images.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(images.size) { index -> Surface(shape = MaterialTheme.shapes.small, border = BorderStroke(if (selected == index) 2.dp else 1.dp, if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), modifier = Modifier.size(72.dp).testTag("product_thumbnail_$index").semantics { this.selected = selected == index }.clickable { selected = index }) {
                ProductPhoto(images[index], "Ảnh ${index + 1} ${product.name}", Modifier.fillMaxSize().padding(4.dp))
            } }
        }
    }
}

@Composable
private fun DetailSummary(product: Product, state: StoreState, onEvent: (StoreEvent) -> Unit, modifier: Modifier) {
    var quantity by rememberSaveable(product.id) { mutableIntStateOf(1) }
    val availableMax = product.quantityOnHand?.coerceIn(0, 999) ?: 999
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(product.name, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onEvent(StoreEvent.Favorite(product.id)) }) { Icon(if (state.favorites.any { it.id == product.id }) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Yêu thích", tint = MaterialTheme.colorScheme.primary) }
        }
        if (state.reviews.reviewCount > 0 && state.reviews.averageRating != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Star, null, tint = androidx.compose.ui.graphics.Color(0xFFF59E0B)); Text("${state.reviews.averageRating.stripTrailingZeros().toPlainString()} (${state.reviews.reviewCount} đánh giá)")
        } else Text("Chưa có đánh giá", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(money(product.price, product.unit), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        product.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
        SuggestionChip(onClick = {}, label = { Text(stockText(product.stockStatus)) }, icon = { Icon(Icons.Outlined.Inventory2, null, Modifier.size(18.dp)) })
        Text("Số lượng${product.unit?.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}", fontWeight = FontWeight.SemiBold)
        QuantitySelector(quantity, availableMax, !state.busy && product.stockStatus == StockStatus.IN_STOCK) { quantity = it }
        Button(onClick = { onEvent(StoreEvent.Add(product, quantity)) }, enabled = !state.busy && product.stockStatus == StockStatus.IN_STOCK && availableMax > 0,
            shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("master_detail_add")) {
            Icon(Icons.Outlined.ShoppingCart, null); Spacer(Modifier.width(8.dp)); Text("Thêm vào giỏ hàng")
        }
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.primary); Column { Text("Thanh toán COD", fontWeight = FontWeight.SemiBold); Text("Thanh toán khi nhận hàng. Phí vận chuyển hiển thị khi đặt đơn.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary); Text("Thông tin giao hàng và đổi trả theo chính sách được cửa hàng xác nhận.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun QuantitySelector(quantity: Int, max: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Surface(shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(quantity - 1) }, enabled = enabled && quantity > 1) { Icon(Icons.Outlined.Remove, "Giảm số lượng") }
            Text(quantity.toString(), Modifier.widthIn(min = 36.dp).testTag("quantity_value"), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onChange(quantity + 1) }, enabled = enabled && quantity < max.coerceAtMost(999)) { Icon(Icons.Outlined.Add, "Tăng số lượng") }
        }
    }
}

@Composable
private fun QuickInfo(product: Product) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Thông tin nhanh", fontWeight = FontWeight.Bold)
            product.categoryName?.let { Text("Danh mục: $it") }
            product.unit?.takeIf(String::isNotBlank)?.let { Text("Đơn vị tính: $it") }
            product.brand?.let { Text("Thương hiệu: $it") }
            product.quantityOnHand?.let { Text("Tồn kho: $it${product.unit?.let { u -> " $u" }.orEmpty()}") }
            Text(stockText(product.stockStatus))
        }
    }
}

@Composable
private fun ReviewsContent(product: Product, state: StoreState, onEvent: (StoreEvent) -> Unit) {
    SectionTitle("Đánh giá sản phẩm")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        item { FilterChip(state.reviewRating == null, { onEvent(StoreEvent.Reviews(null)) }, label = { Text("Tất cả") }) }
        (5 downTo 1).forEach { rating -> item { FilterChip(state.reviewRating == rating, { onEvent(StoreEvent.Reviews(rating)) }, label = { Text("$rating★") }) } }
    }
    if (state.reviews.items.isEmpty()) Text("Chưa có đánh giá phù hợp.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    state.reviews.items.forEach { review -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${review.customerName} · ${review.rating}/5", fontWeight = FontWeight.SemiBold)
        review.comment?.let { Text(it) }; Text(review.createdAt.take(10), style = MaterialTheme.typography.bodySmall); HorizontalDivider()
    } }
    Pagination(state.reviews.page, state.reviews.totalPages, !state.busy) { onEvent(StoreEvent.Reviews(state.reviewRating, it)) }
    if (state.user == null) OutlinedButton(onClick = { onEvent(StoreEvent.Navigate(Destination.ACCOUNT)) }) { Text("Đăng nhập để viết đánh giá") }
    else {
        var orderId by rememberSaveable(product.id) { mutableStateOf(state.selectedOrder?.takeIf { it.orderStatus == "DELIVERED" && it.items.any { item -> item.productId == product.id } }?.id?.toString().orEmpty()) }
        var rating by rememberSaveable(product.id) { mutableIntStateOf(5) }
        var comment by rememberSaveable(product.id) { mutableStateOf("") }
        Text("Viết đánh giá sau khi nhận hàng", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(orderId, { orderId = it.filter(Char::isDigit) }, label = { Text("Mã số đơn hàng đã giao") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("review_order"), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
        Row { (1..5).forEach { value -> IconButton(onClick = { rating = value }) { Icon(if (value <= rating) Icons.Outlined.Star else Icons.Outlined.StarOutline, "$value sao", tint = androidx.compose.ui.graphics.Color(0xFFF59E0B)) } } }
        OutlinedTextField(comment, { comment = it.take(1000) }, label = { Text("Nhận xét") }, modifier = Modifier.fillMaxWidth().testTag("review_comment"), minLines = 2)
        Button(onClick = { orderId.toLongOrNull()?.let { onEvent(StoreEvent.ReviewProduct(product.id, it, rating, comment)) } }, enabled = !state.busy && orderId.toLongOrNull() != null, modifier = Modifier.testTag("review_submit")) { Text("Gửi đánh giá") }
    }
}
