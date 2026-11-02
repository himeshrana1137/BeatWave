package com.example.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.Color
import com.example.model.Song
import com.example.ui.components.getPlaceholderGradient
import com.example.ui.theme.PrimaryViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Collections
import java.util.Locale

/**
 * Robust, memory-efficient album art loader with multi-tier fallback,
 * LRU memory caching, and disk caching optimized for low-RAM 32-bit and 64-bit devices.
 *
 * Fallback order:
 * 1. Embedded picture inside audio file via MediaMetadataRetriever.embeddedPicture
 * 2. On Android 10+ (API 29+) contentResolver.loadThumbnail(), on older Android MediaStore album art URI
 * 3. Folder artwork (folder.jpg, cover.jpg, album.jpg, front.jpg, albumart.jpg case-insensitive)
 * 4. Optional online cover art fetcher from public API (iTunes/Deezer) if enabled in settings
 * 5. Returns null to render procedural gradient placeholder with music note icon
 */
object AlbumArtLoader {

    private const val TAG = "AlbumArtLoader"
    const val SIZE_THUMBNAIL = 128
    const val SIZE_FULL = 512

    private const val PREFS_NAME = "harmoniq_music_storage"
    private const val KEY_FETCH_ONLINE_COVER = "key_fetch_online_cover"

    // Memory cache: sized proportionally to available runtime heap (1/8th of heap, clamped 4MB - 24MB)
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceIn(4096, 24576)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    // Dominant color cache for instant background gradient transitions in full-screen player
    private val dominantColorCache = LruCache<String, Int>(400)

    // Tracks song IDs that have no art to prevent repeated heavy I/O and retriever creation on scroll
    private val noArtCache = Collections.synchronizedSet(LinkedHashSet<String>())
    private const val MAX_NO_ART_CACHE_SIZE = 1500

    @Volatile
    private var onlineCoverFetchEnabled: Boolean? = null

    /**
     * Clears the "no art" cache, allowing retries when settings change (e.g. online cover enabled)
     * or when new media files are scanned.
     */
    fun clearNoArtCache() {
        noArtCache.clear()
    }

    fun setOnlineCoverFetchEnabled(enabled: Boolean) {
        onlineCoverFetchEnabled = enabled
        if (enabled) {
            clearNoArtCache()
        }
    }

