package org.parkjw.capycaller

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.parkjw.capycaller.data.ApiRepository
import org.parkjw.capycaller.data.UserDataStore
import org.parkjw.capycaller.ui.theme.CapyCallerTheme

/**
 * 다중 API 위젯을 설정하는 액티비티입니다.
 * 사용자가 홈 화면에 위젯을 추가할 때 실행되며, 위젯에 표시할 API 목록을 선택할 수 있게 합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
class MultiApiWidgetConfigureActivity : ComponentActivity() {

    // 설정 중인 위젯의 고유 ID. 이 ID로 위젯을 식별하고 관리합니다.
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    // 위젯에 추가할 수 있는 API의 최대 개수
    private val maxSelectionCount = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 사용자가 설정을 완료하지 않고 뒤로가기 등으로 종료할 경우를 대비해 기본 결과값을 CANCELED로 설정합니다.
        setResult(RESULT_CANCELED)

        // 인텐트에서 위젯 ID를 가져옵니다.
        intent.extras?.let {
            appWidgetId = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // 유효한 위젯 ID가 없으면 액티비티를 즉시 종료합니다.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // 리포지토리에서 전체 API 목록을 가져옵니다.
        val repository = ApiRepository(this)
        val apiItems = repository.getApiItems()

        // Jetpack Compose로 UI를 구성합니다.
        setContent {
            CapyCallerTheme {
                // 선택된 API들의 ID를 저장하는 상태 변수입니다.
                val selectedApiIds = remember { mutableStateOf(emptySet<String>()) }
                // 코루틴 스코프 (Snackbar 표시에 사용)
                val scope = rememberCoroutineScope()
                // Snackbar의 상태를 관리하는 호스트
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.select_apis_max, maxSelectionCount)) },
                            actions = {
                                // "전체 선택" 버튼
                                TextButton(onClick = {
                                    // 최대 선택 개수만큼 모든 API의 ID를 가져와 선택 상태로 설정합니다.
                                    val allIds = apiItems.map { it.id }.take(maxSelectionCount)
                                    selectedApiIds.value = allIds.toSet()
                                }) {
                                    Text(stringResource(R.string.select_all))
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        // 저장 버튼 (FAB)
                        FloatingActionButton(onClick = {
                            // 현재 선택된 API ID 목록을 저장합니다.
                            saveSelection(selectedApiIds.value)
                        }) {
                            Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.save))
                        }
                    }
                ) { paddingValues ->
                    // 스크롤 가능한 API 목록
                    LazyColumn(modifier = Modifier.padding(paddingValues)) {
                        items(apiItems) { apiItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { // 행 전체를 클릭 가능하게 만듭니다.
                                        val currentSelection = selectedApiIds.value
                                        if (apiItem.id in currentSelection) {
                                            // 이미 선택된 항목이면 선택 해제합니다.
                                            selectedApiIds.value = currentSelection - apiItem.id
                                        } else if (currentSelection.size < maxSelectionCount) {
                                            // 최대 선택 개수에 도달하지 않았으면 선택 목록에 추가합니다.
                                            selectedApiIds.value = currentSelection + apiItem.id
                                        } else {
                                            // 최대 선택 개수에 도달했으면 사용자에게 알립니다.
                                            scope.launch {
                                                snackbarHostState.showSnackbar(getString(R.string.max_selection_error, maxSelectionCount))
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = apiItem.id in selectedApiIds.value, // 체크박스 상태를 선택 여부와 동기화합니다.
                                    onCheckedChange = { isChecked ->
                                        val currentSelection = selectedApiIds.value
                                        if (isChecked) {
                                            if (currentSelection.size < maxSelectionCount) {
                                                selectedApiIds.value = currentSelection + apiItem.id
                                            } else {
                                                // 체크박스를 통해 최대 개수를 초과하려고 할 때도 알려줍니다.
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(getString(R.string.max_selection_error, maxSelectionCount))
                                                }
                                            }
                                        } else {
                                            selectedApiIds.value = currentSelection - apiItem.id
                                        }
                                    }
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(apiItem.name) // API 이름 표시
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 사용자가 선택한 API 목록을 저장하고 위젯을 업데이트합니다.
     * @param selectedApiIds 사용자가 선택한 API들의 ID 집합.
     */
    private fun saveSelection(selectedApiIds: Set<String>) {
        val context = applicationContext
        // SharedPreferences를 사용하여 위젯 ID별로 선택된 API ID 목록을 저장합니다.
        val prefs = context.getSharedPreferences("widget_prefs", MODE_PRIVATE).edit()
        prefs.putString("widget_multi_$appWidgetId", selectedApiIds.joinToString(","))
        prefs.apply()

        // 위젯 관리자를 통해 해당 위젯의 UI를 업데이트하도록 요청합니다.
        val appWidgetManager = AppWidgetManager.getInstance(context)
        ApiWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)

        // 위젯 설정이 성공적으로 완료되었음을 알리는 결과값을 설정합니다.
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        // 설정 액티비티를 종료합니다.
        finish()
    }

    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking { UserDataStore(newBase).getLanguage.first() }
        val locale = MainActivity.getLocaleFromLanguage(language)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}