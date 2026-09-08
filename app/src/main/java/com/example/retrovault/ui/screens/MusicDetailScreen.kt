package com.example.retrovault.ui.screens

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.retrovault.R
import com.example.retrovault.data.Music
import com.example.retrovault.data.MusicRepository
import com.example.retrovault.ui.components.*
import com.example.retrovault.ui.theme.DarkBackground
import com.example.retrovault.util.DateUtils
import com.example.retrovault.util.StorageUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicDetailScreen(
    musicId: String,
    repository: MusicRepository,
    onEditMusic: (String) -> Unit,
    onFormatClick: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val music by repository.getMusicStream(musicId).collectAsState(initial = null)
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val accentColor = MaterialTheme.colorScheme.primary
    
    val screenshots = emptyList<String>()
    val isMediaLoading = false
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Info", "Media", "My Details")

    var statusExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("Plan to listen", "Listening", "Listened", "Favorite")

    var versionExpanded by remember { mutableStateOf(false) }
    val versionOptions = listOf("Physical", "Digital")

    var showNotesDialog by remember { mutableStateOf(false) }
    var editingNotes by remember { mutableStateOf("") }

    music?.let { currentMusic ->
        LaunchedEffect(currentMusic.notes) {
            editingNotes = currentMusic.notes
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val headerCorners = 12.dp
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .background(Color.Black.copy(alpha = 0.3f), getAppCorners(headerCorners))
                                .border(1.dp, accentColor.copy(alpha = 0.8f), getAppCorners(headerCorners))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "MUSIC",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 8.sp,
                                        color = accentColor.copy(alpha = 0.7f)
                                    )
                                )
                                Text(
                                    "DETAILS",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        fontSize = 14.sp,
                                        color = accentColor
                                    )
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        NeonIconButton(
                            iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_back),
                            onClick = onBack,
                            color = accentColor,
                            size = 44.dp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeonIconButton(
                                iconPainter = androidx.compose.ui.res.painterResource(
                                    id = if (currentMusic.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                                ),
                                onClick = { 
                                    scope.launch { repository.toggleFavorite(currentMusic.id) }
                                },
                                color = if (currentMusic.isFavorite) Color(0xFFAD1457) else accentColor,
                                size = 40.dp
                            )

                            NeonIconButton(
                                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_edit),
                                onClick = { onEditMusic(currentMusic.id) },
                                color = accentColor,
                                size = 40.dp
                            )

                            NeonIconButton(
                                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_trash),
                                onClick = { showDeleteConfirmation = true },
                                color = accentColor,
                                size = 40.dp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = currentMusic.coverUri.ifEmpty { "https://via.placeholder.com/400x400?text=${currentMusic.title}" },
                        contentDescription = currentMusic.title,
                        modifier = Modifier
                            .width(200.dp)
                            .aspectRatio(1f)
                            .clip(getAppCorners(12.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), getAppCorners(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currentMusic.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tabs
                DetailTabSelector(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    when (selectedTabIndex) {
                        0 -> MusicInfoTab(currentMusic, accentColor, onFormatClick)
                        1 -> MusicMediaTab(accentColor, screenshots, isMediaLoading, onImageClick = { selectedImageIndex = it })
                        2 -> MusicDetailsTab(
                            music = currentMusic, 
                            accentColor = accentColor,
                            statusExpanded = statusExpanded,
                            onStatusClick = { statusExpanded = true },
                            onStatusDismiss = { statusExpanded = false },
                            onStatusSelect = { newStatus ->
                                scope.launch { repository.updateMusic(currentMusic.copy(status = newStatus)) }
                            },
                            statusOptions = statusOptions,
                            versionExpanded = versionExpanded,
                            onVersionClick = { versionExpanded = true },
                            onVersionDismiss = { versionExpanded = false },
                            onVersionSelect = { newVersion ->
                                scope.launch { repository.updateMusic(currentMusic.copy(version = newVersion)) }
                            },
                            versionOptions = versionOptions,
                            onNotesClick = {
                                editingNotes = currentMusic.notes
                                showNotesDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
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
                music?.let { currentMusic ->
                    scope.launch { repository.updateMusic(currentMusic.copy(notes = newNotes)) }
                }
                showNotesDialog = false
            },
            accentColor = accentColor
        )
    }

    if (showDeleteConfirmation) {
        NeonAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            onConfirm = {
                music?.let { currentMusic ->
                    scope.launch {
                        if (currentMusic.coverUri.isNotEmpty()) {
                            StorageUtil.deleteImage(context, currentMusic.coverUri)
                        }
                        repository.deleteMusic(currentMusic)
                        onBack()
                    }
                }
                showDeleteConfirmation = false
            },
            title = stringResource(R.string.msg_delete_music_confirmation_title),
            text = stringResource(R.string.msg_delete_music_confirmation_text),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            color = accentColor,
            confirmColor = Color.Red
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
fun MusicInfoTab(
    music: Music,
    accentColor: Color,
    onFormatClick: (String) -> Unit
) {
    val (formatLabel, formatIcon) = when(music.formatId) {
        "cassette" -> "Music Cassette" to Icons.Rounded.SettingsBackupRestore
        "cd" -> "Music CD" to Icons.Rounded.Album
        "vinyl" -> "Vinyl" to Icons.Rounded.SettingsInputComponent
        else -> "Unknown" to Icons.Rounded.MusicNote
    }

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
                    onClick = { onFormatClick(music.formatId) }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Rounded.Person, label = "Artist", value = music.artist.ifEmpty { "None" }, color = accentColor)
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Rounded.Category, label = "Genre", value = music.genre.ifEmpty { "None" }, color = accentColor)
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Rounded.Event, label = "Released", value = DateUtils.formatDisplayDate(music.releaseDate).ifEmpty { "None" }, color = accentColor)
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Rounded.Business, label = "Label", value = music.label.ifEmpty { "None" }, color = accentColor)
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
                    text = music.description.ifEmpty { "No description available." },
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f), lineHeight = 20.sp)
                )
            }
        }
    }
}

