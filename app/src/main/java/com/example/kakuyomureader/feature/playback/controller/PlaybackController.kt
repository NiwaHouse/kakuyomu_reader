package com.example.kakuyomureader.feature.playback.controller

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.kakuyomureader.core.common.AppLogger
import com.example.kakuyomureader.core.model.NovelParagraph
import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.core.model.PlaybackState
import com.example.kakuyomureader.feature.playback.service.NovelPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 再生コントローラー
 *
 * [責務]: NovelPlaybackServiceとのバインド/アンバインドを管理し、UI/ViewModelに安全な再生操作APIを提供する。
 * [影響する状態]: Service接続状態、PlaybackState。
 * [発生しうる例外・エラー]: Serviceバインド失敗時の状態管理。
 */
class PlaybackController(private val context: Context) {

    private var service: NovelPlaybackService? = null
    private var isBound: Boolean = false

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentParagraph = MutableStateFlow<NovelParagraph?>(null)
    val currentParagraph: StateFlow<NovelParagraph?> = _currentParagraph.asStateFlow()

    var onNextParagraphCallback: (() -> Unit)? = null
    var onParagraphStartedCallback: ((NovelParagraph) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            AppLogger.d("PlaybackController", "Service接続成功")
            val localBinder = binder as? NovelPlaybackService.LocalBinder
            service = localBinder?.getService()
            isBound = true

            service?.onNextParagraphRequested = {
                onNextParagraphCallback?.invoke()
            }
            service?.onParagraphStarted = { paragraph ->
                _currentParagraph.value = paragraph
                onParagraphStartedCallback?.invoke(paragraph)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            AppLogger.d("PlaybackController", "Service切断")
            service = null
            isBound = false
        }
    }

    /**
     * Serviceをフォアグラウンドサービスとして明示的に起動・常駐化する。
     */
    fun startForegroundService() {
        val intent = Intent(context, NovelPlaybackService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
        AppLogger.d("PlaybackController", "startForegroundService 実行")
    }

    /**
     * Serviceにバインドする。
     */
    fun bind() {
        if (!isBound) {
            val intent = Intent(context, NovelPlaybackService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            AppLogger.d("PlaybackController", "Serviceバインド要求発行")
        }
    }

    /**
     * Serviceからアンバインドする。
     */
    fun unbind() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            service = null
            AppLogger.d("PlaybackController", "Serviceアンバインド完了")
        }
    }

    fun speak(paragraph: NovelParagraph) {
        startForegroundService()
        bind()
        _currentParagraph.value = paragraph
        _playbackState.value = PlaybackState.PLAYING
        service?.speakParagraph(paragraph)
    }

    fun speakList(paragraphs: List<NovelParagraph>) {
        if (paragraphs.isEmpty()) return
        startForegroundService()
        bind()
        _currentParagraph.value = paragraphs.first()
        _playbackState.value = PlaybackState.PLAYING
        service?.speakParagraphs(paragraphs)
    }

    fun pause() {
        _playbackState.value = PlaybackState.PAUSED
        service?.pausePlayback()
    }

    fun resume() {
        _playbackState.value = PlaybackState.PLAYING
        service?.resumePlayback()
    }

    fun stop() {
        _playbackState.value = PlaybackState.STOPPED
        service?.stopPlayback()
    }

    fun setSpeed(speed: PlaybackSpeed) {
        service?.setSpeed(speed)
    }
}
