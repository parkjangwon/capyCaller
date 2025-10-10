package org.parkjw.capycaller.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ApiRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getApiItems(): List<ApiItem> {
        val json = sharedPreferences.getString("api_items", null)
        return if (json != null) {
            val type = object : TypeToken<List<ApiItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveApiItems(apiItems: List<ApiItem>) {
        val json = gson.toJson(apiItems)
        sharedPreferences.edit {
            putString("api_items", json)
        }
    }

    fun getApiItem(id: String): ApiItem? {
        return getApiItems().find { it.id == id }
    }
}
