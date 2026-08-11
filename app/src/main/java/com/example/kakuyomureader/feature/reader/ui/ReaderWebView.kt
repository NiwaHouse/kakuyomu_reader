package com.example.kakuyomureader.feature.reader.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kakuyomureader.feature.reader.bridge.AndroidBridge
import com.example.kakuyomureader.feature.reader.viewmodel.ReaderEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * WebViewコンポーザブル
 *
 * [責務]: Android WebViewをComposeに埋め込み、JS Bridgeの登録およびJSコマンド実行を管理する。
 * [影響する状態]: WebViewインスタンス、ページ読込状態、JS実行。
 * [発生しうる例外・エラー]: なし。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderWebView(
    currentUrl: String,
    jsExecutionFlow: SharedFlow<String>,
    onEvent: (ReaderEvent) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            // AndroidBridgeの登録
            val bridge = AndroidBridge(
                onParagraphTap = { id, text ->
                    post { onEvent(ReaderEvent.OnParagraphTapped(id, text)) }
                },
                onNextParagraph = { id, text ->
                    post { onEvent(ReaderEvent.OnNextParagraphReceived(id, text)) }
                },
                onEpisodeEnd = {
                    post { onEvent(ReaderEvent.OnEndOfEpisodeReached) }
                },
                onContainerExtracted = { json ->
                    post { onEvent(ReaderEvent.OnContainerParagraphsReceived(json)) }
                },
                onNoNextEpisode = {
                    post { onEvent(ReaderEvent.OnNoNextEpisodeReceived) }
                },
                onSelectedText = { text ->
                    post { onEvent(ReaderEvent.OnSelectedTextReceived(text)) }
                }
            )
            addJavascriptInterface(bridge, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let { onEvent(ReaderEvent.OnPageStarted(it)) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val title = view?.title ?: ""
                    url?.let { onEvent(ReaderEvent.OnPageFinished(it, title)) }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onEvent(ReaderEvent.OnProgressChanged(newProgress))
                }
            }
        }
    }

    LaunchedEffect(webView) {
        onWebViewReady(webView)
    }

    // URLの変更検知
    LaunchedEffect(currentUrl) {
        if (webView.url != currentUrl) {
            webView.loadUrl(currentUrl)
        }
    }

    // ViewModelからのJavaScript実行ストリームを購読
    LaunchedEffect(jsExecutionFlow) {
        jsExecutionFlow.collectLatest { script ->
            webView.evaluateJavascript(script, null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
