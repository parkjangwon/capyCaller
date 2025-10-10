package org.parkjw.capycaller.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun KeyValueInput(
    pairs: SnapshotStateList<Pair<String, String>>,
    keyLabel: String = "Key",
    valueLabel: String = "Value"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        pairs.forEachIndexed { index, pair ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pair.first,
                    onValueChange = { pairs[index] = pairs[index].copy(first = it) },
                    label = { Text(keyLabel) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = pair.second,
                    onValueChange = { pairs[index] = pairs[index].copy(second = it) },
                    label = { Text(valueLabel) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { pairs.removeAt(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }
    Button(onClick = { pairs.add("" to "") }, modifier = Modifier.padding(top = 8.dp)) {
        Icon(Icons.Default.Add, contentDescription = "Add")
        Text("Add")
    }
}
