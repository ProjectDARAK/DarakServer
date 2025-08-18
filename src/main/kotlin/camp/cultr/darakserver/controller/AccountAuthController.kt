package camp.cultr.darakserver.controller

import camp.cultr.darakserver.dto.LoginRequest
import camp.cultr.darakserver.service.AccountAuthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/account/auth")
class AccountAuthController(
    private val accountAuthService: AccountAuthService,
) {

    @GetMapping("/auth/checkLoginType")
    fun checkLoginType(@RequestParam username: String) = accountAuthService.checkLoginType(username)

    @PostMapping("/auth/login/password")
    fun passwordLogin(@RequestBody loginRequest: LoginRequest) = accountAuthService.passwordLogin(loginRequest)

    @PostMapping("/auth/login/otp")
    fun otpLogin(@RequestBody loginRequest: LoginRequest) = accountAuthService.otpLogin(loginRequest)

    @PostMapping("/auth/otp")
    fun requestOtpRegister() = accountAuthService.requestOtpRegister()

    @PutMapping("/auth/otp")
    fun otpRegistrationVerify(@RequestParam otpCode: String) = accountAuthService.otpRegistrationVerify(otpCode)
}