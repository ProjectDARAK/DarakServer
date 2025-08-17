package camp.cultr.darakserver.service

import camp.cultr.darakserver.domain.Account
import camp.cultr.darakserver.repository.AccountRepository
import kotlin.jvm.optionals.getOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

data class UserDetailsImpl(
    val id: Long,
    val email: String,
    val name: String,
    val nickname: String,
    private val authorities: List<GrantedAuthority>,
    val enabled: Boolean,
    val _pw: String,
) : UserDetails {
    override fun getAuthorities() = authorities.toMutableList()

    override fun getPassword() = _pw

    override fun getUsername() = nickname

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

    override fun isCredentialsNonExpired() = true

    override fun isEnabled() = enabled

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserDetailsImpl
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        private val serialVersionUID: Long = 1

        fun build(account: Account) =
            UserDetailsImpl(
                account.id,
                account.email,
                account.username,
                account.nickname,
                listOf(),
                account.enabled,
                account.password,
            )
    }
}

@Service
class UserDetailsServiceImpl(private val accountRepository: AccountRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails? {
        if (username == null) throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val user =
            accountRepository.findById(username.toLong()).getOrNull()
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return UserDetailsImpl.build(user)
    }

    fun loadUser(user: UserDetailsImpl) = accountRepository.findById(user.id).get()
}
