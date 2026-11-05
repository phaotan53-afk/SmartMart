package com.example.smartmart.master

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI. */
@Composable
fun CartScreen(state: StoreState, onEvent: (StoreEvent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("cart_screen"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Giỏ hàng của bạn") }
        if (state.cart.items.isEmpty()) item { EmptyState("Giỏ hàng đang trống.", "Mua sắm ngay") { onEvent(StoreEvent.Navigate(Destination.HOME)) } }
        state.cart.items.forEach { item -> item(key = item.productId) {
            Panel(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProductPhoto(item.imageUrl, item.name, Modifier.size(88.dp).clickable { onEvent(StoreEvent.OpenProduct(item.productId)) })
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(money(item.price, item.unit), color = MaterialTheme.colorScheme.primary)
                        if (item.stockStatus != "IN_STOCK") Text("Kiểm tra lại tình trạng hàng trước khi đặt.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onEvent(StoreEvent.Remove(item.productId)) }, enabled = !state.busy) { Icon(Icons.Outlined.DeleteOutline, "Xóa ${item.name}") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    QuantitySelector(item.quantity, item.quantityOnHand ?: 999, !state.busy) { onEvent(StoreEvent.Quantity(item.productId, it)) }
                    Text(money(item.lineTotal), fontWeight = FontWeight.Bold)
                }
            }
        } }
        if (state.cart.items.isNotEmpty()) item {
            Panel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tạm tính"); Text(money(state.cart.total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                Text("Phí vận chuyển và ưu đãi được xác nhận khi đặt hàng.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { onEvent(StoreEvent.Navigate(Destination.CHECKOUT)) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("checkout_open")) { Text("Tiến hành đặt hàng") }
            }
        }
    }
}

