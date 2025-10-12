
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

/**
 * 단일 API 위젯을 설정하는 액티비티입니다.
 * 사용자가 홈 화면에 위젯을 추가할 때 실행되며, 위젯이 호출할 API 하나를 선택할 수 있게 합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
class SingleApiWidgetConfigureActivity : ComponentActivity() {

    // 설정 중인 위젯의 고유 ID. 이 ID로 위젯을 식별하고 관리합니다.
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 사용자가 설정을 완료하지 않고 종료할 경우를 대비해 기본 결과값을 CANCELED로 설정합니다.
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
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("호출할 API 선택") })
                    }
                ) { paddingValues ->
                    // 스크롤 가능한 API 목록을 표시합니다.
                    LazyColumn(modifier = Modifier.padding(paddingValues)) {
                        items(apiItems) { apiItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    // 각 API 아이템을 클릭하면 selectApi 함수가 호출됩니다.
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

    /**
     * 사용자가 목록에서 특정 API를 선택했을 때 호출됩니다.
     * 선택된 API 정보로 위젯을 설정하고 액티비티를 종료합니다.
     * @param apiItem 사용자가 선택한 API 아이템.
     */
    private fun selectApi(apiItem: ApiItem) {
        val context = applicationContext
        // SharedPreferences를 사용하여 위젯 ID에 선택된 API의 ID와 이름을 저장합니다.
        val prefs = context.getSharedPreferences("widget_prefs", MODE_PRIVATE).edit()
        prefs.putString("widget_${appWidgetId}_id", apiItem.id)
        prefs.putString("widget_${appWidgetId}_name", apiItem.name)
        prefs.apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        // 위젯의 레이아웃을 위한 RemoteViews 객체를 생성합니다.
        val views = RemoteViews(context.packageName, R.layout.single_api_widget)
        // 위젯 버튼의 텍스트를 선택된 API의 이름으로 설정합니다.
        views.setTextViewText(R.id.widget_button, apiItem.name)

        // 위젯 클릭 시 API 호출을 처리할 TransparentActivity를 실행할 인텐트를 생성합니다.
        val intent = Intent(context, TransparentActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("myapp://apicall/${apiItem.id}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        // 인텐트를 즉시 실행하지 않고, 특정 시점(버튼 클릭)에 실행되도록 PendingIntent를 생성합니다.
        val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // 생성된 PendingIntent를 위젯 버튼의 클릭 이벤트에 설정합니다.
        views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

        // AppWidgetManager를 통해 위젯의 UI를 최종적으로 업데이트합니다.
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // 위젯 설정이 성공적으로 완료되었음을 알리는 결과값을 설정합니다.
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        // 설정 액티비티를 종료합니다.
        finish()
    }
}
