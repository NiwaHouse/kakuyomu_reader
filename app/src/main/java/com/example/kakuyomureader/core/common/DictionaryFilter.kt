package com.example.kakuyomureader.core.common

import com.example.kakuyomureader.core.model.DictionaryItem

/**
 * ユーザー辞書自動置換ユーティリティ
 *
 * [責務]: 登録済み辞書データ（文字数の長い単語を優先）に基づいて本文テキストを「読み」に置換する。
 * [影響する状態]: 発話用テキストの生成。
 * [発生しうる例外・エラー]: なし。
 */
object DictionaryFilter {

    /**
     * テキスト内の辞書単語を「読み」へ置換する。
     *
     * [責務]: 長い文字列の単語から優先的に一括置換を行う。
     * [影響する状態]: なし。
     * [発生しうる例外・エラー]: なし。
     */
    fun applyDictionary(text: String, dictionary: List<DictionaryItem>): String {
        if (text.isBlank() || dictionary.isEmpty()) return text

        var result = text
        // 文字数が長い単語から順にソートして部分一致の誤置換を防ぐ
        val sortedDict = dictionary
            .filter { it.surface.isNotBlank() && it.reading.isNotBlank() }
            .sortedByDescending { it.surface.length }

        for (item in sortedDict) {
            if (result.contains(item.surface)) {
                result = result.replace(item.surface, item.reading)
            }
        }
        return result
    }
}
