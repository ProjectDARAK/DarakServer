package camp.cultr.darakserver.util.filter

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.contracts.contract
import kotlin.jvm.Throws

/**
 * Represents the parameters required for constructing a `ResponseStatusException`.
 *
 * This data class encapsulates the HTTP status and an optional message, which are used
 * to define the response details when an exception of type `ResponseStatusException` is thrown.
 *
 * @property status The HTTP status to associate with the exception. It indicates the
 * response code corresponding to the exception.
 * @property message An optional message providing additional details about the exception.
 */
data class ResponseStatusExceptionParams(val status: HttpStatus, val message: String? = null)

/**
 * Throws a `ResponseStatusException` if the given condition is not met.
 *
 * This method evaluates the provided condition and, if it evaluates to `false`,
 * it constructs a `ResponseStatusException` using the `lazyMessage` function's result
 * and throws it. The lazy evaluation ensures that message generation is deferred until
 * needed, improving efficiency when the condition is `true`.
 *
 * @param condition The boolean condition to check. If `false`, an exception is thrown.
 * @param lazyMessage A lambda returning the parameters for the `ResponseStatusException`
 * in case the condition is not met. The result includes the HTTP status and a detailed message.
 * @throws ResponseStatusException if the condition evaluates to `false`.
 */
@Throws(ResponseStatusException::class)
public inline fun requireOrThrowResponseStatusException(condition: Boolean, lazyMessage: () -> ResponseStatusExceptionParams) {
    if(!condition) {
        val message = lazyMessage()
        throw ResponseStatusException(message.status, message.message)
    }
}