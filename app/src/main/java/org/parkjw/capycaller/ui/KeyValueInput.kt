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

/**
 * 키-값(Key-Value) 쌍의 목록을 입력받기 위한 재사용 가능한 Composable 입니다.
 * HTTP 헤더나 쿼리 파라미터 입력 UI에 사용됩니다.
 *
 * @param pairs UI와 바인딩될 키-값 쌍의 `SnapshotStateList`. 리스트가 변경되면 UI가 자동으로 업데이트됩니다.
 * @param keyLabel 키 입력 필드의 라벨 텍스트.
 * @param valueLabel 값 입력 필드의 라벨 텍스트.
 */
@Composable
fun KeyValueInput(
    pairs: SnapshotStateList<Pair<String, String>>,
    keyLabel: String = "Key",
    valueLabel: String = "Value"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 리스트의 각 아이템에 대해 키-값 입력 행을 생성합니다.
        pairs.forEachIndexed { index, pair ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 키 입력 필드
                OutlinedTextField(
                    value = pair.first,
                    onValueChange = { pairs[index] = pairs[index].copy(first = it) }, // 값이 변경되면 리스트의 해당 아이템을 업데이트
                    label = { Text(keyLabel) },
                    modifier = Modifier.weight(1f) // 가로 공간을 값 필드와 동일하게 차지
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 값 입력 필드
                OutlinedTextField(
                    value = pair.second,
                    onValueChange = { pairs[index] = pairs[index].copy(second = it) },
                    label = { Text(valueLabel) },
                    modifier = Modifier.weight(1f)
                )
                // 삭제 버튼
                IconButton(onClick = { pairs.removeAt(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                }
            }
        }
    }
    // 새 키-값 쌍을 추가하는 버튼
    Button(onClick = { pairs.add("" to "") }, modifier = Modifier.padding(top = 8.dp)) {
        Icon(Icons.Default.Add, contentDescription = "추가")
        Text("추가")
    }
}
