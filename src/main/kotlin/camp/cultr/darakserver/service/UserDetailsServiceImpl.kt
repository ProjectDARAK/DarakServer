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

/**
 * Implementation of the `UserDetails` interface provided by Spring Security.
 * Represents the core user information exposed to Spring Security during authentication
 * and authorization processes.
 *
 * This data class encapsulates user details and contains additional methods for
 * determining the state of the user's account.
 *
 * @property id The unique identifier of the user.
 * @property email The email address associated with the user.
 * @property name The full name of the user.
 * @property nickname The nickname or username used for authentication.
 * @property authorities A list of granted authorities or roles assigned to the user.
 * @property enabled Indicates whether the user account is enabled.
 * @property _pw The hashed password for authentication purposes.
 */
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

/**
 * Service implementation of `UserDetailsService` for managing user authentication and retrieval.
 *
 * This class integrates with Spring Security to access, load, and return user details from a
 * data source during the authentication process. It uses the `AccountRepository` to retrieve
 * user data based on identifiers or provided user details.
 *
 * @constructor Creates an instance of `UserDetailsServiceImpl`.
 * @param accountRepository The repository used to fetch account information.
 */
@Service
class UserDetailsServiceImpl(private val accountRepository: AccountRepository) : UserDetailsService {
    /**
     * Loads a user based on their username and retrieves their details from the data source.
     * Utilizes the `AccountRepository` to fetch the corresponding user account. If the username
     * is null or if no user is found for the provided identifier, an exception is thrown.
     *
     * @param username The username of the user to be retrieved. This is expected to be convertible to a Long.
     * @return The `UserDetails` object representing the authenticated user, or null if the user could not be loaded.
     * @throws ResponseStatusException if the username is null or the user cannot be found in the data source.
     */
    override fun loadUserByUsername(username: String?): UserDetails? {
        if (username == null) throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val user =
            accountRepository.findById(username.toLong()).getOrNull()
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return UserDetailsImpl.build(user)
    }

    /**
     * Loads the user details based on the provided `UserDetailsImpl` object.
     * It retrieves the user account from the `AccountRepository` using the user's ID.
     *
     * @param user The `UserDetailsImpl` object containing the user's details, including the ID.
     * @return The user account retrieved from the repository.
     */
    fun loadUser(user: UserDetailsImpl) = accountRepository.findById(user.id).get()
}
