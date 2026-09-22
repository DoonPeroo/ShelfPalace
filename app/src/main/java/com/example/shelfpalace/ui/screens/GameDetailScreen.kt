package com.example.shelfpalace.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.automirrored.rounded.OpenInNew

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.data.Game
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.remote.IgdbService
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.theme.DarkBackground
import com.example.shelfpalace.util.DateUtils
import com.example.shelfpalace.util.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelfpalace.data.remote.IgdbVideo
import com.example.shelfpalace.data.remote.MetacriticService
import java.util.Locale

fun formatRatingDisplay(score: Double?): String {
    if (score == null || score <= 0 || score > 100) return "-"
    val valToFormat = if (score > 10.0) score / 10.0 else score
    val formatted = String.format(Locale.US, "%.1f", valToFormat)
    return if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
}

fun formatMetacriticCriticDisplay(score: Double?): String {
    if (score == null || score <= 0) return "-"
    return if (score > 10.0) {
        score.toInt().toString()
    } else {
        val formatted = String.format(Locale.US, "%.1f", score)
        if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
    }
}

fun formatMetacriticUserDisplay(score: Double?): String {
    if (score == null || score <= 0) return "-"
    val valToFormat = if (score > 10.0) score / 10.0 else score
    val formatted = String.format(Locale.US, "%.1f", valToFormat)
    return if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: String,
    repository: GameRepository,
    onEditGame: (String) -> Unit,
    onPlatformClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val game by repository.getGameStream(gameId).collectAsStateWithLifecycle(initialValue = null)
    var showDeleteConfirmation by remember { mutableStateOf(value = false) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var showCoverFullscreen by remember { mutableStateOf(false) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var selectedVideoTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val accentColor = MaterialTheme.colorScheme.primary
    var screenshots by remember { mutableStateOf<List<String>>(emptyList()) }
    var videos by remember { mutableStateOf<List<IgdbVideo>>(emptyList()) }
    var isMediaLoading by remember { mutableStateOf(value = false) }
    
    var hasFetchedRatings by rememberSaveable(gameId) { mutableStateOf(false) }

    LaunchedEffect(game?.id) {
        val currentGame = game ?: return@LaunchedEffect

        if (!hasFetchedRatings) {
            hasFetchedRatings = true

            try {
                var updated = false
                var newUserRating = currentGame.userRating?.takeIf { it <= 100 }
                var newIgdbCriticRating = currentGame.igdbCriticRating?.takeIf { it <= 100 }
                var newCriticRating = currentGame.criticRating?.takeIf { it <= 100 }
                var newMetacriticUserRating = currentGame.metacriticUserRating?.takeIf { it <= 100 }
                var newMetacriticCriticRating = currentGame.metacriticCriticRating?.takeIf { it <= 100 }
                var newIgdbId = currentGame.igdbId

                if (newUserRating != currentGame.userRating ||
                    newIgdbCriticRating != currentGame.igdbCriticRating ||
                    newCriticRating != currentGame.criticRating ||
                    newMetacriticUserRating != currentGame.metacriticUserRating ||
                    newMetacriticCriticRating != currentGame.metacriticCriticRating) {
                    updated = true
                }

                // Fetch IGDB ratings if missing
                if (newUserRating == null || newIgdbCriticRating == null || newIgdbId == null) {
                    val matches = withContext(Dispatchers.IO) {
                        IgdbService.search(currentGame.title, currentGame.platformId)
                    }
                    val match = matches.firstOrNull()
                    if (match != null) {
                        if (newUserRating == null) newUserRating = match.rating
                        if (newIgdbCriticRating == null) newIgdbCriticRating = match.aggregatedRating
                        if (newIgdbId == null) newIgdbId = match.id
                        if (newCriticRating == null) newCriticRating = match.aggregatedRating
                        updated = true
                    }
                }

                // Fetch Metacritic ratings if missing
                if (newMetacriticUserRating == null || newMetacriticCriticRating == null) {
                    val metacriticResult = withContext(Dispatchers.IO) {
                        MetacriticService.fetchRatings(currentGame.title, currentGame.platformId)
                    }
                    if (metacriticResult.criticScore != null || metacriticResult.userScore != null) {
                        if (newMetacriticCriticRating == null && metacriticResult.criticScore != null) {
                            newMetacriticCriticRating = metacriticResult.criticScore
                            if (newCriticRating == null) newCriticRating = metacriticResult.criticScore
                            updated = true
                        }
                        if (newMetacriticUserRating == null && metacriticResult.userScore != null) {
                            newMetacriticUserRating = metacriticResult.userScore
                            updated = true
                        }
                    }
                }

                if (updated) {
                    val updatedGame = currentGame.copy(
                        userRating = newUserRating,
                        igdbCriticRating = newIgdbCriticRating,
                        criticRating = newCriticRating,
                        metacriticUserRating = newMetacriticUserRating,
                        metacriticCriticRating = newMetacriticCriticRating,
                        igdbId = newIgdbId
                    )
                    repository.updateGame(updatedGame)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fetch Screenshots if empty
        if (screenshots.isEmpty()) {
            isMediaLoading = true
            try {
                val targetId = currentGame.igdbId ?: withContext(Dispatchers.IO) {
                    IgdbService.search(currentGame.title, currentGame.platformId).firstOrNull()?.id
                }
                if (targetId != null) {
                    val igdbGame = withContext(Dispatchers.IO) { IgdbService.getGameById(targetId) }
                    igdbGame?.let { fetched ->
                        fetched.screenshots?.let { list ->
                            screenshots = list.asSequence()
                                .mapNotNull { it.url }
                                .map { url -> if (url.startsWith("//")) "https:$url" else url }
                                .map { it.replace("t_thumb", "t_720p") }
                                .toList()
                        }
                        fetched.videos?.let { list ->
                            videos = list.filter { !it.videoId.isNullOrBlank() }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isMediaLoading = false
            }
        }
    }
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Info", "Media", "My Details")

    var statusExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("Unplayed", "Playing", "Completed")

    var showNotesDialog by remember { mutableStateOf(false) }
    var editingNotes by remember { mutableStateOf("") }

    game?.let { currentGame ->
        LaunchedEffect(currentGame.notes) {
            editingNotes = currentGame.notes
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
                            text = "GAME DETAILS",
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
                        // Favorite
                        NeonIconButton(
                            iconPainter = painterResource(
                                id = if (game?.isFavorite == true) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                            ),
                            onClick = { 
                                game?.let { currentGame ->
                                    scope.launch { repository.toggleFavorite(currentGame.id) }
                                }
                            },
                            color = if (game?.isFavorite == true) Color(0xFFAD1457) else accentColor,
                            tint = if (game?.isFavorite == true) Color(0xFFAD1457) else accentColor,
                            size = 38.dp
                        )

                        // Edit
                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.pencil),
                            onClick = { game?.let { onEditGame(it.id) } },
                            color = accentColor,
                            size = 38.dp
                        )

                        // Delete
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
                            model = currentGame.coverUri.ifEmpty { "https://via.placeholder.com/150x215?text=${currentGame.title}" },
                            contentDescription = currentGame.title,
                            modifier = Modifier
                                .width(165.dp)
                                .aspectRatio(0.7f)
                                .clip(getAppCorners(12.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), getAppCorners(12.dp))
                                .clickable(enabled = currentGame.coverUri.isNotEmpty()) {
                                    showCoverFullscreen = true
                                },
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentGame.title,
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
                            0 -> GameInfoTab(currentGame, accentColor, onPlatformClick)
                            1 -> GameMediaTab(
                                platformId = currentGame.platformId,
                                accentColor = accentColor, 
                                screenshots = screenshots, 
                                videos = videos,
                                isLoading = isMediaLoading, 
                                onImageClick = { selectedImageIndex = it },
                                onVideoClick = { id, title ->
                                    selectedVideoId = id
                                    selectedVideoTitle = title
                                }
                            )
                            2 -> GameMyDetailsTab(
                                game = currentGame,
                                accentColor = accentColor, 
                                statusExpanded = statusExpanded, 
                                onStatusClick = { statusExpanded = true },
                                onStatusDismiss = { statusExpanded = false },
                                onStatusSelect = { newStatus -> 
                                    scope.launch { repository.updateGame(currentGame.copy(status = newStatus)) }
                                },
                                statusOptions = statusOptions,
                                onNotesClick = { 
                                    editingNotes = currentGame.notes
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
                game?.let { currentGame ->
                    scope.launch { repository.updateGame(currentGame.copy(notes = newNotes)) }
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
                game?.let { currentGame ->
                    scope.launch {
                        if (currentGame.coverUri.isNotEmpty()) {
                            com.example.shelfpalace.util.StorageUtil.deleteImage(context, currentGame.coverUri)
                        }
                        repository.deleteGame(currentGame)
                        onBack()
                    }
                }
                showDeleteConfirmation = false
            }
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
        game?.coverUri?.takeIf { it.isNotEmpty() }?.let { cover ->
            FullscreenImageDialog(
                screenshots = listOf(cover),
                initialIndex = 0,
                onDismiss = { showCoverFullscreen = false },
                accentColor = accentColor
            )
        }
    }

    selectedVideoId?.let { videoId ->
        VideoPlayerDialog(
            videoId = videoId,
            videoTitle = selectedVideoTitle,
            onDismissRequest = { selectedVideoId = null },
            accentColor = accentColor
        )
    }
}

@Composable
fun GameInfoTab(
    game: Game,
    accentColor: Color,
    onPlatformClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Info Card mit integriertem 2-Spalten Wertungsraster
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            color = accentColor,
            containerAlpha = 0.1f,
            padding = 0.dp
        ) {
            Column {
                InfoRow(
                    icon = Icons.Rounded.Gamepad, 
                    label = "Platform", 
                    value = PlatformUtils.getPlatformName(game.platformId), 
                    color = accentColor,
                    onClick = { onPlatformClick(game.platformId) }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Rounded.Business, label = "Developer", value = game.developer.ifEmpty { "None" }, color = accentColor)
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Rounded.Storefront, label = "Publisher", value = game.publisher.ifEmpty { "None" }, color = accentColor)
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Rounded.Category, label = "Genre", value = game.genre.ifEmpty { "None" }, color = accentColor)
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Rounded.Event, label = "Released", value = DateUtils.formatDisplayDate(game.releaseDate).ifEmpty { "None" }, color = accentColor)

                // IGDB & Metacritic Wertungen im Info-Raster (nebeneinander & kompakter)
                val igdbUserRating = game.userRating?.takeIf { it > 0 }
                val igdbCriticRating = game.igdbCriticRating?.takeIf { it > 0 } ?: game.criticRating?.takeIf { it > 0 }
                val metacriticCriticRating = game.metacriticCriticRating?.takeIf { it > 0 }
                val metacriticUserRating = game.metacriticUserRating?.takeIf { it > 0 }

                val hasIgdb = igdbUserRating != null || igdbCriticRating != null
                val hasMetacritic = metacriticCriticRating != null || metacriticUserRating != null

                if (hasIgdb || hasMetacritic) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // IGDB Spalte
                        if (hasIgdb) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = Color(0xFF9146FF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "IGDB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9146FF),
                                            letterSpacing = 0.5.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatRatingDisplay(igdbCriticRating),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "Critic",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatRatingDisplay(igdbUserRating),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "User",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Trennlinie wenn beide vorhanden
                        if (hasIgdb && hasMetacritic) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                        }

                        // Metacritic Spalte
                        if (hasMetacritic) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFBD3F),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "METACRITIC",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFBD3F),
                                            letterSpacing = 0.5.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatMetacriticCriticDisplay(metacriticCriticRating),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "Critic",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatMetacriticUserDisplay(metacriticUserRating),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "User",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                    "Description".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = game.description.ifEmpty { "No description available." },
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f), lineHeight = 20.sp)
                )
            }
        }
    }
}

