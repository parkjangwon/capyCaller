package org.parkjw.capycaller

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.parkjw.capycaller.data.ApiRepository
import org.parkjw.capycaller.data.ApiResult
import org.parkjw.capycaller.data.ApiSettings
import org.parkjw.capycaller.data.UserDataStore

/**
 * UI가 없는 투명한 액티비티입니다.
 * 앱 바로가기(Shortcut)나 위젯에서 API 호출을 트리거했을 때 실행됩니다.
 * 실제 API 호출을 수행하고, 결과를 사용자에게 알림으로 보여준 뒤 즉시 종료됩니다.
 */
class TransparentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 액티비티의 생명주기 스코프 내에서 코루틴을 실행합니다.
        lifecycleScope.launch {
            try {
                // UserDataStore를 통해 사용자 설정을 가져옵니다.
                val userDataStore = UserDataStore(applicationContext)
                val usePushNotifications = userDataStore.getUsePushNotifications.first() // 푸시 알림 사용 여부 확인

                // 인텐트 데이터를 분석하여 호출해야 할 API의 ID를 추출합니다.
                // "myapp://apicall/{apiId}" 형태의 URI 또는 인텐트 extra("api_id")에서 ID를 찾습니다.
                val apiId: String? = if (intent.data?.scheme == "myapp" && intent.data?.host == "apicall") {
                    intent.data?.lastPathSegment
                } else {
                    intent.getStringExtra("api_id")
                }

                if (apiId != null) {
                    // 리포지토리에서 전체 API 목록을 가져옵니다.
                    val repository = ApiRepository(applicationContext)
                    // 추출한 ID와 일치하는 ApiItem을 찾습니다.
                    val apiItem = repository.getApiItems().find { it.id == apiId }

                    if (apiItem != null) {
                        // DataStore에서 API 설정을 비동기적으로 불러옵니다.
                        val apiSettings = ApiSettings(
                            ignoreSslErrors = userDataStore.getIgnoreSslErrors.first(),
                            connectTimeout = userDataStore.getConnectTimeout.first(),
                            readTimeout = userDataStore.getReadTimeout.first(),
                            writeTimeout = userDataStore.getWriteTimeout.first(),
                            baseUrl = userDataStore.getBaseUrl.first(),
                            useCookieJar = userDataStore.getUseCookieJar.first(),
                            sendNoCache = userDataStore.getSendNoCache.first(),
                            followRedirects = userDataStore.getFollowRedirects.first()
                        )
                        // 불러온 설정으로 ApiCaller 인스턴스 생성
                        val apiCaller = ApiCaller(apiSettings)
                        // API 호출을 실행합니다.
                        val result = apiCaller.call(apiItem)

                        // 푸시 알림 사용이 활성화된 경우에만 알림을 표시합니다.
                        if (usePushNotifications) {
                            val (title, content) = when (result) {
                                is ApiResult.Success -> {
                                    getString(R.string.execution_success) to "API: ${apiItem.name}"
                                }
                                is ApiResult.Error -> {
                                    val contentMessage = if (result.code != 0) {
                                        "API: ${apiItem.name} (코드: ${result.code})"
                                    } else {
                                        "API: ${apiItem.name}"
                                    }
                                    getString(R.string.execution_fail) to contentMessage
                                }
                            }
                            NotificationHelper.showNotification(applicationContext, title, content)
                        }
                    } else {
                        // 해당 ID의 API를 찾지 못한 경우
                        if (usePushNotifications) {
                            NotificationHelper.showNotification(
                                applicationContext,
                                getString(R.string.execution_fail),
                                getString(R.string.api_not_found)
                            )
                        }
                    }
                }
            } finally {
                // 모든 작업이 끝나면 (성공, 실패, 예외 발생 여부와 관계없이) 액티비티를 즉시 종료하여
                // 사용자에게 빈 화면이 보이지 않도록 합니다.
                finish()
            }
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val language = runBlocking { UserDataStore(newBase).getLanguage.first() }
        val locale = MainActivity.getLocaleFromLanguage(language)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}