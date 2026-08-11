package com.example.kakuyomureader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.kakuyomureader.core.common.AppLogger

/**
 * アプリケーションクラス
 *
 * [責務]: アプリ起動時のグローバル初期化（通知チャネル生成、ログ設定等）を行う。
 * [影響する状態]: システムの通知チャネル設定。
 * [発生しうる例外・エラー]: NotificationManager取得失敗時のNullPointerException。
 */
class KakuyomuReaderApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "novel_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.d("KakuyomuReaderApp", "Application onCreate: アプリケーション初期化開始")
        createNotificationChannel()
    }

    /**
     * 音声読み上げ通知用の通知チャネルを生成する。
     *
     * [責務]: Android 8.0 (API 26) 以降向けのフォアグラウンドサービス通知チャネルを登録する。
     * [影響する状態]: システム通知マネージャー。
     * [発生しうる例外・エラー]: なし。
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT // 通知パネルに確実に表示
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            AppLogger.d("KakuyomuReaderApp", "通知チャネル作成完了: $NOTIFICATION_CHANNEL_ID")
        }
    }
}
