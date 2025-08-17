package camp.cultr.darakserver.domain

import camp.cultr.darakserver.util.database.ColumnEncryptConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "account",
    indexes = [
        Index(name = "username_idx", columnList = "username", unique = true)
    ]
)
data class Account(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    val username: String,
    var nickname: String,
    var password: String,
    var email: String,
    var pavicon: String? = null,
    var enabled: Boolean = true,
    @Convert(converter = ColumnEncryptConverter::class)
    @Column(name = "otp_secret", columnDefinition = "TEXT")
    var otpSecret: String? = null,
    @Column(name = "otp_enabled", columnDefinition = "BOOLEAN DEFAULT false")
    var otpEnabled: Boolean = false,
    @Column(name = "passkey_enabled", columnDefinition = "BOOLEAN DEFAULT false")
    var passkeyEnabled: Boolean = false,
    @CreatedDate var createdAt: ZonedDateTime? = null,
    @LastModifiedDate var updateAt: ZonedDateTime? = null,
    @OneToMany(mappedBy = "account") var groupMappings: MutableList<AccountGroupMember> = mutableListOf(),
)

@Entity
@Table(name = "account_group")
data class AccountGroup(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    var name: String,
    var description: String? = null,
    var enabled: Boolean = true,
    @CreatedDate var createdAt: ZonedDateTime,
    @LastModifiedDate var updateAt: ZonedDateTime,
    @OneToMany(mappedBy = "group") var memberMappings: MutableList<AccountGroupMember> = mutableListOf(),
)

@Entity
@Table(name = "account_group_member")
data class AccountGroupMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @ManyToOne
    @JoinColumn(name = "account_id")
    val account: Account,
    @ManyToOne
    @JoinColumn(name = "account_group_id")
    val group: AccountGroup,
)