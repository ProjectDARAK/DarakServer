package camp.cultr.darakserver.dto

data class CommonResponse<T>(
    val code: Int = 200,
    val data: T? = null,
    val transactionId: String? = null,
)

data class PaginationResponse<T>(
    val code: Int,
    val data: List<T>,
    val totalPage: Long,
    val currentPage: Int,
)