package camp.cultr.darakserver.util.filter

import camp.cultr.darakserver.dto.CommonResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class ApiKeyFilter(
    @Value("\${darak.api-key}") private val apiKey: String
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val apiKeyHeader = request.getHeader("X-API-KEY")
        if (!apiKeyHeader.isNullOrEmpty() && apiKeyHeader == apiKey) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.use {
                it.print(
                    ObjectMapper().writeValueAsString(
                        CommonResponse(
                            code = HttpServletResponse.SC_UNAUTHORIZED,
                            data = "Invalid API Key",
                            transactionId = UUID.randomUUID().toString(),
                        )
                    )
                )
            }
        }
    }
}