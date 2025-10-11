package org.parkjw.capycaller.data

data class ApiSettings(
    val ignoreSslErrors: Boolean = false,
    val connectTimeout: Long = 60000L,
    val readTimeout: Long = 60000L,
    val writeTimeout: Long = 60000L,
    val baseUrl: String = "",
    val useCookieJar: Boolean = true,
    val sendNoCache: Boolean = true,
    val followRedirects: Boolean = true
)
