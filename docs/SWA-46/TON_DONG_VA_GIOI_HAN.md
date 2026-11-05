# Tồn đọng và giới hạn SmartMart Master

Bản nguồn có các luồng ứng dụng và API được nêu trong ma trận. Mức nghiệm thu được tách khỏi việc có code; chỉ các mục có bằng chứng mới được ghi Đã kiểm chứng. Không kết luận nghiệm thu toàn bộ hệ thống khi các điều kiện dưới đây còn thiếu.

## Các kết nối bên ngoài

| Hạng mục | Trạng thái | Đã làm | Điều kiện còn thiếu |
|---|---|---|---|
| Ví điện tử và thẻ | Bị chặn | Interface provider, phương thức không khả dụng, lỗi 503 rõ; không tạo đơn/trừ tồn khi thiếu provider; không giữ dữ liệu thẻ | Chọn nhà cung cấp, tài khoản sandbox và credential; viết adapter tạo session và xác minh webhook có chữ ký, amount/reference/idempotency; chạy nghiệm thu sandbox thực tế |
| Hoàn tiền online | Bị chặn | Tách yêu cầu hoàn và trạng thái; COD có xác nhận hoàn tiền mặt riêng | Adapter/provider xác nhận giao dịch hoàn thật và callback lặp; không nhận một nút hoặc redirect là chứng cứ tiền đã hoàn |
| Tối ưu tuyến đường | Bị chặn | API có quyền SHIPPER và trả MAP_PROVIDER_UNCONFIGURED | Nhà cung cấp bản đồ/geocoding/routing, cấu hình và dữ liệu hợp lệ; không có tuyến đường tự bịa |
| GPS realtime tự động | Bị chặn theo tiêu chí realtime | API lưu tọa độ do shipper đăng nhập chủ động báo, có recordedAt và realtime=false; chỉ chủ đơn đang giao được đọc vị trí gần đây | Thu GPS trên thiết bị/quyền phù hợp, cập nhật nền, kết nối trực tiếp và nghiệm thu vị trí/thời gian thực; dữ liệu test không phải GPS đã đo |

Các phần trả 503 được kiểm chứng về luồng lỗi, không được tính là thanh toán/bản đồ thật đã hoàn tất. Chưa chọn dịch vụ trả phí, tạo giao dịch tiền thật, thay firewall hoặc cài lại SQL Server.

## Phạm vi kiểm chứng giao diện

Các test native và ảnh thật được liệt kê trong BAO_CAO_KIEM_THU và ui-functional-verification.json. Các màn quản trị/kho/giao hàng có code và API/SQL đã được kiểm tra, nhưng những thao tác chưa chạy riêng bằng UI vẫn là Chưa chạy trong MA_TRAN_CHUC_NANG.csv. Kiểm tra bằng API không tự nâng các dòng này thành đã nghiệm thu UI.

Gửi review hợp lệ đã được chứng minh qua Backend/SQL thật; phần FE có test event dương tính, guard request cũ và giao diện danh sách/lọc/lỗi khi chưa đủ điều kiện mua. Luồng dương tính hoàn chỉnh Android → Backend → SQL để gửi review trên một đơn đã giao chưa được chạy trong bộ UI này.

Một đề xuất fixture UI định đọc token ADMIN trên thiết bị và đổi trạng thái đơn bằng API quản trị đã bị automatic approval review từ chối vì truy cập credential quản trị và tác động có quyền cao chưa được cho phép cụ thể cho cách kiểm thử đó. Đề xuất đã bị bỏ; phiên fixture riêng được thu hồi và đúng các file fixture mới trên thiết bị/cục bộ được gỡ. Không dùng agent/công cụ khác để thực hiện lại thao tác bị từ chối. Thay thế bằng UI người dùng thông thường, component/ViewModel và các bằng chứng Backend/SQL đã có. Đây là giới hạn kiểm thử, không có fixture hay credential quản trị trong ứng dụng/gói nguồn.

## Môi trường phát triển và dữ liệu

- Base URL debug mặc định dùng host emulator 10.0.2.2. Thiết bị thật cần cấu hình URL LAN/HTTPS phù hợp; không tự sửa firewall. Release chặn cleartext và chưa có quy trình ký/phát hành production trong phạm vi bàn giao này.
- Credential DB chỉ nằm trong .local/backend.env.ps1, ACL giới hạn, bị loại khỏi gói nguồn. Spring không tự đọc .env; script chạy nạp biến môi trường. Máy mới phải tự tạo cấu hình/login Master phù hợp.
- Schema nhóm dùng số lượng INT theo đơn vị bán; chưa có nghiệp vụ cân lẻ 0,5 kg. Không tự đổi toàn bộ schema hoặc tự gán kg cho các sản phẩm khác đơn vị.
- Seed có nhãn demo; ảnh là minh họa được phép dùng. Không khẳng định thương hiệu, hạn dùng, xuất xứ, doanh số hoặc chính sách giao/đổi trả khi chưa có nguồn cấu hình.
- Gợi ý sản phẩm dùng ProductRecommendations nếu có, nếu không thì catalog gần đây với source được ghi rõ; chưa được mô tả thành mô hình cá nhân hóa đã huấn luyện.
- Có index unique số điện thoại tùy chọn. Khi nâng cấp một DB cũ có phone trùng, migration dừng với lỗi rõ để đối soát, không tự xóa hoặc sửa dữ liệu thật.

## Bảo toàn nguồn và bằng chứng

Hai manifest sau kiểm tra phải khớp toàn bộ byte và Git state ban đầu. Trong quá trình mở IDE, Android Studio tự mở lại cửa sổ Demo cũ và tạo mới .idea/jpa.xml; cửa sổ đó đã được đóng, file phát sinh được lưu bằng chứng rồi gỡ đúng file không tồn tại ban đầu. Kiểm tra lại 841 file nhóm và 4303 file Demo đều khớp; không sửa/xóa bất kỳ file nguồn gốc nào.

Các lần test lỗi, lỗi seed UTF-8, ảnh và thông báo che thao tác được giữ trong audit cùng kết quả sau sửa. Bản báo cáo cũ và các giá trị mặc định 33/9/4 không phải bằng chứng mới. Báo cáo Word được xuất qua WPS Office Word.Application COM và raster PNG bằng Poppler trong runtime; render_docx.py không tìm thấy LibreOffice trên máy nên đã dùng đường render native được ghi log, không cài phần mềm hoặc nhận Word là đã kiểm chỉ từ XML.

## Hành động tiếp theo

Chọn nhà cung cấp/sandbox trước khi kết nối ví/thẻ/bản đồ; cấu hình secret cục bộ và chạy lại các kịch bản nhà cung cấp. Với demo quản lý, người có quyền SQL Windows cấp role cho tài khoản đã đăng ký bằng Set-DevelopmentRole.ps1 và đăng nhập lại; không đưa token quản trị vào file trên thiết bị. Chạy bổ sung các thao tác UI chưa có bằng chứng trong ma trận, đặc biệt review dương tính sau giao hàng và từng nghiệp vụ quản trị. Các bước này là nghiệm thu tiếp, không được gắn nhãn đã hoàn tất trong bản hiện tại.
