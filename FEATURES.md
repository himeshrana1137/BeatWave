# BeatWave — Comprehensive Feature Inventory & Status Report

This document is an exhaustive, code-verified inventory of every feature in the **BeatWave** Android codebase (Kotlin + Jetpack Compose). Every item is linked to the primary source file implementing it, with concrete implementation details and status tags:
* ✅ **Done**: Fully implemented and functional in code.
* 🟡 **Partial / Stubbed**: Implemented in part (e.g. UI exists without underlying logic, or backend exists without complete UI wiring).
* ❌ **Missing**: Not implemented in code.

---

## 1. Library and Scanning

* ✅ **MediaStore Audio Scanner**: Automatically queries device storage via `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` extracting track titles, artists, albums, duration, date added, file paths, and folder names. — `app/src/main/java/com/example/data/MusicRepository.kt` (`scanMediaStoreAudio`)
* ✅ **Automatic Background Rescan (ContentObserver)**: Registers a `ContentObserver` on `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` to silently rescan media whenever device storage changes when auto-scan is enabled. — `app/src/main/java/com/example/ui/MusicViewModel.kt` (`registerMediaStoreObserver`)
* ✅ **Short Audio Filter**: Filters out notification chimes, voice notes, and short sound clips based on a configurable duration threshold. — `app/src/main/java/com/example/data/MusicRepository.kt` (`loadSongs`)
* ✅ **Configurable Duration Threshold**: Adjustable slider (15 to 120 seconds, default 60s) controlling minimum audio duration allowed in the library. — `app/src/main/java/com/example/ui/components/SettingsScreen.kt` & `MusicRepository.kt`
* ✅ **Folder Blacklist Manager**: Excludes user-selected folders (e.g., WhatsApp Audio, Podcasts, Recordings) from appearing anywhere in the audio library. — `app/src/main/java/com/example/ui/components/FolderBlacklistDialog.kt` & `MusicRepository.kt`
* ✅ **Duplicate Song Finder**: Identifies duplicate tracks across the library by normalized title and artist comparison, sorting duplicates by duration and providing one-tap deletion. — `app/src/main/java/com/example/ui/components/DuplicateFinderDialog.kt` & `MusicRepository.kt` (`findDuplicateSongs`)
* ✅ **Folders View**: Groups audio library by file directories, showing folder name, track count, folder path, and navigation to folder songs. — `app/src/main/java/com/example/ui/components/LibraryScreen.kt`
* ✅ **Albums 2-Column Grid**: Groups songs by album and displays them in a 2-column square card grid with album art, artist, and track count. — `app/src/main/java/com/example/ui/components/LibraryScreen.kt` & `MediaGroupViews.kt`
* ✅ **Artists View**: Groups songs by artist, displaying artist card with total track count and navigation to all tracks by that artist. — `app/src/main/java/com/example/ui/components/LibraryScreen.kt` & `MediaGroupViews.kt`
* ✅ **Audio Quality Tier Filter**: Filter chip row allowing users to filter songs by quality tier (All, Lossless Hi-Res FLAC/WAV, High Quality 320kbps MP3, Standard, Low). — `app/src/main/java/com/example/ui/components/LibraryScreen.kt` & `util/MediaQualityManager.kt`
* ✅ **Synthesized Demo Audio Generator**: Automatically creates 4 offline WAV synth tracks on first launch if the device has no music files so the app is immediately testable. — `app/src/main/java/com/example/data/MusicRepository.kt` (`generateDemoTracksIfEmpty`)
* 🟡 **Genre & Year Smart Library Tabs**: `GenreItem`, `YearItem`, and `NavigationTab.GENRES` & `NavigationTab.YEARS` exist in data models, but filter chips, tab views, and category grouping screens are not yet wired into the Library UI. — `app/src/main/java/com/example/model/Song.kt` & `ui/components/LibraryScreen.kt`
* 🟡 **"Never Played" & "Recently Added" Library Tabs**: Automatic playlist concepts exist in Home and ViewModel (`recentlyAdded`, `recentlyPlayed`), but dedicated standalone Library filter tabs for "Never played", "Recently added", and "Most played" are missing from `LibraryScreen.kt`. — `app/src/main/java/com/example/ui/components/LibraryScreen.kt`

---

## 2. Audio Player

