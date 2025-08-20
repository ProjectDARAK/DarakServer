package camp.cultr.darakserver.util.errorhandler

import camp.cultr.darakserver.util.Logger
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * JWT Authentication Entry Point
 *
 * This class handles unauthorized access attempts to secured resources. It implements Spring Security's
 * AuthenticationEntryPoint interface to provide a custom response when a user tries to access a protected resource
 * without proper authentication.
 *
 * When an unauthenticated user attempts to access a secured endpoint, this entry point is triggered and returns a
 * standardized error response with a 401 Unauthorized status.
 */
@Component
class AuthEntryPointJwt() : AuthenticationEntryPoint, Logger {

    /**
     * Handles unauthorized access attempts.
     *
     * This method is called by Spring Security when an AuthenticationException is thrown during the authentication
     * process. It creates a standardized error response with a 401 Unauthorized status and a JSON error message.
     *
     * @param request The HTTP request that resulted in an AuthenticationException
     * @param response The HTTP response to be modified
     * @param authException The exception that was thrown during authentication
     * @throws IOException If an input or output error occurs
     * @throws ServletException If a servlet error occurs
     */
    @Throws(IOException::class, ServletException::class)
    override fun commence(
        request: HttpServletRequest?,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        // Log the unauthorized access attempt
        logger.error("Unauthorized error: ${authException.message}")

        // Send a 401 Unauthorized response with a JSON error message
        response.sendError(
            HttpServletResponse.SC_UNAUTHORIZED,
            "Unauthorized: Authentication token was either missing or invalid.",
        )
    }
}