@Composable
fun GameMediaTab(
    platformId: String,
    accentColor: Color,
    screenshots: List<String>,
    videos: List<IgdbVideo>,
    isLoading: Boolean,
    onImageClick: (Int) -> Unit,
    onVideoClick: (String, String) -> Unit
) {
    val isDualScreen = platformId == "nintendo_ds" || platformId == "nintendo_3ds"

    Column {
        // Screenshots Section
        Text(
            "Screenshots",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (screenshots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.White.copy(alpha = 0.05f), getAppCorners(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), getAppCorners(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No screenshots found on IGDB", color = Color.White.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(screenshots) { index, url ->
                    val modifier = if (isDualScreen) {
                        Modifier
                            .height(280.dp)
                            .width(200.dp)
                    } else {
                        Modifier
                            .size(width = 280.dp, height = 157.dp)
                    }

                    AsyncImage(
                        model = url,
                        contentDescription = "Screenshot",
                        modifier = modifier
                            .clip(getAppCorners(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), getAppCorners(12.dp))
                            .clickable { onImageClick(index) },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Videos Section
        Text(
            "Videos",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
            }
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.White.copy(alpha = 0.05f), getAppCorners(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), getAppCorners(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.VideocamOff, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No videos found on IGDB", color = Color.White.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(videos) { _, video ->
                    val videoId = video.videoId ?: return@itemsIndexed
                    val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                    val videoTitle = video.name.takeUnless { it.isNullOrBlank() } ?: "Trailer / Gameplay"

                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .clickable {
                                onVideoClick(videoId, videoTitle)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
                                .clip(getAppCorners(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), getAppCorners(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = videoTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Dark overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )

                            // Play Button Badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = getAppCorners(20.dp),
                                border = BorderStroke(1.dp, accentColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = accentColor,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameMyDetailsTab(
    game: com.example.shelfpalace.data.Game,
    accentColor: Color,
    statusExpanded: Boolean,
    onStatusClick: () -> Unit,
    onStatusDismiss: () -> Unit,
    onStatusSelect: (String) -> Unit,
    statusOptions: List<String>,
    onNotesClick: () -> Unit
) {
    val normalizedStatus = remember(game.status) {
        if (game.status.uppercase() == "BACKLOG") "Unplayed" else game.status
    }

    val statusColor = when (normalizedStatus.uppercase()) {
        "PLAYING" -> Color(0xFF00E5FF) // Cyan
        "COMPLETED" -> Color(0xFFFFD600) // Yellow
        "UNPLAYED" -> Color(0xFFBF8AC7) // Pink-ish Purple
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
                    icon = Icons.Rounded.Gamepad,
                    label = "Status",
                    value = normalizedStatus,
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
                            val displayOption = if (option.uppercase() == "BACKLOG") "Unplayed" else option
                            val optionColor = when (displayOption.uppercase()) {
                                "PLAYING" -> Color(0xFF00E5FF)
                                "COMPLETED" -> Color(0xFFFFD600)
                                "UNPLAYED" -> Color(0xFFBF8AC7)
                                else -> Color.White
                            }
                            DropdownMenuItem(
                                text = { Text(displayOption, color = optionColor) },
                                onClick = { onStatusSelect(displayOption); onStatusDismiss() }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            DetailRow(
                icon = Icons.Rounded.Verified,
                label = "Game Condition",
                value = game.condition,
                color = accentColor,
                onClick = null
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            DetailRow(
                icon = Icons.Rounded.Layers,
                label = "Game Edition",
                value = game.gameEdition,
                color = accentColor,
                onClick = null
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            DetailRow(
                icon = Icons.Rounded.CalendarMonth,
                label = "Added to Library on",
                value = DateUtils.formatTimestamp(game.dateAdded),
                color = accentColor,
                onClick = null
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            if (game.purchaseDate.isNotBlank()) {
                DetailRow(
                    icon = Icons.Rounded.ShoppingBag,
                    label = "Purchased on",
                    value = DateUtils.formatDisplayDate(game.purchaseDate).ifEmpty { game.purchaseDate },
                    color = accentColor,
                    onClick = null
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (game.pricePaid.isNotBlank()) {
                DetailRow(
                    icon = Icons.Rounded.AttachMoney,
                    label = "Paid",
                    value = game.pricePaid,
                    color = accentColor,
                    onClick = null
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            }
            DetailRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                label = "Notes",
                value = game.notes.ifEmpty { "Tap to add notes..." },
                color = accentColor,
                valueColor = if (game.notes.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
                onClick = onNotesClick
            )
        }
    }
}

@Composable
fun VideoPlayerDialog(
    videoId: String,
    videoTitle: String,
    onDismissRequest: () -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isFullscreen by remember { mutableStateOf(false) }
    val effectiveFullscreen = isFullscreen || isLandscape

    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Configure Activity orientation, cutout mode, and system bars for fullscreen
    DisposableEffect(activity, effectiveFullscreen, customView) {
        if (activity != null) {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)

            if (effectiveFullscreen || customView != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val params = window.attributes
                    params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    window.attributes = params
                }
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (activity != null) {
                val window = activity.window
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (customView != null) {
        Dialog(
            onDismissRequest = {
                try {
                    customViewCallback?.onCustomViewHidden()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                customView = null
                customViewCallback = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val dialogView = LocalView.current
            SideEffect {
                val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
                if (dialogWindow != null) {
                    WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                    dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                    dialogWindow.setGravity(Gravity.CENTER)
                    ViewCompat.setOnApplyWindowInsetsListener(dialogView) { _, _ ->
                        WindowInsetsCompat.CONSUMED
                    }
                    val controller = WindowCompat.getInsetsController(dialogWindow, dialogView)
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val params = dialogWindow.attributes
                        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        dialogWindow.attributes = params
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { _ ->
                        (customView?.parent as? ViewGroup)?.removeView(customView)
                        customView!!
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    val webView = remember(videoId) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = false
            settings.loadWithOverviewMode = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    isFullscreen = true
                    try {
                        callback?.onCustomViewHidden()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("intent:") || url.startsWith("vnd.youtube:") || url.startsWith("market:")) {
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val jsHide = """
                        (function() {
                            var hideStyle = document.createElement('style');
                            hideStyle.innerHTML = '.ytp-youtube-button, .ytp-impression-link, .ytp-watermark, .ytp-title-link { display: none !important; opacity: 0 !important; pointer-events: none !important; }';
                            document.head.appendChild(hideStyle);
                            
                            setInterval(function() {
                                var btns = document.querySelectorAll('.ytp-youtube-button, .ytp-impression-link, .ytp-watermark, .ytp-title-link');
                                for (var i = 0; i < btns.length; i++) {
                                    btns[i].style.display = 'none';
                                    btns[i].style.opacity = '0';
                                    btns[i].style.pointerEvents = 'none';
                                }
                            }, 500);
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(jsHide, null)
                }
            }

            val htmlData = """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
                <style>
                  * {
                    margin: 0 !important;
                    padding: 0 !important;
                    box-sizing: border-box !important;
                  }
                  :root {
                    --sat: 0px !important;
                    --sar: 0px !important;
                    --sab: 0px !important;
                    --sal: 0px !important;
                    --safe-area-inset-top: 0px !important;
                    --safe-area-inset-right: 0px !important;
                    --safe-area-inset-bottom: 0px !important;
                    --safe-area-inset-left: 0px !important;
                  }
                  html, body {
                    width: 100% !important;
                    height: 100% !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    background-color: #000000 !important;
                    overflow: hidden !important;
                  }
                  .video-container {
                    position: fixed !important;
                    top: 0 !important;
                    left: 0 !important;
                    right: 0 !important;
                    bottom: 0 !important;
                    width: 100% !important;
                    height: 100% !important;
                    background-color: #000000 !important;
                    display: flex !important;
                    justify-content: center !important;
                    align-items: center !important;
                  }
                  iframe {
                    width: 100% !important;
                    height: 100% !important;
                    border: 0 !important;
                  }
                </style>
                </head>
                <body>
                  <div class="video-container">
                    <iframe id="player"
                            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&controls=1&enablejsapi=1&rel=0&modestbranding=1"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowfullscreen></iframe>
                  </div>
                </body>
                </html>
            """.trimIndent()

            loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlData, "text/html", "UTF-8", null)
        }
    }

    DisposableEffect(videoId) {
        onDispose {
            try {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            if (dialogWindow != null) {
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                dialogWindow.setGravity(Gravity.CENTER)
                ViewCompat.setOnApplyWindowInsetsListener(dialogView) { _, _ ->
                    WindowInsetsCompat.CONSUMED
                }
                val controller = WindowCompat.getInsetsController(dialogWindow, dialogView)
                if (effectiveFullscreen) {
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val params = dialogWindow.attributes
                        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        dialogWindow.attributes = params
                    }
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (effectiveFullscreen) Color.Black else Color.Black.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            // Single AndroidView instance - stays composed during fullscreen toggle!
            AndroidView(
                factory = { webView },
                modifier = if (effectiveFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 60.dp)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(getAppCorners(12.dp))
                        .border(1.dp, accentColor, getAppCorners(12.dp))
                },
                update = { view ->
                    view.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    view.requestLayout()
                    view.invalidate()
                    view.postDelayed({
                        view.evaluateJavascript("javascript:window.dispatchEvent(new Event('resize'));", null)
                    }, 100)
                }
            )

            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        if (effectiveFullscreen) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        }
                    )
                    .then(
                        if (effectiveFullscreen) {
                            Modifier
                                .displayCutoutPadding()
                                .statusBarsPadding()
                                .padding(top = 8.dp)
                        } else {
                            Modifier.statusBarsPadding()
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))

                NeonIconButton(
                    icon = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    onClick = { isFullscreen = !isFullscreen },
                    color = accentColor,
                    size = 36.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                NeonIconButton(
                    iconPainter = painterResource(id = R.drawable.ic_close),
                    onClick = onDismissRequest,
                    color = accentColor,
                    size = 36.dp
                )
            }

            if (!effectiveFullscreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    NeonButton(
                        text = "OPEN IN YOUTUBE",
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = accentColor,
                        height = 42.dp
                    )
                }
            }
        }
    }
}
