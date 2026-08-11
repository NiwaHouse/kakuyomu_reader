package com.example.kakuyomureader.feature.reader.usecase

import com.example.kakuyomureader.feature.siteparser.api.NovelSiteParser
import com.example.kakuyomureader.feature.siteparser.api.ParserRegistry

/**
 * WebViewスクリプト生成・注入ユースケース
 *
 * [責務]: ロードされたURLに対応するパーサーを取得し、注入用JS・ハイライトJS・次段落取得JSを提供する。
 * [影響する状態]: WebViewのスクリプト実行内容。
 * [発生しうる例外・エラー]: なし。
 */
class WebViewScriptInjectorUseCase(private val registry: ParserRegistry = ParserRegistry()) {

    fun getParserForUrl(url: String): NovelSiteParser {
        return registry.resolve(url)
    }

    fun getInjectionScript(url: String): String {
        return registry.resolve(url).getInjectionScript()
    }

    fun getHighlightScript(url: String, paragraphId: String): String {
        return registry.resolve(url).getHighlightScript(paragraphId)
    }

    fun getNextParagraphScript(url: String, currentParagraphId: String): String {
        return registry.resolve(url).getNextParagraphScript(currentParagraphId)
    }

    fun getClearHighlightScript(url: String): String {
        return registry.resolve(url).getClearHighlightScript()
    }

    fun getNextEpisodeScript(url: String): String {
        return registry.resolve(url).getNextEpisodeScript()
    }
}
