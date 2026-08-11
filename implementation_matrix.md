# 実装マトリクス (Implementation Matrix) - スピークブラウザ (Speak Browser)

## 1. ファイル一覧と責務・依存関係

| ファイルパス | レイヤー / 分類 | 責務・役割 | 主要な依存先 |
| :--- | :--- | :--- | :--- |
| `docs/TTS_日本語音声データダウンロード手順.md` | Docs / Setup | Fire OS / Android端末向けTTS日本語ボイス設定・ダウンロード手順書 | なし |
| `app/src/main/res/mipmap-*/ic_launcher.png` | Resource / Asset | 本×スピーカーの落ち着いたモノクロアプリアイコン画像 | Android OS Launcher |
| `KakuyomuReaderApp.kt` | Application | アプリケーションのエントリーポイント、通知チャネル初期化 | Android Framework |
| `MainActivity.kt` | Presentation | メインActivity、Jetpack Composeのエントリーポイント、Serviceバインド管理 | `ReaderViewModel`, `ReaderScreen` |
| **`core/model/`** | | | |
| `NovelParagraph.kt` | Core / Model | 段落情報データクラス（ID、テキスト、インデックス、サイト種別） | なし |
| `BookmarkItem.kt` | Core / Model | お気に入り（ブックマーク）データクラス（ID、タイトル、URL、作成日時） | なし |
| `HistoryItem.kt` | Core / Model | 閲覧履歴データクラス（ID、タイトル、URL、閲覧日時） | なし |
| `DictionaryItem.kt` | Core / Model | ユーザー辞書データクラス（ID、表記、読み、作成日時） | なし |
| `ReadingMode.kt` | Core / Model | 読み上げ方式モードEnum（CONTAINER: 一括読み, PARAGRAPH: 順次） | なし |
| `PlaybackState.kt` | Core / Model | 音声再生状態Enum（IDLE, PLAYING, PAUSED, STOPPED, COMPLETED, ERROR） | なし |
| `PlaybackSpeed.kt` | Core / Model | 読み上げ速度Value Class（0.5f〜3.0fの安全な速度制御） | なし |
| `SiteType.kt` | Core / Model | 小説サイト種別Enum（KAKUYOMU, NAROU, GENERIC） | なし |
| **`core/common/`** | | | |
| `TextChunker.kt` | Core / Util | TTS安定読み上げのための長文句読点分割ロジック | なし |
| `RubyFilter.kt` | Core / Util | 文字列中のルビ表記（二重読み）除去ユーティリティ | なし |
| `DictionaryFilter.kt` | Core / Util | 登録済みユーザー辞書に基づくテキスト自動置換ユーティリティ | `DictionaryItem` |
| `AppLogger.kt` | Core / Util | 統一ログ出力ラッパー | Android Log |
| **`feature/bookmark/`** | | | |
| `data/BookmarkHistoryRepository.kt` | Bookmark / Data | お気に入り・閲覧履歴・読書設定のローカル永続化リポジトリ | `BookmarkItem`, `HistoryItem`, `ReadingMode` |
| `ui/BookmarkHistoryDrawer.kt` | Bookmark / UI | サイドドロワー本体（お気に入り/履歴/設定） | `DrawerTabsHeader`, `BookmarkHistoryRepository` |
| `ui/DrawerTabRow.kt` | Bookmark / UI | ドロワータブヘッダー・アイテム行・設定ビューコンポーネント | `BookmarkItem`, `HistoryItem`, `ReadingMode` |
| **`feature/siteparser/`** | | | |
| `api/NovelSiteParser.kt` | Parser / Plugin IF | サイト別パーサーの共通インターフェース（URL判定、JS注入生成、ハイライトJS等） | `SiteType`, `NovelParagraph` |
| `api/ParserRegistry.kt` | Parser / Registry | URLから適切なパーサーを探索・提供するレジストリ | `NovelSiteParser`, `SiteType` |
| `kakuyomu/KakuyomuSiteParser.kt`| Parser / Plugin | カクヨム専用のパーサー実装（`.widget-episode-body p` ルビ除去、DOM抽出） | `NovelSiteParser` |
| `narou/NarouSiteParser.kt` | Parser / Plugin | 小説家になろう専用パーサー実装（`#novel_honbun p` ルビ除去、DOM抽出） | `NovelSiteParser` |
| `generic/GenericSiteParser.kt` | Parser / Plugin | 汎用Webサイト用フォールバックパーサー実装 | `NovelSiteParser` |
| **`feature/playback/`** | | | |
| `tts/TtsEngineManager.kt` | Playback / TTS | Android標準TextToSpeechの初期化・発話・キュー管理・リスナー制御 | `TextChunker`, `PlaybackSpeed`, `PlaybackState` |
| `tts/TtsEventListener.kt` | Playback / TTS | TTSの発話進捗・完了・エラーを通知するリスナーインターフェース | なし |
| `service/NovelPlaybackService.kt`| Playback / Service | フォアグラウンドサービス、Media3 MediaSession構築、バックグラウンド再生持続・通知バーコントロール管理 | `TtsEngineManager`, `MediaNotificationHelper` |
| `service/MediaNotificationHelper.kt`| Playback / Notification | 再生/一時停止/前へ/次へ/停止等の通知バー・ロックスクリーンパネル生成 | Android NotificationCompat |
| `controller/PlaybackController.kt`| Playback / Controller | Serviceとのバインド・双方向通信を管理するコントローラー | `NovelPlaybackService`, `PlaybackState` |
| `usecase/SpeakParagraphUseCase.kt`| Playback / UseCase | 指定段落の音声読み上げ開始ユースケース | `PlaybackController`, `NovelParagraph` |
| `usecase/TogglePlaybackUseCase.kt`| Playback / UseCase | 再生/一時停止トグル制御ユースケース | `PlaybackController` |
| `usecase/ChangeSpeedUseCase.kt` | Playback / UseCase | 読み上げ速度変更ユースケース | `PlaybackController`, `PlaybackSpeed` |
| **`feature/reader/`** | | | |
| `bridge/AndroidBridge.kt` | Reader / JS Bridge | WebView内のJavaScriptから呼び出されるブリッジインターフェース（`onEndOfEpisode`等） | `NovelParagraph` |
| `usecase/WebViewScriptInjectorUseCase.kt`| Reader / UseCase | サイトに応じたクリック監視・抽出JSスクリプト生成ユースケース | `ParserRegistry` |
| `usecase/ProcessTappedParagraphUseCase.kt`| Reader / UseCase | タップされた要素から段落データを生成・検証するユースケース | `NovelParagraph`, `SiteType` |
| `viewmodel/ReaderUiState.kt` | Reader / ViewModel | 画面表示に必要な全状態のイミュータブルデータクラス | `NovelParagraph`, `PlaybackState`, `PlaybackSpeed`, `SiteType` |
| `viewmodel/ReaderEvent.kt` | Reader / ViewModel | 画面操作・システムイベントのSealed Interface | `NovelParagraph`, `PlaybackSpeed` |
| `viewmodel/ReaderViewModel.kt`| Reader / ViewModel | 画面状態の管理、UseCase連携、BG再生開始・次話自動遷移・再生速度保持・最終話終了制御 | `ReaderUiState`, `ReaderEvent`, `SpeakParagraphUseCase` 等 |
| `ui/ReaderScreen.kt` | Reader / UI | リーダー画面全体のCompose Scaffoldレイアウト | `ReaderViewModel`, `ReaderTopBar`, `ReaderControlBottomBar` |
| `ui/ReaderWebView.kt` | Reader / UI | `AndroidView` によるWebViewラッパー、JSインジェクション、Bridge連携 | `AndroidBridge`, `WebViewScriptInjectorUseCase` |
| `ui/ReaderTopBar.kt` | Reader / UI | URL入力欄、戻る/進む/更新ナビゲーションバー | `ReaderEvent` |
| `ui/ReaderControlBottomBar.kt`| Reader / UI | 再生/一時停止/停止/次へ/BG再生、速度スライダー、読み上げ中テキスト表示 | `ReaderUiState`, `ReaderEvent` |
