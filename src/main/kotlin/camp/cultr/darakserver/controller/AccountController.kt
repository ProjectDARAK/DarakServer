package camp.cultr.darakserver.controller

import camp.cultr.darakserver.dto.LoginRequest
import camp.cultr.darakserver.dto.RegisterRequest
import camp.cultr.darakserver.service.AccountService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/account")
class AccountController(
    private val accountService: AccountService,
) {
    @GetMapping("/auth/checkLoginType")
    fun checkLoginType(@RequestParam username: String) = accountService.checkLoginType(username)

    @PostMapping("/auth/login/password")
    fun passwordLogin(@RequestBody loginRequest: LoginRequest) = accountService.passwordLogin(loginRequest)

    @PostMapping("/auth/login/otp")
    fun otpLogin(@RequestBody loginRequest: LoginRequest) = accountService.otpLogin(loginRequest)

    @PostMapping("/auth/otp")
    fun requestOtpRegister() = accountService.requestOtpRegister()

    @PutMapping("/auth/otp")
    fun otpRegistrationVerify(@RequestParam otpCode: String) = accountService.otpRegistrationVerify(otpCode)

    @PostMapping("/account/user")
    fun registerUser(@RequestBody registerRequest: RegisterRequest) = accountService.registerUser(registerRequest)
}