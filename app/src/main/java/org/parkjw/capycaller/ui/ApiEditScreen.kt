package org.parkjw.capycaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.parkjw.capycaller.data.ApiItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiEditScreen(
    apiItem: ApiItem?,
    onSave: (ApiItem) -> Unit,
    onExecute: (ApiItem) -> Unit,
) {
    var name by remember(apiItem) { mutableStateOf(apiItem?.name ?: "") }
    var url by remember(apiItem) { mutableStateOf(apiItem?.url ?: "") }
    var method by remember(apiItem) { mutableStateOf(apiItem?.method ?: "GET") }

    val queryParams = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(apiItem?.queryParams ?: emptyList()) } }
    val headers = remember { mutableStateListOf<Pair<String, String>>().also { it.addAll(apiItem?.headers ?: emptyList()) } }

    var bodyType by remember(apiItem) { mutableStateOf(apiItem?.bodyType ?: "application/json") }
    var body by remember(apiItem) { mutableStateOf(apiItem?.body ?: "") }
    var selectedTab by remember { mutableStateOf(0) }

    fun buildApiItem(): ApiItem {
        return apiItem?.copy(
            name = name,
            url = url,
            method = method,
            headers = headers.toList(),
            queryParams = queryParams.toList(),
            bodyType = bodyType,
            body = body
        ) ?: ApiItem(
            id = apiItem?.id ?: java.util.UUID.randomUUID().toString(),
            name = name,
            url = url,
            method = method,
            headers = headers.toList(),
            queryParams = queryParams.toList(),
            bodyType = bodyType,
            body = body
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (apiItem == null) "Add API" else "Edit API") })
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = { onExecute(buildApiItem()) }
                ) {
                    Text("Execute")
                }
                Button(onClick = { onSave(buildApiItem()) }) {
                    Text("Save")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("API Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                 var methodMenuExpanded by remember { mutableStateOf(false) }
                 Box(modifier = Modifier.weight(0.3f)) {
                    TextButton(onClick = { methodMenuExpanded = true }) {
                        Text(method)
                    }
                    DropdownMenu(expanded = methodMenuExpanded, onDismissRequest = { methodMenuExpanded = false }) {
                        listOf("GET", "POST", "PUT", "DELETE", "PATCH").forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                method = selection
                                methodMenuExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.weight(0.7f)
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Params") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Headers") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Body") })
            }

            when (selectedTab) {
                0 -> KeyValueInput(queryParams, keyLabel = "Parameter")
                1 -> KeyValueInput(headers, keyLabel = "Header")
                2 -> BodyInput(body, bodyType, onBodyChange = { body = it }, onBodyTypeChange = { bodyType = it })
            }
        }
    }
}

@Composable
fun BodyInput(body: String, bodyType: String, onBodyChange: (String) -> Unit, onBodyTypeChange: (String) -> Unit) {
     var bodyTypeMenuExpanded by remember { mutableStateOf(false) }
     Box {
        TextButton(onClick = { bodyTypeMenuExpanded = true }) {
            Text(bodyType)
        }
        DropdownMenu(expanded = bodyTypeMenuExpanded, onDismissRequest = { bodyTypeMenuExpanded = false }) {
            listOf("application/json", "text/plain", "application/x-www-form-urlencoded").forEach { selection ->
                DropdownMenuItem(text = { Text(selection) }, onClick = {
                    onBodyTypeChange(selection)
                    bodyTypeMenuExpanded = false
                })
            }
        }
    }
    OutlinedTextField(
        value = body,
        onValueChange = onBodyChange,
        label = { Text("Request Body") },
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp)
    )
}
