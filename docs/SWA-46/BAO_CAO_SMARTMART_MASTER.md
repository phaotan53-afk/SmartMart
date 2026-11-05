# Báo cáo hợp nhất và kiểm chứng SmartMart Master

**Dự án của nhóm SmartMart và phần tích hợp của Lê Tuấn Thành**

Ngày báo cáo 05 tháng 11 năm 2026. Nền tảng Android Kotlin Jetpack Compose, Backend Kotlin Spring Boot và Microsoft SQL Server.

SmartMart Master được xây từ nội dung nguồn nhóm đang có trên đĩa, ghép chọn lọc phần chi tiết sản phẩm trong Demo và bổ sung các nghiệp vụ còn thiếu. Bản hợp nhất có luồng mua sắm dùng dữ liệu SQL Server thật, phân quyền theo phiên đăng nhập và bảng truy nguyên từng file. Chức năng kết nối ví điện tử, thẻ và tối ưu tuyến đường cần nhà cung cấp; chưa được nghiệm thu như giao dịch ngoài thực tế.

## Kết quả và phạm vi bàn giao

Backend đã vượt qua 17 test JUnit, trong đó 8 test dùng SQL Server thật, cùng 210 assertion HTTP ở ba kịch bản nghiệm thu. Android đã có 42 unit test đạt; kiểm tra UI native và ảnh kích thước rộng đang được hoàn tất. Kết quả cuối của giao diện được cập nhật ở phần kiểm thử trước khi giao.

| Thành phần | Kết quả cụ thể |
|---|---|
| Nguồn | 841 file nhóm và 4303 file Demo khớp SHA256 ban đầu; Git state không đổi |
| Dữ liệu | DB Master riêng, 5 migration bổ sung, 8 sản phẩm demo có nhãn và 35 FK hoạt động |
| Hợp đồng | 70 API operations và 68 OpenAPI schemas, có kiểm tra coverage và response thật |
| Phân công | 59 chức năng backlog cùng yêu thích; tách người được giao, tác giả có bằng chứng và phần AI bổ sung |
| Bàn giao | Android, Backend, database, tools và tài liệu tiếng Việt tại SmartMart-Master |

Đường dẫn mở dự án là `D:\TuanThanh\Workspace\Projects\SmartMart-Master\android`. Backend chạy bằng `tools/start-backend.ps1`; cấu hình cục bộ và credential nằm ngoài gói nguồn. Báo cáo kiểm thử chi tiết, OpenAPI và ma trận chức năng đi kèm để đối soát từng kết luận.

:::pagebreak

## Mục lục

:::toc

:::pagebreak

## Nền tảng hợp nhất và nguồn gốc đóng góp

Nguồn nhóm chỉ có màn danh sách Android gọi HTTP trực tiếp, API collection sản phẩm và health, schema cùng các test mẫu. Demo bổ sung detail domain, DTO validation, repository, ViewModel có cancellation và các test có thể tái sử dụng. Hai bản SWA36 trùng nội dung không được tính thành hai đóng góp độc lập. AuthenDemo Firebase và ứng dụng JSONPlaceholder không được ghép thành xác thực SmartMart.

| Nhóm file | Phần được giữ | Phần sửa hoặc bổ sung |
|---|---|---|
| MainActivity và Android | Class, package, onCreate, theme, launcher và cấu hình nền | Thay khối gọi HTTP trong Composable bằng StoreRoot và ViewModel để UI nhận state/phát event |
| Detail từ Demo | DTO/domain, kiểm tra ID/giá/đơn vị, 404 khác 500, cancellation và test hữu ích | Nối đúng ID từ catalog, giỏ thật, ảnh, đánh giá và điều hướng |
| Backend nhóm | Entrypoint, health và tên trường List cũ | DTO công khai, service, auth, transaction và nghiệp vụ giỏ/đơn/kho/giao/đánh giá |
| SQL nhóm | Schema và script gốc, PK/FK và dữ liệu nguồn | DB phát triển riêng, migration bổ sung và seed demo lặp an toàn |
| Tài liệu | Tài liệu lịch sử và bằng chứng từng đường dẫn | Đối chiếu mới theo code, kết quả test thực tế, API và báo cáo này |

API collection cũ vẫn là List với productId/productName. Vì có costPrice, endpoint này yêu cầu ADMIN; ứng dụng công khai dùng API v1 không có giá vốn. Đây là thay đổi quyền có chủ đích, được ghi rõ để client cũ có đường chuyển đổi; không âm thầm đổi List thành envelope.

### Phần việc của Lê Tuấn Thành

