package com.example.shelfpalace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.data.Movie
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.remote.RottenTomatoesService
import com.example.shelfpalace.data.remote.TmdbService
import com.example.shelfpalace.data.remote.TmdbVideo
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.theme.DarkBackground
import com.example.shelfpalace.util.DateUtils
import com.example.shelfpalace.util.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: String,
    repository: MovieRepository,
    onEditMovie: (String) -> Unit,
    onFormatClick: (String) -> Unit,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val movie by repository.getMovieStream(movieId).collectAsStateWithLifecycle(initialValue = null)
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var showCoverFullscreen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val accentColor = MaterialTheme.colorScheme.primary
    
    var screenshots by remember { mutableStateOf(emptyList<String>()) }
    var videos by remember { mutableStateOf(emptyList<TmdbVideo>()) }
    var isMediaLoading by remember { mutableStateOf(false) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var selectedVideoTitle by remember { mutableStateOf("") }
    var fetchedTmdbRating by remember { mutableStateOf<String?>(null) }
    var fetchedTomatometer by remember { mutableStateOf<Int?>(null) }
    var fetchedPopcornmeter by remember { mutableStateOf<Int?>(null) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Info", "Media", "My Details")

    var statusExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("Plan to watch", "Watching", "Watched")

    var showNotesDialog by remember { mutableStateOf(false) }
    var editingNotes by remember { mutableStateOf("") }

    movie?.let { currentMovie ->
        LaunchedEffect(currentMovie.notes) {
            editingNotes = currentMovie.notes
        }

        LaunchedEffect(currentMovie.title, currentMovie.releaseDate, currentMovie.language) {
            screenshots = emptyList()
            videos = emptyList()
            fetchedTmdbRating = currentMovie.tmdbRating?.let { String.format(Locale.US, "%.1f", it) }
            fetchedTomatometer = currentMovie.tomatometer
            fetchedPopcornmeter = currentMovie.popcornmeter

            if (currentMovie.title.isNotBlank()) {
                isMediaLoading = true
                try {
                    val yearPart = currentMovie.releaseDate.take(4)
                    val movieLanguage = currentMovie.language.ifBlank { "en-US" }

                    var newTmdbRating = currentMovie.tmdbRating
                    val searchRes = withContext(Dispatchers.IO) {
                        TmdbService.search(
                            query = currentMovie.title,
                            year = yearPart,
                            language = movieLanguage
                        )
                    }
                    val firstMovie = searchRes.results.firstOrNull()
                    if (firstMovie?.voteAverage != null && firstMovie.voteAverage > 0.0) {
                        newTmdbRating = firstMovie.voteAverage
                        fetchedTmdbRating = String.format(Locale.US, "%.1f", firstMovie.voteAverage)
                    }
                    if (firstMovie?.id != null) {
                        val (images, vids) = withContext(Dispatchers.IO) {
                            TmdbService.fetchMovieMediaById(firstMovie.id, language = movieLanguage)
                        }
                        screenshots = images.mapNotNull { it.fullUrl }
                        videos = vids
                    }

                    // Fetch Rotten Tomatoes ratings
                    val rtRating = RottenTomatoesService.fetchRatings(
                        title = currentMovie.title,
                        releaseYear = yearPart
                    )

                    var newTomatometer = currentMovie.tomatometer
                    var newPopcornmeter = currentMovie.popcornmeter

                    if (rtRating.tomatometer != null) {
                        newTomatometer = rtRating.tomatometer
                        fetchedTomatometer = rtRating.tomatometer
                    }
                    if (rtRating.popcornmeter != null) {
                        newPopcornmeter = rtRating.popcornmeter
                        fetchedPopcornmeter = rtRating.popcornmeter
                    }

                    // Update movie in repository if any rating changed
                    if (newTomatometer != currentMovie.tomatometer ||
                        newPopcornmeter != currentMovie.popcornmeter ||
                        newTmdbRating != currentMovie.tmdbRating) {
                        val updatedMovie = currentMovie.copy(
                            tomatometer = newTomatometer,
                            popcornmeter = newPopcornmeter,
                            tmdbRating = newTmdbRating
                        )
                        repository.updateMovie(updatedMovie)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isMediaLoading = false
            }
        }

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.back),
                        onClick = onBack,
                        color = accentColor,
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val headerCorners = 24.dp
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
                            text = "MOVIE DETAILS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontSize = 13.sp,
                                color = accentColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NeonIconButton(
                            iconPainter = painterResource(
                                id = if (currentMovie.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                            ),
                            onClick = { 
                                scope.launch { repository.toggleFavorite(currentMovie.id) }
                            },
                            color = if (currentMovie.isFavorite) Color(0xFFAD1457) else accentColor,
                            tint = if (currentMovie.isFavorite) Color(0xFFAD1457) else accentColor,
                            size = 38.dp
                        )

                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.pencil),
                            onClick = { onEditMovie(currentMovie.id) },
                            color = accentColor,
                            size = 38.dp
                        )

                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.trash_can),
                            onClick = { showDeleteConfirmation = true },
                            color = accentColor,
                            size = 38.dp
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Fixed Header Section (Cover + Title + Tabs)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = currentMovie.coverUri.ifEmpty { "https://via.placeholder.com/150x215?text=${currentMovie.title}" },
                            contentDescription = currentMovie.title,
                            modifier = Modifier
                                .width(165.dp)
                                .aspectRatio(0.7f)
                                .clip(getAppCorners(12.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), getAppCorners(12.dp))
                                .clickable(enabled = currentMovie.coverUri.isNotEmpty()) {
                                    showCoverFullscreen = true
                                },
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentMovie.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fixed Tabs (Info / Media / My Details)
                    DetailTabSelector(
                        tabs = tabs,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = { selectedTabIndex = it },
                        accentColor = accentColor
                    )
                }

                // Scrollable Tab Content Area below the Tabs invisible line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .clipToBounds()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp)
                    ) {
                        when (selectedTabIndex) {
                            0 -> MovieInfoTab(
                                movie = currentMovie,
                                accentColor = accentColor,
                                onFormatClick = onFormatClick,
                                rating = fetchedTmdbRating,
                                tomatometer = fetchedTomatometer,
                                popcornmeter = fetchedPopcornmeter
                            )
                            1 -> MovieMediaTab(
                                accentColor = accentColor,
                                screenshots = screenshots,
                                videos = videos,
                                isLoading = isMediaLoading,
                                onImageClick = { selectedImageIndex = it },
                                onVideoClick = { vId, vTitle ->
                                    selectedVideoId = vId
                                    selectedVideoTitle = vTitle
                                }
                            )
                            2 -> MovieMyDetailsTab(
                                movie = currentMovie,
                                accentColor = accentColor,
                                statusExpanded = statusExpanded,
                                onStatusClick = { statusExpanded = true },
                                onStatusDismiss = { statusExpanded = false },
                                onStatusSelect = { newStatus ->
                                    scope.launch { repository.updateMovie(currentMovie.copy(status = newStatus)) }
                                },
                                statusOptions = statusOptions,
                                onNotesClick = {
                                    editingNotes = currentMovie.notes
                                    showNotesDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    } ?: Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = accentColor)
    }

    if (showNotesDialog) {
        NotesDialog(
            initialNotes = editingNotes,
            onDismissRequest = { showNotesDialog = false },
            onSave = { newNotes ->
                movie?.let { currentMovie ->
                    scope.launch { repository.updateMovie(currentMovie.copy(notes = newNotes)) }
                }
                showNotesDialog = false
            },
            accentColor = accentColor
        )
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                movie?.let { currentMovie ->
                    scope.launch {
                        if (currentMovie.coverUri.isNotEmpty()) {
                            StorageUtil.deleteImage(context, currentMovie.coverUri)
                        }
                        repository.deleteMovie(currentMovie)
                        onBack()
                    }
                }
                showDeleteConfirmation = false
            },
            title = stringResource(R.string.msg_delete_movie_confirmation_title),
            text = stringResource(R.string.msg_delete_movie_confirmation_text)
        )
    }

    selectedVideoId?.let { vId ->
        VideoPlayerDialog(
            videoId = vId,
            videoTitle = selectedVideoTitle,
            onDismissRequest = { selectedVideoId = null },
            accentColor = accentColor
        )
    }

    selectedImageIndex?.let { index ->
        FullscreenImageDialog(
            screenshots = screenshots,
            initialIndex = index,
            onDismiss = { selectedImageIndex = null },
            accentColor = accentColor
        )
    }

    if (showCoverFullscreen) {
        movie?.coverUri?.takeIf { it.isNotEmpty() }?.let { cover ->
            FullscreenImageDialog(
                screenshots = listOf(cover),
                initialIndex = 0,
                onDismiss = { showCoverFullscreen = false },
                accentColor = accentColor
            )
        }
    }
}