* ✅ **Playback Controls**: Play, pause, skip next, skip previous, seek slider, and toggle play/pause via MediaPlayer engine. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt`
* ✅ **Dynamic Queue Management**: Active playback queue supporting `playNext(song)`, `addToQueue(song)`, `moveQueueItem(from, to)`, and `updateQueueOrder(newOrder)`. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt`
* ✅ **Queue Bottom Sheet in Player**: Bottom sheet in full-screen player showing up-next songs, current song indicator, and live reordering. — `app/src/main/java/com/example/ui/components/FullScreenPlayer.kt`
* ✅ **Shuffle Playback**: True random queue shuffle preserving the current song at index 0 and restoring the exact original order when turned off. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt` (`toggleShuffle`, `rebuildActiveQueue`)
* ✅ **Repeat Modes**: 3-state cycle: Repeat All (`RepeatMode.ALL`), Repeat One Track (`RepeatMode.ONE`), and Repeat Off (`RepeatMode.OFF`). — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt` (`cycleRepeatMode`)
* ✅ **A-B Loop Repeat**: Precision Point A and Point B looping with collapsible controls in the full-screen player; loops automatically during the position ticker. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt` (`setPointA`, `setPointB`, `clearABRepeat`) & `FullScreenPlayer.kt`
* ✅ **Variable Playback Speed & Pitch**: Real-time adjustment from 0.5x to 2.0x speed and variable audio pitch using Android `PlaybackParams`. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt` & `FullScreenPlayer.kt`
* ✅ **Hardware 5-Band Equalizer**: Connects to Android `Equalizer` audio effect session with 5 millibel bands (-15dB to +15dB). — `app/src/main/java/com/example/playback/AudioEqualizerManager.kt` & `ui/components/EqualizerScreen.kt`
* ✅ **Equalizer Presets**: Built-in sound profiles: Flat, Bass Boost, Electronic, Rock, Pop, Vocal, and Custom. — `app/src/main/java/com/example/playback/AudioEqualizerManager.kt` (`selectPreset`)
* ✅ **Bass Boost FX**: Hardware bass boost strength slider from 0 to 1000 millibels via Android `BassBoost`. — `app/src/main/java/com/example/playback/AudioEqualizerManager.kt`
* ✅ **Virtualizer (3D Surround FX)**: Spatial 3D audio effect slider from 0 to 1000 via Android `Virtualizer`. — `app/src/main/java/com/example/playback/AudioEqualizerManager.kt`
* ✅ **Volume Normalization**: Uniform gain boost (+8dB leveling) across varying audio tracks using Android `LoudnessEnhancer`. — `app/src/main/java/com/example/playback/AudioEqualizerManager.kt`
* ✅ **Fade In / Out**: Smooth 10-step logarithmic volume ramp on play, pause, and track skip to prevent sudden audio pops. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt` (`play`, `pause`)
* ✅ **Sleep Timer**: Countdown timer (15m, 30m, 45m, 60m, custom) that pauses playback and frees audio focus on completion with live seconds countdown. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt` (`setSleepTimer`, `cancelSleepTimer`) & `FullScreenPlayer.kt`
* ✅ **Synchronized .LRC Lyrics**: Line-by-line synchronized lyrics auto-scrolling with current position, clickable lyrics lines to seek, and manual `.lrc` file import. — `app/src/main/java/com/example/playback/LrcParser.kt` & `ui/components/FullScreenPlayer.kt`
* ✅ **Online Lyrics Auto-Fetch (LRCLIB)**: Automatically queries the LRCLIB REST API by track title, artist, and duration; caches fetched `.lrc` lyrics in app storage. — `app/src/main/java/com/example/playback/LyricsRepository.kt`
* ✅ **Album Art Loader**: Multi-tier artwork extraction (MediaStore content URI, embedded ID3 APIC frames, and local disk cache) with sub-sampling to stay 100% crash-free on 32-bit low-RAM devices. — `app/src/main/java/com/example/util/AlbumArtLoader.kt`
* ✅ **Online Cover Art Fetcher**: Background cover art query from iTunes/Deezer APIs with local caching. — `app/src/main/java/com/example/util/AlbumArtLoader.kt` (`fetchOnlineCoverArt`)
* ❌ **Gapless Playback**: `gaplessPlaybackEnabled` setting exists in `AppSettings.kt`, but `MediaPlayer.setNextMediaPlayer` pre-buffering pipeline is not yet implemented in `MusicPlaybackManager.kt`. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt`
* ❌ **Lyrics Offset Adjustment**: No "+0.5s" / "-0.5s" sync shift buttons or per-song offset persistence in the lyrics viewer. — `app/src/main/java/com/example/ui/components/FullScreenPlayer.kt`

---

## 3. Video Player

* ✅ **MP4 / WebM / MKV Video Engine**: Full-screen video player built on `SurfaceView` and `MediaPlayer` with audio focus and orientation management. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Touch Gesture Controls**: Vertical drag on left screen for brightness, vertical drag on right screen for system media volume, and double-tap on sides for ±10s seek. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Variable Video Playback Speed**: 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, and 2.0x video playback speeds with audio pitch compensation. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Video Resume Position**: Automatically remembers the exact millisecond playback position per video and prompts one-tap resume. — `app/src/main/java/com/example/video/VideoPlayerView.kt` & `data/MusicRepository.kt` (`saveVideoProgress`)
* ✅ **Multiple Audio Track Selection**: Detects all embedded audio tracks in multi-language videos and allows user switching via menu. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Subtitle Support (.SRT / .VTT)**: Loads subtitles via system document picker, auto-detects sibling `.srt` files matching the video name, and supports custom delay (±0.5s), text size, text color, and background styling. — `app/src/main/java/com/example/video/VideoPlayerView.kt` (`SubtitleParser`)
* ✅ **Picture-in-Picture (PiP)**: Dedicated PiP button and automatic PiP entry when pressing the Home button (`onUserLeaveHint`), with remote play/pause action and controls hiding in PiP. — `app/src/main/java/com/example/MainActivity.kt` & `video/VideoPlayerView.kt`
* ✅ **Touch Screen Lock**: Lock button disables all gestures and touch controls to prevent accidental touches; long-press on floating lock icon with haptic feedback unlocks. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Auto-Rotate & Manual Rotation**: Automatically rotates to landscape for wide aspect-ratio videos if device auto-rotate is enabled; includes manual rotation toggle. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Aspect Ratio Modes**: Fit (letterbox), Fill (crop), Zoom (135%), and Original (pixel-true) with per-video preference persistence. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Audio-Only Background Video Mode**: Dimmed screen mode allowing video sound to continue playing in background or with screen turned off. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Video Technical Info & Codec Validation**: Checks codec decoder support using `MediaExtractor` / `MediaCodecList`; displays bitrate, resolution, container format, and frame rate. — `app/src/main/java/com/example/video/VideoPlayerView.kt` & `util/MediaQualityManager.kt`
* ✅ **Video Screenshot Capture**: Captures the currently displayed video frame via `MediaMetadataRetriever` and saves it to device storage. — `app/src/main/java/com/example/video/VideoPlayerView.kt`
* ✅ **Network Video / Audio Streaming**: Stream online video or audio URLs (HTTP, HTTPS, HLS) directly in the video player via dialog. — `app/src/main/java/com/example/ui/components/StreamUrlDialog.kt`
* ✅ **Synthesized Demo Video Generator**: Generates an offline H.264/AAC MP4 video with synced `.srt` subtitles using `MediaCodec` and `MediaMuxer` if no videos exist on the device. — `app/src/main/java/com/example/data/MusicRepository.kt` (`encodeDemoMp4`)

---

## 4. Playlists, Ordering, and Organization

* ✅ **Custom Playlist Creation**: Create, rename, and delete playlists with custom names and descriptions. — `app/src/main/java/com/example/ui/components/PlaylistDialogs.kt` & `data/MusicRepository.kt`
* ✅ **Custom Song Ordering (Drag-and-Drop)**: Long-press and drag on drag handle (≡) to reorder songs with haptic feedback and smooth placement animation. — `app/src/main/java/com/example/ui/components/ReorderableSongList.kt`
* ✅ **Menu-Based Song Reordering**: "Move up", "Move down", "Move to top", and "Move to bottom" in each song's 3-dot dropdown menu. — `app/src/main/java/com/example/ui/components/SongItemCard.kt`
* ✅ **Persistent Custom Order Storage**: Custom order is saved permanently per playlist ID and per folder path; new songs go to the end and deleted songs are purged. — `app/src/main/java/com/example/data/MusicRepository.kt` (`saveCustomOrder`, `resolveContainerSongs`)
* ✅ **Container Sort Menu**: Sort by Custom order, Title (A-Z), Artist (A-Z), Date added (newest first), or Duration; dragging is enabled exclusively in Custom order. — `app/src/main/java/com/example/data/MusicRepository.kt` & `ui/components/LibraryScreen.kt`
* ✅ **Reset Order Option**: "Reset order" button restores playlist to default order. — `app/src/main/java/com/example/data/MusicRepository.kt` (`clearCustomOrder`)
* ✅ **Smart Mood Playlists**: Automatic dynamic playlists based on song mood tags and metadata: Chill Vibes 🍃, Workout Energy ⚡, Party Hits 🎉, and Focus Flow 🧠. — `app/src/main/java/com/example/data/MusicRepository.kt` (`loadPlaylists`)
* ✅ **Song Mood Tagger**: Dialog to assign mood tags (Chill, Workout, Party, Focus, Acoustic, Road Trip, Romantic) to any song. — `app/src/main/java/com/example/ui/components/MoodTagDialog.kt`
* ✅ **Favorites Collection**: Quick heart toggle to favorite/unfavorite songs, with dedicated Favorites view. — `app/src/main/java/com/example/ui/components/LibraryScreen.kt` & `data/MusicRepository.kt`
* ✅ **Swipe Actions on Song Rows**: Swipe right on any song row to "Play Next"; swipe left to "Add to Queue". — `app/src/main/java/com/example/ui/components/SongItemCard.kt`

---

## 5. Background and System Integration

* ✅ **Foreground Media Service**: Runs as an Android `FOREGROUND_SERVICE_MEDIA_PLAYBACK` service to ensure uninterrupted playback with screen off. — `app/src/main/java/com/example/playback/MusicService.kt`
* ✅ **Rich Media Notification**: Notification showing album art thumbnail, title, artist, album, Previous, Play/Pause, and Next buttons. — `app/src/main/java/com/example/playback/MusicService.kt`
* ✅ **MediaSession & Lock Screen Integration**: System `MediaSession` with metadata (title, artist, album, full art bitmap) and `PlaybackState` with `SystemClock.elapsedRealtime()` for lock-screen scrub bar. — `app/src/main/java/com/example/playback/MusicService.kt`
* ✅ **Audio Becoming Noisy Handler**: BroadcastReceiver listening for `ACTION_AUDIO_BECOMING_NOISY` that immediately pauses playback when headphones are disconnected. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt`
* ✅ **Audio Focus Management**: Requests `AUDIOFOCUS_GAIN` on `AudioManager`; automatically ducks or pauses on transient loss (calls/navigation) and auto-resumes when focus returns. — `app/src/main/java/com/example/playback/MusicPlaybackManager.kt`
* ✅ **Headset Double-Press Controls**: Intercepts `KEYCODE_HEADSETHOOK` and media buttons; double-press skips to next track, single-press toggles play/pause. — `app/src/main/java/com/example/playback/MusicService.kt`
* ✅ **Shake to Skip**: Battery-optimized accelerometer listener that skips to the next track when a firm phone shake is detected during active playback. — `app/src/main/java/com/example/playback/ShakeDetector.kt`
* 🟡 **Home Screen App Widget**: 4x1 AppWidget (`BeatWaveWidgetProvider`) with album art, title, artist, prev, play/pause, and next. However, the requested independent 2x1 small widget and 4x2 large widget with progress bar are not yet split into separate providers. — `app/src/main/java/com/example/widget/BeatWaveWidgetProvider.kt` & `res/layout/widget_beatwave.xml`

