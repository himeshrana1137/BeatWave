package com.example.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.LyricLine
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LyricsRepository(private val context: Context) {

    private val lyricsDir = File(context.filesDir, "lyrics").apply {
        if (!exists()) mkdirs()
    }

    suspend fun getOrFetchLyrics(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        // 1. Check if user attached a custom lyrics URI
        if (!song.lyricsUriString.isNullOrBlank()) {
            val lines = parseUriLyrics(song.lyricsUriString)
            if (lines.isNotEmpty()) return@withContext lines
        }

        // 2. Check local disk cache
        val cachedFile = File(lyricsDir, "${song.id}.lrc")
        if (cachedFile.exists() && cachedFile.length() > 10) {
            val text = cachedFile.readText()
            val parsed = LrcParser.parseLrcText(text)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        // 3. Online fetch from LRCLIB API
        try {
            val syncedLrc = fetchFromLrclib(song.title, song.artist, song.album, (song.durationMs / 1000).toInt())
            if (!syncedLrc.isNullOrBlank()) {
                // Save to cache
                FileOutputStream(cachedFile).use { fos ->
                    fos.write(syncedLrc.toByteArray())
                }
                return@withContext LrcParser.parseLrcText(syncedLrc)
            }
        } catch (e: Exception) {
            Log.w("LyricsRepository", "LRCLIB fetch error for ${song.title}: ${e.message}")
        }

        emptyList()
    }

    private fun parseUriLyrics(uriString: String): List<LyricLine> {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val text = inputStream.bufferedReader().use { it.readText() }
            LrcParser.parseLrcText(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchFromLrclib(
        title: String,
        artist: String,
        album: String,
        durationSec: Int
    ): String? {
        val cleanTitle = title.replace(Regex("\\[.*?\\]|\\(.*?\\)"), "").trim()
        val cleanArtist = if (artist.equals("Unknown Artist", ignoreCase = true)) "" else artist.trim()
        val cleanAlbum = if (album.equals("Unknown Album", ignoreCase = true)) "" else album.trim()

        // Attempt 1: Direct exact match
        try {
            val queryParams = StringBuilder("track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}")
            if (cleanArtist.isNotBlank()) queryParams.append("&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}")
            if (cleanAlbum.isNotBlank()) queryParams.append("&album_name=${URLEncoder.encode(cleanAlbum, "UTF-8")}")
            if (durationSec > 0) queryParams.append("&duration=$durationSec")

            val url = URL("https://lrclib.net/api/get?$queryParams")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "BeatWave-MusicPlayer/1.0 (Android)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val synced = obj.optString("syncedLyrics", "")
                if (synced.isNotBlank()) return synced
            }
        } catch (_: Exception) {}

        // Attempt 2: Search fallback
        try {
            val searchQuery = if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle
            val searchUrl = URL("https://lrclib.net/api/search?q=${URLEncoder.encode(searchQuery, "UTF-8")}")
            val conn = (searchUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "BeatWave-MusicPlayer/1.0 (Android)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val synced = item.optString("syncedLyrics", "")
                    if (synced.isNotBlank()) return synced
                }
            }
        } catch (_: Exception) {}

        return null
    }
}
