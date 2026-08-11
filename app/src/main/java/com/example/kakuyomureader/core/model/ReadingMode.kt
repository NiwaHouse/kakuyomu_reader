package com.example.kakuyomureader.core.model

/**
 * 読み上げ方式モード
 *
 * [責務]: 読み上げの抽出・進行アルゴリズム（コンテナ一括 / 段落順次）を定義する。
 * [影響する状態]: ReaderUiState。
 * [発生しうる例外・エラー]: なし。
 */
enum class ReadingMode(val displayName: String, val description: String) {
    /**
     * タイトル/コンテナ一括読みモード
     * 親コンテナ内の本文段落を一括抽出し、アプリ側のキューで連続再生（画面OFF時も安定）。
     */
    CONTAINER("コンテナ一括読み (推奨)", "親要素から本文を一括抽出し、画面OFFでも途切れず再生します"),

    /**
     * 段落クリック順次モード
     * 段落単位でハイライトとスクロール追従を行いながら順次読み進める。
     */
    PARAGRAPH("段落クリック順次", "タップした段落から1行ずつハイライト追従して読みます")
}
