package camp.cultr.darakserver.service

import camp.cultr.darakserver.dto.AuthType
import camp.cultr.darakserver.dto.CommonResponse
import camp.cultr.darakserver.dto.JwtResponse
import camp.cultr.darakserver.dto.LoginRequest
import camp.cultr.darakserver.repository.AccountGroupMemberRepository
import camp.cultr.darakserver.repository.AccountGroupRepository
import camp.cultr.darakserver.repository.AccountRepository
import camp.cultr.darakserver.component.AccountUtil
import camp.cultr.darakserver.component.Generator
import camp.cultr.darakserver.domain.Account
import camp.cultr.darakserver.domain.AccountGroupMember
import camp.cultr.darakserver.dto.RegisterRequest
import camp.cultr.darakserver.component.JwtUtil
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.jvm.optionals.getOrNull

@Service
class AccountAuthService(
    private val accountRepository: AccountRepository,
    private val accountGroupRepository: AccountGroupRepository,
    private val accountGroupMemberRepository: AccountGroupMemberRepository,
    private val otpService: OtpService,
    private val jwtUtil: JwtUtil,
    private val accountUtil: AccountUtil,
    private val generator: Generator,
    private val passwordEncoder: PasswordEncoder,
) {
    /**
     * Determines the type of login authentication required for the specified user.
     *
     * Based on the user's account configuration, this method checks the available
     * authentication mechanisms (OTP, passkey, or password) and returns the respective
     * authentication type.
     *
     * @param username The username of the account to check login type for.
     * @return A `CommonResponse` containing the determined `AuthType` for the user's login.
     * @throws ResponseStatusException If the user is not found in the repository.
     */
    fun checkLoginType(username: String): CommonResponse<AuthType> {
        val user = accountRepository.findByUsername(username) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "User not found"
        )
        return CommonResponse(
            data = if (user.otpEnabled) {
                AuthType.OTP
            } else if (user.passkeyEnabled) {
                AuthType.PASSKEY
            } else {
                AuthType.PASSWORD
            }
        )
    }

    /**
     * Authenticates a user using their username and password.
     *
     * This method validates the provided credentials against stored data and generates
     * a JWT access token if authentication is successful. If the user has OTP login enabled,
     * the method denies password authentication.
     *
     * @param loginRequest Contains the username and password provided by the user for authentication.
     * @return A `CommonResponse` containing a `JwtResponse` with the generated JWT access token if authentication succeeds.
     * @throws ResponseStatusException If the user is not found, the password is incorrect, or OTP login is enabled for the user.
     */
    fun passwordLogin(loginRequest: LoginRequest): CommonResponse<JwtResponse> {
        val user = accountRepository.findByUsername(loginRequest.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        if (user.otpEnabled) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "OTP login enabled")
        }
        if (passwordEncoder.matches(loginRequest.password, user.password)) {
            return CommonResponse(
                data = JwtResponse(
                    jwtUtil.createAccessToken(user.id.toString(), emptyList()),
                )
            )
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
    }

    /**
     * Handles the OTP login flow for a user. Validates the user credentials against OTP and security recovery codes.
     * If the user is authenticated by security recovery codes, the security recovery code that used this login is removed from the user's account.
     *
     * @param loginRequest the login request containing the username and password or OTP token to authenticate the user.
     * @return a CommonResponse object containing a JwtResponse with the generated JWT token upon successful authentication.
     * @throws ResponseStatusException with HttpStatus.NOT_FOUND if the user is not found or authentication fails.
     * @throws ResponseStatusException with HttpStatus.BAD_REQUEST if OTP login is disabled for the user.
     */
    @Transactional
    fun otpLogin(loginRequest: LoginRequest): CommonResponse<JwtResponse> {
        val user = accountRepository.findByUsername(loginRequest.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        if(!user.otpEnabled) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP login disabled")
        }
        val otpValid = otpService.validate(user.otpSecret!!.toByteArray(), loginRequest.password)
        val objectMapper = ObjectMapper()
        val securityRecovery = objectMapper.readValue(user.securityRecovery, ArrayList::class.java)
        val securityRecoveryValid = securityRecovery.contains(loginRequest.password)
        if (otpValid || securityRecoveryValid) {
            if(securityRecoveryValid) {
                securityRecovery.remove(loginRequest.password)
                user.securityRecovery = objectMapper.writeValueAsString(securityRecovery)
            }
            return CommonResponse(
                data = JwtResponse(
                    jwtUtil.createAccessToken(user.id.toString(), emptyList()),
                )
            )
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
    }

    /**
     * Initiates the user's OTP registration process by generating and associating a unique OTP secret.
     *
     * This method retrieves the currently authenticated user and generates a base32-encoded
     * OTP secret which is stored in the user's account. An OTP URI is returned, which can
     * be used for configuring OTP in client applications (e.g., an authenticator app).
     *
     * @return A `CommonResponse` containing the generated OTP URI associated with the user's OTP secret.
     */
    @Transactional
    fun requestOtpRegister(): CommonResponse<String> {
        val user = accountUtil.getUserOrThrow()
        user.otpEnabled = false
        user.otpSecret = generator.generateBase32RandomChallengeToken(8)
        return CommonResponse(
            data = otpService.generateOtpUri(user.otpSecret!!.toByteArray(), user.username)
        )
    }

    /**
     * Verifies the OTP provided by the user for enabling OTP-based authentication.
     *
     * This method checks the validity of the provided OTP using the user's stored OTP secret.
     * If the verification is successful, OTP is enabled for the user's account, and a list of
     * security recovery tokens is generated and stored. These tokens can be used by the user
     * in case they lose access to their OTP device.
     *
     * @param otpCode The OTP code provided by the user for verification.
     * @return A `CommonResponse` containing a list of security recovery tokens if OTP verification succeeds.
     * @throws ResponseStatusException If the provided OTP is invalid.
     */
    @Transactional
    fun otpRegistrationVerify(otpCode: String): CommonResponse<List<String>> {
        val user = accountUtil.getUserOrThrow()
        if (otpService.validate(user.otpSecret!!.toByteArray(), otpCode)) {
            user.otpEnabled = true
            val securityRecovery = List(8) { generator.generateBase32RandomChallengeToken(8) }
            user.securityRecovery = ObjectMapper().writeValueAsString(securityRecovery)
            return CommonResponse(data = securityRecovery)
        } else {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is not valid")
        }
    }

    /**
     * Disables OTP-based authentication for the current user.
     *
     * This method verifies the provided security recovery token against the stored tokens for the user.
     * If the token is valid, OTP-based authentication is disabled. Otherwise, an error is thrown.
     *
     * @param securityRecoveryToken The security recovery token provided for disabling OTP.
     * @return A `CommonResponse` containing "OK" if OTP was successfully disabled.
     * @throws ResponseStatusException If the provided security recovery token is invalid.
     */
    @Transactional
    fun disableOtp(securityRecoveryToken: String): CommonResponse<String> {
        val user = accountUtil.getUserOrThrow()
        val securityRecovery = ObjectMapper().readValue(user.securityRecovery, List::class.java)
        if(!securityRecovery.contains(securityRecoveryToken)) {
            user.otpEnabled = false
            return CommonResponse(data = "OK")
        } else {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid security recovery token")
        }
    }

    /**
     * Generates a new set of security recovery tokens for the user and updates the user's account with the new tokens.
     *
     * The method retrieves the current user, generates a list of 8 randomly generated security recovery tokens,
     * encodes them as a JSON string, and updates the user's security recovery information.
     *
     * @return A response object containing the list of newly generated security recovery tokens.
     */
    @Transactional
    fun regenerateSecurityRecovery(): CommonResponse<List<String>> {
        val user = accountUtil.getUserOrThrow()
        val securityRecovery = List(8) { generator.generateBase32RandomChallengeToken(8) }
        user.securityRecovery = ObjectMapper().writeValueAsString(securityRecovery)
        return CommonResponse(data = securityRecovery)
    }
}
