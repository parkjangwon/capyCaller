package org.parkjw.capycaller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.UserDataStore

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userDataStore = UserDataStore(application)

    val theme: StateFlow<String> = userDataStore.getTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "System"
    )

    fun setTheme(theme: String) {
        viewModelScope.launch {
            userDataStore.saveTheme(theme)
        }
    }
}
