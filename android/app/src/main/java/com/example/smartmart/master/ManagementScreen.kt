package com.example.smartmart.master

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.math.BigDecimal

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI. */
@Composable
fun ManagementScreen(state: StoreState, onEvent: (StoreEvent) -> Unit) {
    val role = state.user?.role ?: return
    val tabs = if (role == "WAREHOUSE_MANAGER") listOf("Sản phẩm", "Danh mục", "Tồn kho") else if (role == "SHIPPER") listOf("Giao hàng", "Báo vị trí") else listOf("Sản phẩm", "Danh mục", "Tồn kho", "Đơn hàng", "Ưu đãi", "Người dùng", "Báo cáo")
    var tab by rememberSaveable(role) { mutableStateOf(tabs.first()) }
    var productEditor by remember { mutableStateOf(false) }; var editingProduct by remember { mutableStateOf<AdminProduct?>(null) }
    var categoryEditor by remember { mutableStateOf(false) }; var editingCategory by remember { mutableStateOf<Category?>(null) }
    var inventory by remember { mutableStateOf<InventoryItem?>(null) }
    var voucherEditor by remember { mutableStateOf(false) }
    var editingVoucher by remember { mutableStateOf<Voucher?>(null) }
    var userQuery by rememberSaveable { mutableStateOf("") }
    var reportFrom by rememberSaveable { mutableStateOf(java.time.LocalDate.now().minusDays(30).toString()) }
    var reportTo by rememberSaveable { mutableStateOf(java.time.LocalDate.now().toString()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { SectionTitle("Quản lý SmartMart") }; IconButton(onClick = { onEvent(StoreEvent.RefreshManagement) }) { Icon(Icons.Outlined.Refresh, "Tải lại dữ liệu quản lý") } } }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { tabs.forEach { title -> item { FilterChip(tab == title, { tab = title }, label = { Text(title) }) } } } }
        when (tab) {
            "Sản phẩm" -> {
                item { Button(onClick = { editingProduct = null; productEditor = true }) { Icon(Icons.Outlined.Add, null); Text("Thêm sản phẩm") } }
                state.management.products.forEach { product -> item(key = "product-${product.id}") { Panel(Modifier.fillMaxWidth()) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium); Text("${product.sku} · ${money(product.price, product.unit)}")
                    Text("Tồn: ${product.quantityOnHand} · ${if (product.isActive) "Đang kinh doanh" else "Ngừng kinh doanh"}")
                    TextButton(onClick = { editingProduct = product; productEditor = true }) { Text("Chỉnh sửa") }
                } } }
            }
            "Danh mục" -> {
                item { Button(onClick = { editingCategory = null; categoryEditor = true }) { Text("Thêm danh mục") } }
                state.management.categories.forEach { category -> item { Panel(Modifier.fillMaxWidth()) { Text(category.name); TextButton(onClick = { editingCategory = category; categoryEditor = true }) { Text("Chỉnh sửa") } } } }
            }
            "Tồn kho" -> state.management.inventory.forEach { stock -> item(key = stock.productId) { Panel(Modifier.fillMaxWidth()) {
                Text(stock.name, style = MaterialTheme.typography.titleMedium)
                Text("Hiện có: ${stock.quantityOnHand} · Mức cảnh báo: ${stock.reorderLevel ?: 0}")
                if (stock.lowStock) Text("Tồn kho thấp", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { inventory = stock }) { Text("Nhập / điều chỉnh / kiểm kê") }
            } } }
            "Đơn hàng" -> state.management.orders.forEach { order -> item(key = order.id) { AdminOrderCard(order, state.management.shippers, state.busy, onEvent) } }
            "Ưu đãi" -> {
                item { Button(onClick = { editingVoucher = null; voucherEditor = true }) { Text("Tạo mã ưu đãi") } }
                state.management.vouchers.forEach { voucher -> item { Panel(Modifier.fillMaxWidth()) {
                    Text(voucher.code, fontWeight = FontWeight.Bold); Text("${voucher.discountValue.stripTrailingZeros().toPlainString()} ${if (voucher.discountType == "PERCENT") "%" else "đ"}"); Text(if (voucher.isActive) "Đang bật" else "Đã tắt")
                    Row { TextButton(onClick = { editingVoucher = voucher; voucherEditor = true }) { Text("Chỉnh sửa") }; if (voucher.isActive) TextButton(onClick = { onEvent(StoreEvent.Admin("voucher_disable", voucher.id, emptyMap())) }, enabled = !state.busy) { Text("Tắt mã") } }
                } } }
            }
            "Người dùng" -> {
                item { Panel(Modifier.fillMaxWidth()) { OutlinedTextField(userQuery, { userQuery = it }, label = { Text("Tìm theo tên hoặc email") }, modifier = Modifier.fillMaxWidth()); Button(onClick = { onEvent(StoreEvent.UserSearch(userQuery)) }, enabled = !state.busy) { Text("Tìm người dùng") } } }
                state.management.users.forEach { user -> item(key = user.id) { Panel(Modifier.fillMaxWidth()) {
                    Text(user.fullName, style = MaterialTheme.typography.titleMedium); Text(user.email); Text("${user.role} · ${if (user.isActive) "Đang hoạt động" else "Đã vô hiệu hóa"}")
                    if (user.id != state.user.id) UserStatusButton(user, state.busy, onEvent)
                } } }
            }
            "Báo cáo" -> {
                item { Panel(Modifier.fillMaxWidth()) { EditField("Từ ngày (yyyy-MM-dd)", reportFrom) { reportFrom = it }; EditField("Đến ngày (yyyy-MM-dd)", reportTo) { reportTo = it }; Button(onClick = { onEvent(StoreEvent.ReportRange(reportFrom, reportTo)) }, enabled = !state.busy) { Text("Xem báo cáo") } } }
                item { ReportPanel(state.management.report) }
            }
            "Giao hàng" -> {
                if (state.management.shipments.isEmpty() && !state.busy) item { EmptyState("Bạn chưa có đơn giao được phân công.") }
                state.management.shipments.forEach { shipment -> item(key = shipment.id) { ShipmentCard(shipment, state.busy, onEvent) } }
            }
            "Báo vị trí" -> item { LocationReportPanel(state.busy, onEvent) }
        }
    }
    if (productEditor) ProductEditor(editingProduct, state.management.categories, state.busy, { productEditor = false }) { body -> onEvent(StoreEvent.Admin("product", editingProduct?.id, body)); productEditor = false }
    if (categoryEditor) CategoryEditor(editingCategory, state.busy, { categoryEditor = false }) { body -> onEvent(StoreEvent.Admin("category", editingCategory?.id, body)); categoryEditor = false }
    inventory?.let { stock -> InventoryEditor(stock, state.busy, { inventory = null }) { action, body -> onEvent(StoreEvent.Admin(action, stock.productId, body)); inventory = null } }
    if (voucherEditor) VoucherEditor(editingVoucher, state.busy, { voucherEditor = false }) { body -> onEvent(StoreEvent.Admin("voucher", editingVoucher?.id, body)); voucherEditor = false }
}

