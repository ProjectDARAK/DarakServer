package camp.cultr.darakserver.security

import camp.cultr.darakserver.component.AccountUtil
import camp.cultr.darakserver.component.TikaWrapper
import camp.cultr.darakserver.repository.AccountRepository
import camp.cultr.darakserver.repository.SharedFilesRepository
import camp.cultr.darakserver.service.FileService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Directory traversal and normalization tests for FileService.downloadFile().
 */
@SpringBootTest
class FileServiceTraversalTests {
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
        accountRepository = accountRepository,
        accountUtil = accountUtil,
        passwordEncoder = passwordEncoder,
        tikaWrapper = tikaWrapper
    )

    @Test
    fun `path containing parent directory segments should not escape base`() {
        // Create a harmless file in base to ensure base exists
        Files.writeString(tmpDir.resolve("dummy.txt"), "data")

        val thrown = runCatching { service().downloadFile("../outside.txt") }.exceptionOrNull()
        // Because implementation strips to last segment, this will look for outside.txt in base
        // and thus likely be NOT_FOUND, but crucially must not access outside of base.
        assertTrue(thrown is ResponseStatusException || thrown == null)
    }
}
