package com.example.kakuyomureader.feature.reader.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 辞書登録ダイアログ (Compose Component)
 *
 * [責務]: 選択されたテキスト表記と、ユーザーが入力した「よみ」の辞書登録インターフェースを提供する。
 * [影響する状態]: 辞書データの追加、ダイアログの表示状態。
 * [発生しうる例外・エラー]: なし。
 */
@Composable
fun DictionaryRegisterDialog(
    initialSurface: String,
    onSave: (surface: String, reading: String) -> Unit,
    onDismiss: () -> Unit
) {
    var surface by remember(initialSurface) { mutableStateOf(initialSurface) }
    var reading by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📖 ユーザー辞書登録", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = surface,
                    onValueChange = { surface = it },
                    label = { Text("表記（単語）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reading,
                    onValueChange = { reading = it },
                    label = { Text("よみ（読み方）") },
                    placeholder = { Text("例: まおう") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (surface.isNotBlank() && reading.isNotBlank()) {
                        onSave(surface, reading)
                    }
                },
                enabled = surface.isNotBlank() && reading.isNotBlank()
            ) {
                Text("登録")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