@Composable
private fun EditorDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp).padding(16.dp)) {
            Column(Modifier.padding(20.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) { SectionTitle(title); content() }
        }
    }
}

@Composable
private fun ProductEditor(product: AdminProduct?, categories: List<Category>, busy: Boolean, onDismiss: () -> Unit, onSave: (Map<String, Any?>) -> Unit) {
    var name by remember { mutableStateOf(product?.name.orEmpty()) }; var sku by remember { mutableStateOf(product?.sku.orEmpty()) }
    var unit by remember { mutableStateOf(product?.unit.orEmpty()) }; var price by remember { mutableStateOf(product?.price?.toPlainString().orEmpty()) }
    var cost by remember { mutableStateOf(product?.costPrice?.toPlainString().orEmpty()) }; var brand by remember { mutableStateOf(product?.brand.orEmpty()) }
    var description by remember { mutableStateOf(product?.description.orEmpty()) }; var image by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id) }; var active by remember { mutableStateOf(product?.isActive ?: true) }
    EditorDialog(if (product == null) "Thêm sản phẩm" else "Sửa sản phẩm", onDismiss) {
        EditField("Tên sản phẩm", name) { name = it }; EditField("SKU", sku) { sku = it }
        Choice("Danh mục", categories.associate { it.id.toString() to it.name }, category?.toString().orEmpty()) { category = it.toLongOrNull() }
        EditField("Đơn vị bán", unit) { unit = it }; EditField("Giá bán (đ)", price, true) { price = it }
        EditField("Giá vốn (đ, nội bộ)", cost, true) { cost = it }; EditField("Thương hiệu", brand) { brand = it }
        EditField("Mô tả", description) { description = it }; EditField("URL ảnh HTTPS hoặc /assets/", image) { image = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(active, { active = it }); Text("Đang kinh doanh") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(mapOf("categoryId" to category, "sku" to sku.trim(), "name" to name.trim(), "description" to description, "unit" to unit.trim(), "price" to price.toBigDecimalOrNull(), "costPrice" to cost.toBigDecimalOrNull(), "brand" to brand.takeIf(String::isNotBlank), "imageUrl" to image.takeIf(String::isNotBlank), "isActive" to active)) }, enabled = !busy && category != null && listOf(name, sku, unit).all(String::isNotBlank) && price.toBigDecimalOrNull()?.let { it >= BigDecimal.ZERO } == true) { Text("Lưu") }
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    }
}

