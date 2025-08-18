package camp.cultr.darakserver.service

import camp.cultr.darakserver.component.AccountUtil
import camp.cultr.darakserver.domain.Account
import camp.cultr.darakserver.domain.ShareType
import camp.cultr.darakserver.domain.SharedFiles
import camp.cultr.darakserver.dto.CommonResponse
import camp.cultr.darakserver.dto.FileResponse
import camp.cultr.darakserver.dto.FileShareRequest
import camp.cultr.darakserver.dto.FileShareResponse
import camp.cultr.darakserver.repository.AccountRepository
import camp.cultr.darakserver.repository.SharedFilesRepository
import camp.cultr.darakserver.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.jvm.optionals.getOrNull

private fun java.nio.file.Path.isSubPathOf(parent: java.nio.file.Path): Boolean {
    return try {
        normalize().toAbsolutePath().startsWith(parent.normalize().toAbsolutePath())
    } catch (e: Exception) {
        false
    }
}

class FileException(message: String) : Exception(message)

@Service
class FileService(
    @Value("\${darak.base-directory}") private val baseDirectory: String,
    private val accountUtil: AccountUtil,
    private val passwordEncoder: PasswordEncoder,
    private val sharedFilesRepository: SharedFilesRepository,
    private val accountRepository: AccountRepository,
) : Logger {
    /**
     * Lists the contents of a user's personal directory.
     *
     * The method resolves the provided path relative to the user's base directory
     * and retrieves entries in the directory, excluding symbolic links.
     * Each entry is mapped to a `FileResponse` containing details such as the filename,
     * extension, size, and a flag indicating whether it is a directory.
     * If the path is empty or invalid, the user's base directory is used as the target.
     *
     * @param path A relative path within the user's base directory. Defaults to an empty string, indicating the base directory itself.
     * @return A `CommonResponse` wrapping a list of `FileResponse` objects representing the directory's contents.
     */
    fun listPersonalDirectory(path: String = ""): CommonResponse<List<FileResponse>> = CommonResponse(
        // List directory entries, exclude symbolic links, and map each entry to FileResponse containing filename, extension, directory flag and size
        data = getTargetPath(path).listDirectoryEntries().filter { !it.isSymbolicLink() }.map {
            FileResponse(
                filename = it.fileName.toString(),
                extension = it.extension,
                isDirectory = it.isDirectory(),
                size = it.fileSize()
            )
        }
    )

    /**
     * Creates a directory at the specified path.
     *
     * This method resolves the provided path relative to the user's base directory
     * and creates the directory (including any necessary but nonexistent parent directories)
     * if it does not already exist. The resulting directory details are returned as a response.
     *
     * @param path The relative path within the base directory where the directory should be created.
     * @return A `CommonResponse` containing a `FileResponse` with details of the created directory,
     *         including the directory name, extension (empty for directories), a flag indicating
     *         it is a directory, and its size (defaulted to 0 for new directories).
     */
    fun mkdir(path: String): CommonResponse<FileResponse> {
        val targetDir = getTargetPath(path)
        if (!targetDir.exists()) {
            targetDir.toFile().mkdirs()
        }
        return CommonResponse(
            data = FileResponse(
                filename = path,
                extension = "",
                isDirectory = true,
                size = 0
            )
        )
    }

    /**
     * Saves a file to the specified path on the server.
     *
     * This method resolves the provided path within the intended directory structure,
     * validates the file input, and writes the file to the target location. If the file
     * already exists, it will be replaced. If the file name is null or if the path is invalid,
     * an exception is thrown.
     *
     * @param path The relative path within the base directory where the file should be saved.
     *             It is resolved against the user's target directory.
     * @param file The `MultipartFile` object containing the file data to be saved.
     * @return A `CommonResponse` containing a `FileResponse` with details about the saved file,
     *         including filename, extension, file size, and an indicator that it is not a directory.
     */
    fun saveFile(path: String, file: MultipartFile): CommonResponse<FileResponse> {
        val targetDir = getTargetPath(path)
        val filename = file.originalFilename?.substringAfterLast('/')?.substringAfterLast('\\')
            ?: throw FileException("File name is null")
        val targetFile = Path(targetDir.absolutePathString(), file.name).normalize()
        require(targetFile.startsWith(targetDir)) { "Invalid path: $path" }
        try {
            file.inputStream.use { inputStream ->
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING)
            }
            return CommonResponse(
                data = FileResponse(
                    filename = filename,
                    extension = targetFile.extension,
                    isDirectory = false,
                    size = targetFile.fileSize()
                )
            )
        } catch (e: IOException) {
            logger.error("Failed to save file(${targetFile.absolutePathString()}: ${e.message}")
            throw e
        }
    }

    /**
     * Deletes a file or directory at the specified path.
     *
     * This method resolves the provided path relative to the application's base directory.
     * If the file or directory exists, it will be deleted recursively. If the path does not exist,
     * a `FileException` is thrown.
     *
     * @param path The relative path to the file or directory to delete.
     * @return A `CommonResponse` containing a `FileResponse` object with details about
     *         the deleted file or directory, including its name, extension, whether it was a directory,
     *         and its size before deletion.
     * @throws FileException If the file or directory does not exist.
     */
    fun deleteFile(path: String): CommonResponse<FileResponse> {
        val targetFile = getTargetPath(path)
        if (targetFile.exists()) {
            targetFile.toFile().deleteRecursively()
            return CommonResponse(
                data = FileResponse(
                    filename = targetFile.fileName.toString(),
                    extension = targetFile.extension,
                    isDirectory = targetFile.isDirectory(),
                    size = targetFile.fileSize()
                )
            )
        } else {
            throw FileException("File not found: $path")
        }
    }

    /**
     * Shares a file or multiple files based on the provided request details.
     *
     * The method validates the sharing request against several conditions, ensures
     * the files exist, and saves the sharing configuration in the repository. It
     * supports various sharing types such as internal sharing, direct link sharing,
     * and websites with optional password protection.
     *
     * @param request The file sharing request containing paths, share type, shared accounts, and optional password.
     * @return A response containing the sharing URI wrapped in a common response object.
     */
    fun shareFile(request: FileShareRequest): CommonResponse<FileShareResponse> {
        val user = accountUtil.getUserOrThrow()
        val paths = request.paths.map { getTargetPath(it, user) }
        require(paths.all { it.exists() }) { "Invalid path included" }
        require(request.shareType == ShareType.INTERNAL && !request.sharedTo.isNullOrEmpty()) { "Internal share requires at least one sharedTo" }
        require(request.shareType == ShareType.DIRECT_LINK && request.password.isNullOrBlank()) { "Direct link share cannot include password" }
        require(request.shareType == ShareType.DIRECT_LINK && request.paths.size == 1) { "Direct link share can only be used for one file" }
        val sharedFiles = sharedFilesRepository.saveAndFlush(
            SharedFiles(
                files = request.paths.map { Path(user.username, it).toString() }.toMutableList(),
                shareType = request.shareType,
                password = if (request.shareType == ShareType.WEBSITE && !request.password.isNullOrBlank()) passwordEncoder.encode(
                    request.password
                ) else null,
                sharedBy = user,
                sharedTo = if (request.shareType == ShareType.INTERNAL && !request.sharedTo.isNullOrEmpty()) request.sharedTo.mapNotNull {
                    accountRepository.findById(
                        it
                    ).getOrNull()
                }.toMutableList() else mutableListOf(),
            )
        )
        return CommonResponse(
            data = FileShareResponse(
                shareUri = sharedFiles.id
            )
        )
    }

    fun listSharedFiles(shareUri: UUID, password: String = ""): CommonResponse<List<FileResponse>> {
        val sharedFiles = sharedFilesRepository.findById(shareUri).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: $shareUri")
        if (sharedFiles.shareType == ShareType.DIRECT_LINK) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")
        }
        if (sharedFiles.password != null && !passwordEncoder.matches(password, sharedFiles.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password")
        }
        return CommonResponse(
            data = sharedFiles.files.map {
                val path = Path(it)
                FileResponse(
                    filename = path.fileName.toString(),
                    extension = path.extension,
                    isDirectory = path.isDirectory(),
                    size = path.fileSize()
                )
            }
        )
    }

    fun getDirectSharedFile(shareUri: UUID): ResponseEntity<FileSystemResource> {
        val sharedFiles = sharedFilesRepository.findById(shareUri).getOrNull()
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: $shareUri")
        if (sharedFiles.shareType != ShareType.DIRECT_LINK) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")
        }
        val file = Path(sharedFiles.files.first(), sharedFiles.sharedBy.username)
        return ResponseEntity.ok(FileSystemResource(file))
    }

    private fun getTargetPath(path: String, _user: Account? = null): Path {

        val user = _user ?: accountUtil.getUserOrThrow()
        val baseDir = Path(baseDirectory, user.username)
        if (!baseDir.exists()) {
            baseDir.toFile().mkdirs()
        }
        return if (path.isBlank()) {
            baseDir
        } else {
            val normalized = Path(baseDir.absolutePathString(), path).normalize()
            if (!normalized.isSubPathOf(baseDir)) {
                baseDir
            } else {
                if (!normalized.exists()) normalized.toFile().mkdirs()
                normalized
            }
        }
    }
}