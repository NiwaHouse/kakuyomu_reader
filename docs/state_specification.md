# 状態仕様書 (State Specification) - スピークブラウザ (Speak Browser)

## 1. 状態管理の概要
本アプリでは、単方向データフロー（Unidirectional Data Flow）に基づき、UI状態（`ReaderUiState`）および再生エンジン状態（`PlaybackState`）を一元的に管理します。

---

## 2. 状態モデル定義

### 2.1. 再生状態 (`PlaybackState`)
音声再生機能（TTS / Foreground Service）におけるコア状態。

| 状態名 | 説明 | 遷移可能な次の状態 |
| :--- | :--- | :--- |
| `IDLE` | 初期状態または再生待機状態 | `INITIALIZING`, `PLAYING` |
| `INITIALIZING` | TTSエンジンの初期化中 | `IDLE`, `ERROR` |
| `PLAYING` | 現在段落またはチャンクを音声読み上げ中 | `PAUSED`, `STOPPED`, `COMPLETED`, `ERROR` |
| `PAUSED` | 読み上げ一時停止中 | `PLAYING`, `STOPPED` |
| `STOPPED` | 読み上げ停止（リセット完了） | `PLAYING`, `IDLE` |
| `COMPLETED` | 1つの段落（またはエピソード全体）の読み上げ完了 | `PLAYING` (次段落), `IDLE` |
| `ERROR` | TTS初期化失敗または再生エラー | `IDLE`, `INITIALIZING` |

---

### 2.2. リーダー画面UI状態 (`ReaderUiState`)
`ReaderViewModel` から Compose UI に公開される統合UI状態。

| フィールド | 型 | デフォルト値 | 説明 |
| :--- | :--- | :--- | :--- |
| `currentUrl` | `String` | `"https://kakuyomu.jp/"` | 現在表示中のWebページURL |
| `pageTitle` | `String` | `""` | 読み込んだWebページのタイトル |
| `isLoadingWebPage` | `Boolean` | `false` | WebViewのページ読み込み中フラグ |
| `webPageProgress` | `Int` | `0` | ページ読み込み進捗 (0-100) |
| `currentParagraph` | `NovelParagraph?` | `null` | 現在選択/読み上げ中の段落データ |
| `playbackState` | `PlaybackState` | `PlaybackState.IDLE` | 現在の音声再生状態 |
| `playbackSpeed` | `PlaybackSpeed` | `PlaybackSpeed.NORMAL (1.0f)` | 読み上げ速度設定 (0.5f〜3.0f) |
| `isAutoPlayNext` | `Boolean` | `true` | 自動連続読み上げ有効フラグ |
| `readingMode` | `ReadingMode` | `ReadingMode.CONTAINER` | 読み上げ方式モード (CONTAINER / PARAGRAPH) |
| `parentLevels` | `Int` | `2` | 親コンテナ探索の遡り階層数 (1〜5) |
| `bookmarks` | `List<BookmarkItem>` | `emptyList()` | お気に入り一覧 |
| `history` | `List<HistoryItem>` | `emptyList()` | 閲覧履歴一覧 |
| `isCurrentPageBookmarked` | `Boolean` | `false` | 現在のページがお気に入り登録済みか |
| `isDrawerOpen` | `Boolean` | `false` | ドロワー開閉フラグ |
| `isDictionaryMode` | `Boolean` | `false` | 辞書登録モードフラグ（ON時はスクロール追従・段落タップ再生が無効化） |
| `dictionaryList` | `List<DictionaryItem>` | `emptyList()` | 登録済みユーザー辞書アイテム一覧 |
| `isDictionaryDialogOpen` | `Boolean` | `false` | 辞書登録ダイアログ表示フラグ |
| `selectedTextForDictionary` | `String` | `""` | 辞書登録用に選択されたテキスト |
| `canGoBack` | `Boolean` | `false` | WebViewが「戻る」可能か |
| `canGoForward` | `Boolean` | `false` | WebViewが「進む」可能か |
| `activeSiteType` | `SiteType` | `SiteType.KAKUYOMU` | 適用中のパーサー種別 |
| `errorMessage` | `String?` | `null` | エラー通知用メッセージ |

---

### 2.3. 段落データモデル (`NovelParagraph`)

| フィールド | 型 | 説明 |
| :--- | :--- | :--- |
| `id` | `String` | DOM要素の一意な識別子（例: `"p_12"`, `"kakuyomu_p_3"`） |
| `text` | `String` | ルビ（`<rt>`）除外およびユーザー辞書置換適用済みのTTS用テキスト |
| `rawText` | `String` | ルビやタグを含む元のテキスト（表示補助用） |
| `index` | `Int` | エピソード内の段落インデックス番号 |
| `siteType` | `SiteType` | 抽出元の小説サイト種別 |

---

### 2.4. ユーザー辞書モデル (`DictionaryItem`)

