package com.example.kakuyomureader.feature.reader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kakuyomureader.core.common.AppLogger
import com.example.kakuyomureader.core.model.NovelParagraph
import com.example.kakuyomureader.core.model.PlaybackSpeed
import com.example.kakuyomureader.core.model.PlaybackState
import com.example.kakuyomureader.core.model.ReadingMode
import com.example.kakuyomureader.feature.bookmark.data.BookmarkHistoryRepository
import com.example.kakuyomureader.feature.playback.controller.PlaybackController
import com.example.kakuyomureader.feature.playback.usecase.ChangeSpeedUseCase
import com.example.kakuyomureader.feature.playback.usecase.SpeakParagraphUseCase
import com.example.kakuyomureader.feature.playback.usecase.TogglePlaybackUseCase
import com.example.kakuyomureader.feature.reader.usecase.ProcessTappedParagraphUseCase
import com.example.kakuyomureader.feature.reader.usecase.WebViewScriptInjectorUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * リーダー画面ViewModel
 *
 * [責務]: 閲覧画面のUI状態管理、再生コントローラー連携、JS実行コマンド発行、お気に入り・履歴・設定の管理を行う。
 * [影響する状態]: ReaderUiState, PlaybackController, BookmarkHistoryRepository。
 * [発生しうる例外・エラー]: なし。
 */
