package org.parkjw.capycaller

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.parkjw.capycaller.data.AllSettings
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiResult
import org.parkjw.capycaller.data.ApiSettings
import org.parkjw.capycaller.data.BackupData
import org.parkjw.capycaller.data.UserDataStore
import org.parkjw.capycaller.ui.ApiEditScreen
import org.parkjw.capycaller.ui.ApiListScreen
import org.parkjw.capycaller.ui.ApiSettingsViewModel
import org.parkjw.capycaller.ui.SettingsScreen
import org.parkjw.capycaller.ui.SettingsViewModel
import org.parkjw.capycaller.ui.theme.CapyCallerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 앱의 메인 액티비티입니다.
 * Jetpack Compose를 사용하여 UI를 구성하고, Navigation Component를 통해 화면 간 이동을 관리합니다.
 * API 데이터 관리, 설정, 백업/복원 등의 핵심 로직을 처리합니다.
 */
class MainActivity : ComponentActivity() {

    // API 데이터 관리를 위한 ViewModel
    private val apiViewModel: ApiViewModel by viewModels()
    // 실제 API 호출을 수행하는 클래스
    private lateinit var apiCaller: ApiCaller
    // '뒤로가기' 버튼 두 번 클릭으로 앱을 종료하기 위한 시간 저장 변수
    private var backPressedTime: Long = 0
    // DataStore를 사용하여 사용자 설정을 영구적으로 저장
    private lateinit var userDataStore: UserDataStore
    // 복원할 파일의 URI를 저장하고, 복원 확인 대화상자를 띄우는 상태 변수
    private var restoreUri by mutableStateOf<Uri?>(null)

