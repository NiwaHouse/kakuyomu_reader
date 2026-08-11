package com.example.kakuyomureader.feature.siteparser.api

import com.example.kakuyomureader.core.model.SiteType

/**
 * サイト別パーサー共通インターフェース (Plugin IF)
 *
 * [責務]: 各Web小説サイトのDOM構造、ルビ除去ルール、ハイライト処理、次段落取得JSの定義を提供する。
 * [影響する状態]: WebViewへのJavaScript注入スクリプトの生成。
 * [発生しうる例外・エラー]: なし。
 */
interface NovelSiteParser {

    /** サイト種別 */
    val siteType: SiteType

    /**
     * 指定されたURLを本パーサーで処理可能か判定する。
     *
     * [責務]: URLのドメイン・パスパターンを検証する。
     * [影響する状態]: なし。
     * [発生しうる例外・エラー]: なし。
     */
    fun canHandle(url: String): Boolean

    /**
     * ページ読み込み完了時にWebViewへ注入するクリック監視・段落抽出JavaScriptを生成する。
     *
     * [責務]: タップイベントのフック、ルビ(<rt>)の除外、クリーンテキスト抽出、Bridge通知を行うJSコードを返却する。
     * [影響する状態]: WebView内のDOMイベントリスナー。
     * [発生しうる例外・エラー]: なし。
     */
    fun getInjectionScript(): String

    /**
     * 指定した段落要素を視覚的にハイライト（背景色変更）するJavaScriptを生成する。
     *
     * [責務]: 読み上げ中の段落をユーザーに明示する。
     * [影響する状態]: WebView内の要素スタイル。
     * [発生しうる例外・エラー]: なし。
     */
    fun getHighlightScript(paragraphId: String): String

    /**
     * 現在の段落から次の段落要素を取得して読み上げを開始するJavaScriptを生成する。
     *
     * [責務]: 自動連続再生のための次ノード探索ロジックを提供する。
     * [影響する状態]: JS Bridge経由での次段落コールバック発火。
     * [発生しうる例外・エラー]: なし。
     */
    fun getNextParagraphScript(currentParagraphId: String): String

    /**
     * ハイライトを全解除するJavaScriptを生成する。
     *
     * [責務]: 停止時やリセット時にDOMのハイライトを消去する。
     * [影響する状態]: WebView内の要素スタイル。
     * [発生しうる例外・エラー]: なし。
     */
    fun getClearHighlightScript(): String

    /**
     * 次のエピソード（話）へのリンクを探索し、自動移動または次話不存在通知を行うJavaScriptを生成する。
     *
     * [責務]: 自動連続再生におけるエピソード間ナビゲーションロジックを提供する。
     * [影響する状態]: WebViewのページ遷移またはJS Bridge経由のonNoNextEpisode呼び出し。
     * [発生しうる例外・エラー]: なし。
     */
    fun getNextEpisodeScript(): String
}
