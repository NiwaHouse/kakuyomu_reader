package com.example.kakuyomureader.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TextChunkerのユニットテスト
 *
 * [責務]: 長文が句読点で適切なチャンクに分割されるか検証する。
 * [影響する状態]: なし。
 * [発生しうる例外・エラー]: アサーション失敗時のAssertionError。
 */
class TextChunkerTest {

    @Test
    fun splitIntoChunks_splitsByPunctuation() {
        val input = "これは第一文です。これは第二文です！これは第三文ですか？"
        val chunks = TextChunker.splitIntoChunks(input, maxChunkLength = 10)

        assertTrue(chunks.size >= 2)
        assertEquals("これは第一文です。", chunks[0])
    }

    @Test
    fun splitIntoChunks_shortTextReturnsSingleChunk() {
        val input = "短いテキストです。"
        val chunks = TextChunker.splitIntoChunks(input, maxChunkLength = 50)
        assertEquals(1, chunks.size)
        assertEquals(input, chunks[0])
    }
}
