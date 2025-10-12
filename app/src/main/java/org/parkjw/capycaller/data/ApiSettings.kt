package org.parkjw.capycaller.data

/**
 * API 호출과 관련된 전역 설정을 담는 데이터 클래스입니다.
 * 이 설정들은 ApiCaller가 OkHttpClient를 초기화할 때 사용됩니다.
 *
 * @property ignoreSslErrors SSL/TLS 인증서 오류를 무시할지 여부. true이면 자체 서명된 인증서 등도 허용됩니다.
 * @property connectTimeout 서버와의 연결을 시도할 때의 타임아웃 시간 (밀리초 단위).
 * @property readTimeout 연결된 서버로부터 데이터를 읽어올 때의 타임아웃 시간 (밀리초 단위).
 * @property writeTimeout 연결된 서버로 데이터를 보낼 때의 타임아웃 시간 (밀리초 단위).
 * @property baseUrl 모든 API 호출의 기본이 되는 URL. 각 ApiItem의 URL이 상대 경로일 경우 이 baseUrl에 합쳐집니다.
 * @property useCookieJar HTTP 요청 시 쿠키를 자동으로 관리(저장 및 전송)할지 여부.
 * @property sendNoCache 모든 요청에 'Cache-Control: no-cache' 헤더를 추가하여 캐시된 응답을 사용하지 않도록 강제할지 여부.
 * @property followRedirects 서버에서 3xx 리다이렉트 응답을 받았을 때 자동으로 다음 URL로 요청을 보낼지 여부.
 */
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
