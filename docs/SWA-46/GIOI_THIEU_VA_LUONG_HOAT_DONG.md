# Giới thiệu SmartMart Master và luồng hoạt động

SmartMart Master là bản hợp nhất ứng dụng mua sắm Android của nhóm với phần chi tiết sản phẩm và tích hợp trong Demo của Lê Tuấn Thành. Bản nền được lấy từ nội dung nguồn nhóm đang có trên đĩa, sau đó ghép chọn lọc các lớp có thể tái sử dụng và bổ sung chức năng còn thiếu. Bảng `MA_TRAN_CHUC_NANG.csv` tách tình trạng có code với tình trạng đã kiểm chứng; chỉ có màn hình hoặc bảng SQL chưa đủ để coi một chức năng đã nghiệm thu.

Mục tiêu sử dụng là chọn sản phẩm đúng dữ liệu, lưu giỏ theo tài khoản, nhập địa chỉ, tạo đơn và ghi đúng trạng thái thanh toán. Phạm vi backlog còn gồm quản lý sản phẩm, kho, xử lý đơn, voucher, đánh giá, giao hàng và báo cáo. Các phần thiếu hoặc phụ thuộc dịch vụ ngoài được ghi riêng trong `TON_DONG_VA_GIOI_HAN.md`, không suy ra đã xong toàn bộ từ luồng mua sắm chính.

## Người dùng và quyền

| Vai trò trong SQL | Nhu cầu | Nơi kiểm tra quyền |
|---|---|---|
| CUSTOMER | Duyệt hàng, giỏ, địa chỉ, đơn của mình, đánh giá, yêu thích | Backend lấy tài khoản từ Bearer session; kiểm tra sở hữu tài nguyên |
| WAREHOUSE_MANAGER | Sản phẩm, danh mục và nghiệp vụ tồn kho | Backend kiểm role trên thao tác quản lý |
| ADMIN | Quản lý hệ thống, đơn, voucher và báo cáo theo endpoint | Backend kiểm role; API cũ có giá vốn chỉ dành ADMIN |
| SHIPPER | Các đơn giao được phân công và cập nhật giao hàng | Kiểm role và phân công; xem mức hỗ trợ thực tế trong ma trận |

Chọn một tab giao diện không tự cấp quyền. Client không được gửi userId cố định hoặc tự chọn role để trở thành quản trị. Đăng ký mới tạo CUSTOMER; tài khoản quản lý phải do người có quyền cấp ở môi trường phát triển phù hợp. Demo Firebase và JSONPlaceholder không được dùng thay xác thực của hệ thống này.

Email được dùng khi đăng ký; phone tùy chọn được chuẩn hóa và kiểm duy nhất nếu có. Đăng nhập giữ field tên email để tương thích nhưng chấp nhận email hoặc phone. Hồ sơ cho sửa họ tên, phone và email; email null/bỏ qua giữ email cũ, phone null/rỗng giữ phone cũ. Sổ địa chỉ là dữ liệu riêng của khách; không lấy địa chỉ từ tài khoản cố định. Không có thao tác tự cấp role từ ứng dụng.

## Công nghệ và trách nhiệm từng tầng

Android dùng Kotlin, Jetpack Compose và Material3. UI nhận state và phát event; ViewModel giữ StateFlow và gọi repository; Retrofit/OkHttp thực hiện HTTP; DTO được kiểm tra và chuyển sang model sản phẩm. Inter cùng giấy phép được giữ trong tài nguyên. Android Studio mở thư mục `android`.

Backend dùng Kotlin, Spring Boot và Gradle Kotlin DSL. Controller nhận HTTP và gọi các service; service thực hiện validation, kiểm quyền và transaction; truy vấn SQL dùng JdbcTemplate có tham số. Danh mục mới đi qua ProductQueryRepository. Một số service nghiệp vụ truy cập JdbcTemplate trực tiếp theo cách dùng JDBC của nền nhóm, không giả định đã có lớp JPA hoặc repository riêng cho mọi bảng.

