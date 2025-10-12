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

/**
 * API 호출을 담당하는 클래스입니다.
 * 주어진 ApiSettings에 따라 OkHttpClient를 설정하고, ApiItem 정보를 사용하여 실제 API 호출을 수행합니다.
 * @param settings API 호출에 사용될 전역 설정 (타임아웃, SSL 오류 무시 등).
 */
class ApiCaller(private val settings: ApiSettings) {

    // API 호출에 사용될 OkHttpClient 인스턴스
    private val client: OkHttpClient

    init {
        // OkHttpClient 빌더를 생성하고 설정을 적용합니다.
        val builder = OkHttpClient.Builder()
            .connectTimeout(settings.connectTimeout, TimeUnit.MILLISECONDS) // 연결 타임아웃 설정
            .readTimeout(settings.readTimeout, TimeUnit.MILLISECONDS)    // 읽기 타임아웃 설정
            .writeTimeout(settings.writeTimeout, TimeUnit.MILLISECONDS)   // 쓰기 타임아웃 설정
            .followRedirects(settings.followRedirects) // 리다이렉트 자동 처리 여부 설정

        // SSL 오류를 무시하도록 설정된 경우, 모든 인증서를 신뢰하는 TrustManager를 추가합니다.
        if (settings.ignoreSslErrors) {
            val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager") object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {} // 클라이언트 신뢰 여부 검사 (항상 통과)
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {} // 서버 신뢰 여부 검사 (항상 통과)
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf() // 허용된 인증서 발급자 배열 (비어 있음)
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager) // SSL 소켓 팩토리 설정
            builder.hostnameVerifier { _, _ -> true } // 호스트 이름 검증 (항상 통과)
        }

        // 쿠키 사용이 설정된 경우, JavaNetCookieJar를 사용하여 쿠키를 관리합니다.
        if (settings.useCookieJar) {
            builder.cookieJar(JavaNetCookieJar(java.net.CookieManager()))
        } else {
            // 쿠키를 사용하지 않는 경우, NO_COOKIES를 설정합니다.
            builder.cookieJar(CookieJar.NO_COOKIES)
        }

        // 설정이 완료된 빌더를 사용하여 OkHttpClient 인스턴스를 생성합니다.
        client = builder.build()
    }

    /**
     * 주어진 ApiItem 정보를 사용하여 비동기적으로 API를 호출합니다.
     * @param apiItem 호출할 API의 상세 정보 (URL, 메소드, 헤더, 본문 등).
     * @return API 호출 결과를 담은 ApiResult 객체 (Success 또는 Error).
     */
    suspend fun call(apiItem: ApiItem): ApiResult = withContext(Dispatchers.IO) {
        var result: ApiResult
        // API 호출 시간을 측정합니다.
        val time = measureTimeMillis {
            result = try {
                // 기본 URL이 설정되어 있고, API 아이템의 URL이 http로 시작하지 않는 경우, 두 URL을 조합합니다.
                val urlString = if (settings.baseUrl.isNotBlank() && !apiItem.url.startsWith("http")) {
                    settings.baseUrl + apiItem.url
                } else {
                    apiItem.url
                }

                // URL 문자열을 HttpUrl 객체로 변환하고 쿼리 파라미터를 추가합니다.
                val urlBuilder = urlString.toHttpUrlOrNull()?.newBuilder() ?: throw IllegalArgumentException("Invalid URL")
                apiItem.queryParams.forEach {
                    urlBuilder.addQueryParameter(it.first, it.second)
                }
                val finalUrl = urlBuilder.build()

                // Content-Type을 헤더 또는 bodyType에서 결정합니다.
                val contentType = apiItem.headers.find { it.first.equals("Content-Type", ignoreCase = true) }?.second ?: apiItem.bodyType

                // POST 또는 PUT 메소드인 경우, 요청 본문을 생성합니다.
                val requestBody = if (apiItem.method.equals("POST", ignoreCase = true) || apiItem.method.equals("PUT", ignoreCase = true)) {
                    apiItem.body.toRequestBody(contentType.toMediaTypeOrNull())
                } else {
                    null
                }

                // Request 빌더를 생성하고 URL과 메소드, 요청 본문을 설정합니다.
                val requestBuilder = Request.Builder()
                    .url(finalUrl)
                    .method(apiItem.method, requestBody)

                // 캐시를 사용하지 않도록 설정된 경우, CacheControl.FORCE_NETWORK를 적용합니다.
                if (settings.sendNoCache) {
                    requestBuilder.cacheControl(CacheControl.FORCE_NETWORK)
                }

                // 헤더를 요청에 추가합니다. Content-Type은 중복 추가되지 않도록 처리합니다.
                requestBuilder.apply {
                    apiItem.headers.forEach {
                        if (!it.first.equals("Content-Type", ignoreCase = true)) {
                            addHeader(it.first, it.second)
                        }
                    }
                    // Content-Type 헤더가 없고 요청 본문이 있는 경우, Content-Type을 추가합니다.
                    if (apiItem.headers.none { it.first.equals("Content-Type", ignoreCase = true) } && requestBody != null) {
                        addHeader("Content-Type", contentType)
                    }
                }
                
                val request = requestBuilder.build()

                // OkHttp 클라이언트를 사용하여 API를 호출하고 응답을 처리합니다.
                client.newCall(request).execute().use { response ->
                    val headers = response.headers.toMultimap().mapValues { it.value.joinToString() }
                    if (response.isSuccessful) {
                        // 응답이 성공적인 경우, Success 객체를 생성합니다.
                        ApiResult.Success(response.code, response.body?.string() ?: "Empty response", headers, 0L)
                    } else {
                        // 응답이 실패한 경우, Error 객체를 생성합니다.
                        ApiResult.Error(response.code, "${response.message} - ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                // 예외 발생 시 Error 객체를 생성합니다.
                ApiResult.Error(0, e.message ?: "Unknown error")
            }
        }
        // 결과가 Success인 경우, 측정된 시간을 포함하여 새로운 Success 객체를 반환합니다.
        if (result is ApiResult.Success) {
            (result as ApiResult.Success).copy(time = time)
        } else {
            result
        }
    }
}
