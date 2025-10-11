package org.parkjw.capycaller

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import org.parkjw.capycaller.ui.theme.CapycallerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val apiViewModel: ApiViewModel by viewModels()
    private lateinit var apiCaller: ApiCaller
    private var backPressedTime: Long = 0
    private lateinit var userDataStore: UserDataStore
    private var restoreUri by mutableStateOf<Uri?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is required to show execution status.", Toast.LENGTH_LONG).show()
        }
    }

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { backupData(it) }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        restoreUri = uri
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userDataStore = UserDataStore(application)

        lifecycleScope.launch {
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
            apiCaller = ApiCaller(apiSettings)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        })

        NotificationHelper.createNotificationChannel(this)
        askNotificationPermission()

        setContent {
            if (restoreUri != null) {
                AlertDialog(
                    onDismissRequest = { restoreUri = null },
                    title = { Text("Confirm Restore") },
                    text = { Text("Are you sure you want to restore? This will overwrite your current APIs and settings.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                restoreUri?.let { restoreData(it) }
                                restoreUri = null
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { restoreUri = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            val settingsViewModel: SettingsViewModel = viewModel()
            val apiSettingsViewModel: ApiSettingsViewModel = viewModel()
            val theme by settingsViewModel.theme.collectAsState()
            val useDarkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            CapycallerTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val apiItems by apiViewModel.apiItems.collectAsState()

                NavHost(navController = navController, startDestination = "apiList") {
                    composable("apiList") {
                        ApiListScreen(
                            apiItems = apiItems,
                            onAddApi = { navController.navigate("addApi") },
                            onApiClick = { apiItem -> navController.navigate("editApi/${apiItem.id}") },
                            onExecuteApis = { executeApis(it) },
                            onDeleteApis = { apiViewModel.deleteApis(it) },
                            onCopyApi = { apiViewModel.copyApi(it) },
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("addApi") {
                        var apiResult by remember { mutableStateOf<ApiResult?>(null) }
                        ApiEditScreen(
                            apiItem = null,
                            apiResult = apiResult,
                            onSave = { apiViewModel.addApi(it) },
                            onExecute = { apiItem ->
                                lifecycleScope.launch {
                                    apiResult = apiCaller.call(apiItem)
                                }
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("editApi/{apiId}") { backStackEntry ->
                        val apiId = backStackEntry.arguments?.getString("apiId")
                        val apiItem = apiViewModel.getApiItem(apiId)
                        var apiResult by remember { mutableStateOf<ApiResult?>(null) }
                        ApiEditScreen(
                            apiItem = apiItem,
                            apiResult = apiResult,
                            onSave = { apiViewModel.updateApi(it) },
                            onExecute = { apiItem ->
                                lifecycleScope.launch {
                                    apiResult = apiCaller.call(apiItem)
                                }
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            settingsViewModel = settingsViewModel,
                            apiSettingsViewModel = apiSettingsViewModel,
                            onBackupClick = { handleBackup() },
                            onRestoreClick = { openDocumentLauncher.launch("application/json") }
                        )
                    }
                }
            }
        }
    }

    private fun handleBackup() {
        lifecycleScope.launch {
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
                followRedirects = userDataStore.getFollowRedirects.first()
            )
            val backupData = BackupData(
                apiItems = apiViewModel.apiItems.value,
                settings = allSettings
            )

            val timeStamp = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
            val fileName = "capyCaller-backup-$timeStamp.json"
            createDocumentLauncher.launch(fileName)
        }
    }

    private fun backupData(uri: Uri) {
        lifecycleScope.launch {
            try {
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
                    followRedirects = userDataStore.getFollowRedirects.first()
                )
                val backupData = BackupData(
                    apiItems = apiViewModel.apiItems.value,
                    settings = allSettings
                )
                val json = Gson().toJson(backupData)
                contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                Toast.makeText(this@MainActivity, "Backup successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun restoreData(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (json != null) {
                    val type = object : TypeToken<BackupData>() {}.type
                    val backupData: BackupData = Gson().fromJson(json, type)
                    apiViewModel.restoreApis(backupData.apiItems)

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

                    Toast.makeText(this@MainActivity, "Restore successful", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun executeApis(apiItems: List<ApiItem>) {
        apiItems.forEach { apiItem ->
            lifecycleScope.launch {
                val result = apiCaller.call(apiItem)
                val (title, content) = when (result) {
                    is ApiResult.Success -> "Execution successful" to "API: ${apiItem.name}"
                    is ApiResult.Error -> "Execution failed" to "API: ${apiItem.name} (Code: ${result.code})"
                }
                NotificationHelper.showNotification(applicationContext, title, content)
            }
        }
    }
}
