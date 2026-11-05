@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.smartmart.master

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.smartmart.BuildConfig
import com.example.smartmart.domain.model.Product
import com.example.smartmart.domain.model.StockStatus

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI. */
@Composable
fun HomeScreen(state: StoreState, wide: Boolean, onEvent: (StoreEvent) -> Unit) {
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (wide) Column(Modifier.width(248.dp).fillMaxHeight().padding(start = 16.dp, top = 16.dp).verticalScroll(rememberScrollState())) {
            FiltersPanel(state, onEvent) { }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val columns = when { maxWidth < 340.dp -> 1; maxWidth >= 1100.dp -> 4; maxWidth >= 650.dp -> 3; else -> 2 }
            LazyColumn(Modifier.fillMaxSize().testTag("catalog_list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { HeroBanner(wide) { onEvent(StoreEvent.Query(state.query.copy(query = "", page = 0))); } }
                item { BenefitsRow(wide) }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) { SectionTitle("Sản phẩm nổi bật"); Text("${state.catalog.totalItems} sản phẩm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (!wide) IconButton(onClick = { filterOpen = true }) { Icon(Icons.Outlined.Tune, "Mở bộ lọc") }
                        SortMenu(state.query, onEvent)
                    }
                }
                when {
                    state.catalogLoading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    state.catalogError != null -> item { EmptyState(checkNotNull(state.catalogError), "Thử lại") { onEvent(StoreEvent.Retry) } }
                    state.catalog.items.isEmpty() -> item { EmptyState("Không có sản phẩm phù hợp.", "Xóa bộ lọc") { onEvent(StoreEvent.Query(CatalogQuery())) } }
                    else -> {
                        state.catalog.items.chunked(columns).forEach { row ->
                            item(key = row.joinToString { it.id.toString() }) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { product -> ProductCard(product, state.favorites.any { it.id == product.id }, state.busy, onEvent, Modifier.weight(1f)) }
                                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
                item { Pagination(state.query.page, state.catalog.totalPages, enabled = !state.catalogLoading) { onEvent(StoreEvent.Query(state.query.copy(page = it))) } }
            }
        }
    }
    if (filterOpen) ModalBottomSheet(onDismissRequest = { filterOpen = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding()) { FiltersPanel(state, onEvent) { filterOpen = false } }
    }
}

@Composable
private fun HeroBanner(wide: Boolean, onShop: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(if (wide) 24.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(if (wide) 1f else 1.3f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mua sắm thực phẩm\ntươi ngon, tiện lợi tại\nSmartMart", style = if (wide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                if (wide) Text("Khám phá sản phẩm và chọn hàng cho gia đình bạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onShop, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Text("Mua sắm ngay"); Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.ArrowForward, null, Modifier.size(18.dp)) }
            }
            AsyncImage(model = BuildConfig.API_BASE_URL.trimEnd('/') + "/assets/products/hero.jpg", contentDescription = "Rau quả tươi trong giỏ", contentScale = ContentScale.Crop,
                onError = { timber.log.Timber.w("Hero image failed: %s", it.result.throwable.javaClass.simpleName) },
                modifier = Modifier.weight(1f).height(if (wide) 200.dp else 160.dp).clip(RoundedCornerShape(12.dp)))
        }
    }
}

@Composable
private fun BenefitsRow(wide: Boolean) {
    Panel(Modifier.fillMaxWidth()) {
        val labels = listOf(Triple(Icons.Outlined.ShoppingBasket, "Mua sắm tiện lợi", "Lựa chọn theo nhu cầu"), Triple(Icons.Outlined.Inventory2, "Theo dõi đơn hàng", "Trạng thái từ hệ thống"), Triple(Icons.Outlined.Payments, "Thanh toán COD", "Trả tiền khi nhận hàng"))
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { labels.forEach { (icon, title, subtitle) ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Column { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall) } }
        } } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { labels.forEach { (icon, title, _) -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 2) } } }
    }
}

