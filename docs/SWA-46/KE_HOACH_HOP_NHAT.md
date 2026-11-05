# Kế hoạch hợp nhất SmartMart Master

Ngày bắt đầu: 05/11/2026, múi giờ Asia/Saigon. Bản nền là toàn bộ tệp nguồn phù hợp của SmartMart đang có trên đĩa, bao gồm thay đổi chưa commit. Không dùng Demo thay thế bản nền. Không push, pull, đổi nhánh hoặc sửa nguồn.

## Bằng chứng và nguyên tắc

- Đã chụp SHA256 toàn bộ 841 tệp SmartMart và 4303 tệp Demo, kể cả metadata/cache, tại `docs/audit/source-*-before.json` trước khi hợp nhất. Cuối đợt so lại toàn bộ tệp và Git state.
- Bản nền sao chép 79 tệp; 762 tệp metadata, cache, build, cấu hình máy và log bị loại khỏi gói. Danh sách cụ thể ở `docs/audit/baseline-excluded.json`.
- `docs/NGUON_GOC_THAY_DOI.csv` khởi tạo trước khi sửa code; sẽ cập nhật từ so sánh nội dung và nguồn thực tế. Tệp nhóm giữ nguyên nếu không cần sửa; mỗi sửa đổi có lý do và kiểm thử.
- Không chạy lệnh trong tài liệu tham khảo. Phân biệt người được giao với tác giả code. Mọi bổ sung AI phải ghi rõ nguồn gốc.
- Schema `database` và `database_N` cần đối chiếu hash; chỉ dùng schema nhóm được chứng minh đang chạy, bổ sung migration không xóa dữ liệu.

## Thứ tự và phân loại ban đầu

| Phạm vi | Giữ từ nhóm | Hợp nhất hoặc bổ sung | Kiểm tra |
|---|---|---|---|
| Android | applicationId/package, Activity, theme, launcher, Gradle và test gốc | Tách phần gọi HTTP khỏi UI; tái sử dụng có chọn lọc DTO/domain/ViewModel/repository và test detail từ Demo; thêm native catalog/cart/account/order/payment/review/admin theo API | Baseline riêng; unit/lint/APK/connected UI và ảnh hẹp/rộng |
| Backend | entrypoint, health API, contract collection cũ, Kotlin/Spring/JDBC/SQL Server | Detail Demo qua service; API công khai an toàn; auth/quyền sở hữu; giỏ/đơn/checkout/COD/review; chức năng quản trị theo backlog | Baseline, unit/contract, SQL thật và E2E |
| Database | Schema PK/FK/20 bảng của nhóm và dữ liệu nguồn | Database phát triển riêng SmartMartMaster; migration bổ sung có phiên bản, seed demo có nhãn và chạy lặp | DB mới, nâng cấp, FK, transaction, tồn kho |
| Docs | Tài liệu nhóm và bản gốc lưu nguyên | Đọc đầy đủ DOCX/XLSX, đối chiếu mã chức năng, sơ đồ/code/API hiện tại, báo cáo kiểm thử mới và Word | Hash/từng sheet/bảng; render từng trang Word |
| Không áp dụng | Không đưa vào luồng SmartMart | Danh bạ/Content Provider, web thay Android, installer/cache/credential/log nhạy cảm, code demo không đúng nghiệp vụ | Danh mục loại trừ có lý do |

## Checklist thực hiện

- [x] Đọc prompt được người dùng cho phép thực hiện và quy tắc repo.
- [x] Quan sát cả hai ảnh tham chiếu.
- [x] Kiểm kê, hash toàn bộ hai nguồn và tạo bản nền có kiểm soát.
- [x] Đọc/đối chiếu toàn bộ tài liệu và backlog; xác nhận ma trận 59 chức năng, 70 đường dẫn tài liệu, 42 hash byte duy nhất.
- [x] Chạy baseline Android và Backend trong bản sao `.local/baseline`, không build trong nguồn. Android sau sửa wrapper: unit/assemble/lint exit 0. Backend sau cấp biến môi trường vào DB riêng: test/bootJar exit 0. Các lần thất bại ban đầu được giữ nguyên log.
- [ ] Tích hợp các tầng/API/SQL và luồng sản phẩm đến giỏ/đơn/thanh toán/đánh giá.
- [ ] Hoàn thiện các chức năng backlog còn thiếu, ghi trạng thái từng phần.
- [ ] Kiểm chứng SQL Server thật, test/build và ảnh Android hẹp/rộng.
- [ ] Tài liệu tiếng Việt và Word render/kiểm từng trang.
- [ ] Rà soát nguồn không đổi, provenance, tồn đọng và bàn giao trung thực.

## Điều kiện môi trường đã quan sát ban đầu

SQL Server MSSQLSERVER/TCP 1433 hoạt động; Windows Authentication qua sqlcmd thành công. Database SmartMart của nhóm có sẵn, chỉ đọc. Dự kiến tạo DB riêng sau khi kiểm tra trùng tên. SDK 37 cài sẵn; AVD Medium_Phone tồn tại, chưa có thiết bị kết nối ở đầu đợt. AGP/Gradle/JDK được giữ theo file thực tế, chỉ đổi khi có bằng chứng lỗi tương thích.

Database riêng `SmartMartMaster_20261105` đã được tạo; migration bổ sung và 8 sản phẩm demo có nhãn đã chạy. Login riêng chỉ được cấp quyền DML trên DB Master; credential lưu cục bộ tại `.local/backend.env.ps1`, ACL giới hạn và không nằm trong gói bàn giao.

Lỗi môi trường Gradle được xác định từ stacktrace và source JDK 25: Windows AF_UNIX trong PipeImpl kết nối lỗi `Invalid argument`. Script `Use-LocalBuildEnvironment.ps1` đặt `jdk.net.unixdomain.tmpdir` vào đường dẫn không tồn tại để JDK fallback TCP, chỉ trong tiến trình hiện tại. Không hạ phiên bản và không sửa JDK/firewall/registry. Thuộc tính được mô tả ở [Oracle Networking Properties](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/doc-files/net-properties.html); cơ chế fallback đã kiểm tra trong `src.zip` JDK đang cài và bằng Gradle help/build thực tế. Đây là giải pháp cục bộ của đợt này, không mặc định mọi máy cần cùng cách xử lý.

Baseline, kết quả cuối và điều kiện chưa chạy sẽ ghi riêng trong báo cáo; không lấy số test từ báo cáo ngày cũ.
