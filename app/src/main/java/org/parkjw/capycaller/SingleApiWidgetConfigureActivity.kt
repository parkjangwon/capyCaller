
package org.parkjw.capycaller

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiRepository
import org.parkjw.capycaller.ui.theme.CapyCallerTheme

@OptIn(ExperimentalMaterial3Api::class)
class SingleApiWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        intent.extras?.let {
            appWidgetId = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = ApiRepository(this)
        val apiItems = repository.getApiItems()

        setContent {
            CapyCallerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Select an API") })
                    }
                ) { paddingValues ->
                    LazyColumn(modifier = Modifier.padding(paddingValues)) {
                        items(apiItems) { apiItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { selectApi(apiItem) }
                            ) {
                                Text(
                                    text = apiItem.name,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun selectApi(apiItem: ApiItem) {
        val context = applicationContext
        val prefs = context.getSharedPreferences("widget_prefs", MODE_PRIVATE).edit()
        prefs.putString("widget_${appWidgetId}_id", apiItem.id)
        prefs.putString("widget_${appWidgetId}_name", apiItem.name)
        prefs.apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.single_api_widget)
        views.setTextViewText(R.id.widget_button, apiItem.name)

        val intent = Intent(context, TransparentActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("myapp://apicall/${apiItem.id}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
