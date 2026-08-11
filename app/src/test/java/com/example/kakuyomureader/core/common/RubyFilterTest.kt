package com.example.kakuyomureader.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RubyFilterのユニットテスト
 *
 * [責務]: HTMLルビタグ（<rt>）の除外および小説記法ルビのサニタイズが正常に動作するか検証する。
 * [影響する状態]: なし。
 * [発生しうる例外・エラー]: アサーション失敗時のAssertionError。
 */
class RubyFilterTest {

    @Test
    fun filterRubyAndHtml_removesRtTagsCorrectly() {
        val input = "<ruby>漢<rt>かん</rt>字<rt>じ</rt></ruby>"
        val actual = RubyFilter.filterRubyAndHtml(input)
        assertEquals("漢字", actual)
    }

    @Test
    fun filterRubyAndHtml_removesTextRubyNotation() {
        val input = "｜魔法陣《まほうじん》を展開した。"
        val actual = RubyFilter.filterRubyAndHtml(input)
        assertEquals("魔法陣を展開した。", actual)
    }

    @Test
    fun filterRubyAndHtml_handlesEmptyOrBlank() {
        assertEquals("", RubyFilter.filterRubyAndHtml(""))
        assertEquals("", RubyFilter.filterRubyAndHtml("   "))
    }
}