@Composable
private fun CategoryEditor(category: Category?, busy: Boolean, onDismiss: () -> Unit, onSave: (Map<String, Any?>) -> Unit) {
    var name by remember { mutableStateOf(category?.name.orEmpty()) }; var active by remember { mutableStateOf(category?.isActive ?: true) }
    EditorDialog("Danh mục", onDismiss) { EditField("Tên danh mục", name) { name = it }; Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(active, { active = it }); Text("Đang sử dụng") }
        Button(onClick = { onSave(mapOf("name" to name.trim(), "isActive" to active)) }, enabled = !busy && name.isNotBlank()) { Text("Lưu danh mục") }; TextButton(onClick = onDismiss) { Text("Hủy") }
    }
}

@Composable
private fun InventoryEditor(stock: InventoryItem, busy: Boolean, onDismiss: () -> Unit, onSave: (String, Map<String, Any?>) -> Unit) {
    var mode by remember { mutableStateOf("IMPORT") }; var quantity by remember { mutableStateOf("") }; var reorder by remember { mutableStateOf((stock.reorderLevel ?: 0).toString()) }; var note by remember { mutableStateOf("") }
    EditorDialog("Tồn kho ${stock.name}", onDismiss) {
        Text("Hiện có: ${stock.quantityOnHand}")
        Choice("Thao tác", linkedMapOf("IMPORT" to "Nhập hàng", "ADJUSTMENT" to "Điều chỉnh tăng/giảm", "DAMAGE" to "Ghi nhận hư hỏng", "COUNT" to "Kiểm kê"), mode) { mode = it }
        EditField(if (mode == "COUNT") "Số lượng thực tế" else "Số lượng thay đổi (âm để giảm)", quantity, true) { quantity = it }
        if (mode == "COUNT") EditField("Ngưỡng cảnh báo", reorder, true) { reorder = it }
        EditField("Lý do / ghi chú", note) { note = it }
        Button(onClick = { if (mode == "COUNT") onSave("count", mapOf("quantityOnHand" to quantity.toIntOrNull(), "reorderLevel" to reorder.toIntOrNull(), "note" to note)) else onSave("inventory", mapOf("quantityChange" to quantity.toIntOrNull(), "type" to mode, "note" to note)) }, enabled = !busy && quantity.toIntOrNull() != null && note.isNotBlank() && (mode != "COUNT" || reorder.toIntOrNull() != null)) { Text("Lưu tồn kho") }
        TextButton(onClick = onDismiss) { Text("Hủy") }
    }
}

