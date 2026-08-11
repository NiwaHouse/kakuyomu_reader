package com.example.kakuyomureader.core.common

import android.util.Log

/**
 * 統一ロガーユーティリティ
 *
 * [責務]: アプリ全体のログ出力を一元管理し、開発/リリース時のログ制御を可能にする。
 * [影響する状態]: Logcatへの出力。
 * [発生しうる例外・エラー]: なし。
 */
object AppLogger {

    private const val GLOBAL_TAG = "KakuyomuReader"
    var isDebugEnabled: Boolean = true

    /**
     * デバッグログ出力
     */
    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d("$GLOBAL_TAG:$tag", message)
        }
    }

    /**
     * 情報ログ出力
     */
    fun i(tag: String, message: String) {
        Log.i("$GLOBAL_TAG:$tag", message)
    }

    /**
     * 警告ログ出力
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$GLOBAL_TAG:$tag", message, throwable)
        } else {
            Log.w("$GLOBAL_TAG:$tag", message)
        }
    }

    /**
     * エラーログ出力
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$GLOBAL_TAG:$tag", message, throwable)
        } else {
            Log.e("$GLOBAL_TAG:$tag", message)
        }
    }
}
