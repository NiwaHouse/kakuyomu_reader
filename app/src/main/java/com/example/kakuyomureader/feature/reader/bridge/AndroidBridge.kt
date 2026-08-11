package com.example.kakuyomureader.feature.reader.bridge

import android.webkit.JavascriptInterface
import com.example.kakuyomureader.core.common.AppLogger

/**
 * WebView - Android Native ブリッジインターフェース
 *
 * [責務]: WebView内で実行されたJavaScriptからの段落タップ、次段落通知、エピソード末尾通知を受信する。
 * [影響する状態]: ViewModelへの段落イベント通知（バックグラウンドスレッドで到達）。
 * [発生しうる例外・エラー]: なし。
 */
class AndroidBridge(
    private val onParagraphTap: (id: String, text: String) -> Unit,
    private val onNextParagraph: (id: String, text: String) -> Unit,
    private val onEpisodeEnd: () -> Unit,
    private val onContainerExtracted: (paragraphsJson: String) -> Unit = {},
    private val onNoNextEpisode: () -> Unit = {},
    private val onSelectedText: (text: String) -> Unit = {}
) {

    /**
     * ユーザーが段落をタップした際にJSから呼び出される。
     */
    @JavascriptInterface
    fun onParagraphClicked(paragraphId: String, text: String) {
        AppLogger.d("AndroidBridge", "onParagraphClicked: id=$paragraphId, text=${text.take(15)}...")
        onParagraphTap(paragraphId, text)
    }

    /**
     * コンテナ一括読みモードで、親コンテナ内の全段落配列が一括抽出された際にJSから呼び出される。
     */
    @JavascriptInterface
    fun onContainerParagraphsExtracted(paragraphsJson: String) {
        AppLogger.d("AndroidBridge", "onContainerParagraphsExtracted: jsonLen=${paragraphsJson.length}")
        onContainerExtracted(paragraphsJson)
    }

    /**
     * 自動連続再生で次の段落が見つかった際にJSから呼び出される。
     */
    @JavascriptInterface
    fun onNextParagraphFound(paragraphId: String, text: String) {
        AppLogger.d("AndroidBridge", "onNextParagraphFound: id=$paragraphId, text=${text.take(15)}...")
        onNextParagraph(paragraphId, text)
    }

    /**
     * ページ内の全段落を読み終えた際にJSから呼び出される。
     */
    @JavascriptInterface
    fun onEndOfEpisode() {
        AppLogger.d("AndroidBridge", "onEndOfEpisode: エピソード末尾到達")
        onEpisodeEnd()
    }

    /**
     * 次のエピソード（話）へのリンクが存在しない（最終話である）際にJSから呼び出される。
     */
    @JavascriptInterface
    fun onNoNextEpisode() {
        AppLogger.d("AndroidBridge", "onNoNextEpisode: 次話リンクなし (最終話到達)")
        onNoNextEpisode()
    }

    /**
     * 辞書登録用にWebView内で選択中のテキストを受信した際にJSから呼び出される。
     */
    @JavascriptInterface
    fun onSelectedTextReceived(text: String) {
        AppLogger.d("AndroidBridge", "onSelectedTextReceived: text=$text")
        onSelectedText(text)
    }
}