@Composable
fun MusicMediaTab(accentColor: Color, screenshots: List<String>, isLoading: Boolean, onImageClick: (Int) -> Unit) {
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
                    Text("No screenshots available", color = Color.White.copy(alpha = 0.4f))
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
fun MusicDetailsTab(
    music: Music,
    accentColor: Color,
    statusExpanded: Boolean,
    onStatusClick: () -> Unit,
    onStatusDismiss: () -> Unit,
    onStatusSelect: (String) -> Unit,
    statusOptions: List<String>,
    versionExpanded: Boolean,
    onVersionClick: () -> Unit,
    onVersionDismiss: () -> Unit,
    onVersionSelect: (String) -> Unit,
    versionOptions: List<String>,
    onNotesClick: () -> Unit
) {
    val statusColor = when (music.status.uppercase()) {
        "LISTENING" -> Color(0xFF00E5FF)
        "LISTENED" -> Color(0xFFFFD600)
        "PLAN TO LISTEN" -> Color(0xFFBF8AC7)
        "FAVORITE" -> Color(0xFFFFD600)
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
                    icon = Icons.Rounded.MusicNote,
                    label = "Status",
                    value = music.status,
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
                                "LISTENING" -> Color(0xFF00E5FF)
                                "LISTENED" -> Color(0xFFFFD600)
                                "PLAN TO LISTEN" -> Color(0xFFBF8AC7)
                                "FAVORITE" -> Color(0xFFFFD600)
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
            Box {
                DetailRow(
                    icon = Icons.Rounded.DiscFull,
                    label = "Version",
                    value = music.version,
                    color = accentColor,
                    onClick = onVersionClick
                )
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = DarkBackground)) {
                    DropdownMenu(
                        expanded = versionExpanded,
                        onDismissRequest = { onVersionDismiss() },
                        modifier = Modifier
                            .background(DarkBackground)
                            .border(1.dp, accentColor.copy(alpha = 0.5f), getAppCorners(8.dp))
                    ) {
                        versionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.White) },
                                onClick = { onVersionSelect(option); onVersionDismiss() }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            DetailRow(
                icon = Icons.Rounded.CalendarMonth,
                label = "Added on",
                value = DateUtils.formatTimestamp(music.dateAdded),
                color = accentColor
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            DetailRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                label = "Notes",
                value = music.notes.ifEmpty { "Tap to add notes..." },
                color = accentColor,
                valueColor = if (music.notes.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
                onClick = onNotesClick
            )
        }
    }
}