@Composable
fun MovieInfoTab(
    movie: Movie,
    accentColor: Color,
    onFormatClick: (String) -> Unit,
    rating: String? = null,
    tomatometer: Int? = null,
    popcornmeter: Int? = null
) {
    val (formatLabel, formatIcon) = when(movie.formatId) {
        "vhs" -> "VHS" to Icons.Rounded.Videocam
        "laserdisc" -> "LaserDisc" to Icons.Rounded.DiscFull
        "vcd" -> "Video CD" to Icons.Rounded.Album
        "umd" -> "UMD Video" to Icons.Rounded.Album
        "dvd" -> "DVD" to Icons.Rounded.Album
        "bluray" -> "Blu-ray" to Icons.Rounded.Album
        "hddvd" -> "HD DVD" to Icons.Rounded.Album
        "bluray3d" -> "3D Blu-ray" to Icons.Rounded.Movie
        "bluray4k" -> "4K Blu-ray" to Icons.Rounded.HighQuality
        else -> "Unknown" to Icons.Rounded.Movie
    }

    val effectiveTomatometer = tomatometer ?: movie.tomatometer
    val effectivePopcornmeter = popcornmeter ?: movie.popcornmeter

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Info Card
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            color = accentColor,
            containerAlpha = 0.1f,
            padding = 0.dp
        ) {
            Column {
                InfoRow(
                    icon = formatIcon,
                    label = "Medium",
                    value = formatLabel,
                    color = accentColor,
                    onClick = { onFormatClick(movie.formatId) }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(
                    icon = Icons.Rounded.Person, 
                    label = stringResource(R.string.label_director), 
                    value = movie.director.ifEmpty { "None" }, 
                    color = accentColor
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(
                    icon = Icons.Rounded.Groups, 
                    label = stringResource(R.string.label_cast), 
                    value = movie.cast.ifEmpty { "None" }, 
                    color = accentColor
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(
                    icon = Icons.Rounded.Category, 
                    label = "Genre", 
                    value = movie.genre.ifEmpty { "None" }, 
                    color = accentColor
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(
                    icon = Icons.Rounded.Event, 
                    label = "Released", 
                    value = DateUtils.formatDisplayDate(movie.releaseDate).ifEmpty { "None" }, 
                    color = accentColor
                )

                if (!rating.isNullOrBlank()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    InfoRow(
                        icon = Icons.Rounded.Star,
                        label = "Rating (TMDb)",
                        value = "⭐️ $rating / 10",
                        color = Color(0xFFFFC107)
                    )
                }

                if (effectiveTomatometer != null) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    InfoRowWithEmoji(
                        emoji = "🍅",
                        label = "Tomatometer (Critics)",
                        value = "$effectiveTomatometer%" + (if (effectiveTomatometer >= 60) " • Fresh" else " • Rotten"),
                        valueColor = if (effectiveTomatometer >= 60) Color(0xFFFF3D00) else Color(0xFF8BC34A)
                    )
                }

                if (effectivePopcornmeter != null) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                    InfoRowWithEmoji(
                        emoji = "🍿",
                        label = "Popcornmeter (Audience)",
                        value = "$effectivePopcornmeter%" + (if (effectivePopcornmeter >= 60) " • Fresh" else " • Spilled"),
                        valueColor = if (effectivePopcornmeter >= 60) Color(0xFFFFC107) else Color(0xFF9E9E9E)
                    )
                }
            }
        }

        // Description Card
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            color = accentColor,
            containerAlpha = 0.1f
        ) {
            Column {
                Text(
                    stringResource(R.string.label_description).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.description.ifEmpty { "No description available." },
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f), lineHeight = 20.sp)
                )
            }
        }
    }
}

