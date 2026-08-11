package com.example.kakuyomureader.feature.playback.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import com.example.kakuyomureader.core.common.AppLogger
import com.example.kakuyomureader.core.model.NovelParagraph
import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.core.model.PlaybackState
import com.example.kakuyomureader.feature.playback.tts.TtsEngineManager
import com.example.kakuyomureader.feature.playback.tts.TtsEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 音声読み上げフォアグラウンドサービス
 *
 * [責務]: アプリのバックグラウンド移行時もTTS再生を維持し、MediaSessionによる通知・ロック画面操作を提供する。
 * [影響する状態]: フォアグラウンド通知、グローバル再生状態（playbackStateFlow）。
 * [発生しうる例外・エラー]: TTSエラー、Serviceバインド解除時のリソース解放漏れ。
 */
class NovelPlaybackService : Service(), TtsEventListener {

    inner class LocalBinder : Binder() {
        fun getService(): NovelPlaybackService = this@NovelPlaybackService
    }

    private val binder = LocalBinder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var ttsEngine: TtsEngineManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationHelper: MediaNotificationHelper

    private val _playbackStateFlow = MutableStateFlow(PlaybackState.IDLE)
    val playbackStateFlow: StateFlow<PlaybackState> = _playbackStateFlow.asStateFlow()

    private val _currentParagraphFlow = MutableStateFlow<NovelParagraph?>(null)
    val currentParagraphFlow: StateFlow<NovelParagraph?> = _currentParagraphFlow.asStateFlow()

    private var currentSpeed: PlaybackSpeed = PlaybackSpeed.NORMAL
    private var paragraphQueue: MutableList<NovelParagraph> = mutableListOf()
    var onParagraphStarted: ((NovelParagraph) -> Unit)? = null
    var onNextParagraphRequested: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.d("NovelPlaybackService", "Service onCreate: 初期化開始")
        notificationHelper = MediaNotificationHelper(this)
        ttsEngine = TtsEngineManager(this, this)
        setupMediaSession()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "NovelPlaybackSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { resumePlayback() }
                override fun onPause() { pausePlayback() }
                override fun onStop() { stopPlayback() }
                override fun onSkipToNext() { skipToNextParagraph() }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateForegroundNotification(_playbackStateFlow.value)
        when (intent?.action) {
            MediaNotificationHelper.ACTION_PLAY -> resumePlayback()
            MediaNotificationHelper.ACTION_PAUSE -> pausePlayback()
            MediaNotificationHelper.ACTION_STOP -> stopPlayback()
            MediaNotificationHelper.ACTION_NEXT -> skipToNextParagraph()
            MediaNotificationHelper.ACTION_CYCLE_SPEED -> cyclePlaybackSpeed()
        }
        return START_NOT_STICKY
    }

    fun skipToNextParagraph() {
        ttsEngine.stop()
        playNextInQueueOrRequest()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun speakParagraph(paragraph: NovelParagraph) {
        paragraphQueue.clear()
        playSingleParagraph(paragraph)
    }

    fun speakParagraphs(paragraphs: List<NovelParagraph>) {
        paragraphQueue = paragraphs.toMutableList()
        playNextInQueueOrRequest()
    }

    fun cyclePlaybackSpeed() {
        val nextRate = when (currentSpeed.rate) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            2.0f -> 0.8f
            else -> 1.0f
        }
        val newSpeed = PlaybackSpeed.fromFloatClamped(nextRate)
        setSpeed(newSpeed)
    }

    fun setSpeed(speed: PlaybackSpeed) {
        currentSpeed = speed
        ttsEngine.setSpeechRate(speed)
        updateForegroundNotification(_playbackStateFlow.value)

        // 現在再生中かつ対象段落が存在する場合、キューを保持したまま現在段落を新速度で即時言い直し発話
        if (_playbackStateFlow.value == PlaybackState.PLAYING) {
            val current = _currentParagraphFlow.value
            if (current != null) {
                AppLogger.d("NovelPlaybackService", "再生中スピード変更検出: 新速度 ${speed.displayString} で現在段落を言い直し再発話 (キュー保持: size=${paragraphQueue.size})")
                playSingleParagraph(current)
            }
        }
    }

    private fun playNextInQueueOrRequest() {
        if (paragraphQueue.isNotEmpty()) {
            val nextParagraph = paragraphQueue.removeAt(0)
            playSingleParagraph(nextParagraph)
            onParagraphStarted?.invoke(nextParagraph)
        } else {
            if (_currentParagraphFlow.value?.id == "end_of_episode_notification") {
                stopPlayback()
            } else if (_currentParagraphFlow.value != null) {
                val endParagraph = NovelParagraph(
                    id = "end_of_episode_notification",
                    text = "読み上げが終了しました",
                    rawText = "読み上げが終了しました",
                    index = -1,
                    siteType = _currentParagraphFlow.value?.siteType ?: com.example.kakuyomureader.core.model.SiteType.GENERIC
                )
                playSingleParagraph(endParagraph)
            } else {
                onNextParagraphRequested?.invoke()
            }
        }
    }

    private fun playSingleParagraph(paragraph: NovelParagraph) {
        _currentParagraphFlow.value = paragraph
        _playbackStateFlow.value = PlaybackState.PLAYING
        ttsEngine.speakParagraph(paragraph)
        updateForegroundNotification(PlaybackState.PLAYING)
    }

    fun pausePlayback() {
        ttsEngine.stop()
        _playbackStateFlow.value = PlaybackState.PAUSED
        updateForegroundNotification(PlaybackState.PAUSED)
    }

    fun resumePlayback() {
        _currentParagraphFlow.value?.let { playSingleParagraph(it) }
    }

    fun stopPlayback() {
        paragraphQueue.clear()
        ttsEngine.stop()
        _playbackStateFlow.value = PlaybackState.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateForegroundNotification(state: PlaybackState) {
        val text = _currentParagraphFlow.value?.shortPreview ?: ""
        val notification = notificationHelper.buildNotification(text, state, currentSpeed, mediaSession)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                MediaNotificationHelper.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(MediaNotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    // TtsEventListener 実装
    override fun onTtsReady() {
        AppLogger.d("NovelPlaybackService", "TTSエンジン準備完了")
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        _playbackStateFlow.value = state
        if (state == PlaybackState.STOPPED || state == PlaybackState.IDLE) {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }

    override fun onParagraphCompleted(paragraphId: String) {
        AppLogger.d("NovelPlaybackService", "段落再生完了: $paragraphId")
        mainHandler.post {
            if (paragraphId == "end_of_episode_notification") {
                stopPlayback()
            } else {
                playNextInQueueOrRequest()
            }
        }
    }

    override fun onTtsError(errorMessage: String) {
        AppLogger.e("NovelPlaybackService", "TTSエラー: $errorMessage")
        _playbackStateFlow.value = PlaybackState.ERROR
    }

    override fun onDestroy() {
        paragraphQueue.clear()
        ttsEngine.shutdown()
        mediaSession.release()
        super.onDestroy()
        AppLogger.d("NovelPlaybackService", "Service onDestroy: リソース解放完了")
    }
}
