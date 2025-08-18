package camp.cultr.darakserver.dto

import camp.cultr.darakserver.domain.ShareType
import java.util.UUID

data class FileResponse(
    val filename: String,
    val extension: String,
    val isDirectory: Boolean,
    val size: Long,
)

data class FileUploadResponse(
    val filename: String,
    val id: UUID
)

data class FileShareRequest(
    val shareType: ShareType,
    val password: String? = null,
    val paths: List<String>,
    val sharedTo: List<Long>? = null,
)
data class FileShareResponse(
    val shareUri: UUID,
)