---

## 6. Personalization and UI

* ✅ **Dark / OLED Black Theme**: Restrained premium visual palette (`#0B0B10` background, `#14141B` surface, `#1C1C26` elevated surface, `#8B5CF6` violet accent). — `app/src/main/java/com/example/ui/theme/Theme.kt`
* ✅ **Theme Selector**: In-settings toggle between Dark (cool graphite) and OLED Pure Black (`#000000`). — `app/src/main/java/com/example/ui/components/SettingsScreen.kt` & `theme/Theme.kt`
* ✅ **Reduce Animations Switch**: Setting to disable heavy layout transitions and spring physics for maximum battery savings and low-end phone performance. — `app/src/main/java/com/example/ui/components/SettingsScreen.kt`
* ✅ **Audio Quality Badges**: Visual pill indicators (FLAC, 320k, 256k, 128k, HD Video) displayed on track and album cards with an on/off setting toggle. — `app/src/main/java/com/example/ui/components/QualityBadges.kt` & `util/MediaQualityManager.kt`
* ✅ **Clean Display Titles**: Cleans messy download tags (`(Lyrics)`, `(8D AUDIO)`, `[HD]`, `(Official Video)`) and replaces underscore dividers for display without touching audio files. — `app/src/main/java/com/example/model/Song.kt` (`displayTitle`, `displayArtist`)
* ✅ **Currently Playing Highlight**: Purple title and animated 3-bar equalizer indicator on currently playing tracks across every list. — `app/src/main/java/com/example/ui/components/SongItemCard.kt`
* ✅ **Bottom Navigation Tabs**: Home, Search, Library, and Videos tabs. — `app/src/main/java/com/example/ui/MusicPlayerScreen.kt`
* ✅ **Search Screen Recent Searches & Recently Played**: Search screen displaying recent search tags (with clear all) and a horizontal "Recently played" row. — `app/src/main/java/com/example/ui/components/SearchScreen.kt`
* ❌ **In-App Language Switcher**: Not implemented; all user interface strings are currently English only.

