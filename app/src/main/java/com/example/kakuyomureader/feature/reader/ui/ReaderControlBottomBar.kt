package com.example.kakuyomureader.feature.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.core.model.PlaybackState
import com.example.kakuyomureader.feature.reader.viewmodel.ReaderEvent
import com.example.kakuyomureader.feature.reader.viewmodel.ReaderUiState

/**
 * リーダー画面コントロールボトムバー (Compose)
 *
 * [責務]: 音声再生の操作（再生/一時停止/停止/速度/自動連続）および現在の読み上げ文を表示する。
 * [影響する状態]: PlaybackState, PlaybackSpeed, isAutoPlayNext。
 * [発生しうる例外・エラー]: なし。
 */
@Composable
fun ReaderControlBottomBar(
    uiState: ReaderUiState,
    onEvent: (ReaderEvent) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 読み上げ中のテキスト表示
            Text(
                text = uiState.currentParagraph?.let { "読み上げ中: ${it.shortPreview}" }
                    ?: "タップした段落を読み上げます",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 主要コントロール行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 再生・一時停止・停止・バックグラウンド再生ボタン群
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isPlaying = uiState.playbackState == PlaybackState.PLAYING
                    FilledIconButton(
                        onClick = { onEvent(ReaderEvent.OnTogglePlayPause) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "一時停止" else "再生"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { onEvent(ReaderEvent.OnStopPlayback) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "停止")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    androidx.compose.material3.OutlinedButton(
                        onClick = { onEvent(ReaderEvent.OnStartBackgroundPlayback) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("🎧 BG", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 辞書モードトグルボタン
                    androidx.compose.material3.FilterChip(
                        selected = uiState.isDictionaryMode,
                        onClick = { onEvent(ReaderEvent.OnToggleDictionaryMode(!uiState.isDictionaryMode)) },
                        label = { Text("📖 辞書", fontSize = 11.sp) },
                        modifier = Modifier.height(36.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // 辞書登録実行ボタン
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { onEvent(ReaderEvent.OnRegisterDictionaryClicked) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("➕ 登録", fontSize = 11.sp)
                    }
                }

                // 自動連続読み上げトグル
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "連続",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = uiState.isAutoPlayNext,
                        onCheckedChange = { onEvent(ReaderEvent.OnToggleAutoPlayNext(it)) }
                    )
                }
            }

            // 速度スライダー行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "速度: ${uiState.playbackSpeed.displayString}",
                    fontSize = 12.sp,
                    modifier = Modifier.width(70.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = uiState.playbackSpeed.rate,
                    onValueChange = { onEvent(ReaderEvent.OnSpeedChanged(PlaybackSpeed.fromFloatClamped(it))) },
                    valueRange = 0.5f..2.5f,
                    steps = 7,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