SQL Server lưu giỏ, đơn, thanh toán, đánh giá và tồn kho. Schema20 bảng của nhóm được giữ làm nền; các thay đổi đi qua migration bổ sung. Giá dùng BigDecimal/DECIMAL. Số lượng hiện theo schema INT của nhóm; đơn vị hiển thị lấy từ Products.Unit, không tự gán kg cho mọi mặt hàng.

Phiên bản AGP, wrapper, SDK và toolchain lấy từ build files thực tế. Kết quả kiểm tra môi trường và lệnh chạy nằm ở `BAO_CAO_KIEM_THU.md` và `HUONG_DAN_CHAY_VA_DEMO.md`; không dùng hướng dẫn JDK17/SDK34 của prompt cũ để tự hạ cấu hình dự án.

## Luồng sản phẩm đến giỏ hàng

1. Trang chủ tải `GET /api/v1/products` với từ khóa, danh mục, giá, đánh giá, tồn, thương hiệu, sort và page. Metadata bộ lọc lấy từ `GET /api/catalog/filters`.
2. Chọn sản phẩm truyền đúng id tới chi tiết; `GET /api/products/{id}` trả model id/name tương thích phần Demo. UI có Loading, Success, Error và NotFound; HTTP404 được phân biệt với500.
3. Ảnh dùng URL từ API và có fallback khi lỗi riêng ảnh. Giá, đơn vị, tồn và điểm đánh giá lấy từ dữ liệu, không chép các số4.8/126 hoặc lời hứa giao hàng trong ảnh tham chiếu thành dữ kiện thật.
4. Người dùng chọn số lượng và thêm giỏ. Nếu chưa đăng nhập, ứng dụng chuyển tới tài khoản. Backend kiểm lại sản phẩm, tồn và quyền giỏ; chỉ sau khi lưu thành công client mới hiển thị thông báo đã thêm.
5. Màn giỏ tải lại giỏ từ API, thay đổi số lượng hoặc xóa sản phẩm rồi nhận tổng mới. Giỏ lưu ở SQL theo người dùng; không chỉ đổi bộ đếm trong bộ nhớ giao diện.

## Luồng đặt hàng và thanh toán

Người dùng chọn hoặc lưu địa chỉ của mình, kiểm tra giỏ và chọn phương thức thanh toán. Client gửi `POST /api/orders` với `Idempotency-Key`; backend kiểm quyền địa chỉ, đọc lại giá/tồn, áp voucher và tạo dữ liệu đơn trong transaction. Retry phải dùng cùng khóa cho cùng yêu cầu khi chưa biết kết quả, tránh tạo đơn hoặc trừ tồn hai lần.

COD là thanh toán khi nhận hàng: đơn vừa tạo còn chưa thanh toán. Phương thức ví/thẻ cần nhà cung cấp và cấu hình hợp lệ; khi thiếu, hệ thống phải trả lỗi rõ, không tạo giao dịch thật hoặc báo đã trả tiền. Redirect trở về ứng dụng không phải bằng chứng thành công; tích hợp online chỉ được nghiệm thu khi xác minh callback/webhook và xử lý lặp đúng. Xem trạng thái adapter cụ thể trong ma trận và báo cáo kiểm thử.

Lịch sử và chi tiết đơn được đọc lại từ API theo tài khoản. Thay đổi trạng thái và hoàn tiền phải theo quyền và trạng thái cho phép; không dùng một nút giao diện để khẳng định tiền đã thực sự hoàn từ nhà cung cấp.

## Luồng đánh giá và tương tác

Khách hàng xem và lọc đánh giá trong chi tiết. Khi gửi đánh giá, backend kiểm tra đơn thuộc tài khoản, sản phẩm có trong đơn và điều kiện mua hàng hợp lệ; rating phải trong1–5. Điểm trung bình và số lượng đánh giá được tính từ DB. Yêu thích lưu theo tài khoản và chỉ báo thành công sau khi API lưu được.