class ReaderViewModel(
    application: Application,
    private val playbackController: PlaybackController = PlaybackController(application),
    private val repository: BookmarkHistoryRepository = BookmarkHistoryRepository(application),
    private val scriptInjectorUseCase: WebViewScriptInjectorUseCase = WebViewScriptInjectorUseCase(),
    private val processParagraphUseCase: ProcessTappedParagraphUseCase = ProcessTappedParagraphUseCase(),
    private val speakParagraphUseCase: SpeakParagraphUseCase = SpeakParagraphUseCase(playbackController),
    private val togglePlaybackUseCase: TogglePlaybackUseCase = TogglePlaybackUseCase(playbackController),
    private val changeSpeedUseCase: ChangeSpeedUseCase = ChangeSpeedUseCase(playbackController)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _jsExecutionFlow = MutableSharedFlow<String>()
    val jsExecutionFlow: SharedFlow<String> = _jsExecutionFlow.asSharedFlow()

    // エピソード間を自動遷移中かどうかの内部フラグ
    private var isAutoNavigatingNextEpisode: Boolean = false

    init {
        loadInitialData()
        setupPlaybackListeners()
    }

    private fun loadInitialData() {
        val bookmarks = repository.getBookmarks()
        val history = repository.getHistory()
        val dictionary = repository.getDictionary()
        val mode = repository.getReadingMode()
        val levels = repository.getParentLevels()
        val speedRate = repository.getPlaybackSpeed()
        val speed = PlaybackSpeed.fromFloatClamped(speedRate)
        _uiState.update {
            it.copy(
                bookmarks = bookmarks,
                history = history,
                dictionaryList = dictionary,
                readingMode = mode,
                parentLevels = levels,
                playbackSpeed = speed
            )
        }
    }

    private fun setupPlaybackListeners() {
        playbackController.onNextParagraphCallback = { requestNextParagraphFromWebView() }
        playbackController.onParagraphStartedCallback = { paragraph ->
            _uiState.update { it.copy(currentParagraph = paragraph, playbackState = PlaybackState.PLAYING) }
            if (paragraph.id == "end_of_episode_notification") {
                AppLogger.d("ReaderViewModel", "最終話終了アナウンス再生中")
            } else if (!_uiState.value.isDictionaryMode) {
                // 辞書モードでない場合のみスクロール追従ハイライトを実行
                executeJs(scriptInjectorUseCase.getHighlightScript(_uiState.value.currentUrl, paragraph.id))
            }
        }
    }

    fun onEvent(event: ReaderEvent) {
        when (event) {
            is ReaderEvent.OnUrlEntered -> handleUrlEntered(event.url)
            is ReaderEvent.OnHomeClicked -> handleUrlEntered("https://www.google.com")
            is ReaderEvent.OnPageStarted -> handlePageStarted(event.url)
            is ReaderEvent.OnPageFinished -> handlePageFinished(event.url, event.title)
            is ReaderEvent.OnProgressChanged -> _uiState.update { it.copy(webPageProgress = event.progress) }
            is ReaderEvent.OnParagraphTapped -> handleParagraphTapped(event.id, event.rawText)
            is ReaderEvent.OnContainerParagraphsReceived -> handleContainerExtracted(event.paragraphsJson)
            is ReaderEvent.OnTogglePlayPause -> handleTogglePlayPause()
            is ReaderEvent.OnStartBackgroundPlayback -> handleStartBackgroundPlayback()
            is ReaderEvent.OnStopPlayback -> handleStopPlayback()
            is ReaderEvent.OnSpeedChanged -> handleSpeedChanged(event.speed)
            is ReaderEvent.OnToggleAutoPlayNext -> _uiState.update { it.copy(isAutoPlayNext = event.enabled) }
            is ReaderEvent.OnNextParagraphReceived -> handleNextParagraphReceived(event.id, event.rawText)
            is ReaderEvent.OnEndOfEpisodeReached -> handleEndOfEpisode()
            is ReaderEvent.OnNoNextEpisodeReceived -> handleNoNextEpisode()
            is ReaderEvent.OnErrorDismissed -> _uiState.update { it.copy(errorMessage = null) }
            is ReaderEvent.OnToggleDictionaryMode -> handleToggleDictionaryMode(event.enabled)
            is ReaderEvent.OnRegisterDictionaryClicked -> handleRegisterDictionaryClicked()
            is ReaderEvent.OnSelectedTextReceived -> handleSelectedTextReceived(event.text)
            is ReaderEvent.OnSaveDictionaryItem -> handleSaveDictionaryItem(event.surface, event.reading)
            is ReaderEvent.OnDeleteDictionaryItem -> handleDeleteDictionaryItem(event.id)
            is ReaderEvent.OnDismissDictionaryDialog -> _uiState.update { it.copy(isDictionaryDialogOpen = false) }
            is ReaderEvent.OnToggleBookmark -> handleToggleBookmark()
            is ReaderEvent.OnDeleteBookmark -> _uiState.update { it.copy(bookmarks = repository.deleteBookmark(event.id)) }
            is ReaderEvent.OnDeleteHistory -> _uiState.update { it.copy(history = repository.deleteHistory(event.id)) }
            is ReaderEvent.OnClearAllHistory -> _uiState.update { it.copy(history = repository.clearAllHistory()) }
            is ReaderEvent.OnDrawerOpenChanged -> _uiState.update { it.copy(isDrawerOpen = event.isOpen) }
            is ReaderEvent.OnReadingModeChanged -> {
                repository.setReadingMode(event.mode)
                _uiState.update { it.copy(readingMode = event.mode) }
            }
            is ReaderEvent.OnParentLevelsChanged -> {
                repository.setParentLevels(event.levels)
                _uiState.update { it.copy(parentLevels = event.levels) }
            }
        }
    }

    private fun handleUrlEntered(url: String) {
        val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        isAutoNavigatingNextEpisode = false
        playbackController.stop()
        _uiState.update { it.copy(currentUrl = formatted, isLoadingWebPage = true, isDrawerOpen = false, playbackState = PlaybackState.STOPPED, currentParagraph = null) }
    }

    private fun handlePageStarted(url: String) {
        val parser = scriptInjectorUseCase.getParserForUrl(url)
        // 自動遷移中でない手動ページ開始の場合は再生を停止
        if (!isAutoNavigatingNextEpisode && _uiState.value.playbackState != PlaybackState.PLAYING) {
            playbackController.stop()
            _uiState.update { it.copy(playbackState = PlaybackState.STOPPED, currentParagraph = null) }
        }
        _uiState.update { it.copy(currentUrl = url, isLoadingWebPage = true, activeSiteType = parser.siteType) }
    }

    private fun handlePageFinished(url: String, title: String) {
        val newHistory = repository.addHistory(title, url)
        val isBookmarked = _uiState.value.bookmarks.any { it.url == url }
        _uiState.update { it.copy(currentUrl = url, pageTitle = title, isLoadingWebPage = false, history = newHistory, isCurrentPageBookmarked = isBookmarked) }
        
        // JSの注入
        executeJs(scriptInjectorUseCase.getInjectionScript(url))

        // 自動ナビゲーション中、または再生が継続していた場合、速度を維持して本文を一括抽出し、自動再生を再開
        if (isAutoNavigatingNextEpisode || _uiState.value.playbackState == PlaybackState.PLAYING) {
            AppLogger.d("ReaderViewModel", "次話ページ遷移完了: 再生スピード ${_uiState.value.playbackSpeed.displayString} を適用し自動再生再開")
            changeSpeedUseCase(_uiState.value.playbackSpeed)
            isAutoNavigatingNextEpisode = false
            val extractScript = "setTimeout(function() { window.NovelReaderExtractAll ? window.NovelReaderExtractAll() : (function(){ let p = document.querySelector('.widget-episode-body p, #novel_honbun p, article p, p'); if (p) p.click(); })(); }, 500);"
            executeJs(extractScript)
        }
    }

    private fun handleToggleBookmark() {
        val url = _uiState.value.currentUrl
        val title = _uiState.value.pageTitle.ifBlank { url }
        val isAlready = _uiState.value.bookmarks.any { it.url == url }
        val updated = if (isAlready) {
            val item = _uiState.value.bookmarks.firstOrNull { it.url == url }
            if (item != null) repository.deleteBookmark(item.id) else _uiState.value.bookmarks
        } else {
            repository.saveBookmark(title, url)
        }
        _uiState.update { it.copy(bookmarks = updated, isCurrentPageBookmarked = !isAlready) }
    }

    private fun handleParagraphTapped(id: String, rawText: String) {
        // 辞書登録モード時はタップでの再生場所変更を無効化
        if (_uiState.value.isDictionaryMode) return

        if (_uiState.value.readingMode == ReadingMode.PARAGRAPH) {
            val paragraph = processParagraphUseCase(id, rawText, _uiState.value.activeSiteType, _uiState.value.dictionaryList) ?: return
            _uiState.update { it.copy(currentParagraph = paragraph, playbackState = PlaybackState.PLAYING) }
            speakParagraphUseCase(paragraph)
            executeJs(scriptInjectorUseCase.getHighlightScript(_uiState.value.currentUrl, id))
        }
    }

    private fun handleContainerExtracted(json: String) {
        // 辞書登録モード時はタップ等によるコンテナ抽出からの再生場所変更を無効化
        if (_uiState.value.isDictionaryMode) return

        try {
            val array = JSONArray(json)
            val paragraphs = mutableListOf<NovelParagraph>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val text = obj.getString("text")
                val p = processParagraphUseCase(id, text, _uiState.value.activeSiteType, _uiState.value.dictionaryList)
                if (p != null) paragraphs.add(p)
            }
            if (paragraphs.isNotEmpty()) {
                _uiState.update { it.copy(currentParagraph = paragraphs.first(), playbackState = PlaybackState.PLAYING) }
                playbackController.speakList(paragraphs)
            }
        } catch (e: Exception) {
            AppLogger.e("ReaderViewModel", "コンテナJSONパース失敗: ${e.message}")
        }
    }

    private fun handleToggleDictionaryMode(enabled: Boolean) {
        _uiState.update { it.copy(isDictionaryMode = enabled) }
        AppLogger.d("ReaderViewModel", "辞書登録モード切り替え: enabled=$enabled")
    }

    private fun handleRegisterDictionaryClicked() {
        // WebView内で現在選択中の文字列を取得するJSを発行
        val getSelectedTextScript = """
            (function() {
                let selection = window.getSelection ? window.getSelection().toString() : '';
                window.AndroidBridge && window.AndroidBridge.onSelectedTextReceived(selection.trim());
            })();
        """.trimIndent()
        executeJs(getSelectedTextScript)
    }

    private fun handleSelectedTextReceived(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "登録したいテキストを選択してください") }
        } else {
            _uiState.update { it.copy(selectedTextForDictionary = text, isDictionaryDialogOpen = true) }
        }
    }

    private fun handleSaveDictionaryItem(surface: String, reading: String) {
        val updated = repository.saveDictionaryItem(surface, reading)
        _uiState.update { it.copy(dictionaryList = updated, isDictionaryDialogOpen = false, selectedTextForDictionary = "") }
        AppLogger.d("ReaderViewModel", "辞書アイテム保存完了: surface=$surface, reading=$reading")
        
        // 再生中テキストおよびキューへの即時置換適用
        val script = "window.NovelReaderExtractAll ? window.NovelReaderExtractAll() : (function(){ let p = document.querySelector('.widget-episode-body p, #novel_honbun p, article p, p'); if (p) p.click(); })();"
        executeJs(script)
    }

    private fun handleDeleteDictionaryItem(id: String) {
        val updated = repository.deleteDictionaryItem(id)
        _uiState.update { it.copy(dictionaryList = updated) }
    }

    private fun handleStartBackgroundPlayback() {
        playbackController.startForegroundService()
        if (_uiState.value.playbackState == PlaybackState.PAUSED) {
            handleTogglePlayPause()
        } else {
            val script = "window.NovelReaderExtractAll ? window.NovelReaderExtractAll() : (function(){ let p = document.querySelector('.widget-episode-body p, #novel_honbun p, article p, p'); if (p) p.click(); })();"
            executeJs(script)
        }
    }

    private fun handleTogglePlayPause() {
        val current = _uiState.value.playbackState
        togglePlaybackUseCase(current)
        val nextState = if (current == PlaybackState.PLAYING) PlaybackState.PAUSED else PlaybackState.PLAYING
        _uiState.update { it.copy(playbackState = nextState) }
    }

    private fun handleStopPlayback() {
        isAutoNavigatingNextEpisode = false
        playbackController.stop()
        _uiState.update { it.copy(playbackState = PlaybackState.STOPPED, currentParagraph = null) }
        executeJs(scriptInjectorUseCase.getClearHighlightScript(_uiState.value.currentUrl))
    }

    private fun handleSpeedChanged(speed: PlaybackSpeed) {
        changeSpeedUseCase(speed)
        repository.setPlaybackSpeed(speed.rate)
        _uiState.update { it.copy(playbackSpeed = speed) }

        // 現在再生中かつ対象段落が存在する場合、新速度で即座に言い直し発話
        if (_uiState.value.playbackState == PlaybackState.PLAYING) {
            val current = _uiState.value.currentParagraph
            if (current != null) {
                AppLogger.d("ReaderViewModel", "再生中スピード変更検出: 新速度 ${speed.displayString} で即時再発話")
                speakParagraphUseCase(current)
            }
        }
    }

    private fun requestNextParagraphFromWebView() {
        if (!_uiState.value.isAutoPlayNext || _uiState.value.readingMode == ReadingMode.CONTAINER) return
        val currentId = _uiState.value.currentParagraph?.id ?: return
        if (currentId == "end_of_episode_notification") {
            // アナウンス発話終了にともないサービスおよび再生を完全停止
            handleStopPlayback()
            return
        }
        executeJs(scriptInjectorUseCase.getNextParagraphScript(_uiState.value.currentUrl, currentId))
    }

    private fun handleNextParagraphReceived(id: String, rawText: String) {
        if (_uiState.value.readingMode == ReadingMode.PARAGRAPH) {
            handleParagraphTapped(id, rawText)
        }
    }

    private fun handleEndOfEpisode() {
        if (!_uiState.value.isAutoPlayNext) {
            handleNoNextEpisode()
            return
        }
        AppLogger.d("ReaderViewModel", "エピソード末尾到達: 次話リンクの探索・移動を実行")
        isAutoNavigatingNextEpisode = true
        executeJs(scriptInjectorUseCase.getNextEpisodeScript(_uiState.value.currentUrl))
    }

    private fun handleNoNextEpisode() {
        AppLogger.d("ReaderViewModel", "最終話到達（次話リンクなし）: 最終話終了アナウンス発話")
        isAutoNavigatingNextEpisode = false
        executeJs(scriptInjectorUseCase.getClearHighlightScript(_uiState.value.currentUrl))
        val completion = NovelParagraph(
            id = "end_of_episode_notification",
            text = "作品の末尾です",
            rawText = "作品の末尾です",
            index = -1,
            siteType = _uiState.value.activeSiteType
        )
        _uiState.update { it.copy(currentParagraph = completion, playbackState = PlaybackState.COMPLETED) }
        speakParagraphUseCase(completion)
    }

    private fun executeJs(script: String) {
        viewModelScope.launch { _jsExecutionFlow.emit(script) }
    }
}
