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
import camp.cultr.darakserver.util.JwtUtil
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.jvm.optionals.getOrNull

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val accountGroupRepository: AccountGroupRepository,
    private val accountGroupMemberRepository: AccountGroupMemberRepository,
    private val jwtUtil: JwtUtil,
    private val accountUtil: AccountUtil,
    private val generator: Generator,
    private val otpService: OtpService,
    private val passwordEncoder: PasswordEncoder,
) {
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

    fun passwordLogin(loginRequest: LoginRequest): CommonResponse<JwtResponse> {
        val user = accountRepository.findByUsername(loginRequest.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
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

    fun otpLogin(loginRequest: LoginRequest): CommonResponse<JwtResponse> {
        val user = accountRepository.findByUsername(loginRequest.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        if (otpService.validate(user.otpSecret!!.toByteArray(), loginRequest.password)) {
            return CommonResponse(
                data = JwtResponse(
                    jwtUtil.createAccessToken(user.id.toString(), emptyList()),
                )
            )
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
    }

    @Transactional
    fun requestOtpRegister(): CommonResponse<String> {
        val user = accountUtil.getUserOrThrow()
        user.otpSecret = generator.generateBase32RandomChallengeToken(8)
        return CommonResponse(
            data = otpService.generateOtpUri(user.otpSecret!!.toByteArray(), user.username)
        )
    }

    @Transactional
    fun otpRegistrationVerify(otpCode: String): CommonResponse<String> {
        val user = accountUtil.getUserOrThrow()
        if (otpService.validate(user.otpSecret!!.toByteArray(), otpCode)) {
            user.otpEnabled = true
            return CommonResponse(data = "OK")
        } else {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is not valid")
        }
    }

    @Transactional
    fun registerUser(req: RegisterRequest): CommonResponse<String> {
        val group = accountGroupRepository.findById(req.defaultGroupId).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
        val user = accountRepository.saveAndFlush(
            Account(
                username = req.username,
                nickname = req.nickname,
                password = passwordEncoder.encode(req.password),
                email = req.email
            )
        )
        accountGroupMemberRepository.saveAndFlush(
            AccountGroupMember(
                account = user,
                group = group
            )
        )
        return CommonResponse(data = "success")
    }
}
