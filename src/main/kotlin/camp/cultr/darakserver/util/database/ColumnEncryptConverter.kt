package camp.cultr.darakserver.util.database

import camp.cultr.darakserver.component.Generator
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Component responsible for encrypting and decrypting strings using AES-GCM with a 256-bit key.
 * This class is implemented as an `AttributeConverter` to handle the automatic conversion of attributes
 * into encrypted data for storage in the database and back to plain text when retrieved.
 *
 * The encryption process uses AES-GCM (Galois/Counter Mode) for authenticated encryption, ensuring both
 * data confidentiality and integrity. The encryption payload includes the initialization vector (IV),
 * ciphertext, and authentication tag, all of which are Base64-encoded for storage purposes.
 *
 * Dependencies:
 * - Requires a secure base AES-256 key, which is provided via the application configuration.
 * - Utilizes the `Generator` component to create a secure random initialization vector (IV) for each encryption.
 *
 * Use Cases:
 * - Can be applied to encrypt sensitive fields in entity classes, ensuring secure storage of data at rest.
 *
 * Behavior:
 * - For encryption (`convertToDatabaseColumn`):
 *   - Skips encryption for `null` or empty values.
 *   - Generates a random IV of 12 bytes and uses AES-GCM to produce the ciphertext and tag.
 *   - Combines the IV, ciphertext, and tag into a single payload and encodes it as a Base64 string.
 * - For decryption (`convertToEntityAttribute`):
 *   - Skips decryption for `null` or empty values.
 *   - Decodes the Base64-encoded payload, extracts the IV, ciphertext, and tag.
 *   - Uses AES-GCM to decrypt the data and return the original plain text.
 *
 * Security Considerations:
 * - The AES key must be securely stored and properly configured in the application.
 * - The key must be 256 bits (32 bytes) in size to meet AES-256 specifications.
 * - Invalid payloads (e.g., incorrect size or altered data) will result in an exception, ensuring
 *   that tampered data cannot be successfully decrypted.
 */
@Converter(autoApply = false)
@Component
class ColumnEncryptConverter(
    @Value("\${darak.base-aes-key}") private val aesKey: String,
    private val generator: Generator,
) :
    AttributeConverter<String, String> {

    private val keySpec: SecretKeySpec by lazy {
        val keyBytes = Base64.getDecoder().decode(aesKey)
        require(keyBytes.size == 32) { "AES key must be 32 bytes for AES-256." }
        SecretKeySpec(keyBytes, AES)
    }

    /**
     * Converts the given attribute into a database-compatible encrypted string representation.
     *
     * This method encrypts a non-null, non-empty input string using AES-GCM encryption with a randomly
     * generated IV and a predefined encryption key. The resulting encrypted data, along with the IV
     * and authentication tag, is encoded into a Base64 string for storage compatibility in the database.
     * If the input string is `null` or empty, it is returned as-is without encryption.
     *
     * @param attribute The input string to be encrypted. Can be null.
     * @return A Base64-encoded, encrypted string representation of the input;
     *         or the original input if it is null or empty.
     */
    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) return null
        if (attribute.isEmpty()) return attribute // 비어있는 문자열은 그대로 저장(정책에 따라 암호화해도 무방)
        val iv = generator.randomIV12()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(TAG_BIT_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertextWithTag = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))

        // [IV || CIPHERTEXT||TAG] → Base64
        val payload = ByteArray(iv.size + ciphertextWithTag.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(ciphertextWithTag, 0, payload, iv.size, ciphertextWithTag.size)
        return Base64.getEncoder().encodeToString(payload)
    }

    /**
     * Converts the provided database-compatible string into its decrypted plain text representation.
     *
     * This method decrypts a non-null, non-empty Base64-encoded string using AES-GCM decryption,
     * with the IV and authentication tag extracted from the input. The decryption is performed using
     * a predefined encryption key. If the input string is `null` or empty, it is returned as-is.
     *
     * @param dbData The input Base64-encoded string to be decrypted. Can be null.
     * @return The decrypted plain text string, or the original input if it is null or empty.
     * @throws IllegalArgumentException if the provided input does not meet the expected format for AES-GCM payloads.
     */
    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) return null
        if (dbData.isEmpty()) return dbData
        val payload = Base64.getDecoder().decode(dbData)
        require(payload.size >= 12 + 16) { "Invalid AES-GCM payload (too short)." }

        val iv = payload.copyOfRange(0, 12)
        val cipherTextWithTag = payload.copyOfRange(12, payload.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(TAG_BIT_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val plain = cipher.doFinal(cipherTextWithTag)
        return String(plain, Charsets.UTF_8)
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BIT_LENGTH = 128
        private const val AES = "AES"
    }
}