---

## 7. Stats and Fun

* ✅ **BeatWave Wrapped**: Spotify-Wrapped-style summary dialog with top songs, top artist, total minutes listened, and most active listening times. — `app/src/main/java/com/example/ui/components/BeatWaveWrappedDialog.kt`
* ✅ **Achievements & Badges System**: 8 unlockable milestones (First Beat, Night Owl, Weekend Warrior, Audiophile, Playlist Curator, Centurion, Marathoner, Variety Master). — `app/src/main/java/com/example/util/AchievementManager.kt` & `ui/components/AchievementsDialog.kt`
* ✅ **Shareable Now Playing Card Generator**: Renders a styled Instagram-ready "Now Playing" story bitmap with album art, title, artist, and waveform, shareable via system share sheet. — `app/src/main/java/com/example/util/NowPlayingCardGenerator.kt`
* ✅ **Listening Statistics Overview**: Displays total tracks played, total listening hours, top song, top artist, total songs, and playlists in Settings. — `app/src/main/java/com/example/data/MusicRepository.kt` (`getListeningStats`) & `ui/components/SettingsScreen.kt`

---

## 8. Data and Safety

* ✅ **JSON Backup (Storage Access Framework)**: Exports all playlists, custom song ordering, favorites, listening stats, achievements, mood tags, settings, and video resume positions into a single versioned JSON file with zero permissions. — `app/src/main/java/com/example/data/MusicRepository.kt` (`exportBackupJson`) & `ui/components/SettingsScreen.kt`
* ✅ **JSON Restore with Fuzzy Matching**: Restores backup data by matching tracks by file path first, then by title and duration (within ±3s); prompts confirmation before overwriting and displays a summary of matched/unmatched tracks. — `app/src/main/java/com/example/data/MusicRepository.kt` (`importBackupJson`) & `ui/components/SettingsScreen.kt`
* ✅ **Resume Last Played State**: Saves active queue, current song, playback position, shuffle/repeat mode, speed, and pitch on song change, pause, or background; restores state paused with mini player on launch. — `app/src/main/java/com/example/playback/PlaybackPersistenceManager.kt` & `MusicPlaybackManager.kt` (`restoreSavedPlaybackState`)
* ✅ **Crash Reporting System**: Uncaught exception handler records stack trace, timestamp, app version, Android version, and device model to local storage (retains last 10 reports); includes copy, share, and delete actions. — `app/src/main/java/com/example/util/CrashReportManager.kt` & `ui/components/SettingsScreen.kt`
* ✅ **Hidden Video Vault**: Private encrypted directory for hidden videos protected by 4-digit PIN code and device biometric authentication. — `app/src/main/java/com/example/util/VaultManager.kt` & `ui/components/VaultDialog.kt`

