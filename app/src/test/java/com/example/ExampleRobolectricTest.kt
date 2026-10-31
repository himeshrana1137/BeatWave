package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ListeningStats
import com.example.model.Song
import com.example.model.VideoItem
import com.example.playback.EqualizerState
import com.example.playback.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BeatWave", appName)
    }

    @Test
    fun `song formatted duration test`() {
        val song = Song(
            id = "test_1",
            title = "Test Track",
            durationMs = 125000L, // 2 minutes 5 seconds
            uriString = "content://audio/1"
        )
        assertEquals("02:05", song.formattedDuration)
    }

    @Test
    fun `video formatted duration test`() {
        val video = VideoItem(
            id = "vid_1",
            title = "Test Video",
            durationMs = 3665000L, // 61 minutes 5 seconds
            uriString = "content://video/1"
        )
        assertEquals("61:05", video.formattedDuration)
    }

    @Test
    fun `equalizer state default presets test`() {
        val eqState = EqualizerState()
        assertTrue(eqState.isEnabled)
        assertTrue(eqState.availablePresets.contains("Bass Boost"))
        assertEquals(5, eqState.bandLevels.size)
    }

    @Test
    fun `lrc parser text parsing test`() {
        val sampleLrc = """
            [ti:Test Title]
            [00:02.50]First line of lyrics
            [00:05.80]Second line of lyrics
        """.trimIndent()

        val parsed = LrcParser.parseLrcText(sampleLrc)
        assertEquals(2, parsed.size)
        assertEquals(2500L, parsed[0].timeMs)
        assertEquals("First line of lyrics", parsed[0].text)
        assertEquals(5800L, parsed[1].timeMs)
        assertEquals("Second line of lyrics", parsed[1].text)
    }

    @Test
    fun `listening stats formatted total time test`() {
        val stats = ListeningStats(
            totalTracksPlayed = 15,
            totalListeningTimeMs = 7320000L // 2 hours 2 minutes
        )
        assertEquals("2h 2m", stats.formattedTotalTime)
    }

    @Test
    fun `album, artist, and folder grouping test`() {
        val song1 = Song(
            id = "s1",
            title = "Alpha",
            artist = "Solaris",
            album = "Cosmic Vol 1",
            folderName = "Synthwave",
            uriString = "content://audio/1"
        )
        val song2 = Song(
            id = "s2",
            title = "Beta",
            artist = "Solaris",
            album = "Cosmic Vol 2",
            folderName = "Synthwave",
            uriString = "content://audio/2"
        )
        val song3 = Song(
            id = "s3",
            title = "Gamma",
            artist = "Luna",
            album = "Moonlight",
            folderName = "Chill",
            uriString = "content://audio/3"
        )

        val songs = listOf(song1, song2, song3)

        // Albums grouping
        val albums = songs.groupBy { it.album }.map { (albumName, tracks) ->
            com.example.model.AlbumItem(
                title = albumName,
                artist = tracks.first().artist,
                songCount = tracks.size,
                songs = tracks
            )
        }
        assertEquals(3, albums.size)

        // Artists grouping
        val artists = songs.groupBy { it.artist }.map { (artistName, tracks) ->
            com.example.model.ArtistItem(
                name = artistName,
                songCount = tracks.size,
                albumCount = tracks.map { it.album }.distinct().size,
                songs = tracks
            )
        }
        val solaris = artists.first { it.name == "Solaris" }
        assertEquals(2, solaris.songCount)
        assertEquals(2, solaris.albumCount)

        // Folders grouping
        val folders = songs.groupBy { it.folderName }.map { (folderName, tracks) ->
            com.example.model.FolderItem(
                name = folderName,
                path = "/storage/emulated/0/$folderName",
                songCount = tracks.size,
                songs = tracks
            )
        }
        val synthFolder = folders.first { it.name == "Synthwave" }
        assertEquals(2, synthFolder.songCount)
    }
}
