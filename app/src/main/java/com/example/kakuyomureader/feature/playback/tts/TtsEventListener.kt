package com.example.kakuyomureader.feature.playback.tts

import com.example.kakuyomureader.core.model.PlaybackState

/**
 * TTSイベント通知リスナー
 *
 * [責務]: TTSエンジンの初期化完了、発話開始、段落完了、エラー発生イベントを上位レイヤーに通知する。
 * [影響する状態]: 再生状態（PlaybackState）の更新。
 * [発生しうる例外・エラー]: なし。
 */
interface TtsEventListener {

    /**
     * TTSの準備完了通知
     */
    fun onTtsReady()

    /**
     * 再生状態の変更通知
     */
    fun onPlaybackStateChanged(state: PlaybackState)

    /**
     * 1つの段落の発話完了通知（自動次段落再生トリガー）
     */
    fun onParagraphCompleted(paragraphId: String)

    /**
     * TTSエラー発生通知
     */
    fun onTtsError(errorMessage: String)
}
