package com.example.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.LyricLine
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

object LrcParser {

    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\]")

    fun parseLrcFromUri(context: Context, uri: Uri): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        lines.addAll(parseLrcLine(line))
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LrcParser", "Error reading LRC file from $uri: ${e.message}")
        }
        return lines.sortedBy { it.timeMs }
    }

    fun parseLrcText(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcContent.lineSequence().forEach { line ->
            lines.addAll(parseLrcLine(line))
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun parseLrcLine(rawLine: String): List<LyricLine> {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return emptyList()

        val matcher = TIME_TAG_PATTERN.matcher(trimmed)
        val timestamps = mutableListOf<Long>()
        var lastMatchEnd = 0

        while (matcher.find()) {
            val minutes = matcher.group(1)?.toLong() ?: 0L
            val seconds = matcher.group(2)?.toLong() ?: 0L
            val fractionStr = matcher.group(3) ?: "0"
            val fraction = if (fractionStr.length == 2) fractionStr.toLong() * 10 else fractionStr.toLong()
            val timeMs = (minutes * 60000L) + (seconds * 1000L) + fraction
            timestamps.add(timeMs)
            lastMatchEnd = matcher.end()
        }

        if (timestamps.isEmpty()) return emptyList()

        val lyricText = trimmed.substring(lastMatchEnd).trim()
        return timestamps.map { timeMs -> LyricLine(timeMs, lyricText) }
    }

    fun generateDemoLyrics(songTitle: String): List<LyricLine> {
        return listOf(
            LyricLine(0L, "♪ (Intro - Harmoniq Beats) ♪"),
            LyricLine(2000L, "Drifting through the midnight neon glow"),
            LyricLine(5000L, "Subtle synth lines pulsing soft and slow"),
            LyricLine(8000L, "Bass resonance vibrates through the room"),
            LyricLine(11000L, "Atmospheric chords breaking through the gloom"),
            LyricLine(13500L, "Harmoniq soundwaves taking full control"),
            LyricLine(16000L, "♪ (Outro fade) ♪")
        )
    }
}
