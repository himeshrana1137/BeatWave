package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ListeningStatsOverview
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
            title = "Midnight Horizon",
            artist = "Aether",
            durationMs = 125000L, // 2 minutes 5 seconds
            uriString = "content://audio/1"
        )
        assertEquals("02:05", song.formattedDuration)
    }

    @Test
    fun `listening stats formatted total time test`() {
        val overview = ListeningStatsOverview(
            totalPlays = 42,
            totalListeningTimeMs = 152100000L // 42 hours 15 minutes
        )
        assertEquals("42 h 15 min", overview.formattedTotalTime)
    }

    @Test
    fun `listening stats formatted short time test`() {
        val overview = ListeningStatsOverview(
            totalPlays = 3,
            totalListeningTimeMs = 45000L // 45 seconds
        )
        assertEquals("45 sec", overview.formattedTotalTime)
    }

    @Test
    fun `play count threshold logic test`() {
        // Condition: >= 30 seconds or >= 50% if song < 30 seconds
        fun shouldCountPlay(songDurationMs: Long, listenedMs: Long): Boolean {
            return if (songDurationMs < 30_000L) {
                listenedMs >= (songDurationMs / 2)
            } else {
                listenedMs >= 30_000L
            }
        }

        // Long song: 3 minutes (180s)
        assertFalse(shouldCountPlay(180_000L, 29_000L))
        assertTrue(shouldCountPlay(180_000L, 30_000L))
        assertTrue(shouldCountPlay(180_000L, 120_000L))

        // Short song: 20 seconds (20s) -> threshold is 10s
        assertFalse(shouldCountPlay(20_000L, 9_000L))
        assertTrue(shouldCountPlay(20_000L, 10_000L))
        assertTrue(shouldCountPlay(20_000L, 20_000L))
    }

    @Test
    fun `playlist model properties and immutability test`() {
        val song1 = Song(id = "1", title = "Track 1", durationMs = 60000L)
        val song2 = Song(id = "2", title = "Track 2", durationMs = 120000L)

        val playlist = Playlist(
            id = 1L,
            name = "Night Drives",
            songs = listOf(song1, song2)
        )

        assertEquals("Night Drives", playlist.name)
        assertEquals(2, playlist.songCount)
        assertEquals(180000L, playlist.totalDurationMs)
        assertEquals("03:00", playlist.formattedTotalDuration)
    }

    @Test
    fun `song sort options test`() {
        val s1 = Song(id = "1", title = "Bravo", artist = "Charlie", dateAdded = 100L)
        val s2 = Song(id = "2", title = "Alpha", artist = "Delta", dateAdded = 300L)
        val s3 = Song(id = "3", title = "Charlie", artist = "Alpha", dateAdded = 200L)

        val list = listOf(s1, s2, s3)

        val sortedByTitle = list.sortedWith(SortOption.TITLE.comparator)
        assertEquals("Alpha", sortedByTitle[0].title)
        assertEquals("Bravo", sortedByTitle[1].title)
        assertEquals("Charlie", sortedByTitle[2].title)

        val sortedByArtist = list.sortedWith(SortOption.ARTIST.comparator)
        assertEquals("Alpha", sortedByArtist[0].artist)
        assertEquals("Charlie", sortedByArtist[1].artist)
        assertEquals("Delta", sortedByArtist[2].artist)

        val sortedByDate = list.sortedWith(SortOption.DATE_ADDED.comparator)
        assertEquals(300L, sortedByDate[0].dateAdded)
        assertEquals(200L, sortedByDate[1].dateAdded)
        assertEquals(100L, sortedByDate[2].dateAdded)
    }
}
