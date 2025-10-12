package org.parkjw.capycaller.data

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.longPreferencesKey

// `preferencesDataStore`를 사용하여 "settings"라는 이름의 DataStore 인스턴스를 생성합니다.
// 이 인스턴스는 Application 컨텍스트를 통해 앱 전체에서 싱글톤으로 사용됩니다.
private val Application.dataStore by preferencesDataStore(name = "settings")

/**
 * Jetpack DataStore를 사용하여 사용자 설정을 비동기적으로, 그리고 트랜잭션을 보장하며 저장하고 불러오는 클래스입니다.
 * SharedPreferences의 단점(UI 스레드에서의 동기적 I/O 등)을 보완합니다.
 * @param application DataStore 인스턴스에 접근하기 위한 Application 컨텍스트.
 */
class UserDataStore(private val application: Application) {

    // DataStore에 데이터를 저장하고 불러올 때 사용할 키(Key)들을 정의합니다.
    // 각 키는 타입(string, boolean, long 등)과 고유한 이름을 가집니다.
    private val themeKey = stringPreferencesKey("theme")
    private val usePushNotificationsKey = booleanPreferencesKey("use_push_notifications")
    private val ignoreSslErrorsKey = booleanPreferencesKey("ignore_ssl_errors")
    private val connectTimeoutKey = longPreferencesKey("connect_timeout")
    private val readTimeoutKey = longPreferencesKey("read_timeout")
    private val writeTimeoutKey = longPreferencesKey("write_timeout")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val useCookieJarKey = booleanPreferencesKey("use_cookie_jar")
    private val sendNoCacheKey = booleanPreferencesKey("send_no_cache")
    private val followRedirectsKey = booleanPreferencesKey("follow_redirects")


    // 각 설정 값을 Flow<T> 형태로 외부에 노출합니다.
    // 이를 통해 데이터가 변경될 때마다 UI 등이 자동으로 업데이트되도록 반응형으로 구현할 수 있습니다.
    // `map` 연산자를 사용하여 DataStore에서 읽어온 Preferences 객체에서 원하는 키의 값을 추출합니다.
    // 값이 없을 경우(?:), 각 설정의 기본값을 반환합니다.
    val getTheme = application.dataStore.data.map { it[themeKey] ?: "System" }
    val getUsePushNotifications = application.dataStore.data.map { it[usePushNotificationsKey] ?: true }
    val getIgnoreSslErrors = application.dataStore.data.map { it[ignoreSslErrorsKey] ?: false }
    val getConnectTimeout = application.dataStore.data.map { it[connectTimeoutKey] ?: 60000L }
    val getReadTimeout = application.dataStore.data.map { it[readTimeoutKey] ?: 60000L }
    val getWriteTimeout = application.dataStore.data.map { it[writeTimeoutKey] ?: 60000L }
    val getBaseUrl = application.dataStore.data.map { it[baseUrlKey] ?: "" }
    val getUseCookieJar = application.dataStore.data.map { it[useCookieJarKey] ?: true }
    val getSendNoCache = application.dataStore.data.map { it[sendNoCacheKey] ?: true }
    val getFollowRedirects = application.dataStore.data.map { it[followRedirectsKey] ?: true }

    /**
     * 테마 설정을 DataStore에 저장합니다.
     * `suspend` 함수이므로 코루틴 내에서 호출되어야 합니다.
     * @param theme 저장할 테마 이름 (예: "Light", "Dark", "System").
     */
    suspend fun setTheme(theme: String) {
        application.dataStore.edit {
            it[themeKey] = theme
        }
    }

    /**
     * 푸시 알림 사용 여부를 DataStore에 저장합니다.
     * @param use 푸시 알림 사용 여부.
     */
    suspend fun setUsePushNotifications(use: Boolean) {
        application.dataStore.edit {
            it[usePushNotificationsKey] = use
        }
    }

    /**
     * SSL 오류 무시 여부를 DataStore에 저장합니다.
     * @param ignore SSL 오류 무시 여부.
     */
    suspend fun setIgnoreSslErrors(ignore: Boolean) {
        application.dataStore.edit {
            it[ignoreSslErrorsKey] = ignore
        }
    }

    /**
     * 연결 타임아웃 시간을 DataStore에 저장합니다.
     * @param timeout 연결 타임아웃 시간 (밀리초).
     */
    suspend fun setConnectTimeout(timeout: Long) {
        application.dataStore.edit {
            it[connectTimeoutKey] = timeout
        }
    }

    /**
     * 읽기 타임아웃 시간을 DataStore에 저장합니다.
     * @param timeout 읽기 타임아웃 시간 (밀리초).
     */
    suspend fun setReadTimeout(timeout: Long) {
        application.dataStore.edit {
            it[readTimeoutKey] = timeout
        }
    }

    /**
     * 쓰기 타임아웃 시간을 DataStore에 저장합니다.
     * @param timeout 쓰기 타임아웃 시간 (밀리초).
     */
    suspend fun setWriteTimeout(timeout: Long) {
        application.dataStore.edit {
            it[writeTimeoutKey] = timeout
        }
    }

    /**
     * 기본 URL을 DataStore에 저장합니다.
     * @param url 기본 URL 문자열.
     */
    suspend fun setBaseUrl(url: String) {
        application.dataStore.edit {
            it[baseUrlKey] = url
        }
    }

    /**
     * 쿠키 저장소 사용 여부를 DataStore에 저장합니다.
     * @param use 쿠키 저장소 사용 여부.
     */
    suspend fun setUseCookieJar(use: Boolean) {
        application.dataStore.edit {
            it[useCookieJarKey] = use
        }
    }

    /**
     * 'no-cache' 헤더 전송 여부를 DataStore에 저장합니다.
     * @param send 'no-cache' 헤더 전송 여부.
     */
    suspend fun setSendNoCache(send: Boolean) {
        application.dataStore.edit {
            it[sendNoCacheKey] = send
        }
    }

    /**
     * 리다이렉트 자동 처리 여부를 DataStore에 저장합니다.
     * @param follow 리다이렉트 자동 처리 여부.
     */
    suspend fun setFollowRedirects(follow: Boolean) {
        application.dataStore.edit {
            it[followRedirectsKey] = follow
        }
    }
}
