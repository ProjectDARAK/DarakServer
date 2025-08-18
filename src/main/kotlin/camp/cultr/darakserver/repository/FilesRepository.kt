package camp.cultr.darakserver.repository

import camp.cultr.darakserver.domain.SharedFiles
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SharedFilesRepository: JpaRepository<SharedFiles, UUID>

