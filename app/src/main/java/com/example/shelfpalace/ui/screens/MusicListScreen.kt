package com.example.shelfpalace.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfpalace.R
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.data.SortOption
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.util.matchesSearchQuery
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
    formatId: String,
    repository: MusicRepository,
    settingsRepository: SettingsRepository,
    onMusicSelected: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onAddMusic: () -> Unit,
    onScanClick: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val currentSortOption by settingsRepository.sortOption.collectAsState(initial = SortOption.NAME)
    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val musicList by remember(repository, formatId) { repository.getMusicForFormat(formatId) }
        .collectAsState(initial = emptyList())

    val filteredMusic = remember(musicList, searchQuery, currentSortOption, disabledIds) {
        if (disabledIds.contains("media_music")) return@remember emptyList()
        val validList = musicList.filter { !disabledIds.contains(it.formatId) }
        val filtered = if (searchQuery.isEmpty()) validList
        else validList.filter { it.title.matchesSearchQuery(searchQuery) || it.artist.matchesSearchQuery(searchQuery) }

        when (currentSortOption) {
            SortOption.NAME -> filtered.sortedBy { it.title }
            SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
            SortOption.PLATFORM -> filtered.sortedBy { music -> 
                StaticData.musicFormats.find { it.id == music.formatId }?.name ?: ""
            }
        }
    }

    val formatName = remember(formatId) {
        if (formatId == "all") "ALL MUSIC"
        else StaticData.musicFormats.find { it.id == formatId }?.name ?: "MUSIC"
    }

    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            if (isSearchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.ic_close),
                            onClick = { isSearchActive = false; searchQuery = "" },
                            contentDescription = stringResource(R.string.content_desc_close_search),
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search music...") },
                            singleLine = true,
                            shape = getAppCorners(),
                            colors = synthwaveTextFieldColors(accentColor),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                        )
                        LaunchedEffect(isSearchActive) {
                            if (isSearchActive) focusRequester.requestFocus()
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NeonBackButton(
                        onClick = onBack,
                        color = accentColor
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val headerCorners = 24.dp
                    val headerTitle = formatName.uppercase()
                    val dynamicFontSize = when {
                        headerTitle.length >= 22 -> 10.5.sp
                        headerTitle.length >= 17 -> 11.5.sp
                        headerTitle.length >= 14 -> 12.5.sp
                        headerTitle.length >= 10 -> 13.sp
                        else -> 13.sp
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(Color.Black.copy(alpha = 0.3f), getAppCorners(headerCorners))
                            .border(1.dp, accentColor.copy(alpha = 0.8f), getAppCorners(headerCorners))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontSize = dynamicFontSize,
                                color = accentColor
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SortIconButton(
                            currentSortOption = currentSortOption,
                            onSortOptionSelected = { scope.launch { settingsRepository.setSortOption(it) } },
                            color = accentColor
                        )
                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.search),
                            onClick = { isSearchActive = true },
                            contentDescription = stringResource(R.string.action_search),
                            color = accentColor,
                            tint = Color.Unspecified
                        )
                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.camera),
                            onClick = onScanClick,
                            contentDescription = "Scan Music",
                            color = accentColor,
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clipToBounds()
        ) {
            if (filteredMusic.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (searchQuery.isEmpty()) {
                    SectionHeader(
                        text = stringResource(R.string.msg_no_music),
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        shape = getAppCorners(12.dp),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(bottom = 80.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "No Results for:".uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = searchQuery,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 0.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredMusic) { music ->
                    MusicGridItem(
                        music = music,
                        onClick = { onMusicSelected(music.id) }
                    )
                }
            }
        }
    }
}
}
