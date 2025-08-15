package camp.cultr.darakserver.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.util.Date


@ConfigurationProperties(prefix = "darak")
data class JwtProperties(val jwtSecret: String, val jwtExpirationInSeconds: Long, val domains: List<String>)

@Component
class JwtUtil(private val jwtProperties: JwtProperties) {

    private val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.jwtSecret))
    private val issuer = jwtProperties.domains[0]
    private val ttl = jwtProperties.jwtExpirationInSeconds

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

    /** 유효하면 Claims 반환, 아니면 예외 발생 */
    fun validateAndParse(token: String) =
        Jwts.parser().verifyWith(key).requireIssuer(issuer).build().parseClaimsJws(token).payload
}
