package org.parkjw.capycaller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiResult
import org.parkjw.capycaller.ui.ApiEditScreen
import org.parkjw.capycaller.ui.ApiListScreen
import org.parkjw.capycaller.ui.theme.CapycallerTheme

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        askNotificationPermission()

        setContent {
            CapycallerTheme {
                val navController = rememberNavController()
                val apiItems by apiViewModel.apiItems.collectAsState()

                NavHost(navController = navController, startDestination = "apiList") {
                    composable("apiList") {
                        ApiListScreen(
                            apiItems = apiItems,
                            onAddApi = { navController.navigate("addApi") },
                            onApiClick = { apiItem -> navController.navigate("editApi/${apiItem.id}") },
                            onExecuteApi = { executeApi(it) },
                            onEditApi = { apiItem -> navController.navigate("editApi/${apiItem.id}") },
                            onDeleteApi = { apiItem -> apiViewModel.deleteApi(apiItem) }
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
                }
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
