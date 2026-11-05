package com.smartmart.smartmart_backend

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.Pattern
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Bổ sung trong đợt hợp nhất theo yêu cầu của Lê Tuấn Thành, có hỗ trợ AI.
 * Vai trò lấy từ dbo.Users; client không được tự chọn userId hay quyền.
 */
data class SessionUser(val id: Long, val fullName: String, val email: String, val phone: String?, val role: String)
data class SessionResponse(val token: String, val user: SessionUser)
data class RegisterRequest(
    @field:NotBlank @field:Size(max = 120) val fullName: String,
    @field:Email @field:NotBlank @field:Size(max = 254) val email: String,
    @field:Size(min = 10, max = 128) val password: String,
    @field:Size(max = 20) @field:Pattern(regexp = "^$|[+]?[0-9][0-9 .()-]{6,18}[0-9]") val phone: String? = null
)
// Field name email is retained for client compatibility; accepts email or phone identifier.
data class LoginRequest(@field:NotBlank @field:Size(max = 254) val email: String, @field:Size(min = 1, max = 128) val password: String)
data class ProfileRequest(@field:NotBlank @field:Size(max = 120) val fullName: String,
    @field:Size(max = 20) @field:Pattern(regexp = "^$|[+]?[0-9][0-9 .()-]{6,18}[0-9]") val phone: String? = null,
    @field:Email @field:Size(max = 254) val email: String? = null)