Sprint giao Lê Tuấn Thành FE và tích hợp chi tiết sản phẩm với mã SWA-36; Backend chi tiết được giao người khác trong tài liệu Sprint. CSV backlog gọi chi tiết là SWA-24, còn SWA-36 là danh sách đơn. Các khóa PRODUCT_DETAIL và ADMIN_ORDER_LIST tách hai nghĩa này. COD, ví/thẻ và gửi/xem đánh giá thuộc phần được giao cho Thành trong CSV SWA-14/15/16/21/22 và vẫn nằm trong nghiệm thu.

Tác giả Git của nền nhóm được giữ theo bằng chứng; không gắn tên Thành lên toàn bộ code nhóm hoặc Backend. Phần mới được ghi “Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI”. Bảng NGUON_GOC_THAY_DOI.csv và PHAN_VIEC_LE_TUAN_THANH.md truy từ chức năng tới file, nguồn, phần sửa và test.

:::pagebreak

## Kiến trúc và luồng hoạt động

![Thành phần hiện tại và luồng dữ liệu đã kiểm chứng](assets/architecture-master.png)

Compose chỉ hiển thị state và phát event. StoreViewModel quản lý phiên làm việc, catalog và thao tác mua hàng; ProductDetailViewModel giữ hành vi tải chi tiết và cancellation được tái sử dụng. Repository kiểm dữ liệu trước khi chuyển DTO thành domain. Backend dùng controller, service và JDBC có tham số; giao dịch ghi được kiểm quyền và thực hiện trong transaction SQL Server.

### Ví dụ từ danh sách đến đơn COD

1. Catalog tải dữ liệu theo tìm kiếm, danh mục, khoảng giá, đánh giá, tồn, thương hiệu, sort và page từ API công khai.
2. Khi chọn sản phẩm, detail nhận đúng productId và trả state Loading, Success, Error hoặc NotFound. Ảnh lỗi có fallback riêng; thiếu đơn vị không tự thêm kg.
3. Thêm giỏ chỉ báo thành công sau khi server lưu theo tài khoản. Server kiểm lại sản phẩm, giá và tồn; giỏ mở lại được từ SQL sau đăng nhập lại.
4. Người dùng nhập địa chỉ bằng UI, nhận báo giá từ server và tạo đơn với Idempotency-Key. Cùng yêu cầu thử lại trả cùng đơn; nội dung khác với khóa cũ trả xung đột.
5. Transaction khóa tồn và voucher, tạo đơn, dòng hàng, thanh toán, vận đơn, lịch sử và dọn giỏ. Tranh mua đơn vị cuối chỉ có một giao dịch thành công.
6. Đơn COD mới là UNPAID. Giao xong vẫn chưa tự xác nhận đã thu tiền. Gửi đánh giá yêu cầu đơn của chính tài khoản đã giao và có sản phẩm tương ứng; aggregate lấy từ DB.

Giá dùng BigDecimal và DECIMAL. Số lượng theo đơn vị bán INT trong schema nhóm; không tự ép mọi sản phẩm thành kg. Snapshot địa chỉ/tên hàng/đơn vị trong đơn giữ thông tin lịch sử khi dữ liệu hiện tại được cập nhật.

:::pagebreak

## Giao diện native trên điện thoại

Hai ảnh tham chiếu định hướng màu xanh, card trắng, font Inter, banner thực phẩm, catalog và detail thiên về ảnh. Điện thoại xếp dọc, bộ lọc gọn và thanh điều hướng dưới; tablet dùng sidebar và hai cột. Không dùng WebView hoặc ảnh chụp nguyên trang làm UI.

:::pair screenshots/narrow-home-live.png|screenshots/narrow-detail-live.png|Ảnh ứng dụng chạy thật trên emulator API 37 với dữ liệu từ SQL Server Master

Ảnh sản phẩm được phục vụ từ Backend, có giấy phép và danh mục nguồn. Tên, giá, đơn vị, tồn và điểm đánh giá lấy từ API. Dữ liệu demo được ghi nhãn; không đưa số 4.8/126, xuất xứ, hạn dùng hoặc lời hứa giao hàng trong ảnh mẫu thành dữ kiện thật.

:::pagebreak

## Giao diện cửa sổ rộng và khả năng thích ứng

Ảnh rộng sẽ được cập nhật từ ứng dụng đang chạy sau vòng kiểm tra cuối. Sidebar và lưới sản phẩm cùng màn chi tiết hai cột phải hoạt động trên cùng code Compose, không phải một website thay thế. Kiểm tra font scale và trạng thái lỗi được ghi riêng trong bằng chứng UI.

Các màn ngoài hai ảnh dùng chung màu, typography, card, nút và thông báo tiếng Việt. Thao tác tài khoản, địa chỉ, giỏ, đơn, đánh giá và quản trị đi qua API thật; role trên giao diện không tự cấp quyền Backend.

:::pagebreak

## Kiểm thử và bằng chứng mới

