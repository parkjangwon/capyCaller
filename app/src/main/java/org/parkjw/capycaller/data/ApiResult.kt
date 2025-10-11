package org.parkjw.capycaller.data

sealed class ApiResult {
    data class Success(
        val code: Int,
        val data: String,
        val headers: Map<String, String>,
        val time: Long
    ) : ApiResult()

    data class Error(
        val code: Int,
        val message: String
    ) : ApiResult()
}
