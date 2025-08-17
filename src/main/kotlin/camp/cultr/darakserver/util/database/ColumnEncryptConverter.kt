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
