package com.example.util

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.util.LruCache
import com.example.model.AudioQuality
import com.example.model.AudioQualityTier
import com.example.model.Song
import com.example.model.VideoItem
import com.example.model.VideoQuality
import com.example.model.VideoResolutionTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Robust, background-driven quality detector and cache for audio and video media.
 * Extracts format, bitrate, sample rate, bit depth, channels, resolution, fps, codecs,
 * HDR, and device hardware decoder compatibility.
 */
object MediaQualityManager {

    private const val TAG = "MediaQualityManager"
    private const val PREFS_NAME = "beatwave_media_quality_cache"
    private const val KEY_AUDIO_CACHE = "cached_audio_qualities"
    private const val KEY_VIDEO_CACHE = "cached_video_qualities"

    // In-memory caches for 60fps smooth scrolling
    private val audioMemoryCache = LruCache<String, AudioQuality>(500)
    private val videoMemoryCache = LruCache<String, VideoQuality>(300)

    private var persistentAudioJson: JSONObject? = null
    private var persistentVideoJson: JSONObject? = null

    @Synchronized
    private fun initPersistentCaches(context: Context) {
        if (persistentAudioJson == null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val aStr = prefs.getString(KEY_AUDIO_CACHE, "{}") ?: "{}"
            persistentAudioJson = try { JSONObject(aStr) } catch (_: Exception) { JSONObject() }

            val vStr = prefs.getString(KEY_VIDEO_CACHE, "{}") ?: "{}"
            persistentVideoJson = try { JSONObject(vStr) } catch (_: Exception) { JSONObject() }
        }
    }

    /**
     * Fast synchronous lookup (Memory -> Disk Cache -> Heuristic). Never blocks main thread.
     */
    fun getAudioQualitySync(song: Song): AudioQuality {
        val cached = audioMemoryCache.get(song.id)
        if (cached != null) return cached

        persistentAudioJson?.optJSONObject(song.id)?.let { obj ->
            val q = deserializeAudioQuality(obj)
            audioMemoryCache.put(song.id, q)
            return q
        }

        // Quick instant heuristic based on extension and filename
        return computeAudioHeuristic(song)
    }

    /**
     * Deep background audio quality extraction using MediaMetadataRetriever and MediaExtractor.
     */
    suspend fun getAudioQuality(context: Context, song: Song): AudioQuality = withContext(Dispatchers.IO) {
        initPersistentCaches(context)

        val cached = audioMemoryCache.get(song.id)
        if (cached != null) return@withContext cached

        persistentAudioJson?.optJSONObject(song.id)?.let { obj ->
            val q = deserializeAudioQuality(obj)
            audioMemoryCache.put(song.id, q)
            return@withContext q
        }

        // Perform deep inspection
        val extracted = extractAudioQualityInternal(context, song)
        audioMemoryCache.put(song.id, extracted)

        // Save to persistent storage
        try {
            persistentAudioJson?.put(song.id, serializeAudioQuality(extracted))
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_AUDIO_CACHE, persistentAudioJson.toString()).apply()
        } catch (_: Exception) {}

