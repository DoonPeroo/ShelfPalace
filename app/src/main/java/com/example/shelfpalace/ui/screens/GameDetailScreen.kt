package com.example.shelfpalace.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.remote.IgdbService
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.theme.DarkBackground
import com.example.shelfpalace.util.DateUtils
import com.example.shelfpalace.util.PlatformUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelfpalace.data.Game
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
    val scope = rememberCoroutineScope()
    val accentColor = MaterialTheme.colorScheme.primary
    val deepPurple = Color(0xFF9C27B0)
    
    var screenshots by remember { mutableStateOf<List<String>>(emptyList()) }
    var isMediaLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(game) {
        val currentGame = game ?: return@LaunchedEffect
        if (screenshots.isNotEmpty()) return@LaunchedEffect
        
        isMediaLoading = true
        try {
            var targetIgdbId = currentGame.igdbId
            
            // If ID is missing, try to find it by title
            if (targetIgdbId == null) {
                val searchResults = IgdbService.search(currentGame.title, currentGame.platformId)
                targetIgdbId = searchResults.firstOrNull { 
                    it.name?.equals(currentGame.title, ignoreCase = true) == true 
                }?.id ?: searchResults.firstOrNull()?.id
            }
            
            targetIgdbId?.let { id ->
                val igdbGame = IgdbService.getGameById(id)
                igdbGame?.screenshots?.let { list ->
                    screenshots = list.mapNotNull { it.url }.map { url ->
                        if (url.startsWith("//")) "https:$url" else url
                    }.map { it.replace("t_thumb", "t_720p") }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isMediaLoading = false
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
                            horizontalAlignment = Alignment.CenterHorizontally
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
                                isLoading = isMediaLoading, 
                                onImageClick = { selectedImageIndex = it }
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
                InfoRow(icon = Icons.Rounded.Event, label = "Released", value = DateUtils.formatDisplayDate(game.releaseDate).ifEmpty { "None" }, color = accentColor)
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
    isLoading: Boolean, 
    onImageClick: (Int) -> Unit
) {
    val isDualScreen = platformId == "nintendo_ds" || platformId == "nintendo_3ds"
    
    Column {
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
