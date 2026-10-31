package com.example.video

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.SubtitleLine
import java.io.BufferedReader
import java.io.InputStreamReader

object SubtitleParser {

    fun parseSrtFromUri(context: Context, uri: Uri): List<SubtitleLine> {
        val lines = mutableListOf<SubtitleLine>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    var currentStartMs: Long? = null
                    var currentEndMs: Long? = null
                    val currentText = StringBuilder()

                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.contains("-->")) {
                            // Timestamp line: 00:01:20,000 --> 00:01:24,000
                            val parts = trimmed.split("-->")
                            if (parts.size == 2) {
                                currentStartMs = parseTimestampToMs(parts[0].trim())
                                currentEndMs = parseTimestampToMs(parts[1].trim())
                            }
                        } else if (trimmed.isEmpty()) {
                            // Blank line ends the subtitle block
                            if (currentStartMs != null && currentEndMs != null && currentText.isNotEmpty()) {
                                lines.add(
                                    SubtitleLine(
                                        startMs = currentStartMs,
                                        endMs = currentEndMs,
                                        text = currentText.toString().trim()
                                    )
                                )
                            }
                            currentStartMs = null
                            currentEndMs = null
                            currentText.clear()
                        } else if (trimmed.all { it.isDigit() } && currentStartMs == null) {
                            // Sequence number, ignore
                        } else {
                            if (currentText.isNotEmpty()) currentText.append("\n")
                            currentText.append(trimmed)
                        }
                        line = reader.readLine()
                    }

                    // Last block if file doesn't end with blank line
                    if (currentStartMs != null && currentEndMs != null && currentText.isNotEmpty()) {
                        lines.add(
                            SubtitleLine(
                                startMs = currentStartMs,
                                endMs = currentEndMs,
                                text = currentText.toString().trim()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SubtitleParser", "Error parsing SRT: ${e.message}")
        }
        return lines
    }

    private fun parseTimestampToMs(timeStr: String): Long {
        try {
            // Format: HH:MM:SS,mmm or HH:MM:SS.mmm
            val clean = timeStr.replace(',', '.')
            val timeParts = clean.split(":")
            if (timeParts.size == 3) {
                val hours = timeParts[0].trim().toLong()
                val minutes = timeParts[1].trim().toLong()
                val secParts = timeParts[2].trim().split(".")
                val seconds = secParts[0].toLong()
                val millis = if (secParts.size > 1) {
                    val mStr = secParts[1].padEnd(3, '0').take(3)
                    mStr.toLong()
                } else 0L

                return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
            }
        } catch (e: Exception) {
            Log.w("SubtitleParser", "Invalid timestamp '$timeStr': ${e.message}")
        }
        return 0L
    }
}
