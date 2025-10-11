package org.parkjw.capycaller

import android.annotation.SuppressLint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.JavaNetCookieJar
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiResult
import org.parkjw.capycaller.data.ApiSettings
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.system.measureTimeMillis

class ApiCaller(private val settings: ApiSettings) {

    private val client: OkHttpClient

    init {
        val builder = OkHttpClient.Builder()
            .connectTimeout(settings.connectTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(settings.readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(settings.writeTimeout, TimeUnit.MILLISECONDS)
            .followRedirects(settings.followRedirects)

        if (settings.ignoreSslErrors) {
            val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager") object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        if (settings.useCookieJar) {
            builder.cookieJar(JavaNetCookieJar(java.net.CookieManager()))
        } else {
            builder.cookieJar(CookieJar.NO_COOKIES)
        }

        client = builder.build()
    }

    suspend fun call(apiItem: ApiItem): ApiResult = withContext(Dispatchers.IO) {
        var result: ApiResult
        val time = measureTimeMillis {
            result = try {
                val urlString = if (settings.baseUrl.isNotBlank() && !apiItem.url.startsWith("http")) {
                    settings.baseUrl + apiItem.url
                } else {
                    apiItem.url
                }

                val urlBuilder = urlString.toHttpUrlOrNull()?.newBuilder() ?: throw IllegalArgumentException("Invalid URL")
                apiItem.queryParams.forEach {
                    urlBuilder.addQueryParameter(it.first, it.second)
                }
                val finalUrl = urlBuilder.build()

                val contentType = apiItem.headers.find { it.first.equals("Content-Type", ignoreCase = true) }?.second ?: apiItem.bodyType

                val requestBody = if (apiItem.method.equals("POST", ignoreCase = true) || apiItem.method.equals("PUT", ignoreCase = true)) {
                    apiItem.body.toRequestBody(contentType.toMediaTypeOrNull())
                } else {
                    null
                }

                val requestBuilder = Request.Builder()
                    .url(finalUrl)
                    .method(apiItem.method, requestBody)

                if (settings.sendNoCache) {
                    requestBuilder.cacheControl(CacheControl.FORCE_NETWORK)
                }

                requestBuilder.apply {
                    apiItem.headers.forEach {
                        if (!it.first.equals("Content-Type", ignoreCase = true)) {
                            addHeader(it.first, it.second)
                        }
                    }
                    if (apiItem.headers.none { it.first.equals("Content-Type", ignoreCase = true) } && requestBody != null) {
                        addHeader("Content-Type", contentType)
                    }
                }
                
                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->
                    val headers = response.headers.toMultimap().mapValues { it.value.joinToString() }
                    if (response.isSuccessful) {
                        ApiResult.Success(response.code, response.body?.string() ?: "Empty response", headers, 0L)
                    } else {
                        ApiResult.Error(response.code, "${response.message} - ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                ApiResult.Error(0, e.message ?: "Unknown error")
            }
        }
        if (result is ApiResult.Success) {
            (result as ApiResult.Success).copy(time = time)
        } else {
            result
        }
    }
}
