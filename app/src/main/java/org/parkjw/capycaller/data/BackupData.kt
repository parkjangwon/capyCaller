package org.parkjw.capycaller.data

/**
 * 앱의 백업 및 복원을 위해 모든 관련 데이터를 하나로 묶는 데이터 클래스입니다.
 * 이 객체는 JSON으로 변환되어 파일로 저장되거나, 파일로부터 읽어와 앱 상태를 복원하는 데 사용됩니다.
 *
 * @property apiItems 사용자가 생성한 모든 API 아이템의 목록입니다.
 * @property settings 사용자가 설정한 모든 앱 설정 값입니다.
 */
data class BackupData(
    val apiItems: List<ApiItem>,
    val settings: AllSettings
)

/**
 * 사용자가 앱 내에서 설정할 수 있는 모든 항목을 포함하는 데이터 클래스입니다.
 * 백업/복원 시 `BackupData`에 포함되어 전체 설정 상태를 한 번에 관리할 수 있게 합니다.
 *
 * @property theme 앱의 표시 테마 설정 (예: "Light", "Dark", "System").
 * @property usePushNotifications API 실행 결과를 푸시 알림으로 받을지 여부.
 * @property ignoreSslErrors SSL/TLS 인증서 오류를 무시할지 여부.
 * @property connectTimeout 연결 타임아웃 시간 (밀리초).
 * @property readTimeout 읽기 타임아웃 시간 (밀리초).
 * @property writeTimeout 쓰기 타임아웃 시간 (밀리초).
 * @property baseUrl API 호출의 기본 URL.
 * @property useCookieJar 쿠키 자동 관리(Cookie Jar) 사용 여부.
 * @property sendNoCache 캐시 비활성화(`no-cache`) 요청 헤더 전송 여부.
 * @property followRedirects 리다이렉트 자동 이동 여부.
 */
data class AllSettings(
    val theme: String,
    val usePushNotifications: Boolean,
    val ignoreSslErrors: Boolean,
    val connectTimeout: Long,
    val readTimeout: Long,
    val writeTimeout: Long,
    val baseUrl: String,
    val useCookieJar: Boolean,
    val sendNoCache: Boolean,
    val followRedirects: Boolean
)