@Composable
private fun AdminOrderCard(order: Order, shippers: List<Shipper>, busy: Boolean, onEvent: (StoreEvent) -> Unit) {
    var action by remember { mutableStateOf<String?>(null) }; var reason by remember { mutableStateOf("") }
    var shipperId by rememberSaveable(order.id) { mutableStateOf<Long?>(null) }
    val transitions = mapOf("PENDING" to listOf("CONFIRMED", "CANCELLED"), "CONFIRMED" to listOf("PREPARING", "CANCELLED"), "PREPARING" to listOf("SHIPPING", "CANCELLED"), "SHIPPING" to listOf("DELIVERED"))
    Panel(Modifier.fillMaxWidth()) {
        Text(order.orderCode, style = MaterialTheme.typography.titleMedium); Text("${statusLabel(order.orderStatus)} · ${statusLabel(order.paymentStatus)}"); Text(money(order.totalAmount), color = MaterialTheme.colorScheme.primary)
        TextButton(onClick = { onEvent(StoreEvent.OpenOrder(order.id)) }, enabled = !busy) { Text("Xem chi tiết đơn hàng") }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { transitions[order.orderStatus].orEmpty().forEach { target -> item { OutlinedButton(onClick = { action = target }, enabled = !busy) { Text(statusLabel(target)) } } } }
        if (order.orderStatus == "DELIVERED" && order.paymentStatus == "UNPAID") Button(onClick = { action = "COD" }, enabled = !busy) { Text("Xác nhận đã nhận tiền COD") }
        if (order.paymentStatus == "PAID") OutlinedButton(onClick = { action = "REFUND" }, enabled = !busy) { Text("Tạo yêu cầu hoàn tiền") }
        if (order.paymentStatus == "PAID") TextButton(onClick = { action = "REFUND_CASH" }, enabled = !busy) { Text("Xác nhận đã hoàn tiền mặt") }
        if (order.orderStatus in setOf("CONFIRMED", "PREPARING", "SHIPPING")) {
            if (shippers.isEmpty()) Text("Chưa có tài khoản shipper hoạt động.", style = MaterialTheme.typography.bodySmall)
            else { Choice("Phân công giao hàng", shippers.associate { it.id.toString() to it.name }, shipperId?.toString().orEmpty()) { shipperId = it.toLongOrNull() }
                OutlinedButton(onClick = { action = "ASSIGN" }, enabled = !busy && shipperId != null) { Text("Phân công shipper") } }
        }
    }
    action?.let { target -> AlertDialog(onDismissRequest = { action = null }, title = { Text(if (target == "COD") "Xác nhận thu tiền" else if (target == "REFUND") "Yêu cầu hoàn tiền" else "Cập nhật đơn hàng") }, text = {
        if (target == "REFUND") OutlinedTextField(reason, { reason = it }, label = { Text("Lý do hoàn tiền") }) else Text(when (target) {
            "COD" -> "Đã thực tế thu đủ ${money(order.totalAmount)} cho đơn ${order.orderCode}?"
            "REFUND_CASH" -> "Chỉ xác nhận sau khi đã tạo yêu cầu hoàn và thực tế trả lại đủ tiền mặt cho khách. Thao tác này ghi nhận đối soát, không tự chuyển tiền."
            "ASSIGN" -> "Phân công đơn ${order.orderCode} cho shipper đã chọn?"
            else -> "Chuyển ${order.orderCode} sang ${statusLabel(target)}?"
        })
    }, confirmButton = { TextButton(onClick = { action = null; onEvent(when (target) {
        "COD" -> StoreEvent.Admin("cod", order.id, mapOf("amount" to order.totalAmount))
        "REFUND" -> StoreEvent.Admin("refund", order.id, mapOf("reason" to reason))
        "REFUND_CASH" -> StoreEvent.Admin("refund_cash", order.id, emptyMap())
        "ASSIGN" -> StoreEvent.Admin("assign_shipper", order.id, mapOf("shipperId" to shipperId))
        else -> StoreEvent.Admin("order", order.id, mapOf("status" to target))
    }) }, enabled = target != "REFUND" || reason.isNotBlank()) { Text("Xác nhận") } }, dismissButton = { TextButton(onClick = { action = null }) { Text("Đóng") } }) }
}

