package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

object NowPlayingCardGenerator {

    suspend fun generateAndShareCard(context: Context, song: Song) = withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background dark gradient
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(android.graphics.Color.parseColor("#12121A"), android.graphics.Color.parseColor("#0F0F14")),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Violet glow circle at top
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    width * 0.2f, height * 0.1f, width * 0.8f, height * 0.4f,
                    intArrayOf(android.graphics.Color.parseColor("#448B5CF6"), android.graphics.Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(width * 0.5f, height * 0.25f, width * 0.45f, glowPaint)

            // Header: BeatWave Wave Logo + Brand Name
            val brandPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#8B5CF6")
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.2f
            }
            canvas.drawText("BEATWAVE", width * 0.5f - 140f, 130f, brandPaint)

            val tagPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#A1A1AA")
                textSize = 28f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText("NOW PLAYING", width * 0.5f - 90f, 175f, tagPaint)

            // Album Artwork Card (Center square)
            val artSize = 680f
            val artLeft = (width - artSize) / 2f
            val artTop = 240f
            val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

            var artDrawn = false
            if (!song.albumArtPath.isNullOrBlank()) {
                val artFile = File(song.albumArtPath)
                if (artFile.exists()) {
                    try {
                        val decoded = BitmapFactory.decodeFile(artFile.absolutePath)
                        if (decoded != null) {
                            val scaled = Bitmap.createScaledBitmap(decoded, artSize.toInt(), artSize.toInt(), true)
                            val artPath = Path().apply {
                                addRoundRect(artRect, 48f, 48f, Path.Direction.CW)
                            }
                            canvas.save()
                            canvas.clipPath(artPath)
                            canvas.drawBitmap(scaled, artLeft, artTop, null)
                            canvas.restore()
                            artDrawn = true
                        }
                    } catch (_: Exception) {}
                }
            }

            if (!artDrawn) {
                // Procedural Gradient artwork
                val artPaint = Paint().apply {
                    shader = LinearGradient(
                        artLeft, artTop, artLeft + artSize, artTop + artSize,
                        intArrayOf(android.graphics.Color.parseColor("#8B5CF6"), android.graphics.Color.parseColor("#EC4899")),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(artRect, 48f, 48f, artPaint)

                val notePaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                    textSize = 180f
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("♪", width * 0.5f - 40f, artTop + artSize * 0.55f, notePaint)
            }

            // Outer subtle border around artwork
            val borderPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = android.graphics.Color.parseColor("#338B5CF6")
            }
            canvas.drawRoundRect(artRect, 48f, 48f, borderPaint)

            // Song Title
            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 58f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val title = if (song.title.length > 24) song.title.substring(0, 22) + "..." else song.title
            canvas.drawText(title, width * 0.5f, 1010f, titlePaint)

            // Artist & Album
            val artistPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#A1A1AA")
                textSize = 38f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            val artistText = if (song.artist.length > 28) song.artist.substring(0, 26) + "..." else song.artist
            canvas.drawText(artistText, width * 0.5f, 1075f, artistPaint)

            // Waveform visualizer decorative line
            val wavePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#8B5CF6")
                strokeWidth = 6f
                strokeCap = Paint.Cap.ROUND
            }
            val waveY = 1170f
            val waveWidth = 500f
            val waveLeft = (width - waveWidth) / 2f
            val bars = 32
            val barStep = waveWidth / bars
            for (i in 0 until bars) {
                val bx = waveLeft + i * barStep
                val h = (sin(i * 0.6) * 28 + 36).toFloat()
                canvas.drawLine(bx, waveY - h / 2, bx, waveY + h / 2, wavePaint)
            }

            // Footer tagline
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#71717A")
                textSize = 26f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.1f
            }
            canvas.drawText("LISTEN ON BEATWAVE PLAYER", width * 0.5f, 1260f, footerPaint)

            // Save to cache and share
            val shareDir = File(context.cacheDir, "shares").apply { if (!exists()) mkdirs() }
            val shareFile = File(shareDir, "now_playing_${System.currentTimeMillis()}.png")
            FileOutputStream(shareFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Listening to \"${song.title}\" by ${song.artist} on BeatWave ♪")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Track Card").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

        } catch (e: Exception) {
            Log.e("NowPlayingCardGenerator", "Failed to generate card: ${e.message}")
        }
    }
}
