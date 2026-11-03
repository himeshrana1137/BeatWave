package com.example.video

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.model.SubtitleLine
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object SubtitleParser {

    /**
     * Parses .srt or .vtt subtitles from a given Uri.
     * Robustly decodes UTF-8 (including Indian scripts: Devanagari, Tamil, Telugu,
     * Bengali, Malayalam, Kannada, Gujarati, Punjabi), UTF-16, and fallback encodings.
     */
    fun parseSrtFromUri(context: Context, uri: Uri): List<SubtitleLine> {
        val lines = mutableListOf<SubtitleLine>()
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return emptyList()

            val text = decodeSubtitleBytes(bytes)
            return parseSubtitleText(text)
        } catch (e: Exception) {
            Log.e("SubtitleParser", "Error reading subtitle uri $uri: ${e.message}")
        }
        return lines
    }

    /**
     * Decodes subtitle byte array by checking Byte Order Marks (BOM),
     * attempting strict UTF-8 decoding, and falling back to common legacy encodings.
     */
    fun decodeSubtitleBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // Check BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }

        // Try strict UTF-8
        try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val charBuffer = decoder.decode(ByteBuffer.wrap(bytes))
            return charBuffer.toString()
        } catch (_: Exception) {}

        // Fallback to Windows-1252 / ISO-8859-1
        return try {
            String(bytes, Charset.forName("windows-1252"))
        } catch (_: Exception) {
            String(bytes, StandardCharsets.ISO_8859_1)
        }
    }

    /**
     * Parses normalized subtitle text, supporting both SubRip (.srt) and WebVTT (.vtt).
     */
    fun parseSubtitleText(content: String): List<SubtitleLine> {
        val lines = mutableListOf<SubtitleLine>()
        val tagRegex = """<[^>]+>""".toRegex() // Strip HTML / VTT formatting tags

        val rawLines = content.lines()
        var currentStartMs: Long? = null
        var currentEndMs: Long? = null
        val currentText = StringBuilder()

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()

            // Skip WebVTT header lines
            if (trimmed.startsWith("WEBVTT") || trimmed.startsWith("NOTE") || trimmed.startsWith("STYLE")) {
                continue
            }

            if (trimmed.contains("-->")) {
                // Save previous block if exists
                if (currentStartMs != null && currentEndMs != null && currentText.isNotEmpty()) {
                    lines.add(
                        SubtitleLine(
                            startMs = currentStartMs,
                            endMs = currentEndMs,
                            text = currentText.toString().trim()
                        )
                    )
                    currentText.clear()
                }

                // Parse timestamp line: "00:01:20.000 --> 00:01:24.000" or "01:20.000 --> 01:24.000"
                val parts = trimmed.split("-->")
                if (parts.size == 2) {
                    val startStr = parts[0].trim().substringBefore(' ')
                    val endStr = parts[1].trim().substringBefore(' ')
                    currentStartMs = parseTimestampToMs(startStr)
                    currentEndMs = parseTimestampToMs(endStr)
                }
            } else if (trimmed.isEmpty()) {
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
                // Sequence number in .srt, ignore
            } else {
                val cleanText = tagRegex.replace(trimmed, "")
                if (cleanText.isNotBlank()) {
                    if (currentText.isNotEmpty()) currentText.append("\n")
                    currentText.append(cleanText)
                }
            }
        }

        // Add last block if present
        if (currentStartMs != null && currentEndMs != null && currentText.isNotEmpty()) {
            lines.add(
                SubtitleLine(
                    startMs = currentStartMs,
                    endMs = currentEndMs,
                    text = currentText.toString().trim()
                )
            )
        }

        return lines
    }

    /**
     * Automatically scans for a matching subtitle file (.srt or .vtt) in the same
     * directory as the video file.
     */
    fun autoDetectSubtitle(context: Context, videoUriString: String): Uri? {
        try {
            // Case 1: Direct file:// URI or local file path
            val filePath = if (videoUriString.startsWith("file://")) {
                Uri.parse(videoUriString).path
            } else if (!videoUriString.startsWith("content://")) {
                videoUriString
            } else {
                // Case 2: Resolve content:// URI via MediaStore
                resolveContentPath(context, Uri.parse(videoUriString))
            }

            if (!filePath.isNullOrBlank()) {
                val videoFile = File(filePath)
                val parentDir = videoFile.parentFile
                if (parentDir != null && parentDir.exists() && parentDir.isDirectory) {
                    val baseName = videoFile.nameWithoutExtension

                    // Check exact sister files: baseName.srt, baseName.vtt, baseName.en.srt, baseName.hi.srt
                    val candidates = listOf(
                        "$baseName.srt",
                        "$baseName.vtt",
                        "$baseName.en.srt",
                        "$baseName.en.vtt",
                        "$baseName.hi.srt",
                        "$baseName.hi.vtt"
                    )

                    for (candidate in candidates) {
                        val subFile = File(parentDir, candidate)
                        if (subFile.exists() && subFile.length() > 0) {
                            return Uri.fromFile(subFile)
                        }
                    }

                    // Check any file starting with baseName and ending with .srt / .vtt
                    val matching = parentDir.listFiles { _, name ->
                        name.startsWith(baseName, ignoreCase = true) &&
                                (name.endsWith(".srt", ignoreCase = true) || name.endsWith(".vtt", ignoreCase = true))
                    }
                    if (!matching.isNullOrEmpty()) {
                        return Uri.fromFile(matching.first())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SubtitleParser", "Could not auto-detect subtitle: ${e.message}")
        }
        return null
    }

    private fun resolveContentPath(context: Context, uri: Uri): String? {
        return try {
            val proj = arrayOf(MediaStore.Video.Media.DATA)
            context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTimestampToMs(timeStr: String): Long {
        try {
            // Format: HH:MM:SS,mmm or HH:MM:SS.mmm or MM:SS.mmm
            val clean = timeStr.replace(',', '.')
            val timeParts = clean.split(":")
            if (timeParts.size == 3) {
                val hours = timeParts[0].trim().toLong()
                val minutes = timeParts[1].trim().toLong()
                val secParts = timeParts[2].trim().split(".")
                val seconds = secParts[0].toLong()
                val millis = if (secParts.size > 1) {
                    secParts[1].padEnd(3, '0').take(3).toLong()
                } else 0L
                return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
            } else if (timeParts.size == 2) {
                // MM:SS.mmm
                val minutes = timeParts[0].trim().toLong()
                val secParts = timeParts[1].trim().split(".")
                val seconds = secParts[0].toLong()
                val millis = if (secParts.size > 1) {
                    secParts[1].padEnd(3, '0').take(3).toLong()
                } else 0L
                return (minutes * 60000L) + (seconds * 1000L) + millis
            }
        } catch (_: Exception) {}
        return 0L
    }
}
