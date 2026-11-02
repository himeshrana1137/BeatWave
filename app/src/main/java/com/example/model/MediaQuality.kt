package com.example.model

enum class AudioQualityTier(val label: String, val rank: Int) {
    LOSSLESS("Lossless", 4),
    HIGH("High", 3),
    STANDARD("Standard", 2),
    LOW("Low", 1)
}

data class AudioQuality(
    val format: String = "AUDIO",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val bitDepth: Int? = null,
    val channelCount: Int = 2,
    val tier: AudioQualityTier = AudioQualityTier.STANDARD,
    val isHiRes: Boolean = false,
    val badgeText: String = format
) {
    val sampleRateFormatted: String
        get() = if (sampleRateHz > 0) {
            val khz = sampleRateHz / 1000.0
            if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else "$khz kHz"
        } else ""

    val bitrateFormatted: String
        get() = if (bitrateKbps > 0) "$bitrateKbps kbps" else ""

    val channelsFormatted: String
        get() = when (channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1"
            8 -> "7.1"
            else -> if (channelCount > 0) "$channelCount Ch" else ""
        }

    val bitDepthFormatted: String
        get() = if (bitDepth != null && bitDepth > 0) "${bitDepth}-bit" else ""
}

enum class VideoResolutionTier(val label: String, val rank: Int) {
    UHD_8K("8K", 6),
    UHD_4K("4K", 5),
    QHD_2K("2K", 4),
    FHD("FHD", 3),
    HD("HD", 2),
    SD("SD", 1)
}

data class VideoQuality(
    val width: Int = 0,
    val height: Int = 0,
    val fps: Int = 0,
    val videoCodec: String = "Unknown",
    val videoBitrateKbps: Int = 0,
    val audioCodec: String = "Unknown",
    val isHdr: Boolean = false,
    val resolutionTier: VideoResolutionTier = VideoResolutionTier.SD,
    val isSupported: Boolean = true,
    val fileSizeFormatted: String = ""
) {
    val resolutionFormatted: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else ""

    val fpsFormatted: String
        get() = if (fps > 0) "$fps fps" else ""

    val bitrateFormatted: String
        get() = if (videoBitrateKbps > 0) "$videoBitrateKbps kbps" else ""
}

enum class AudioQualityFilter(val displayName: String) {
    ALL("All"),
    LOSSLESS("Lossless 💎"),
    HIGH("High (256k+)"),
    STANDARD("Standard"),
    LOW("Low (<128k)")
}

enum class VideoResolutionFilter(val displayName: String) {
    ALL("All"),
    UHD_4K_8K("4K / 8K"),
    FHD_1080P("1080p FHD"),
    HD_720P("720p HD"),
    SD("SD")
}

enum class VideoSortOption(val displayName: String) {
    DATE_DESC("Recently Added"),
    RESOLUTION_DESC("Highest Resolution"),
    RESOLUTION_ASC("Lowest Resolution"),
    DURATION_DESC("Longest Duration"),
    TITLE_ASC("Title A-Z")
}
