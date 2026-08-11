package com.example.kakuyomureader.core.common

import com.example.kakuyomureader.core.model.DictionaryItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DictionaryFilterのユニットテスト
 *
 * [責務]: ユーザー辞書による文字置換および長文単語優先置換が正常に動作するか検証する。
 * [影響する状態]: なし。
 * [発生しうる例外・エラー]: アサーション失敗時のAssertionError。
 */
class DictionaryFilterTest {

    @Test
    fun applyDictionary_replacesSurfaceWithReading() {
        val dict = listOf(
            DictionaryItem(surface = "魔王", reading = "まおう"),
            DictionaryItem(surface = "勇者", reading = "ゆうしゃ")
        )
        val input = "魔王と勇者が対峙した。"
        val actual = DictionaryFilter.applyDictionary(input, dict)
        assertEquals("まおうとゆうしゃが対峙した。", actual)
    }

    @Test
    fun applyDictionary_prioritizesLongerSurfaces() {
        val dict = listOf(
            DictionaryItem(surface = "魔王", reading = "まおう"),
            DictionaryItem(surface = "魔王城", reading = "まおうじょう")
        )
        val input = "魔王城へ向かう。"
        val actual = DictionaryFilter.applyDictionary(input, dict)
        assertEquals("まおうじょうへ向かう。", actual)
    }

    @Test
    fun applyDictionary_returnsOriginalTextIfEmpty() {
        val input = "テスト本文"
        assertEquals(input, DictionaryFilter.applyDictionary(input, emptyList()))
    }
}
