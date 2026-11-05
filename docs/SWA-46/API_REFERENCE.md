# Tham chiếu API SmartMart Master

Tài liệu đối chiếu controller, DTO và SQL của bản Master. `openapi.json` là OpenAPI3.0.3 cùng phạm vi; `docs/audit/api-route-coverage.json` kiểm tra đủ method/path với source. Có endpoint không tự chứng minh đã qua kiểm thử. Kết quả thực tế xem `BAO_CAO_KIEM_THU.md`.

## Hợp đồng chung

Base URL backend phát triển mặc định `http://localhost:8080`; Android Emulator debug dùng host `10.0.2.2`. Máy thật dùng URL LAN/HTTPS cấu hình qua build. Bearer là opaque session token; không gửi userId hoặc role tự chọn. Không đưa token/mật khẩu vào ảnh, báo cáo hoặc Git. Spring không tự đọc `.env`; launcher nạp biến môi trường cục bộ.

Tiền là JSON number được xử lý bằng BigDecimal và lưu DECIMAL; client không gửi tổng tiền để quyết định đơn. Số lượng là INT theo đơn vị Products.Unit, không tự quy mọi hàng thành kg. Nullable unit/ảnh/điểm đánh giá được giữ rõ. Timestamp lấy từ DB có thể là local datetime; không suy diễn timezone/GPS từ định dạng hiển thị.

Lỗi có dạng `{"code":"INVALID_INPUT","message":"Thông báo tiếng Việt"}`. Các mã HTTP thường gặp:400 dữ liệu sai;401 chưa đăng nhập/phiên hết hạn;403 sai quyền;404 không tìm thấy hoặc không thuộc người gọi;409 xung đột tồn/trạng thái/retry/trùng dữ liệu;503 SQL tạm không xử lý được hoặc provider chưa cấu hình;500 lỗi bất ngờ. Client hiển thị message an toàn, không stacktrace. Endpoint không tồn tại giữ404, method không hỗ trợ giữ405.

Collection nhóm `GET /api/products` vẫn trả List với productId/productName và trường costPrice, nhưng yêu cầu ADMIN để tránh lộ giá vốn. Đây là thay đổi quyền cần chuyển client khách sang `GET /api/v1/products`; endpoint mới trả CatalogPage công khai, không costPrice. Detail `GET /api/products/{id}` giữ id/name của Demo. Không đổi âm thầm response List cũ thành envelope.

## Cách chạy kiểm tra có dữ liệu thật

Chạy `tools/initialize-sql-master.ps1`, sau đó `tools/start-backend.ps1 -Build` từ Master. Dùng `tools/e2e_master.py` để tạo tài khoản/giỏ/đơn thử trên API thật của database Master riêng; script tạo credential tạm cục bộ và không ghi secret vào báo cáo. Mọi ví dụ ID cần lấy từ API của môi trường đang chạy.

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod "http://localhost:8080/api/v1/products?query=Táo&sort=price_asc&page=0&size=12"
```

POST đăng ký có body `{ "fullName": "Tên người thử", "email": "email-duy-nhat@example.invalid", "password": "<mật khẩu tự đặt ít nhất10 ký tự>" }`. Phản hồi trả token và user; giữ token cục bộ và gắn `Authorization: Bearer <token>` cho route có quyền. Mật khẩu trong ví dụ là placeholder, không phải credential được seed.

## Các endpoint hiện có

### GET /api/admin/products

Đọc danh sách sản phẩm.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/AdminProduct"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Products, Categories, Inventory, ProductImages. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:196` → `products`.

### POST /api/admin/products

