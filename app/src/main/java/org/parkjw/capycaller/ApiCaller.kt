package org.parkjw.capycaller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiResult

class ApiCaller {

    private val client = OkHttpClient()

    suspend fun call(apiItem: ApiItem): ApiResult = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = apiItem.url.toHttpUrlOrNull()?.newBuilder() ?: throw IllegalArgumentException("Invalid URL")
            apiItem.queryParams.forEach {
                urlBuilder.addQueryParameter(it.first, it.second)
            }
            val finalUrl = urlBuilder.build()

            // Determine Content-Type: Use header if present, otherwise use bodyType
            val contentType = apiItem.headers.find { it.first.equals("Content-Type", ignoreCase = true) }?.second ?: apiItem.bodyType

            val requestBody = if (apiItem.method.equals("POST", ignoreCase = true) || apiItem.method.equals("PUT", ignoreCase = true)) {
                apiItem.body.toRequestBody(contentType.toMediaTypeOrNull())
            } else {
                null
            }

            val request = Request.Builder()
                .url(finalUrl)
                .method(apiItem.method, requestBody)
                .apply {
                    apiItem.headers.forEach {
                        // Avoid duplicating Content-Type header
                        if (!it.first.equals("Content-Type", ignoreCase = true)) {
                            addHeader(it.first, it.second)
                        }
                    }
                    // Add the determined Content-Type header if it's not already added
                    if (apiItem.headers.none { it.first.equals("Content-Type", ignoreCase = true) } && requestBody != null) {
                        addHeader("Content-Type", contentType)
                    }
                }
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ApiResult.Success(response.code, response.body?.string() ?: "Empty response")
                } else {
                    ApiResult.Error(response.code, "${response.message} - ${response.body?.string()}")
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(0, e.message ?: "Unknown error")
        }
    }
}
