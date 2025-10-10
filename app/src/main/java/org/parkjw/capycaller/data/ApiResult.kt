package org.parkjw.capycaller.data

sealed class ApiResult {
    data class Success(val data: String) : ApiResult()
    data class Error(val message: String) : ApiResult()
}