object Passwords {
    private const val ITERATIONS = 210_000
    private val random = SecureRandom()
    fun hash(password: String): String {
        val salt = ByteArray(16).also(random::nextBytes)
        val result = derive(password, salt, ITERATIONS)
        return "pbkdf2-sha256:$ITERATIONS:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(result)}"
    }
    fun verify(password: String, encoded: String): Boolean = try {
        val parts = encoded.split(':')
        if (parts.size != 4 || parts[0] != "pbkdf2-sha256") false
        else {
            val iterations = parts[1].toInt()
            if (iterations !in 100_000..1_000_000) false
            else MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]), derive(password, Base64.getDecoder().decode(parts[2]), iterations))
        }
    } catch (_: IllegalArgumentException) { false }
    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    fun token(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
    fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

@Service
class AuthService(private val jdbc: JdbcTemplate, @Value("\${smartmart.session-hours:24}") private val sessionHours: Int) {
    private val userColumns = "u.UserID AS id,u.FullName AS fullName,u.Email AS email,u.Phone AS phone,u.Role AS role"
    private fun user(row: Map<String, Any?>) = SessionUser((row["id"] as Number).toLong(), row["fullName"] as String,
        row["email"] as String, row["phone"] as String?, row["role"] as String)

    @Transactional
    fun register(body: RegisterRequest): SessionResponse {
        val email = body.email.trim().lowercase()
        val phone = normalizePhone(body.phone)
        if (jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Users WITH(UPDLOCK,HOLDLOCK) WHERE Email=?", Int::class.java, email)!! > 0)
            conflict("EMAIL_EXISTS", "Email đã được sử dụng")
        if (phone != null && jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Users WITH(UPDLOCK,HOLDLOCK) WHERE Phone=?",Int::class.java,phone)!! > 0)
            conflict("PHONE_EXISTS", "Số điện thoại đã được sử dụng")
        val id = jdbc.queryForObject("INSERT dbo.Users(FullName,Email,Phone,PasswordHash,Role) OUTPUT INSERTED.UserID VALUES(?,?,?,?,'CUSTOMER')",
            Long::class.java, body.fullName.trim(), email, phone, Passwords.hash(body.password))!!
        jdbc.update("INSERT dbo.Carts(UserID) VALUES(?)", id)
        return issue(find(id))
    }

    @Transactional
    fun login(body: LoginRequest): SessionResponse {
        val identifier = body.email.trim().lowercase()
        val row = jdbc.queryForList("SELECT $userColumns,u.PasswordHash AS passwordHash FROM dbo.Users u WHERE (u.Email=? OR u.Phone=?) AND u.IsActive=1", identifier,normalizePhone(identifier)).firstOrNull()
        // Run a derivation even for a missing account so the error does not disclose account existence.
        val encoded = row?.get("passwordHash") as String? ?: DUMMY_HASH
        if (!Passwords.verify(body.password, encoded) || row == null)
            throw ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng")
        return issue(user(row))
    }

    private fun issue(user: SessionUser): SessionResponse {
        val token = Passwords.token()
        jdbc.update("INSERT dbo.SessionTokens(TokenHash,UserID,ExpiresAt) VALUES(?,?,DATEADD(HOUR,?,SYSUTCDATETIME()))",
            Passwords.digest(token), user.id, sessionHours.coerceIn(1, 168))
        return SessionResponse(token, user)
    }

    fun current(request: HttpServletRequest): SessionUser {
        val hash = tokenHash(request)
        val row = jdbc.queryForList("SELECT $userColumns FROM dbo.SessionTokens s JOIN dbo.Users u ON u.UserID=s.UserID WHERE s.TokenHash=? AND s.ExpiresAt>SYSUTCDATETIME() AND u.IsActive=1", hash).firstOrNull()
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại")
        return user(row)
    }

    fun roles(request: HttpServletRequest, vararg allowed: String): SessionUser = current(request).also { if (it.role !in allowed) forbidden() }
    fun find(id: Long): SessionUser = jdbc.queryForList("SELECT $userColumns FROM dbo.Users u WHERE u.UserID=?", id).firstOrNull()?.let(::user) ?: notFound()
    fun logout(request: HttpServletRequest) {
        current(request)
        jdbc.update("DELETE dbo.SessionTokens WHERE TokenHash=?", tokenHash(request))
    }
    @Transactional
    fun profile(id: Long, body: ProfileRequest): SessionUser {
        if (body.email != null && body.email.isBlank()) badRequest("Email không được để trống")
        val email=body.email?.trim()?.lowercase()
        val phone=normalizePhone(body.phone)
        if(email!=null && jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Users WITH(UPDLOCK,HOLDLOCK) WHERE Email=? AND UserID<>?",Int::class.java,email,id)!!>0) conflict("EMAIL_EXISTS","Email đã được sử dụng")
        if(phone!=null && jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Users WITH(UPDLOCK,HOLDLOCK) WHERE Phone=? AND UserID<>?",Int::class.java,phone,id)!!>0) conflict("PHONE_EXISTS","Số điện thoại đã được sử dụng")
        jdbc.update("UPDATE dbo.Users SET FullName=?,Phone=COALESCE(?,Phone),Email=COALESCE(?,Email) WHERE UserID=?", body.fullName.trim(), phone, email, id)
        return find(id)
    }
    private fun tokenHash(request: HttpServletRequest): String {
        val header = request.getHeader("Authorization") ?: ""
        if (!header.startsWith("Bearer ") || header.length !in 30..200)
            throw ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Vui lòng đăng nhập để tiếp tục")
        return Passwords.digest(header.removePrefix("Bearer "))
    }
    companion object {
        private val DUMMY_HASH = Passwords.hash("No real account has this password")
        private fun normalizePhone(value: String?): String? = value?.trim()?.takeIf(String::isNotBlank)?.replace(Regex("[ .()-]"),"")
    }
}

@RestController
class AuthController(private val auth: AuthService) {
    @PostMapping("/api/auth/register") @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody body: RegisterRequest) = auth.register(body)
    @PostMapping("/api/auth/login") fun login(@Valid @RequestBody body: LoginRequest) = auth.login(body)
    @PostMapping("/api/auth/logout") fun logout(request: HttpServletRequest): Map<String, Boolean> { auth.logout(request); return mapOf("success" to true) }
    @GetMapping("/api/me") fun me(request: HttpServletRequest) = auth.current(request)
    @PutMapping("/api/me") fun profile(request: HttpServletRequest, @Valid @RequestBody body: ProfileRequest) = auth.profile(auth.current(request).id, body)
}
