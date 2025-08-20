package camp.cultr.darakserver.dto

/**
 * Represents a common structure for API responses.
 *
 * This class is a generic response wrapper that can be used across API endpoints.
 * It encapsulates the response code, the actual response data, and a transaction ID
 * for tracking the specific request or operation.
 *
 * @param T The type of the response data that this class will encapsulate.
 * @property code The HTTP-like code representing the result of the operation (e.g., 200 for success).
 * @property data The payload or content of the response. It is nullable to accommodate responses without a body.
 * @property transactionId An optional identifier for tracking the transaction or operation associated with the response.
 */
data class CommonResponse<T>(
    val code: Int = 200,
    val data: T? = null,
    val transactionId: String? = null,
)

/**
 * Represents a paginated response for API results.
 *
 * This data class provides a structure to return paginated data from the server.
 * It includes information about the result code, the current page of data,
 * the total number of pages available, and the list of data items.
 *
 * @param T The type of the data elements contained in the response.
 * @property code The HTTP-like code indicating the status of the response (e.g., 200 for success).
 * @property data The list of data items for the current page.
 * @property totalPage The total number of pages available for the requested data.
 * @property currentPage The current page index in the pagination.
 */
data class PaginationResponse<T>(
    val code: Int,
    val data: List<T>,
    val totalPage: Long,
    val currentPage: Int,
)