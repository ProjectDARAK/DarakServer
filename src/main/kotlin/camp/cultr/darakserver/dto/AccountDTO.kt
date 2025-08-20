package camp.cultr.darakserver.dto

/**
 * Enumeration representing different authentication methods supported by the system.
 *
 * This enum is primarily used to define the type of authentication mechanism
 * a user can employ during login or registration processes. It encapsulates
 * the following options:
 *
 * - PASSWORD: Traditional authentication using a password.
 * - OTP: One-Time Password-based authentication.
 * - PASSKEY: Secure authentication using passkeys.
 *
 * It is utilized in various service functionalities to determine or validate
 * the type of authentication mechanism required or implemented for a user.
 */
enum class AuthType {
    PASSWORD, OTP, PASSKEY
}

/**
 * Represents the data required to initiate a login request.
 *
 * This data class is used to encapsulate the user's credentials during the login process.
 * It includes the username and password/OTP provided by the user seeking authentication.
 *
 * @property username The username of the user attempting to log in.
 * @property password The corresponding password/OTP associated with the username.
 */
data class LoginRequest(val username: String, val password: String)

/**
 * Represents a response containing JWT tokens.
 *
 * This data class encapsulates the access token required for authenticated interactions
 * and an optional refresh token for generating new access tokens without re-authentication.
 *
 * @property accessToken The JWT access token for client authentication.
 * @property refreshToken An optional JWT refresh token used to obtain new access tokens.
 */
data class JwtResponse(val accessToken: String, val refreshToken: String? = null)

/**
 * Represents a request to register a new user account.
 *
 * This data class is used to encapsulate all the necessary information required to
 * register a new user in the system. It includes fields like username, nickname,
 * password, email, and the default group ID to which the user will be assigned.
 *
 * @property username The username for the new account.
 * @property nickname The display name for the user.
 * @property password The password for the account. It should be securely hashed before storage.
 * @property email The email address associated with the account.
 * @property defaultGroupId The ID of the default group to which the user will be added.
 */
data class RegisterRequest(
    val username: String,
    val nickname: String,
    val password: String,
    val email: String,
    val defaultGroupId: Long
)
