# SmartMart Master

Bản hợp nhất Android Kotlin Jetpack Compose, Backend Kotlin Spring Boot và Microsoft SQL Server. Bản nền được sao chép từ nguồn nhóm trên đĩa; phần chi tiết sản phẩm được tái sử dụng có chọn lọc từ Demo; các nghiệp vụ còn thiếu được bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI. Không gán phần bổ sung thành code lịch sử của thành viên.

Đường dẫn làm việc: `D:\TuanThanh\Workspace\Projects\SmartMart-Master`. Mở **thư mục android** trong Android Studio. Hai nguồn `SmartMart` và `Demo` được giữ để đối chiếu; không chạy công cụ bàn giao trong nguồn.

## Chạy nhanh trên máy hiện tại

Dùng PowerShell 7 tại thư mục SmartMart-Master:

```powershell
.\tools\Test-Environment.ps1
.\tools\initialize-sql-master.ps1
.\tools\start-backend.ps1 -Build
```

Initializer tạo hoặc nâng cấp database phát triển riêng `SmartMartMaster_20261105`, không ghi vào database `SmartMart` của nhóm. Lần đầu cần Windows login có quyền tạo DB/login; máy hiện tại đã thực hiện bước này. Credential riêng được lưu tại `.local/backend.env.ps1` với ACL giới hạn, không đóng gói hoặc commit. Spring không tự đọc `.env`; script khởi động nạp biến môi trường. `backend/.env.example` chỉ là danh sách cấu hình mẫu.

Trong Android Studio, mở `D:\TuanThanh\Workspace\Projects\SmartMart-Master\android`, chọn SDK đã cài và Gradle JDK 25, đồng bộ rồi chạy cấu hình app trên emulator API 37. Backend cần đang chạy ở cổng 8080. Debug mặc định dùng `http://10.0.2.2:8080/`; cấp quyền mạng cục bộ khi Android SDK 37 yêu cầu. Tạo tài khoản CUSTOMER ngay trong ứng dụng để thử luồng giỏ/đơn; không có userId cố định hay tài khoản quản trị công khai.

Build Android từ dòng lệnh:

```powershell
.\tools\Build-Android.ps1
.\tools\Build-Android.ps1 -ConnectedTests
```

Nếu chạy trên máy thật, cấu hình URL LAN/HTTPS và kết nối phù hợp, ví dụ `-Psmartmart.baseUrl=https://dia-chi-backend-cua-ban/`. Bản release chặn HTTP không mã hóa. Không dùng địa chỉ IP hoặc đường dẫn riêng của tác giả trong mã ứng dụng.

## Phiên bản được giữ

Android: AGP 9.5.0, Gradle 9.6.0, SDK 37, daemon JDK 25. Backend: Spring Boot 4.1.2, Kotlin 2.3.21, Gradle 9.8.2, Java toolchain 21 và daemon JDK 25. Wrapper JAR Android bị thiếu trong nền đã được khôi phục từ bản Demo phù hợp. Không hạ SDK/JDK theo prompt cũ. Lỗi Windows AF_UNIX của Gradle được xử lý cục bộ trong `Use-LocalBuildEnvironment.ps1`, không sửa JDK, registry hoặc firewall.

## Cấu trúc

- `android`: ứng dụng Compose native; UI → ViewModel → Repository → Retrofit/OkHttp.
- `backend`: REST controller, service/transaction, JDBC có tham số và SQL Server.
- `SmartMartDB`: thư mục dữ liệu chung gồm schema SQL Server của nhóm, migrations, seeds và tài liệu thiết kế.
- `tools`: kiểm tra môi trường, khởi tạo DB, build/chạy, E2E và thu thập bằng chứng.
- `docs`: tài liệu tiếng Việt, ma trận chức năng, API/OpenAPI, nguồn gốc, kiểm thử và báo cáo Word.
- `.local`: cấu hình riêng, credential và bản sao baseline; không thuộc gói nguồn bàn giao.

## Luồng và giới hạn cần biết

Catalog công khai dùng `/api/v1/products` có phân trang; detail giữ `/api/products/{id}` với `id/name`. API collection cũ `/api/products` giữ List và `productId/productName`, nhưng yêu cầu ADMIN vì chứa giá vốn. Giỏ, địa chỉ, đơn và đánh giá được kiểm tra session/quyền sở hữu. Giá và tồn được kiểm lại khi ghi. COD vừa tạo là UNPAID; giao thành công cũng chưa tự đánh dấu đã thu tiền.

Ví điện tử/thẻ và tối ưu tuyến đường trả lỗi chưa cấu hình rõ ràng; chưa có giao dịch online hoặc nghiệm thu nhà cung cấp. Vị trí shipper là dữ liệu tài khoản shipper gửi có thời điểm ghi, không tự nhận là GPS realtime. Seed có nhãn demo và ảnh minh họa có giấy phép; không dùng số đánh giá/doanh số từ ảnh tham chiếu làm dữ liệu thật.

Kết quả mới, số test và điều kiện chưa nghiệm thu xem `docs/BAO_CAO_KIEM_THU.md`, `docs/MA_TRAN_CHUC_NANG.csv` và `docs/TON_DONG_VA_GIOI_HAN.md`. Không dùng số 33/9/4 trong báo cáo cũ thay kết quả đợt này.

## Tài liệu bàn giao

Bắt đầu với `docs/HUONG_DAN_CHAY_VA_DEMO.md`; xem `API_REFERENCE.md` và `openapi.json` cho hợp đồng API, `DATABASE_GUIDE.md` cho dữ liệu, `DOI_CHIEU_YEU_CAU_VA_PHAN_CONG.md` và `PHAN_VIEC_LE_TUAN_THANH.md` cho phân công, `NGUON_GOC_THAY_DOI.csv` cho từng file. Báo cáo trình bày là `docs/BAO_CAO_SMARTMART_MASTER.docx` và bản Markdown tương ứng.

Trong môi trường phát triển, người có quyền SQL Windows có thể cấp role cho tài khoản đã đăng ký bằng `tools/Set-DevelopmentRole.ps1 -Email email-cua-ban -Role ADMIN` (hoặc WAREHOUSE_MANAGER/SHIPPER). Script chỉ làm việc trong DB Master, dùng SQL có tham số, không đổi mật khẩu và buộc đăng nhập lại. Ứng dụng không tự nhận role từ client. Không đưa token quản trị vào thiết bị/emulator để làm fixture.
