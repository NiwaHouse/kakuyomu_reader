package com.example.kakuyomureader.feature.playback.usecase

import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.feature.playback.controller.PlaybackController

/**
 * 読み上げ速度変更ユースケース
 *
 * [責務]: 速度変更要求を安全にコントローラー・TTSエンジンへ反映する。
 * [影響する状態]: TTS再生速度。
 * [発生しうる例外・エラー]: なし。
 */
class ChangeSpeedUseCase(private val controller: PlaybackController) {

    operator fun invoke(speed: PlaybackSpeed) {
        controller.setSpeed(speed)
    }
}
