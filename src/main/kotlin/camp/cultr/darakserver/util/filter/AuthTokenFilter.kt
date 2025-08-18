package camp.cultr.darakserver.util.filter

import camp.cultr.darakserver.component.JwtUtil
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.util.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT Authentication Filter
 *
 * This filter intercepts HTTP requests to validate JWT tokens and authenticate users. It's part of the Spring Security
 * filter chain and is responsible for:
 * 1. Extracting JWT tokens from requests
 * 2. Validating tokens
 * 3. Loading user details based on the token
 * 4. Setting up authentication in the Spring Security context
 *
 * The filter extends OncePerRequestFilter to ensure it's only executed once per request.
 */
@Component
class AuthTokenFilter(private val jwtUtil: JwtUtil, @Lazy private val userDetailsService: UserDetailsService) :
    OncePerRequestFilter() {

    var myLogger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Main filter method that processes each HTTP request.
     *
     * This method extracts and validates the JWT token from the request, loads the user details, and sets up
     * authentication if the token is valid.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param filterChain The filter chain for passing the request to the next filter or handler
     * @throws ServletException If a servlet error occurs
     * @throws IOException If an input or output error occurs
     */
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            // Extract JWT token from the request
            val jwt = getJwt(request)

            // Validate the token and set up authentication
            if (jwt != null && jwtUtil.validate(jwt)) {
                // TODO: JWT DB Validation
                // Extract username from the token
                val username: String = jwtUtil.validateAndParse(jwt).subject
                myLogger.info("Validated JWT token for user: $username")

                // Load user details from the database
                val userDetails = userDetailsService.loadUserByUsername(username)

                // Create an authentication token with user details and authorities
                val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails!!.authorities)
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                // Set authentication in the security context
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (ex: JwtException) {
            myLogger.info("JWT token is invalid: ${ex.message}")
        } catch (e: Exception) {
            // Log if authentication information is missing or invalid
            myLogger.info("Missing authentication info")
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response)
    }

    /**
     * Extracts the JWT token from the HTTP request.
     *
     * The token can be provided in two ways:
     * 1. In the Authorization header
     * 2. As a Base64-encoded 'token' parameter
     *
     * @param request The HTTP request
     * @return The JWT token string, or null if not found
     */
    private fun getJwt(request: HttpServletRequest): String? {
        val headerAuth =
            request.getHeader("Authorization") ?: String(Base64.getDecoder().decode(request.getParameter("token")))
        logger.info("parseJwt:auth:$headerAuth")
        return headerAuth.replace("Bearer ", "")
    }
}
