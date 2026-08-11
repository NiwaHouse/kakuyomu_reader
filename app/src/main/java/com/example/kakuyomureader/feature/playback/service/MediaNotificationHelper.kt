package com.example.kakuyomureader.feature.playback.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.kakuyomureader.KakuyomuReaderApp
import com.example.kakuyomureader.MainActivity
import com.example.kakuyomureader.R
import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.core.model.PlaybackState

/**
 * メディア通知生成ヘルパー
 *
 * [責務]: フォアグラウンドサービス用のメディアコントロール通知（再生/停止/速度/次へ）を構築する。
 * [影響する状態]: システム通知領域およびロックスクリーン。
 * [発生しうる例外・エラー]: なし。
 */
class MediaNotificationHelper(private val context: Context) {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.example.kakuyomureader.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.kakuyomureader.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.kakuyomureader.ACTION_STOP"
        const val ACTION_NEXT = "com.example.kakuyomureader.ACTION_NEXT"
        const val ACTION_CYCLE_SPEED = "com.example.kakuyomureader.ACTION_CYCLE_SPEED"
    }

    /**
     * 再生状態・段落テキスト・速度に応じたNotificationを構築する。
     */
    fun buildNotification(
        readingText: String,
        playbackState: PlaybackState,
        playbackSpeed: PlaybackSpeed,
        mediaSession: MediaSessionCompat
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPlaying = playbackState == PlaybackState.PLAYING

        // 1. 再生/一時停止アクション
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "一時停止",
                createServicePendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "再生",
                createServicePendingIntent(ACTION_PLAY)
            )
        }

        // 2. 次の段落アクション
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "次へ",
            createServicePendingIntent(ACTION_NEXT)
        )

        // 3. 速度切り替えアクション
        val speedAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_rotate,
            "速度: ${playbackSpeed.displayString}",
            createServicePendingIntent(ACTION_CYCLE_SPEED)
        )

        // 4. 停止アクション
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "停止",
            createServicePendingIntent(ACTION_STOP)
        )

        val title = if (isPlaying) "小説読み上げ中 (${playbackSpeed.displayString})" else "読み上げ一時停止中"
        val displayText = readingText.ifBlank { "待機中" }

        return NotificationCompat.Builder(context, KakuyomuReaderApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(displayText)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(isPlaying || playbackState == PlaybackState.PAUSED)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(speedAction)
            .addAction(stopAction)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // コンパクト表示時に再生/次へ/速度を表示
            )
            .build()
    }

    private fun createServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(context, NovelPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
