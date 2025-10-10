package org.parkjw.capycaller

import android.app.Application
import android.widget.Toast
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.data.ApiRepository

class ApiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ApiRepository(application)
    private val shortcutController = ShortcutController(application)

    private val _apiItems = MutableStateFlow<List<ApiItem>>(emptyList())
    val apiItems: StateFlow<List<ApiItem>> = _apiItems.asStateFlow()

    init {
        loadApis()
    }

    private fun loadApis() {
        viewModelScope.launch {
            val apiItems = repository.getApiItems()
            _apiItems.value = apiItems
            shortcutController.updateShortcuts(apiItems)
        }
    }

    fun getApiItem(id: String?): ApiItem? {
        return _apiItems.value.find { it.id == id }
    }

    fun addApi(apiItem: ApiItem) {
        viewModelScope.launch {
            var itemToSave = apiItem
            if (apiItem.isShortcut) {
                val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(getApplication())
                val currentShortcutCount = _apiItems.value.count { it.isShortcut }
                if (currentShortcutCount >= maxShortcuts) {
                    itemToSave = apiItem.copy(isShortcut = false)
                    Toast.makeText(getApplication(), "Maximum number of shortcuts reached.", Toast.LENGTH_SHORT).show()
                }
            }

            val updatedList = _apiItems.value + itemToSave
            repository.saveApiItems(updatedList)
            _apiItems.value = updatedList
            shortcutController.updateShortcuts(updatedList)
        }
    }

    fun updateApi(apiItem: ApiItem) {
        viewModelScope.launch {
            var itemToSave = apiItem
            val originalItem = _apiItems.value.find { it.id == apiItem.id }
            if (apiItem.isShortcut && originalItem?.isShortcut == false) {
                val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(getApplication())
                val currentShortcutCount = _apiItems.value.count { it.isShortcut }
                if (currentShortcutCount >= maxShortcuts) {
                    itemToSave = apiItem.copy(isShortcut = false)
                    Toast.makeText(getApplication(), "Maximum number of shortcuts reached.", Toast.LENGTH_SHORT).show()
                }
            }

            val updatedList = _apiItems.value.map {
                if (it.id == itemToSave.id) itemToSave else it
            }
            repository.saveApiItems(updatedList)
            _apiItems.value = updatedList
            shortcutController.updateShortcuts(updatedList)
        }
    }

    fun deleteApi(apiItem: ApiItem) {
        viewModelScope.launch {
            val updatedList = _apiItems.value.filter { it.id != apiItem.id }
            repository.saveApiItems(updatedList)
            _apiItems.value = updatedList
            shortcutController.updateShortcuts(updatedList)
        }
    }

    fun deleteApis(apiItems: List<ApiItem>) {
        viewModelScope.launch {
            val idsToDelete = apiItems.map { it.id }.toSet()
            val updatedList = _apiItems.value.filter { it.id !in idsToDelete }
            repository.saveApiItems(updatedList)
            _apiItems.value = updatedList
            shortcutController.updateShortcuts(updatedList)
        }
    }

    fun copyApi(apiItem: ApiItem) {
        viewModelScope.launch {
            var newName = "${apiItem.name} Copy"
            var counter = 2
            while (_apiItems.value.any { it.name == newName }) {
                newName = "${apiItem.name} Copy $counter"
                counter++
            }

            val newApiItem = apiItem.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = newName,
                isShortcut = false
            )

            val updatedList = _apiItems.value + newApiItem
            repository.saveApiItems(updatedList)
            _apiItems.value = updatedList
            shortcutController.updateShortcuts(updatedList)
        }
    }

    fun restoreApis(restoredApis: List<ApiItem>) {
        viewModelScope.launch {
            repository.saveApiItems(restoredApis)
            _apiItems.value = restoredApis
            shortcutController.updateShortcuts(restoredApis)
        }
    }
}
