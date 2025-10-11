package org.parkjw.capycaller.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.ui.theme.getHttpMethodColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiListScreen(
    apiItems: List<ApiItem>,
    onAddApi: () -> Unit,
    onApiClick: (ApiItem) -> Unit,
    onExecuteApis: (List<ApiItem>) -> Unit,
    onDeleteApis: (List<ApiItem>) -> Unit,
    onCopyApi: (ApiItem) -> Unit,
    onSettingsClick: () -> Unit,
) {
    var selectedApiIds by remember { mutableStateOf(emptySet<String>()) }
    val isInSelectionMode = selectedApiIds.isNotEmpty()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }
    var confirmDialogTitle by remember { mutableStateOf("") }
    var confirmDialogText by remember { mutableStateOf("") }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(confirmDialogTitle) },
            text = { Text(confirmDialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction()
                        showConfirmDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    BackHandler(enabled = isInSelectionMode) {
        selectedApiIds = emptySet()
    }

    fun toggleSelection(apiId: String) {
        selectedApiIds = if (apiId in selectedApiIds) {
            selectedApiIds - apiId
        } else {
            selectedApiIds + apiId
        }
    }

    Scaffold(
        topBar = {
            if (isInSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedApiIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedApiIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { selectedApiIds = apiItems.map { it.id }.toSet() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(onClick = {
                            confirmDialogTitle = "Execute APIs"
                            confirmDialogText = "Are you sure you want to execute ${selectedApiIds.size} APIs?"
                            confirmAction = {
                                val selectedItems = apiItems.filter { it.id in selectedApiIds }
                                onExecuteApis(selectedItems)
                                selectedApiIds = emptySet()
                            }
                            showConfirmDialog = true
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Execute selected")
                        }
                        IconButton(
                            onClick = {
                                confirmDialogTitle = "Copy API"
                                confirmDialogText = "Are you sure you want to copy this API?"
                                confirmAction = {
                                    val selectedItem = apiItems.find { it.id == selectedApiIds.first() }
                                    if (selectedItem != null) {
                                        onCopyApi(selectedItem)
                                    }
                                    selectedApiIds = emptySet()
                                }
                                showConfirmDialog = true
                            },
                            enabled = selectedApiIds.size == 1
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy selected")
                        }
                        IconButton(onClick = {
                            confirmDialogTitle = "Delete APIs"
                            confirmDialogText = "Are you sure you want to delete ${selectedApiIds.size} APIs?"
                            confirmAction = {
                                val selectedItems = apiItems.filter { it.id in selectedApiIds }
                                onDeleteApis(selectedItems)
                                selectedApiIds = emptySet()
                            }
                            showConfirmDialog = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("CapyCaller") },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isInSelectionMode) {
                FloatingActionButton(onClick = onAddApi) {
                    Icon(Icons.Filled.Add, contentDescription = "Add API")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(apiItems, key = { it.id }) { apiItem ->
                ApiListItem(
                    apiItem = apiItem,
                    isSelected = apiItem.id in selectedApiIds,
                    isInSelectionMode = isInSelectionMode,
                    onToggleSelection = { toggleSelection(apiItem.id) },
                    onNavigateToDetails = { onApiClick(apiItem) }
                )
            }
        }
    }
}

@Composable
fun ApiListItem(
    apiItem: ApiItem,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onNavigateToDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(isInSelectionMode) {
                detectTapGestures(
                    onLongPress = { onToggleSelection() },
                    onTap = {
                        if (isInSelectionMode) {
                            onToggleSelection()
                        } else {
                            onNavigateToDetails()
                        }
                    }
                )
            },
        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HttpMethodLabel(method = apiItem.method)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = apiItem.name, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = apiItem.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                if (apiItem.memo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = apiItem.memo, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
    }
}
