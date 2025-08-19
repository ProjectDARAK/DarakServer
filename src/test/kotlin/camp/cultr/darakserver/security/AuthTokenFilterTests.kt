package camp.cultr.darakserver.security

import camp.cultr.darakserver.component.JwtUtil
import camp.cultr.darakserver.util.filter.AuthTokenFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import java.util.Base64

/**
 * Tests for AuthTokenFilter to verify that a valid token passed via the Base64-encoded
 * `token` query parameter is accepted and authentication is populated into the SecurityContext.
 */
@SpringBootTest
class AuthTokenFilterTests {
    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Autowired
    private lateinit var userDetailsService: UserDetailsService

    @Autowired
    private lateinit var authTokenFilter: AuthTokenFilter


    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `auth filter sets authentication when valid token provided via token param`() {
        val token = jwtUtil.createAccessToken("0", listOf("USER"))
        val encoded = Base64.getEncoder().encodeToString("Bearer $token".toByteArray())

        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = MockFilterChain()

        Mockito.`when`(request.getHeader("Authorization")).thenReturn(null)
        Mockito.`when`(request.getParameter("token")).thenReturn(encoded)

        val user = User("0", "noop", listOf(SimpleGrantedAuthority("ROLE_USER")))
//        Mockito.`when`(userDetailsService.loadUserByUsername("0")).thenReturn(user)
        authTokenFilter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth, "Authentication should be set in the security context")
        assertEquals("0", auth.name)
    }

    private class MockFilterChain : FilterChain {
        override fun doFilter(request: ServletRequest?, response: ServletResponse?) {
            // no-op
        }
    }
}
