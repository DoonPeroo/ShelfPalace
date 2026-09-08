package com.example.retrovault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.retrovault.R
import com.example.retrovault.data.GameRepository
import com.example.retrovault.data.SettingsRepository
import com.example.retrovault.data.StaticData
import com.example.retrovault.ui.components.*
import com.example.retrovault.ui.theme.RetroVaultTheme
import com.example.retrovault.util.StorageUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    gameRepository: GameRepository,
    movieRepository: com.example.retrovault.data.MovieRepository,
    musicRepository: com.example.retrovault.data.MusicRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val disabledIds by repository.disabledIds.collectAsState(initial = emptySet())
    val cornerStyle by repository.cornerStyle.collectAsState(initial = com.example.retrovault.data.CornerStyle.ROUNDED)
    var expandedManufacturerId by remember { mutableStateOf<String?>(null) }
    var isGamesExpanded by remember { mutableStateOf(false) }
    var isVideoExpanded by remember { mutableStateOf(false) }
    var isMusicExpanded by remember { mutableStateOf(false) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBackupOptions by remember { mutableStateOf(false) }
    var showRestoreReplaceConfirm by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    
    var backupIncludeImages by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isBackingUp = true
            scope.launch {
                try {
                    val games = gameRepository.getAllGames().first()
                    val movies = movieRepository.getAllMovies().first()
                    val music = musicRepository.getAllMusic().first()
                    
                    val backupData = com.example.retrovault.data.BackupData(
                        games = games,
                        movies = movies,
                        music = music
                    )
                    
                    val zipData = StorageUtil.createBackupZip(context, backupData, backupIncludeImages)
                    if (zipData != null) {
                        val success = StorageUtil.writeToUri(context, uri, zipData)
                        if (success) {
                            android.widget.Toast.makeText(context, "Backup successful", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Backup failed", android.widget.Toast.LENGTH_SHORT).show()
                } finally {
                    isBackingUp = false
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val currentGames = gameRepository.getAllGames().first()
                val currentMovies = movieRepository.getAllMovies().first()
                val currentMusic = musicRepository.getAllMusic().first()
                
                if (currentGames.isNotEmpty() || currentMovies.isNotEmpty() || currentMusic.isNotEmpty()) {
                    pendingRestoreUri = uri
                    showRestoreReplaceConfirm = true
                } else {
                    performRestore(uri, context, gameRepository, movieRepository, musicRepository) { isRestoring = it }
                }
            }
        }
    }

    val lavender = com.example.retrovault.ui.theme.SynthwaveLavender

    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    NeonHeader(
                        text = stringResource(R.string.action_settings),
                        fullWidth = false,
                        color = accentColor
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp), color = accentColor)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CompactSectionHeader(text = "Appearance", color = accentColor)
                NeonCard(
                    color = accentColor,
                    modifier = Modifier.fillMaxWidth(),
                    padding = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Corner Shape",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        NeonToggle(
                            options = listOf("ROUND", "SQUARE"),
                            selectedOption = if (cornerStyle == com.example.retrovault.data.CornerStyle.ROUNDED) "ROUND" else "SQUARE",
                            onOptionSelected = { option ->
                                scope.launch {
                                    repository.setCornerStyle(
                                        if (option == "ROUND") com.example.retrovault.data.CornerStyle.ROUNDED 
                                        else com.example.retrovault.data.CornerStyle.SQUARE
                                    )
                                }
                            },
                            modifier = Modifier.width(130.dp),
                            height = 28.dp,
                            color = accentColor
                        )
                    }
                }
            }

            item {
                CompactSectionHeader(text = "Game Library Filters", color = accentColor)
                NeonCard(
                    color = accentColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Global Games Toggle
                        val isGamesEnabled = !disabledIds.contains("media_games")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isGamesExpanded = !isGamesExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isGamesExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = if (isGamesEnabled) accentColor else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.header_games),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isGamesEnabled) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            NeonSwitch(
                                checked = isGamesEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { repository.toggleVisibility("media_games", checked) }
                                },
                                scale = 0.85f
                            )
                        }

                        AnimatedVisibility(
                            visible = isGamesExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(start = 24.dp, bottom = 4.dp)
                                    .fillMaxWidth()
                            ) {
                                StaticData.manufacturers.forEachIndexed { index, manufacturer ->
                                    val isEnabled = !disabledIds.contains(manufacturer.id)
                                    val isExpanded = expandedManufacturerId == manufacturer.id

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    expandedManufacturerId = if (isExpanded) null else manufacturer.id 
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                                                    contentDescription = null,
                                                    tint = if (isEnabled && isGamesEnabled) accentColor else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = manufacturer.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (isEnabled && isGamesEnabled) Color.White else Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            NeonSwitch(
                                                checked = isEnabled,
                                                onCheckedChange = { checked ->
                                                    scope.launch { repository.toggleVisibility(manufacturer.id, checked) }
                                                },
                                                enabled = isGamesEnabled,
                                                scale = 0.85f
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically(),
                                            exit = shrinkVertically()
                                        ) {
                                            val platforms = StaticData.platforms.filter { it.manufacturerId == manufacturer.id }
                                            Column(
                                                modifier = Modifier
                                                    .padding(start = 24.dp, bottom = 4.dp)
                                                    .fillMaxWidth()
                                            ) {
                                                platforms.forEach { platform ->
                                                    val isPlatformEnabled = !disabledIds.contains(platform.id)
                                                    SettingsToggleRow(
                                                        label = platform.name,
                                                        isEnabled = isPlatformEnabled,
                                                        onToggle = { checked ->
                                                            scope.launch { repository.toggleVisibility(platform.id, checked) }
                                                        },
                                                        enabled = isEnabled && isGamesEnabled
                                                    )
                                                }
                                            }
                                        }
                                        
                                        if (index < StaticData.manufacturers.size - 1) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                CompactSectionHeader(text = "Media Library Filters", color = lavender)
                NeonCard(
                    color = lavender,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Video Group
                        val isMoviesEnabled = !disabledIds.contains("media_movies")
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isVideoExpanded = !isVideoExpanded }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (isVideoExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isMoviesEnabled) accentColor else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.header_movies),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isMoviesEnabled) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                NeonSwitch(
                                    checked = isMoviesEnabled,
                                    onCheckedChange = { checked ->
                                        scope.launch { repository.toggleVisibility("media_movies", checked) }
                                    },
                                    scale = 0.85f
                                )
                            }

                            AnimatedVisibility(
                                visible = isVideoExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 24.dp, bottom = 8.dp)
                                        .fillMaxWidth()
                                ) {
                                    StaticData.movieFormats.forEach { format ->
                                        val isEnabled = !disabledIds.contains(format.id)
                                        SettingsToggleRow(
                                            label = format.name,
                                            isEnabled = isEnabled,
                                            onToggle = { checked ->
                                                scope.launch { repository.toggleVisibility(format.id, checked) }
                                            },
                                            enabled = isMoviesEnabled
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                        // Music Group
                        val isMusicEnabled = !disabledIds.contains("media_music")
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMusicExpanded = !isMusicExpanded }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (isMusicExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isMusicEnabled) accentColor else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.header_music),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isMusicEnabled) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                NeonSwitch(
                                    checked = isMusicEnabled,
                                    onCheckedChange = { checked ->
                                        scope.launch { repository.toggleVisibility("media_music", checked) }
                                    },
                                    scale = 0.85f
                                )
                            }

                            AnimatedVisibility(
                                visible = isMusicExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 24.dp, bottom = 8.dp)
                                        .fillMaxWidth()
                                ) {
                                    StaticData.musicFormats.forEach { format ->
                                        val isEnabled = !disabledIds.contains(format.id)
                                        SettingsToggleRow(
                                            label = format.name,
                                            isEnabled = isEnabled,
                                            onToggle = { checked ->
                                                scope.launch { repository.toggleVisibility(format.id, checked) }
                                            },
                                            enabled = isMusicEnabled
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                CompactSectionHeader(text = "More Library Filters", color = lavender)
                NeonCard(
                    color = lavender,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isMoreEnabled = !disabledIds.contains("media_more")
                    SettingsToggleRow(
                        label = "Enable More Category",
                        isEnabled = isMoreEnabled,
                        onToggle = { checked ->
                            scope.launch { repository.toggleVisibility("media_more", checked) }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                CompactSectionHeader(text = "Data Management", color = accentColor)
                NeonCard(
                    color = accentColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NeonButton(
                                text = if (isBackingUp) "..." else "Backup",
                                icon = Icons.Rounded.CloudUpload,
                                onClick = { showBackupOptions = true },
                                modifier = Modifier.weight(1f),
                                color = accentColor,
                                height = 40.dp
                            )

                            NeonButton(
                                text = if (isRestoring) "..." else "Restore",
                                icon = Icons.Rounded.CloudDownload,
                                onClick = {
                                    if (!isRestoring) {
                                        filePickerLauncher.launch("application/zip")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                color = accentColor,
                                height = 40.dp
                            )
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                        NeonButton(
                            text = "Delete All Data",
                            iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_trash),
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Red,
                            height = 40.dp
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ver. ALPHA 0.75",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        NeonAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = {
                scope.launch {
                    gameRepository.clearAllGames()
                    movieRepository.clearAllMovies()
                    musicRepository.clearAllMusic()
                    android.widget.Toast.makeText(context, "All data deleted", android.widget.Toast.LENGTH_SHORT).show()
                }
                showDeleteConfirm = false
            },
            title = "DELETE ALL DATA?",
            text = "This will permanently remove all games, movies, and music from your library. This action cannot be undone.",
            confirmText = "DELETE",
            color = MaterialTheme.colorScheme.primary,
            confirmColor = Color.Red
        )
    }

    if (showRestoreReplaceConfirm) {
        NeonAlertDialog(
            onDismissRequest = { 
                showRestoreReplaceConfirm = false
                pendingRestoreUri = null
            },
            onConfirm = {
                pendingRestoreUri?.let { uri ->
                    scope.launch {
                        gameRepository.clearAllGames()
                        movieRepository.clearAllMovies()
                        musicRepository.clearAllMusic()
                        performRestore(uri, context, gameRepository, movieRepository, musicRepository) { isRestoring = it }
                    }
                }
                showRestoreReplaceConfirm = false
                pendingRestoreUri = null
            },
            title = "REPLACE DATABASE?",
            text = "Your current library is not empty. Do you want to DELETE everything and replace it with the backup? This cannot be undone.",
            confirmText = "REPLACE",
            color = accentColor,
            confirmColor = Color.Red
        )
    }

    if (showBackupOptions) {
        Dialog(onDismissRequest = { showBackupOptions = false }) {
            NeonCard(
                color = accentColor,
                containerAlpha = 0.9f,
                modifier = Modifier.width(320.dp).wrapContentHeight()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "BACKUP OPTIONS",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    
                    Text(
                        text = "Select what you want to include in the backup.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    NeonButton(
                        text = "JSON ONLY (Text)",
                        onClick = {
                            backupIncludeImages = false
                            showBackupOptions = false
                            createDocumentLauncher.launch("retrovault_backup_${System.currentTimeMillis()}.zip")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        height = 48.dp
                    )

                    NeonButton(
                        text = "JSON + COVER ART",
                        onClick = {
                            backupIncludeImages = true
                            showBackupOptions = false
                            createDocumentLauncher.launch("retrovault_full_backup_${System.currentTimeMillis()}.zip")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = accentColor,
                        height = 48.dp
                    )
                }
            }
        }
    }
}

private suspend fun performRestore(
    uri: android.net.Uri,
    context: android.content.Context,
    gameRepository: GameRepository,
    movieRepository: com.example.retrovault.data.MovieRepository,
    musicRepository: com.example.retrovault.data.MusicRepository,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val jsonString = StorageUtil.restoreFromBackupZip(context, uri)
        if (jsonString != null) {
            val json = Json { ignoreUnknownKeys = true }
            val backupData = try {
                json.decodeFromString<com.example.retrovault.data.BackupData>(jsonString)
            } catch (e: Exception) {
                // Fallback for legacy backups
                val games = json.decodeFromString<List<com.example.retrovault.data.Game>>(jsonString)
                com.example.retrovault.data.BackupData(games = games)
            }
            
            gameRepository.insertGames(backupData.games)
            movieRepository.insertMovies(backupData.movies)
            musicRepository.insertMusicList(backupData.music)
            
            val total = backupData.games.size + backupData.movies.size + backupData.music.size
            android.widget.Toast.makeText(context, "Database restored: $total items added", android.widget.Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Restore failed: Invalid backup file", android.widget.Toast.LENGTH_LONG).show()
    } finally {
        setLoading(false)
    }
}



@Composable
fun SettingsToggleRow(
    label: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled && isEnabled) Color.White else Color.Gray
        )
        NeonSwitch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = enabled,
            scale = 0.8f
        )
    }
}

@Composable
fun NeonSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scale: Float = 1f
) {
    val trackWidth = 52.dp
    val trackHeight = 26.dp
    val thumbSize = 18.dp
    val gap = 4.dp
    
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - gap else gap,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )
    
    // We use the secondary color for the active state to match the settings theme
    val activeColor = MaterialTheme.colorScheme.secondary
    val trackColor = if (checked) activeColor.copy(alpha = 0.3f) else Color.DarkGray.copy(alpha = 0.5f)
    val thumbColor = if (checked) activeColor else Color.Gray

    // Ensure we use a larger radius for the track to make it clearly square/round
    val trackRadius = 12.dp 
    val thumbRadius = 6.dp

    Box(
        modifier = modifier
            .scale(scale)
            .size(trackWidth, trackHeight)
            .background(if (enabled) trackColor else trackColor.copy(alpha = 0.2f), getAppCorners(trackRadius))
            .border(1.5.dp, if (enabled) thumbColor.copy(alpha = 0.5f) else Color.Transparent, getAppCorners(trackRadius))
            .clickable(enabled = enabled && onCheckedChange != null) { 
                onCheckedChange?.invoke(!checked) 
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(thumbOffset.roundToPx(), 0) }
                .size(thumbSize)
                .background(if (enabled) thumbColor else thumbColor.copy(alpha = 0.3f), getAppCorners(thumbRadius))
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    val database = remember { com.example.retrovault.data.local.RetroVaultDatabase.getDatabase(context) }
    val repository = remember { GameRepository(database.gameDao()) }
    val movieRepository = remember { com.example.retrovault.data.MovieRepository(database.movieDao()) }
    val musicRepository = remember { com.example.retrovault.data.MusicRepository(database.musicDao()) }
    val settingsRepository = remember { SettingsRepository(context) }
    
    RetroVaultTheme {
        Box(modifier = Modifier.background(com.example.retrovault.ui.theme.DarkBackground)) {
            SettingsScreen(
                repository = settingsRepository,
                gameRepository = repository,
                movieRepository = movieRepository,
                musicRepository = musicRepository,
                onBack = {}
            )
        }
    }
}
