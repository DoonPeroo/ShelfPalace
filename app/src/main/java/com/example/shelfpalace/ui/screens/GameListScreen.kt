package com.example.shelfpalace.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.data.Game
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.data.SortOption
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.util.matchesSearchQuery
import com.example.shelfpalace.util.DateUtils
import com.example.shelfpalace.util.PlatformUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    platformId: String,
    repository: GameRepository,
    settingsRepository: SettingsRepository,
    onGameSelected: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onAddGame: () -> Unit,
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

    val games by remember(repository, platformId) { repository.getGamesForPlatform(platformId) }
        .collectAsState(initial = emptyList())

    val filteredGames = remember(games, searchQuery, currentSortOption, disabledIds) {
        if (disabledIds.contains("media_games")) return@remember emptyList()
        val validList = games.filter { game ->
            if (disabledIds.contains(game.platformId)) return@filter false
            val platform = StaticData.platforms.find { it.id == game.platformId }
            platform == null || !disabledIds.contains(platform.manufacturerId)
        }
        val filtered = if (searchQuery.isEmpty()) validList
        else validList.filter { it.title.matchesSearchQuery(searchQuery) }
        
        when (currentSortOption) {
            SortOption.NAME -> filtered.sortedBy { it.title }
            SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
            SortOption.PLATFORM -> filtered.sortedBy { game -> 
                StaticData.platforms.find { it.id == game.platformId }?.name ?: ""
            }
        }
    }

    val platformName = remember(platformId) {
        if (platformId == "all") "ALL GAMES"
        else StaticData.platforms.find { it.id == platformId }?.name ?: "GAMES"
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
                            placeholder = { Text(stringResource(R.string.msg_search_games)) },
                            singleLine = true,
                            shape = getAppCorners(8.dp),
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

                    val headerCorners = 12.dp
                    val headerTitle = platformName.uppercase()
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
                            contentDescription = stringResource(R.string.header_scan_cover),
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
            if (filteredGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (searchQuery.isEmpty()) {
                    SectionHeader(
                        text = stringResource(R.string.msg_no_games),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        shape = getAppCorners(12.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
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
                                color = MaterialTheme.colorScheme.primary
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
                items(filteredGames) { game ->
                    GameGridItem(
                        game = game,
                        onClick = { onGameSelected(game.id) },
                        aspectRatio = PlatformUtils.getAspectRatioForPlatform(game.platformId)
                    )
                }
            }
        }
    }
}
}
