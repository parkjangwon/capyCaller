package org.parkjw.capycaller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.UserDataStore

class ApiSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userDataStore = UserDataStore(application)

    val ignoreSslErrors = userDataStore.getIgnoreSslErrors.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val connectTimeout = userDataStore.getConnectTimeout.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 60000L
    )

    val readTimeout = userDataStore.getReadTimeout.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 60000L
    )

    val writeTimeout = userDataStore.getWriteTimeout.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 60000L
    )

    val baseUrl = userDataStore.getBaseUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val useCookieJar = userDataStore.getUseCookieJar.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val sendNoCache = userDataStore.getSendNoCache.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val followRedirects = userDataStore.getFollowRedirects.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    fun setIgnoreSslErrors(ignore: Boolean) {
        viewModelScope.launch {
            userDataStore.setIgnoreSslErrors(ignore)
        }
    }

    fun setConnectTimeout(timeout: Long) {
        viewModelScope.launch {
            userDataStore.setConnectTimeout(timeout)
        }
    }

    fun setReadTimeout(timeout: Long) {
        viewModelScope.launch {
            userDataStore.setReadTimeout(timeout)
        }
    }

    fun setWriteTimeout(timeout: Long) {
        viewModelScope.launch {
            userDataStore.setWriteTimeout(timeout)
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            userDataStore.setBaseUrl(url)
        }
    }

    fun setUseCookieJar(use: Boolean) {
        viewModelScope.launch {
            userDataStore.setUseCookieJar(use)
        }
    }

    fun setSendNoCache(send: Boolean) {
        viewModelScope.launch {
            userDataStore.setSendNoCache(send)
        }
    }

    fun setFollowRedirects(follow: Boolean) {
        viewModelScope.launch {
            userDataStore.setFollowRedirects(follow)
        }
    }
}
