# Hướng dẫn chạy và trình diễn SmartMart Master

Mọi lệnh dưới đây chạy trong PowerShell 7 tại thư mục SmartMart-Master. Không sửa hoặc build trong SmartMart/Demo. Xem BAO_CAO_KIEM_THU để biết môi trường và những bước thực sự đã chạy.

## Khởi động dịch vụ và thiết bị

1. Chạy `tools/Test-Environment.ps1` để xem JDK, SDK, wrapper, emulator và SQL Server. Công cụ chỉ kiểm tra, không cài phần mềm hay đổi cấu hình toàn máy.
2. Chạy `tools/initialize-sql-master.ps1` để tạo/nâng cấp DB Master bằng Windows Authentication. Script dừng nếu credential của login đã tồn tại nhưng bị mất; không đổi mật khẩu để vượt lỗi. Không chạy riêng cả database và database_N.
3. Chạy `tools/start-backend.ps1 -Build` ở cửa sổ riêng. Kiểm tra `http://localhost:8080/api/health` và `/api/v1/products`. Cổng khác dùng tham số `-Port`; cần đồng bộ base URL Android. Không dừng process lạ để lấy cổng.
4. Android Studio mở chính xác `SmartMart-Master\android`, dùng Gradle wrapper đi kèm, SDK 37 và JDK daemon 25. Dùng AVD có sẵn hoặc thiết bị phù hợp; mạng emulator gọi host qua 10.0.2.2. Debug có quyền mạng cục bộ SDK 37; release cần HTTPS.
5. Chạy app, tạo tài khoản có email và số điện thoại riêng hoặc đăng nhập tài khoản do chính bạn vừa tạo. Tài khoản mới là CUSTOMER. Token chỉ được giữ trong tiến trình ứng dụng; sau khi ứng dụng bị kết thúc có thể cần đăng nhập lại, giỏ vẫn nằm trong SQL.

Bản debug có thể build bằng `tools/Build-Android.ps1`. Với emulator đang kết nối, thêm `-ConnectedTests` để thực sự chạy instrumentation; build APK kiểm thử riêng không đồng nghĩa đã chạy UI test. Nếu Android Studio gặp lỗi AF_UNIX như CLI ban đầu, mở IDE từ phiên PowerShell đã nạp `tools/Use-LocalBuildEnvironment.ps1` hoặc đặt cùng VM option theo cấu hình cục bộ, không hạ JDK/SDK tùy ý.

## Kịch bản trình bày khoảng tám phút

| Thời lượng | Thao tác | Giá trị cần chỉ rõ |
|---|---|---|
| 1 phút | Giới thiệu cấu trúc và nguồn | Nhóm là nền; detail Demo ghép chọn lọc; AI bổ sung có ghi nguồn |
| 2 phút | Trang chủ, tìm/lọc/sort/page, mở hai sản phẩm | ID/name/giá/đơn vị/tồn đúng API; UI hẹp/rộng thích ứng |
| 1 phút | Đăng nhập, chọn lượng, thêm/sửa/xóa giỏ, mở lại | Giỏ lưu ở SQL theo tài khoản; chỉ báo thành công sau khi lưu |
| 1 phút | Chọn địa chỉ, xem tổng, tạo đơn COD | Kiểm lại giá/tồn; COD chưa trả; retry dùng cùng khóa không tạo trùng |
| 1 phút | Lịch sử/chi tiết/trạng thái đơn; đánh giá | Chỉ sản phẩm đã giao từ đơn của mình mới gửi đánh giá được |
| 1 phút | Dùng tài khoản quản lý được cấp để minh họa kho/đơn/voucher/báo cáo | Backend kiểm quyền; tồn/voucher có transaction và chống lặp |
| 1 phút | Mở báo cáo kiểm thử và tồn đọng | Phân biệt đã kiểm chứng với kết nối ví/thẻ/bản đồ chưa có nhà cung cấp |

