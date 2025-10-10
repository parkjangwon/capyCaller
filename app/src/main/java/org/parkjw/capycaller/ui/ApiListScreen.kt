package org.parkjw.capycaller.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.parkjw.capycaller.data.ApiItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiListScreen(
    apiItems: List<ApiItem>,
    onAddApi: () -> Unit,
    onApiClick: (ApiItem) -> Unit, // This will now be for navigation to edit
    onExecuteApi: (ApiItem) -> Unit,
    onEditApi: (ApiItem) -> Unit,
    onDeleteApi: (ApiItem) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddApi) {
                Icon(Icons.Filled.Add, contentDescription = "Add API")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) { 
            items(apiItems) { apiItem ->
                ApiListItem(apiItem, onApiClick, onExecuteApi, onEditApi, onDeleteApi)
            }
        }
    }
}

@Composable
fun ApiListItem(
    apiItem: ApiItem,
    onClick: (ApiItem) -> Unit,
    onExecute: (ApiItem) -> Unit,
    onEdit: (ApiItem) -> Unit,
    onDelete: (ApiItem) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Execute API") },
            text = { Text("Are you sure you want to execute '${apiItem.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onExecute(apiItem)
                        showConfirmDialog = false
                    }
                ) {
                    Text("Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showMenu = true },
                    onTap = { onClick(apiItem) } // Changed to simple click for navigation
                )
            }
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = apiItem.name, style = MaterialTheme.typography.titleMedium)
                Text(text = apiItem.url, style = MaterialTheme.typography.bodySmall)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(text = { Text("Execute") }, onClick = {
                    showConfirmDialog = true
                    showMenu = false
                })
                DropdownMenuItem(text = { Text("Edit") }, onClick = {
                    onEdit(apiItem)
                    showMenu = false
                })
                DropdownMenuItem(text = { Text("Delete") }, onClick = {
                    onDelete(apiItem)
                    showMenu = false
                })
            }
        }
    }
}
