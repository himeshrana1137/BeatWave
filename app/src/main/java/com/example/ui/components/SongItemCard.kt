package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.theme.AccentPink
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryAccent
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SongItemCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onSongInfo: ((Song) -> Unit)? = null,
    onShareSong: ((Song) -> Unit)? = null,
    onCutRingtone: ((Song) -> Unit)? = null,
    showQualityBadges: Boolean = true,
    showDragHandle: Boolean = false,
    isCustomOrder: Boolean = true,
    dragHandleModifier: Modifier = Modifier,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 68.dp.toPx() }
    val maxSwipePx = with(density) { 130.dp.toPx() }

    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = if (reduceAnimations) tween(0) else tween(140),
        label = "swipe_offset"
    )

    val containerColor = if (isCurrent) SurfaceHighlightDark else SurfaceDark.copy(alpha = 0.6f)
    val borderColor = if (isCurrent) PrimaryViolet.copy(alpha = 0.7f) else CardBorder.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .testTag("song_item_${song.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Swipe Background Action Indicators
        if (animatedOffsetX > 8f && onPlayNext != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(SecondaryCyan.copy(alpha = 0.25f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = "Play Next",
                        tint = SecondaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Play Next",
                        color = SecondaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (animatedOffsetX < -8f && onAddToQueue != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(PrimaryViolet.copy(alpha = 0.28f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Add to Queue",
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Add to Queue",
                        tint = PrimaryViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Foreground Card Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedOffsetX }
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .pointerInput(song.id, onPlayNext, onAddToQueue) {
                    if (onPlayNext != null || onAddToQueue != null) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffsetX > swipeThresholdPx && onPlayNext != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPlayNext.invoke(song)
                                } else if (dragOffsetX < -swipeThresholdPx && onAddToQueue != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAddToQueue.invoke(song)
                                }
                                dragOffsetX = 0f
                            },
                            onDragCancel = {
                                dragOffsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-maxSwipePx, maxSwipePx)
                            }
                        )
                    }
                }
                .clickable { onSongClick(song) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with playing indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                AlbumArtThumbnail(
                    song = song,
                    size = 50.dp,
                    shapeRadius = 10.dp
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    ) {
                        MiniEqualizerBars(
                            isPlaying = isPlaying,
                            color = PrimaryViolet,
                            reduceAnimations = reduceAnimations
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.displayTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            color = if (isCurrent) PrimaryViolet else TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isCurrent) {
                        MiniEqualizerBars(
                            isPlaying = isPlaying,
                            color = PrimaryViolet,
                            reduceAnimations = reduceAnimations
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showQualityBadges) {
                        val quality = remember(song.id) {
                            com.example.util.MediaQualityManager.getAudioQualitySync(song)
                        }
                        AudioQualityBadge(quality = quality, compact = true)
                    }
                    Text(
                        text = song.displayArtist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    Text(
                        text = song.formattedDuration,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            }

        // Favorite Button
        IconButton(
            onClick = { onToggleFavorite(song) },
            modifier = Modifier
                .testTag("favorite_button_${song.id}")
                .minimumInteractiveComponentSize()
                .size(40.dp)
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (song.isFavorite) AccentPink else TextMuted
            )
        }

        // More Menu
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .testTag("song_menu_${song.id}")
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More song options",
                    tint = TextMuted
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(SurfaceHighlightDark)
            ) {
                onPlayNext?.let { playNextAction ->
                    DropdownMenuItem(
                        text = { Text("Play Next", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = PrimaryViolet
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            playNextAction(song)
                        }
                    )
                }

                onAddToQueue?.let { addToQueueAction ->
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = SecondaryAccent
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            addToQueueAction(song)
                        }
                    )
                }

                onMoveToTop?.let { moveToTopAction ->
                    DropdownMenuItem(
                        text = { Text("Move to Top", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VerticalAlignTop,
                                contentDescription = null,
                                tint = SecondaryCyan
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            moveToTopAction()
                        }
                    )
                }

                onMoveUp?.let { moveUpAction ->
                    DropdownMenuItem(
                        text = { Text("Move Up", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = SecondaryCyan
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            moveUpAction()
                        }
                    )
                }

                onMoveDown?.let { moveDownAction ->
                    DropdownMenuItem(
                        text = { Text("Move Down", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = SecondaryCyan
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            moveDownAction()
                        }
                    )
                }

                onMoveToBottom?.let { moveToBottomAction ->
                    DropdownMenuItem(
                        text = { Text("Move to Bottom", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VerticalAlignBottom,
                                contentDescription = null,
                                tint = SecondaryCyan
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            moveToBottomAction()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = TextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = PrimaryViolet
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylist(song)
                    }
                )
                onSongInfo?.let { onInfo ->
                    DropdownMenuItem(
                        text = { Text("Song Info", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SecondaryAccent
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onInfo(song)
                        }
                    )
                }
                onShareSong?.let { onShare ->
                    DropdownMenuItem(
                        text = { Text("Share Track", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = PrimaryViolet
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShare(song)
                        }
                    )
                }
                onCutRingtone?.let { onCut ->
                    DropdownMenuItem(
                        text = { Text("Cut Ringtone", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = SecondaryAccent
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onCut(song)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete Track", color = Color(0xFFEF4444)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDeleteSong(song)
                    }
                )
            }
        }

        // Reorder Drag Handle (≡)
        if (showDragHandle) {
            Box(
                modifier = dragHandleModifier
                    .testTag("drag_handle_${song.id}")
                    .size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reorder song (Long-press & drag)",
                    tint = if (isCustomOrder) TextSecondary else TextMuted.copy(alpha = 0.35f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
}
