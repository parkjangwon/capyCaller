package org.parkjw.capycaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.capycaller.data.ApiItem
import org.parkjw.capycaller.ui.theme.getHttpMethodColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiListScreen(
    apiItems: List<ApiItem>,
    onAddApi: () -> Unit,
    onApiClick: (ApiItem) -> Unit,
    onExecuteApi: (ApiItem) -> Unit,
    onCopyApi: (ApiItem) -> Unit,
    onDeleteApi: (ApiItem) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CapyCaller") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddApi) {
                Icon(Icons.Filled.Add, contentDescription = "Add API")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) { 
            items(apiItems) { apiItem ->
                ApiListItem(apiItem, onApiClick, onExecuteApi, onCopyApi, onDeleteApi)
            }
        }
    }
}

@Composable
fun ApiListItem(
    apiItem: ApiItem,
    onClick: (ApiItem) -> Unit,
    onExecute: (ApiItem) -> Unit,
    onCopy: (ApiItem) -> Unit,
    onDelete: (ApiItem) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showExecuteConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text(apiItem.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showExecuteConfirm = true; showMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Execute")
                        Spacer(Modifier.width(8.dp))
                        Text("Execute")
                    }
                    OutlinedButton(
                        onClick = { onCopy(apiItem); showMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Copy") // Placeholder Icon
                        Spacer(Modifier.width(8.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true; showMenu = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExecuteConfirm) {
        AlertDialog(
            onDismissRequest = { showExecuteConfirm = false },
            title = { Text("Execute API") },
            text = { Text("Are you sure you want to execute '${apiItem.name}'?") },
            confirmButton = {
                Button(onClick = { onExecute(apiItem); showExecuteConfirm = false }) {
                    Text("Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExecuteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete API") },
            text = { Text("Are you sure you want to delete '${apiItem.name}'?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(apiItem); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
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
                    onTap = { onClick(apiItem) } 
                )
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HttpMethodLabel(method = apiItem.method)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = apiItem.name, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = apiItem.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}
