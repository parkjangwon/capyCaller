package org.parkjw.capycaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.ApiRepository
import org.parkjw.capycaller.data.ApiResult

class TransparentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val apiId: String? = if (intent.data?.scheme == "myapp" && intent.data?.host == "apicall") {
                    intent.data?.lastPathSegment
                } else {
                    intent.getStringExtra("api_id")
                }

                if (apiId != null) {
                    val repository = ApiRepository(application)
                    val apiItem = repository.getApiItems().find { it.id == apiId }

                    if (apiItem != null) {
                        val apiCaller = ApiCaller()
                        val result = apiCaller.call(apiItem)
                        val (title, content) = when (result) {
                            is ApiResult.Success -> {
                                "Execution successful" to "API: ${apiItem.name}"
                            }
                            is ApiResult.Error -> {
                                val contentMessage = if (result.code != 0) {
                                    "API: ${apiItem.name} (Code: ${result.code})"
                                } else {
                                    "API: ${apiItem.name}"
                                }
                                "Execution failed" to contentMessage
                            }
                        }
                        NotificationHelper.showNotification(applicationContext, title, content)
                    } else {
                        NotificationHelper.showNotification(applicationContext, "Execution failed", "API not found")
                    }
                }
            } finally {
                finish()
            }
        }
    }
}
