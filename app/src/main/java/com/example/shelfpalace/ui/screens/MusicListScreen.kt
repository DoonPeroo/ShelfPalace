package com.example.shelfpalace.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
    val scope = rememberCoroutineScope()

    val musicList by remember(repository, formatId) { repository.getMusicForFormat(formatId) }
        .collectAsState(initial = emptyList())

    val filteredMusic = remember(musicList, searchQuery, currentSortOption) {
        val filtered = if (searchQuery.isEmpty()) musicList
        else musicList.filter { it.title.matchesSearchQuery(searchQuery) || it.artist.matchesSearchQuery(searchQuery) }

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
            CenterAlignedTopAppBar(
                title = { 
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search music...") },
                            singleLine = true,
                            shape = getAppCorners(8.dp),
                            colors = synthwaveTextFieldColors(accentColor),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                        )
                        LaunchedEffect(isSearchActive) {
                            if (isSearchActive) focusRequester.requestFocus()
                        }
                    } else {
                        val headerCorners = 12.dp
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .background(Color.Black.copy(alpha = 0.3f), getAppCorners(headerCorners))
                                .border(1.dp, accentColor.copy(alpha = 0.8f), getAppCorners(headerCorners))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "MUSIC",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 8.sp,
                                        color = accentColor.copy(alpha = 0.7f)
                                    )
                                )
                                Text(
                                    text = formatName.uppercase(),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        fontSize = 14.sp,
                                        color = accentColor
                                    )
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        NeonIconButton(
                            iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_close),
                            onClick = { isSearchActive = false; searchQuery = "" },
                            contentDescription = stringResource(R.string.content_desc_close_search),
                            modifier = Modifier.padding(start = 8.dp),
                            color = accentColor
                        )
                    } else {
                        NeonBackButton(
                            onClick = onBack, 
                            modifier = Modifier.padding(start = 8.dp), 
                            color = accentColor
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        SortIconButton(
                            currentSortOption = currentSortOption,
                            onSortOptionSelected = { scope.launch { settingsRepository.setSortOption(it) } },
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.search),
                            onClick = { isSearchActive = true },
                            contentDescription = stringResource(R.string.action_search),
                            color = accentColor,
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.camera),
                            onClick = onScanClick,
                            contentDescription = "Scan Music",
                            color = accentColor,
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
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
