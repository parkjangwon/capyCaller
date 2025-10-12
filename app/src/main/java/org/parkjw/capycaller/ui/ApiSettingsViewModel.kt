package org.parkjw.capycaller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.UserDataStore

/**
 * API 관련 전역 설정(네트워크, 타임아웃 등)을 관리하는 ViewModel입니다.
 * `UserDataStore`와 상호작용하여 설정을 불러오고 저장합니다.
 */
class ApiSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userDataStore = UserDataStore(application)

    // UserDataStore의 Flow를 StateFlow로 변환하여 UI에 노출합니다.
    // stateIn 연산자를 사용하여 Flow를 StateFlow로 만듭니다.
    // - viewModelScope: ViewModel의 생명주기를 따르는 코루틴 스코프입니다.
    // - SharingStarted.WhileSubscribed(5000): 구독자가 있는 동안만 Flow를 활성 상태로 유지하고, 마지막 구독자가 사라진 후 5초의 유예 시간을 줍니다.
    // - initialValue: StateFlow의 초기값입니다.

    /** SSL 인증서 오류 무시 여부 설정 값 */
    val ignoreSslErrors = userDataStore.getIgnoreSslErrors.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    /** 연결 타임아웃 시간 설정 값 (밀리초) */
    val connectTimeout = userDataStore.getConnectTimeout.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 60000L
    )

    /** 읽기 타임아웃 시간 설정 값 (밀리초) */
    val readTimeout = userDataStore.getReadTimeout.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 60000L
    )

    /** 쓰기 타임아웃 시간 설정 값 (밀리초) */
    val writeTimeout = userDataStore.getWriteTimeout.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 60000L
    )

    /** 기본 URL 설정 값 */
    val baseUrl = userDataStore.getBaseUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    /** 쿠키 저장소 사용 여부 설정 값 */
    val useCookieJar = userDataStore.getUseCookieJar.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    /** `no-cache` 헤더 전송 여부 설정 값 */
    val sendNoCache = userDataStore.getSendNoCache.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    /** 리다이렉트 자동 처리 여부 설정 값 */
    val followRedirects = userDataStore.getFollowRedirects.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    /** SSL 오류 무시 설정을 저장합니다. */
    fun setIgnoreSslErrors(ignore: Boolean) {
        viewModelScope.launch {
            userDataStore.setIgnoreSslErrors(ignore)
        }
    }

    /** 연결 타임아웃 설정을 저장합니다. */
    fun setConnectTimeout(timeout: Long) {
        viewModelScope.launch {
            userDataStore.setConnectTimeout(timeout)
        }
    }

    /** 읽기 타임아웃 설정을 저장합니다. */
    fun setReadTimeout(timeout: Long) {
        viewModelScope.launch {
            userDataStore.setReadTimeout(timeout)
        }
    }

    /** 쓰기 타임아웃 설정을 저장합니다. */
    fun setWriteTimeout(timeout: Long) {
        viewModelScope.launch {
            userDataStore.setWriteTimeout(timeout)
        }
    }

    /** 기본 URL 설정을 저장합니다. */
    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            userDataStore.setBaseUrl(url)
        }
    }

    /** 쿠키 저장소 사용 여부 설정을 저장합니다. */
    fun setUseCookieJar(use: Boolean) {
        viewModelScope.launch {
            userDataStore.setUseCookieJar(use)
        }
    }

    /** `no-cache` 헤더 전송 여부 설정을 저장합니다. */
    fun setSendNoCache(send: Boolean) {
        viewModelScope.launch {
            userDataStore.setSendNoCache(send)
        }
    }

    /** 리다이렉트 자동 처리 여부 설정을 저장합니다. */
    fun setFollowRedirects(follow: Boolean) {
        viewModelScope.launch {
            userDataStore.setFollowRedirects(follow)
        }
    }
}
