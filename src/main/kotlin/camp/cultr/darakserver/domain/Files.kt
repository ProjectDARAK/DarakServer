package camp.cultr.darakserver.domain

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.ZonedDateTime
import java.util.UUID

enum class ShareType {
    INTERNAL, WEBSITE, DIRECT_LINK
}
@Entity
@Table(
    name = "shared_files"
)
data class SharedFiles(
    @Id
    val id: UUID = UUID.randomUUID(),
    val files: MutableList<String> = mutableListOf(),
    val shareType: ShareType,
    val password: String? = null,
    @ManyToOne
    val sharedBy: Account,
    @OneToMany
    val sharedTo: MutableList<Account> = mutableListOf(),
    @CreatedDate var createdAt: ZonedDateTime? = null,
    @LastModifiedDate var updateAt: ZonedDateTime? = null,
)

data class SharedFilesDownloadRequest(
    val filesUuid: List<UUID>
)