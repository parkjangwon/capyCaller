package org.parkjw.capycaller.data

/**
 * A data class to hold all data for backup and restore.
 */
data class BackupData(
    val apiItems: List<ApiItem>,
    val settings: AllSettings
)

/**
 * A data class to hold all user-configurable settings.
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
