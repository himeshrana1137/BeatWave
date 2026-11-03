package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.Song

/**
 * High-performance, low-RAM-friendly reorderable list for songs in playlists and folders.
 * Features drag handle (≡) on each row, long-press + drag, haptic feedback, and smooth animated placement.
 */
@Composable
fun ReorderableSongList(
    songs: List<Song>,
    isCustomOrder: Boolean,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onSongInfo: ((Song) -> Unit)? = null,
    onShareSong: ((Song) -> Unit)? = null,
    onCutRingtone: ((Song) -> Unit)? = null,
    showQualityBadges: Boolean = true,
    reduceAnimations: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 72.dp.toPx() }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id }
        ) { index, song ->
            val isCurrent = song.id == currentSongId
            val isBeingDragged = draggingIndex == index

            val animatedElevation by animateFloatAsState(
                targetValue = if (isBeingDragged) 12f else 0f,
                animationSpec = tween(150),
                label = "elevation"
            )

            val animatedScale by animateFloatAsState(
                targetValue = if (isBeingDragged) 1.03f else 1.0f,
                animationSpec = tween(150),
                label = "scale"
            )

            val itemModifier = if (isBeingDragged) {
                Modifier
                    .zIndex(10f)
                    .graphicsLayer {
                        translationY = dragOffsetY
                        scaleX = animatedScale
                        scaleY = animatedScale
                        shadowElevation = animatedElevation
                    }
            } else {
                Modifier.zIndex(0f)
            }

            // Drag handle gesture modifier (enabled only when in custom order)
            val dragHandleModifier = if (isCustomOrder) {
                Modifier.pointerInput(song.id, index) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            draggingIndex = index
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                            val activeIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress

                            if (dragOffsetY > itemHeightPx * 0.75f && activeIdx < songs.lastIndex) {
                                onReorder(activeIdx, activeIdx + 1)
                                draggingIndex = activeIdx + 1
                                dragOffsetY -= itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetY < -itemHeightPx * 0.75f && activeIdx > 0) {
                                onReorder(activeIdx, activeIdx - 1)
                                draggingIndex = activeIdx - 1
                                dragOffsetY += itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            draggingIndex = null
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragOffsetY = 0f
                        }
                    )
                }
            } else {
                Modifier
            }

            SongItemCard(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isCurrent && isPlaying,
                onSongClick = { onSongClick(song) },
                onToggleFavorite = { onToggleFavorite(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onDeleteSong = { onDeleteSong(song) },
                onPlayNext = { onPlayNext(song) },
                onAddToQueue = { onAddToQueue(song) },
                onMoveUp = if (index > 0) {
                    { onReorder(index, index - 1) }
                } else null,
                onMoveDown = if (index < songs.lastIndex) {
                    { onReorder(index, index + 1) }
                } else null,
                onMoveToTop = if (index > 0) {
                    { onReorder(index, 0) }
                } else null,
                onMoveToBottom = if (index < songs.lastIndex) {
                    { onReorder(index, songs.lastIndex) }
                } else null,
                showDragHandle = true,
                isCustomOrder = isCustomOrder,
                dragHandleModifier = dragHandleModifier,
                onSongInfo = onSongInfo,
                onShareSong = onShareSong,
                onCutRingtone = onCutRingtone,
                showQualityBadges = showQualityBadges,
                reduceAnimations = reduceAnimations,
                modifier = itemModifier
            )
        }
    }
}
