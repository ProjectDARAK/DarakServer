package camp.cultr.darakserver.service

import camp.cultr.darakserver.component.AccountUtil
import camp.cultr.darakserver.component.TikaWrapper
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
import camp.cultr.darakserver.util.filter.ResponseStatusExceptionParams
import camp.cultr.darakserver.util.filter.requireOrThrowResponseStatusException
import camp.cultr.darakserver.util.v5FromString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.jvm.optionals.getOrNull

/**
 * Checks if the current path is a sub-path of the specified parent path.
 *
 * @param parent the parent path to check against
 * @return true if the current path is a sub-path of the parent, false otherwise
 */
private fun java.nio.file.Path.isSubPathOf(parent: java.nio.file.Path): Boolean {
    return try {
        normalize().toAbsolutePath().startsWith(parent.normalize().toAbsolutePath())
    } catch (e: Exception) {
        false
    }
}

/**
 * A specific type of exception that is thrown to indicate an error related to file operations.
 *
 * @constructor Creates a [FileException] with the specified error [message].
 * @param message The detail message that provides information about the exception.
 */
class FileException(message: String) : Exception(message)

@Service
class FileService(
    @Value("\${darak.base-directory}") private val baseDirectory: String,
    private val sharedFilesRepository: SharedFilesRepository,
    private val accountRepository: AccountRepository,
    private val accountUtil: AccountUtil,
    private val passwordEncoder: PasswordEncoder,
    private val tikaWrapper: TikaWrapper
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
    fun listPersonalDirectory(path: String = ""): List<FileResponse> =
        getTargetPath(path).listDirectoryEntries().filter { !it.isSymbolicLink() }.map {
            FileResponse(
                fileUUID = v5FromString(it.fileName.toString()),
                filename = it.fileName.toString(),
                extension = it.extension,
                isDirectory = it.isDirectory(),
                size = it.fileSize()
            )
        }

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
    fun mkdir(path: String): FileResponse {
        val targetDir = getTargetPath(path)
        if (!targetDir.exists()) {
            targetDir.toFile().mkdirs()
        }
        return FileResponse(
            fileUUID = v5FromString(targetDir.fileName.toString()),
            filename = targetDir.fileName.toString(),
            extension = "",
            isDirectory = true,
            size = 0
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
    fun saveFile(path: String, file: MultipartFile): FileResponse {
        val targetDir = getTargetPath(path)
        val filename = file.originalFilename?.substringAfterLast('/')?.substringAfterLast('\\')
            ?: throw FileException("File name is null")
        val targetFile = Path(targetDir.absolutePathString(), file.name).normalize()
        require(targetFile.startsWith(targetDir)) { "Invalid path: $path" }
        try {
            file.inputStream.use { inputStream ->
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING)
            }
            return FileResponse(
                fileUUID = v5FromString(filename),
                filename = filename,
                extension = targetFile.extension,
                isDirectory = false,
                size = targetFile.fileSize()
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
    fun deleteFile(path: String): FileResponse {
        val targetFile = getTargetPath(path)
        if (targetFile.exists()) {
            targetFile.toFile().deleteRecursively()
            return FileResponse(
                fileUUID = v5FromString(targetFile.fileName.toString()),
                filename = targetFile.fileName.toString(),
                extension = targetFile.extension,
                isDirectory = targetFile.isDirectory(),
                size = targetFile.fileSize()
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
    fun shareFile(request: FileShareRequest): FileShareResponse {
        val user = accountUtil.getUserOrThrow()
        val paths = request.paths.map { getTargetPath(it, user).normalize() }.filter { it.exists() }.distinct()
        require(paths.all { it.exists() }) { "Invalid path included" }
        require(request.shareType == ShareType.INTERNAL && !request.sharedTo.isNullOrEmpty()) { "Internal share requires at least one sharedTo" }
        require(request.shareType == ShareType.DIRECT_LINK && request.password.isNullOrBlank()) { "Direct link share cannot include password" }
        require(request.shareType == ShareType.DIRECT_LINK && request.paths.size == 1) { "Direct link share can only be used for one file" }
        val sharedFiles = sharedFilesRepository.saveAndFlush(
            SharedFiles(
                files = paths.map { it.toString() }.toMutableList(),
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
        return FileShareResponse(
            shareUri = sharedFiles.id
        )
    }

    /**
     * Retrieves a list of shared files associated with a given share URI. If the shared files are
     * password-protected, the correct password must be provided to access them.
     *
     * @param shareUri The unique identifier of the shared file resource.
     * @param password The password to unlock access to the files, if required. Defaults to an empty string.
     * @return A response object containing a list of file metadata wrapped in a CommonResponse object.
     *         Each file metadata includes the filename, extension, whether it is a directory, and its size.
     * @throws ResponseStatusException with NOT_FOUND status if the shared file resource is not found or inaccessible.
     * @throws ResponseStatusException with UNAUTHORIZED status if the provided password is incorrect for password-protected files.
     */
    fun listSharedFiles(shareUri: UUID, password: String = ""): List<FileResponse> {
        val sharedFiles = sharedFilesRepository.findById(shareUri).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: $shareUri")
        if (sharedFiles.shareType == ShareType.DIRECT_LINK) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")
        }
        if (sharedFiles.password != null && !passwordEncoder.matches(password, sharedFiles.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password")
        }
        return sharedFiles.files.map {
            val path = Path(it)
            FileResponse(
                fileUUID = v5FromString(path.fileName.toString()),
                filename = path.fileName.toString(),
                extension = path.extension,
                isDirectory = path.isDirectory(),
                size = path.fileSize()
            )
        }
    }

    /**
     * Downloads a shared file based on the given share URI and file UUIDs. Handles different share types
     * (INTERNAL, DIRECT_LINK, WEBSITE) and validates user authorization or password as necessary.
     *
     * @param shareUri The unique identifier of the share containing the files to download.
     * @param fileUuids A list of unique identifiers for the files to be downloaded within the shared URI.
     * @param password The password required to access shared files if the share type is PASSWORD-PROTECTED (default is an empty string).
     * @return A ResponseEntity containing a StreamingResponseBody for the requested file download.
     * @throws ResponseStatusException If the share or specific file UUIDs are not found, user lacks access, or password is invalid.
     */
    fun downloadSharedFile(
        shareUri: UUID,
        fileUuids: List<UUID>,
        password: String = ""
    ): ResponseEntity<StreamingResponseBody> {
        val sharedFiles = sharedFilesRepository.findById(shareUri).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: $shareUri")
        val allFiles = sharedFiles.files.associate {
            val path = Path(it)
            v5FromString(path.fileName.toString()) to path
        }
        requireOrThrowResponseStatusException(fileUuids.all { allFiles.contains(it) }) {
            ResponseStatusExceptionParams(
                HttpStatus.NOT_FOUND,
                "fileUUID is not included in the shared file: $shareUri."
            )
        }
        when (sharedFiles.shareType) {
            ShareType.INTERNAL -> {
                if (sharedFiles.sharedTo.contains(accountUtil.getUserOrThrow())) {
                    return makeSharedFilesDownloadable(allFiles, fileUuids)
                } else {
                    throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this file")
                }
            }

            ShareType.DIRECT_LINK -> {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")
            }

            ShareType.WEBSITE -> {
                if (sharedFiles.password != null && !passwordEncoder.matches(password, sharedFiles.password)) {
                    throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password")
                }
                return makeSharedFilesDownloadable(allFiles, fileUuids)
            }
        }
    }

    /**
     * Prepares shared files for download. If only one file is requested, it handles downloading
     * the file directly. If multiple files are requested, it packages them into a zip before
     * making them downloadable.
     *
     * @param allFiles a map where the key is the UUID of the file and the value is its corresponding path
     * @param downloadFileUuids a list of UUIDs representing the files to be downloaded
     * @return ResponseEntity containing a StreamingResponseBody for the requested download
     */
    private fun makeSharedFilesDownloadable(
        allFiles: Map<UUID, Path>,
        downloadFileUuids: List<UUID>
    ): ResponseEntity<StreamingResponseBody> =
        if (downloadFileUuids.size == 1) downloadFile(allFiles[downloadFileUuids.first()]!!)
        else downloadFileZip(downloadFileUuids.map { allFiles[it]!! })

    /**
     * Retrieves a directly shared file associated with the provided share URI.
     *
     * @param shareUri The unique identifier of the shared file.
     * @return A ResponseEntity containing the requested file as a FileSystemResource.
     * @throws ResponseStatusException if the file is not found or the share type is not a direct link.
     */
    fun getDirectSharedFile(shareUri: UUID): ResponseEntity<StreamingResponseBody> {
        val sharedFiles = sharedFilesRepository.findById(shareUri).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: $shareUri")
        if (sharedFiles.shareType != ShareType.DIRECT_LINK) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")
        }
        val file = Path(sharedFiles.files.first(), sharedFiles.sharedBy.username)
        return downloadFile(file.toString())
    }

    /**
     * Downloads a file for the user from the specified path.
     *
     * @param path The file path for the file to be downloaded.
     * @return A ResponseEntity containing a StreamingResponseBody for the file download.
     */
    fun downloadFileForUser(path: String): ResponseEntity<StreamingResponseBody> = downloadFile(getTargetPath(path))

    /**
     * Downloads a file from the specified path.
     * Alias for [downloadFile(String)]
     *
     * @param file the path of the file to be downloaded
     * @see downloadFile(String)
     */
    private fun downloadFile(file: Path) = downloadFile(file.toString())

    /**
     * Downloads a file from the specified file path and returns it as a response entity.
     *
     * Validates that the requested file path is within the allowed base directory to prevent unauthorized access.
     * Streams the file content directly to the client to handle large files efficiently.
     *
     * @param filePath The path of the file to be downloaded. The path must be relative to the configured base directory.
     * @return A ResponseEntity object containing the file as a streaming response body, along with proper headers such as content disposition and content type.
     * Throws IllegalArgumentException if the requested path is outside of the base directory.
     */
    private fun downloadFile(filePath: String): ResponseEntity<StreamingResponseBody> {
        val requestedPath = Path(filePath.substringAfterLast('/')).normalize()
        val basePath = Path(baseDirectory).normalize()
        val normalizedPath = basePath.resolve(requestedPath).normalize()

        requireOrThrowResponseStatusException(normalizedPath.isSubPathOf(basePath)) {
            ResponseStatusExceptionParams(
                HttpStatus.FORBIDDEN,
                "File path is outside of the base directory"
            )
        }
        requireOrThrowResponseStatusException(!normalizedPath.isSymbolicLink()) {
            ResponseStatusExceptionParams(
                HttpStatus.FORBIDDEN,
                "Symbolic link detected"
            )
        }
        requireOrThrowResponseStatusException(normalizedPath.exists()) {
            ResponseStatusExceptionParams(
                HttpStatus.NOT_FOUND,
                "File not found: $filePath"
            )
        }

        val body = StreamingResponseBody { output ->
            Files.newInputStream(normalizedPath).use { input: InputStream ->
                input.copyTo(output, 1024 * 64) // 64KB 버퍼
            }
        }

        val cd = ContentDisposition.attachment()
            .filename(normalizedPath.fileName.toString(), StandardCharsets.UTF_8)
            .build()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
            .contentType(MediaType.valueOf(tikaWrapper.getMimeType(normalizedPath)))
            .contentLength(normalizedPath.fileSize())
            .body(body)
    }

    /**
     * Creates a downloadable ZIP file containing the specified files from the server's directory.
     *
     * @param filePaths A list of file paths relative to the base directory that need to be included in the ZIP file.
     * @return A ResponseEntity containing a StreamingResponseBody which streams the ZIP file to the client.
     *         The response includes appropriate headers indicating that it is a file attachment.
     */
    private fun downloadFileZip(filePaths: List<Path>): ResponseEntity<StreamingResponseBody> {
        val requestedPaths = filePaths.map { it.normalize() }
        val basePath = Path(baseDirectory).normalize()
        val normalizedPaths = requestedPaths.map { basePath.resolve(it).normalize() }

        requireOrThrowResponseStatusException(normalizedPaths.all { it.isSubPathOf(basePath) }) {
            ResponseStatusExceptionParams(HttpStatus.FORBIDDEN, "File path is outside of the base directory")
        }
        requireOrThrowResponseStatusException(normalizedPaths.none { it.isSymbolicLink() }) {
            ResponseStatusExceptionParams(HttpStatus.FORBIDDEN, "Symbolic link detected")
        }
        requireOrThrowResponseStatusException(normalizedPaths.all { it.exists() }) {
            ResponseStatusExceptionParams(HttpStatus.NOT_FOUND, "Some files not found")
        }

        val body = StreamingResponseBody { output ->
            ZipOutputStream(output).use { zipOut ->
                normalizedPaths.forEach { path ->
                    addToZip(zipOut, path, basePath)
                }
            }
        }

        val cd = ContentDisposition.attachment()
            .filename("archive.zip", StandardCharsets.UTF_8)
            .build()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(body)
    }

    /**
     * Adds files or directories to a ZIP output stream.
     *
     * @param zipOut The ZipOutputStream where files or directories will be added.
     * @param path The Path of the file or directory to be added to the ZIP.
     * @param basePath The base Path to calculate relative paths for entries within the ZIP.
     */
    private fun addToZip(zipOut: ZipOutputStream, path: Path, basePath: Path) {
        if (path.isDirectory()) {
            Files.walk(path)
                .filter { !it.isDirectory() }
                .forEach { file ->
                    val entryName = basePath.relativize(file).toString()
                    zipOut.putNextEntry(ZipEntry(entryName))
                    Files.copy(file, zipOut)
                    zipOut.closeEntry()
                }
        } else {
            val entryName = basePath.relativize(path).toString()
            zipOut.putNextEntry(ZipEntry(entryName))
            Files.copy(path, zipOut)
            zipOut.closeEntry()
        }
    }

    /**
     * Resolves and returns the target path based on the given relative path and optional user account.
     *
     * @param path The relative path for which the target path is to be determined. Can be blank.
     * @param _user An optional user account to determine the base directory. If not provided, the current user is used.
     * @return The resolved absolute target path, ensuring it is within the user's base directory. If necessary, directories will be created.
     */
    private fun getTargetPath(path: String, _user: Account? = null): Path {
        checkFilePath(path)
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

    /**
     * Validates a file path to ensure that it does not contain invalid patterns or locations.
     *
     * @param path the file path to be validated
     */
    fun checkFilePath(path: String) = requireOrThrowResponseStatusException(
        !(path.contains("..") || path.contains(baseDirectory) || path.startsWith("/")),
    ) { ResponseStatusExceptionParams(HttpStatus.BAD_REQUEST, "Invalid path") }
}