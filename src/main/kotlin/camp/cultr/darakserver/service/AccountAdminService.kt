package camp.cultr.darakserver.service

import camp.cultr.darakserver.domain.Account
import camp.cultr.darakserver.domain.AccountGroupMember
import camp.cultr.darakserver.dto.RegisterRequest
import camp.cultr.darakserver.repository.AccountGroupMemberRepository
import camp.cultr.darakserver.repository.AccountGroupRepository
import camp.cultr.darakserver.repository.AccountRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.jvm.optionals.getOrNull

@Service
class AccountAdminService(
    private val accountRepository: AccountRepository,
    private val accountGroupRepository: AccountGroupRepository,
    private val accountGroupMemberRepository: AccountGroupMemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    /**
     * Registers a new user by creating an account and associating it with a default group.
     *
     * @param req Contains the registration details such as username, nickname, password, email,
     *            and the ID of the default group the user will be associated with.
     * @return A response indicating the result of the registration process. If successful,
     *         it contains a message with the value "success".
     * @throws ResponseStatusException if the specified group is not found.
     */
    @Transactional
    fun registerUser(req: RegisterRequest): String {
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
        return "success"
    }
}