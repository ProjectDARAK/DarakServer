package camp.cultr.darakserver.util.errorhandler

import camp.cultr.darakserver.dto.CommonResponse
import camp.cultr.darakserver.util.Logger
import io.sentry.Hint
import io.sentry.Sentry
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Global exception handler for web applications.
 *
 * This class provides centralized exception handling for specific types of exceptions and extends
 * `ResponseEntityExceptionHandler` to benefit from Spring's built-in exception handling capabilities.
 * It uses a custom response format (`CommonResponse`) to standardize error responses across the application.
 *
 * The class handles the following exception types:
 * - ResponseStatusException: Handles exceptions with custom HTTP status codes and reasons.
 * - General exceptions: Catches other unhandled exceptions, logging errors and returning a
 *   generic internal server error response.
 *
 * Additionally, it includes a private utility method (`captureException`) that can be used
 * to log or process exceptions further if needed.
 */
@ControllerAdvice
class GlobalWebExceptionHandler: ResponseEntityExceptionHandler(), Logger {
    /**
     * Handles exceptions of type ResponseStatusException by logging the error details, capturing the exception
     * for additional processing, and returning a standardized error response encapsulated in a ResponseEntity.
     *
     * @param exception The ResponseStatusException thrown during execution, containing the HTTP status and reason for failure.
     * @param request The WebRequest object corresponding to the request that caused the exception.
     * @return A ResponseEntity containing a CommonResponse with the HTTP status code and reason provided by the exception.
     */
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        exception: ResponseStatusException,
        request: WebRequest
    ): ResponseEntity<CommonResponse<String>> {
        logger.error("handleResponseStatusException(request: ${request.contextPath}, status: ${exception.statusCode}, reason: ${exception.reason})")
        val transactionId = captureException(exception, request)
        return ResponseEntity(
            CommonResponse(
                code = exception.statusCode.value(),
                data = exception.reason ?: "HTTP STATUS: ${exception.statusCode.value()}",
                transactionId = transactionId,
            ),
            exception.statusCode,
        )
    }

    /**
     * Handles unhandled exceptions by logging the error details, capturing the exception for additional processing,
     * and returning a standardized error response encapsulated in a ResponseEntity.
     *
     * @param exception The exception thrown during execution that was not explicitly handled.
     * @param request The WebRequest object corresponding to the request that caused the exception.
     * @return A ResponseEntity containing a CommonResponse with a generic HTTP status code (500) and the exception message
     * or a default "INTERNAL SERVER ERROR" message if no exception message is available.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnhandledException(
        exception: Exception,
        request: WebRequest
    ): ResponseEntity<CommonResponse<String>> {
        logger.error("handleResponseStatusException(request: ${request.contextPath}, message: ${exception.message})")
        val transactionId = captureException(exception, request)
        return ResponseEntity(
            CommonResponse(
                code = 500,
                data = exception.message ?: "INTERNAL SERVER ERROR",
                transactionId = transactionId,
            ),
            HttpStatusCode.valueOf(500),
        )
    }

    private fun captureException(
        exception: Exception,
        request: WebRequest) = Sentry.captureException(exception, Hint().apply {
            this["contextPath"] = request.contextPath
    }).toString()
}