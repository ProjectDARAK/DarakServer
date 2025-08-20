package camp.cultr.darakserver.util

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * Generates a version 5 UUID (Name-Based UUID) using the given name and optional namespace UUID.
 * The UUID is generated according to the RFC 4122 specification.
 *
 * @param name The name to generate the UUID from. This is typically a string that represents the identity of the object.
 * @param namespace An optional namespace UUID that specifies the namespace for the generated UUID.
 * Defaults to the UUID for "6ba7b812-9dad-11d1-80b4-00c04fd430c8"(OID).
 * @return A version 5 UUID derived from the name and namespace.
 */
fun v5FromString(name: String, namespace: UUID? = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8")): UUID {
    val md = MessageDigest.getInstance("SHA-1")
    if (namespace != null) md.update(uuidToBytes(namespace))
    md.update(name.toByteArray(Charsets.UTF_8))
    val hash = md.digest()
    val bytes = hash.copyOfRange(0, 16)

    // 버전(0101b=5), variant(RFC 4122) 세팅
    bytes[6] = ((bytes[6].toInt() and 0x0F) or (5 shl 4)).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

    return bytesToUuid(bytes)
}

private fun uuidToBytes(u: UUID): ByteArray =
    ByteBuffer.allocate(16).putLong(u.mostSignificantBits).putLong(u.leastSignificantBits).array()

private fun bytesToUuid(bytes: ByteArray): UUID {
    val bb = ByteBuffer.wrap(bytes)
    return UUID(bb.long, bb.long)
}