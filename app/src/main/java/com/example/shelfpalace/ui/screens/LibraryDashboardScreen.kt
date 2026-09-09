package com.example.shelfpalace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.example.shelfpalace.ui.components.getAppCorners
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.data.Game
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.Movie
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.Music
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.ui.components.NeonCard
import com.example.shelfpalace.ui.theme.DarkBackground
import com.example.shelfpalace.ui.theme.SynthwaveLavender
import com.example.shelfpalace.ui.theme.SynthwavePink
import com.example.shelfpalace.util.PlatformUtils

@Composable
fun LibraryDashboardScreen(
    gameRepository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    onGameClick: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onMusicClick: (String) -> Unit,
    onGamesHeaderClick: (String) -> Unit,
    onManufacturerSelected: (String) -> Unit,
    onMoviesHeaderClick: (String) -> Unit,
    onMovieFormatSelected: (String) -> Unit,
    onMusicHeaderClick: (String) -> Unit,
    onMusicFormatSelected: (String) -> Unit,
    onScanClick: () -> Unit,
    disabledIds: Set<String> = emptySet(),
    filterOption: String = "Default",
    onFilterOptionChange: (String) -> Unit = {}
) {
    val games by gameRepository.getAllGames().collectAsStateWithLifecycle(initialValue = emptyList())
    val movies by movieRepository.getAllMovies().collectAsStateWithLifecycle(initialValue = emptyList())
    val music by musicRepository.getAllMusic().collectAsStateWithLifecycle(initialValue = emptyList())

    var searchQuery by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = searchQuery.isNotEmpty()) {
        searchQuery = ""
    }

    val filteredGames = remember(games, searchQuery, filterOption, disabledIds) {
        if (disabledIds.contains("media_games")) return@remember emptyList()
        val baseList = if (filterOption == "Recently Added") games.reversed() else games
        if (searchQuery.isEmpty()) baseList.take(10)
        else baseList.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val filteredMovies = remember(movies, searchQuery, filterOption, disabledIds) {
        if (disabledIds.contains("media_movies")) return@remember emptyList()
        val baseList = if (filterOption == "Recently Added") movies.reversed() else movies
        if (searchQuery.isEmpty()) baseList.take(10)
        else baseList.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val filteredMusic = remember(music, searchQuery, filterOption, disabledIds) {
        if (disabledIds.contains("media_music")) return@remember emptyList()
        val baseList = if (filterOption == "Recently Added") music.reversed() else music
        if (searchQuery.isEmpty()) music.take(10)
        else baseList.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sticky Top Area (Header + Search + Categories)
        Column(
            modifier = Modifier
                .statusBarsPadding()
        ) {
            // 1. Header
            LibraryHeader()
            
            // 2. Search & Filter & Category Tabs (Shifted Upward)
            Box(modifier = Modifier.offset(y = (-24).dp)) {
                Column {
                    SearchAndFilterRow(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onScanClick = onScanClick,
                        onFilterOptionSelected = onFilterOptionChange
                    )

                    if (searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // 3. Category Tabs (Now Sticky)
                        CategoryTabs(
                            onGamesClick = { onGamesHeaderClick(searchQuery) },
                            onManufacturerSelected = onManufacturerSelected,
                            onMoviesClick = { onMoviesHeaderClick(searchQuery) },
                            onMovieFormatSelected = onMovieFormatSelected,
                            onMusicClick = { onMusicHeaderClick(searchQuery) },
                            onMusicFormatSelected = onMusicFormatSelected,
                            disabledIds = disabledIds
                        )
                    }
                }
            }
        }

        // Scrollable Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            if (searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

                    // 4. Content Sections
            // Games Section
            if (!disabledIds.contains("media_games") && filteredGames.isNotEmpty()) {
                val sectionTitle = when {
                    searchQuery.isNotEmpty() -> "Games Search"
                    filterOption == "Recently Added" -> "Recently Added Games"
                    else -> "My Games"
                }
                DashboardSection(
                    title = sectionTitle,
                    onHeaderClick = { onGamesHeaderClick(searchQuery) },
                    items = filteredGames,
                    itemContent = { game ->
                        DashboardItemCard(
                            title = game.title,
                            imageUri = game.coverUri,
                            tag = PlatformUtils.getShortPlatformName(game.platformId)
                        ) { onGameClick(game.id) }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Movies Section
            if (!disabledIds.contains("media_movies") && filteredMovies.isNotEmpty()) {
                val sectionTitle = when {
                    searchQuery.isNotEmpty() -> "Movies Search"
                    filterOption == "Recently Added" -> "Recently Added Movies"
                    else -> "My Movies"
                }
                DashboardSection(
                    title = sectionTitle,
                    onHeaderClick = { onMoviesHeaderClick(searchQuery) },
                    items = filteredMovies,
                    itemContent = { movie ->
                        DashboardItemCard(
                            title = movie.title,
                            imageUri = movie.coverUri,
                            tag = movie.formatId.uppercase()
                        ) { onMovieClick(movie.id) }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Music Section
            if (!disabledIds.contains("media_music") && filteredMusic.isNotEmpty()) {
                val sectionTitle = when {
                    searchQuery.isNotEmpty() -> "Music Search"
                    filterOption == "Recently Added" -> "Recently Added Music"
                    else -> "My Music"
                }
                DashboardSection(
                    title = sectionTitle,
                    onHeaderClick = { onMusicHeaderClick(searchQuery) },
                    items = filteredMusic,
                    itemContent = { album ->
                        DashboardItemCard(
                            title = album.title,
                            imageUri = album.coverUri,
                            tag = album.formatId.uppercase(),
                            aspectRatio = 1f,
                            cardWidth = 160.dp
                        ) { onMusicClick(album.id) }
                    }
                )
            }
            
            if (searchQuery.isNotEmpty() && filteredGames.isEmpty() && filteredMovies.isEmpty() && filteredMusic.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    Text("No results found for \"$searchQuery\"", color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun LibraryHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Shelf Palace Logo",
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .offset(y = (-18).dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun SearchAndFilterRow(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onFilterOptionSelected: (String) -> Unit
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        NeonCard(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
            containerAlpha = 0.3f,
            padding = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.search),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_close),
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { onScanClick() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.camera),
                        contentDescription = "Scan",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Filter Button
        Box {
            com.example.shelfpalace.ui.components.NeonCard(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                containerAlpha = 0.3f,
                padding = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { filterMenuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.control),
                        contentDescription = "Filter",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Black.copy(alpha = 0.4f))) {
                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), getAppCorners(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Default View", color = Color.White, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onFilterOptionSelected("Default")
                            filterMenuExpanded = false
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    DropdownMenuItem(
                        text = { Text("Recently Added", color = Color.White, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onFilterOptionSelected("Recently Added")
                            filterMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryTabs(
    onGamesClick: () -> Unit,
    onManufacturerSelected: (String) -> Unit,
    onMoviesClick: () -> Unit,
    onMovieFormatSelected: (String) -> Unit,
    onMusicClick: () -> Unit,
    onMusicFormatSelected: (String) -> Unit,
    disabledIds: Set<String> = emptySet()
) {
    var gamesMenuExpanded by remember { mutableStateOf(false) }
    var moviesMenuExpanded by remember { mutableStateOf(false) }
    var musicMenuExpanded by remember { mutableStateOf(false) }

    val manufacturers = StaticData.manufacturers.filter { !disabledIds.contains(it.id) }
    val movieFormats = StaticData.movieFormats.filter { !disabledIds.contains(it.id) }
    val musicFormats = StaticData.musicFormats.filter { !disabledIds.contains(it.id) }

    val categories = remember(disabledIds) {
        val list = mutableListOf<CategoryItem>()
        if (!disabledIds.contains("media_games")) {
            list.add(CategoryItem(name = "Games", iconResId = R.drawable.game) { gamesMenuExpanded = true })
        }
        if (!disabledIds.contains("media_movies")) {
            list.add(CategoryItem(name = "Movies", iconResId = R.drawable.movie) { moviesMenuExpanded = true })
        }
        if (!disabledIds.contains("media_music")) {
            list.add(CategoryItem(name = "Music", iconResId = R.drawable.music) { musicMenuExpanded = true })
        }
        if (!disabledIds.contains("media_more")) {
            list.add(CategoryItem("More", Icons.Rounded.MoreHoriz) { /* Handle More */ })
        }
        list
    }

    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.primary,
        containerAlpha = 0.3f,
        padding = 4.dp
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories) { category ->
                var itemWidthDp by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        itemWidthDp = with(density) { coordinates.size.width.toDp() }
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(getAppCorners(12.dp))
                            .clickable { category.onClick() }
                            .padding(vertical = 3.dp, horizontal = 6.dp)
                    ) {
                        if (category.iconResId != null) {
                            Icon(
                                painter = painterResource(id = category.iconResId),
                                contentDescription = category.name,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(44.dp)
                            )
                        } else if (category.icon != null) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.name,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    val menuWidthDp = 118.dp
                    val xOffsetDp = if (itemWidthDp > 0.dp) (itemWidthDp - menuWidthDp) / 2 else (-35).dp

                    when (category.name) {
                        "Games" -> {
                            DropdownMenu(
                                expanded = gamesMenuExpanded,
                                onDismissRequest = { gamesMenuExpanded = false },
                                offset = DpOffset(xOffsetDp, 4.dp),
                                containerColor = Color.Black.copy(alpha = 0.95f),
                                modifier = Modifier
                                    .width(menuWidthDp)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                manufacturers.forEach { manufacturer ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = manufacturer.name, 
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) 
                                        },
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        onClick = {
                                            gamesMenuExpanded = false
                                            onManufacturerSelected(manufacturer.id)
                                        }
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "ALL MFRS.", 
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, fontSize = 14.sp),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) 
                                    },
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    onClick = {
                                        gamesMenuExpanded = false
                                        onGamesClick()
                                    }
                                )
                            }
                        }
                        "Movies" -> {
                            DropdownMenu(
                                expanded = moviesMenuExpanded,
                                onDismissRequest = { moviesMenuExpanded = false },
                                offset = DpOffset(xOffsetDp, 4.dp),
                                containerColor = Color.Black.copy(alpha = 0.95f),
                                modifier = Modifier
                                    .width(menuWidthDp)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                movieFormats.forEach { format ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = format.name, 
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) 
                                        },
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        onClick = {
                                            moviesMenuExpanded = false
                                            onMovieFormatSelected(format.id)
                                        }
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "ALL MOVIES", 
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, fontSize = 14.sp),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) 
                                    },
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    onClick = {
                                        moviesMenuExpanded = false
                                        onMoviesClick()
                                    }
                                )
                            }
                        }
                        "Music" -> {
                            DropdownMenu(
                                expanded = musicMenuExpanded,
                                onDismissRequest = { musicMenuExpanded = false },
                                offset = DpOffset(xOffsetDp, 4.dp),
                                containerColor = Color.Black.copy(alpha = 0.95f),
                                modifier = Modifier
                                    .width(menuWidthDp)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                    musicFormats.forEach { format ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    text = format.name, 
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) 
                                            },
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            onClick = {
                                                musicMenuExpanded = false
                                                onMusicFormatSelected(format.id)
                                            }
                                        )
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = "ALL MUSIC", 
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, fontSize = 14.sp),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) 
                                        },
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        onClick = {
                                            musicMenuExpanded = false
                                            onMusicClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

data class CategoryItem(
    val name: String,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val onClick: () -> Unit
)

@Composable
fun <T> DashboardSection(
    title: String,
    onHeaderClick: () -> Unit,
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHeaderClick() }
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "See more",
                tint = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                itemContent(item)
            }
        }
    }
}

@Composable
fun DashboardItemCard(
    title: String,
    imageUri: String,
    tag: String = "",
    aspectRatio: Float = 0.75f,
    cardWidth: Dp = 140.dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(getAppCorners(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            AsyncImage(
                model = imageUri.ifEmpty { "https://via.placeholder.com/140x186?text=$title" },
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (tag.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), getAppCorners(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