@Composable
fun MovieMediaTab(
    accentColor: Color,
    screenshots: List<String>,
    videos: List<TmdbVideo>,
    isLoading: Boolean,
    onImageClick: (Int) -> Unit,
    onVideoClick: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // TRAILERS & VIDEOS SECTION
        Text(
            "Trailers & Videos",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(28.dp))
            }
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color.White.copy(alpha = 0.05f), getAppCorners(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), getAppCorners(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No videos available", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(videos) { video ->
                    Box(
                        modifier = Modifier
                            .size(width = 200.dp, height = 112.dp)
                            .clip(getAppCorners(12.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), getAppCorners(12.dp))
                            .clickable { video.key?.let { onVideoClick(it, video.name ?: "Trailer") } },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .border(1.dp, accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // SCREENSHOTS & STILLS SECTION
        Text(
            "Screenshots & Stills",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(28.dp))
            }
        } else if (screenshots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.White.copy(alpha = 0.05f), getAppCorners(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), getAppCorners(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("No screenshots available", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(screenshots) { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Screenshot",
                        modifier = Modifier
                            .size(width = 280.dp, height = 157.dp)
                            .clip(getAppCorners(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), getAppCorners(12.dp))
                            .clickable { onImageClick(index) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun MovieMyDetailsTab(
    movie: Movie, 
    accentColor: Color,
    statusExpanded: Boolean,
    onStatusClick: () -> Unit,
    onStatusDismiss: () -> Unit,
    onStatusSelect: (String) -> Unit,
    statusOptions: List<String>,
    onNotesClick: () -> Unit
) {
    val statusColor = when (movie.status.uppercase()) {
        "WATCHING" -> Color(0xFF00E5FF)
        "WATCHED" -> Color(0xFFFFD600)
        "PLAN TO WATCH" -> Color(0xFFBF8AC7)
        else -> Color.White
    }

    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor,
        containerAlpha = 0.1f,
        padding = 0.dp
    ) {
        Column {
            Box {
                DetailRow(
                    icon = Icons.Rounded.Movie,
                    label = "Status",
                    value = movie.status,
                    color = accentColor,
                    valueColor = statusColor,
                    onClick = onStatusClick
                )
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = DarkBackground)) {
                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { onStatusDismiss() },
                        modifier = Modifier
                            .background(DarkBackground)
                            .border(1.dp, accentColor.copy(alpha = 0.5f), getAppCorners(8.dp))
                    ) {
                        statusOptions.forEach { option ->
                            val optionColor = when (option.uppercase()) {
                                "WATCHING" -> Color(0xFF00E5FF)
                                "WATCHED" -> Color(0xFFFFD600)
                                "PLAN TO WATCH" -> Color(0xFFBF8AC7)
                                else -> Color.White
                            }
                            DropdownMenuItem(
                                text = { Text(option, color = optionColor) },
                                onClick = { onStatusSelect(option); onStatusDismiss() }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            DetailRow(
                icon = Icons.Rounded.CalendarMonth,
                label = "Added to Library on",
                value = DateUtils.formatTimestamp(movie.dateAdded),
                color = accentColor,
                onClick = null
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            if (movie.purchaseDate.isNotBlank()) {
                DetailRow(
                    icon = Icons.Rounded.ShoppingBag,
                    label = "Purchased on",
                    value = DateUtils.formatDisplayDate(movie.purchaseDate).ifEmpty { movie.purchaseDate },
                    color = accentColor,
                    onClick = null
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (movie.pricePaid.isNotBlank()) {
                DetailRow(
                    icon = Icons.Rounded.AttachMoney,
                    label = "Paid",
                    value = movie.pricePaid,
                    color = accentColor,
                    onClick = null
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            }
            DetailRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                label = "Notes",
                value = movie.notes.ifEmpty { "Tap to add notes..." },
                color = accentColor,
                valueColor = if (movie.notes.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
                onClick = onNotesClick
            )
        }
    }
}
