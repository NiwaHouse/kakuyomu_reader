package com.example.kakuyomureader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.kakuyomureader.core.common.AppLogger
import com.example.kakuyomureader.feature.playback.controller.PlaybackController
import com.example.kakuyomureader.feature.reader.ui.ReaderScreen
import com.example.kakuyomureader.feature.reader.viewmodel.ReaderViewModel

/**
 * メインActivity
 *
 * [責務]: アプリケーションのエントリーポイントActivity。PlaybackControllerのServiceバインド管理およびCompose UIの初期化を行う。
 * [影響する状態]: Activityライフサイクル、Serviceバインド。
 * [発生しうる例外・エラー]: なし。
 */
class MainActivity : ComponentActivity() {

    private lateinit var playbackController: PlaybackController
    private lateinit var readerViewModel: ReaderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("MainActivity", "onCreate: メイン画面初期化")

        // コントローラーおよびViewModelの初期化
        playbackController = PlaybackController(this)
        readerViewModel = ReaderViewModel(application, playbackController)

        // 通知権限のリクエスト (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReaderScreen(viewModel = readerViewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // フォアグラウンド移行時にServiceへバインド
        playbackController.bind()
    }

    override fun onStop() {
        // バックグラウンド移行時はアンバインド（Service自体はフォアグラウンドサービスとして持続）
        playbackController.unbind()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            val isGranted = grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            AppLogger.d("MainActivity", "POST_NOTIFICATIONS 権限結果: granted=$isGranted")
        }
    }

    override fun onDestroy() {
        AppLogger.d("MainActivity", "onDestroy: メイン画面終了")
        super.onDestroy()
    }
}
