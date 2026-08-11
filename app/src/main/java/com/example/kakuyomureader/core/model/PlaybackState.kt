package com.example.kakuyomureader.core.model

/**
 * 音声読み上げ再生状態Enum
 *
 * [責務]: TTSエンジンおよびフォアグラウンドサービスにおける現在の再生状態を表現する。
 * [影響する状態]: UI上の再生・一時停止ボタン表示、通知パネルのボタンアイコン、自動再生ループ制御。
 * [発生しうる例外・エラー]: なし。
 */
enum class PlaybackState {
    /** 待機状態（初期状態、未再生） */
    IDLE,

    /** TTSエンジンの初期化中 */
    INITIALIZING,

    /** 音声読み上げ再生中 */
    PLAYING,

    /** 一時停止中 */
    PAUSED,

    /** 停止中（リセット済み） */
    STOPPED,

    /** 段落の読み上げ完了 */
    COMPLETED,

    /** エラー発生状態 */
    ERROR
}
