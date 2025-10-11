package org.parkjw.capycaller.data

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.longPreferencesKey

private val Application.dataStore by preferencesDataStore(name = "settings")

class UserDataStore(private val application: Application) {

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

    suspend fun setTheme(theme: String) {
        application.dataStore.edit {
            it[themeKey] = theme
        }
    }

    suspend fun setUsePushNotifications(use: Boolean) {
        application.dataStore.edit {
            it[usePushNotificationsKey] = use
        }
    }

    suspend fun setIgnoreSslErrors(ignore: Boolean) {
        application.dataStore.edit {
            it[ignoreSslErrorsKey] = ignore
        }
    }

    suspend fun setConnectTimeout(timeout: Long) {
        application.dataStore.edit {
            it[connectTimeoutKey] = timeout
        }
    }

    suspend fun setReadTimeout(timeout: Long) {
        application.dataStore.edit {
            it[readTimeoutKey] = timeout
        }
    }

    suspend fun setWriteTimeout(timeout: Long) {
        application.dataStore.edit {
            it[writeTimeoutKey] = timeout
        }
    }

    suspend fun setBaseUrl(url: String) {
        application.dataStore.edit {
            it[baseUrlKey] = url
        }
    }

    suspend fun setUseCookieJar(use: Boolean) {
        application.dataStore.edit {
            it[useCookieJarKey] = use
        }
    }

    suspend fun setSendNoCache(send: Boolean) {
        application.dataStore.edit {
            it[sendNoCacheKey] = send
        }
    }

    suspend fun setFollowRedirects(follow: Boolean) {
        application.dataStore.edit {
            it[followRedirectsKey] = follow
        }
    }
}
