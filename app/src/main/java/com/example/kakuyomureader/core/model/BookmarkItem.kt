package com.example.kakuyomureader.core.model

/**
 * お気に入り（ブックマーク）データモデル
 *
 * [責務]: ユーザーが保存したWebページのタイトル、URL、登録日時の情報を保持する。
 * [影響する状態]: なし (イミュータブル)。
 * [発生しうる例外・エラー]: なし。
 */
data class BookmarkItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)
