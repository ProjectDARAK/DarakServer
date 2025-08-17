package camp.cultr.darakserver.component

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.util.Date

/**
 * Represents the properties required for JWT configuration within the application.
 *
 * The properties are loaded and bound to this class from the application's configuration
 * using the prefix `darak`. These properties include the JWT secret key, expiration time,
 * and domain used for token generation and validation.
 *
 * @property jwtSecret The secret key used to sign and verify JWT tokens.
 * @property jwtExpirationInSeconds The expiration duration of JWT tokens, specified in seconds.
 * @property domain The domain to be included as the issuer in the JWT tokens.
 */
@ConfigurationProperties(prefix = "darak")
data class JwtProperties(val jwtSecret: String, val jwtExpirationInSeconds: Long, val domain: String)

/**
 * Utility class for handling JSON Web Tokens (JWT) operations, such as token creation
 * and validation, configured using the provided `JwtProperties`.
 *
 * This class is registered as a Spring component and can be injected wherever needed.
 *
 * @constructor Initializes the `JwtUtil` with the given `JwtProperties` for token signing and verification.
 * @property jwtProperties Provides the necessary configuration for JWT operations, such as the secret key,
 *    expiration time, and issuer domain.
 */
@Component
class JwtUtil(private val jwtProperties: JwtProperties) {

    private val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.jwtSecret))
    private val issuer = jwtProperties.domain
    private val ttl = jwtProperties.jwtExpirationInSeconds

    /**
     * Creates an access token (JWT) for the given subject with specified roles.
     *
     * @param subject The unique identifier or username of the subject for whom the token is created.
     * @param roles A list of roles to be included as a claim in the token.
     * @return A signed JWT string containing the subject, roles, issuer, issue time, and expiration time.
     */
    fun createAccessToken(subject: String, roles: List<String>): String {
        val now = Date()
        val exp = Date(now.time + ttl * 1000)
        return Jwts.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(now)
            .expiration(exp)
            .claim("roles", roles) // ["USER","ADMIN"]
            .signWith(key)
            .compact()
    }

    /**
     * Validates the given JSON Web Token (JWT) to ensure its integrity and authenticity.
     *
     * This method checks if the token is properly signed and issued by the expected issuer.
     *
     * @param token The JWT as a string to be validated.
     * @return `true` if the token is valid, otherwise `false`.
     */
    fun validate(token: String): Boolean =
        try {
            Jwts.parser().verifyWith(key).requireIssuer(issuer).build().parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Validates the given JSON Web Token (JWT) and extracts its payload.
     *
     * This method ensures the token is properly signed and issued by the expected issuer.
     * If the token is valid, the payload is parsed and returned.
     *
     * @param token The JWT as a string to be validated and parsed.
     * @return The payload of the validated JWT.
     * @throws io.jsonwebtoken.JwtException If the token is invalid or cannot be parsed.
     */
    fun validateAndParse(token: String) =
        Jwts.parser().verifyWith(key).requireIssuer(issuer).build().parseClaimsJws(token).payload
}