## Quản lý và giao hàng

ADMIN và WAREHOUSE_MANAGER được quản lý sản phẩm/danh mục và kho theo quyền backend. Sản phẩm hoặc danh mục được ngừng sử dụng bằng active để giữ FK/lịch sử. Nhập/điều chỉnh/kiểm kê có note và transaction; MutationKeys chống retry cộng tồn trùng khi gửi Idempotency-Key. lowStock so sánh tồn với reorderLevel trong SQL.

ADMIN xử lý đơn, phân công shipper, voucher, tài khoản và báo cáo. Xác nhận giao thành công vẫn để COD UNPAID; thao tác xác nhận thực tế đã thu đủ tiền sau DELIVERED mới chuyển payment SUCCESS và đơn PAID. Hoàn tiền COD có hai bước tạo yêu cầu và xác nhận tiền mặt đã hoàn. Báo cáo tách tiền đã thu và chưa thu, gắn khoảng thời gian; không cộng mọi đơn mới vào doanh thu.

SHIPPER chỉ thấy vận đơn được giao cho session, nhận hàng rồi cập nhật tiến trình hợp lệ. Retry phân công cùng shipper không làm lùi tiến trình; đổi người sau khi nhận/đang giao bị từ chối. Khách chỉ xem vận đơn/vị trí của đơn mình. Màn giao hàng hiển thị địa chỉ snapshot. Liên kết geo mở hộp thoại để người dùng nhập/xác nhận riêng địa chỉ trước khi chuyển sang ứng dụng bản đồ, không tự gửi toàn snapshot gồm họ tên/phone. Tọa độ thuần có thể được điền sẵn an toàn. Chưa có bản đồ nhúng hoặc geocode của hệ thống.

Tọa độ shipper có thể chủ động nhập/gửi vào ShipperLocations, backend trả recordedAt và realtime=false cho bản ghi gần đây của đơn SHIPPING. Đây chưa phải GPS nền/tracking realtime. Tối ưu tuyến trả503 vì chưa có provider; không tạo tuyến hoặc vị trí giả để báo hoàn tất. Phạm vi UI và bằng chứng thao tác cho từng chức năng xem ma trận, không suy từ danh sách API.

## Giao diện và dữ liệu trình diễn

Hai ảnh tham chiếu được dùng cho nhận diện SmartMart: xanh chính16A34A, nền xanh nhạtDCFCE7, card trắng, font Inter và khoảng cách đồng nhất. Màn hẹp xếp dọc, bộ lọc gọn; màn rộng dùng nhiều cột và vùng bộ lọc phù hợp. Đây là Compose native; không dùng ảnh nguyên trang hoặc WebView làm UI.

Seed Master được gắn IsDemo và SKU MASTER-DEMO, gồm táo và các sản phẩm phục vụ demo. Tên thương hiệu có chữ Demo và mô tả nói rõ dữ liệu thử nghiệm. Dữ liệu này không khẳng định xuất xứ, hạn dùng, doanh số hay chất lượng hàng thương mại. Ảnh ứng dụng đang chạy, kích thước thử và kết quả so sánh nằm trong báo cáo kiểm thử, không dùng ảnh báo cáo cũ làm bằng chứng mới.

## Các tài liệu cần đọc tiếp

`KIEN_TRUC_VA_LUONG_DU_LIEU.md` giải thích lớp và sequence. `API_REFERENCE.md`/OpenAPI mô tả contract. `DATABASE_GUIDE.md` giải thích schema/migration. `DOI_CHIEU_YEU_CAU_VA_PHAN_CONG.md` cùng `PHAN_VIEC_LE_TUAN_THANH.md` tách phân công và nguồn gốc code. `BAO_CAO_KIEM_THU.md` và `TON_DONG_VA_GIOI_HAN.md` xác định phần đã kiểm chứng và phần chưa nghiệm thu.
