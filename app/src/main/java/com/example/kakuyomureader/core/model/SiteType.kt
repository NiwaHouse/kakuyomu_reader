package com.example.kakuyomureader.core.model

/**
 * 対象小説サイト種別Enum
 *
 * [責務]: アプリが対応するWeb小説サイトまたは汎用Webページの種別を定義する。
 * [影響する状態]: 適用されるパーサー（NovelSiteParser）の決定。
 * [発生しうる例外・エラー]: なし。
 */
enum class SiteType(val displayName: String) {
    /** カクヨム (kakuyomu.jp) */
    KAKUYOMU("カクヨム"),

    /** 小説家になろう (syosetu.com / ncode.syosetu.com) */
    NAROU("小説家になろう"),

    /** 汎用Webページ */
    GENERIC("Web一般")
}
