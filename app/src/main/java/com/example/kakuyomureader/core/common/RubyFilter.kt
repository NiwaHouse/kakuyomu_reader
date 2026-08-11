package com.example.kakuyomureader.core.common

/**
 * ルビ表記・HTMLタグ補助除去フィルター
 *
 * [責務]: JavaScript側でのルビ除去に加え、Kotlin層でも残存したHTMLタグやなろう形式のルビ記号をサニタイズする。
 * [影響する状態]: 発話テキストの整形。
 * [発生しうる例外・エラー]: なし。
 */
object RubyFilter {

    // HTMLの<rt>〜</rt>タグおよびその中身にマッチする正規表現
    private val RT_TAG_REGEX = Regex("<rt[^>]*>.*?</rt>", RegexOption.IGNORE_CASE)

    // その他のHTMLタグ全般にマッチする正規表現
    private val HTML_TAG_REGEX = Regex("<[^>]+>")

    // 小説テキストルビ表記: |漢字《かんじ》, ｜漢字《かんじ》 -> 漢字
    private val TEXT_RUBY_REGEX = Regex("[|｜]?([^|｜《]+)《[^》]+》")

    /**
     * テキストからHTMLタグやルビタグを除去してプレーンテキストにする。
     *
     * [責務]: 文字列をTTS読み上げに適した形に整形する。
     * [影響する状態]: なし。
     * [発生しうる例外・エラー]: なし。
     */
    fun filterRubyAndHtml(input: String): String {
        if (input.isBlank()) return ""

        var result = input
        // 1. <rt>〜</rt>を中身ごと除去
        result = RT_TAG_REGEX.replace(result, "")
        // 2. 残りのHTMLタグ（<ruby>, <p>, <span>等）のタグ自体を除去
        result = HTML_TAG_REGEX.replace(result, "")
        // 3. テキストルビ記法 (|漢字《かんじ》, ｜漢字《かんじ》) を漢字のみに置換
        result = TEXT_RUBY_REGEX.replace(result, "$1")

        // 4. 余分な連続空白や改行を正規化
        return result.trim()
    }
}