        extracted
    }

    /**
     * Fast synchronous video quality lookup.
     */
    fun getVideoQualitySync(video: VideoItem): VideoQuality {
        val cached = videoMemoryCache.get(video.id)
        if (cached != null) return cached

        persistentVideoJson?.optJSONObject(video.id)?.let { obj ->
            val q = deserializeVideoQuality(obj)
            videoMemoryCache.put(video.id, q)
            return q
        }

        return computeVideoHeuristic(video)
    }

    /**
     * Deep background video quality extraction using MediaMetadataRetriever and MediaExtractor.
     */
    suspend fun getVideoQuality(context: Context, video: VideoItem): VideoQuality = withContext(Dispatchers.IO) {
        initPersistentCaches(context)

        val cached = videoMemoryCache.get(video.id)
        if (cached != null) return@withContext cached

        persistentVideoJson?.optJSONObject(video.id)?.let { obj ->
            val q = deserializeVideoQuality(obj)
            videoMemoryCache.put(video.id, q)
            return@withContext q
        }

        val extracted = extractVideoQualityInternal(context, video)
        videoMemoryCache.put(video.id, extracted)

        try {
            persistentVideoJson?.put(video.id, serializeVideoQuality(extracted))
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_VIDEO_CACHE, persistentVideoJson.toString()).apply()
        } catch (_: Exception) {}

        extracted
    }

    // --- Audio Quality Extraction ---

    private fun extractAudioQualityInternal(context: Context, song: Song): AudioQuality {
        var format = song.uriString.substringAfterLast('.', "").uppercase().takeIf { it.isNotBlank() && it.length <= 5 } ?: "AUDIO"
        var bitrateKbps = 0
        var sampleRateHz = 0
        var bitDepth: Int? = null
        var channelCount = 2
        var fileSize = 0L

        val uriStr = song.uriString

        // 1. Calculate file size if content / file
        try {
            if (uriStr.startsWith("content://")) {
                context.contentResolver.query(Uri.parse(uriStr), arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val sIdx = c.getColumnIndex(OpenableColumns.SIZE)
                        if (sIdx >= 0) fileSize = c.getLong(sIdx)
                    }
                }
            } else {
                val f = File(uriStr.removePrefix("file://"))
                if (f.exists()) fileSize = f.length()
            }
        } catch (_: Exception) {}

        // 2. MediaMetadataRetriever inspection
        val retriever = MediaMetadataRetriever()
        var retrieverSet = false
        try {
            if (uriStr.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(uriStr))
                retrieverSet = true
            } else {
                val f = File(uriStr.removePrefix("file://"))
                if (f.exists() && f.canRead()) {
                    retriever.setDataSource(f.absolutePath)
                    retrieverSet = true
                }
            }

            if (retrieverSet) {
                val bStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                bStr?.toLongOrNull()?.let {
                    if (it > 0) bitrateKbps = (it / 1000).toInt()
                }

                val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                if (!mime.isNullOrBlank()) {
                    val sub = mime.substringAfterLast('/').uppercase()
                    if (sub.isNotBlank()) {
                        format = when {
                            sub.contains("MPEG") || sub.contains("MP3") -> "MP3"
                            sub.contains("FLAC") -> "FLAC"
                            sub.contains("WAV") || sub.contains("X-WAV") -> "WAV"
                            sub.contains("MP4") || sub.contains("AAC") || sub.contains("M4A") -> "AAC"
                            sub.contains("OGG") || sub.contains("VORBIS") -> "OGG"
                            sub.contains("OPUS") -> "OPUS"
                            sub.contains("ALAC") -> "ALAC"
                            sub.contains("AIFF") -> "AIFF"
                            else -> sub
                        }
                    }
                }

                val srStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                srStr?.toIntOrNull()?.let { if (it > 0) sampleRateHz = it }

                if (Build.VERSION.SDK_INT >= 31) {
                    try {
                        val bitsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                        bitsStr?.toIntOrNull()?.let { if (it > 0) bitDepth = it }
                    } catch (_: Throwable) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio retriever failed for ${song.title}: ${e.message}")
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }

        // 3. MediaExtractor inspection for bit-depth, exact sample rate & channels
        try {
            val extractor = MediaExtractor()
            if (uriStr.startsWith("content://")) {
                extractor.setDataSource(context, Uri.parse(uriStr), null)
            } else {
                extractor.setDataSource(uriStr.removePrefix("file://"))
            }

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    if (sampleRateHz == 0 && trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (bitrateKbps == 0 && trackFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        val b = trackFormat.getInteger(MediaFormat.KEY_BIT_RATE)
                        if (b > 0) bitrateKbps = b / 1000
                    }
                    if (bitDepth == null) {
                        if (Build.VERSION.SDK_INT >= 24 && trackFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            val enc = trackFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            bitDepth = when (enc) {
                                2 -> 16 // AudioFormat.ENCODING_PCM_16BIT
                                3 -> 8  // AudioFormat.ENCODING_PCM_8BIT
                                4 -> 32 // AudioFormat.ENCODING_PCM_FLOAT
                                else -> null
                            }
                        }
                    }
                    break
                }
            }
            extractor.release()
        } catch (_: Exception) {}

        // Fallback bitrate calculation from size and duration
        if (bitrateKbps == 0 && fileSize > 0 && song.durationMs > 0) {
            bitrateKbps = ((fileSize * 8) / song.durationMs).toInt()
        }

        // Defaults if unresolved
        if (sampleRateHz == 0) sampleRateHz = 44100
        val isLosslessFormat = format in setOf("FLAC", "WAV", "ALAC", "AIFF")

        // Quality Tier classification
        val tier = when {
            isLosslessFormat -> AudioQualityTier.LOSSLESS
            bitrateKbps >= 256 -> AudioQualityTier.HIGH
            bitrateKbps in 128..255 -> AudioQualityTier.STANDARD
            bitrateKbps in 1..127 -> AudioQualityTier.LOW
            else -> AudioQualityTier.STANDARD
        }

        // Hi-Res classification: 24-bit+ or sample rate > 48000 Hz
        val isHiRes = (bitDepth != null && bitDepth >= 24) || (sampleRateHz > 48000)

        // Badge text: "FLAC", "MP3 320", "AAC 256", "WAV"
        val badgeText = if (isLosslessFormat) {
            format
        } else if (bitrateKbps > 0) {
            "$format $bitrateKbps"
        } else {
            format
        }

        return AudioQuality(
            format = format,
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            channelCount = channelCount,
            tier = tier,
            isHiRes = isHiRes,
            badgeText = badgeText
        )
    }

    private fun computeAudioHeuristic(song: Song): AudioQuality {
        val format = song.uriString.substringAfterLast('.', "").uppercase().takeIf { it.isNotBlank() && it.length <= 5 } ?: "AUDIO"
        val isLossless = format in setOf("FLAC", "WAV", "ALAC", "AIFF") || song.isDemoTrack
        val tier = if (isLossless) AudioQualityTier.LOSSLESS else AudioQualityTier.HIGH
        val defaultBitrate = if (isLossless) 0 else 320
        return AudioQuality(
            format = if (song.isDemoTrack) "WAV" else format,
            bitrateKbps = defaultBitrate,
            sampleRateHz = 44100,
            bitDepth = 16,
            channelCount = 2,
            tier = tier,
            isHiRes = false,
            badgeText = if (isLossless) (if (song.isDemoTrack) "WAV" else format) else "$format 320"
        )
    }

    // --- Video Quality Extraction ---

    private fun extractVideoQualityInternal(context: Context, video: VideoItem): VideoQuality {
        var width = 0
        var height = 0
        var fps = 0
        var videoCodec = "Unknown"
        var audioCodec = "Unknown"
        var videoBitrateKbps = 0
        var isHdr = false
        var fileSize = 0L

        val uriStr = video.uriString

        // Calculate file size
        try {
            if (uriStr.startsWith("content://")) {
                context.contentResolver.query(Uri.parse(uriStr), arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val sIdx = c.getColumnIndex(OpenableColumns.SIZE)
                        if (sIdx >= 0) fileSize = c.getLong(sIdx)
                    }
                }
            } else {
                val f = File(uriStr.removePrefix("file://"))
                if (f.exists()) fileSize = f.length()
            }
        } catch (_: Exception) {}

        // MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        var retrieverSet = false
        try {
            if (uriStr.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(uriStr))
                retrieverSet = true
            } else {
                val f = File(uriStr.removePrefix("file://"))
                if (f.exists() && f.canRead()) {
                    retriever.setDataSource(f.absolutePath)
                    retrieverSet = true
                }
            }

            if (retrieverSet) {
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

                // In portrait videos, rotation swaps dimensions
                if (rot == 90 || rot == 270) {
                    width = h
                    height = w
                } else {
                    width = w
                    height = h
                }

                val bStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                bStr?.toLongOrNull()?.let { if (it > 0) videoBitrateKbps = (it / 1000).toInt() }

                if (Build.VERSION.SDK_INT >= 23) {
                    val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    fpsStr?.toFloatOrNull()?.let { if (it > 0) fps = it.toInt() }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Video retriever failed for ${video.title}: ${e.message}")
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }

        // MediaExtractor inspection for exact codecs, frame rate & HDR
        var rawVideoMime = ""
        try {
            val extractor = MediaExtractor()
            if (uriStr.startsWith("content://")) {
                extractor.setDataSource(context, Uri.parse(uriStr), null)
            } else {
                extractor.setDataSource(uriStr.removePrefix("file://"))
            }

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("video/")) {
                    rawVideoMime = mime
                    videoCodec = mapVideoMimeToFriendlyName(mime)

                    if (fps == 0 && trackFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        fps = try {
                            trackFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
                        } catch (_: Exception) {
                            trackFormat.getFloat(MediaFormat.KEY_FRAME_RATE).toInt()
                        }
                    }

                    if (width == 0 && trackFormat.containsKey(MediaFormat.KEY_WIDTH)) {
                        width = trackFormat.getInteger(MediaFormat.KEY_WIDTH)
                    }
                    if (height == 0 && trackFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
                        height = trackFormat.getInteger(MediaFormat.KEY_HEIGHT)
                    }

                    // Check HDR (BT2020 or ST2084 / HLG)
                    if (Build.VERSION.SDK_INT >= 24) {
                        if (trackFormat.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                            val transfer = trackFormat.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                            if (transfer == MediaFormat.COLOR_TRANSFER_ST2084 || transfer == MediaFormat.COLOR_TRANSFER_HLG) {
                                isHdr = true
                            }
                        }
                    }
                } else if (mime.startsWith("audio/")) {
                    audioCodec = mapAudioMimeToFriendlyName(mime)
                }
            }
            extractor.release()
        } catch (_: Exception) {}

        // Fallback dimensions if unread (demo visualizer is 1080p FHD)
        if (width == 0 || height == 0) {
            if (video.isDemo) {
                width = 1920
                height = 1080
                fps = 60
                videoCodec = "H.264"
                audioCodec = "AAC"
            } else {
                width = 1280
                height = 720
                fps = 30
            }
        }

        // Determine resolution tier
        val minDim = minOf(width, height)
        val resolutionTier = when {
            minDim >= 4320 -> VideoResolutionTier.UHD_8K
            minDim >= 2160 -> VideoResolutionTier.UHD_4K
            minDim >= 1440 -> VideoResolutionTier.QHD_2K
            minDim >= 1080 -> VideoResolutionTier.FHD
            minDim >= 720 -> VideoResolutionTier.HD
            else -> VideoResolutionTier.SD
        }

        // Check hardware/software decoder support
        val isSupported = if (rawVideoMime.isNotBlank()) isVideoCodecSupported(rawVideoMime) else true

        // Formatted file size
        val sizeFormatted = when {
            fileSize >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", fileSize.toDouble() / (1024 * 1024 * 1024))
            fileSize >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", fileSize.toDouble() / (1024 * 1024))
            fileSize >= 1024 -> String.format(Locale.US, "%.0f KB", fileSize.toDouble() / 1024)
            fileSize > 0 -> "$fileSize bytes"
            video.isDemo -> "12.4 MB"
            else -> ""
        }

        return VideoQuality(
            width = width,
            height = height,
            fps = fps,
            videoCodec = videoCodec,
            videoBitrateKbps = videoBitrateKbps,
            audioCodec = audioCodec,
            isHdr = isHdr,
            resolutionTier = resolutionTier,
            isSupported = isSupported,
            fileSizeFormatted = sizeFormatted
        )
    }

    private fun computeVideoHeuristic(video: VideoItem): VideoQuality {
        if (video.isDemo) {
            return VideoQuality(
                width = 1920,
                height = 1080,
                fps = 60,
                videoCodec = "H.264",
                videoBitrateKbps = 3200,
                audioCodec = "AAC",
                isHdr = false,
                resolutionTier = VideoResolutionTier.FHD,
                isSupported = true,
                fileSizeFormatted = "12.4 MB"
            )
        }
        val lower = video.title.lowercase(Locale.ROOT)
        val tier = when {
            lower.contains("8k") -> VideoResolutionTier.UHD_8K
            lower.contains("4k") || lower.contains("2160") -> VideoResolutionTier.UHD_4K
            lower.contains("2k") || lower.contains("1440") -> VideoResolutionTier.QHD_2K
            lower.contains("1080") || lower.contains("fhd") -> VideoResolutionTier.FHD
            lower.contains("720") || lower.contains("hd") -> VideoResolutionTier.HD
            else -> VideoResolutionTier.HD
        }
        val is60 = lower.contains("60fps") || lower.contains("60p")
        val isHdr = lower.contains("hdr")
        return VideoQuality(
            width = if (tier == VideoResolutionTier.FHD) 1920 else 1280,
            height = if (tier == VideoResolutionTier.FHD) 1080 else 720,
            fps = if (is60) 60 else 30,
            videoCodec = "H.264",
            videoBitrateKbps = 2400,
            audioCodec = "AAC",
            isHdr = isHdr,
            resolutionTier = tier,
            isSupported = true,
            fileSizeFormatted = ""
        )
    }

    fun isVideoCodecSupported(mimeType: String): Boolean {
        return try {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            list.codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun mapVideoMimeToFriendlyName(mime: String): String {
        return when {
            mime.contains("avc", ignoreCase = true) -> "H.264 / AVC"
            mime.contains("hevc", ignoreCase = true) -> "H.265 / HEVC"
            mime.contains("vp9", ignoreCase = true) -> "VP9"
            mime.contains("vp8", ignoreCase = true) -> "VP8"
            mime.contains("av01", ignoreCase = true) -> "AV1"
            mime.contains("mp4v", ignoreCase = true) -> "MPEG-4"
            mime.contains("3gpp", ignoreCase = true) -> "H.263"
            else -> mime.substringAfterLast('/').uppercase()
        }
    }

    private fun mapAudioMimeToFriendlyName(mime: String): String {
        return when {
            mime.contains("mp4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
            mime.contains("ac3", ignoreCase = true) -> "Dolby AC3"
            mime.contains("eac3", ignoreCase = true) -> "E-AC3"
            mime.contains("opus", ignoreCase = true) -> "Opus"
            mime.contains("flac", ignoreCase = true) -> "FLAC"
            mime.contains("vorbis", ignoreCase = true) -> "Vorbis"
            mime.contains("raw", ignoreCase = true) || mime.contains("pcm", ignoreCase = true) -> "PCM"
            else -> mime.substringAfterLast('/').uppercase()
        }
    }

    // --- JSON Serialization for Local Cache Persistence ---

    private fun serializeAudioQuality(q: AudioQuality): JSONObject {
        return JSONObject().apply {
            put("format", q.format)
            put("bitrateKbps", q.bitrateKbps)
            put("sampleRateHz", q.sampleRateHz)
            put("bitDepth", q.bitDepth ?: -1)
            put("channelCount", q.channelCount)
            put("tier", q.tier.name)
            put("isHiRes", q.isHiRes)
            put("badgeText", q.badgeText)
        }
    }

    private fun deserializeAudioQuality(obj: JSONObject): AudioQuality {
        val bDepth = obj.optInt("bitDepth", -1).takeIf { it > 0 }
        val tier = try {
            AudioQualityTier.valueOf(obj.optString("tier", "STANDARD"))
        } catch (_: Exception) { AudioQualityTier.STANDARD }

        return AudioQuality(
            format = obj.optString("format", "AUDIO"),
            bitrateKbps = obj.optInt("bitrateKbps", 0),
            sampleRateHz = obj.optInt("sampleRateHz", 44100),
            bitDepth = bDepth,
            channelCount = obj.optInt("channelCount", 2),
            tier = tier,
            isHiRes = obj.optBoolean("isHiRes", false),
            badgeText = obj.optString("badgeText", "AUDIO")
        )
    }

    private fun serializeVideoQuality(q: VideoQuality): JSONObject {
        return JSONObject().apply {
            put("width", q.width)
            put("height", q.height)
            put("fps", q.fps)
            put("videoCodec", q.videoCodec)
            put("videoBitrateKbps", q.videoBitrateKbps)
            put("audioCodec", q.audioCodec)
            put("isHdr", q.isHdr)
            put("resolutionTier", q.resolutionTier.name)
            put("isSupported", q.isSupported)
            put("fileSizeFormatted", q.fileSizeFormatted)
        }
    }

    private fun deserializeVideoQuality(obj: JSONObject): VideoQuality {
        val resTier = try {
            VideoResolutionTier.valueOf(obj.optString("resolutionTier", "SD"))
        } catch (_: Exception) { VideoResolutionTier.SD }

        return VideoQuality(
            width = obj.optInt("width", 0),
            height = obj.optInt("height", 0),
            fps = obj.optInt("fps", 0),
            videoCodec = obj.optString("videoCodec", "Unknown"),
            videoBitrateKbps = obj.optInt("videoBitrateKbps", 0),
            audioCodec = obj.optString("audioCodec", "Unknown"),
            isHdr = obj.optBoolean("isHdr", false),
            resolutionTier = resTier,
            isSupported = obj.optBoolean("isSupported", true),
            fileSizeFormatted = obj.optString("fileSizeFormatted", "")
        )
    }
}
