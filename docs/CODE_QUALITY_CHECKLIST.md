# SmartMart Code Quality Checklist

## Architecture
- [ ] UI không gọi Repository trực tiếp
- [ ] Business logic không nằm trong Composable
- [ ] ViewModel quản lý state và validation
- [ ] Repository che giấu nguồn dữ liệu

## Exception Handling
- [ ] Exception được bắt ở tầng phù hợp
- [ ] Không có `catch` rỗng
- [ ] Không nuốt exception im lặng
- [ ] `IOException`/`HttpException` được ánh xạ sang lỗi UI phù hợp
- [ ] Không hiển thị stack trace hoặc `e.message` thô cho người dùng
- [ ] Không dùng exception để điều khiển nghiệp vụ bình thường
- [ ] Có test cho error path quan trọng

## Logging & Security
- [ ] Dùng Timber
- [ ] Không log password/token/OTP/payment data
- [ ] Không commit secret/key

## Testing
- [ ] Logic mới có unit test phù hợp
- [ ] Happy path pass
- [ ] Invalid/error path pass
- [ ] Test độc lập và dễ đọc

## Git & Jira
- [ ] Branch có Jira ID
- [ ] Commit có Jira ID
- [ ] PR title có Jira ID
- [ ] PR mô tả thay đổi và cách test

## Ready to Merge
- [ ] Build thành công
- [ ] Không còn blocker comment
- [ ] Reviewer approve
- [ ] Acceptance Criteria được đáp ứng
- [ ] Không còn bug Critical/Blocker
