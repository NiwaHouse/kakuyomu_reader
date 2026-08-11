package com.example.kakuyomureader.feature.reader.viewmodel

import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.core.model.ReadingMode

/**
 * リーダー画面イベントSealed Interface
 *
 * [責務]: UI操作およびBridge/Serviceからの各種イベント定義。
 * [影響する状態]: ReaderViewModel内の状態遷移。
 * [発生しうる例外・エラー]: なし。
 */
sealed interface ReaderEvent {
    data class OnUrlEntered(val url: String) : ReaderEvent
    data object OnHomeClicked : ReaderEvent
    data class OnPageStarted(val url: String) : ReaderEvent
    data class OnPageFinished(val url: String, val title: String) : ReaderEvent
    data class OnProgressChanged(val progress: Int) : ReaderEvent
    data class OnParagraphTapped(val id: String, val rawText: String) : ReaderEvent
    data class OnContainerParagraphsReceived(val paragraphsJson: String) : ReaderEvent
    data object OnTogglePlayPause : ReaderEvent
    data object OnStartBackgroundPlayback : ReaderEvent
    data object OnStopPlayback : ReaderEvent
    data class OnSpeedChanged(val speed: PlaybackSpeed) : ReaderEvent
    data class OnToggleAutoPlayNext(val enabled: Boolean) : ReaderEvent
    data class OnNextParagraphReceived(val id: String, val rawText: String) : ReaderEvent
    data object OnEndOfEpisodeReached : ReaderEvent
    data object OnNoNextEpisodeReceived : ReaderEvent
    data class OnErrorDismissed(val message: String? = null) : ReaderEvent

    // 辞書イベント
    data class OnToggleDictionaryMode(val enabled: Boolean) : ReaderEvent
    data object OnRegisterDictionaryClicked : ReaderEvent
    data class OnSelectedTextReceived(val text: String) : ReaderEvent
    data class OnSaveDictionaryItem(val surface: String, val reading: String) : ReaderEvent
    data class OnDeleteDictionaryItem(val id: String) : ReaderEvent
    data object OnDismissDictionaryDialog : ReaderEvent

    // お気に入り・履歴・設定・ドロワーイベント
    data object OnToggleBookmark : ReaderEvent
    data class OnDeleteBookmark(val id: String) : ReaderEvent
    data class OnDeleteHistory(val id: String) : ReaderEvent
    data object OnClearAllHistory : ReaderEvent
    data class OnDrawerOpenChanged(val isOpen: Boolean) : ReaderEvent
    data class OnReadingModeChanged(val mode: ReadingMode) : ReaderEvent
    data class OnParentLevelsChanged(val levels: Int) : ReaderEvent
}
