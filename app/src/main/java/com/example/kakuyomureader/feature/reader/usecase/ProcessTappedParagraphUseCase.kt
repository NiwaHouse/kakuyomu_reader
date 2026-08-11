package com.example.kakuyomureader.feature.reader.usecase

import com.example.kakuyomureader.core.common.DictionaryFilter
import com.example.kakuyomureader.core.common.RubyFilter
import com.example.kakuyomureader.core.model.DictionaryItem
import com.example.kakuyomureader.core.model.NovelParagraph
import com.example.kakuyomureader.core.model.SiteType

/**
 * タップ段落処理ユースケース
 *
 * [責務]: JSから渡された段落ID・テキストを検証・サニタイズ・辞書置換し、ドメインモデルNovelParagraphを生成する。
 * [影響する状態]: なし。
 * [発生しうる例外・エラー]: なし。
 */
class ProcessTappedParagraphUseCase {

    operator fun invoke(
        id: String,
        rawText: String,
        siteType: SiteType,
        dictionary: List<DictionaryItem> = emptyList()
    ): NovelParagraph? {
        val rubyFiltered = RubyFilter.filterRubyAndHtml(rawText)
        val cleanText = DictionaryFilter.applyDictionary(rubyFiltered, dictionary)
        if (cleanText.isBlank()) return null

        return NovelParagraph(
            id = id,
            text = cleanText,
            rawText = rawText,
            siteType = siteType
        )
    }
}
