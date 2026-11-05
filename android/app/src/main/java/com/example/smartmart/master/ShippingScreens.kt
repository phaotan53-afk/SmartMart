package com.example.smartmart.master

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * Dữ liệu giao hàng lấy từ API kiểm quyền shipper; không tạo vị trí GPS hoặc lộ trình giả.
 */
@Composable
internal fun ShipmentCard(shipment: Shipment, busy: Boolean, onEvent: (StoreEvent) -> Unit) {
    var confirmation by remember { mutableStateOf<String?>(null) }
    Panel(Modifier.fillMaxWidth()) {
        Text(shipment.orderCode, style = MaterialTheme.typography.titleMedium)
        Text(statusLabel(shipment.status), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        shipment.addressSnapshot?.let { address -> Text("Giao đến: $address"); MapLink(address, "Mở địa chỉ giao hàng") }
        Text("Thu hộ: ${money(shipment.totalAmount)} · ${statusLabel(shipment.paymentStatus)}")
        TextButton(onClick = { onEvent(StoreEvent.OpenShipment(shipment.id)) }, enabled = !busy) { Text("Xem chi tiết đơn giao") }
        shipment.order?.items.orEmpty().forEach { item -> Text("${item.name ?: item.productName ?: "Sản phẩm"} × ${item.quantity}") }
        val actions = when (shipment.status) {
            "ASSIGNED" -> listOf("PICKED_UP")
            "PICKED_UP" -> if (shipment.orderStatus == "PREPARING") listOf("IN_TRANSIT") else emptyList()
            "IN_TRANSIT" -> listOf("DELIVERED", "FAILED")
            else -> emptyList()
        }
        if (shipment.status == "PICKED_UP" && shipment.orderStatus != "PREPARING") Text("Cửa hàng cần chuyển đơn sang Đang chuẩn bị trước khi bạn bắt đầu giao.", style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions.forEach { action -> item {
            OutlinedButton(onClick = { confirmation = action }, enabled = !busy) { Text(if (action == "PICKED_UP") "Nhận đơn giao" else statusLabel(action)) }
        } } }
        if (shipment.status == "DELIVERED" && shipment.paymentStatus == "UNPAID") Text("Đã giao hàng; việc thu tiền COD được quản trị đối soát riêng.", style = MaterialTheme.typography.bodySmall)
    }
    confirmation?.let { target -> AlertDialog(
        onDismissRequest = { confirmation = null }, title = { Text("Cập nhật giao hàng") },
        text = { Text("Xác nhận ${statusLabel(target).lowercase()} cho ${shipment.orderCode}? Chỉ ghi nhận đúng tình trạng thực tế.") },
        confirmButton = { TextButton(onClick = {
            confirmation = null
            onEvent(if (target == "PICKED_UP") StoreEvent.Admin("shipment_accept", shipment.id, emptyMap()) else StoreEvent.Admin("shipment_status", shipment.id, mapOf("status" to target)))
        }) { Text("Xác nhận") } }, dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Đóng") } },
    ) }
}

@Composable
internal fun MapLink(query: String, label: String) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    // AddressSnapshot also contains recipient/phone. Never send those fields to a maps app.
    val coordinatesOnly = query.matches(Regex("-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?"))
    var mapQuery by remember(query) { mutableStateOf(if (coordinatesOnly) query else "") }
    TextButton(onClick = { open = true }) { Text(label) }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Mở ứng dụng bản đồ") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Chỉ nhập địa chỉ cần tìm hoặc tọa độ. Nội dung này sẽ được gửi sang ứng dụng bản đồ; không nhập tên hay điện thoại người nhận.")
        OutlinedTextField(mapQuery, { mapQuery = it }, label = { Text("Địa chỉ hoặc tọa độ") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { TextButton(onClick = {
        open = false
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(mapQuery.trim())))) }
        catch (_: ActivityNotFoundException) { Toast.makeText(context, "Thiết bị chưa có ứng dụng bản đồ.", Toast.LENGTH_LONG).show() }
    }, enabled = mapQuery.isNotBlank()) { Text("Mở bản đồ") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Đóng") } })
}

@Composable
internal fun LocationReportPanel(busy: Boolean, onEvent: (StoreEvent) -> Unit) {
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val lat = latitude.toBigDecimalOrNull(); val lon = longitude.toBigDecimalOrNull()
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle("Báo vị trí giao hàng")
        Text("Nhập tọa độ bạn đã xác định từ ứng dụng bản đồ. Hệ thống lưu người gửi và thời điểm; chưa thu GPS nền hoặc cung cấp theo dõi thời gian thực.")
        OutlinedTextField(latitude, { latitude = it }, label = { Text("Vĩ độ (−90 đến 90)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(longitude, { longitude = it }, label = { Text("Kinh độ (−180 đến 180)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { confirm = true }, enabled = !busy && lat != null && lon != null && lat >= BigDecimal(-90) && lat <= BigDecimal(90) && lon >= BigDecimal(-180) && lon <= BigDecimal(180)) { Text("Lưu vị trí đã xác định") }
        Text("Tối ưu tuyến đường cần kết nối nhà cung cấp bản đồ và hiện chưa được nghiệm thu.", style = MaterialTheme.typography.bodySmall)
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Gửi vị trí đã xác định") }, text = { Text("Gửi tọa độ $latitude, $longitude cho đơn đang giao? Khách hàng có thể xem bản ghi gần nhất theo quyền sở hữu đơn.") },
        confirmButton = { TextButton(onClick = { confirm = false; onEvent(StoreEvent.Admin("location", null, mapOf("latitude" to lat, "longitude" to lon))) }) { Text("Gửi vị trí") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Đóng") } })
}

@Composable
internal fun UserStatusButton(user: UserStatus, busy: Boolean, onEvent: (StoreEvent) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { confirm = true }, enabled = !busy) { Text(if (user.isActive) "Vô hiệu hóa tài khoản" else "Mở lại tài khoản") }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Thay đổi trạng thái tài khoản") },
        text = { Text("${if (user.isActive) "Vô hiệu hóa" else "Mở lại"} ${user.fullName}? Lịch sử giao dịch được giữ lại.") },
        confirmButton = { TextButton(onClick = { confirm = false; onEvent(StoreEvent.Admin("user_status", user.id, mapOf("isActive" to !user.isActive))) }) { Text("Xác nhận") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Đóng") } })
}
