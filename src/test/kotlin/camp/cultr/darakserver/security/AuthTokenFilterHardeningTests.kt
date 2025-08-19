package camp.cultr.darakserver.security

import camp.cultr.darakserver.component.JwtProperties
import camp.cultr.darakserver.component.JwtUtil
import camp.cultr.darakserver.util.filter.AuthTokenFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService

/**
 * Additional hardening tests for AuthTokenFilter around malformed/absent inputs.
 */
@SpringBootTest
class AuthTokenFilterHardeningTests {

    private fun filter(): AuthTokenFilter {
        val secret = "Y0ZhWVg4bllDa0VOQ2FIMzdRWHpCUFM2WHBHQ0p3bnFkc1JrTDU5VDdFOTJIOFhzWkc2NXFGd3VtaHZtN3dBZFBSSFpGbjhrUmZKeHpQNUhZWlFUTTNSMjN4VnU5ZlZLdEFnUm1VeUF6c1UzZXNnOHR3REhWS2VLYnNlN3VndG4="
        val jwtProps = JwtProperties(jwtSecret = secret, jwtExpirationInSeconds = 3600, domain = "localhost")
        val jwtUtil = JwtUtil(jwtProps)
        val uds = mock(UserDetailsService::class.java)
        return AuthTokenFilter(jwtUtil, uds)
    }

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `no header and no token param should not authenticate and should not throw`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = NoopChain()

        Mockito.`when`(request.getHeader("Authorization")).thenReturn(null)
        Mockito.`when`(request.getParameter("token")).thenReturn(null)

        filter().doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication, "Authentication must remain null")
    }

    @Test
    fun `invalid base64 token param should not authenticate`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = NoopChain()

        Mockito.`when`(request.getHeader("Authorization")).thenReturn(null)
        Mockito.`when`(request.getParameter("token")).thenReturn("@@not_base64@@")

        filter().doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication, "Authentication must remain null when token is garbage")
    }

    @Test
    fun `decoded token without Bearer prefix should not authenticate`() {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = NoopChain()

        Mockito.`when`(request.getHeader("Authorization")).thenReturn(null)
        // Encodes just a raw jwt string which won't start with "Bearer "
        Mockito.`when`(request.getParameter("token")).thenReturn(java.util.Base64.getEncoder().encodeToString("just-a-token".toByteArray()))

        filter().doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication, "Authentication must remain null without Bearer prefix and valid JWT")
    }

    private class NoopChain : FilterChain {
        override fun doFilter(request: ServletRequest?, response: ServletResponse?) { /* no-op */ }
    }
}
