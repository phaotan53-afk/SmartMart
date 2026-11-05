# Phần việc Lê Tuấn Thành và nguồn gốc triển khai

Tài liệu này chỉ rõ phần được giao, phần code tái sử dụng và phần được bổ sung trong đợt hợp nhất. Phân công trong DOCX/CSV/XLSX không tự chứng minh tác giả Git của từng file. Không gán toàn bộ Android, Backend hoặc schema của nhóm cho Lê Tuấn Thành; phần AI bổ sung được ghi riêng, không tạo lịch sử đóng góp giả.

## Phân công theo từng nguồn

| Chức năng | Mã theo nguồn | Người được giao | Bằng chứng |
|---|---|---|---|
| Chi tiết sản phẩm FE và tích hợp | PRODUCT_DETAIL; SWA-36 trong Sprint, SWA-24 trong CSV | Sprint giao Lê Tuấn Thành FE/Integration; Nhân BE. CSV giao Phao cho chức năng chi tiết | Ke_Hoach_Sprint_SmartMart.docx; Prompt_Master_SWA_36_SmartMart.md; CSV Jira |
| Danh sách/tìm kiếm liên quan tích hợp | PRODUCT_LIST SWA-35/SWA-23; PRODUCT_SEARCH SWA-17/SWA-8 | Sprint Phao BE list, Nhân BE search, Phong FE; Thành hỗ trợ tích hợp | Kế hoạch Sprint và tài liệu GitHub Sprint1 |
| COD | PAYMENT_COD; SWA-14 CSV, STT25 XLSX | Lê Tuấn Thành | CSV Jira; Product Backlog dòng26 |
| Ví điện tử | PAYMENT_EWALLET; SWA-15 CSV, STT26 XLSX | Lê Tuấn Thành | CSV Jira; Product Backlog dòng27 |
| Thẻ | PAYMENT_CARD; SWA-16 CSV, STT27 XLSX | Lê Tuấn Thành | CSV Jira; Product Backlog dòng28 |
| Gửi đánh giá | REVIEW_CREATE; SWA-21 CSV, STT13 XLSX | Lê Tuấn Thành | CSV Jira; Product Backlog dòng14 |
| Xem/lọc đánh giá | REVIEW_LIST; SWA-22 CSV, STT14 XLSX | Lê Tuấn Thành | CSV Jira; Product Backlog dòng15 |

STT Excel là thứ tự, không phải Jira. CSV dùng SWA-36 cho danh sách đơn, khác SWA-36 chi tiết trong Sprint. Người được ghi Nguyễn Thành Nhân ở một số dòng XLSX và Nguyen Thanh Nhan/Nguyễn Thanh Nhân ở nguồn khác được giữ đúng chữ theo từng nguồn trong ma trận; không dùng khác biệt chính tả làm bằng chứng tác giả.

## Phần nền nhóm được giữ

Nguồn nhóm tại SmartMart là nền của Master, gồm android, backend, database/database_N và docs, cùng thay đổi trên đĩa nếu có. ApplicationId/package, wrapper/build theo phiên bản thực tế, HealthController, application entry, schema20 bảng và những tài liệu/quy tắc liên quan được giữ hoặc chỉ thay điểm cần thiết được ghi trong NGUON_GOC_THAY_DOI.csv.

MainActivity và ProductController là file hợp nhất, không lấy bản Demo cùng tên đè nguyên file. MainActivity giữ vai trò Activity/theme và gắn host Compose theo kiến trúc mới. ProductController giữ query/dạng collection gốc của nhóm; bổ sung detail thông qua CatalogService và giới hạn truy cập collection có CostPrice. Việc thay điểm tích hợp có ảnh hưởng được giải thích và cần kiểm thử hồi quy, không mô tả là giữ nguyên mọi hành vi.

Schema nền không đổi tên PK/FK/tables để khớp Demo. Bổ sung cột/bảng/index nằm trong migration Master. Cả database_N được giữ để truy vết nhưng không chạy thêm bộ schema trùng. Hash nguồn trước/sau và bảng nguồn gốc do quy trình hợp nhất lưu trong docs/audit và NGUON_GOC_THAY_DOI.csv.

## Tái sử dụng phần chi tiết từ Demo

