# Backend SmartMart Master

Phần nền của nhóm được giữ: entrypoint Spring Boot, HealthController, Gradle Kotlin DSL và dependency/toolchain thực tế. ProductController vẫn giữ collection dạng List với các trường productId/productName và truy vấn nhóm. Endpoint collection cũ nay yêu cầu ADMIN vì trả giá vốn; Android mới dùng DTO công khai tại `/api/v1/products`. Đây là thay đổi quyền có chủ đích, cần hướng dẫn client cũ chuyển đổi; không coi nó tương thích về quyền truy cập ẩn danh.

## Nguồn và phần bổ sung

Detail tái sử dụng có chọn lọc ý tưởng SQL có tham số, chọn ảnh chính theo thứ tự xác định và contract id/name/unit nullable từ Demo `SmartMart/Code/SmartMart-main`. SQL chuyển vào ProductQueryRepository, orchestration vào CatalogService; Controller nhận ID và gọi service. Bốn test Demo được giữ các assertion nghiệp vụ và đổi điểm kiểm tra sang Repository/Service. Không gán tác giả Backend nhóm hoặc toàn bộ phần mới cho Lê Tuấn Thành.

Auth.kt, ApiErrors.kt, các phần mở rộng Catalog.kt, Commerce.kt, Admin.kt và Shipping.kt là **Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI**. Phân công Sprint/CSV là bằng chứng giao việc, không phải bằng chứng ai đã viết các file mới. Chi tiết API, quyền và DTO nằm ở API_REFERENCE.md và openapi.json.

## Hành vi đã triển khai

| Phạm vi | Thực hiện trong code |
|---|---|
| Tài khoản | PBKDF2 có salt; Bearer token ngẫu nhiên chỉ lưu SHA256 trong SQL; phiên có hạn; đăng ký CUSTOMER; đăng nhập bằng email hoặc số điện thoại trong trường request tên email để giữ contract; đăng xuất; hồ sơ tên/phone/email; uniqueness email/phone và quyền từ Users |
| Sản phẩm | DTO công khai không có CostPrice; category/brand metadata; tìm kiếm có escape ký tự LIKE, lọc kết hợp giá/danh mục/đánh giá/tồn/thương hiệu; sort allowlist; page/size có giới hạn; detail, ảnh/spec, liên quan, số bán/điểm đánh giá từ DB |
| Giỏ và địa chỉ | Sở hữu theo session; cập nhật giỏ lưu SQL; giá/tồn được đọc lại; mặc định địa chỉ có transaction; snapshot địa chỉ trong đơn không bị sửa theo sổ địa chỉ |
| Đặt hàng | Transaction khóa theo tài khoản, sản phẩm/tồn và voucher; giá BigDecimal/DECIMAL; khóa retry cùng nội dung trả lại cùng đơn; thay nội dung cùng khóa trả409; trừ tồn, order items, payment, shipment/history và xóa giỏ trong cùng transaction |
| Thanh toán COD | Đơn mới UNPAID; giao thành công tự nó chưa là đã thu tiền; chỉ ADMIN xác nhận đã thu đúng số tiền sau DELIVERED mới chuyển PAID; thao tác xác nhận lặp không thu/trừ lần hai |
| Đánh giá/yêu thích | Kiểm tra đơn thuộc người gửi, sản phẩm trong đơn DELIVERED; chặn trùng đánh giá trong cùng đơn; xem/lọc/phân trang và tổng hợp SQL; yêu thích lưu theo tài khoản |
| Quản trị/kho | CRUD mềm sản phẩm/danh mục; ADMIN hoặc WAREHOUSE_MANAGER; tồn, nhập/điều chỉnh/kiểm kê và lịch sử; chống tồn âm; retry key cho inventory; lowStock theo reorderLevel; quản lý voucher/usage; user search/soft disable; báo cáo doanh thu đã thu theo thời gian, đơn, bán chạy và giao hàng |
| Xử lý đơn | Trạng thái theo đồ thị cho phép; hủy khôi phục tồn một lần và giải phóng voucher; yêu cầu hoàn COD và xác nhận tiền mặt thực tế đã trả riêng; giữ FK và lịch sử |
| Giao hàng | ADMIN phân công shipper; chỉ shipper được giao xem/nhận/cập nhật vận đơn; retry cùng phân công giữ tiến độ, chặn đổi người sau nhận hàng; khách chỉ xem vận đơn/vị trí của đơn mình; vị trí là bản báo của shipper với thời điểm, realtime=false |

Collection list cũ dùng productId/productName; detail giữ id/name. Không âm thầm đổi List cũ thành envelope. API v1 dùng envelope riêng cho phân trang. Quantity vẫn INT theo đơn vị bán trong schema nhóm, không tự ép các mặt hàng thành kg. Unit không có sẽ không được thêm giả vào DTO.

## Database và vận hành

Chọn `database/Create_20_Tables_SQLServer.sql` làm nền; schema trong database_N trùng hash và không chạy lần thứ hai. File schema/seed nhóm được giữ nguyên. Công cụ tạo **database riêng SmartMartMaster_20261105**, thêm các migration V001–V005 theo thứ tự và seed 8 sản phẩm có IsDemo/SKU MASTER-DEMO. Không DROP/TRUNCATE, không ghi vào DB SmartMart của nhóm.

