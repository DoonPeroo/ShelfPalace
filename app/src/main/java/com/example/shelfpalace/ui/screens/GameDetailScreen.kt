package com.example.shelfpalace.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.automirrored.rounded.OpenInNew

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: String,
    repository: GameRepository,
    onEditGame: (String) -> Unit,
    onPlatformClick: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val game by repository.getGameStream(gameId).collectAsStateWithLifecycle(initialValue = null)
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var selectedVideoTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val accentColor = MaterialTheme.colorScheme.primary
    val deepPurple = Color(0xFF9C27B0)
    var screenshots by remember { mutableStateOf<List<String>>(emptyList()) }
    var videos by remember { mutableStateOf<List<IgdbVideo>>(emptyList()) }
    var isMediaLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(game?.id) {
        val currentGame = game ?: return@LaunchedEffect
        
        // 1. Fetch Ratings if missing
        if (currentGame.userRating == null || currentGame.criticRating == null) {
            try {
                val matches = withContext(Dispatchers.IO) {
                    IgdbService.search(currentGame.title, currentGame.platformId)
                }
                val match = matches.firstOrNull()
                if (match != null) {
                    val newUserRating = currentGame.userRating ?: match.rating ?: match.totalRating
                    val newCriticRating = currentGame.criticRating ?: match.aggregatedRating ?: match.totalRating
                    val newIgdbId = currentGame.igdbId ?: match.id
                    
                    if (newUserRating != currentGame.userRating || 
                        newCriticRating != currentGame.criticRating || 
                        newIgdbId != currentGame.igdbId) {
                        
                        val updatedGame = currentGame.copy(
                            userRating = newUserRating,
                            criticRating = newCriticRating,
                            igdbId = newIgdbId
                        )
                        repository.updateGame(updatedGame)
                    }
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
                            screenshots = list.mapNotNull { it.url }.map { url ->
                                if (url.startsWith("//")) "https:$url" else url
                            }.map { it.replace("t_thumb", "t_720p") }
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

                    val headerCorners = 12.dp
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
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                                .width(140.dp)
                                .aspectRatio(0.7f)
                                .clip(getAppCorners(12.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), getAppCorners(12.dp)),
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
        // Info Card
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
                val userFormatted = game.userRating?.takeIf { it > 0 }?.let { "User: %.1f / 100".format(it) }
                val criticFormatted = game.criticRating?.takeIf { it > 0 }?.let { "Critic: %.1f / 100".format(it) }
                val ratingsDisplay = when {
                    userFormatted != null && criticFormatted != null -> "$userFormatted | $criticFormatted"
                    userFormatted != null -> userFormatted
                    criticFormatted != null -> criticFormatted
                    else -> "(None)"
                }

                InfoRow(icon = Icons.Rounded.Event, label = "Released", value = DateUtils.formatDisplayDate(game.releaseDate).ifEmpty { "None" }, color = accentColor)
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(
                    icon = Icons.Rounded.Star, 
                    label = "Ratings", 
                    value = ratingsDisplay, 
                    color = accentColor
                )
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
    val context = LocalContext.current

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
            DetailRow(
                icon = Icons.Rounded.ShoppingBag,
                label = "Purchased on",
                value = if (game.purchaseDate.isNotBlank()) DateUtils.formatDisplayDate(game.purchaseDate).ifEmpty { game.purchaseDate } else "(None)",
                color = accentColor,
                onClick = null
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            DetailRow(
                icon = Icons.Rounded.AttachMoney,
                label = "Paid",
                value = game.pricePaid.ifBlank { "(None)" },
                color = accentColor,
                onClick = null
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
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
    var isFullscreen by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(if (isFullscreen) 0.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = if (isFullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFullscreen) {
                                Modifier
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            } else {
                                Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            }
                        ),
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

                    Spacer(modifier = Modifier.width(6.dp))

                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.ic_close),
                        onClick = onDismissRequest,
                        color = accentColor,
                        size = 36.dp
                    )
                }

                if (!isFullscreen) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val context = LocalContext.current
                val webView = remember(videoId) {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        webChromeClient = object : WebChromeClient() {
                            override fun getDefaultVideoPoster(): Bitmap {
                                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                super.onShowCustomView(view, callback)
                                customView = view
                                customViewCallback = callback
                            }

                            override fun onHideCustomView() {
                                super.onHideCustomView()
                                try {
                                    customViewCallback?.onCustomViewHidden()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                customView = null
                                customViewCallback = null
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
                        }

                        val htmlData = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                              * { margin: 0; padding: 0; box-sizing: border-box; }
                              html, body { width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                              .video-container { position: relative; width: 100%; height: 100%; }
                              iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
                            </style>
                            </head>
                            <body>
                              <div class="video-container">
                                <iframe id="player"
                                        src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&controls=1&enablejsapi=1&origin=https://www.youtube.com&widget_referrer=https://www.youtube.com"
                                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                        allowfullscreen></iframe>
                              </div>
                            </body>
                            </html>
                        """.trimIndent()

                        loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null)
                    }
                }

                NeonCard(
                    modifier = if (isFullscreen) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    color = accentColor,
                    padding = 0.dp
                ) {
                    AndroidView(
                        factory = { webView },
                        update = { view ->
                            view.requestLayout()
                            view.invalidate()
                        },
                        onRelease = { view ->
                            view.stopLoading()
                            view.loadUrl("about:blank")
                            view.destroy()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isFullscreen) {
                    Spacer(modifier = Modifier.height(12.dp))

                    val context = LocalContext.current
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
