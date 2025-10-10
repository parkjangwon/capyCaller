package org.parkjw.capycaller

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.parkjw.capycaller.data.ApiItem

class ShortcutController(private val context: Context) {

    fun updateShortcuts(apiItems: List<ApiItem>) {
        val shortcuts = apiItems.map {
            val intent = Intent(context, TransparentActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("myapp://apicall/${it.id}")
                // These flags are crucial for starting the activity in a new, separate task.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            ShortcutInfoCompat.Builder(context, it.id)
                .setShortLabel(it.name)
                .setLongLabel(it.name)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
