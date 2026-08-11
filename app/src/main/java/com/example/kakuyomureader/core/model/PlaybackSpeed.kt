package com.example.kakuyomureader.core.model

/**
 * 読み上げ速度Value Class
 *
 * [責務]: 0.5倍速〜3.0倍速の安全なTTS再生速度をカプセル化する。
 * [影響する状態]: TTSエンジンの `setSpeechRate` パラメータ。
 * [発生しうる例外・エラー]: なし（範囲外の値は自動的にクランプされる）。
 */
data class PlaybackSpeed(val rate: Float) {

    init {
        require(rate in MIN_SPEED..MAX_SPEED) {
            "再生速度は $MIN_SPEED 〜 $MAX_SPEED の範囲である必要があります: $rate"
        }
    }

    companion object {
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 3.0f
        val SLOW = PlaybackSpeed(0.8f)
        val NORMAL = PlaybackSpeed(1.0f)
        val FAST = PlaybackSpeed(1.2f)
        val VERY_FAST = PlaybackSpeed(1.5f)

        /**
         * 範囲内にクランプしたPlaybackSpeedを生成する。
         *
         * [責務]: 任意浮動小数点数を安全な再生速度値に変換する。
         * [影響する状態]: なし。
         * [発生しうる例外・エラー]: なし。
         */
        fun fromFloatClamped(value: Float): PlaybackSpeed {
            val clamped = value.coerceIn(MIN_SPEED, MAX_SPEED)
            return PlaybackSpeed(clamped)
        }
    }

    /** 表示用フォーマット文字列（例: "1.2x"） */
    val displayString: String
        get() = String.format("%.1fx", rate)
}
