package org.parkjw.capycaller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserDataStore(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
    }

    val getTheme: Flow<String> = dataStore.data.map {
        it[THEME_KEY] ?: "System"
    }

    suspend fun saveTheme(theme: String) {
        dataStore.edit {
            it[THEME_KEY] = theme
        }
    }
}
