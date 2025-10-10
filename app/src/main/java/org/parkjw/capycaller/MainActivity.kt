package org.parkjw.capycaller

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiResult
import org.parkjw.capycaller.ui.ApiEditScreen
import org.parkjw.capycaller.ui.ApiListScreen
import org.parkjw.capycaller.ui.SettingsScreen
import org.parkjw.capycaller.ui.SettingsViewModel
import org.parkjw.capycaller.ui.theme.CapycallerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val apiViewModel: ApiViewModel by viewModels()
    private val apiCaller by lazy { ApiCaller() }

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
        uri?.let { restoreData(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        askNotificationPermission()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
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
                            onExecuteApi = { executeApi(it) },
                            onCopyApi = { apiItem -> apiViewModel.copyApi(apiItem) },
                            onDeleteApi = { apiItem -> apiViewModel.deleteApi(apiItem) },
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("addApi") {
                        ApiEditScreen(
                            apiItem = null,
                            onSave = {
                                apiViewModel.addApi(it)
                                navController.popBackStack()
                            },
                            onExecute = { executeApi(it) }
                        )
                    }
                    composable("editApi/{apiId}") { backStackEntry ->
                        val apiId = backStackEntry.arguments?.getString("apiId")
                        val apiItem = apiViewModel.getApiItem(apiId)
                        ApiEditScreen(
                            apiItem = apiItem,
                            onSave = {
                                apiViewModel.updateApi(it)
                                navController.popBackStack()
                            },
                            onExecute = { executeApi(it) }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            settingsViewModel = settingsViewModel,
                            onBackupClick = { handleBackup() },
                            onRestoreClick = { handleRestore() }
                        )
                    }
                }
            }
        }
    }

    private fun handleBackup() {
        val timeStamp = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
        val fileName = "capyCaller-backup-$timeStamp.json"
        createDocumentLauncher.launch(fileName)
    }

    private fun handleRestore() {
        openDocumentLauncher.launch("application/json")
    }

    private fun backupData(uri: Uri) {
        try {
            val json = Gson().toJson(apiViewModel.apiItems.value)
            contentResolver.openOutputStream(uri)?.use {
                it.write(json.toByteArray())
            }
            Toast.makeText(this, "Backup successful", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreData(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (json != null) {
                val type = object : TypeToken<List<ApiItem>>() {}.type
                val restoredApis: List<ApiItem> = Gson().fromJson(json, type)
                apiViewModel.restoreApis(restoredApis)
                Toast.makeText(this, "Restore successful", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
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

    private fun executeApi(apiItem: ApiItem) {
        lifecycleScope.launch {
            val result = apiCaller.call(apiItem)
            val message = when (result) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> result.message
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}