@Composable
internal fun ProductCard(product: Product, favorite: Boolean, busy: Boolean, onEvent: (StoreEvent) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.testTag("product_${product.id}"), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().clickable { onEvent(StoreEvent.OpenProduct(product.id)) }) {
                ProductPhoto(product.imageUrl, product.name, Modifier.fillMaxWidth().aspectRatio(1.25f))
                IconButton(onClick = { onEvent(StoreEvent.Favorite(product.id)) }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, if (favorite) "Bỏ yêu thích ${product.name}" else "Yêu thích ${product.name}", tint = MaterialTheme.colorScheme.primary) }
            }
            Text(product.name, Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { onEvent(StoreEvent.OpenProduct(product.id)) }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(product.description ?: product.categoryName.orEmpty(), Modifier.heightIn(min = 34.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(money(product.price, product.unit), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            if (product.stockStatus != StockStatus.IN_STOCK) Text(stockText(product.stockStatus), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Button(onClick = { onEvent(StoreEvent.Add(product, 1)) }, enabled = !busy && product.stockStatus == StockStatus.IN_STOCK,
                shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Thêm vào giỏ", maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
internal fun ProductPhoto(url: String?, name: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val placeholder = androidx.compose.ui.res.painterResource(com.example.smartmart.R.drawable.product_image_placeholder)
    var imageState by remember(url) { mutableStateOf("Đang tải ảnh") }
    AsyncImage(model = StoreGraph.image(url), contentDescription = name, placeholder = placeholder, error = placeholder, fallback = placeholder,
        onSuccess = { imageState = "Ảnh sản phẩm đã tải" },
        onError = { imageState = "Ảnh chưa sẵn sàng"; timber.log.Timber.w("Product image failed: %s", it.result.throwable.javaClass.simpleName) },
        contentScale = contentScale, modifier = modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surface).semantics { stateDescription = imageState })
}

@Composable
internal fun FiltersPanel(state: StoreState, onEvent: (StoreEvent) -> Unit, onApplied: () -> Unit) {
    var min by rememberSaveable(state.query.minPrice) { mutableStateOf(state.query.minPrice) }
    var max by rememberSaveable(state.query.maxPrice) { mutableStateOf(state.query.maxPrice) }
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle("Danh mục sản phẩm")
        TextButton(onClick = { onEvent(StoreEvent.Query(state.query.copy(categoryId = null, page = 0))); onApplied() }, modifier = Modifier.fillMaxWidth()) { Text("Tất cả danh mục") }
        state.filters.categories.forEach { category ->
            FilterChip(selected = state.query.categoryId == category.id, onClick = { onEvent(StoreEvent.Query(state.query.copy(categoryId = category.id, page = 0))); onApplied() }, label = { Text(category.name) }, modifier = Modifier.fillMaxWidth())
        }
        HorizontalDivider(); SectionTitle("Bộ lọc")
        Text("Mức giá (đ)", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(min, { min = it.filter(Char::isDigit) }, label = { Text("Từ") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("filter_min"), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
        OutlinedTextField(max, { max = it.filter(Char::isDigit) }, label = { Text("Đến") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("filter_max"), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
        Button(onClick = { onEvent(StoreEvent.Query(state.query.copy(minPrice = min, maxPrice = max, page = 0))); onApplied() }, modifier = Modifier.fillMaxWidth().testTag("filter_apply_price")) { Text("Áp dụng giá") }
        if (state.filters.brands.isNotEmpty()) {
            HorizontalDivider(); Text("Thương hiệu", fontWeight = FontWeight.SemiBold)
            state.filters.brands.forEach { brand -> FilterChip(state.query.brand == brand, { onEvent(StoreEvent.Query(state.query.copy(brand = if (state.query.brand == brand) null else brand, page = 0))); onApplied() }, label = { Text(brand) }) }
        }
        HorizontalDivider(); Text("Đánh giá tối thiểu", fontWeight = FontWeight.SemiBold)
        (5 downTo 1).forEach { rating -> Row(Modifier.fillMaxWidth().clickable { onEvent(StoreEvent.Query(state.query.copy(minRating = if (state.query.minRating == rating) null else rating, page = 0))); onApplied() }, verticalAlignment = Alignment.CenterVertically) { Checkbox(state.query.minRating == rating, null); Text("$rating sao trở lên") } }
        HorizontalDivider(); Text("Tình trạng", fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().clickable { onEvent(StoreEvent.Query(state.query.copy(inStock = if (state.query.inStock == true) null else true, page = 0))); onApplied() }, verticalAlignment = Alignment.CenterVertically) { Checkbox(state.query.inStock == true, null); Text("Còn hàng") }
        Row(Modifier.fillMaxWidth().clickable { onEvent(StoreEvent.Query(state.query.copy(inStock = if (state.query.inStock == false) null else false, page = 0))); onApplied() }, verticalAlignment = Alignment.CenterVertically) { Checkbox(state.query.inStock == false, null); Text("Hết hàng") }
        OutlinedButton(onClick = { onEvent(StoreEvent.Query(CatalogQuery())); onApplied() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Xóa bộ lọc") }
    }
}

@Composable
private fun SortMenu(query: CatalogQuery, onEvent: (StoreEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sorts = linkedMapOf("name" to "Tên A–Z", "price_asc" to "Giá tăng", "price_desc" to "Giá giảm", "newest" to "Mới nhất")
    Box { OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(sorts[query.sort] ?: "Sắp xếp", style = MaterialTheme.typography.labelMedium); Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp)) }
        DropdownMenu(expanded, { expanded = false }) { sorts.forEach { (key, title) -> DropdownMenuItem(text = { Text(title) }, onClick = { expanded = false; onEvent(StoreEvent.Query(query.copy(sort = key, page = 0))) }) } }
    }
}
@Composable
internal fun Pagination(page: Int, totalPages: Int, enabled: Boolean = true, onPage: (Int) -> Unit) {
    if (totalPages > 1) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onPage(page - 1) }, enabled = enabled && page > 0) { Icon(Icons.Outlined.ChevronLeft, "Trang trước") }
        Text("${page + 1} / $totalPages", Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = { onPage(page + 1) }, enabled = enabled && page + 1 < totalPages) { Icon(Icons.Outlined.ChevronRight, "Trang sau") }
    }
}
internal fun stockText(status: StockStatus): String = when (status) { StockStatus.IN_STOCK -> "Còn hàng"; StockStatus.OUT_OF_STOCK -> "Hết hàng"; StockStatus.UNAVAILABLE -> "Chưa thể mua sản phẩm này" }
