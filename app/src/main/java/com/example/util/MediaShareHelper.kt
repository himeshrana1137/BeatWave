package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.Song
import com.example.model.VideoItem
import java.io.File

object MediaShareHelper {

    fun shareSong(context: Context, song: Song) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                val uri = resolveUri(context, song.uriString)
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                putExtra(Intent.EXTRA_SUBJECT, song.title)
                putExtra(Intent.EXTRA_TEXT, "Listen to \"${song.title}\" by ${song.artist} on BeatWave!")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share \"${song.title}\" via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share track: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareVideo(context: Context, video: VideoItem) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                val uri = resolveUri(context, video.uriString)
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                putExtra(Intent.EXTRA_SUBJECT, video.title)
                putExtra(Intent.EXTRA_TEXT, "Watch \"${video.title}\" on BeatWave!")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share \"${video.title}\" via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveUri(context: Context, uriString: String): Uri? {
        return if (uriString.startsWith("content://")) {
            Uri.parse(uriString)
        } else if (uriString.startsWith("file://")) {
            val file = File(Uri.parse(uriString).path ?: "")
            if (file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.parse(uriString)
            }
        } else {
            val file = File(uriString)
            if (file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.parse(uriString)
            }
        }
    }
}