    fun isOnlineCoverFetchEnabled(context: Context): Boolean {
        onlineCoverFetchEnabled?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_FETCH_ONLINE_COVER, false)
        onlineCoverFetchEnabled = enabled
        return enabled
    }

    /**
     * Instant synchronous memory cache lookup. Returns null if not in memory.
     */
    fun getCachedArt(songId: String?, targetSize: Int = SIZE_THUMBNAIL): Bitmap? {
        if (songId == null) return null
        val key = "${songId}_$targetSize"
        return memoryCache.get(key)
    }

    /**
     * Instant dominant color lookup from memory cache. Falls back to song-seeded placeholder color.
     */
    fun getCachedDominantColor(song: Song?): Color {
        if (song == null) return PrimaryViolet
        val cached = dominantColorCache.get(song.id)
        if (cached != null) return Color(cached)
        return getPlaceholderGradient(song.title).first()
    }

    /**
     * Asynchronously loads downsampled album art on Dispatchers.IO.
     */
    suspend fun loadArt(
        context: Context,
        song: Song,
        targetSize: Int = SIZE_THUMBNAIL
    ): Bitmap? = withContext(Dispatchers.IO) {
        loadArtInternal(context, song, targetSize)
    }

    /**
     * Synchronous blocking loader safe for background workers, widget providers, and playback service.
     */
    fun loadArtSync(
        context: Context,
        song: Song,
        targetSize: Int = SIZE_THUMBNAIL
    ): Bitmap? {
        return loadArtInternal(context, song, targetSize)
    }

    /**
     * Computes or loads dominant color from album art or cached result.
     */
    suspend fun loadDominantColor(context: Context, song: Song): Color = withContext(Dispatchers.IO) {
        val cached = dominantColorCache.get(song.id)
        if (cached != null) return@withContext Color(cached)

        val bitmap = loadArtInternal(context, song, SIZE_THUMBNAIL)
        val color = extractDominantColor(song, bitmap)
        color
    }

    private fun loadArtInternal(
        context: Context,
        song: Song,
        targetSize: Int
    ): Bitmap? {
        val key = "${song.id}_$targetSize"

        // 1. Check in-memory LruCache
        val inMemory = memoryCache.get(key)
        if (inMemory != null && !inMemory.isRecycled) {
            return inMemory
        }

        // 2. Fast check: song known to have no artwork
        if (noArtCache.contains(song.id)) {
            return null
        }

        // 3. Check persistent disk cache
        val diskFile = getDiskCacheFile(context, song.id, targetSize)
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val cachedBmp = BitmapFactory.decodeFile(diskFile.absolutePath, opts)
                if (cachedBmp != null) {
                    memoryCache.put(key, cachedBmp)
                    cacheDominantColorIfMissing(song, cachedBmp)
                    return cachedBmp
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading disk cache for ${song.title}: ${e.message}")
            }
        }

        // If not in disk cache, execute the 4-step fallback hierarchy
        var resolvedBitmap: Bitmap? = null

        // Step 1: Embedded picture inside audio file via MediaMetadataRetriever
        resolvedBitmap = loadEmbeddedPicture(context, song, targetSize)

        // Step 2: MediaStore thumbnail (Android 10+) or legacy MediaStore album art URI
        if (resolvedBitmap == null) {
            resolvedBitmap = loadMediaStoreArt(context, song, targetSize)
        }

        // Step 3: Folder image (folder.jpg, cover.jpg, album.jpg, front.jpg, albumart.jpg)
        if (resolvedBitmap == null) {
            resolvedBitmap = loadFolderArt(song, targetSize)
        }

        // Step 4 (Optional): Online cover fetch if internet available & setting enabled
        if (resolvedBitmap == null && isOnlineCoverFetchEnabled(context)) {
            resolvedBitmap = fetchOnlineCover(context, song, targetSize)
        }

        if (resolvedBitmap != null) {
            // Put in memory cache
            memoryCache.put(key, resolvedBitmap)

            // Save to disk cache asynchronously
            saveToDiskCache(diskFile, resolvedBitmap)

            // Pre-calculate dominant color
            cacheDominantColorIfMissing(song, resolvedBitmap)
            return resolvedBitmap
        } else {
            // Remember "no art found" to avoid repeating expensive I/O on every scroll
            if (noArtCache.size >= MAX_NO_ART_CACHE_SIZE) {
                val first = noArtCache.iterator().next()
                noArtCache.remove(first)
            }
            noArtCache.add(song.id)
            return null
        }
    }

    /**
     * Fallback 1: Extract embedded album artwork from audio file tags.
     * Accurately parses ID3v2 APIC/PIC tags and MP4 covr atoms directly in Kotlin to prevent
     * invoking native MediaMetadataRetriever on files without embedded pictures (which triggers C++ ALOGE).
     */
    private fun loadEmbeddedPicture(
        context: Context,
        song: Song,
        targetSize: Int
    ): Bitmap? {
        // Skip synthesized demo tracks and formats without picture support
        if (song.isDemoTrack) return null
        if (song.uriString.endsWith(".wav", ignoreCase = true) ||
            song.uriString.endsWith(".mid", ignoreCase = true) ||
            song.uriString.endsWith(".midi", ignoreCase = true)) {
            return null
        }

        // 1. Try pure Kotlin ID3/APIC embedded picture extractor from URI
        if (song.uriString.startsWith("content://")) {
            val artBytes = extractEmbeddedPictureBytes(context, Uri.parse(song.uriString))
            if (artBytes != null && artBytes.isNotEmpty()) {
                val bmp = decodeSampledBitmap(artBytes, targetSize, targetSize)
                if (bmp != null) return bmp
            }
        }

        // 2. Try pure Kotlin ID3/APIC embedded picture extractor from file
        val rawPath = song.uriString.removePrefix("file://")
        val directFile = File(rawPath)
        if (directFile.exists() && directFile.isFile && directFile.canRead()) {
            val artBytes = extractEmbeddedPictureBytes(directFile)
            if (artBytes != null && artBytes.isNotEmpty()) {
                val bmp = decodeSampledBitmap(artBytes, targetSize, targetSize)
                if (bmp != null) return bmp
            }
        }

        // 3. Try folder search for audio file matching title
        if (song.folderPath.isNotBlank()) {
            val dir = File(song.folderPath)
            if (dir.exists() && dir.isDirectory) {
                val matchingFile = dir.listFiles()?.firstOrNull { file ->
                    file.isFile && (file.name.contains(song.title, ignoreCase = true) ||
                            song.title.contains(file.nameWithoutExtension, ignoreCase = true))
                }
                if (matchingFile != null && matchingFile.canRead() && !matchingFile.name.endsWith(".wav", ignoreCase = true)) {
                    val artBytes = extractEmbeddedPictureBytes(matchingFile)
                    if (artBytes != null && artBytes.isNotEmpty()) {
                        val bmp = decodeSampledBitmap(artBytes, targetSize, targetSize)
                        if (bmp != null) return bmp
                    }
                }
            }
        }

        // 4. For M4A/MP4 where ID3 is not present, only invoke retriever if 'covr' atom is verified
        if (hasM4aCover(context, song)) {
            val retriever = MediaMetadataRetriever()
            try {
                if (song.uriString.startsWith("content://")) {
                    retriever.setDataSource(context, Uri.parse(song.uriString))
                } else if (directFile.exists() && directFile.isFile) {
                    retriever.setDataSource(directFile.absolutePath)
                }
                val artBytes = retriever.embeddedPicture
                if (artBytes != null && artBytes.isNotEmpty()) {
                    return decodeSampledBitmap(artBytes, targetSize, targetSize)
                }
            } catch (_: Exception) {
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }
        }

        return null
    }

    private fun hasM4aCover(context: Context, song: Song): Boolean {
        if (!song.uriString.contains(".m4a", ignoreCase = true) &&
            !song.uriString.contains(".mp4", ignoreCase = true)) {
            return false
        }
        return try {
            val stream = if (song.uriString.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(song.uriString))
            } else {
                val f = File(song.uriString.removePrefix("file://"))
                if (f.exists()) f.inputStream() else null
            }
            stream?.use { s ->
                val buf = ByteArray(65536)
                val read = s.read(buf)
                if (read > 0) {
                    val covrBytes = byteArrayOf('c'.code.toByte(), 'o'.code.toByte(), 'v'.code.toByte(), 'r'.code.toByte())
                    indexOf(buf, covrBytes, 0) != -1
                } else false
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun extractEmbeddedPictureBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                extractEmbeddedPictureFromStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun extractEmbeddedPictureBytes(file: File): ByteArray? {
        return try {
            if (file.exists() && file.isFile && file.canRead()) {
                file.inputStream().use { stream ->
                    extractEmbeddedPictureFromStream(stream)
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractEmbeddedPictureFromStream(stream: InputStream): ByteArray? {
        val bis = java.io.BufferedInputStream(stream, 32768)
        val header = ByteArray(10)
        bis.mark(10)
        val read = bis.read(header, 0, 10)
        if (read < 10) return null

        // ID3v2 header: "ID3"
        if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
            val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                          ((header[7].toInt() and 0x7F) shl 14) or
                          ((header[8].toInt() and 0x7F) shl 7) or
                          (header[9].toInt() and 0x7F)

            if (tagSize <= 0) return null
            val maxRead = tagSize.coerceAtMost(5 * 1024 * 1024)
            val tagBytes = ByteArray(maxRead)
            var total = 0
            while (total < maxRead) {
                val r = bis.read(tagBytes, total, maxRead - total)
                if (r <= 0) break
                total += r
            }

            return extractApicFromId3(tagBytes)
        }

        // FLAC header "fLaC"
        if (header[0] == 'f'.code.toByte() && header[1] == 'L'.code.toByte() &&
            header[2] == 'a'.code.toByte() && header[3] == 'C'.code.toByte()) {
            return extractFlacPicture(bis)
        }

        return null
    }

    private fun extractApicFromId3(data: ByteArray): ByteArray? {
        val apicBytes = byteArrayOf('A'.code.toByte(), 'P'.code.toByte(), 'I'.code.toByte(), 'C'.code.toByte())
        val picBytes = byteArrayOf('P'.code.toByte(), 'I'.code.toByte(), 'C'.code.toByte())

        var idx = indexOf(data, apicBytes, 0)
        val isV22 = (idx == -1 && indexOf(data, picBytes, 0).also { idx = it } != -1)
        if (idx == -1) return null

        val headerLen = if (isV22) 6 else 10
        val searchStart = idx + headerLen
        if (searchStart >= data.size) return null

        val searchLimit = (searchStart + 256).coerceAtMost(data.size - 4)
        for (i in searchStart until searchLimit) {
            // JPEG: FF D8 FF
            if (data[i] == 0xFF.toByte() && data[i + 1] == 0xD8.toByte() && data[i + 2] == 0xFF.toByte()) {
                val imgStart = i
                return data.copyOfRange(imgStart, data.size)
            }
            // PNG: 89 50 4E 47
            if (data[i] == 0x89.toByte() && data[i + 1] == 0x50.toByte() &&
                data[i + 2] == 0x4E.toByte() && data[i + 3] == 0x47.toByte()) {
                val imgStart = i
                return data.copyOfRange(imgStart, data.size)
            }
            // WebP
            if (data[i] == 'R'.code.toByte() && data[i + 1] == 'I'.code.toByte() &&
                data[i + 2] == 'F'.code.toByte() && data[i + 3] == 'F'.code.toByte() &&
                i + 11 < data.size && data[i + 8] == 'W'.code.toByte() && data[i + 9] == 'E'.code.toByte()) {
                val imgStart = i
                return data.copyOfRange(imgStart, data.size)
            }
        }
        return null
    }

    private fun extractFlacPicture(bis: java.io.BufferedInputStream): ByteArray? {
        try {
            while (true) {
                val bHeader = ByteArray(4)
                if (bis.read(bHeader, 0, 4) < 4) break
                val isLast = (bHeader[0].toInt() and 0x80) != 0
                val blockType = bHeader[0].toInt() and 0x7F
                val blockLen = ((bHeader[1].toInt() and 0xFF) shl 16) or
                               ((bHeader[2].toInt() and 0xFF) shl 8) or
                               (bHeader[3].toInt() and 0xFF)

                if (blockType == 6) { // METADATA_BLOCK_PICTURE
                    val pData = ByteArray(blockLen.coerceAtMost(5 * 1024 * 1024))
                    var readP = 0
                    while (readP < pData.size) {
                        val r = bis.read(pData, readP, pData.size - readP)
                        if (r <= 0) break
                        readP += r
                    }
                    for (i in 0 until (pData.size - 4).coerceAtMost(256)) {
                        if (pData[i] == 0xFF.toByte() && pData[i + 1] == 0xD8.toByte() && pData[i + 2] == 0xFF.toByte()) {
                            return pData.copyOfRange(i, pData.size)
                        }
                        if (pData[i] == 0x89.toByte() && pData[i + 1] == 0x50.toByte() &&
                            pData[i + 2] == 0x4E.toByte() && pData[i + 3] == 0x47.toByte()) {
                            return pData.copyOfRange(i, pData.size)
                        }
                    }
                } else {
                    bis.skip(blockLen.toLong())
                }
                if (isLast) break
            }
        } catch (_: Exception) {}
        return null
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (fromIndex >= source.size || target.isEmpty()) return -1
        outer@ for (i in fromIndex..(source.size - target.size)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    /**
     * Fallback 2: MediaStore thumbnail API on Android 10+ or legacy MediaStore albumart URI.
     */
    private fun loadMediaStoreArt(
        context: Context,
        song: Song,
        targetSize: Int
    ): Bitmap? {
        // Android 10+ (Q, API 29+): ContentResolver.loadThumbnail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentUri: Uri? = when {
                    song.uriString.startsWith("content://") -> Uri.parse(song.uriString)
                    song.id.startsWith("media_") -> {
                        val mediaId = song.id.removePrefix("media_").toLongOrNull()
                        if (mediaId != null) {
                            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
                        } else null
                    }
                    else -> null
                }

                if (contentUri != null) {
                    val bitmap = context.contentResolver.loadThumbnail(
                        contentUri,
                        Size(targetSize, targetSize),
                        null
                    )
                    if (bitmap != null) return bitmap
                }
            } catch (_: Exception) {
                // Expected for tracks without MediaStore thumbnail
            }
        }

        // Pre-Android 10 or fallback: MediaStore albumart URI
        try {
            val candidateUris = mutableListOf<Uri>()

            if (!song.albumArtPath.isNullOrBlank() && song.albumArtPath.startsWith("content://")) {
                candidateUris.add(Uri.parse(song.albumArtPath))
            }

            if (song.id.startsWith("media_")) {
                val rawId = song.id.removePrefix("media_")
                candidateUris.add(Uri.parse("content://media/external/audio/media/$rawId/albumart"))
            }

            for (artUri in candidateUris) {
                try {
                    val bmp = decodeSampledBitmapFromUri(context, artUri, targetSize, targetSize)
                    if (bmp != null) return bmp
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Fallback 3: Folder image files (folder.jpg, cover.jpg, album.jpg, front.jpg, albumart.jpg).
     */
    private fun loadFolderArt(
        song: Song,
        targetSize: Int
    ): Bitmap? {
        val candidateFolders = mutableListOf<File>()

        if (song.folderPath.isNotBlank()) {
            candidateFolders.add(File(song.folderPath))
        }

        val rawUri = song.uriString.removePrefix("file://")
        if (!rawUri.startsWith("content://")) {
            val parent = File(rawUri).parentFile
            if (parent != null) candidateFolders.add(parent)
        }

        if (!song.albumArtPath.isNullOrBlank() && !song.albumArtPath.startsWith("content://")) {
            val artFile = File(song.albumArtPath)
            if (artFile.exists() && artFile.isFile) {
                val bmp = decodeSampledBitmapFromFile(artFile, targetSize, targetSize)
                if (bmp != null) return bmp
            } else if (artFile.parentFile != null) {
                candidateFolders.add(artFile.parentFile)
            }
        }

        val validNames = setOf(
            "folder.jpg", "folder.jpeg", "folder.png",
            "cover.jpg", "cover.jpeg", "cover.png",
            "album.jpg", "album.jpeg", "album.png",
            "front.jpg", "front.jpeg", "front.png",
            "albumart.jpg", "albumart.jpeg", "albumart.png"
        )

        for (folder in candidateFolders) {
            try {
                if (folder.exists() && folder.isDirectory) {
                    val matchingFile = folder.listFiles()?.firstOrNull { file ->
                        file.isFile && file.name.lowercase(Locale.ROOT) in validNames
                    }
                    if (matchingFile != null && matchingFile.canRead()) {
                        val bmp = decodeSampledBitmapFromFile(matchingFile, targetSize, targetSize)
                        if (bmp != null) return bmp
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking folder art in ${folder.path}: ${e.message}")
            }
        }

        return null
    }

    /**
     * Fallback 4 (Optional): Online cover fetcher from iTunes Search API.
     * Caches downloaded cover into app cache directory and never alters user audio files.
     */
    private fun fetchOnlineCover(
        context: Context,
        song: Song,
        targetSize: Int
    ): Bitmap? {
        if (!isNetworkConnected(context)) return null
        if (song.title.isBlank() || song.title.startsWith("Track ") || song.isDemoTrack) return null

        try {
            val queryText = if (song.artist != "Unknown Artist" && song.artist.isNotBlank()) {
                "${song.title} ${song.artist}"
            } else {
                song.title
            }

            val encoded = URLEncoder.encode(queryText.trim(), "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=1")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "BeatWave/1.0")
            }

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val root = JSONObject(jsonString)
                val results = root.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val firstItem = results.getJSONObject(0)
                    var artUrl = firstItem.optString("artworkUrl100", null)
                    if (!artUrl.isNullOrBlank()) {
                        // Request high-res 512x512 artwork from iTunes CDN
                        val highResDimension = "${targetSize.coerceAtLeast(512)}x${targetSize.coerceAtLeast(512)}bb.jpg"
                        artUrl = artUrl.replace("100x100bb.jpg", highResDimension)

                        val imgConn = (URL(artUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 5000
                            readTimeout = 5000
                        }

                        if (imgConn.responseCode == 200) {
                            val bytes = imgConn.inputStream.use { it.readBytes() }
                            imgConn.disconnect()
                            if (bytes.isNotEmpty()) {
                                return decodeSampledBitmap(bytes, targetSize, targetSize)
                            }
                        }
                    }
                }
            } else {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online cover art fetch failed for ${song.title}: ${e.message}")
        }

        return null
    }

    private fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm?.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val info = cm?.activeNetworkInfo
                info?.isConnected == true
            }
        } catch (_: Exception) {
            false
        }
    }

    // --- Downsampling & Image Decoding Helpers ---

    private fun decodeSampledBitmap(
        bytes: ByteArray,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // 50% less RAM for 32-bit devices

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding byte array: ${e.message}")
            null
        }
    }

    private fun decodeSampledBitmapFromFile(
        file: File,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding file ${file.name}: ${e.message}")
            null
        }
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    // --- Disk Cache Management ---

    private fun getDiskCacheFile(context: Context, songId: String, targetSize: Int): File {
        val dir = File(context.cacheDir, "album_art_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val safeName = songId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return File(dir, "art_${safeName}_${targetSize}.jpg")
    }

    private fun saveToDiskCache(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write disk cache for ${file.name}: ${e.message}")
        }
    }

    // --- Dominant Color Extraction ---

    private fun cacheDominantColorIfMissing(song: Song, bitmap: Bitmap) {
        if (dominantColorCache.get(song.id) == null) {
            extractDominantColor(song, bitmap)
        }
    }

    private fun extractDominantColor(song: Song, bitmap: Bitmap?): Color {
        val cached = dominantColorCache.get(song.id)
        if (cached != null) return Color(cached)

        if (bitmap != null && !bitmap.isRecycled) {
            try {
                var redSum = 0L
                var greenSum = 0L
                var blueSum = 0L
                var count = 0
                val stepX = (bitmap.width / 10).coerceAtLeast(1)
                val stepY = (bitmap.height / 10).coerceAtLeast(1)

                for (x in 0 until bitmap.width step stepX) {
                    for (y in 0 until bitmap.height step stepY) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = android.graphics.Color.red(pixel)
                        val g = android.graphics.Color.green(pixel)
                        val b = android.graphics.Color.blue(pixel)
                        val brightness = (r + g + b) / 3
                        // Filter out extreme black and pure white to extract rich brand accent
                        if (brightness in 35..220) {
                            redSum += r
                            greenSum += g
                            blueSum += b
                            count++
                        }
                    }
                }

                if (count > 0) {
                    val avgR = (redSum / count).toInt()
                    val avgG = (greenSum / count).toInt()
                    val avgB = (blueSum / count).toInt()
                    val colorInt = android.graphics.Color.rgb(avgR, avgG, avgB)
                    dominantColorCache.put(song.id, colorInt)
                    return Color(colorInt)
                }
            } catch (_: Exception) {}
        }

        val fallback = getPlaceholderGradient(song.title).first()
        return fallback
    }
}
