package com.example.shelfpalace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfpalace.R
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.data.SortOption
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.util.PlatformUtils
import com.example.shelfpalace.util.matchesSearchQuery
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    gameRepository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    settingsRepository: SettingsRepository,
    onGameSelected: (String) -> Unit,
    onMovieSelected: (String) -> Unit,
    onMusicSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    val favoriteGames by gameRepository.getFavoriteGames().collectAsState(initial = emptyList())
    val favoriteMovies by movieRepository.getFavoriteMovies().collectAsState(initial = emptyList())
    val favoriteMusic by musicRepository.getFavoriteMusic().collectAsState(initial = emptyList())

    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
    val accentColor = MaterialTheme.colorScheme.primary
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }
    
    val currentSortOption by settingsRepository.sortOption.collectAsState(initial = SortOption.NAME)
    val scope = rememberCoroutineScope()

    val filteredGames = remember(favoriteGames, disabledIds, currentSortOption, searchQuery) {
        if (disabledIds.contains("media_games")) return@remember emptyList()
        val base = favoriteGames.filter { game ->
            val platform = StaticData.platforms.find { it.id == game.platformId }
            platform != null && !disabledIds.contains(platform.id) && !disabledIds.contains(platform.manufacturerId)
        }
        val filtered = if (searchQuery.isBlank()) base else base.filter { it.title.matchesSearchQuery(searchQuery) }
        when (currentSortOption) {
            SortOption.NAME -> filtered.sortedBy { it.title }
            SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
            SortOption.PLATFORM -> filtered.sortedBy { game -> 
                StaticData.platforms.find { it.id == game.platformId }?.name ?: ""
            }
        }
    }

    val filteredMovies = remember(favoriteMovies, disabledIds, currentSortOption, searchQuery) {
        val base = if (disabledIds.contains("media_movies")) emptyList()
        else favoriteMovies.filter { !disabledIds.contains(it.formatId) }
        val filtered = if (searchQuery.isBlank()) base else base.filter { it.title.matchesSearchQuery(searchQuery) }
        
        when (currentSortOption) {
            SortOption.NAME -> filtered.sortedBy { it.title }
            SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
            SortOption.PLATFORM -> filtered.sortedBy { movie -> 
                StaticData.movieFormats.find { it.id == movie.formatId }?.name ?: ""
            }
        }
    }

    val filteredMusic = remember(favoriteMusic, disabledIds, currentSortOption, searchQuery) {
        val base = if (disabledIds.contains("media_music")) emptyList()
        else favoriteMusic.filter { !disabledIds.contains(it.formatId) }
        val filtered = if (searchQuery.isBlank()) base else base.filter { it.title.matchesSearchQuery(searchQuery) || it.artist.matchesSearchQuery(searchQuery) }
        
        when (currentSortOption) {
            SortOption.NAME -> filtered.sortedBy { it.title }
            SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
            SortOption.PLATFORM -> filtered.sortedBy { music -> 
                StaticData.musicFormats.find { it.id == music.formatId }?.name ?: ""
            }
        }
    }

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
                            placeholder = { Text("Search Favorites...") },
                            singleLine = true,
                            shape = getAppCorners(8.dp),
                            colors = synthwaveTextFieldColors(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                        )
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    } else {
                        Text(
                            text = "FAVORITES",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = accentColor
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.ic_close),
                            onClick = { isSearchActive = false; searchQuery = "" },
                            contentDescription = stringResource(R.string.action_clear),
                            color = accentColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else {
                        NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp))
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        SortIconButton(
                            currentSortOption = currentSortOption,
                            onSortOptionSelected = { scope.launch { settingsRepository.setSortOption(it) } },
                            showConsoleSort = true
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
            if (filteredGames.isEmpty() && filteredMovies.isEmpty() && filteredMusic.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_filled),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFAD1457).copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO FAVORITES YET",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 0.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredGames.isNotEmpty()) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        CompactSectionHeader(
                            text = "GAMES",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(filteredGames) { game ->
                        GameGridItem(
                            game = game,
                            onClick = { onGameSelected(game.id) },
                            aspectRatio = PlatformUtils.getAspectRatioForPlatform(game.platformId),
                            showFavoriteBadge = false
                        )
                    }
                }

                if (filteredMovies.isNotEmpty()) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CompactSectionHeader(
                            text = "MOVIES",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(filteredMovies) { movie ->
                        MovieGridItem(
                            movie = movie,
                            onClick = { onMovieSelected(movie.id) },
                            showFavoriteBadge = false
                        )
                    }
                }

                if (filteredMusic.isNotEmpty()) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CompactSectionHeader(
                            text = "MUSIC",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(filteredMusic) { music ->
                        MusicGridItem(
                            music = music,
                            onClick = { onMusicSelected(music.id) },
                            showFavoriteBadge = false
                        )
                    }
                }
            }
        }
    }
}
}
