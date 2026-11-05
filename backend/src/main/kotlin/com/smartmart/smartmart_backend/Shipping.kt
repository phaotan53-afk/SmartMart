package com.smartmart.smartmart_backend

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * Vị trí là bản ghi shipper gửi thật; API trả thời điểm ghi, không bịa nguồn GPS/realtime.
 */
@Service
class ShippingService(private val jdbc: JdbcTemplate,private val commerce: CommerceService,private val catalog: CatalogService) {
    private val columns="s.ShipmentID AS id,s.OrderID AS orderId,s.ShipperID AS shipperId,s.ShipmentStatus AS status,s.TrackingCode AS trackingCode,s.AssignedAt AS assignedAt,s.ShippedAt AS shippedAt,s.DeliveredAt AS deliveredAt,o.OrderCode AS orderCode,o.AddressSnapshot AS addressSnapshot,o.TotalAmount AS totalAmount,o.PaymentStatus AS paymentStatus,o.OrderStatus AS orderStatus"
    fun shippers()=jdbc.queryForList("SELECT UserID AS id,FullName AS name FROM dbo.Users WHERE Role='SHIPPER' AND IsActive=1 ORDER BY FullName")
    @Transactional
    fun assign(actor: Long,orderId: Long,shipperId: Long): Map<String,Any?> {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Users WHERE UserID=? AND Role='SHIPPER' AND IsActive=1",Int::class.java,shipperId)!! == 0) badRequest("Shipper không hợp lệ")
        val row=jdbc.queryForList("SELECT OrderStatus AS status FROM dbo.Orders WITH(UPDLOCK,HOLDLOCK) WHERE OrderID=?",orderId).firstOrNull() ?: notFound("Không tìm thấy đơn hàng")
        if(row["status"] !in listOf("CONFIRMED","PREPARING","SHIPPING")) conflict("ORDER_STATE","Chỉ phân công đơn đã xác nhận và chưa hoàn tất")
        val shipment=jdbc.queryForList("SELECT $columns FROM dbo.Shipments s WITH(UPDLOCK,HOLDLOCK) JOIN dbo.Orders o ON o.OrderID=s.OrderID WHERE s.OrderID=?",orderId).firstOrNull() ?: notFound("Chưa có vận đơn")
        // Retrying assignment must not rewind PICKED_UP/IN_TRANSIT or lose delivery progress.
        if((shipment["shipperId"] as Number?)?.toLong()==shipperId) return shipment
        if(row["status"]=="SHIPPING" || shipment["status"] in listOf("PICKED_UP","IN_TRANSIT"))
            conflict("SHIPMENT_STATE","Không đổi người giao sau khi shipper đã nhận hàng hoặc đơn đang giao")
        jdbc.update("UPDATE dbo.Shipments SET ShipperID=?,ShipmentStatus='ASSIGNED',AssignedAt=SYSDATETIME() WHERE OrderID=?",shipperId,orderId)
        return jdbc.queryForList("SELECT $columns FROM dbo.Shipments s JOIN dbo.Orders o ON o.OrderID=s.OrderID WHERE s.OrderID=?",orderId).firstOrNull() ?: notFound("Chưa có vận đơn")
    }
    fun list(shipperId: Long)=jdbc.queryForList("SELECT $columns FROM dbo.Shipments s JOIN dbo.Orders o ON o.OrderID=s.OrderID WHERE s.ShipperID=? ORDER BY s.ShipmentID DESC",shipperId)
    fun detail(shipperId: Long,id: Long): Map<String,Any?> {
        val row=jdbc.queryForList("SELECT $columns FROM dbo.Shipments s JOIN dbo.Orders o ON o.OrderID=s.OrderID WHERE s.ShipperID=? AND s.ShipmentID=?",shipperId,id).firstOrNull() ?: notFound("Không tìm thấy vận đơn được giao cho bạn")
        return row+mapOf("order" to commerce.order(shipperId,(row["orderId"] as Number).toLong(),true))
    }
    @Transactional
    fun status(shipperId: Long,id: Long,target: String): Map<String,Any?> {
        val row=jdbc.queryForList("SELECT s.ShipmentStatus AS status,s.OrderID AS orderId,o.OrderStatus AS orderStatus FROM dbo.Shipments s WITH(UPDLOCK,HOLDLOCK) JOIN dbo.Orders o WITH(UPDLOCK,HOLDLOCK) ON o.OrderID=s.OrderID WHERE s.ShipperID=? AND s.ShipmentID=?",shipperId,id).firstOrNull() ?: notFound("Không tìm thấy vận đơn được giao cho bạn")
        val old=row["status"] as String
        if(old==target) return detail(shipperId,id)
        val allowed=mapOf("ASSIGNED" to setOf("PICKED_UP"),"PICKED_UP" to setOf("IN_TRANSIT"),"IN_TRANSIT" to setOf("DELIVERED","FAILED"))
        if(target !in allowed[old].orEmpty() || row["orderStatus"] in listOf("CANCELLED","REFUNDED","DELIVERED")) conflict("SHIPMENT_STATE","Chuyển trạng thái giao hàng không hợp lệ")
        val orderId=(row["orderId"] as Number).toLong()
        if(target=="IN_TRANSIT" && row["orderStatus"]!="PREPARING") conflict("ORDER_STATE","Đơn hàng cần được chuẩn bị trước khi giao")
        jdbc.update("UPDATE dbo.Shipments SET ShipmentStatus=?,ShippedAt=CASE WHEN ?='IN_TRANSIT' THEN COALESCE(ShippedAt,SYSDATETIME()) ELSE ShippedAt END,DeliveredAt=CASE WHEN ?='DELIVERED' THEN SYSDATETIME() ELSE DeliveredAt END WHERE ShipmentID=?",target,target,target,id)
        val orderStatus=when(target) { "IN_TRANSIT" -> "SHIPPING";"DELIVERED" -> "DELIVERED";else -> null }
        if(orderStatus!=null) {
            jdbc.update("UPDATE dbo.Orders SET OrderStatus=? WHERE OrderID=?",orderStatus,orderId)
            jdbc.update("INSERT dbo.OrderStatusHistory(OrderID,ChangedByUserID,OldStatus,NewStatus,Note) VALUES(?,?,?,?,N'Shipper cập nhật tiến trình giao')",orderId,shipperId,row["orderStatus"],orderStatus)
        }
        return detail(shipperId,id)
    }
    fun tracking(customerId: Long,orderId: Long): Map<String,Any?> {
        commerce.order(customerId,orderId)
        return jdbc.queryForList("SELECT s.ShipmentID AS id,s.ShipmentStatus AS status,s.TrackingCode AS trackingCode,s.AssignedAt AS assignedAt,s.ShippedAt AS shippedAt,s.DeliveredAt AS deliveredAt,u.FullName AS shipperName FROM dbo.Shipments s LEFT JOIN dbo.Users u ON u.UserID=s.ShipperID WHERE s.OrderID=?",orderId).firstOrNull() ?: notFound("Chưa có vận đơn")
    }
    fun location(shipperId: Long,body: ShipperLocation): Map<String,Any?> {
        val id=jdbc.queryForObject("INSERT dbo.ShipperLocations(ShipperID,Latitude,Longitude) OUTPUT INSERTED.ShipperLocationID VALUES(?,?,?)",Long::class.java,shipperId,body.latitude,body.longitude)!!
        return jdbc.queryForList("SELECT Latitude AS latitude,Longitude AS longitude,RecordedAt AS recordedAt FROM dbo.ShipperLocations WHERE ShipperLocationID=?",id).first()
    }
    fun customerLocation(customerId: Long,orderId: Long): Map<String,Any?> {
        commerce.order(customerId,orderId)
        val row=jdbc.queryForList("SELECT TOP(1) l.Latitude AS latitude,l.Longitude AS longitude,l.RecordedAt AS recordedAt FROM dbo.Orders o JOIN dbo.Shipments s ON s.OrderID=o.OrderID JOIN dbo.ShipperLocations l ON l.ShipperID=s.ShipperID WHERE o.OrderID=? AND o.CustomerID=? AND o.OrderStatus='SHIPPING' AND l.RecordedAt>DATEADD(HOUR,-24,SYSDATETIME()) ORDER BY l.ShipperLocationID DESC",orderId,customerId).firstOrNull()
            ?: notFound("Chưa có vị trí gần đây cho đơn đang giao")
        return row+mapOf("realtime" to false,"source" to "AUTHENTICATED_SHIPPER_REPORT")
    }
    fun recommendations(customerId: Long): Map<String,Any?> {
        val ids=jdbc.queryForList("SELECT TOP(12) r.ProductID FROM dbo.ProductRecommendations r JOIN dbo.Products p ON p.ProductID=r.ProductID WHERE r.CustomerID=? AND p.IsActive=1 ORDER BY r.Score DESC,r.ProductRecommendationID",Long::class.java,customerId)
        if(ids.isNotEmpty()) return mapOf("source" to "RECORDED_RECOMMENDATIONS","items" to ids.map { catalog.summary(it!!) })
        return mapOf("source" to "RECENT_CATALOG","items" to catalog.page(CatalogFilter(null,null,null,null,null,null,null,"newest",0,8)).items)
    }
}

