package com.example.kakuyomureader.feature.playback.usecase

import com.example.kakuyomureader.core.model.NovelParagraph
import com.example.kakuyomureader.feature.playback.controller.PlaybackController

/**
 * 段落読み上げ開始ユースケース
 *
 * [責務]: 指定された段落の音声読み上げをコントローラー経由でトリガーする。
 * [影響する状態]: PlaybackState (PLAYING), currentParagraph。
 * [発生しうる例外・エラー]: なし。
 */
class SpeakParagraphUseCase(private val controller: PlaybackController) {

    operator fun invoke(paragraph: NovelParagraph) {
        if (paragraph.isNotEmpty) {
            controller.speak(paragraph)
        }
    }
}
