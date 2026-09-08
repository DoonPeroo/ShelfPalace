package com.example.retrovault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.retrovault.R
import com.example.retrovault.data.Game
import com.example.retrovault.data.GameRepository
import com.example.retrovault.ui.components.*
import com.example.retrovault.ui.theme.SynthwaveCyan
import com.example.retrovault.ui.theme.SynthwaveDark
import com.example.retrovault.util.DateUtils
import com.example.retrovault.util.StorageUtil
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.example.retrovault.util.StorageUtil.cropBitmap
import com.example.retrovault.util.StorageUtil.loadBitmapFromUri
import com.example.retrovault.util.StorageUtil.saveBitmapToRetroVaultDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

private val GENRES = listOf(
    "Action",
    "Adventure",
    "RPG",
    "Platformer",
    "Shooter",
    "Sports",
    "Racing",
    "Fighting",
    "Puzzle",
    "Strategy",
    "Simulation",
    "Horror",
    "Music",
    "Beat 'em up",
    "Stealth",
    "Survival",
    "Visual Novel",
    "Arcade",
    "Party",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditGameScreen(
    platformId: String?,
    gameId: String?,
    igdbId: Long? = null,
    repository: GameRepository,
    onSave: () -> Unit,
    onIgdbSearch: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    
    var currentPlatformId by rememberSaveable { mutableStateOf(platformId ?: "") }
    var title by rememberSaveable { mutableStateOf("") }
    var releaseDate by rememberSaveable { mutableStateOf("") }
    var displayDate by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var genreExpanded by rememberSaveable { mutableStateOf(false) }
    var developer by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var coverUri by rememberSaveable { mutableStateOf("") }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var userRating by rememberSaveable { mutableStateOf<Double?>(null) }
    var criticRating by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentIgdbId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateAdded by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var tempImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var bitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var shouldLaunchCamera by rememberSaveable { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    var selectedDay by rememberSaveable { mutableStateOf("") }
    var selectedMonth by rememberSaveable { mutableStateOf("") }
    var selectedYear by rememberSaveable { mutableStateOf("") }
    var dayExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val isEditMode = gameId != null

    LaunchedEffect(igdbId) {
        if (igdbId != null) {
            currentIgdbId = igdbId
            val igdbGame = com.example.retrovault.data.remote.IgdbService.getGameById(igdbId)
            igdbGame?.let {
                title = it.name ?: ""
                description = it.summary ?: ""
                genre = it.genres?.firstOrNull()?.name ?: ""
                developer = it.involvedCompanies?.firstOrNull { it.company?.name?.isNotEmpty() == true }?.company?.name ?: ""
                userRating = it.rating
                criticRating = it.aggregatedRating
                
                it.firstReleaseDate?.let { timestamp ->
                    val date = java.util.Date(timestamp * 1000)
                    val cal = java.util.Calendar.getInstance().apply { time = date }
                    selectedYear = cal.get(java.util.Calendar.YEAR).toString()
                    selectedMonth = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
                    selectedDay = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                    releaseDate = "$selectedYear-$selectedMonth-$selectedDay"
                    displayDate = DateUtils.formatDisplayDate(releaseDate)
                }
                
                it.cover?.url?.let { url ->
                    val highResUrl = if (url.startsWith("//")) "https:$url" else url
                    coverUri = highResUrl.replace("t_thumb", "t_cover_big")
                }
            }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val savedUri = withContext(Dispatchers.IO) {
                    StorageUtil.saveImageToRetroVaultDir(context, uri)
                }
                if (savedUri != null) {
                    coverUri = savedUri.toString()
                }
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success) {
            tempImageUriString?.let { uriString ->
                scope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        loadBitmapFromUri(context, android.net.Uri.parse(uriString))
                    }
                    if (bitmap != null) {
                        bitmapToCrop = bitmap
                        showCropDialog = true
                    }
                }
            }
        }
    }

    LaunchedEffect(gameId) {
        if (isEditMode) {
            val game = repository.getGameById(gameId)
            game?.let {
                currentPlatformId = it.platformId
                title = it.title
                releaseDate = it.releaseDate
                genre = it.genre
                developer = it.developer
                description = it.description
                coverUri = it.coverUri
                isFavorite = it.isFavorite
                userRating = it.userRating
                criticRating = it.criticRating
                currentIgdbId = it.igdbId
                dateAdded = it.dateAdded
                
                if (it.releaseDate.isNotEmpty()) {
                    displayDate = DateUtils.formatDisplayDate(it.releaseDate)
                    val parts = it.releaseDate.split("-")
                    if (parts.size == 3) {
                        selectedYear = parts[0]
                        selectedMonth = parts[1]
                        selectedDay = parts[2]
                    }
                }
            }
        }
    }

    LaunchedEffect(cameraPermissionState.status, shouldLaunchCamera) {
        if (cameraPermissionState.status.isGranted && shouldLaunchCamera) {
            shouldLaunchCamera = false // Consume the request immediately
            val uri = StorageUtil.createImageUri(context)
            if (uri != null) {
                tempImageUriString = uri.toString()
                try {
                    takePicture.launch(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val days = remember { (1..31).map { it.toString().padStart(2, '0') } }
    val months = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val years = remember { 
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        (currentYear downTo 1950).map { it.toString() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    NeonHeader(
                        text = stringResource(if (isEditMode) R.string.header_edit_game else R.string.header_add_game),
                        fullWidth = false
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    NeonButton(
                        text = if (isEditMode) "UPDATE DATA VIA IGDB" else "IMPORT FROM IGDB",
                        icon = Icons.Rounded.Language,
                        onClick = { onIgdbSearch(title) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        height = 40.dp
                    )
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.label_title)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors()
                    )
                    
                    Text(
                        text = stringResource(R.string.label_release_date).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Day Dropdown
                        ExposedDropdownMenuBox(
                            expanded = dayExpanded,
                            onExpandedChange = { dayExpanded = !dayExpanded },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            OutlinedTextField(
                                value = selectedDay,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("DD", fontSize = 12.sp) },
                                colors = synthwaveTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = getAppCorners(8.dp),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = dayExpanded,
                                onDismissRequest = { dayExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                days.forEach { dayOption ->
                                    DropdownMenuItem(
                                        text = { Text(text = dayOption, color = Color.White) },
                                        onClick = {
                                            selectedDay = dayOption
                                            dayExpanded = false
                                            if (selectedDay.isNotEmpty() && selectedMonth.isNotEmpty() && selectedYear.isNotEmpty()) {
                                                releaseDate = "$selectedYear-$selectedMonth-$selectedDay"
                                                displayDate = DateUtils.formatDisplayDate(releaseDate)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Month Dropdown
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = !monthExpanded },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            OutlinedTextField(
                                value = selectedMonth,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("MM", fontSize = 12.sp) },
                                colors = synthwaveTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = getAppCorners(8.dp),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                months.forEach { monthOption ->
                                    DropdownMenuItem(
                                        text = { Text(text = monthOption, color = Color.White) },
                                        onClick = {
                                            selectedMonth = monthOption
                                            monthExpanded = false
                                            if (selectedDay.isNotEmpty() && selectedMonth.isNotEmpty() && selectedYear.isNotEmpty()) {
                                                releaseDate = "$selectedYear-$selectedMonth-$selectedDay"
                                                displayDate = DateUtils.formatDisplayDate(releaseDate)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Year Dropdown
                        ExposedDropdownMenuBox(
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = !yearExpanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = selectedYear,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("YYYY", fontSize = 12.sp) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded)
                                },
                                colors = synthwaveTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = getAppCorners(8.dp),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                years.forEach { yearOption ->
                                    DropdownMenuItem(
                                        text = { Text(text = yearOption, color = Color.White) },
                                        onClick = {
                                            selectedYear = yearOption
                                            yearExpanded = false
                                            if (selectedDay.isNotEmpty() && selectedMonth.isNotEmpty() && selectedYear.isNotEmpty()) {
                                                releaseDate = "$selectedYear-$selectedMonth-$selectedDay"
                                                displayDate = DateUtils.formatDisplayDate(releaseDate)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = genreExpanded,
                        onExpandedChange = { genreExpanded = !genreExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = genre,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_genre)) },
                            placeholder = { Text("None") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded)
                            },
                            colors = synthwaveTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = getAppCorners(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = genreExpanded,
                            onDismissRequest = { genreExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(12.dp))
                        ) {
                            GENRES.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = selectionOption,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (genre == selectionOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (genre == selectionOption) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                    },
                                    onClick = {
                                        genre = selectionOption
                                        genreExpanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = Color.White,
                                        trailingIconColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = developer,
                        onValueChange = { developer = it },
                        label = { Text(stringResource(R.string.label_developer)) },
                        placeholder = { Text("None") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors()
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.label_description)) },
                        placeholder = { Text("None") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        minLines = 3,
                        colors = synthwaveTextFieldColors()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonButton(
                            text = stringResource(R.string.action_capture),
                            icon = Icons.Rounded.PhotoCamera,
                            onClick = { 
                                shouldLaunchCamera = true
                                if (!cameraPermissionState.status.isGranted) {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            height = 56.dp
                        )

                        NeonButton(
                            text = stringResource(R.string.action_select),
                            icon = Icons.Rounded.AddAPhoto,
                            onClick = { 
                                pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            height = 56.dp
                        )
                    }
                }
            }
            
            if (coverUri.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeader(
                        text = stringResource(R.string.label_preview),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    NeonButton(
                        text = "ADJUST COVER",
                        icon = Icons.Rounded.Crop,
                        onClick = {
                            scope.launch {
                                val bitmap = withContext(Dispatchers.IO) {
                                    loadBitmapFromUri(context, Uri.parse(coverUri))
                                }
                                if (bitmap != null) {
                                    bitmapToCrop = bitmap
                                    showCropDialog = true
                                }
                            }
                        },
                        height = 32.dp,
                        color = MaterialTheme.colorScheme.primary,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    )
                }

                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = "Cover Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(getAppCorners(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            NeonButton(
                text = stringResource(R.string.action_save_game),
                onClick = {
                    scope.launch {
                        val game = Game(
                            id = gameId ?: UUID.randomUUID().toString(),
                            platformId = currentPlatformId,
                            title = title,
                            releaseDate = releaseDate,
                            genre = genre,
                            developer = developer,
                            description = description,
                            coverUri = coverUri,
                            isFavorite = isFavorite,
                            userRating = userRating,
                            criticRating = criticRating,
                            igdbId = currentIgdbId,
                            dateAdded = dateAdded
                        )
                        if (isEditMode) {
                            repository.updateGame(game)
                        } else {
                            repository.insertGame(game)
                        }
                        onSave()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp
            )
        }
    }

    if (showCropDialog && bitmapToCrop != null) {
        ImageCropDialog(
            bitmap = bitmapToCrop!!,
            onCropConfirmed = { rect ->
                scope.launch {
                    val oldUri = coverUri
                    val cropped = withContext(Dispatchers.IO) {
                        val croppedBitmap = StorageUtil.cropBitmap(bitmapToCrop!!, rect)
                        saveBitmapToRetroVaultDir(context, croppedBitmap, oldUri)
                    }
                    if (cropped != null) {
                        coverUri = cropped.toString()
                    }
                    showCropDialog = false
                    bitmapToCrop = null
                }
            },
            onDismiss = {
                showCropDialog = false
                bitmapToCrop = null
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditGameScreenPreview() {
    val context = LocalContext.current
    val database = remember { com.example.retrovault.data.local.RetroVaultDatabase.getDatabase(context) }
    val repository = remember { com.example.retrovault.data.GameRepository(database.gameDao()) }
    
    com.example.retrovault.ui.theme.RetroVaultTheme {
        Box(modifier = Modifier.background(com.example.retrovault.ui.theme.DarkBackground)) {
            // Note: In real app, the title would be entered by user.
            // This preview is just to see the layout.
            AddEditGameScreen(
                platformId = "sony_ps2",
                gameId = null,
                repository = repository,
                onSave = {},
                onIgdbSearch = {},
                onBack = {},
                onHome = {}
            )
        }
    }
}