Các ứng viên chính đến từ `Demo/SmartMart/Code/SmartMart-main`, được đối chiếu với bản `SmartMart_SWA36_Source/SmartMart-main` và các project root còn lại. Tài liệu Demo mô tả phần FE/Integration của Thành; việc giữ class không có nghĩa toàn bộ code trong Demo hoặc Backend liên quan đều là do Thành viết.

| Chức năng và file/class | Nội dung tái sử dụng hoặc ghép | Điểm tích hợp trong Master | Kiểm thử liên quan |
|---|---|---|---|
| DTO và domain: ProductResponse, domain.model.Product, StockStatus | Nullable DTO, BigDecimal, đơn vị tùy chọn và trạng thái tồn | Bổ sung dữ liệu catalog/detail có nguồn; không tự thêm kg hay rating | ProductRepositoryTest; mapping mới theo báo cáo test |
| data.repository.ProductRepositoryImpl và ProductResponse.toDomain | Kiểm ID/tên/giá, HTTP404, dữ liệu sai và cancellation | Cấp qua StoreGraph, tiếp tục dùng ProductApiService cho detail | ProductRepositoryTest |
| domain.repository.ProductRepository và ProductServiceException | Contract detail độc lập Retrofit/UI | ProductDetailViewModel phụ thuộc interface; có thể inject fake trong test | ProductDetailViewModelTest |
| ProductDetailViewModel và ProductDetailUiState | Loading/Success/Error/NotFound; retry; hủy request cũ | StoreRoot theo dõi productId, collect theo lifecycle, UI mới nhận state | ProductDetailViewModelTest; Compose detail tests |
| ProductDetailRoute và ProductDetailScreen | Entry point/UI/test phần Demo được giữ và ghép phù hợp | Luồng Master dùng StoreApp/DetailScreen với state từ ViewModel được giữ | ProductDetailScreenTest; test UI Master khi đã chạy |
| Theme, font Inter, chuỗi/icon/placeholder | Nhận diện theo tham chiếu cùng giấy phép | Dùng chung hệ thống Compose mới | Lint; ảnh kiểm tra màn hẹp/rộng; ASSET_LICENSES |
| Ý tưởng query detail Backend | ID có tham số, ảnh ưu tiên ổn định, tồn/active và unit | Catalog.kt ghép vào ProductQueryRepository/CatalogService; controller nhóm giữ phần gốc | Test contract và SQL thật theo báo cáo; không nhận toàn bộ BE là phần FE của Thành |

CatalogViewModel/cầu nối danh sách cũ và test tương ứng có thể được giữ để bảo toàn nguồn/test tốt; luồng catalog của Master dùng StoreViewModel/StoreRepository. Tài liệu không đếm một lớp được lưu mà không được host gọi là một luồng mới đã hoạt động.

## Bổ sung trong đợt hợp nhất có hỗ trợ AI

Các phần dưới đây được tạo để hoàn thiện yêu cầu của Lê Tuấn Thành trong đợt này. Chúng không được ghi thành code lịch sử của cá nhân hoặc thành viên nhóm.

| Nhóm file/class | Vai trò và thay đổi | Nguồn gốc |
|---|---|---|
| master/StoreApi, StoreRepository, ApiStoreRepository, StoreGraph, StoreModels | Contract mua sắm, Retrofit dùng chung, bearer session, DTO/mapping, lỗi thân thiện | Bổ sung mới có hỗ trợ AI; tái dùng mapping detail phù hợp |
| master/StoreViewModel, StoreEvent, StoreApp và các màn Master | State/event; productId/filter/page qua SavedStateHandle; điều hướng list/detail/cart/order, tài khoản, đánh giá và quản lý | Bổ sung mới có hỗ trợ AI; nối state chi tiết từ Demo |
| Backend Auth.kt, ApiErrors.kt, Catalog.kt, Admin.kt, Shipping.kt và các service nghiệp vụ mới | Xác thực/role, DTO công khai, lỗi tập trung, catalog và thao tác ghi qua SQL | Bổ sung đợt hợp nhất; giữ contract nhóm tại phần liên quan; không gán tác giả BE cho Thành |
| Commerce.kt: CommerceService, OnlinePaymentProvider | Giỏ/địa chỉ/checkout/đơn/review/yêu thích; COD chưa thanh toán; adapter online chưa cấu hình trả lỗi rõ | Bổ sung mới theo phạm vi hiện tại; kết nối nhà cung cấp online chưa được coi hoàn tất |
| database/migrations và database/seeds | Mở rộng schema an toàn, session/idempotency/specifications/refund; seed có nhãn demo | Bổ sung mới; schema20 bảng nhóm là nền |
| tools và docs mới | Môi trường, build/SQL/scripts kiểm thử, ma trận, báo cáo và hướng dẫn | Bổ sung mới; số liệu chỉ lấy từ bằng chứng hiện tại |

