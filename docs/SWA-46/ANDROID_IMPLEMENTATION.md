# Hợp nhất và kiểm chứng Android SmartMart Master

## Phần nền được giữ

Giữ package và applicationId `com.example.smartmart`, lớp `MainActivity`, model `Product` cũ, cấu trúc `onCreate → setContent → SmartMartTheme → SmartMartScreen`, launcher và các test gốc. Không sao chép nguyên MainActivity của Demo. Khối gọi HttpURLConnection trực tiếp trong Composable cũ được thay bằng lời gọi StoreRoot để thực hiện yêu cầu tách UI khỏi mạng; API collection cũ vẫn được giữ ở Backend với quyền ADMIN do có giá vốn.

Giữ AGP 9.5.0, Gradle 9.6.0, SDK 37 và daemon JDK 25 theo bản nhóm. Gradle wrapper JAR bị thiếu trong nhóm được khôi phục từ bản Demo đã đối chiếu, SHA256 `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`. Không hạ SDK/JDK để né lỗi môi trường. Script của Master đặt thuộc tính JDK cục bộ cho tiến trình build nhằm khắc phục lỗi AF_UNIX của máy này.

## Tái sử dụng có chọn lọc từ Demo

Tái sử dụng domain Product/StockStatus, DTO và mapper kiểm tra id/name/price/tồn, ProductRepositoryImpl, ProductDetailViewModel có cancellation, trạng thái Loading/Success/Error/NotFound và các test tương ứng. Giữ các test catalog bridge cũ và UI detail độc lập làm hồi quy. Domain/DTO được mở rộng để nhận danh mục, thương hiệu, ảnh, thông số và số liệu đánh giá thật. Inter và giấy phép OFL được giữ trong tài nguyên; không suy diễn tác giả backend từ tên thư mục Demo.

UI detail gốc vẫn được giữ để test hồi quy; màn Master dùng DetailScreen nhận state từ ViewModel đã tái sử dụng và phát StoreEvent. KDoc chỉ rõ phần FE/Integration được giao Lê Tuấn Thành; các màn và nghiệp vụ mới ghi có hỗ trợ AI, không gán thành code lịch sử của thành viên.

## Luồng native đã bổ sung

StoreRoot dùng collectAsStateWithLifecycle. StoreViewModel giữ StateFlow, SavedStateHandle cho điều hướng/id/bộ lọc và khóa retry. StoreRepository là contract chung; ApiStoreRepository dùng Retrofit/OkHttp singleton trong StoreGraph. Không gọi API hoặc SQL trong Composable.

- Trang chủ: banner, ảnh tài nguyên có giấy phép từ Backend, lưới sản phẩm, metadata danh mục/thương hiệu, phối hợp tìm kiếm/giá/đánh giá/tồn/sort/page. Điện thoại dùng sheet bộ lọc; cửa sổ rộng dùng sidebar.
- Chi tiết: đúng id, ảnh lớn và thumbnail khi API có nhiều ảnh, tồn và đơn vị thật, số lượng, giỏ, mô tả/thông số/đánh giá và sản phẩm liên quan. Không hardcode 4.8/126, xuất xứ hoặc lời hứa giao hàng từ ảnh tham chiếu.
- Tài khoản: đăng ký, đăng nhập bằng email hoặc điện thoại, đăng xuất, sửa họ tên/email/điện thoại, thêm/sửa/xóa địa chỉ theo quyền sở hữu. Password không lưu trong saved state; token chỉ sống trong tiến trình ứng dụng.
- Giỏ và đặt hàng: tăng/giảm/xóa, tổng từ API, tạo địa chỉ nhận hàng, báo giá có giảm giá/phí/tổng trước xác nhận, COD. Khóa đặt hàng được giữ khi lỗi mạng để retry không tạo đơn trùng. Kết quả đã ghi thành công không phụ thuộc một GET giỏ tiếp theo.
- Đơn: lịch sử/chi tiết/snapshot địa chỉ/tiến trình và trạng thái giao hàng; chỉ khách hàng có đơn PENDING mới được yêu cầu hủy. Tạo COD giữ UNPAID; giao hàng và đối soát tiền là hai thao tác riêng.
- Đánh giá và yêu thích: lưu qua API; Backend kiểm đơn đã giao thuộc tài khoản. Bộ lọc và tổng hợp đánh giá lấy từ SQL.
- Quản trị: sản phẩm/danh mục và ngừng kinh doanh; nhập/điều chỉnh/kiểm kê/cảnh báo kho; đơn và phân công shipper; voucher thêm/sửa/tắt; người dùng tìm kiếm/vô hiệu hóa; báo cáo theo ngày và hiệu suất giao. Quyền quản lý kho chỉ tải phần được cho phép. Retry nhập/điều chỉnh tồn giữ cùng khóa idempotency.
- Shipper: tải đúng vận đơn được phân công, chi tiết hàng/địa chỉ, nhận đơn và cập nhật giao; báo tọa độ do người dùng nhập và thời điểm máy chủ ghi. Khách chỉ được xem bản ghi gần nhất của shipper thuộc đơn đang giao của mình. Không gọi đây là GPS tự động hoặc theo dõi thời gian thực.