@Composable
private fun VoucherEditor(voucher: Voucher?, busy: Boolean, onDismiss: () -> Unit, onSave: (Map<String, Any?>) -> Unit) {
    var code by remember { mutableStateOf(voucher?.code.orEmpty()) }; var type by remember { mutableStateOf(voucher?.discountType ?: "PERCENT") }; var value by remember { mutableStateOf(voucher?.discountValue?.toPlainString().orEmpty()) }; var minimum by remember { mutableStateOf(voucher?.minOrderValue?.toPlainString() ?: "0") }
    var start by remember { mutableStateOf(voucher?.startAt?.take(19) ?: java.time.LocalDate.now().atStartOfDay().toString()) }; var end by remember { mutableStateOf(voucher?.endAt?.take(19) ?: java.time.LocalDate.now().plusMonths(1).atStartOfDay().toString()) }; var limit by remember { mutableStateOf(voucher?.usageLimit?.toString().orEmpty()) }
    var active by remember { mutableStateOf(voucher?.isActive ?: true) }
    EditorDialog(if (voucher == null) "Tạo mã ưu đãi" else "Sửa mã ưu đãi", onDismiss) {
        EditField("Mã", code) { code = it }; Choice("Loại giảm", mapOf("PERCENT" to "Phần trăm", "AMOUNT" to "Số tiền"), type) { type = it }
        EditField("Giá trị", value, true) { value = it }; EditField("Đơn tối thiểu (đ)", minimum, true) { minimum = it }
        EditField("Bắt đầu (yyyy-MM-ddTHH:mm)", start) { start = it }; EditField("Kết thúc (yyyy-MM-ddTHH:mm)", end) { end = it }; EditField("Số lượt tối đa (để trống nếu không giới hạn)", limit, true) { limit = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(active, { active = it }); Text("Đang sử dụng") }
        Button(onClick = { onSave(mapOf("code" to code, "description" to voucher?.description, "discountType" to type, "discountValue" to value.toBigDecimalOrNull(), "minOrderValue" to minimum.toBigDecimalOrNull(), "startAt" to start, "endAt" to end, "usageLimit" to limit.toIntOrNull(), "isActive" to active)) }, enabled = !busy && code.isNotBlank() && value.toBigDecimalOrNull() != null && minimum.toBigDecimalOrNull() != null) { Text("Lưu mã") }; TextButton(onClick = onDismiss) { Text("Hủy") }
    }
}

@Composable
private fun ReportPanel(report: Map<String, Any?>) {
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle("Báo cáo kinh doanh")
        Text("Từ ${report["from"].orEmptyText()} đến ${report["to"].orEmptyText()}", style = MaterialTheme.typography.bodySmall)
        val summary = report["summary"] as? Map<*, *>
        if (summary == null) Text("Chưa có dữ liệu báo cáo.") else {
            Text("Số đơn: ${summary["orderCount"].orEmptyText()}")
            Text("Doanh thu đã thu: ${summary["paidRevenue"].asMoney()}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Tiền chưa thu: ${summary["unpaidAmount"].asMoney()}")
        }
        val best = report["bestSellers"] as? List<*>
        if (!best.isNullOrEmpty()) { HorizontalDivider(); Text("Sản phẩm bán chạy (đơn đã giao)", fontWeight = FontWeight.Bold); best.forEach { row -> (row as? Map<*, *>)?.let { Text("${it["name"]}: ${it["quantitySold"].orEmptyText()}") } } }
        val statuses = report["ordersByStatus"] as? List<*>
        statuses.orEmpty().forEach { row -> (row as? Map<*, *>)?.let { Text("${statusLabel(it["status"].toString())}: ${it["count"].orEmptyText()}") } }
        val delivery = report["deliveryPerformance"] as? List<*>
        if (!delivery.isNullOrEmpty()) { HorizontalDivider(); Text("Hiệu suất giao hàng", fontWeight = FontWeight.Bold); delivery.forEach { row -> (row as? Map<*, *>)?.let { Text("${it["name"]}: ${it["deliveredCount"].orEmptyText()} / ${it["shipmentCount"].orEmptyText()} đơn đã giao") } } }
        val daily = report["daily"] as? List<*>
        if (!daily.isNullOrEmpty()) { HorizontalDivider(); Text("Doanh thu theo ngày", fontWeight = FontWeight.Bold); daily.forEach { row -> (row as? Map<*, *>)?.let { Text("${it["date"]}: ${it["paidRevenue"].asMoney()}") } } }
    }
}
private fun Any?.orEmptyText() = this?.toString() ?: "—"
private fun Any?.asMoney() = this?.toString()?.toBigDecimalOrNull()?.let { money(it) } ?: "—"

@Composable
private fun EditField(label: String, value: String, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (numeric) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text))
}
@Composable
private fun Choice(label: String, options: Map<String, String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column { Text(label, style = MaterialTheme.typography.labelMedium); Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(options[selected] ?: "Chọn"); Icon(Icons.Outlined.ExpandMore, null) }; DropdownMenu(expanded, { expanded = false }) { options.forEach { (key, title) -> DropdownMenuItem(text = { Text(title) }, onClick = { expanded = false; onSelect(key) }) } } } }
}
