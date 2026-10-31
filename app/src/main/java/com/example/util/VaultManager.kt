package com.example.util

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class VaultManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("beatwave_vault_prefs", Context.MODE_PRIVATE)

    private val vaultDir: File = File(context.filesDir, "vault").apply {
        if (!exists()) mkdirs()
    }

    companion object {
        private const val KEY_VAULT_PIN = "vault_pin"
        private const val KEY_VAULT_VIDEOS = "vault_videos"
    }

    fun isPinSet(): Boolean {
        return prefs.contains(KEY_VAULT_PIN)
    }

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_VAULT_PIN, pin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_VAULT_PIN, null) ?: return pin == "0000" // Default initial PIN
        return saved == pin
    }

    fun isDeviceSecure(): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    suspend fun loadVaultVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val jsonStr = prefs.getString(KEY_VAULT_VIDEOS, null) ?: return@withContext emptyList()
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<VideoItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val file = File(obj.getString("path"))
                if (file.exists()) {
                    list.add(
                        VideoItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            durationMs = obj.optLong("durationMs", 0L),
                            uriString = Uri.fromFile(file).toString(),
                            lastPositionMs = obj.optLong("lastPositionMs", 0L),
                            thumbnailPath = obj.optString("thumbnailPath", null),
                            dateAdded = obj.optLong("dateAdded", System.currentTimeMillis()),
                            isDemo = false
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            Log.e("VaultManager", "Error parsing vault videos: ${e.message}")
            emptyList()
        }
    }

    suspend fun importVideoToVault(video: VideoItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val srcUri = Uri.parse(video.uriString)
            val destFile = File(vaultDir, "vault_${UUID.randomUUID()}.mp4")

            // Copy file to internal private vault
            context.contentResolver.openInputStream(srcUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                val srcFile = File(video.uriString)
                if (srcFile.exists()) {
                    FileInputStream(srcFile).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                val current = loadVaultVideos().toMutableList()
                current.add(
                    video.copy(
                        id = UUID.randomUUID().toString(),
                        uriString = Uri.fromFile(destFile).toString()
                    )
                )
                saveVaultVideos(current)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("VaultManager", "Failed to import video to vault: ${e.message}")
        }
        false
    }

    suspend fun removeVideoFromVault(videoId: String) = withContext(Dispatchers.IO) {
        val current = loadVaultVideos().toMutableList()
        val found = current.firstOrNull { it.id == videoId }
        if (found != null) {
            try {
                val file = File(Uri.parse(found.uriString).path ?: "")
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            current.removeAll { it.id == videoId }
            saveVaultVideos(current)
        }
    }

    private fun saveVaultVideos(videos: List<VideoItem>) {
        val arr = JSONArray()
        videos.forEach { v ->
            val obj = JSONObject().apply {
                put("id", v.id)
                put("title", v.title)
                put("durationMs", v.durationMs)
                put("path", Uri.parse(v.uriString).path ?: "")
                put("lastPositionMs", v.lastPositionMs)
                put("thumbnailPath", v.thumbnailPath ?: "")
                put("dateAdded", v.dateAdded)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_VAULT_VIDEOS, arr.toString()).apply()
    }
}