@RestController
class ShippingController(private val auth: AuthService,private val shipping: ShippingService) {
    @GetMapping("/api/admin/shippers") fun shippers(request: HttpServletRequest): List<Map<String,Any?>> { auth.roles(request,"ADMIN");return shipping.shippers() }
    @PostMapping("/api/admin/orders/{id}/assign-shipper") fun assign(request: HttpServletRequest,@PathVariable id: Long,@Valid @RequestBody body: ShipperAssignment)=shipping.assign(auth.roles(request,"ADMIN").id,id,body.shipperId)
    @GetMapping("/api/shipper/shipments") fun list(request: HttpServletRequest)=shipping.list(auth.roles(request,"SHIPPER").id)
    @GetMapping("/api/shipper/shipments/{id}") fun detail(request: HttpServletRequest,@PathVariable id: Long)=shipping.detail(auth.roles(request,"SHIPPER").id,id)
    @PostMapping("/api/shipper/shipments/{id}/accept") fun accept(request: HttpServletRequest,@PathVariable id: Long)=shipping.status(auth.roles(request,"SHIPPER").id,id,"PICKED_UP")
    @PutMapping("/api/shipper/shipments/{id}/status") fun status(request: HttpServletRequest,@PathVariable id: Long,@RequestBody body: OrderStatusRequest)=shipping.status(auth.roles(request,"SHIPPER").id,id,body.status)
    @PostMapping("/api/shipper/locations") fun location(request: HttpServletRequest,@Valid @RequestBody body: ShipperLocation)=shipping.location(auth.roles(request,"SHIPPER").id,body)
    @GetMapping("/api/orders/{id}/shipment") fun tracking(request: HttpServletRequest,@PathVariable id: Long)=shipping.tracking(auth.roles(request,"CUSTOMER").id,id)
    @GetMapping("/api/orders/{id}/shipment/location") fun customerLocation(request: HttpServletRequest,@PathVariable id: Long)=shipping.customerLocation(auth.roles(request,"CUSTOMER").id,id)
    @GetMapping("/api/shipper/routes") fun routes(request: HttpServletRequest): Nothing { auth.roles(request,"SHIPPER");throw ApiException(HttpStatus.SERVICE_UNAVAILABLE,"MAP_PROVIDER_UNCONFIGURED","Chưa cấu hình nhà cung cấp bản đồ và tối ưu tuyến đường") }
    @GetMapping("/api/recommendations") fun recommendations(request: HttpServletRequest)=shipping.recommendations(auth.roles(request,"CUSTOMER").id)
}