Tạo sản phẩm.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: Không có tham số. Request: `ProductWriteRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `201 {"$ref": "#/components/schemas/AdminProduct"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Products, Categories, Inventory, ProductImages. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:197` → `addProduct`.

### PUT /api/admin/products/{id}

Cập nhật sản phẩm.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: path id bắt buộc. Request: `ProductWriteRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/AdminProduct"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Products, Categories, Inventory, ProductImages. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:198` → `product`.

### DELETE /api/admin/products/{id}

Ngừng sử dụng sản phẩm.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Products, Categories, Inventory, ProductImages. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:199` → `removeProduct`.

### GET /api/admin/categories

Đọc danh sách danh mục.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/AdminCategory"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Categories. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:200` → `categories`.

### POST /api/admin/categories

Tạo danh mục.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: Không có tham số. Request: `CategoryWriteRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `201 {"$ref": "#/components/schemas/AdminCategory"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Categories. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:201` → `addCategory`.

### PUT /api/admin/categories/{id}

Cập nhật danh mục.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: path id bắt buộc. Request: `CategoryWriteRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/AdminCategory"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Categories. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:202` → `category`.

### DELETE /api/admin/categories/{id}

Ngừng sử dụng danh mục.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Categories. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:203` → `removeCategory`.

### GET /api/admin/inventory

Đọc tồn và cờ cảnh báo thấp.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/Inventory"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Products, Inventory, InventoryTransactions. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:204` → `inventory`.

### POST /api/admin/inventory/{id}/adjust

Nhập hoặc điều chỉnh tồn có lịch sử.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: path id bắt buộc; header Idempotency-Key tùy chọn. Request: `InventoryAdjustment`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/Inventory"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, INVALID_STOCK, IDEMPOTENCY_CONFLICT.
- SQL: Products, Inventory, InventoryTransactions, Users, MutationKeys. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Nhập/điều chỉnh/kiểm kê không âm hoặc vượt INT; lưu InventoryTransactions. Idempotency-Key tùy chọn giúp retry cùng request không cộng tồn trùng; cùng khóa/nội dung khác409.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:205` → `adjust`.

### PUT /api/admin/inventory/{id}/count

Kiểm kê và cập nhật ngưỡng cảnh báo.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: path id bắt buộc; header Idempotency-Key tùy chọn. Request: `InventoryCount`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/Inventory"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, INVALID_STOCK, IDEMPOTENCY_CONFLICT.
- SQL: Products, Inventory, InventoryTransactions, Users, MutationKeys. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Nhập/điều chỉnh/kiểm kê không âm hoặc vượt INT; lưu InventoryTransactions. Idempotency-Key tùy chọn giúp retry cùng request không cộng tồn trùng; cùng khóa/nội dung khác409.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:206` → `count`.

### GET /api/admin/inventory/transactions

Đọc tối đa200 giao dịch kho gần nhất.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN hoặc WAREHOUSE_MANAGER.
- Tham số: query productId tùy chọn. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/InventoryTransaction"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Products, Inventory, InventoryTransactions. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:207` → `history`.

### GET /api/admin/orders

Đọc danh sách đơn cần quản lý.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/AdminOrderSummary"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Orders, OrderItems, Products, Payments, OrderStatusHistory. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:208` → `orders`.

### GET /api/admin/orders/{id}

Đọc chi tiết đơn trong phạm vi quản trị.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/OrderDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Orders, OrderItems, Products, Payments, OrderStatusHistory. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:209` → `order`.

### PUT /api/admin/orders/{id}/status

Chuyển trạng thái xử lý đơn theo hợp đồng.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `OrderStatusRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/OrderDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, ORDER_STATE, SHIPMENT_STATE.
- SQL: Orders, OrderItems, Products, Payments, OrderStatusHistory, Inventory, InventoryTransactions, VoucherRedemptions, Shipments. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:210` → `status`.

### POST /api/admin/orders/{id}/collect-cod

Xác nhận đã thu đúng tiền COD.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `CodCollection`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/OrderDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, ORDER_STATE, PAYMENT_STATE, PAYMENT_PROVIDER_UNCONFIGURED.
- SQL: Orders, OrderItems, Products, Payments, OrderStatusHistory. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Chỉ sau DELIVERED, số tiền khớp TotalAmount, payment COD PENDING. Xác nhận đã nhận tiền thực tế mới đổi SUCCESS/PAID; lặp không thu thêm.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:211` → `collect`.

### POST /api/admin/orders/{id}/refund

Tạo yêu cầu hoàn tiền.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `RefundRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Refund"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, ORDER_STATE, PAYMENT_STATE, PAYMENT_PROVIDER_UNCONFIGURED.
- SQL: Orders, OrderItems, Products, Payments, OrderStatusHistory, RefundRequests. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Chỉ đơn đã PAID; tạo yêu cầu hoàn có lý do, lặp trả yêu cầu đã có; chưa tự đánh dấu hoàn tiền thành công.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:212` → `refund`.

### POST /api/admin/orders/{id}/confirm-cash-refund

Xác nhận tiền mặt đã thực sự hoàn.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `CashReturnedRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/OrderDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, ORDER_STATE, PAYMENT_STATE, PAYMENT_PROVIDER_UNCONFIGURED.
- SQL: Orders, OrderItems, Products, Payments, OrderStatusHistory, RefundRequests. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: cashReturned=true xác nhận đã hoàn tiền mặt; chỉ COD. Ghi REFUNDED/SUCCEEDED. Không gọi nhà cung cấp online và không chứng minh tiền ngoài hệ thống đã chuyển.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:213` → `confirmRefund`.

### GET /api/admin/vouchers

Đọc danh sách voucher.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/Voucher"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Vouchers, VoucherRedemptions. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:214` → `vouchers`.

### POST /api/admin/vouchers

Tạo voucher.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: Không có tham số. Request: `VoucherWriteRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `201 {"$ref": "#/components/schemas/Voucher"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Vouchers, VoucherRedemptions. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:215` → `addVoucher`.

### PUT /api/admin/vouchers/{id}

Cập nhật voucher.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `VoucherWriteRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Voucher"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Vouchers, VoucherRedemptions. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:216` → `voucher`.

### DELETE /api/admin/vouchers/{id}

Ngừng sử dụng voucher.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Vouchers, VoucherRedemptions. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:217` → `removeVoucher`.

### POST /api/vouchers/validate

Kiểm tra mã và mức giảm dự kiến.

- Màn hình gọi: Checkout.
- Quyền: CUSTOMER.
- Tham số: Không có tham số. Request: `VoucherValidation`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/VoucherCheck"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, INVALID_VOUCHER, VOUCHER_MINIMUM, VOUCHER_EXHAUSTED.
- SQL: Vouchers, VoucherRedemptions. Transaction: Đọc; không giữ lượt voucher.
- Nghiệp vụ: Kiểm active/thời hạn/minimum/usage limit; checkout kiểm lại trong transaction.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:218` → `validateVoucher`.

### GET /api/admin/users

Đọc hoặc tìm người dùng theo tên/email.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: query query tùy chọn. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/AdminUser"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Users. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:219` → `users`.

### PUT /api/admin/users/{id}/status

Khóa hoặc mở tài khoản khác.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `UserStatusRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Users, SessionTokens. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:220` → `userStatus`.

### DELETE /api/admin/users/{id}

Ngừng tài khoản và thu hồi phiên, giữ lịch sử.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Users, SessionTokens. Transaction: Product save, inventory adjust/count, order status, COD/refund, user status có @Transactional; các CRUD một statement dùng atomic SQL statement.
- Nghiệp vụ: Role từ session. Soft-delete để giữ FK/lịch sử; DTO có CostPrice chỉ dành quản lý. Trạng thái đơn phải theo các bước cho phép.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:221` → `deleteUser`.

### GET /api/admin/reports

Báo cáo doanh thu đã thu, đơn, sản phẩm và hiệu suất giao.

- Màn hình gọi: Quản lý.
- Quyền: ADMIN.
- Tham số: query from tùy chọn; query to tùy chọn. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Report"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Orders, OrderItems, Products, Shipments, Users. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: from/to ngày gồm cả ngày cuối, mặc định30 ngày tới hôm nay; khoảng≤10 năm. Doanh thu chỉ PaymentStatus PAID; bestSellers từ đơn DELIVERED, không coi tiền chưa thu là doanh thu.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Admin.kt:222` → `report`.

### POST /api/auth/register

Đăng ký tài khoản CUSTOMER.

- Màn hình gọi: Tài khoản, hồ sơ và sổ địa chỉ.
- Quyền: Công khai.
- Tham số: Không có tham số. Request: `RegisterRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `201 {"$ref": "#/components/schemas/SessionResponse"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, EMAIL_EXISTS, PHONE_EXISTS.
- SQL: Users, SessionTokens, Carts. Transaction: register/login/profile: @Transactional; logout xóa token hiện tại.
- Nghiệp vụ: PBKDF2 mật khẩu; token ngẫu nhiên lưu hash và hạn. Role đọc từ DB, không tin role/userId của client. Email/phone duy nhất sau chuẩn hóa.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Auth.kt:138` → `register`.

### POST /api/auth/login

Đăng nhập bằng email hoặc số điện thoại.

- Màn hình gọi: Tài khoản, hồ sơ và sổ địa chỉ.
- Quyền: Công khai.
- Tham số: Không có tham số. Request: `LoginRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/SessionResponse"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, INVALID_CREDENTIALS.
- SQL: Users, SessionTokens. Transaction: register/login/profile: @Transactional; logout xóa token hiện tại.
- Nghiệp vụ: PBKDF2 mật khẩu; token ngẫu nhiên lưu hash và hạn. Role đọc từ DB, không tin role/userId của client. Email/phone duy nhất sau chuẩn hóa.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Auth.kt:140` → `login`.

### POST /api/auth/logout

Kết thúc phiên hiện tại.

- Màn hình gọi: Tài khoản, hồ sơ và sổ địa chỉ.
- Quyền: Mọi tài khoản có Bearer session còn hạn.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Users, SessionTokens. Transaction: register/login/profile: @Transactional; logout xóa token hiện tại.
- Nghiệp vụ: PBKDF2 mật khẩu; token ngẫu nhiên lưu hash và hạn. Role đọc từ DB, không tin role/userId của client. Email/phone duy nhất sau chuẩn hóa.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Auth.kt:141` → `logout`.

### GET /api/me

Xem hồ sơ hiện tại.

- Màn hình gọi: Tài khoản, hồ sơ và sổ địa chỉ.
- Quyền: Mọi tài khoản có Bearer session còn hạn.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/SessionUser"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Users, SessionTokens. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: PBKDF2 mật khẩu; token ngẫu nhiên lưu hash và hạn. Role đọc từ DB, không tin role/userId của client. Email/phone duy nhất sau chuẩn hóa.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Auth.kt:142` → `me`.

### PUT /api/me

Cập nhật họ tên, phone và email của mình.

- Màn hình gọi: Tài khoản, hồ sơ và sổ địa chỉ.
- Quyền: Mọi tài khoản có Bearer session còn hạn.
- Tham số: Không có tham số. Request: `ProfileRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/SessionUser"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, EMAIL_EXISTS, PHONE_EXISTS.
- SQL: Users, SessionTokens. Transaction: register/login/profile: @Transactional; logout xóa token hiện tại.
- Nghiệp vụ: PBKDF2 mật khẩu; token ngẫu nhiên lưu hash và hạn. Role đọc từ DB, không tin role/userId của client. Email/phone duy nhất sau chuẩn hóa.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Auth.kt:143` → `profile`.

### GET /api/v1/products

Tìm kiếm, lọc, sắp xếp và phân trang sản phẩm công khai.

- Màn hình gọi: Trang chủ hoặc chi tiết.
- Quyền: Công khai.
- Tham số: query query tùy chọn; query categoryId tùy chọn; query minPrice tùy chọn; query maxPrice tùy chọn; query minRating tùy chọn; query inStock tùy chọn; query brand tùy chọn; query sort tùy chọn; query page tùy chọn; query size tùy chọn. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/CatalogPage"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR.
- SQL: Products, Categories, Inventory, ProductImages, Reviews. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: SQL có tham số; dữ liệu động lấy từ SQL Server.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Catalog.kt:106` → `list`.

### GET /api/categories

Đọc danh mục đang hoạt động.

- Màn hình gọi: Trang chủ hoặc chi tiết.
- Quyền: Công khai.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/Category"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR.
- SQL: Products, Categories, Inventory, ProductImages, Reviews. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: SQL có tham số; dữ liệu động lấy từ SQL Server.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Catalog.kt:112` → `categories`.

### GET /api/catalog/filters

Đọc danh mục và thương hiệu thực cho bộ lọc.

- Màn hình gọi: Trang chủ hoặc chi tiết.
- Quyền: Công khai.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/CatalogMetadata"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR.
- SQL: Products, Categories, Inventory, ProductImages, Reviews. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: SQL có tham số; dữ liệu động lấy từ SQL Server.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Catalog.kt:113` → `filters`.

### GET /api/addresses

Đọc sổ địa chỉ.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/Address"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Addresses, Users. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:266` → `addresses`.

### POST /api/addresses

Thêm địa chỉ thuộc khách.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: Không có tham số. Request: `AddressRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `201 {"$ref": "#/components/schemas/Address"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Addresses, Users. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:267` → `addAddress`.

### PUT /api/addresses/{id}

Sửa địa chỉ thuộc khách.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `AddressRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Address"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Addresses, Users. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:269` → `updateAddress`.

### DELETE /api/addresses/{id}

Xóa địa chỉ chưa dùng trong đơn.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Addresses, Users, Orders. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:270` → `removeAddress`.

### GET /api/cart

Đọc giỏ đã lưu của khách.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/CartResponse"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Carts, CartItems, Products, Inventory, ProductImages, Users. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:271` → `cart`.

### POST /api/cart/items

Thêm số lượng vào giỏ.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: Không có tham số. Request: `AddCartRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/CartResponse"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, PRODUCT_UNAVAILABLE, INSUFFICIENT_STOCK.
- SQL: Carts, CartItems, Products, Inventory, ProductImages, Users. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:272` → `addCart`.

### PUT /api/cart/items/{id}

Đổi số lượng giỏ.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `QuantityRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/CartResponse"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, PRODUCT_UNAVAILABLE, INSUFFICIENT_STOCK.
- SQL: Carts, CartItems, Products, Inventory, ProductImages, Users. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:273` → `updateCart`.

### DELETE /api/cart/items/{id}

Xóa sản phẩm khỏi giỏ.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/CartResponse"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Carts, CartItems, Products, Inventory, ProductImages, Users. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:274` → `removeCart`.

### GET /api/orders

Đọc lịch sử đơn của khách.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/OrderSummary"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Orders, OrderItems, Products, Payments, OrderStatusHistory. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:275` → `orders`.

### POST /api/checkout/quote

Tính lại tổng giỏ và voucher trước xác nhận.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: Không có tham số. Request: `CheckoutRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/CheckoutQuote"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, EMPTY_CART, PRODUCT_UNAVAILABLE, INSUFFICIENT_STOCK, INVALID_VOUCHER, VOUCHER_MINIMUM, VOUCHER_EXHAUSTED, IDEMPOTENCY_CONFLICT, PAYMENT_PROVIDER_UNCONFIGURED.
- SQL: Addresses, Carts, CartItems, Products, ProductImages, Inventory, Vouchers, VoucherRedemptions. Transaction: Đọc, không reserve tồn.
- Nghiệp vụ: Kiểm địa chỉ và đọc giỏ, tính lại subtotal/discount/shipping/total; không giữ tồn hay tạo đơn. Điều kiện được kiểm lại khi xác nhận.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:276` → `quote`.

### POST /api/orders

Tạo đơn nguyên tử và chống retry trùng.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: header Idempotency-Key bắt buộc. Request: `CheckoutRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `201 {"$ref": "#/components/schemas/OrderDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, EMPTY_CART, PRODUCT_UNAVAILABLE, INSUFFICIENT_STOCK, INVALID_VOUCHER, VOUCHER_MINIMUM, VOUCHER_EXHAUSTED, IDEMPOTENCY_CONFLICT, PAYMENT_PROVIDER_UNCONFIGURED.
- SQL: Users, Addresses, Carts, CartItems, Products, Inventory, InventoryTransactions, Orders, OrderItems, Payments, Shipments, OrderStatusHistory, Vouchers, VoucherRedemptions. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Idempotency-Key bắt buộc16..100 ký tự chữ/số/gạch nối. Khóa user và tồn, fingerprint body, kiểm giá/tồn/voucher, chụp địa chỉ và tên/unit; tạo đơn/paymentCOD PENDING, Orders UNPAID; trừ tồn và xóa giỏ trong một transaction. Khóa cũ cùng body trả đơn cũ, body khác409. Online chưa cấu hình503 trước mọi thay đổi.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:277` → `checkout`.

### GET /api/orders/{id}

Đọc đơn thuộc khách.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/OrderDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Orders, OrderItems, Products, Payments, OrderStatusHistory. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:279` → `order`.

### POST /api/orders/{id}/cancel

Hủy đơn PENDING của khách và trả tồn.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/OrderDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, ORDER_STATE, SHIPMENT_STATE.
- SQL: Users, Orders, OrderItems, Inventory, InventoryTransactions, Payments, VoucherRedemptions, OrderStatusHistory. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:280` → `cancel`.

### GET /api/products/{id}/reviews

Xem và lọc đánh giá thật.

- Màn hình gọi: Tab đánh giá ở chi tiết.
- Quyền: Công khai.
- Tham số: path id bắt buộc; query rating tùy chọn; query page tùy chọn; query size tùy chọn. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/ReviewPage"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR.
- SQL: Reviews, Users, Orders, OrderItems, Products, Categories, Inventory, ProductImages. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:281` → `reviews`.

### POST /api/products/{id}/reviews

Gửi đánh giá sau mua và giao hàng.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `ReviewRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `201 {"$ref": "#/components/schemas/Review"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, REVIEW_EXISTS.
- SQL: Reviews, Users, Orders, OrderItems, Products, Categories, Inventory, ProductImages. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Đơn DELIVERED thuộc khách, có đúng sản phẩm; rating1..5/comment≤1000; một review cho sản phẩm/khách/đơn, trùng409.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:282` → `review`.

### GET /api/favorites

Đọc sản phẩm yêu thích đã lưu.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/ProductSummary"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Favorites, Users, Products, Categories, Inventory, ProductImages, Reviews. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:284` → `favorites`.

### PUT /api/favorites/{id}

Lưu yêu thích.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Favorites, Users, Products, Categories, Inventory, ProductImages, Reviews. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:285` → `favorite`.

### DELETE /api/favorites/{id}

Bỏ yêu thích.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Success"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Favorites, Users, Products, Categories, Inventory, ProductImages, Reviews. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:286` → `unfavorite`.

### GET /api/payment-methods

Đọc phương thức hiện dùng được.

- Màn hình gọi: Checkout.
- Quyền: Công khai.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/PaymentMethod"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR.
- SQL: Không đọc SQL. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:287` → `methods`.

### POST /api/payments/{id}/session

Điểm tích hợp tạo phiên thanh toán online chưa cấu hình.

- Màn hình gọi: Giỏ, checkout, đơn, đánh giá, yêu thích.
- Quyền: CUSTOMER và tài nguyên thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `503 PAYMENT_PROVIDER_UNCONFIGURED; không có giao dịch thành công`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, PAYMENT_PROVIDER_UNCONFIGURED.
- SQL: Orders, Payments cho kiểm quyền; chưa settle/ghi thanh toán online. Transaction: Các thao tác ghi giỏ/địa chỉ/checkout/hủy/review/favorite có @Transactional; đọc không ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:288` → `session`.

### POST /api/payments/webhook

Điểm tích hợp xác minh webhook chưa cấu hình.

- Màn hình gọi: Callback máy chủ.
- Quyền: Không dùng Bearer; nhà cung cấp phải xác minh chữ ký.
- Tham số: header X-Payment-Signature tùy chọn. Request: `ByteArray`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `503 PAYMENT_PROVIDER_UNCONFIGURED; không có giao dịch thành công`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, PAYMENT_PROVIDER_UNCONFIGURED.
- SQL: Không ghi dữ liệu khi provider chưa cấu hình. Transaction: Hiện trả503 trước ghi.
- Nghiệp vụ: Kiểm quyền sở hữu từ session. Giá/tồn đọc lại ở backend. Quantity 1..999 theo đơn vị bán INT của schema.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Commerce.kt:289` → `webhook`.

### GET /api/health

Kiểm tra backend đang phản hồi.

- Màn hình gọi: Kiểm tra tiến trình backend.
- Quyền: Công khai.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Health"}`. Mã HTTP được thiết kế: 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: Không có lỗi nghiệp vụ riêng.
- SQL: Không truy vấn SQL. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Endpoint nhóm trả trạng thái HTTP của ứng dụng; healthUP không tự chứng minh SQL kết nối được.
- Trách nhiệm và nguồn: Phần nhóm được giữ (không gán lại tác giả).
- Sát code: `HealthController.kt:10` → `health`.

### GET /api/products/{id}

Đọc chi tiết đúng productId.

- Màn hình gọi: Trang chủ hoặc chi tiết.
- Quyền: Công khai.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/ProductDetailResponse"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR.
- SQL: Products, Categories, Inventory, ProductImages, Reviews. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: SQL có tham số; dữ liệu động lấy từ SQL Server.
- Trách nhiệm và nguồn: Tái sử dụng chọn lọc logic detail Demo, ghép service/repository; không gán Backend cho FE.
- Sát code: `ProductController.kt:21` → `getProductById`.

### GET /api/products

Đọc collection legacy có dữ liệu nội bộ.

- Màn hình gọi: Client legacy/quản trị.
- Quyền: ADMIN.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/LegacyProduct"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Products. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Giữ List và tên productId/productName/costPrice của nhóm. Thay đổi quyền là chủ đích để giá vốn không công khai; client khách chuyển /api/v1/products. Không đổi âm thầm List thành envelope.
- Trách nhiệm và nguồn: Giữ query/contract nhóm; bổ sung kiểm quyền trong đợt hợp nhất có hỗ trợ AI.
- Sát code: `ProductController.kt:24` → `getAllProducts`.

### GET /api/admin/shippers

Đọc shipper có thể phân công.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: ADMIN.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/Shipper"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Users. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Shipment id khác order id. Shipper chỉ thấy đơn được giao; ASSIGNED→PICKED_UP→IN_TRANSIT→DELIVERED/FAILED; đơn phải PREPARING trước IN_TRANSIT. Giao xong không tự ghi COD đã thu.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:76` → `shippers`.

### POST /api/admin/orders/{id}/assign-shipper

Phân công shipper cho đơn đã xác nhận.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: ADMIN.
- Tham số: path id bắt buộc. Request: `ShipperAssignment`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/ShipmentSummary"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, ORDER_STATE, SHIPMENT_STATE.
- SQL: Users, Orders, Shipments. Transaction: assign/status có @Transactional; location một INSERT; các route đọc không ghi.
- Nghiệp vụ: ADMIN phân công shipper đang active cho đơn CONFIRMED/PREPARING/SHIPPING. Retry cùng shipper giữ nguyên tiến trình; không đổi người sau PICKED_UP/IN_TRANSIT hoặc khi đơn SHIPPING. Shipment/order được khóa trong transaction.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:77` → `assign`.

### GET /api/shipper/shipments

Đọc vận đơn được phân công.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: SHIPPER và vận đơn được phân công.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"type": "array", "items": {"$ref": "#/components/schemas/ShipmentSummary"}}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Shipments, Orders, OrderItems, Payments, Products, OrderStatusHistory. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Shipment id khác order id. Shipper chỉ thấy đơn được giao; ASSIGNED→PICKED_UP→IN_TRANSIT→DELIVERED/FAILED; đơn phải PREPARING trước IN_TRANSIT. Giao xong không tự ghi COD đã thu.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:78` → `list`.

### GET /api/shipper/shipments/{id}

Đọc chi tiết vận đơn được phân công.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: SHIPPER và vận đơn được phân công.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/ShipmentDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Shipments, Orders, OrderItems, Payments, Products, OrderStatusHistory. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Shipment id khác order id. Shipper chỉ thấy đơn được giao; ASSIGNED→PICKED_UP→IN_TRANSIT→DELIVERED/FAILED; đơn phải PREPARING trước IN_TRANSIT. Giao xong không tự ghi COD đã thu.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:79` → `detail`.

### POST /api/shipper/shipments/{id}/accept

Nhận vận đơn và ghi PICKED_UP.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: SHIPPER và vận đơn được phân công.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/ShipmentDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Shipments, Orders, OrderItems, Payments, Products, OrderStatusHistory. Transaction: assign/status có @Transactional; location một INSERT; các route đọc không ghi.
- Nghiệp vụ: Shipment id khác order id. Shipper chỉ thấy đơn được giao; ASSIGNED→PICKED_UP→IN_TRANSIT→DELIVERED/FAILED; đơn phải PREPARING trước IN_TRANSIT. Giao xong không tự ghi COD đã thu.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:80` → `accept`.

### PUT /api/shipper/shipments/{id}/status

Cập nhật tiến trình giao được phép.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: SHIPPER và vận đơn được phân công.
- Tham số: path id bắt buộc. Request: `OrderStatusRequest`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/ShipmentDetail"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, ORDER_STATE, SHIPMENT_STATE.
- SQL: Shipments, Orders, OrderItems, Payments, Products, OrderStatusHistory. Transaction: assign/status có @Transactional; location một INSERT; các route đọc không ghi.
- Nghiệp vụ: Shipment id khác order id. Shipper chỉ thấy đơn được giao; ASSIGNED→PICKED_UP→IN_TRANSIT→DELIVERED/FAILED; đơn phải PREPARING trước IN_TRANSIT. Giao xong không tự ghi COD đã thu.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:81` → `status`.

### POST /api/shipper/locations

Lưu tọa độ shipper chủ động báo.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: SHIPPER và vận đơn được phân công.
- Tham số: Không có tham số. Request: `ShipperLocation`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/LocationRecord"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: ShipperLocations. Transaction: assign/status có @Transactional; location một INSERT; các route đọc không ghi.
- Nghiệp vụ: Shipment id khác order id. Shipper chỉ thấy đơn được giao; ASSIGNED→PICKED_UP→IN_TRANSIT→DELIVERED/FAILED; đơn phải PREPARING trước IN_TRANSIT. Giao xong không tự ghi COD đã thu.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:82` → `location`.

### GET /api/orders/{id}/shipment

Đọc tiến trình giao của đơn khách.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: CUSTOMER và đơn thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/ShipmentTracking"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Orders, Shipments, Users. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Shipment id khác order id. Shipper chỉ thấy đơn được giao; ASSIGNED→PICKED_UP→IN_TRANSIT→DELIVERED/FAILED; đơn phải PREPARING trước IN_TRANSIT. Giao xong không tự ghi COD đã thu.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:83` → `tracking`.

### GET /api/orders/{id}/shipment/location

Đọc vị trí gần đây có thời điểm và nhãn nguồn.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: CUSTOMER và đơn thuộc session.
- Tham số: path id bắt buộc. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/CustomerLocation"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: Orders, Shipments, ShipperLocations. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Khách phải sở hữu đơn SHIPPING; bản ghi vị trí gần nhất trong24h từ shipper được xác thực; response realtime=false và source=AUTHENTICATED_SHIPPER_REPORT. Không phải GPS nền hoặc tracking realtime tự động.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:84` → `customerLocation`.

### GET /api/shipper/routes

Điểm tích hợp tối ưu tuyến chưa cấu hình.

- Màn hình gọi: Giao hàng, quản lý đơn hoặc chi tiết đơn khách.
- Quyền: SHIPPER và vận đơn được phân công.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `503 MAP_PROVIDER_UNCONFIGURED`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN, MAP_PROVIDER_UNCONFIGURED.
- SQL: Shipments, Orders, OrderStatusHistory, Users, ShipperLocations; ProductRecommendations khi gợi ý. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Chưa có nhà cung cấp bản đồ/tối ưu tuyến; trả503 MAP_PROVIDER_UNCONFIGURED, không bịa tuyến.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:85` → `routes`.

### GET /api/recommendations

Đọc gợi ý đã lưu hoặc catalog gần đây có nhãn nguồn.

- Màn hình gọi: Sản phẩm gợi ý.
- Quyền: CUSTOMER và đơn thuộc session.
- Tham số: Không có tham số. Request: `Không có request body`; field/type/validation xem schema cùng tên trong OpenAPI.
- Response: `200 {"$ref": "#/components/schemas/Recommendations"}`. Mã HTTP được thiết kế: 400, 404, 409, 503, 500, 401, 403; chỉ các điều kiện tương ứng mới phát sinh. Nhóm code cần xử lý khi có điều kiện tương ứng: INVALID_INPUT, NOT_FOUND, DATA_CONFLICT, DATABASE_UNAVAILABLE, INTERNAL_ERROR, AUTH_REQUIRED, SESSION_EXPIRED, FORBIDDEN.
- SQL: ProductRecommendations, Products, Categories, ProductImages, Inventory, Reviews. Transaction: Đọc, không ghi dữ liệu.
- Nghiệp vụ: Nếu có ProductRecommendations trả RECORDED_RECOMMENDATIONS, nếu chưa có trả RECENT_CATALOG có nhãn nguồn; không giả thuật toán cá nhân hóa.
- Trách nhiệm và nguồn: Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
- Sát code: `Shipping.kt:86` → `recommendations`.

## Thanh toán online và bản đồ chưa nghiệm thu

`OnlinePaymentProvider` có contract requireConfigured/createSession/verifyWebhook và VerifiedPaymentEvent, nhưng implementation hiện là UnconfiguredPaymentProvider. API session/webhook luôn503; chưa có provider adapter thật, lưu event webhook chống lặp hoặc cơ chế settle thanh toán online đã được nghiệm thu. Không coi redirect hoặc payload tự gửi là thành công. Cần chọn nhà cung cấp, cấu hình sandbox/credential kín, triển khai xác minh chữ ký/số tiền/transactionRef/eventId và kiểm thử lặp trước khi bật available. Không tạo giao dịch thật hoặc lưu dữ liệu thẻ nhạy cảm trong bản này.

ShipperLocations lưu dữ liệu shipper báo với thời điểm, customerLocation chỉ trả bản ghi gần đây và realtime=false. Chưa có GPS nền, cơ chế phát realtime hoặc provider tối ưu tuyến. `/api/shipper/routes` trả503 rõ ràng. Địa chỉ snapshot trong shipment là dữ liệu đơn; không suy ra đã geocode hoặc định tuyến.

## Đọc schema và kiểm chứng

Các DTO request được trích từ data class và annotation validation; response Map được đối chiếu SELECT alias/mapOf. OpenAPI khai báo response là hợp đồng, không phải kết quả test. Report/SQL/ảnh UI và acceptance từng chức năng nằm trong báo cáo kiểm thử và ma trận. `tools/e2e_master.py` và test Kotlin là ví dụ thực thi có kiểm tra lỗi, khác các lệnh minh họa ngắn phía trên.

Route cần Bearer còn đọc Users/SessionTokens để xác thực và role, ngoài các bảng nghiệp vụ liệt kê ở từng endpoint. Request DTO không chấp nhận field không khai báo theo cấu hình fail-on-unknown-properties; ví dụ client không được gửi role để tự cấp quyền khi đăng ký. Endpoint có body Map không được suy từ OpenAPI thành cho phép thay đổi schema.
