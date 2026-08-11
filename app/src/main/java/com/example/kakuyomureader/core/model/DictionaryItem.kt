package com.example.kakuyomureader.core.model

import java.util.UUID

/**
 * ユーザー辞書データモデル (Core Model)
 *
 * [責務]: 置換対象の表記（単語）と読み（よみ）のペアおよび一意なID・作成日時を保持する。
 * [影響する状態]: なし。
 * [発生しうる例外・エラー]: なし。
 */
data class DictionaryItem(
    val id: String = UUID.randomUUID().toString(),
    val surface: String,
    val reading: String,
    val createdAt: Long = System.currentTimeMillis()
)
