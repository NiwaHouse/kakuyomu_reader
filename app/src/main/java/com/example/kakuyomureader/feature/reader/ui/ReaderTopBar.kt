package com.example.kakuyomureader.feature.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kakuyomureader.core.model.SiteType
import com.example.kakuyomureader.feature.reader.viewmodel.ReaderEvent
import com.example.kakuyomureader.feature.reader.viewmodel.ReaderUiState

/**
 * リーダー画面トップバー (Compose)
 *
 * [責務]: メニュー開閉・ナビゲーション・URL入力・ホーム・お気に入り・読込進捗バーを表示する。
 * [影響する状態]: URL更新、WebViewナビゲーション、ドロワー開閉、お気に入りトグル。
 * [発生しうる例外・エラー]: なし。
 */
@Composable
fun ReaderTopBar(
    uiState: ReaderUiState,
    onEvent: (ReaderEvent) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onReload: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    var textInput by remember(uiState.currentUrl) { mutableStateOf(uiState.currentUrl) }
    val focusManager = LocalFocusManager.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "メニュー (お気に入り・履歴)")
                }
                IconButton(onClick = onGoBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                }
                IconButton(onClick = onGoForward) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "進む")
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            onEvent(ReaderEvent.OnUrlEntered(textInput))
                            focusManager.clearFocus()
                        }
                    ),
                    trailingIcon = {
                        SiteTypeBadge(uiState.activeSiteType)
                    }
                )

                IconButton(onClick = { onEvent(ReaderEvent.OnToggleBookmark) }) {
                    Icon(
                        imageVector = if (uiState.isCurrentPageBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "お気に入り追加/解除",
                        tint = if (uiState.isCurrentPageBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onEvent(ReaderEvent.OnHomeClicked) }) {
                    Icon(Icons.Default.Home, contentDescription = "ホーム (Google)")
                }

                IconButton(onClick = onReload) {
                    Icon(Icons.Default.Refresh, contentDescription = "更新")
                }
            }

            if (uiState.isLoadingWebPage) {
                LinearProgressIndicator(
                    progress = { uiState.webPageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun SiteTypeBadge(siteType: SiteType) {
    val (label, bgColor, textColor) = when (siteType) {
        SiteType.KAKUYOMU -> Triple("カクヨム", Color(0xFF1E88E5), Color.White)
        SiteType.NAROU -> Triple("なろう", Color(0xFF43A047), Color.White)
        SiteType.GENERIC -> Triple("Web", Color(0xFF757575), Color.White)
    }

    Badge(
        containerColor = bgColor,
        contentColor = textColor,
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(text = label, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }
}
