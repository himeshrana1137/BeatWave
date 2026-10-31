package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationTab
import com.example.model.Song
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredSongs: List<Song>,
    allSongs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onCategoryClick: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("search_screen")
            .fillMaxSize()
    ) {
        // Top Header
        Text(
            text = "Search",
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Search Bar Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .testTag("search_text_field")
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = {
                Text(
                    text = "What do you want to listen to?",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotBlank()) PrimaryViolet else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceVariantDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = Color(0x338B5CF6),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryViolet
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (searchQuery.isBlank()) {
            // Category Browse Cards
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Browse all",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryBrowseCard(
                            title = "Songs",
                            subtitle = "${allSongs.size} tracks",
                            icon = Icons.Default.MusicNote,
                            gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                            onClick = { onCategoryClick(NavigationTab.SONGS) },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryBrowseCard(
                            title = "Albums",
                            subtitle = "Full collections",
                            icon = Icons.Default.Album,
                            gradient = listOf(Color(0xFF06B6D4), Color(0xFF0E7490)),
                            onClick = { onCategoryClick(NavigationTab.ALBUMS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryBrowseCard(
                            title = "Artists",
                            subtitle = "Performers & bands",
                            icon = Icons.Default.Person,
                            gradient = listOf(Color(0xFFEC4899), Color(0xFFBE185D)),
                            onClick = { onCategoryClick(NavigationTab.ARTISTS) },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryBrowseCard(
                            title = "Folders",
                            subtitle = "Device storage",
                            icon = Icons.Default.Folder,
                            gradient = listOf(Color(0xFF6366F1), Color(0xFF4338CA)),
                            onClick = { onCategoryClick(NavigationTab.FOLDERS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            // Live Search Results
            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Please check the spelling or try another title, artist or album.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredSongs.size} results found",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(filteredSongs, key = { it.id }) { song ->
                        val isCurrent = song.id == currentSongId
                        SongItemCard(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onSongClick = { onSongClick(song, filteredSongs) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onDeleteSong = { onDeleteSong(song) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Spotify-style colorful Category Card (Songs, Albums, Artists, Folders)
 */
@Composable
fun CategoryBrowseCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("category_card_$title")
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradient))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 17.sp
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            )
        }

        // Tilted category icon in bottom right corner
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .size(46.dp)
                .align(Alignment.BottomEnd)
        )
    }
}
