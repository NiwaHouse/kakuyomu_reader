package com.example.kakuyomureader.core.model

/**
 * 小説の段落データモデル
 *
 * [責務]: DOMから抽出された1つの段落情報（ID、読み上げ用テキスト、原文、インデックス、サイト種別）を保持する。
 * [影響する状態]: UI上の読み上げテキストプレビュー、ハイライト対象要素の特定、TTSへの発話テキスト供給。
 * [発生しうる例外・エラー]: なし。
 */
data class NovelParagraph(
    /** DOM要素の一意識別子（例: "p_12", "widget-episode-body-3"） */
    val id: String,

    /** ルビ（<rt>）等を除去したクリーンな読み上げ用テキスト */
    val text: String,

    /** ルビ等を含む原文テキスト（プレビュー・デバッグ用） */
    val rawText: String = text,

    /** ページ内での段落インデックス（0開始） */
    val index: Int = 0,

    /** 抽出元の小説サイト種別 */
    val siteType: SiteType = SiteType.GENERIC
) {
    /**
     * テキストが空でないか判定
     */
    val isNotEmpty: Boolean
        get() = text.trim().isNotEmpty()

    /**
     * 表示用の省略テキスト（先頭30文字）
     */
    val shortPreview: String
        get() = if (text.length > 30) "${text.take(30)}..." else text
}
