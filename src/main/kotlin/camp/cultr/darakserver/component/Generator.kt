package camp.cultr.darakserver.component

import org.apache.commons.codec.binary.Base32
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class Generator {

    /**
     * Generates a random token consisting of printable ASCII characters.
     *
     * @param length The desired length of the generated token.
     * @return A randomly generated string token of the specified length.
     */
    fun generateChallengeToken(length: Int): String {
        val asciiStart = 33 // '!'의 ASCII 코드
        val asciiEnd = 126 // '~'의 ASCII 코드
        val random = SecureRandom()

        return buildString(length) {
            repeat(length) {
                val charCode = random.nextInt(asciiEnd - asciiStart + 1) + asciiStart
                append(charCode.toChar())
            }
        }
    }

    fun generateBase32RandomChallengeToken(length: Int): String {
        val base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.map { base32Alphabet[(it.toInt() and 0xFF) and 31] }.joinToString("")
    }

    fun randomIV12(): ByteArray {
        val random = SecureRandom()
        return ByteArray(12).also { random.nextBytes(it) }
    }
}
