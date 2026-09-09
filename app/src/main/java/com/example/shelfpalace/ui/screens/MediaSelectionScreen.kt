package com.example.shelfpalace.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfpalace.R
import com.example.shelfpalace.data.Movie
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.Music
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSelectionScreen(
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    settingsRepository: SettingsRepository,
    onMoviesClick: () -> Unit,
    onMusicClick: () -> Unit,
    onMovieSelected: (String) -> Unit,
    onMusicSelected: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
    val isMoviesEnabled = !disabledIds.contains("media_movies")
    val isMusicEnabled = !disabledIds.contains("media_music")

    val allMovies by movieRepository.getAllMovies().collectAsState(initial = emptyList())
    val allMusic by musicRepository.getAllMusic().collectAsState(initial = emptyList())

    val filteredMovies = remember(allMovies, searchQuery, disabledIds) {
        if (!isMoviesEnabled) emptyList()
        else if (searchQuery.isEmpty()) emptyList<Movie>()
        else allMovies.filter { it.title.contains(searchQuery, ignoreCase = true) && !disabledIds.contains(it.formatId) }
    }

    val filteredMusic = remember(allMusic, searchQuery, disabledIds) {
        if (!isMusicEnabled) emptyList()
        else if (searchQuery.isEmpty()) emptyList<Music>()
        else allMusic.filter { (it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)) && !disabledIds.contains(it.formatId) }
    }

    Scaffold(
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isSearchActive) {
                // Inline Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        isSearchActive = false
                        searchQuery = ""
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.back),
                            contentDescription = "Exit Search",
                            tint = Color.Unspecified
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                }
                
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }

            if (!isSearchActive) {
                ShelfPalaceLogo()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonButton(
                        text = stringResource(R.string.header_games),
                        onClick = onHome,
                        modifier = Modifier.weight(1f),
                        height = 40.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    NeonButton(
                        text = stringResource(R.string.header_media),
                        onClick = { /* Already here */ },
                        modifier = Modifier.weight(1f),
                        height = 40.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), getAppCorners(8.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), getAppCorners(8.dp))
                        .clickable { isSearchActive = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "SEARCH MOVIES & MUSIC...",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                NeonHeader(
                    text = "SELECT MEDIA TYPE",
                    fullWidth = true,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cardModifier = if (isMoviesEnabled && isMusicEnabled) Modifier.weight(1f) else Modifier.fillMaxWidth(0.6f)
                    
                    if (isMoviesEnabled) {
                        NeonCard(
                            modifier = cardModifier
                                .height(120.dp)
                                .clickable { onMoviesClick() },
                            color = MaterialTheme.colorScheme.secondary
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = stringResource(R.string.header_movies),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }
                        }
                    }

                    if (isMoviesEnabled && isMusicEnabled) {
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    if (isMusicEnabled) {
                        NeonCard(
                            modifier = cardModifier
                                .height(120.dp)
                                .clickable { onMusicClick() },
                            color = MaterialTheme.colorScheme.secondary
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = stringResource(R.string.header_music),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Search Results
                if (searchQuery.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("TYPE TO SEARCH MEDIA", color = Color.White.copy(alpha = 0.5f))
                    }
                } else if (filteredMovies.isEmpty() && filteredMusic.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("NO MEDIA FOUND", color = Color.White.copy(alpha = 0.5f))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (filteredMovies.isNotEmpty()) {
                            CompactSectionHeader(text = "MOVIES", color = MaterialTheme.colorScheme.secondary)
                            filteredMovies.forEach { movie ->
                                MovieGridItem(movie = movie, onClick = { onMovieSelected(movie.id) })
                            }
                        }
                        if (filteredMusic.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CompactSectionHeader(text = "MUSIC", color = MaterialTheme.colorScheme.secondary)
                            filteredMusic.forEach { music ->
                                MusicGridItem(music = music, onClick = { onMusicSelected(music.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
