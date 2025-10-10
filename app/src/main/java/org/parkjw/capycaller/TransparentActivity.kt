package org.parkjw.capycaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.parkjw.capycaller.data.ApiRepository
import org.parkjw.capycaller.data.ApiResult

class TransparentActivity : ComponentActivity() {

    private val repository by lazy { ApiRepository(this) }
    private val apiCaller by lazy { ApiCaller() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.Main).launch {
            var title: String? = null
            var content: String? = null

            try {
                val data = intent.data
                if (data != null) {
                    val apiId = data.lastPathSegment
                    if (apiId != null) {
                        val apiItem = withContext(Dispatchers.IO) { repository.getApiItem(apiId) }
                        if (apiItem != null) {
                            val result = withContext(Dispatchers.IO) { apiCaller.call(apiItem) }
                            when (result) {
                                is ApiResult.Success -> {
                                    title = "'${apiItem.name}' executed"
                                    content = "The request was processed normally."
                                }
                                is ApiResult.Error -> {
                                    title = "Failed to execute '${apiItem.name}'"
                                    content = result.message
                                }
                            }
                        } else {
                            title = "Error"
                            content = "API not found."
                        }
                    }
                }
            } finally {
                if (title != null && content != null) {
                    NotificationHelper.showNotification(this@TransparentActivity, title, content)
                }
                // Disable exit animation and finish the activity
                overridePendingTransition(0, 0)
                finish()
            }
        }
    }
}
