package com.example.kakuyomureader.feature.playback.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.kakuyomureader.core.common.AppLogger
import com.example.kakuyomureader.core.common.RubyFilter
import com.example.kakuyomureader.core.common.TextChunker
import com.example.kakuyomureader.core.model.NovelParagraph
import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.core.model.PlaybackState
import java.util.Locale

/**
 * TTSエンジン管理クラス
 *
 * [責務]: Android標準TextToSpeechの初期化、ライフサイクル管理、長文チャンク分割再生、発話進行管理を行う。
 * [影響する状態]: TTSの再生キュー、PlaybackState。
 * [発生しうる例外・エラー]: TTS初期化失敗、未サポート言語エラー。
 */
class TtsEngineManager(
    private val context: Context,
    private val listener: TtsEventListener
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isReady: Boolean = false
        private set

    private var currentSpeed: PlaybackSpeed = PlaybackSpeed.NORMAL
    private var activeParagraph: NovelParagraph? = null
    private var pendingChunks: List<String> = emptyList()
    private var currentChunkIndex: Int = 0
    private var pendingParagraphToSpeak: NovelParagraph? = null

    init {
        AppLogger.d("TtsEngineManager", "TTSエンジンの初期化を開始")
        listener.onPlaybackStateChanged(PlaybackState.INITIALIZING)
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsInstance = tts
            if (ttsInstance == null) {
                listener.onTtsError("TTSインスタンスが無効です。")
                listener.onPlaybackStateChanged(PlaybackState.ERROR)
                return
            }

            // 日本語ロケール設定 (JAPANESE または JAPAN を優先試行)
            var langResult = ttsInstance.setLanguage(Locale.JAPANESE)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                langResult = ttsInstance.setLanguage(Locale.JAPAN)
            }
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                langResult = ttsInstance.setLanguage(Locale.getDefault())
            }

            isReady = true
            applySpeechRate(currentSpeed)
            setupProgressListener()
            AppLogger.d("TtsEngineManager", "TTS初期化完了 (langResult=$langResult)")
            listener.onTtsReady()
            listener.onPlaybackStateChanged(PlaybackState.IDLE)

            // 初期化完了前にリクエストされていた段落があれば即座に再生
            pendingParagraphToSpeak?.let { paragraph ->
                pendingParagraphToSpeak = null
                speakParagraph(paragraph)
            }
        } else {
            AppLogger.e("TtsEngineManager", "TTSの初期化に失敗しました (status: $status)")
            listener.onTtsError("TTSエンジンの初期化に失敗しました (status=$status)。")
            listener.onPlaybackStateChanged(PlaybackState.ERROR)
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                AppLogger.d("TtsEngineManager", "発話開始: utteranceId=$utteranceId")
                listener.onPlaybackStateChanged(PlaybackState.PLAYING)
            }

            override fun onDone(utteranceId: String?) {
                AppLogger.d("TtsEngineManager", "発話完了: utteranceId=$utteranceId")
                handleChunkDone(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                AppLogger.e("TtsEngineManager", "発話エラー: utteranceId=$utteranceId")
                listener.onPlaybackStateChanged(PlaybackState.ERROR)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                AppLogger.e("TtsEngineManager", "発話エラー(詳細): utteranceId=$utteranceId, errorCode=$errorCode")
                listener.onPlaybackStateChanged(PlaybackState.ERROR)
            }
        })
    }

    /**
     * 段落の音声読み上げを開始する（長文はチャンク分割）。
     *
     * [責務]: テキストのルビ除去・サニタイズ後、チャンク分割して先頭チャンクから再生開始する。
     * [影響する状態]: PlaybackState.PLAYING
     * [発生しうる例外・エラー]: TTS未初期化時は初期化完了後に自動再生。
     */
    fun speakParagraph(paragraph: NovelParagraph) {
        if (!isReady || tts == null) {
            AppLogger.d("TtsEngineManager", "TTS初期化待機中のため再生キューに保持: id=${paragraph.id}")
            pendingParagraphToSpeak = paragraph
            return
        }

        stop()
        activeParagraph = paragraph

        val cleanText = RubyFilter.filterRubyAndHtml(paragraph.text)
        pendingChunks = TextChunker.splitIntoChunks(cleanText)
        currentChunkIndex = 0

        if (pendingChunks.isEmpty()) {
            listener.onParagraphCompleted(paragraph.id)
            return
        }

        speakNextChunk()
    }

    private fun speakNextChunk() {
        if (currentChunkIndex >= pendingChunks.size) {
            val completedId = activeParagraph?.id ?: ""
            AppLogger.d("TtsEngineManager", "段落全体の読み上げ完了: id=$completedId")
            listener.onPlaybackStateChanged(PlaybackState.COMPLETED)
            listener.onParagraphCompleted(completedId)
            return
        }

        val chunkText = pendingChunks[currentChunkIndex]
        val utteranceId = "chunk_${activeParagraph?.id}_$currentChunkIndex"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = tts?.speak(chunkText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            AppLogger.e("TtsEngineManager", "チャンク発話エラー: $utteranceId")
            listener.onTtsError("音声再生に失敗しました。")
        }
    }

    private fun handleChunkDone(utteranceId: String?) {
        currentChunkIndex++
        speakNextChunk()
    }

    /**
     * 音声読み上げを一時停止または停止する。
     */
    fun stop() {
        pendingParagraphToSpeak = null
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        listener.onPlaybackStateChanged(PlaybackState.STOPPED)
    }

    /**
     * 読み上げ速度を設定する。
     */
    fun setSpeechRate(speed: PlaybackSpeed) {
        currentSpeed = speed
        if (isReady) {
            applySpeechRate(speed)
        }
    }

    private fun applySpeechRate(speed: PlaybackSpeed) {
        tts?.setSpeechRate(speed.rate)
    }

    /**
     * リソースを解放する。
     */
    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
        AppLogger.d("TtsEngineManager", "TTSエンジンを破棄しました")
    }
}
