package com.example.kakuyomureader.feature.bookmark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kakuyomureader.core.model.BookmarkItem
import com.example.kakuyomureader.core.model.DictionaryItem
import com.example.kakuyomureader.core.model.HistoryItem
import com.example.kakuyomureader.core.model.ReadingMode

/**
 * お気に入り・履歴・辞書・設定サイドドロワー
 *
 * [責務]: サイドドロワー内のUI描画、お気に入り・履歴・辞書一覧、読書モード設定を提供する。
 * [影響する状態]: ドロワー開閉、URL遷移、読み上げ設定、辞書削除。
 * [発生しうる例外・エラー]: なし。
 */
@Composable
fun BookmarkHistoryDrawer(
    bookmarks: List<BookmarkItem>,
    history: List<HistoryItem>,
    dictionaryList: List<DictionaryItem> = emptyList(),
    readingMode: ReadingMode,
    parentLevels: Int,
    onUrlSelected: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onDeleteDictionary: (String) -> Unit = {},
    onClearAllHistory: () -> Unit,
    onReadingModeChanged: (ReadingMode) -> Unit,
    onParentLevelsChanged: (Int) -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DrawerTab.BOOKMARKS) }

    ModalDrawerSheet(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "スピークブラウザ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "お気に入り・履歴・設定",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = onClose) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "閉じる"
                        )
                    }
                }
            }

            DrawerTabsHeader(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            HorizontalDivider()

            when (selectedTab) {
                DrawerTab.BOOKMARKS -> {
                    if (bookmarks.isEmpty()) {
                        EmptyStateView("お気に入りはまだ登録されていません。\n画面上の「☆」を押して追加できます。")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(bookmarks, key = { it.id }) { item ->
                                BookmarkRowItem(
                                    item = item,
                                    onClick = onUrlSelected,
                                    onDelete = onDeleteBookmark
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
                DrawerTab.HISTORY -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (history.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onClearAllHistory) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("履歴をすべて消去")
                                }
                            }
                        }
                        if (history.isEmpty()) {
                            EmptyStateView("閲覧履歴はありません。")
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(history, key = { it.id }) { item ->
                                    HistoryRowItem(
                                        item = item,
                                        onClick = onUrlSelected,
                                        onDelete = onDeleteHistory
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
                DrawerTab.DICTIONARY -> {
                    if (dictionaryList.isEmpty()) {
                        EmptyStateView("ユーザー辞書はまだ登録されていません。\n辞書モード（📖）で文字選択後「➕ 登録」を押して追加できます。")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(dictionaryList, key = { it.id }) { item ->
                                DictionaryRowItem(
                                    item = item,
                                    onDelete = onDeleteDictionary
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
                DrawerTab.SETTINGS -> {
                    SettingsTabView(
                        readingMode = readingMode,
                        parentLevels = parentLevels,
                        onReadingModeChanged = onReadingModeChanged,
                        onParentLevelsChanged = onParentLevelsChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
