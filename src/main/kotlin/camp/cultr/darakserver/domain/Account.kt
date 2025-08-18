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

/**
 * Represents an account entity within the application.
 * This class is mapped to the database table "account" and contains the account's core attributes.
 *
 * The `Account` entity is indexed on the "username" column to ensure uniqueness and efficient lookups.
 *
 * @property id The unique identifier for the account. This is auto-generated and serves as the primary key.
 * @property username The unique username associated with the account. It is used as a login credential.
 * @property nickname A user-defined nickname for the account.
 * @property password The hashed password for the account. It is required for authentication.
 * @property email The email address associated with the account.
 * @property pavicon Optional profile picture URL or reference for the account.
 * @property enabled Indicates whether the account is currently active and enabled.
 * @property otpSecret An optional encrypted secret used for enabling Two-Factor Authentication (2FA) with OTP.
 * @property otpEnabled Indicates whether OTP-based Two-Factor Authentication is enabled for the account.
 * @property passkeyEnabled Indicates whether passkey-based authentication is enabled for the account.
 * @property createdAt The timestamp when the account was created.
 * @property updateAt The timestamp when the account was last updated.
 * @property groupMappings A list of `AccountGroupMember` mappings associated with this account, representing the groups the account belongs to.
 */
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

/**
 * Represents a group of accounts within the application.
 * This class is mapped to the "account_group" database table and organizes accounts into groups.
 *
 * @property id The unique identifier for the group. It serves as the primary key and is auto-generated.
 * @property name The name of the account group.
 * @property description An optional textual description of the account group.
 * @property enabled Indicates whether the group is active and enabled for use.
 * @property createdAt The timestamp indicating when the account group was created.
 * @property updateAt The timestamp indicating when the account group was last updated.
 * @property memberMappings A list of `AccountGroupMember` entities representing the associations between accounts and this group.
 */
@Entity
@Table(name = "account_group")
data class AccountGroup(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    var name: String,
    var description: String? = null,
    var enabled: Boolean = true,
    @CreatedDate var createdAt: ZonedDateTime? = null,
    @LastModifiedDate var updateAt: ZonedDateTime? = null,
    @OneToMany(mappedBy = "group") var memberMappings: MutableList<AccountGroupMember> = mutableListOf(),
)

/**
 * Represents the association between an `Account` and an `AccountGroup`.
 * This entity bridges the relationship between accounts and account groups, indicating the groups
 * an account is a member of and the accounts associated with a given group.
 *
 * The class is mapped to the database table "account_group_member".
 *
 * @property id The unique identifier for the account-group membership. It serves as the primary key and is auto-generated.
 * @property account The `Account` entity representing the account associated with this membership.
 * @property group The `AccountGroup` entity representing the group associated with this membership.
 */
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