Map intent chỉ gửi tọa độ hoặc địa chỉ do người dùng xem/nhập trong hộp thoại; không gửi nguyên snapshot có tên và điện thoại sang ứng dụng bản đồ. Bản đồ ngoài cần ứng dụng phù hợp trên thiết bị. Tối ưu tuyến đường chưa có nhà cung cấp. Ví/thẻ hiện hiển thị chưa sẵn sàng và Backend trả lỗi cấu hình, không tạo giao dịch thật.

## Cấu hình và hình ảnh

Debug dùng `http://10.0.2.2:8080/`; có thể đổi bằng Gradle property `smartmart.baseUrl`. Bản release cần URL HTTPS phù hợp. INTERNET có trong manifest; HTTP phát triển và ACCESS_LOCAL_NETWORK API 37 chỉ có ở manifest debug. Không đưa local.properties hoặc credential vào gói nguồn.

Coil dùng một ImageLoader có network fetcher đăng ký rõ ràng ở MainActivity; client ảnh không gắn Bearer token. Lỗi ảnh giữ placeholder riêng, không làm mất dữ liệu sản phẩm. Cách cấu hình dựa [tài liệu Coil](https://coil-kt.github.io/coil/network/) và được kiểm lại bằng ảnh ứng dụng thật; không đổi phiên bản thư viện.

## Kiểm chứng

Kết quả cuối phải đọc cùng `BAO_CAO_KIEM_THU.md` và XML mới. Có 42 unit test đã chạy không lỗi ở lượt kiểm tra sau khi bổ sung chín test contract Master. Các test mới kiểm tra lỗi ghi giỏ không báo thành công, khóa retry checkout/kho, báo giá cũ không ghi đè báo giá mới, giữ bộ lọc/id khi tạo lại ViewModel, email profile, DTO ghi địa chỉ không gửi id response và kết quả giỏ đã commit không phụ thuộc GET tiếp theo.

UI test mới `LiveStoreFlowTest` kết nối Backend/SQL thật, đăng nhập, chọn đúng sản phẩm, tăng số lượng, lưu giỏ, nhập địa chỉ qua UI, xem báo giá và đặt COD rồi kiểm dữ liệu qua API. Các UI test detail cũ được giữ. Lượt đầu test mới thất bại vì snackbar đăng nhập che thao tác chạm; điều kiện chờ và kiểm tra hiển thị/giá trị số lượng được bổ sung, không bỏ test hoặc bỏ assertion. Trạng thái cuối lượt chạy connected và ảnh hẹp/rộng nằm trong báo cáo kiểm thử sau khi hoàn tất.

Lint từng báo lỗi tại local.properties do đường dẫn SDK chưa escape. Đã sửa đúng file cấu hình cục bộ; cần chạy lại phân tích lint để loại kết quả cache cũ. Không thêm baseline hoặc suppress lỗi để tạo báo cáo xanh.
