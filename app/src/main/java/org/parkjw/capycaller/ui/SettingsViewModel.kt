package org.parkjw.capycaller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.UserDataStore

/**
 * 일반 앱 설정(테마, 알림 등)을 관리하는 ViewModel입니다.
 * `UserDataStore`와 상호작용하여 설정을 UI에 제공하고, 사용자 변경사항을 저장합니다.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userDataStore = UserDataStore(application)

    /**
     * 현재 설정된 테마 값을 UI에 노출하는 StateFlow 입니다.
     * `stateIn`을 사용하여 DataStore의 Flow를 StateFlow로 변환, UI가 상태를 구독하고 자동으로 업데이트되도록 합니다.
     */
    val theme: StateFlow<String> = userDataStore.getTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // 구독자가 있을 때만 활성화
        initialValue = "System" // 초기값
    )

    /**
     * 사용자가 선택한 테마를 DataStore에 저장합니다.
     * @param theme 저장할 테마 이름 (예: "System", "Light", "Dark").
     */
    fun setTheme(theme: String) {
        viewModelScope.launch {
            userDataStore.setTheme(theme)
        }
    }

    /**
     * 푸시 알림 사용 여부 값을 UI에 노출하는 StateFlow 입니다.
     */
    val usePushNotifications: StateFlow<Boolean> = userDataStore.getUsePushNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * 푸시 알림 사용 여부를 DataStore에 저장합니다.
     * @param usePushNotifications 푸시 알림을 사용할지 여부.
     */
    fun setUsePushNotifications(usePushNotifications: Boolean) {
        viewModelScope.launch {
            userDataStore.setUsePushNotifications(usePushNotifications)
        }
    }
}
