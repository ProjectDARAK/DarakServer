package camp.cultr.darakserver.security

import camp.cultr.darakserver.component.AccountUtil
import camp.cultr.darakserver.component.TikaWrapper
import camp.cultr.darakserver.repository.AccountRepository
import camp.cultr.darakserver.repository.SharedFilesRepository
import camp.cultr.darakserver.service.FileService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.file.Files
import java.nio.file.Path

/**
 * Security-focused unit tests for FileService.downloadFile() behavior.
 * These tests verify protections against symlink abuse and proper status codes
 * without requiring Spring context or a database.
 */
@SpringBootTest
class FileServiceSecurityTests {

    // Provide minimal dependencies that FileService(downloadFile) does not actually use
    @Autowired
    private lateinit var sharedFilesRepository: SharedFilesRepository
    @Autowired
    private lateinit var accountRepository: AccountRepository
    @Autowired
    private lateinit var accountUtil: AccountUtil
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    private lateinit var tikaWrapper: TikaWrapper

    @TempDir
    lateinit var tmpDir: Path

    private fun service(): FileService = FileService(
        baseDirectory = tmpDir.toAbsolutePath().toString(),
        sharedFilesRepository = sharedFilesRepository,
        accountRepository = accountRepository ,
        accountUtil = accountUtil,
        passwordEncoder = passwordEncoder,
        tikaWrapper = tikaWrapper
    )

    @Test
    fun `downloadFile returns 200 for a valid file in base directory`() {
        val base = tmpDir
        val file = base.resolve("hello.txt")
        Files.writeString(file, "hello world")

        val resp: ResponseEntity<StreamingResponseBody> = service().downloadFile(file.fileName.toString())
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertTrue(resp.headers.contentDisposition.toString().contains("hello.txt"))
        // Ensure content length is set
        assertTrue(resp.headers.contentLength >= 0)
    }

    @Test
    fun `downloadFile forbids symbolic link that points outside base directory`() {
        val base = tmpDir
        // Create a file outside base
        val outsideDir = Files.createTempDirectory("outside")
        val secret = outsideDir.resolve("secret.txt")
        Files.writeString(secret, "top-secret")

        // Create symlink inside base pointing to outside secret
        val link = base.resolve("link.txt")
        try {
            Files.createSymbolicLink(link, secret)
        } catch (e: UnsupportedOperationException) {
            // Filesystem may not support symlinks on Windows without privileges; skip test in that case
            return
        } catch (e: Exception) {
            return
        }

        val resp = runCatching { service().downloadFile(link.fileName.toString()) }.exceptionOrNull()
        // downloadFile uses requireOrThrowResponseStatusException which throws ResponseStatusException
        // on forbidden symbolic links. We check that condition indirectly by seeing an exception thrown.
        assertTrue(resp is org.springframework.web.server.ResponseStatusException)
        if (resp is org.springframework.web.server.ResponseStatusException) {
            assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        }
    }

    @Test
    fun `downloadFile returns 404 for non-existent file`() {
        val resp = runCatching { service().downloadFile("nope.bin") }.exceptionOrNull()
        assertTrue(resp is org.springframework.web.server.ResponseStatusException)
        if (resp is org.springframework.web.server.ResponseStatusException) {
            assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        }
    }
}