---

## 9. Tools and Utilities

* ✅ **Audio Ringtone Cutter**: Interactive trim dialog with start/end time sliders, live preview playback, and saving as custom ringtone or notification sound. — `app/src/main/java/com/example/ui/components/RingtoneCutterDialog.kt` & `util/RingtoneCutter.kt`
* ✅ **Detailed Song Technical Info Dialog**: Detailed audio metadata modal showing exact bitrate (kbps), sample rate (kHz), codec/format (FLAC, MP3, AAC, WAV), file size (MB), full file path, and date added. — `app/src/main/java/com/example/ui/components/SongInfoDialog.kt` & `data/MusicRepository.kt` (`getSongDetailedInfo`)
* ✅ **File Sharing via FileProvider**: Shares audio and video files securely using Android `FileProvider` without exposing raw file paths. — `app/src/main/java/com/example/util/MediaShareHelper.kt` & `AndroidManifest.xml`
* ✅ **Track Deletion**: Deletes songs from app database and playlists with confirmation dialog. — `app/src/main/java/com/example/ui/components/LibraryScreen.kt` & `ui/components/SongItemCard.kt`
* 🟡 **Tag Editor**: `TagEditorDialog.kt` exists with title, artist, album, genre, year, cover art gallery picker, and "write to file" toggle, but is not yet wired to song 3-dot dropdown menus or repository update methods. — `app/src/main/java/com/example/ui/components/TagEditorDialog.kt`

---

## Settings Switches

The following table lists every setting option currently implemented in the **Settings** screen (`SettingsScreen.kt`) and stored in `AppSettings`:

| Setting Title | Category | Description | Default Value | SharedPreferences Key |
|---|---|---|---|---|
| **Auto-Pause on Calls & Unplug** | Audio Playback | Pauses playback on incoming calls or headphone disconnect; resumes when call ends | `true` | `key_auto_pause_on_call` |
| **Headset Double-Press Controls** | Audio Playback | Double click headset hook / media button to skip track; single click to play/pause | `true` | `key_headset_double_press` |
| **Shake to Skip** | Audio Playback | Shake phone firmly during playback to skip to next track | `false` | `key_shake_to_skip` |
| **Fade In / Out on Play & Pause** | Audio Playback | Smooth 10-step volume ramp when starting, resuming, or pausing tracks | `true` | `key_fade_in_out` |
| **Auto-Fetch Synced Lyrics (LRCLIB)** | Audio Playback | Automatically fetch line-by-line synced lyrics over internet when missing | `true` | `key_auto_lyrics` |
| **Online Album Art Fetcher** | Audio Playback | Download missing cover art from iTunes/Deezer APIs into app storage | `false` | `key_fetch_online_cover` |
| **Audio-Only Background Video** | Video Playback | Keeps video audio playing when app is backgrounded or screen locked | `false` | `key_bg_video_audio` |
| **Filter Short Audio (< 60s)** | Library & Scanning | Hides notification sounds, voice notes, and audio clips shorter than duration threshold | `true` | `key_filter_short_audio` |
| **Minimum Duration Threshold** | Library & Scanning | Slider from 15 to 120 seconds determining short audio filter cutoff | `60` seconds | `key_min_duration_sec` |
| **Auto-Scan Device Media** | Library & Scanning | Automatically detects new or deleted media files in background | `true` | `key_auto_scan` |
| **Show Audio Quality Badges** | Interface & UI | Displays FLAC, 320k, 256k, 128k quality badges on track cards | `true` | `key_show_quality_badges` |
| **Reduce UI Animations** | Interface & UI | Disables heavy layout animations and physics for battery & low-end devices | `false` | `key_reduce_anim` |
| **Theme Mode** | Interface & UI | Select between Dark (cool graphite `#0B0B10`) and OLED Pure Black (`#000000`) | `DARK` | `key_theme_mode` |
| **Gapless Playback** | Audio Playback | Pre-buffers consecutive songs to play without silence between them | `true` (model field) | `gaplessPlaybackEnabled` |

---

## Permissions

