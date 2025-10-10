package org.parkjw.capycaller

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import org.parkjw.capycaller.data.ApiItem

class ShortcutController(private val context: Context) {

    fun updateShortcuts(apiItems: List<ApiItem>) {
        val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val shortcuts = apiItems
            .filter { it.isShortcut }
            .take(maxShortcuts)
            .map { apiItem ->
                val intent = Intent(context, TransparentActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("myapp://apicall/${apiItem.id}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

                ShortcutInfoCompat.Builder(context, apiItem.id)
                    .setShortLabel(apiItem.name)
                    .setLongLabel(apiItem.name)
                    .setIntent(intent)
                    .build()
            }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