    // 알림 권한 요청을 위한 ActivityResultLauncher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // 권한이 거부되면 사용자에게 알림
            Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    // 파일 생성을 위한 ActivityResultLauncher (백업용)
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        // 사용자가 파일을 선택하면 해당 URI로 백업 데이터를 저장
        uri?.let { backupData(it) }
    }

    // 파일 선택을 위한 ActivityResultLauncher (복원용)
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // 사용자가 파일을 선택하면 restoreUri 상태를 업데이트하여 확인 대화상자를 표시
        restoreUri = uri
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UserDataStore 인스턴스 초기화
        userDataStore = UserDataStore(applicationContext)

        // 언어 설정 감시 및 적용
        lifecycleScope.launch {
            userDataStore.getLanguage.collect { language ->
                if (getLocaleFromLanguage(language).language != resources.configuration.locales[0].language) {
                    recreate()
                }
            }
        }

        // API 설정을 감시하고 변경될 때마다 ApiCaller를 다시 생성합니다.
        lifecycleScope.launch {
            val settingsFlows = listOf(
                userDataStore.getIgnoreSslErrors,
                userDataStore.getConnectTimeout,
                userDataStore.getReadTimeout,
                userDataStore.getWriteTimeout,
                userDataStore.getBaseUrl,
                userDataStore.getUseCookieJar,
                userDataStore.getSendNoCache,
                userDataStore.getFollowRedirects
            )
            combine(settingsFlows) { values ->
                ApiSettings(
                    ignoreSslErrors = values[0] as Boolean,
                    connectTimeout = values[1] as Long,
                    readTimeout = values[2] as Long,
                    writeTimeout = values[3] as Long,
                    baseUrl = values[4] as String,
                    useCookieJar = values[5] as Boolean,
                    sendNoCache = values[6] as Boolean,
                    followRedirects = values[7] as Boolean
                )
            }.collect { settings ->
                apiCaller = ApiCaller(settings)
            }
        }

        // 뒤로가기 버튼 콜백 등록
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 2초 안에 다시 누르면 앱 종료
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, R.string.press_again_to_exit, Toast.LENGTH_SHORT).show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        })

        // 알림 채널 생성 및 권한 요청
        NotificationHelper.createNotificationChannel(this)
        askNotificationPermission()

        // Jetpack Compose UI 설정
        setContent {
            // 복원 URI가 설정되면 확인 대화상자를 표시
            if (restoreUri != null) {
                AlertDialog(
                    onDismissRequest = { restoreUri = null },
                    title = { Text(stringResource(R.string.restore_confirm_title)) },
                    text = { Text(stringResource(R.string.restore_confirm_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                restoreUri?.let { restoreData(it) } // "확인" 클릭 시 복원 실행
                                restoreUri = null
                            }
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { restoreUri = null }) { // "취소" 클릭 시 대화상자 닫기
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            // 설정 ViewModel과 API 설정 ViewModel 가져오기
            val settingsViewModel: SettingsViewModel = viewModel()
            val apiSettingsViewModel: ApiSettingsViewModel = viewModel()
            // 설정에서 현재 테마 상태를 가져옴
            val theme by settingsViewModel.theme.collectAsState()
            // 테마 설정에 따라 다크 모드 사용 여부 결정
            val useDarkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme() // "System"일 경우 시스템 설정 따름
            }

            // 앱의 전체 테마 적용
            CapyCallerTheme(darkTheme = useDarkTheme) {
                // 내비게이션 컨트롤러 생성
                val navController = rememberNavController()
                // API 목록 상태를 수집
                val apiItems by apiViewModel.apiItems.collectAsState()

                // 내비게이션 호스트 설정
                NavHost(navController = navController, startDestination = "apiList") {
                    // API 목록 화면
                    composable("apiList") {
                        ApiListScreen(
                            apiItems = apiItems,
                            onAddApi = { navController.navigate("addApi") }, // API 추가 화면으로 이동
                            onApiClick = { apiItem -> navController.navigate("editApi/${apiItem.id}") }, // API 수정 화면으로 이동
                            onExecuteApis = { executeApis(it) }, // 선택된 API 실행
                            onDeleteApis = { apiViewModel.deleteApis(it) }, // 선택된 API 삭제
                            onCopyApi = { apiViewModel.copyApi(it) }, // API 복사
                            onSettingsClick = { navController.navigate("settings") } // 설정 화면으로 이동
                        )
                    }
                    // API 추가 화면
                    composable("addApi") {
                        var apiResult by remember { mutableStateOf<ApiResult?>(null) }
                        ApiEditScreen(
                            apiItem = null, // 새 API이므로 null 전달
                            apiResult = apiResult,
                            onSave = { apiViewModel.addApi(it) }, // API 저장
                            onExecute = { apiItem ->
                                lifecycleScope.launch { apiResult = apiCaller.call(apiItem) } // API 테스트 실행
                            },
                            onNavigateBack = { navController.popBackStack() } // 뒤로가기
                        )
                    }
                    // API 수정 화면
                    composable("editApi/{apiId}") { backStackEntry ->
                        val apiId = backStackEntry.arguments?.getString("apiId")
                        val apiItem = apiViewModel.getApiItem(apiId)
                        var apiResult by remember { mutableStateOf<ApiResult?>(null) }
                        ApiEditScreen(
                            apiItem = apiItem, // 수정할 API 정보 전달
                            apiResult = apiResult,
                            onSave = { apiViewModel.updateApi(it) }, // API 업데이트
                            onExecute = { apiItem ->
                                lifecycleScope.launch { apiResult = apiCaller.call(apiItem) } // API 테스트 실행
                            },
                            onNavigateBack = { navController.popBackStack() } // 뒤로가기
                        )
                    }
                    // 설정 화면
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            settingsViewModel = settingsViewModel,
                            apiSettingsViewModel = apiSettingsViewModel,
                            onBackupClick = { handleBackup() }, // 백업 시작
                            onRestoreClick = { openDocumentLauncher.launch("application/json") } // 복원 파일 선택
                        )
                    }
                }
            }
        }
    }

    /**
     * 백업 프로세스를 시작합니다.
     * 현재 시간으로 파일 이름을 생성하고, 파일 생성을 위한 `createDocumentLauncher`를 실행합니다.
     */
    private fun handleBackup() {
        lifecycleScope.launch {
            // 현재 시간으로 파일명 생성 (예: CapyCaller-backup-202310271530.json)
            val timeStamp = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
            val fileName = "CapyCaller-backup-$timeStamp.json"
            // 파일 생성 ActivityResultLauncher 실행
            createDocumentLauncher.launch(fileName)
        }
    }

    /**
     * 지정된 URI에 현재 앱 데이터(API 목록 및 설정)를 JSON 형식으로 백업합니다.
     * @param uri 데이터를 저장할 파일의 URI
     */
    private fun backupData(uri: Uri) {
        lifecycleScope.launch {
            try {
                // DataStore에서 모든 설정을 가져옵니다.
                val allSettings = AllSettings(
                    theme = userDataStore.getTheme.first(),
                    usePushNotifications = userDataStore.getUsePushNotifications.first(),
                    ignoreSslErrors = userDataStore.getIgnoreSslErrors.first(),
                    connectTimeout = userDataStore.getConnectTimeout.first(),
                    readTimeout = userDataStore.getReadTimeout.first(),
                    writeTimeout = userDataStore.getWriteTimeout.first(),
                    baseUrl = userDataStore.getBaseUrl.first(),
                    useCookieJar = userDataStore.getUseCookieJar.first(),
                    sendNoCache = userDataStore.getSendNoCache.first(),
                    followRedirects = userDataStore.getFollowRedirects.first(),
                    language = userDataStore.getLanguage.first()
                )
                // API 목록과 설정을 포함하는 BackupData 객체 생성
                val backupData = BackupData(
                    apiItems = apiViewModel.apiItems.value,
                    settings = allSettings
                )
                // Gson을 사용하여 BackupData 객체를 JSON 문자열로 변환
                val json = Gson().toJson(backupData)
                // ContentResolver를 통해 파일에 JSON 데이터를 씀
                contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                Toast.makeText(this@MainActivity, R.string.backup_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "${getString(R.string.backup_fail)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 지정된 URI의 백업 파일로부터 데이터를 복원합니다.
     * @param uri 복원할 데이터가 있는 파일의 URI
     */
    private fun restoreData(uri: Uri) {
        lifecycleScope.launch {
            try {
                // ContentResolver를 통해 파일에서 JSON 데이터를 읽음
                val json = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (json != null) {
                    val type = object : TypeToken<BackupData>() {}.type
                    // Gson을 사용하여 JSON 문자열을 BackupData 객체로 변환
                    val backupData: BackupData = Gson().fromJson(json, type)
                    // ViewModel을 통해 API 목록 복원
                    apiViewModel.restoreApis(backupData.apiItems)

                    // DataStore에 각 설정 항목을 저장
                    userDataStore.setTheme(backupData.settings.theme)
                    userDataStore.setUsePushNotifications(backupData.settings.usePushNotifications)
                    userDataStore.setIgnoreSslErrors(backupData.settings.ignoreSslErrors)
                    userDataStore.setConnectTimeout(backupData.settings.connectTimeout)
                    userDataStore.setReadTimeout(backupData.settings.readTimeout)
                    userDataStore.setWriteTimeout(backupData.settings.writeTimeout)
                    userDataStore.setBaseUrl(backupData.settings.baseUrl)
                    userDataStore.setUseCookieJar(backupData.settings.useCookieJar)
                    userDataStore.setSendNoCache(backupData.settings.sendNoCache)
                    userDataStore.setFollowRedirects(backupData.settings.followRedirects)
                    userDataStore.setLanguage(backupData.settings.language)

                    Toast.makeText(this@MainActivity, R.string.restore_success, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "${getString(R.string.restore_fail)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Android 13 (TIRAMISU) 이상에서 알림 권한을 요청합니다.
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 이미 권한이 부여되었는지 확인
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                // 권한이 없으면 요청 런처를 실행
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 주어진 API 아이템 목록을 실행하고 결과를 알림으로 표시합니다.
     * @param apiItems 실행할 API 아이템의 리스트
     */
    private fun executeApis(apiItems: List<ApiItem>) {
        apiItems.forEach { apiItem ->
            lifecycleScope.launch {
                // ApiCaller를 사용하여 API 호출
                val result = apiCaller.call(apiItem)
                // 호출 결과에 따라 알림 제목과 내용을 결정
                val (title, content) = when (result) {
                    is ApiResult.Success -> getString(R.string.execution_success) to "API: ${apiItem.name}"
                    is ApiResult.Error -> getString(R.string.execution_fail) to "API: ${apiItem.name} (코드: ${result.code})"
                }
                // NotificationHelper를 사용하여 알림 표시
                NotificationHelper.showNotification(applicationContext, title, content)
            }
        }
    }

    companion object {
        fun getLocaleFromLanguage(language: String): Locale {
            return when (language) {
                "Korean (한국어)" -> Locale("ko")
                "Japanese (日本語)" -> Locale("ja")
                "Simplified Chinese (简体中文)" -> Locale.SIMPLIFIED_CHINESE
                "Traditional Chinese (繁體中文)" -> Locale.TRADITIONAL_CHINESE
                "Spanish (Español)" -> Locale("es")
                "French (Français)" -> Locale("fr")
                "German (Deutsch)" -> Locale("de")
                "Russian (Русский)" -> Locale("ru")
                "Portuguese (Português)" -> Locale("pt")
                else -> Locale.ENGLISH
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking { UserDataStore(newBase).getLanguage.first() }
        val locale = getLocaleFromLanguage(language)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}