Tình trạng cụ thể từng chức năng và lớp quản lý/giao hàng bổ sung được liệt kê trong MA_TRAN_CHUC_NANG.csv, với đường dẫn file/class/function. NGUON_GOC_THAY_DOI.csv là bảng truy file chi tiết và hash; không dùng bảng tóm tắt trên thay provenance.

## Điểm tích hợp bổ sung theo backlog đầy đủ

AuthService bổ sung email/phone duy nhất, đăng nhập bằng identifier, cập nhật email/phone và kiểm session từ SQL. CommerceService giữ giỏ/địa chỉ/quote/checkout/đơn/review/favorite theo quyền sở hữu. AdminService tách sản phẩm/danh mục/kho, order states, xác nhận COD, hoàn tiền mặt, voucher, user status và báo cáo. ShippingService tách phân công/vận đơn/tiến trình/vị trí được báo và recommendations có nhãn nguồn. Đây là phần bổ sung đợt hợp nhất có hỗ trợ AI, không phải bằng chứng code lịch sử Backend của Thành.

Android tương ứng dùng StoreRepository/StoreViewModel và các màn CatalogScreen, DetailScreen, CustomerScreens, ManagementScreen, ShippingScreens. ProductDetailViewModel/domain/repository từ Demo tiếp tục phục vụ chi tiết; host và nghiệp vụ rộng hơn được ghi nguồn gốc mới. Chức năng chỉ được gọi đã kiểm chứng khi có bằng chứng API/SQL và UI theo ma trận; không gán mọi case backend cho kết quả Android.

Ví/thẻ chưa có adapter/provider sandbox thật; tracking realtime chưa có GPS nền/push và routes chưa có provider. Được giao phần này trong tài liệu không đồng nghĩa được phép báo đã hoàn thành kết nối ngoài. Nghiệm thu luồng lỗi503 tách riêng với nghiệm thu giao dịch hoặc định tuyến thành công.

## Nghiệm thu phần thanh toán và đánh giá

COD phải lưu đơn và payment PENDING/UNPAID, không chuyển PAID chỉ vì đặt hàng thành công. Ví/thẻ hiện cần provider/credential và kiểm webhook thực. Interface hoặc màn lựa chọn đã có không đồng nghĩa thanh toán online đã được nghiệm thu; phần kết nối ngoài bị chặn được ghi riêng, không giả lập giao dịch rồi báo hoàn tất thật.

Gửi đánh giá phải được backend xác minh đơn đã giao thuộc khách và có sản phẩm. Xem/lọc và aggregate phải đọc DB, không hardcode theo ảnh. Đủ code và unit test không thay test Android→Spring→SQL thật; xem đúng kết quả ở BAO_CAO_KIEM_THU, đặc biệt tài khoản khác/chưa mua/rating sai/trùng review và tổng hợp sau khi thêm.

## Cách trình bày cá nhân khoảng 6 phút

1. Phút đầu chỉ bảng phân công theo nguồn, nêu rõ PRODUCT_DETAIL và các phần payment/review được giao, cùng phạm vi nền nhóm.
2. Hai phút tiếp đi từ StoreEvent.OpenProduct qua productId, ProductDetailViewModel, ProductRepositoryImpl, Retrofit tới CatalogService/SQL và bốn trạng thái trở về UI.
3. Một phút trình bày chọn số lượng, giỏ lưu thật, checkout với idempotency, kiểm tồn và COD chưa trả tiền.
4. Một phút mở các test/bằng chứng đã chạy và đối chiếu một dữ liệu API với SQL, thêm/xem đánh giá khi đủ điều kiện.
5. Phút cuối nêu phần AI bổ sung, phần không nhận tác giả lịch sử, kết nối online/bản đồ còn thiếu nếu có và việc cần phối hợp tiếp.

Chỉ phát biểu số test, build hoặc SQL thật khi có báo cáo mới tương ứng. Các số33/9/4 và ảnh02/11 là lịch sử; không cộng dồn hoặc chép thành kết quả của bản Master hiện tại.
