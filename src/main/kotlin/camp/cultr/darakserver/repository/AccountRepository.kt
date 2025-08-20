package camp.cultr.darakserver.repository

import camp.cultr.darakserver.domain.Account
import camp.cultr.darakserver.domain.AccountGroup
import camp.cultr.darakserver.domain.AccountGroupMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AccountRepository: JpaRepository<Account, Long> {
    fun findByUsername(username: String): Account?
}

@Repository
interface AccountGroupRepository: JpaRepository<AccountGroup, Long> {}

@Repository
interface AccountGroupMemberRepository: JpaRepository<AccountGroupMember, Long> {}