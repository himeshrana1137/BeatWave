package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationTab
import com.example.model.SortOption
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlightDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    activeSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    songCount: Int,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Search Input Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .testTag("search_text_field")
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = {
                Text(
                    text = "Search tracks, artists, albums...",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SecondaryCyan,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = PrimaryNeon,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Navigation Tabs: Songs, Albums, Artists, Folders, Videos, Playlists, Favorites
        val tabs = listOf(
            Triple(NavigationTab.SONGS, "Songs", Icons.Default.MusicNote),
            Triple(NavigationTab.ALBUMS, "Albums", Icons.Default.Album),
            Triple(NavigationTab.ARTISTS, "Artists", Icons.Default.Person),
            Triple(NavigationTab.FOLDERS, "Folders", Icons.Default.Folder),
            Triple(NavigationTab.VIDEOS, "Videos", Icons.Default.Videocam),
            Triple(NavigationTab.PLAYLISTS, "Playlists", Icons.Default.QueueMusic),
            Triple(NavigationTab.FAVORITES, "Favorites", Icons.Default.Favorite),
            Triple(NavigationTab.RECENTLY_PLAYED, "Recent", Icons.Default.History),
            Triple(NavigationTab.MOST_PLAYED, "Top Played", Icons.Default.TrendingUp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (tab, label, icon) ->
                    val isSelected = activeTab == tab
                    val tabTag = when (tab) {
                        NavigationTab.SONGS -> "nav_tab_songs"
                        NavigationTab.ALBUMS -> "nav_tab_albums"
                        NavigationTab.ARTISTS -> "nav_tab_artists"
                        NavigationTab.FOLDERS -> "nav_tab_folders"
                        NavigationTab.VIDEOS -> "nav_tab_videos"
                        NavigationTab.PLAYLISTS -> "nav_tab_playlists"
                        NavigationTab.FAVORITES -> "nav_tab_favorites"
                        NavigationTab.RECENTLY_PLAYED -> "nav_tab_recent"
                        NavigationTab.MOST_PLAYED -> "nav_tab_most_played"
                    }

                    Box(
                        modifier = Modifier
                            .testTag(tabTag)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryNeon else Color.Transparent)
                            .clickable { onTabSelected(tab) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Sort button
            if (activeTab != NavigationTab.PLAYLISTS && activeTab != NavigationTab.RECENTLY_PLAYED && activeTab != NavigationTab.MOST_PLAYED) {
                Box {
                    Row(
                        modifier = Modifier
                            .testTag("sort_button")
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .clickable { sortMenuExpanded = true }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = when (activeSort) {
                                SortOption.TITLE_ASC -> "A-Z"
                                SortOption.ARTIST_ASC -> "Artist"
                                SortOption.DURATION_DESC -> "Time"
                                SortOption.DATE_ADDED_DESC -> "Recent"
                                SortOption.MOST_PLAYED -> "Plays"
                                SortOption.RECENTLY_PLAYED -> "Played"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(SurfaceHighlightDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recently Added", color = TextPrimary) },
                            onClick = {
                                onSortSelected(SortOption.DATE_ADDED_DESC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Title (A-Z)", color = TextPrimary) },
                            onClick = {
                                onSortSelected(SortOption.TITLE_ASC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Artist", color = TextPrimary) },
                            onClick = {
                                onSortSelected(SortOption.ARTIST_ASC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duration", color = TextPrimary) },
                            onClick = {
                                onSortSelected(SortOption.DURATION_DESC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Most Played", color = TextPrimary) },
                            onClick = {
                                onSortSelected(SortOption.MOST_PLAYED)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
