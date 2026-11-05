@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.smartmart.master

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmart.integration.LocalDevelopmentAccess
import com.example.smartmart.viewmodel.ProductDetailUiState
import com.example.smartmart.viewmodel.ProductDetailViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Phụ trách: Lê Tuấn Thành — FE/Integration.
 * Chức năng: PRODUCT_DETAIL; SWA-36 theo tài liệu Sprint, liên kết danh sách và giỏ.
 * Nguồn: host mới có hỗ trợ AI, tái sử dụng ViewModel/repository detail từ Demo.
 * Giữ MainActivity/theme/applicationId của nhóm; UI nhận state và phát event.
 */
@Composable
fun StoreRoot() {
    LocalDevelopmentAccess {
        val store: StoreViewModel = viewModel(factory = StoreViewModel.Factory)
        val state by store.state.collectAsStateWithLifecycle()
        BackHandler(enabled = state.destination != Destination.HOME) { store.back() }
        val snackbar = remember { SnackbarHostState() }
        // Không để thông báo của màn trước che nút lưu/xác nhận trên màn mới.
        LaunchedEffect(state.destination) { snackbar.currentSnackbarData?.dismiss() }
        LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); store.messageConsumed() } }
        if (state.destination == Destination.DETAIL && state.selectedProductId != null) {
            val id = checkNotNull(state.selectedProductId)
            val detail: ProductDetailViewModel = viewModel(key = "selected-product-detail", factory = ProductDetailViewModel.factory(id, StoreGraph.detail))
            val detailState by detail.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(id) { detail.loadProduct(id) }
            StoreApp(state, detailState, store::dispatch, detail::retry, snackbar)
        } else StoreApp(state, ProductDetailUiState.Loading, store::dispatch, {}, snackbar)
    }
}

/** Pure product UI. Width changes layout, never business data or identity. */
@Composable
fun StoreApp(
    state: StoreState, detail: ProductDetailUiState, onEvent: (StoreEvent) -> Unit,
    onDetailRetry: () -> Unit, snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = { StoreHeader(state, wide, onEvent) },
            bottomBar = { if (!wide) StoreNavigation(state, onEvent) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.pageError?.let { ErrorStrip(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                when (state.destination) {
                    Destination.HOME -> HomeScreen(state, wide, onEvent)
                    Destination.DETAIL -> DetailScreen(detail, state, wide, onDetailRetry, onEvent)
                    Destination.CART -> CartScreen(state, onEvent)
                    Destination.ACCOUNT -> AccountScreen(state, onEvent)
                    Destination.CHECKOUT -> CheckoutScreen(state, onEvent)
                    Destination.ORDERS -> OrdersScreen(state, onEvent)
                    Destination.ORDER_DETAIL -> OrderScreen(state, onEvent)
                    Destination.FAVORITES -> FavoriteScreen(state, onEvent)
                    Destination.MANAGEMENT -> ManagementScreen(state, onEvent)
                }
            }
        }
    }
}

@Composable
private fun StoreHeader(state: StoreState, wide: Boolean, onEvent: (StoreEvent) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column(Modifier.statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.destination != Destination.HOME) IconButton(onClick = { onEvent(StoreEvent.Back) }) { Icon(Icons.Outlined.ArrowBack, "Quay lại") }
                TextButton(onClick = { onEvent(StoreEvent.Navigate(Destination.HOME)) }) {
                    if (wide) { Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(28.dp)); Spacer(Modifier.width(8.dp)) }
                    Text("SmartMart", style = if (wide) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (wide) SearchField(state.query, onEvent, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                if (wide || state.destination == Destination.HOME) IconButton(onClick = { onEvent(StoreEvent.Navigate(Destination.FAVORITES)) }) { Icon(Icons.Outlined.FavoriteBorder, "Yêu thích") }
                IconButton(onClick = { onEvent(StoreEvent.Navigate(Destination.CART)) }, modifier = Modifier.testTag("open_cart")) {
                    BadgedBox(badge = { if (state.cart.itemCount > 0) Badge { Text(state.cart.itemCount.toString()) } }) { Icon(Icons.Outlined.ShoppingCart, "Giỏ hàng") }
                }
                IconButton(onClick = { onEvent(StoreEvent.Navigate(Destination.ACCOUNT)) }) { Icon(Icons.Outlined.PersonOutline, "Tài khoản") }
            }
            if (!wide && state.destination == Destination.HOME) SearchField(state.query, onEvent, Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp))
            if (wide) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                NavText("Trang chủ", Icons.Outlined.Home, state.destination == Destination.HOME) { onEvent(StoreEvent.Navigate(Destination.HOME)) }
                NavText("Đơn hàng của tôi", Icons.Outlined.Inventory2, state.destination in setOf(Destination.ORDERS, Destination.ORDER_DETAIL)) { onEvent(StoreEvent.Navigate(Destination.ORDERS)) }
                NavText("Yêu thích", Icons.Outlined.FavoriteBorder, state.destination == Destination.FAVORITES) { onEvent(StoreEvent.Navigate(Destination.FAVORITES)) }
                if (state.user?.role in setOf("ADMIN", "WAREHOUSE_MANAGER", "SHIPPER")) NavText("Quản lý", Icons.Outlined.Dashboard, state.destination == Destination.MANAGEMENT) { onEvent(StoreEvent.Navigate(Destination.MANAGEMENT)) }
                Spacer(Modifier.weight(1f)); state.user?.let { Text(it.fullName, Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SearchField(query: CatalogQuery, onEvent: (StoreEvent) -> Unit, modifier: Modifier) {
    var text by rememberSaveable(query.query) { mutableStateOf(query.query) }
    OutlinedTextField(value = text, onValueChange = { text = it }, modifier = modifier.testTag("catalog_search"), singleLine = true,
        placeholder = { Text("Tìm sản phẩm, danh mục…") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = { IconButton(onClick = { onEvent(StoreEvent.Query(query.copy(query = text, page = 0))); onEvent(StoreEvent.Navigate(Destination.HOME)) }) { Icon(Icons.Outlined.ArrowForward, "Tìm kiếm") } },
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onEvent(StoreEvent.Query(query.copy(query = text, page = 0))); onEvent(StoreEvent.Navigate(Destination.HOME)) }),
    )
}

@Composable
private fun StoreNavigation(state: StoreState, onEvent: (StoreEvent) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val items = listOf(Triple(Destination.HOME, "Trang chủ", Icons.Outlined.Home), Triple(Destination.ORDERS, "Đơn hàng", Icons.Outlined.Inventory2),
            Triple(Destination.CART, "Giỏ hàng", Icons.Outlined.ShoppingCart), Triple(Destination.ACCOUNT, "Tài khoản", Icons.Outlined.PersonOutline))
        items.forEach { (destination, label, icon) ->
            NavigationBarItem(selected = state.destination == destination, onClick = { onEvent(StoreEvent.Navigate(destination)) }, icon = { Icon(icon, null) }, label = { Text(label, maxLines = 1) })
        }
    }
}

@Composable
private fun NavText(text: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)) {
        Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(text)
    }
}

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
internal fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) }
@Composable
internal fun ErrorStrip(message: String, modifier: Modifier = Modifier) {
    Text(message, modifier, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}
@Composable
internal fun EmptyState(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Outlined.ShoppingBasket, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(message); if (action != null) Button(onClick = onAction) { Text(action) }
    }
}
internal fun money(value: BigDecimal, unit: String? = null): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).apply { roundingMode = java.math.RoundingMode.HALF_UP }.format(value) + "đ" + (unit?.takeIf(String::isNotBlank)?.let { "/$it" } ?: "")
