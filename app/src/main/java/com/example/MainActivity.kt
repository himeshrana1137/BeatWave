package com.example

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MusicPlayerScreen
import com.example.ui.MusicViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.video.VideoPlayerState

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.example.beatwave.PIP_PLAY_PAUSE"
    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP_PLAY_PAUSE) {
                VideoPlayerState.requestTogglePlayPause()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter(ACTION_PIP_PLAY_PAUSE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(pipReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(pipReceiver, filter)
            }
        }

        setContent {
            val themeMode by musicViewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MusicPlayerScreen(viewModel = musicViewModel)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        VideoPlayerState.isPiPActive.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isPiPSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            if (isPiPSupported && VideoPlayerState.isVideoPlaying.value) {
                enterVideoPiP()
            }
        }
    }

    fun enterVideoPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPiPParams(VideoPlayerState.isVideoPlaying.value)
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    fun updatePiPParams(isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPiPParams(isPlaying)
                setPictureInPictureParams(params)
            } catch (_: Exception) {}
        }
    }

    private fun buildPiPParams(isPlaying: Boolean): PictureInPictureParams {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw IllegalStateException("Picture-in-Picture not supported on API < 26")
        }
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))

        val iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val icon = Icon.createWithResource(this, iconRes)
        val title = if (isPlaying) "Pause" else "Play"
        val intent = Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 101, intent, flags)
        val action = RemoteAction(icon, title, title, pendingIntent)

        builder.setActions(listOf(action))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
        }

        return builder.build()
    }

    override fun onPause() {
        super.onPause()
        musicViewModel.savePlaybackState()
    }

    override fun onStop() {
        super.onStop()
        musicViewModel.savePlaybackState()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
    }
}
