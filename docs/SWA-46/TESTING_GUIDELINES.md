# SmartMart Testing Guidelines

## Các mức kiểm thử
1. Unit Testing
2. Integration Testing
3. Functional Testing
4. UI Testing
5. Exception/Error Path Testing
6. Retest
7. Regression Testing

## Functional Testing
Mỗi User Story tối thiểu kiểm tra:
- Happy path
- Invalid input
- Empty input
- Boundary value
- Error path
- Acceptance Criteria chính

## Exception/Error Path Testing
Cần kiểm tra:
- Mất mạng / timeout
- HTTP 4xx / 5xx
- `IOException`
- Dữ liệu API không hợp lệ
- Database operation thất bại
- Unexpected exception
- UI không crash
- UI hiển thị thông báo thân thiện
- Không lộ stack trace

## Mapping
```text
User Story -> Acceptance Criteria -> Test Case -> Execute
Pass -> Review/Done
Fail -> Jira Bug -> Fix -> Retest
```

## Story Done Checklist
- [ ] Acceptance Criteria đã test
- [ ] Happy path pass
- [ ] Error path quan trọng pass
- [ ] Unit Test pass
- [ ] Functional Test hoàn thành
- [ ] Không crash do exception có thể xử lý
- [ ] Không lộ stack trace/thông tin kỹ thuật
- [ ] Bug fix đã Retest
- [ ] Regression đã chạy khi cần
- [ ] PR đã review và merge