Để gửi đánh giá từ UI, cần một đơn thuộc chính tài khoản hiện tại đã DELIVERED. Tài khoản ADMIN xác nhận/chuyển trạng thái theo thứ tự; SHIPPER chỉ xem và cập nhật vận đơn được giao. Giao hàng không tự thu COD; thao tác xác nhận thu tiền là riêng và phải phản ánh nghiệp vụ thực. Không thực hiện giao dịch tiền thật trong buổi demo.

## Lệnh kiểm chứng có thể chạy lại

`tools/e2e_master.py` gọi HTTP thật và tạo dữ liệu E2E có nhãn trong database Master. `tools/e2e_management.py` dùng context của bước trước, tạo tài khoản kiểm thử và cấp role bằng Windows SQL login chỉ trong DB Master, sau đó kiểm luồng quản trị/giao/đánh giá. Công cụ có guard tên DB, không thao tác DB nhóm. Đây là công cụ nghiệm thu phát triển, không triển khai lên production.

```powershell
. .\tools\Use-LocalBuildEnvironment.ps1
. .\.local\backend.env.ps1
Push-Location .\backend
.\gradlew.bat test bootJar
Pop-Location
.\tools\Build-Android.ps1 -ConnectedTests
```

Các script Python bàn giao dùng Python có các thư viện tương ứng. Trong đợt này dùng runtime Python do Codex cung cấp; đường dẫn chính xác và lệnh đã chạy ghi ở BAO_CAO_KIEM_THU. Không yêu cầu cài gói toàn máy để tự tạo kết quả báo cáo.

## Xử lý lỗi theo triệu chứng

- Health lỗi: xem process/cổng, rồi kiểm SQL instance và biến SMARTMART_DB_*. Không tự đổi mật khẩu, chạy installer, mở firewall hoặc chuyển sang fixture.
- Android không gọi được API nhưng host gọi được: kiểm base URL, trạng thái Backend, mạng thiết bị và quyền mạng cục bộ debug. localhost trong emulator là emulator.
- HTTP 401: đăng nhập lại. HTTP 403: tài khoản thiếu quyền hoặc chưa đủ điều kiện đánh giá. HTTP 404: dữ liệu không tồn tại/không thuộc tài khoản. HTTP 409: kiểm tồn, trạng thái, trùng dữ liệu hoặc khóa retry. HTTP 503 ở ví/thẻ/bản đồ là cấu hình ngoài chưa sẵn sàng.
- Không có sản phẩm sau khi lọc: xóa lọc hoặc đổi tiêu chí; không thay bằng số liệu giả. Ảnh lỗi có placeholder riêng, dữ liệu sản phẩm vẫn phải hiển thị.
- Đơn đã dùng địa chỉ: hệ thống giữ địa chỉ/lịch sử để đối soát; không xóa trực tiếp FK.
- Build thất bại: giữ log và XML lỗi, phân biệt baseline với lỗi mới; không xóa test để làm báo cáo xanh.

## Tài nguyên và trạng thái

Ảnh chụp mới ở `docs/screenshots`; các ảnh tham chiếu gốc chỉ dùng để đối chiếu thiết kế. Báo cáo mới ở `BAO_CAO_KIEM_THU.md`; ma trận toàn bộ chức năng ở `MA_TRAN_CHUC_NANG.csv`. Chỉ các mục có bằng chứng mới được ghi Đã kiểm chứng. Kết nối ngoài và các phần chưa đủ điều kiện nghiệm thu ở `TON_DONG_VA_GIOI_HAN.md`.

Nếu cần trình diễn quản lý, đăng ký tài khoản bằng UI trước rồi từ PowerShell có quyền SQL Windows chạy `tools/Set-DevelopmentRole.ps1 -Email email-da-dang-ky -Role ADMIN`. WAREHOUSE_MANAGER dùng cho catalog/kho; SHIPPER dùng cho vận đơn được phân công. Script chỉ truy DB Master và không hỏi/in mật khẩu. Đăng nhập lại sau khi cấp role; không chép credential hoặc token quản trị vào file trong thiết bị để chạy demo.
