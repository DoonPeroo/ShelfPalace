package com.example.retrovault.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.retrovault.R
import com.example.retrovault.data.GameRepository
import com.example.retrovault.data.MovieRepository
import com.example.retrovault.data.MusicRepository
import com.example.retrovault.data.SortOption
import com.example.retrovault.data.StaticData
import com.example.retrovault.ui.components.*
import com.example.retrovault.util.PlatformUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    gameRepository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    settingsRepository: com.example.retrovault.data.SettingsRepository,
    onGameSelected: (String) -> Unit,
    onMovieSelected: (String) -> Unit,
    onMusicSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    val favoriteGames by gameRepository.getFavoriteGames().collectAsState(initial = emptyList())
    val favoriteMovies by movieRepository.getFavoriteMovies().collectAsState(initial = emptyList())
    val favoriteMusic by musicRepository.getFavoriteMusic().collectAsState(initial = emptyList())

    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
    
    val currentSortOption by settingsRepository.sortOption.collectAsState(initial = SortOption.NAME)
    val scope = rememberCoroutineScope()

    val filteredGames = remember(favoriteGames, disabledIds, currentSortOption) {
        if (disabledIds.contains("media_games")) return@remember emptyList()
        val filtered = favoriteGames.filter { game ->
            val platform = StaticData.platforms.find { it.id == game.platformId }
            platform != null && !disabledIds.contains(platform.id) && !disabledIds.contains(platform.manufacturerId)
        }
        when (currentSortOption) {
            SortOption.NAME -> filtered.sortedBy { it.title }
            SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
            SortOption.PLATFORM -> filtered.sortedBy { game -> 
                StaticData.platforms.find { it.id == game.platformId }?.name ?: ""
            }
        }
    }

    val filteredMovies = remember(favoriteMovies, disabledIds, currentSortOption) {
        val filtered = if (disabledIds.contains("media_movies")) emptyList()
        else favoriteMovies.filter { !disabledIds.contains(it.formatId) }
        
        when (currentSortOption) {
            SortOption.NAME -> filtered.sortedBy { it.title }
            SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
            SortOption.PLATFORM -> filtered.sortedBy { movie -> 
                StaticData.movieFormats.find { it.id == movie.formatId }?.name ?: ""
            }
        }
    }

    val filteredMusic = remember(favoriteMusic, disabledIds, currentSortOption) {
        val filtered = if (disabledIds.contains("media_music")) emptyList()
        else favoriteMusic.filter { !disabledIds.contains(it.formatId) }
        
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
                    NeonHeader(
                        text = "MY FAVORITES",
                        fullWidth = false
                    ) 
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp))
                },
                actions = {
                    SortIconButton(
                        currentSortOption = currentSortOption,
                        onSortOptionSelected = { scope.launch { settingsRepository.setSortOption(it) } },
                        modifier = Modifier.padding(end = 8.dp),
                        showConsoleSort = true
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
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
                    top = padding.calculateTopPadding() + 16.dp,
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