Một lần seed ban đầu bị đọc sai codepage bởi sqlcmd, được phát hiện khi xem Android chạy thật. Initializer hiện chỉ định `-f 65001`; V005 sửa đúng văn bản của 8 SKU demo có IsDemo=1 và danh mục chỉ chứa hàng demo, không thay ID, giá, tồn, đơn, snapshot hay bản ghi không phải demo. Snapshot đơn từ đợt test trước sửa được giữ nguyên vì là lịch sử. `tools/verify-backend-unicode.py` đọc JSON UTF-8 thật và xác minh tên/đơn vị/danh mục/spec cùng SHA256 11 ảnh HTTP (gồm hai ảnh phụ gallery Táo đỏ được bổ sung có kiểm soát); bằng chứng ở backend-unicode-api.json và backend-image-api.json.

`tools/initialize-sql-master.ps1` dùng Windows Authentication hiện có để tạo DB/login scoped và áp migration; có thể tiếp tục nếu bước tạo bảng trước đó dở dang. Login ứng dụng chỉ có SELECT/INSERT/UPDATE/DELETE trên schema dbo của DB Master. Mật khẩu ngẫu nhiên lưu `.local/backend.env.ps1`, ACL current user và SYSTEM; không đưa vào Git, log, báo cáo hoặc gói nguồn. Không tự đổi mật khẩu login có sẵn khi thiếu file credential.

Spring không tự đọc `.env`. `backend/.env.example` chỉ là tên biến/placeholder; `tools/start-backend.ps1` nạp cấu hình cục bộ rõ ràng. Script `Use-LocalBuildEnvironment.ps1` đặt tùy chọn JVM trong tiến trình để JDK fallback TCP khi AF_UNIX trên máy này gặp lỗi; không sửa JDK/firewall/registry và không hạ phiên bản dependency.

SQL phiên bản thật đã quan sát 17.0.1145.4. Backend giữ Java toolchain21, daemon25, wrapper9.8.2, Spring Boot4.1.2/Kotlin2.3.21. Kết quả build/test mới đọc từ XML và log hiện tại, không dùng số33/9/4 trong tài liệu cũ.

## Kiểm thử và bằng chứng

- ProductControllerTest: 4 test Demo được thích nghi, giữ bound parameter, thứ tự ảnh chính, nullable unit/description/image,404 và không query cho ID không dương; đây là mock JDBC, được ghi riêng với SQL integration.
- DomainRulesTest: password salt/verify, lọc/sort/range không hợp lệ, adapter chưa cấu hình không chấp nhận webhook và thông báo lỗi không lộ exception.
- SqlServerIntegrationTest: DB_NAME guard chỉ cho DB Master; thực thi service proxy/transaction trên SQL Server thật, gồm cạnh tranh mua đơn vị cuối, retry đồng thời cùng khóa, rollback khi voucher/payment chặn, snapshot đơn, hủy lặp, token hash/ownership, inventory retry rollback, unknown stock và phân công shipper lặp không làm mất tiến độ.
- Test gốc SmartmartBackendApplicationTests được giữ. HTTP E2E độc lập do root chạy với server thật kiểm quyền, validation, API/SQL, COD/review/shipping/voucher/report; xem docs/audit/e2e-*-current.json.
- `tools/verify-sql-repeat.ps1` áp lại migration/seed và so SHA256 các định nghĩa demo/test cùng đơn test đã lưu; kiểm số seed và trạng thái FK. Script cố ý không so tồn kho đang có thể thay đổi bởi Android chạy thật.

Kết quả cuối, lệnh, exit code và số test được tổng hợp trong BAO_CAO_KIEM_THU.md. Các dữ liệu test có nhãn TEST/E2E được giữ để đối soát; sản phẩm/user do JUnit tạo được ngừng kinh doanh/vô hiệu hóa sau test để không chiếm danh mục khách hàng.

## Phần chưa nghiệm thu bên ngoài

Ví điện tử và thẻ có interface OnlinePaymentProvider cùng adapter chưa cấu hình. Tạo phiên/webhook đều trả503; checkout dừng trước khi thay đổi giỏ/tồn/đơn. Không có kết nối nhà cung cấp, giao dịch thật, xác minh callback thành công hoặc lưu dữ liệu thẻ. Để nối provider thật còn cần chọn nhà cung cấp, credential, chữ ký/callback, đối soát amount/order và xử lý event trùng; hiện không báo hoàn tất online.

Tối ưu tuyến đường trả503 vì chưa có nhà cung cấp bản đồ. API ghi vị trí shipper và tra cứu bản báo gần đây đã có; chưa phải luồng GPS liên tục hoặc bản đồ realtime. Không tạo tọa độ giả cho người dùng. Hoàn tiền online chưa có provider; hoàn COD là ghi nhận xác nhận của người có quyền sau khi tiền mặt thực tế đã trả, không tự thực hiện giao dịch ngân hàng.

