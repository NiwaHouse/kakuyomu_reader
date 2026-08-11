package com.example.kakuyomureader.feature.reader.viewmodel

import com.example.kakuyomureader.core.model.BookmarkItem
import com.example.kakuyomureader.core.model.DictionaryItem
import com.example.kakuyomureader.core.model.HistoryItem
import com.example.kakuyomureader.core.model.NovelParagraph
import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.core.model.PlaybackState
import com.example.kakuyomureader.core.model.ReadingMode
import com.example.kakuyomureader.core.model.SiteType

/**
 * リーダー画面UI状態イミュータブルデータクラス
 *
 * [責務]: リーダー画面（WebView、上部バー、下部再生バー、ドロワー、設定）の全表示状態を保持する。
 * [影響する状態]: Jetpack Compose UIの再描画。
 * [発生しうる例外・エラー]: なし。
 */
data class ReaderUiState(
    /** 現在ロード中のURL */
    val currentUrl: String = "https://kakuyomu.jp/",

    /** Webページのタイトル */
    val pageTitle: String = "",

    /** Webページの読込中フラグ */
    val isLoadingWebPage: Boolean = false,

    /** 読込進捗 (0-100) */
    val webPageProgress: Int = 0,

    /** 現在読み上げ中の段落 */
    val currentParagraph: NovelParagraph? = null,

    /** 音声再生状態 */
    val playbackState: PlaybackState = PlaybackState.IDLE,

    /** 読み上げ速度 */
    val playbackSpeed: PlaybackSpeed = PlaybackSpeed.NORMAL,

    /** 自動連続読み上げ有効フラグ */
    val isAutoPlayNext: Boolean = true,

    /** 読み上げ方式モード (CONTAINER / PARAGRAPH) */
    val readingMode: ReadingMode = ReadingMode.CONTAINER,

    /** 親コンテナ遡り階層数 (1〜5) */
    val parentLevels: Int = 2,

    /** お気に入り一覧 */
    val bookmarks: List<BookmarkItem> = emptyList(),

    /** 閲覧履歴一覧 */
    val history: List<HistoryItem> = emptyList(),

    /** 現在のページがお気に入り登録済みか */
    val isCurrentPageBookmarked: Boolean = false,

    /** ドロワー開閉フラグ */
    val isDrawerOpen: Boolean = false,

    /** 辞書登録モードフラグ（ON時はスクロール追従および段落タップ再生が無効化） */
    val isDictionaryMode: Boolean = false,

    /** 登録済み辞書アイテム一覧 */
    val dictionaryList: List<DictionaryItem> = emptyList(),

    /** 辞書登録ダイアログ表示フラグ */
    val isDictionaryDialogOpen: Boolean = false,

    /** 辞書登録用に選択されたテキスト */
    val selectedTextForDictionary: String = "",

    /** WebViewの「戻る」が可能か */
    val canGoBack: Boolean = false,

    /** WebViewの「進む」が可能か */
    val canGoForward: Boolean = false,

    /** 適用中のサイト種別 */
    val activeSiteType: SiteType = SiteType.KAKUYOMU,

    /** エラー通知メッセージ */
    val errorMessage: String? = null
)
