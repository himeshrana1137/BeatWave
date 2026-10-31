package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Achievement
import com.example.model.ListeningStats
import java.util.Calendar

class AchievementManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("beatwave_achievements", Context.MODE_PRIVATE)

    companion object {
        const val BADGE_FIRST_BEAT = "first_beat"
        const val BADGE_CENTURION = "centurion"
        const val BADGE_NIGHT_OWL = "night_owl"
        const val BADGE_AUDIOPHILE = "audiophile"
        const val BADGE_DIRECTOR = "director"
        const val BADGE_VAULT = "vault_keeper"
        const val BADGE_LYRICIST = "lyricist"
        const val BADGE_SPEEDSTER = "speedster"
        const val BADGE_STREAMER = "streamer"
    }

    fun getAchievements(stats: ListeningStats): List<Achievement> {
        val plays = stats.totalTracksPlayed

        // First Beat
        val firstBeatUnlocked = plays >= 1 || prefs.getBoolean("badge_$BADGE_FIRST_BEAT", false)
        if (firstBeatUnlocked && !prefs.getBoolean("badge_$BADGE_FIRST_BEAT", false)) {
            unlock(BADGE_FIRST_BEAT)
        }

        // Centurion (100 plays)
        val centurionUnlocked = plays >= 100 || prefs.getBoolean("badge_$BADGE_CENTURION", false)
        if (centurionUnlocked && !prefs.getBoolean("badge_$BADGE_CENTURION", false)) {
            unlock(BADGE_CENTURION)
        }

        return listOf(
            Achievement(
                id = BADGE_FIRST_BEAT,
                title = "First Beat",
                description = "Play your very first track on BeatWave",
                iconEmoji = "🎵",
                isUnlocked = firstBeatUnlocked,
                progress = plays.coerceAtMost(1),
                maxProgress = 1,
                unlockedDate = prefs.getLong("date_$BADGE_FIRST_BEAT", 0L).takeIf { it > 0 }
            ),
            Achievement(
                id = BADGE_CENTURION,
                title = "Centurion",
                description = "Play 100 songs across your library",
                iconEmoji = "💯",
                isUnlocked = centurionUnlocked,
                progress = plays.coerceAtMost(100),
                maxProgress = 100,
                unlockedDate = prefs.getLong("date_$BADGE_CENTURION", 0L).takeIf { it > 0 }
            ),
            Achievement(
                id = BADGE_NIGHT_OWL,
                title = "Night Owl",
                description = "Listen to music late at night (midnight to 5 AM)",
                iconEmoji = "🦉",
                isUnlocked = prefs.getBoolean("badge_$BADGE_NIGHT_OWL", false),
                progress = if (prefs.getBoolean("badge_$BADGE_NIGHT_OWL", false)) 1 else 0,
                maxProgress = 1,
                unlockedDate = prefs.getLong("date_$BADGE_NIGHT_OWL", 0L).takeIf { it > 0 }
            ),
            Achievement(
                id = BADGE_AUDIOPHILE,
                title = "Audiophile",
                description = "Adjust the Equalizer, Bass Boost or 3D Virtualizer",
                iconEmoji = "🎛️",
                isUnlocked = prefs.getBoolean("badge_$BADGE_AUDIOPHILE", false),
                progress = if (prefs.getBoolean("badge_$BADGE_AUDIOPHILE", false)) 1 else 0,
                maxProgress = 1,
                unlockedDate = prefs.getLong("date_$BADGE_AUDIOPHILE", 0L).takeIf { it > 0 }
            ),
            Achievement(
                id = BADGE_DIRECTOR,
                title = "Video Explorer",
                description = "Watch a video clip or full movie in BeatWave",
                iconEmoji = "🎬",
                isUnlocked = prefs.getBoolean("badge_$BADGE_DIRECTOR", false),
                progress = if (prefs.getBoolean("badge_$BADGE_DIRECTOR", false)) 1 else 0,
                maxProgress = 1,
                unlockedDate = prefs.getLong("date_$BADGE_DIRECTOR", 0L).takeIf { it > 0 }
            ),
            Achievement(
                id = BADGE_VAULT,
                title = "Vault Guardian",
                description = "Open and secure private media in the Vault",
                iconEmoji = "🔒",
                isUnlocked = prefs.getBoolean("badge_$BADGE_VAULT", false),
                progress = if (prefs.getBoolean("badge_$BADGE_VAULT", false)) 1 else 0,
                maxProgress = 1,
                unlockedDate = prefs.getLong("date_$BADGE_VAULT", 0L).takeIf { it > 0 }
            ),
            Achievement(
                id = BADGE_LYRICIST,
                title = "Karaoke Pro",
                description = "Sing along with synced lyrics",
                iconEmoji = "🎤",
                isUnlocked = prefs.getBoolean("badge_$BADGE_LYRICIST", false),
                progress = if (prefs.getBoolean("badge_$BADGE_LYRICIST", false)) 1 else 0,
                maxProgress = 1,
                unlockedDate = prefs.getLong("date_$BADGE_LYRICIST", 0L).takeIf { it > 0 }
            ),
            Achievement(
                id = BADGE_SPEEDSTER,
                title = "Tempo Master",
                description = "Control audio speed or pitch",
                iconEmoji = "⚡",
                isUnlocked = prefs.getBoolean("badge_$BADGE_SPEEDSTER", false),
                progress = if (prefs.getBoolean("badge_$BADGE_SPEEDSTER", false)) 1 else 0,
                maxProgress = 1,
                unlockedDate = prefs.getLong("date_$BADGE_SPEEDSTER", 0L).takeIf { it > 0 }
            ),
            Achievement(
                id = BADGE_STREAMER,
                title = "Stream Explorer",
                description = "Paste and play an online network video stream",
                iconEmoji = "🌐",
                isUnlocked = prefs.getBoolean("badge_$BADGE_STREAMER", false),
                progress = if (prefs.getBoolean("badge_$BADGE_STREAMER", false)) 1 else 0,
                maxProgress = 1,
                unlockedDate = prefs.getLong("date_$BADGE_STREAMER", 0L).takeIf { it > 0 }
            )
        )
    }

    fun checkNightOwl() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 0..4) {
            unlock(BADGE_NIGHT_OWL)
        }
    }

    fun unlock(badgeId: String) {
        if (!prefs.getBoolean("badge_$badgeId", false)) {
            prefs.edit()
                .putBoolean("badge_$badgeId", true)
                .putLong("date_$badgeId", System.currentTimeMillis())
                .apply()
        }
    }
}
