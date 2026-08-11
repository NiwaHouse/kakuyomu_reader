package com.example.kakuyomureader.core.model

/**
 * 閲覧履歴データモデル
 *
 * [責務]: ユーザーがアクセスしたWebページのタイトル、URL、閲覧日時の情報を保持する。
 * [影響する状態]: なし (イミュータブル)。
 * [発生しうる例外・エラー]: なし。
 */
data class HistoryItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)