@Composable
fun AccountScreen(state: StoreState, onEvent: (StoreEvent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle(if (state.user == null) "Tài khoản SmartMart" else "Hồ sơ của bạn") }
        if (state.user == null) item { AuthenticationForm(state.busy, onEvent) }
        else {
            item {
                val user = checkNotNull(state.user)
                var name by rememberSaveable(user.id) { mutableStateOf(user.fullName) }
                var phone by rememberSaveable(user.id) { mutableStateOf(user.phone.orEmpty()) }
                var email by rememberSaveable(user.id) { mutableStateOf(user.email) }
                Panel(Modifier.fillMaxWidth()) {
                    Text("Vai trò: ${when (user.role) { "ADMIN" -> "Quản trị viên"; "WAREHOUSE_MANAGER" -> "Quản lý kho"; "SHIPPER" -> "Nhân viên giao hàng"; else -> "Khách hàng" }}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(name, { name = it }, label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth().testTag("profile_name"), singleLine = true)
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth().testTag("profile_email"), singleLine = true)
                    OutlinedTextField(phone, { phone = it }, label = { Text("Số điện thoại") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { onEvent(StoreEvent.Profile(name, phone, email)) }, enabled = !state.busy && name.isNotBlank() && email.isNotBlank(), modifier = Modifier.testTag("profile_save")) { Text("Lưu hồ sơ") }
                    if (user.role in setOf("ADMIN", "WAREHOUSE_MANAGER", "SHIPPER")) OutlinedButton(onClick = { onEvent(StoreEvent.Navigate(Destination.MANAGEMENT)) }, modifier = Modifier.fillMaxWidth()) { Text("Mở khu vực quản lý") }
                    OutlinedButton(onClick = { onEvent(StoreEvent.Logout) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Đăng xuất") }
                }
            }
            if (state.user.role == "CUSTOMER") item { AddressBook(state, onEvent) }
        }
    }
}

@Composable
private fun AuthenticationForm(busy: Boolean, onEvent: (StoreEvent) -> Unit) {
    var register by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    // Password intentionally excluded from SavedStateHandle and saved instance state.
    var password by remember { mutableStateOf("") }
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle(if (register) "Đăng ký" else "Đăng nhập")
        if (register) OutlinedTextField(name, { name = it }, label = { Text("Họ và tên") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("auth_name"))
        OutlinedTextField(email, { email = it }, label = { Text(if (register) "Email" else "Email hoặc số điện thoại") }, keyboardOptions = KeyboardOptions(keyboardType = if (register) KeyboardType.Email else KeyboardType.Text), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("auth_email"))
        OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("auth_password"))
        if (register) OutlinedTextField(phone, { phone = it }, label = { Text("Số điện thoại (không bắt buộc)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onEvent(StoreEvent.Auth(register, name, email, password, phone)) }, enabled = !busy && email.isNotBlank() && password.isNotBlank() && (!register || name.isNotBlank()), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_submit")) { Text(if (register) "Tạo tài khoản" else "Đăng nhập") }
        TextButton(onClick = { register = !register; password = "" }, modifier = Modifier.fillMaxWidth().testTag("auth_register_toggle")) { Text(if (register) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký") }
    }
}

@Composable
fun CheckoutScreen(state: StoreState, onEvent: (StoreEvent) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    val effectiveId = selected?.takeIf { id -> state.addresses.any { it.id == id } } ?: state.addresses.firstOrNull { it.isDefault }?.id ?: state.addresses.firstOrNull()?.id
    var voucher by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(effectiveId, voucher, state.cart.total) { onEvent(StoreEvent.Quote(effectiveId, voucher)) }
    val matchingQuote = state.quote?.takeIf { effectiveId != null && state.quoteSignature == quoteSignature(effectiveId, voucher) }
    LazyColumn(Modifier.fillMaxSize().testTag("checkout_screen"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Xác nhận đặt hàng") }
        item { AddressBook(state, onEvent, effectiveId) { selected = it } }
        item { Panel(Modifier.fillMaxWidth()) {
            SectionTitle("Phương thức thanh toán")
            Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(true, onClick = {}); Column { Text("Thanh toán khi nhận hàng (COD)", fontWeight = FontWeight.SemiBold); Text("Đơn mới chưa được tính là đã thanh toán.", style = MaterialTheme.typography.bodySmall) } }
            Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(false, onClick = null, enabled = false); Text("Ví điện tử · chưa sẵn sàng", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(false, onClick = null, enabled = false); Text("Thẻ · chưa sẵn sàng", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("Thanh toán trực tuyến sẽ mở khi cửa hàng kết nối nhà cung cấp.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(voucher, { voucher = it }, label = { Text("Mã ưu đãi (nếu có)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it.take(500) }, label = { Text("Ghi chú cho cửa hàng") }, modifier = Modifier.fillMaxWidth())
            HorizontalDivider()
            state.cart.items.forEach { item -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${item.name} × ${item.quantity}", Modifier.weight(1f)); Text(money(item.lineTotal)) } }
            Text("Tiền hàng: ${money(state.cart.total)}")
            if (state.quoteLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            matchingQuote?.let { quote ->
                Text("Ưu đãi: −${money(quote.discountAmount)}"); Text("Phí vận chuyển: ${money(quote.shippingFee)}")
                Text("Tổng thanh toán COD: ${money(quote.totalAmount)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("Giá và tồn kho sẽ được máy chủ kiểm tra lại khi xác nhận.", style = MaterialTheme.typography.bodySmall)
            }
            if (!state.quoteLoading && matchingQuote == null && effectiveId != null) OutlinedButton(onClick = { onEvent(StoreEvent.Quote(effectiveId, voucher)) }) { Text("Kiểm tra tổng tiền") }
            Button(onClick = { effectiveId?.let { onEvent(StoreEvent.Checkout(it, "COD", voucher, note)) } }, enabled = !state.busy && !state.quoteLoading && matchingQuote != null && state.cart.items.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("place_order")) { Text("Đặt hàng COD") }
        } }
    }
}

@Composable
private fun AddressBook(state: StoreState, onEvent: (StoreEvent) -> Unit, selectedId: Long? = null, onSelect: ((Long) -> Unit)? = null) {
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Address?>(null) }
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle("Địa chỉ nhận hàng")
        if (state.addresses.isEmpty()) Text("Bạn chưa có địa chỉ. Hãy thêm địa chỉ để đặt hàng.")
        state.addresses.forEach { address ->
            Row(Modifier.fillMaxWidth().clickable(enabled = onSelect != null) { onSelect?.invoke(address.id) }, verticalAlignment = Alignment.Top) {
                if (onSelect != null) RadioButton(selectedId == address.id, onClick = { onSelect(address.id) })
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text("${address.recipientName} · ${address.phone}", fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull(address.addressLine, address.ward, address.district, address.province).filter(String::isNotBlank).joinToString(", "), style = MaterialTheme.typography.bodySmall)
                    if (address.isDefault) Text("Mặc định", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Row { TextButton(onClick = { editing = address; editor = true }) { Text("Sửa") }; TextButton(onClick = { onEvent(StoreEvent.RemoveAddress(address.id)) }, enabled = !state.busy) { Text("Xóa") } }
                }
            }; HorizontalDivider()
        }
        OutlinedButton(onClick = { editing = null; editor = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AddLocationAlt, null); Spacer(Modifier.width(8.dp)); Text("Thêm địa chỉ") }
        if (editor) AddressEditor(editing, state.busy, onSave = { onEvent(StoreEvent.SaveAddress(it)); editor = false }, onCancel = { editor = false })
    }
}

@Composable
private fun AddressEditor(address: Address?, busy: Boolean, onSave: (Address) -> Unit, onCancel: () -> Unit) {
    var name by rememberSaveable(address?.id) { mutableStateOf(address?.recipientName.orEmpty()) }
    var phone by rememberSaveable(address?.id) { mutableStateOf(address?.phone.orEmpty()) }
    var line by rememberSaveable(address?.id) { mutableStateOf(address?.addressLine.orEmpty()) }
    var ward by rememberSaveable(address?.id) { mutableStateOf(address?.ward.orEmpty()) }
    var district by rememberSaveable(address?.id) { mutableStateOf(address?.district.orEmpty()) }
    var province by rememberSaveable(address?.id) { mutableStateOf(address?.province.orEmpty()) }
    var default by rememberSaveable(address?.id) { mutableStateOf(address?.isDefault ?: false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.imePadding()) {
        OutlinedTextField(name, { name = it }, label = { Text("Tên người nhận") }, modifier = Modifier.fillMaxWidth().testTag("address_recipient"), singleLine = true)
        OutlinedTextField(phone, { phone = it }, label = { Text("Số điện thoại") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth().testTag("address_phone"), singleLine = true)
        OutlinedTextField(line, { line = it }, label = { Text("Số nhà, tên đường") }, modifier = Modifier.fillMaxWidth().testTag("address_line"))
        OutlinedTextField(ward, { ward = it }, label = { Text("Phường/xã (nếu có)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(district, { district = it }, label = { Text("Quận/huyện") }, modifier = Modifier.fillMaxWidth().testTag("address_district"), singleLine = true)
        OutlinedTextField(province, { province = it }, label = { Text("Tỉnh/thành phố") }, modifier = Modifier.fillMaxWidth().testTag("address_province"), singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(default, { default = it }); Text("Đặt làm địa chỉ mặc định") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(Address(address?.id ?: 0L, name, phone, line, ward.takeIf(String::isNotBlank), district, province, default)) }, enabled = !busy && listOf(name, phone, line, district, province).all(String::isNotBlank), modifier = Modifier.testTag("address_save")) { Text("Lưu địa chỉ") }
            TextButton(onClick = onCancel) { Text("Hủy") }
        }
    }
}

@Composable
fun OrdersScreen(state: StoreState, onEvent: (StoreEvent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { SectionTitle("Đơn hàng của tôi") }; IconButton(onClick = { onEvent(StoreEvent.Navigate(Destination.ORDERS)) }) { Icon(Icons.Outlined.Refresh, "Tải lại đơn hàng") } } }
        if (state.orders.isEmpty() && !state.busy) item { EmptyState("Bạn chưa có đơn hàng.", "Mua sắm ngay") { onEvent(StoreEvent.Navigate(Destination.HOME)) } }
        state.orders.forEach { order -> item(key = order.id) {
            Panel(Modifier.fillMaxWidth().clickable { onEvent(StoreEvent.OpenOrder(order.id)) }) {
                Text(order.orderCode, style = MaterialTheme.typography.titleMedium)
                Text("${statusLabel(order.orderStatus)} · ${statusLabel(order.paymentStatus)}", color = MaterialTheme.colorScheme.primary)
                Text(order.createdAt.take(19).replace('T', ' '), style = MaterialTheme.typography.bodySmall)
                Text(money(order.totalAmount), fontWeight = FontWeight.Bold)
                TextButton(onClick = { onEvent(StoreEvent.OpenOrder(order.id)) }) { Text("Xem chi tiết") }
            }
        } }
    }
}

@Composable
fun OrderScreen(state: StoreState, onEvent: (StoreEvent) -> Unit) {
    val order = state.selectedOrder ?: return
    var cancelConfirm by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Đơn ${order.orderCode}") }
        item { Panel(Modifier.fillMaxWidth()) {
            Text("Mã số đơn: ${order.id}")
            Text("Trạng thái: ${statusLabel(order.orderStatus)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("Thanh toán: ${statusLabel(order.paymentStatus)} (${order.paymentMethod.orEmpty()})")
            Text("Ngày tạo: ${order.createdAt.take(19).replace('T', ' ')}")
            order.addressSnapshot?.let { Text("Giao đến: $it") }
            if (order.orderStatus == "PENDING" && state.user?.role == "CUSTOMER") OutlinedButton(onClick = { cancelConfirm = true }, enabled = !state.busy) { Text("Hủy đơn") }
        } }
        state.tracking?.let { tracking -> item { Panel(Modifier.fillMaxWidth()) {
            SectionTitle("Theo dõi giao hàng"); Text(statusLabel(tracking.status))
            tracking.trackingCode?.let { Text("Mã vận đơn: $it") }; tracking.shipperName?.let { Text("Người giao: $it") }
            if (order.orderStatus == "SHIPPING") OutlinedButton(onClick = { onEvent(StoreEvent.TrackLocation(order.id)) }) { Text("Xem vị trí đã báo gần nhất") }
            state.trackingLocation?.let { location ->
                Text("Tọa độ đã báo: ${location.latitude}, ${location.longitude}"); Text("Thời điểm: ${location.recordedAt}", style = MaterialTheme.typography.bodySmall)
                Text("Đây là bản ghi do shipper gửi, không phải định vị thời gian thực.", style = MaterialTheme.typography.bodySmall)
                MapLink("${location.latitude},${location.longitude}", "Mở vị trí trên bản đồ")
            }
        } } }
        if (order.history.isNotEmpty()) item { Panel(Modifier.fillMaxWidth()) {
            SectionTitle("Tiến trình đơn hàng"); order.history.forEach { entry -> Text("${statusLabel(entry.status)} · ${entry.changedAt.take(19).replace('T', ' ')}"); entry.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
        } }
        order.items.forEach { item -> item {
            Panel(Modifier.fillMaxWidth()) {
                Text(item.productName ?: item.name ?: "Sản phẩm ${item.productId}", style = MaterialTheme.typography.titleMedium)
                Text("Số lượng: ${item.quantity}")
                item.lineTotal?.let { Text(money(it), color = MaterialTheme.colorScheme.primary) }
                TextButton(onClick = { onEvent(StoreEvent.OpenProduct(item.productId)) }) { Text(if (order.orderStatus == "DELIVERED") "Xem sản phẩm và đánh giá" else "Xem sản phẩm") }
            }
        } }
        item { Panel(Modifier.fillMaxWidth()) {
            Text("Tiền hàng: ${money(order.subtotal)}"); Text("Ưu đãi: −${money(order.discountAmount)}"); Text("Phí vận chuyển: ${money(order.shippingFee)}")
            HorizontalDivider(); Text("Tổng cộng: ${money(order.totalAmount)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        } }
    }
    if (cancelConfirm) AlertDialog(onDismissRequest = { cancelConfirm = false }, title = { Text("Hủy đơn hàng?") }, text = { Text("Đơn ${order.orderCode} sẽ được hủy nếu còn đủ điều kiện.") }, confirmButton = { TextButton(onClick = { cancelConfirm = false; onEvent(StoreEvent.CancelOrder(order.id)) }) { Text("Hủy đơn") } }, dismissButton = { TextButton(onClick = { cancelConfirm = false }) { Text("Giữ đơn") } })
}

@Composable
fun FavoriteScreen(state: StoreState, onEvent: (StoreEvent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Sản phẩm yêu thích") }
        if (state.favorites.isEmpty()) item { EmptyState("Chưa có sản phẩm yêu thích.", "Khám phá sản phẩm") { onEvent(StoreEvent.Navigate(Destination.HOME)) } }
        state.favorites.chunked(2).forEach { row -> item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { product -> ProductCard(product, true, state.busy, onEvent, Modifier.weight(1f)) }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } }
    }
}
