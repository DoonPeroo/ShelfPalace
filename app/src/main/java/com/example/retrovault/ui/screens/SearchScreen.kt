package com.example.retrovault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.retrovault.R
import com.example.retrovault.data.*
import com.example.retrovault.ui.components.*
import com.example.retrovault.util.PlatformUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    settingsRepository: SettingsRepository,
    onGameSelected: (String) -> Unit,
    onMovieSelected: (String) -> Unit,
    onMusicSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    BackHandler {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else {
            onBack()
        }
    }
    
    val focusRequester = remember { FocusRequester() }
    
    val currentSortOption by settingsRepository.sortOption.collectAsState(initial = SortOption.NAME)
    val scope = rememberCoroutineScope()

    val allGames by repository.getAllGames().collectAsState(initial = emptyList())
    val allMovies by movieRepository.getAllMovies().collectAsState(initial = emptyList())
    val allMusic by musicRepository.getAllMusic().collectAsState(initial = emptyList())
        
    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())

    val filteredGames = remember(allGames, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_games")) emptyList()
        else {
            val enabledPlatforms = StaticData.platforms
                .filter { !disabledIds.contains(it.id) && !disabledIds.contains(it.manufacturerId) }
                .map { it.id }
                .toSet()

            val filtered = allGames.filter { 
                it.title.contains(searchQuery, ignoreCase = true) && 
                enabledPlatforms.contains(it.platformId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedBy { game -> 
                    StaticData.platforms.find { it.id == game.platformId }?.name ?: ""
                }
            }
        }
    }

    val filteredMovies = remember(allMovies, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_movies")) emptyList()
        else {
            val filtered = allMovies.filter { 
                it.title.contains(searchQuery, ignoreCase = true) && 
                !disabledIds.contains(it.formatId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedBy { movie -> 
                    StaticData.movieFormats.find { it.id == movie.formatId }?.name ?: ""
                }
            }
        }
    }

    val filteredMusic = remember(allMusic, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_music")) emptyList()
        else {
            val filtered = allMusic.filter { 
                (it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)) && 
                !disabledIds.contains(it.formatId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedBy { music -> 
                    StaticData.musicFormats.find { it.id == music.formatId }?.name ?: ""
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.msg_search_all_games)) },
                        singleLine = true,
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp))
                },
                actions = {
                    if (searchQuery.isNotEmpty()) {
                        SortIconButton(
                            currentSortOption = currentSortOption,
                            onSortOptionSelected = { scope.launch { settingsRepository.setSortOption(it) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NeonIconButton(
                            iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_close),
                            onClick = { searchQuery = "" },
                            contentDescription = stringResource(R.string.action_clear)
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (searchQuery.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 250.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            text = stringResource(R.string.msg_type_to_search),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 250.dp)
                        )
                    }
                }
            } else if (filteredGames.isEmpty() && filteredMovies.isEmpty() && filteredMusic.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val accentColor = MaterialTheme.colorScheme.primary
                    val borderRadius = 12.dp
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        shape = getAppCorners(borderRadius),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(bottom = 250.dp)
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
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filteredGames.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            SectionHeader(text = "GAMES", color = MaterialTheme.colorScheme.primary)
                        }
                        items(filteredGames) { game ->
                            GameGridItem(
                                game = game,
                                onClick = { onGameSelected(game.id) },
                                aspectRatio = PlatformUtils.getAspectRatioForPlatform(game.platformId)
                            )
                        }
                    }
                    
                    if (filteredMovies.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(text = "MOVIES", color = MaterialTheme.colorScheme.secondary)
                        }
                        items(filteredMovies) { movie ->
                            MovieGridItem(
                                movie = movie,
                                onClick = { onMovieSelected(movie.id) }
                            )
                        }
                    }

                    if (filteredMusic.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(text = "MUSIC", color = MaterialTheme.colorScheme.secondary)
                        }
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
}
