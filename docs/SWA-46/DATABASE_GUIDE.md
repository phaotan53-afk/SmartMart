# Hướng dẫn dữ liệu SmartMart Master

Schema chuẩn của bản Master là `database/Create_20_Tables_SQLServer.sql` sao chép từ nguồn nhóm. Bản `database_N/database/Create_20_Tables_SQLServer.sql` có cùng SHA256 `12ac1133dd12d416c7e100cebddd5ac68ced2ea716b2d256e2a058fd5f3983f8`; chỉ chạy một nguồn schema. `database_N` được giữ để truy vết lịch sử, không phải module cần cài thêm.

Không chạy schema hoặc seed vào database nhóm. Công cụ `tools/initialize-sql-master.ps1` hướng tới database phát triển riêng `SmartMartMaster_20261105`, dùng phiên Windows SQL có quyền tạo database/login giới hạn cho môi trường này. Không có DROP/TRUNCATE, không chạy installer, đổi service hoặc firewall. Kết quả SQL thật, lần migration mới/nâng cấp và seed lặp phải xem trong `BAO_CAO_KIEM_THU.md`; nội dung hướng dẫn này không tự chứng minh các lệnh đã thành công.

## Các bảng nền và quan hệ

| Nhóm | Bảng | Quan hệ chính |
|---|---|---|
| Tài khoản | Users, Addresses | Addresses.UserID→Users; unique(UserID,AddressID) để bảo đảm địa chỉ đơn thuộc khách |
| Danh mục | Categories, Products, ProductImages | Products.CategoryID→Categories; ảnh→Products; ảnh ưu tiên IsPrimary/DisplayOrder/ProductImageID |
| Giỏ | Carts, CartItems | Carts.UserID duy nhất; CartItems unique(CartID,ProductID); Quantity>0 |
| Đơn | Orders, OrderItems, OrderStatusHistory | Đơn→khách, địa chỉ của khách, voucher; dòng đơn→đơn và sản phẩm; lịch sử→đơn/người đổi |
| Thanh toán | Payments | Payments.OrderID→Orders; phương thức COD/EWALLET/CARD/BANK_TRANSFER; trạng thái PENDING/SUCCESS/FAILED/REFUNDED |
| Giao hàng | Shipments, ShipperLocations | Shipment một cho mỗi đơn; ShipperID→Users; vị trí→shipper và giới hạn tọa độ |
| Kho | Inventory, InventoryTransactions | Inventory.ProductID duy nhất; transaction→inventory và user; không cho tồn âm |
| Khuyến mãi | Vouchers, VoucherRedemptions | Redemption→voucher/khách/đơn; mỗi đơn tối đa một redemption |
| Tương tác | Reviews, Favorites, ProductRecommendations | Review→sản phẩm/khách/đơn; Favorites unique(khách,sản phẩm); gợi ý unique(khách,sản phẩm) |

PK của các bảng nền được giữ nguyên. Schema thật dùng Addresses, Inventory, Reviews, Shipments, InventoryTransactions; một số PK/FK CSV và DBML/ERD nguồn là tài liệu của mô hình khác, có UserAddresses, Inventories, ProductReviews, Deliveries, Refunds, StockMovements. Không chạy truy vấn theo tên cũ hoặc coi Refunds đã tồn tại chỉ vì CSV ghi tên. Dùng DDL nền và migration thực tế để tra tên chính xác; giữ những tài liệu nguồn như chứng cứ lịch sử.

Xuất schema từ database Master thật bằng `tools/export-schema.ps1` được lưu ở `docs/audit/schema-current.json`, `Primary_Keys_CURRENT.csv` và `Foreign_Keys_CURRENT.csv`. Bản xuất tại 2026-11-05T07:11:02.5235966+07:00 ghi25 bảng,28 cột thuộc PK và36 cột thuộc FK; số cột khóa khác số constraint khi khóa ghép. Đây là số của snapshot đó, không suy diễn là snapshot sau mọi migration nếu timestamp chưa cập nhật. Đây là nguồn tra cứu hiện tại, tách biệt các CSV lịch sử được giữ nguyên. Nếu chạy migration mới, cần xuất lại để đồng bộ các cột mới và thời điểm kiểm chứng.

