package com.example.kakuyomureader.feature.siteparser.api

import com.example.kakuyomureader.feature.siteparser.generic.GenericSiteParser
import com.example.kakuyomureader.feature.siteparser.kakuyomu.KakuyomuSiteParser
import com.example.kakuyomureader.feature.siteparser.narou.NarouSiteParser

/**
 * サイトパーサーレジストリ
 *
 * [責務]: 登録されたパーサー群を管理し、URLに最適なパーサーを動的に解決・返却する。
 * [影響する状態]: なし。
 * [発生しうる例外・エラー]: なし（マッチしない場合はGenericSiteParserを返却）。
 */
class ParserRegistry(
    private val parsers: List<NovelSiteParser> = listOf(
        KakuyomuSiteParser(),
        NarouSiteParser(),
        GenericSiteParser()
    )
) {

    /**
     * URLに応じた最適なNovelSiteParserを解決する。
     *
     * [責務]: 登録パーサーを順次走査し、canHandleがtrueを返すパーサーを返却する。
     * [影響する状態]: なし。
     * [発生しうる例外・エラー]: なし。
     */
    fun resolve(url: String): NovelSiteParser {
        return parsers.firstOrNull { it.canHandle(url) } ?: GenericSiteParser()
    }
}
