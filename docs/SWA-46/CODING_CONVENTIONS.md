# SmartMart Coding Conventions

## Naming
- Variables/functions: `lowerCamelCase`
- Classes/interfaces/composables: `PascalCase`
- Constants: `UPPER_SNAKE_CASE`
- Callbacks: prefix `on`

## Architecture
```text
Compose UI -> ViewModel -> Repository -> API/Database
API/Database -> Repository -> ViewModel -> UiState -> Compose UI
```

## UiState
```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

## Exception Handling Convention
- `IOException` -> lỗi mạng/kết nối.
- `HttpException` -> lỗi API.
- Database error -> chuyển thành lỗi nghiệp vụ phù hợp.
- Validation error -> ưu tiên kết quả validation, không dùng exception nếu không cần.
- Unknown exception -> `Timber.e(...)` + thông báo chung cho UI.
- Không dùng `catch (Exception) {}` rỗng.
- Không dùng exception để điều khiển luồng nghiệp vụ bình thường.
- Không hiển thị stack trace hoặc `e.message` thô cho người dùng.

```kotlin
viewModelScope.launch {
    _uiState.value = UiState.Loading

    try {
        val products = repository.getProducts()
        _uiState.value = UiState.Success(products)
    } catch (e: IOException) {
        Timber.e(e, "Network error")
        _uiState.value = UiState.Error("Không thể kết nối máy chủ")
    } catch (e: HttpException) {
        Timber.e(e, "API error")
        _uiState.value = UiState.Error("Máy chủ trả về lỗi")
    } catch (e: Exception) {
        Timber.e(e, "Unexpected error")
        _uiState.value = UiState.Error("Đã xảy ra lỗi")
    }
}
```

## Timber
Không log password, token, OTP, payment data hoặc dữ liệu cá nhân nhạy cảm.

## Git & Jira
Branch:
```text
feature/SWA-37-add-product
bugfix/SWA-10-fix-login-validation
```

Commit:
```text
SWA-37 Add product creation screen
SWA-10 Fix invalid login validation
```