| フィールド | 型 | 説明 |
| :--- | :--- | :--- |
| `id` | `String` | 辞書項目の一意な識別子 |
| `surface` | `String` | 置換対象の元の単語・表記（例: `"魔王"`） |
| `reading` | `String` | 置換後の読み（例: `"まおう"`） |
| `createdAt` | `Long` | 登録日時タイムスタンプ |

---

## 3. 状態遷移イベント (`ReaderEvent`)

ユーザー操作やシステム・JS BridgeからViewModelに通知されるイベント。

| イベント名 | 引数 | 発火契機 | 影響する状態 |
| :--- | :--- | :--- | :--- |
| `OnUrlEntered` | `url: String` | 上部URLバーまたはドロワーからURLが指定された | `currentUrl`, `isLoadingWebPage`, `isDrawerOpen` |
| `OnHomeClicked` | なし | 上部ホームボタンが押された | `currentUrl` (Google) |
| `OnPageStarted` | `url: String` | WebViewのページ読み込みが開始された | `isLoadingWebPage`, `webPageProgress` |
| `OnPageFinished` | `url: String, title: String` | WebViewのページ読み込みが完了した | `pageTitle`, `isLoadingWebPage`, `history`, `isCurrentPageBookmarked` |
| `OnParagraphTapped` | `id: String, rawText: String` | ユーザーが段落をタップした | `currentParagraph`, `playbackState` (PLAYING)（※辞書モード時は無視） |
| `OnContainerParagraphsReceived`| `paragraphsJson: String` | 親コンテナから全段落が一括抽出された | `currentParagraph`, `playbackState` (PLAYING, 一括キュー) |
| `OnTogglePlayPause` | なし | 再生/一時停止ボタンが押された | `playbackState` (PLAYING/PAUSED) |
| `OnStartBackgroundPlayback`| なし | 「🎧 BG再生」ボタンが押された | フォアグラウンドサービス常駐起動・通知表示、アプリバックグラウンド移行 (`moveTaskToBack`), `playbackState` (PLAYING) |
| `OnStopPlayback` | なし | 停止ボタンが押された | `playbackState` (STOPPED), `currentParagraph`, サービス/通知停止 |
| `OnSpeedChanged` | `speed: PlaybackSpeed` | 速度変更スライダーが操作された | `playbackSpeed`（ページ遷移時も維持。再生中であれば残りの再生キューを保持したまま現在段落を新速度で即時言い直し連続再生） |
| `OnToggleAutoPlayNext`| `enabled: Boolean` | 自動連続読み上げトグルが変更された | `isAutoPlayNext` |
| `OnToggleDictionaryMode`| `enabled: Boolean` | 辞書登録モードトグルが変更された | `isDictionaryMode` |
| `OnRegisterDictionaryClicked`| なし | 「➕ 登録」ボタンが押された | JS経由で選択文字列を取得要求 |
| `OnSelectedTextReceived`| `text: String` | WebViewから選択中テキストを受信した | `selectedTextForDictionary`, `isDictionaryDialogOpen` （未選択時は `errorMessage`） |
| `OnSaveDictionaryItem` | `surface: String, reading: String` | 辞書登録ダイアログで「登録」が押された | `dictionaryList`, `isDictionaryDialogOpen`, 再生中キューへの即時置換適用 |
| `OnDeleteDictionaryItem` | `id: String` | 辞書一覧から辞書項目が削除された | `dictionaryList` |
| `OnDismissDictionaryDialog`| なし | 辞書登録ダイアログがキャンセル/閉じられた | `isDictionaryDialogOpen` |
| `OnParagraphCompleted`| `id: String` | TTSによる現在の段落再生が完了した | `playbackState` (次段落へ自動進行 / COMPLETED) |
| `OnEndOfEpisodeReached`| なし | エピソード末尾到達（全段落完了） | 次話が存在する場合はWebViewページ自動遷移・`playbackSpeed`維持で先頭段落再生継続。次話が存在しない（最終話）場合は「作品の末尾です」アナウンス再生後 `playbackState` (STOPPED) ＆ サービス停止 |
| `OnToggleBookmark` | なし | 上部☆ボタンが押された | `bookmarks`, `isCurrentPageBookmarked` |
| `OnDeleteBookmark` | `id: String` | お気に入り一覧から削除された | `bookmarks`, `isCurrentPageBookmarked` |
| `OnDeleteHistory` | `id: String` | 履歴一覧から削除された | `history` |
| `OnClearAllHistory` | なし | 履歴全消去が押された | `history` |
| `OnDrawerOpenChanged`| `isOpen: Boolean` | ドロワー開閉が操作された | `isDrawerOpen` |
| `OnReadingModeChanged`| `mode: ReadingMode` | 読み上げ方式が切り替えられた | `readingMode` |
| `OnParentLevelsChanged`| `levels: Int` | 親コンテナ遡り階層数が変更された | `parentLevels` |
| `OnErrorOccurred` | `message: String` | TTSや通信でエラーが発生した | `errorMessage`, `playbackState` (ERROR) |
