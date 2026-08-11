package com.example.kakuyomureader.core.common

/**
 * 長文チャンキング（分割）ユーティリティ
 *
 * [責務]: TTSエンジンのバッファ溢れや発話途切れを防ぐため、長文テキストを句読点や改行で安全な長さに分割する。
 * [影響する状態]: TTSキューへの分割追加単位。
 * [発生しうる例外・エラー]: なし。
 */
object TextChunker {

    // 推奨チャンク文字数（約120文字）
    private const val DEFAULT_MAX_CHUNK_LENGTH = 120

    // 分割の区切りとなる文字
    private val DELIMITERS = charArrayOf('。', '！', '!', '？', '?', '\n')

    /**
     * テキストを安全な長さのチャンクリストに分割する。
     *
     * [責務]: 句読点等の自然な文末区切りを優先しつつ、最大文字数を超えないように文章を分割する。
     * [影響する状態]: なし。
     * [発生しうる例外・エラー]: なし。
     */
    fun splitIntoChunks(text: String, maxChunkLength: Int = DEFAULT_MAX_CHUNK_LENGTH): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxChunkLength) return listOf(trimmed)

        val chunks = mutableListOf<String>()
        var remaining = trimmed

        while (remaining.isNotEmpty()) {
            if (remaining.length <= maxChunkLength) {
                chunks.add(remaining)
                break
            }

            // maxChunkLength以内の最後の区切り文字を探す
            var splitIndex = -1
            for (i in (maxChunkLength - 1) downTo 1) {
                if (DELIMITERS.contains(remaining[i])) {
                    splitIndex = i + 1 // 区切り文字を含めて分割
                    break
                }
            }

            // 区切り文字が見つからない場合はカンマや読点（、）を探す
            if (splitIndex == -1) {
                for (i in (maxChunkLength - 1) downTo 1) {
                    if (remaining[i] == '、' || remaining[i] == ',') {
                        splitIndex = i + 1
                        break
                    }
                }
            }

            // それでも見つからない場合は最大長で強制分割
            if (splitIndex == -1) {
                splitIndex = maxChunkLength
            }

            val chunk = remaining.substring(0, splitIndex).trim()
            if (chunk.isNotEmpty()) {
                chunks.add(chunk)
            }
            remaining = remaining.substring(splitIndex).trim()
        }

        return chunks
    }
}
