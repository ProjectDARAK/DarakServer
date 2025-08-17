package camp.cultr.darakserver.dto

enum class AuthType {
    PASSWORD, OTP, PASSKEY
}

data class LoginRequest(val username: String, val password: String)

data class JwtResponse(val accessToken: String, val refreshToken: String? = null)

data class RegisterRequest(
    val username: String,
    val nickname: String,
    val password: String,
    val email: String,
    val defaultGroupId: Long
)
