package camp.cultr.darakserver.component

import camp.cultr.darakserver.domain.Account
import camp.cultr.darakserver.repository.AccountRepository
import camp.cultr.darakserver.service.UserDetailsImpl
import camp.cultr.darakserver.service.UserDetailsServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class AccountUtil(
    private val userDetailsServiceImpl: UserDetailsServiceImpl,
    private val accountRepository: AccountRepository,
) {
    fun getUser(): Account? {
        return try {
            val principal = SecurityContextHolder.getContext().authentication.principal
            if (principal is UserDetailsImpl) {
                userDetailsServiceImpl.loadUser(principal)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getUserOrThrow() = getUser() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
}
