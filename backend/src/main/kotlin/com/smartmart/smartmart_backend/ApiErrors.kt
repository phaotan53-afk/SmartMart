package com.smartmart.smartmart_backend

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI. */
data class ApiError(val code: String, val message: String)
class ApiException(val status: HttpStatus, val code: String, override val message: String) : RuntimeException(message)
fun badRequest(message: String): Nothing = throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message)
fun notFound(message: String = "Không tìm thấy dữ liệu"): Nothing = throw ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message)
fun conflict(code: String, message: String): Nothing = throw ApiException(HttpStatus.CONFLICT, code, message)
fun forbidden(): Nothing = throw ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này")

@RestControllerAdvice
class ApiErrorHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun api(error: ApiException) = ResponseEntity.status(error.status).body(ApiError(error.code, error.message))

    @ExceptionHandler(MethodArgumentNotValidException::class, ConstraintViolationException::class,
        HttpMessageNotReadableException::class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException::class,
        org.springframework.web.bind.MissingRequestHeaderException::class,
        org.springframework.web.bind.MissingServletRequestParameterException::class)
    fun invalid(error: Exception): ResponseEntity<ApiError> = ResponseEntity.badRequest()
        .body(ApiError("INVALID_INPUT", "Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường bắt buộc và định dạng"))

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException::class,
        org.springframework.web.servlet.NoHandlerFoundException::class)
    fun missing(error: Exception): ResponseEntity<ApiError> = ResponseEntity.status(404)
        .body(ApiError("NOT_FOUND", "Không tìm thấy tài nguyên"))

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException::class)
    fun method(error: Exception): ResponseEntity<ApiError> = ResponseEntity.status(405)
        .body(ApiError("METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun duplicate(error: DataIntegrityViolationException): ResponseEntity<ApiError> = ResponseEntity.status(409)
        .body(ApiError("DATA_CONFLICT", "Dữ liệu đã tồn tại hoặc đang được sử dụng"))

    @ExceptionHandler(DataAccessException::class)
    fun database(error: DataAccessException): ResponseEntity<ApiError> {
        logger.error("Database operation failed: {}", error.javaClass.simpleName)
        return ResponseEntity.status(503).body(ApiError("DATABASE_UNAVAILABLE", "Không thể xử lý dữ liệu lúc này. Vui lòng thử lại"))
    }

    @ExceptionHandler(Exception::class)
    fun unexpected(error: Exception): ResponseEntity<ApiError> {
        logger.error("Unexpected request failure: {}", error.javaClass.simpleName)
        return ResponseEntity.status(500).body(ApiError("INTERNAL_ERROR", "Đã xảy ra lỗi. Vui lòng thử lại"))
    }
}