Orders có FK ghép (CustomerID,AddressID)→Addresses(UserID,AddressID), vì vậy backend không được dùng địa chỉ tài khoản khác. Cart/order/review/favorite vẫn cần kiểm quyền trong nghiệp vụ; FK không thay xác thực. Role Users chỉ nhận ADMIN, WAREHOUSE_MANAGER, CUSTOMER, SHIPPER.

## Migration bổ sung

Chạy các file trong `database/migrations` theo thứ tự tên. `V001__master_extensions.sql` bổ sung Brand và IsDemo cho Products, IdempotencyKey và CheckoutFingerprint cho Orders; tạo SessionTokens, ProductSpecifications, RefundRequests và bảng theo dõi SchemaMigrations.

SessionTokens lưu SHA256 của token và hạn dùng, không lưu token bearer thô. ProductSpecifications lưu cặp tên/giá trị theo sản phẩm để UI chỉ hiển thị thông tin có nguồn. RefundRequests lưu yêu cầu hoàn tiền, không phải bằng chứng nhà cung cấp đã trả tiền. Unique index theo CustomerID/IdempotencyKey bảo vệ retry checkout; unique index review theo sản phẩm/khách/đơn hỗ trợ chống gửi đánh giá trùng.

Migration kiểm tra tồn tại trước khi thêm cột/bảng/index để có thể chạy lại. Script khởi tạo tạo bản schema có guard trong `.local`, giữ nguyên file DDL nguồn; không xóa bảng đã có. Trường hợp database tồn tại nhưng có schema khác không được tự coi là nâng cấp hợp lệ: cần đối chiếu cột/ràng buộc trước và dừng khi lỗi. Không dùng việc có bản ghi SchemaMigrations như thay thế kiểm tra toàn bộ ràng buộc.

`V002__order_snapshots.sql` thêm Orders.AddressSnapshot cùng OrderItems.ProductNameSnapshot/UnitSnapshot. Checkout chụp thông tin nhận hàng, tên và đơn vị vào đơn; sửa hồ sơ/sản phẩm sau đó không làm mất nội dung đã mua. Các cột nullable giúp dữ liệu cũ còn đọc được qua fallback; không backfill bằng thông tin hiện tại rồi gọi đó là ảnh chụp lịch sử chính xác.

`V003__inventory_idempotency.sql` tạo MutationKeys với khóa ghép UserID/OperationType/RequestKey và hash nội dung. Thao tác nhập/điều chỉnh/kiểm kê dùng khóa này cùng transaction để retry không làm thay đổi tồn hai lần; key cũ với nội dung khác trả409. Thất bại phải rollback cả khóa mới lẫn thay đổi tồn.

`V004__unique_optional_phone.sql` tạo unique index có điều kiện cho Users.Phone khác null/rỗng. Auth chuẩn hóa phone, kiểm trùng email/phone khi đăng ký hoặc đổi hồ sơ. Nếu nâng cấp gặp phone trùng trong dữ liệu đã có, migration THROW và dừng, không tự sửa/xóa người dùng để ép qua. Cần người quản lý dữ liệu giải quyết chủ động trước khi chạy lại.

`V005__repair_demo_utf8.sql` khắc phục lỗi codepage của lần seed phát triển đầu. Chỉ sửa các trường văn bản có mẫu mojibake trên SKU MASTER-DEMO đã biết và IsDemo=1, ảnh/spec tương ứng, danh mục không chứa sản phẩm thật và mô tả voucher MASTER10 bị lỗi. Migration không đổi ID, giá, tồn, đơn, snapshot lịch sử hoặc số lượt voucher, không ghi vào database nhóm. Script khởi tạo dùng sqlcmd -f65001 và đọc baseline UTF-8. Bằng chứng API UTF-8 mới nằm ở docs/audit/backend-unicode-api.json; các snapshot đơn thử tạo trước sửa vẫn được giữ nguyên thay vì bịa lại lịch sử.

## Giá, số lượng và transaction

Giá bán, đơn giá, tổng tiền, giảm giá và thanh toán dùng DECIMAL; Kotlin dùng BigDecimal. Dòng đơn giữ UnitPrice lúc mua, LineTotal được SQL tính. Tổng đơn phải do backend tính từ DB, không tin giá/tổng do client gửi.