| Permission | Android API Level | Used By Feature | Justification / Notes |
|---|---|---|---|
| `android.permission.FOREGROUND_SERVICE` | All APIs | Background Audio Service (`MusicService`) | Required to keep playback running when app is closed |
| `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Android 14+ (API 34+) | Background Audio Service (`MusicService`) | Mandatory foreground service type for media players |
| `android.permission.POST_NOTIFICATIONS` | Android 13+ (API 33+) | Media Notification Controls | Shows media controls notification with art & seek bar |
| `android.permission.WAKE_LOCK` | All APIs | Audio & Video Playback | Prevents CPU sleep while playing audio with screen off |
| `android.permission.READ_MEDIA_AUDIO` | Android 13+ (API 33+) | MediaStore Audio Scanner | Scans device music and audio files |
| `android.permission.READ_MEDIA_VIDEO` | Android 13+ (API 33+) | MediaStore Video Scanner | Scans device video files for video player |
| `android.permission.READ_EXTERNAL_STORAGE` | Up to Android 12L (`maxSdkVersion="32"`) | Audio/Video Scanning on Legacy Android | Scans audio/video files on devices running Android 12 or older |
| `android.permission.VIBRATE` | All APIs | Drag-and-drop & Lock Gestures | Haptic feedback when dragging songs or unlocking video |

---

## Libraries Used

From `app/build.gradle.kts` and `gradle/libs.versions.toml`:

| Library / Artifact | Version | Purpose in BeatWave |
|---|---|---|
| `androidx.compose.bom` | `2024.09.00` | Bill of Materials aligning all Jetpack Compose UI libraries |
| `androidx.compose.material3` | (BOM) | Material Design 3 components (Cards, Sliders, Dropdowns, Sheets, NavigationBar) |
| `androidx.compose.material.icons.extended` | (BOM) | Icons (Queue, Equalizer, Repeat, Shuffle, PictureInPicture, Lock, Video, Lyrics) |
| `androidx.activity.compose` | `1.10.1` | Activity integration, `BackHandler`, system document pickers (`rememberLauncherForActivityResult`) |
| `androidx.lifecycle.runtime.compose` / `viewmodel.compose` | `2.8.7` | MVVM state management, `collectAsStateWithLifecycle` |
| `androidx.core.ktx` | `1.18.0` | Android Kotlin extensions, `FileProvider`, Window insets handling |
| `kotlinx.coroutines.android` / `core` | `1.10.2` | Asynchronous file scanning, audio playback timing, backup I/O |
| `com.squareup.retrofit2` / `converter-moshi` | `2.12.0` | HTTP networking client for LRCLIB synced lyrics API |
| `com.squareup.okhttp3` / `logging-interceptor` | `4.10.0` | Underlying HTTP engine for online lyrics and album art fetching |
| `com.squareup.moshi` / `moshi-kotlin` | `1.15.2` | JSON parsing for LRCLIB lyrics response models |
| `androidx.room.runtime` / `room-ktx` | `2.7.0` | Local database framework (configured in Gradle dependencies) |
| `com.google.firebase.ai` / `appcheck` | `34.17.0` | Firebase AI Studio logic and App Check integration |

---

## Screens and Dialogs List

### Full Screens & Primary Views
1. **Home Screen (`HomeScreen.kt`)**: Top greeting, Continue Watching video shelf, Recently Added shelf, Most Played shelf, Albums horizontal shelf, and overflow action menu.
2. **Search Screen (`SearchScreen.kt`)**: Live search bar, recent search query chips (with clear option), quick categories, and horizontal recently played tracks row.
3. **Library Screen (`LibraryScreen.kt`)**: Header with create playlist (+) button, horizontal filter chips (Playlists, Albums, Artists, Folders, Songs, Favorites), and list/detail views.
4. **Videos Screen (`VideoListView.kt`)**: Video list showing video thumbnails, duration, resolution badges, search, and continue watching items.
5. **Full-Screen Player (`FullScreenPlayer.kt`)**: Collapsible top bar, main artwork / synchronized lyrics toggle with soft fade, title/artist/favorite, 4dp custom seek bar, playback controls, and A-B repeat row.
6. **Video Player (`VideoPlayerView.kt`)**: Fullscreen video playback with gesture volume/brightness HUD, PiP, subtitles, audio track selector, lock button, auto-rotate, and aspect ratio cycling.
7. **Equalizer Screen (`EqualizerScreen.kt`)**: 5-band graphic equalizer sliders, preset selector chips (Flat, Bass Boost, Rock, Pop, etc.), Bass Boost dial, and 3D Virtualizer dial.
8. **Settings Screen (`SettingsScreen.kt`)**: Central settings hub with toggles for audio playback, scanning, UI theme, backup/restore, crash reports, and listening stats.

### Dialogs & Modals
9. **Tag Editor Dialog (`TagEditorDialog.kt`)**: Edit title, artist, album, genre, year, pick cover art from gallery, and toggle file write vs database override.
10. **Song Info Dialog (`SongInfoDialog.kt`)**: Technical audio file metadata dialog (format, bitrate, sample rate, file size, path, date added).
11. **Ringtone Cutter Dialog (`RingtoneCutterDialog.kt`)**: Audio wave trim tool with start/end slider markers, audio preview, and save to ringtones.
12. **Folder Blacklist Dialog (`FolderBlacklistDialog.kt`)**: List of excluded folders with add folder path and remove buttons.
13. **Duplicate Finder Dialog (`DuplicateFinderDialog.kt`)**: Scanned duplicate audio groups with one-tap keep highest quality / delete duplicates.
14. **Create Playlist Dialog (`PlaylistDialogs.kt`)**: Text field modal to name and create new custom playlists.
15. **Add to Playlist Dialog (`PlaylistDialogs.kt`)**: Select an existing playlist to add the chosen song.
16. **Mood Tag Dialog (`MoodTagDialog.kt`)**: Checkbox dialog to tag a song with moods (Chill, Workout, Party, Focus, etc.).
17. **Hidden Vault Dialog (`VaultDialog.kt`)**: 4-digit PIN pad and biometric prompt to view and manage private hidden media.
18. **Network Stream URL Dialog (`StreamUrlDialog.kt`)**: Input dialog to stream an HTTP/HTTPS audio or video URL.
19. **BeatWave Wrapped Dialog (`BeatWaveWrappedDialog.kt`)**: Story-style yearly/monthly listening summary presentation.
20. **Achievements Dialog (`AchievementsDialog.kt`)**: Grid dialog showing unlocked and locked badge trophies.
21. **Crash Reports Dialog (`SettingsScreen.kt`)**: Dialog showing crash history with stack trace viewer, copy, share, and clear buttons.
22. **Restore Confirmation & Summary Dialogs (`SettingsScreen.kt`)**: Overwrite confirmation alert and restore results summary.

---

## Partial, Stubbed or Broken Items

1. **Tag Editor Wiring**:
   * *File*: `app/src/main/java/com/example/ui/components/TagEditorDialog.kt` & `SongItemCard.kt`
   * *Status*: `TagEditorDialog.kt` is implemented with fields for Title, Artist, Album, Genre, Year, Cover Art picker, and "Also write to file" toggle. However, it is not yet wired to the 3-dot dropdown menu in `SongItemCard.kt` or `FullScreenPlayer.kt`, and `MusicRepository.kt` does not yet have an `updateSongTags` override method.
2. **Smart Library Genre & Year Tabs**:
   * *File*: `app/src/main/java/com/example/ui/components/LibraryScreen.kt` (lines 182–220) & `model/Song.kt`
   * *Status*: `NavigationTab.GENRES` and `NavigationTab.YEARS` exist in the enum, and `Song.genre`/`Song.year` exist in the model. However, `LibraryScreen.kt` only displays chips for Playlists, Albums, Artists, Folders, Songs, and Favorites; Genre and Year chips and their category detail screens are missing from the UI.
3. **App Widget Sizes (2x1 and 4x2)**:
   * *File*: `app/src/main/java/com/example/widget/BeatWaveWidgetProvider.kt` & `res/xml/beatwave_widget_info.xml`
   * *Status*: Currently only a single 4x1 layout exists (`widget_beatwave.xml`). Separate `widget_beatwave_small.xml` (2x1: art, play/pause, next) and `widget_beatwave_large.xml` (4x2: art, title, artist, prev/play/next, progress bar) widget providers have not been split into dedicated AppWidget declarations.

---

## Wish List Feature Status (Not Implemented vs Implemented)

Analysis of the 16 features from the user's wish list:

| Feature | Status | Actual Code State |
|---|---|---|
| **Backup and Restore** | ✅ Done | Implemented in `MusicRepository.kt` (`exportBackupJson`, `importBackupJson`) & `SettingsScreen.kt` using SAF JSON picker with confirmation dialog and match summary. |
| **Resume Last Played** | ✅ Done | Implemented in `PlaybackPersistenceManager.kt` & `MusicPlaybackManager.kt` (`restoreSavedPlaybackState`); restores queue, song, position, and mini player on launch. |
| **Picture-in-Picture (PiP)** | ✅ Done | Implemented in `VideoPlayerView.kt` and `MainActivity.kt` (`supportsPictureInPicture="true"`, `onUserLeaveHint`, remote play/pause action). |
| **Subtitles (.SRT / .VTT)** | ✅ Done | Implemented in `VideoPlayerView.kt` (`SubtitleParser`); file picker, auto-detection, custom delay, font size, color, and background styling. |
| **Lock Button** | ✅ Done | Implemented in `VideoPlayerView.kt`; intercepts all touches, floating unlock icon with long-press and haptic feedback. |
| **Auto-Rotate** | ✅ Done | Implemented in `VideoPlayerView.kt`; auto-rotates wide videos to landscape if system rotation lock allows, plus manual rotation toggle. |
| **Aspect Ratio Modes** | ✅ Done | Implemented in `VideoPlayerView.kt`; cycles Fit, Fill, Zoom, and Original with per-video persistence. |
| **Tag Editor** | 🟡 Partial | `TagEditorDialog.kt` UI exists; needs wiring into `SongItemCard.kt` 3-dot menu and repository update methods. |
| **Lyrics Offset** | ❌ Missing | Not implemented; `FullScreenPlayer.kt` lacks ±0.5s shift buttons and per-song offset saving. |
| **Genre / Year Tabs** | 🟡 Partial | Model fields exist in `Song.kt`; missing filter chips and view pages in `LibraryScreen.kt`. |
| **Gapless Playback** | ❌ Missing | Setting field exists in `AppSettings.kt`, but `MediaPlayer.setNextMediaPlayer` pre-buffering logic is not yet implemented in `MusicPlaybackManager.kt`. |
| **Small & Large Widgets** | 🟡 Partial | One medium widget exists; 2x1 small and 4x2 large widgets with progress bars are not separated. |
| **Crash Reports** | ✅ Done | Implemented in `CrashReportManager.kt` and `SettingsScreen.kt`; captures stack trace, device info, and supports copy/share/clear. |
| **Sleep Timer** | ✅ Done | Implemented in `MusicPlaybackManager.kt` (lines 575–599) with live countdown in snapshot and UI in `FullScreenPlayer.kt`. |
| **Language Switcher** | ❌ Missing | No in-app language switching mechanism exists; app is English only. |
| **Equalizer Presets** | ✅ Done | Implemented in `AudioEqualizerManager.kt` (Flat, Bass Boost, Electronic, Rock, Pop, Vocal, Custom) & `EqualizerScreen.kt`. |