Baseline được chạy trong bản sao riêng, không build trong nguồn. Android ban đầu thiếu wrapper JAR; Gradle gặp lỗi Windows AF_UNIX; context Backend ban đầu chưa có biến kết nối. Sau sửa tooling/cấu hình cục bộ, baseline Android unit/assemble/lint và Backend test/bootJar đều exit 0, mỗi module có một test gốc. Lỗi này được tách với lỗi do tích hợp.

| Kiểm tra | Kết quả mới | Phạm vi chứng minh |
|---|---|---|
| Android unit | 42 test, 0 fail | Mapping, detail state/cancellation, catalog và contract tích hợp |
| Backend JUnit | 17 test, 0 fail/error/skip | 4 test Demo được giữ/adapt, domain, context và 8 test SQL thật |
| HTTP thực tế | 210 assertion, 0 lỗi | Tài khoản/ownership, catalog, giỏ, đơn, COD, review, kho, voucher, giao và quản trị |
| Migration và seed | 5 migration, lặp không đổi dữ liệu kiểm tra | 8 demo không trùng, snapshot, FK và định nghĩa cũ được giữ |
| UI native | Đang hoàn tất vòng cuối | Không cộng test build APK vào test đã chạy |
| Nguồn và API | Hash nguồn khớp; 70 operations/68 schemas | Coverage, refs và 8 response thật đối chiếu OpenAPI |

SQL thật được kiểm bằng truy vấn dữ liệu và transaction, không dùng H2/mock để nhận là đã tích hợp. Hai sản phẩm được đối chiếu đúng ID, giỏ được thay đổi/mở lại, đơn và thanh toán được lưu, review hợp lệ làm đổi aggregate. Tọa độ và xác nhận tiền mặt trong kịch bản E2E là dữ liệu kiểm thử được khai báo, không phải GPS hoặc giao dịch tiền thật.

Kiểm tra trực quan đã phát hiện và sửa lỗi seed tiếng Việt, fetcher ảnh và thông báo cũ che thao tác sau điều hướng. Mọi lần test lỗi được giữ bằng chứng; không xóa hoặc skip test để tạo báo cáo đạt. Số liệu báo cáo cũ ngày 02/11 không được dùng thay kết quả này.

:::pagebreak

## Tồn đọng và điều kiện nghiệm thu tiếp

Ví điện tử/thẻ đã có interface, danh sách phương thức và luồng lỗi rõ, nhưng chưa có adapter nhà cung cấp/sandbox/credential. Webhook chưa được nhận là hoàn tất chỉ từ redirect hoặc payload bất kỳ. Các API hiện trả 503 khi chưa cấu hình; điều đó kiểm chứng cách xử lý lỗi, không chứng minh thanh toán ngoài đã hoạt động.

Tối ưu tuyến đường cần nhà cung cấp bản đồ. Vị trí shipper hiện là bản ghi chủ động gửi, có recordedAt và realtime=false; chưa phải thu GPS nền hay theo dõi liên tục. Các thao tác UI chưa chạy riêng được đánh dấu Chưa chạy trong ma trận, kể cả khi API đã đạt. Không kết luận nghiệm thu toàn bộ 59 chức năng từ một luồng mua sắm.

### Kịch bản trình bày khoảng tám phút

| Thời lượng | Nội dung trình bày |
|---|---|
| 1 phút | Vấn đề ban đầu, nền nhóm và cách giữ đóng góp |
| 2 phút | Trang chủ, tìm/lọc, mở đúng sản phẩm ở điện thoại và cửa sổ rộng |
| 2 phút | Đăng nhập, lưu giỏ, nhập địa chỉ, báo giá và tạo đơn COD chưa thanh toán |
| 1 phút | Lịch sử đơn, tiến trình giao, điều kiện đánh giá và aggregate thật |
| 1 phút | Quản lý kho/voucher/đơn và các kiểm tra concurrency/retry |
| 1 phút | Bằng chứng kiểm thử, phần chặn nhà cung cấp và bước tiếp theo |

Người trình bày mở đúng thư mục android, chạy Backend bằng script có nạp cấu hình cục bộ và dùng tài khoản tự tạo. Người có quyền cấp tài khoản quản lý riêng nếu cần demo role. Chọn nhà cung cấp thanh toán/bản đồ và cung cấp credential bằng biến môi trường trước khi nghiệm thu các kết nối ngoài. Không đưa bí mật vào báo cáo hoặc gói nguồn.

Tài liệu chi tiết đi kèm gồm API_REFERENCE.md, openapi.json, DATABASE_GUIDE.md, MA_TRAN_CHUC_NANG.csv, BAO_CAO_KIEM_THU.md và TON_DONG_VA_GIOI_HAN.md. Đây là căn cứ kiểm tra phạm vi thực tế và kế hoạch nghiệm thu tiếp.