CartItems.Quantity, OrderItems.Quantity, Inventory.QuantityOnHand và QuantityChange của schema nền là INT. Bản này bán số nguyên của đơn vị Products.Unit, ví dụ1 hộp,1 bó hoặc1 kg theo dữ liệu; chưa có contract bán0,5 kg. Muốn bán lẻ theo cân phải có migration và contract số lượng thập phân, không sửa riêng UI rồi làm mất độ chính xác. Không tự thêm hậu tố /kg khi Unit thiếu.

Khi ghi giỏ/đặt hàng, backend kiểm lại active/tồn/giá. Checkout phải giữ transaction cho đơn, dòng đơn, thanh toán chờ, tồn kho, lịch sử và voucher; khóa dòng hoặc cập nhật có điều kiện cùng kiểm tra affected rows để tránh vượt tồn. Khi lỗi phải rollback, retry không tạo đơn hay trừ tồn thêm. Các quy tắc cụ thể sát code được giải thích ở API_REFERENCE và kết quả cạnh tranh trong báo cáo kiểm thử.

## Seed và tài nguyên demo

`database/Seed_Data.sql` là seed lịch sử của nhóm. File này có USE SmartMart và các dữ liệu/khóa mẫu; không chạy nguyên file vào database Master hoặc database đã có dữ liệu. Bản Master dùng `database/seeds/V001__demo_catalog.sql` sau migration, chọn đúng database phát triển.

Seed Master tạo danh mục demo nếu thiếu, thêm8 sản phẩm bằng SKU ổn định MASTER-DEMO, IsDemo=1, ảnh `/assets/products/*.jpg`, tồn ban đầu40 và ngưỡng5 cho sản phẩm demo chưa có Inventory. Seed thêm thông số gắn nhãn demo và voucher MASTER10 giảm10 phần trăm. Những giá trị này phục vụ kiểm thử, không phải thuộc tính thương mại được xác nhận.

Seed dùng NOT EXISTS, không ghi đè sản phẩm đã có cùng SKU, không reset tồn đang dùng và không gán review/rating/doanh số mẫu. Chạy lặp phải kiểm tra số dòng/khóa không tăng sai và giá/tồn cũ không đổi. Sản phẩm trùng SKU mà không được gắn IsDemo không bị seed bổ sung ảnh/thông số như dữ liệu của Master.

## Cấu hình và cách chạy

Từ thư mục gốc Master trong PowerShell có quyền SQL phù hợp:

```powershell
.\tools\Test-Environment.ps1
.\tools\initialize-sql-master.ps1 -Server localhost
.\tools\start-backend.ps1 -Build
```

Script khởi tạo không tự tăng quyền Windows. Khi thiếu kết nối/quyền, sửa điều kiện môi trường hoặc cung cấp endpoint được phép; không xóa DB nhóm để xử lý lỗi. `-Server` hiện dùng cho kết nối quản trị và URL JDBC phát triển được sinh theo cấu hình script; với named instance/port khác phải rà soát URL SMARTMART_DB_URL thực tế trước khi chạy backend.

Credential phát triển được tạo cục bộ trong `.local/backend.env.ps1`, ACL giới hạn người dùng hiện tại và SYSTEM. Không đưa file này vào Git, ZIP bàn giao hoặc ảnh/log. Nếu login đã tồn tại mà không có credential cục bộ, script dừng và không đổi mật khẩu. Chỉ `.env.example` có placeholder được chia sẻ; Spring không tự đọc .env. `start-backend.ps1` nạp cấu hình PowerShell cục bộ nếu có hoặc dùng biến môi trường do người chạy cấp.

## Kiểm tra sau migration

Kiểm tra database hiện tại, danh sách cột/ràng buộc, số sản phẩm demo, FK không có orphan và giá/tồn sau seed lặp. Gọi ít nhất hai sản phẩm qua API rồi đối chiếu SQL. Tạo giỏ/đơn thật bằng tài khoản thử; kiểm rollback và cạnh tranh tồn, không chỉ SELECT đếm bảng. Các lệnh kiểm tra, database/instance đã dùng, dữ liệu và exit code phải có trong báo cáo kiểm thử; thiếu bằng chứng ghi Chưa chạy hoặc Bị chặn.
