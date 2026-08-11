package com.example.kakuyomureader.feature.reader.ui

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.kakuyomureader.feature.bookmark.ui.BookmarkHistoryDrawer
import com.example.kakuyomureader.feature.reader.viewmodel.ReaderEvent
import com.example.kakuyomureader.feature.reader.viewmodel.ReaderViewModel
import kotlinx.coroutines.launch

/**
 * リーダー画面メインスクリーン (Compose)
 *
 * [責務]: ModalNavigationDrawer, TopBar, WebView, BottomBarを統合配置し、ViewModelとUIイベントを仲介する。
 * [影響する状態]: 画面全体の描画、ドロワー開閉、Snackbar表示。
 * [発生しうる例外・エラー]: なし。
 */
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(ReaderEvent.OnErrorDismissed())
        }
    }

    if (uiState.isDictionaryDialogOpen) {
        DictionaryRegisterDialog(
            initialSurface = uiState.selectedTextForDictionary,
            onSave = { surface, reading ->
                viewModel.onEvent(ReaderEvent.OnSaveDictionaryItem(surface, reading))
            },
            onDismiss = {
                viewModel.onEvent(ReaderEvent.OnDismissDictionaryDialog)
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            BookmarkHistoryDrawer(
                bookmarks = uiState.bookmarks,
                history = uiState.history,
                dictionaryList = uiState.dictionaryList,
                readingMode = uiState.readingMode,
                parentLevels = uiState.parentLevels,
                onUrlSelected = { url ->
                    viewModel.onEvent(ReaderEvent.OnUrlEntered(url))
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteBookmark = { id -> viewModel.onEvent(ReaderEvent.OnDeleteBookmark(id)) },
                onDeleteHistory = { id -> viewModel.onEvent(ReaderEvent.OnDeleteHistory(id)) },
                onDeleteDictionary = { id -> viewModel.onEvent(ReaderEvent.OnDeleteDictionaryItem(id)) },
                onClearAllHistory = { viewModel.onEvent(ReaderEvent.OnClearAllHistory) },
                onReadingModeChanged = { mode -> viewModel.onEvent(ReaderEvent.OnReadingModeChanged(mode)) },
                onParentLevelsChanged = { levels -> viewModel.onEvent(ReaderEvent.OnParentLevelsChanged(levels)) },
                onClose = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                ReaderTopBar(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onGoBack = {
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        }
                    },
                    onGoForward = {
                        if (webViewInstance?.canGoForward() == true) {
                            webViewInstance?.goForward()
                        }
                    },
                    onReload = {
                        webViewInstance?.reload()
                    },
                    onOpenDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            },
            bottomBar = {
                val context = androidx.compose.ui.platform.LocalContext.current
                val activity = context as? android.app.Activity
                ReaderControlBottomBar(
                    uiState = uiState,
                    onEvent = { event ->
                        viewModel.onEvent(event)
                        if (event is ReaderEvent.OnStartBackgroundPlayback) {
                            activity?.moveTaskToBack(true)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ReaderWebView(
                    currentUrl = uiState.currentUrl,
                    jsExecutionFlow = viewModel.jsExecutionFlow,
                    onEvent = viewModel::onEvent,
                    onWebViewReady = { webViewInstance = it }
                )
            }
        }
    }
}
