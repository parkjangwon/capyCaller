package org.parkjw.capycaller

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import org.parkjw.capycaller.data.ApiItem

/**
 * 앱의 동적 바로가기(Dynamic Shortcuts)를 관리하는 클래스입니다.
 * 사용자가 '바로가기 추가'로 설정한 API 항목들을 앱 아이콘을 길게 눌렀을 때 나타나는 메뉴에 등록하거나 업데이트합니다.
 * @param context 바로가기 정보를 시스템에 등록하기 위해 필요한 컨텍스트.
 */
class ShortcutController(private val context: Context) {

    /**
     * 주어진 API 아이템 목록을 기반으로 동적 바로가기를 업데이트합니다.
     * 'isShortcut' 플래그가 true인 항목만 바로가기로 만들어집니다.
     * @param apiItems 전체 API 아이템 목록.
     */
    fun updateShortcuts(apiItems: List<ApiItem>) {
        // 시스템에서 허용하는 앱당 최대 바로가기 개수를 가져옵니다.
        val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        
        // 바로가기로 설정된 API 아이템들만 필터링하고, 최대 개수만큼만 가져와서
        // ShortcutInfoCompat 객체로 변환합니다.
        val shortcuts = apiItems
            .filter { it.isShortcut } // isShortcut이 true인 항목만 선택
            .take(maxShortcuts)      // 최대 허용 개수만큼만 가져옴
            .map { apiItem ->
                // 바로가기를 눌렀을 때 실행될 인텐트를 생성합니다.
                // TransparentActivity를 호출하여 실제 API 호출을 트리거합니다.
                val intent = Intent(context, TransparentActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    // 어떤 API를 호출할지 식별하기 위해 고유한 URI를 데이터로 설정합니다.
                    data = Uri.parse("myapp://apicall/${apiItem.id}")
                    // 새로운 태스크에서 액티비티를 시작하도록 플래그를 설정합니다.
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

                // ShortcutInfoCompat.Builder를 사용하여 바로가기의 정보를 설정합니다.
                ShortcutInfoCompat.Builder(context, apiItem.id) // 각 바로가기는 고유한 ID(apiItem.id)를 가집니다.
                    .setShortLabel(apiItem.name) // 짧은 이름 설정
                    .setLongLabel(apiItem.name)  // 긴 이름 설정
                    .setIntent(intent)           // 실행할 인텐트 설정
                    .build()
            }
        // ShortcutManagerCompat를 사용하여 생성된 바로가기 목록을 시스템에 동적으로 설정합니다.
        // 이전에 있던 동적 바로가기 목록은 이 목록으로 완전히 대체됩니다.
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
