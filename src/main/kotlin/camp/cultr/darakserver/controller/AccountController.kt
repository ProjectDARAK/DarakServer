package camp.cultr.darakserver.controller

import camp.cultr.darakserver.dto.LoginRequest
import camp.cultr.darakserver.dto.RegisterRequest
import camp.cultr.darakserver.service.AccountAdminService
import camp.cultr.darakserver.service.AccountAuthService
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
    private val accountAdminService: AccountAdminService
) {

    @PostMapping("/user")
    fun registerUser(@RequestBody registerRequest: RegisterRequest) = accountAdminService.registerUser(registerRequest)

}