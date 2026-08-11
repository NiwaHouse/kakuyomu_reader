package com.example.kakuyomureader.feature.playback.usecase

import com.example.kakuyomureader.core.model.PlaybackState
import com.example.kakuyomureader.feature.playback.controller.PlaybackController

/**
 * 再生 / 一時停止トグル制御ユースケース
 *
 * [責務]: 現在の再生状態に応じて、再生再開または一時停止の適切な操作を実行する。
 * [影響する状態]: PlaybackState (PLAYING ⇔ PAUSED)。
 * [発生しうる例外・エラー]: なし。
 */
class TogglePlaybackUseCase(private val controller: PlaybackController) {

    operator fun invoke(currentState: PlaybackState) {
        when (currentState) {
            PlaybackState.PLAYING -> controller.pause()
            PlaybackState.PAUSED, PlaybackState.STOPPED, PlaybackState.IDLE -> controller.resume()
            else -> { /* 初期化中やエラー時は何もしない */ }
        }
    }
}
