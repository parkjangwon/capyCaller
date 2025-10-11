
package org.parkjw.capycaller

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.ApiRepository
import org.parkjw.capycaller.ui.theme.CapyCallerTheme

@OptIn(ExperimentalMaterial3Api::class)
class MultiApiWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val maxSelectionCount = 20

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
                val selectedApiIds = remember { mutableStateOf(emptySet<String>()) }
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text("Select APIs (Max $maxSelectionCount)") },
                            actions = {
                                TextButton(onClick = {
                                    val allIds = apiItems.map { it.id }.take(maxSelectionCount)
                                    selectedApiIds.value = allIds.toSet()
                                }) {
                                    Text("Select All")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            saveSelection(selectedApiIds.value)
                        }) {
                            Icon(Icons.Filled.Done, contentDescription = "Save")
                        }
                    }
                ) { paddingValues ->
                    LazyColumn(modifier = Modifier.padding(paddingValues)) {
                        items(apiItems) { apiItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val currentSelection = selectedApiIds.value
                                        if (apiItem.id in currentSelection) {
                                            selectedApiIds.value = currentSelection - apiItem.id
                                        } else if (currentSelection.size < maxSelectionCount) {
                                            selectedApiIds.value = currentSelection + apiItem.id
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("You can select up to $maxSelectionCount APIs.")
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = apiItem.id in selectedApiIds.value,
                                    onCheckedChange = { isChecked ->
                                        val currentSelection = selectedApiIds.value
                                        if (isChecked) {
                                            if (currentSelection.size < maxSelectionCount) {
                                                selectedApiIds.value = currentSelection + apiItem.id
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("You can select up to $maxSelectionCount APIs.")
                                                }
                                            }
                                        } else {
                                            selectedApiIds.value = currentSelection - apiItem.id
                                        }
                                    }
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(apiItem.name)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveSelection(selectedApiIds: Set<String>) {
        val context = applicationContext
        val prefs = context.getSharedPreferences("widget_prefs", MODE_PRIVATE).edit()
        prefs.putString("widget_multi_$appWidgetId", selectedApiIds.joinToString(","))
        prefs.apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        ApiWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
