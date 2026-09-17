package com.example.shelfpalace.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.focus.onFocusChanged
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
import com.example.shelfpalace.R
import com.example.shelfpalace.data.Game
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.theme.SynthwaveCyan
import com.example.shelfpalace.ui.theme.SynthwaveDark
import com.example.shelfpalace.util.DateUtils
import com.example.shelfpalace.util.StorageUtil
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.res.painterResource
import com.example.shelfpalace.data.local.ShelfPalaceDatabase
import com.example.shelfpalace.data.remote.IgdbService
import com.example.shelfpalace.ui.theme.DarkBackground
import com.example.shelfpalace.ui.theme.ShelfPalaceTheme
import com.example.shelfpalace.util.StorageUtil.cropBitmap
import com.example.shelfpalace.util.StorageUtil.loadBitmapFromUri
import com.example.shelfpalace.util.StorageUtil.saveBitmapToShelfPalaceDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
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
    onSave: (String) -> Unit,
    onIgdbSearch: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    @Suppress("UNUSED_PARAMETER") onHome: () -> Unit = {}
) {
    val context = LocalContext.current
    
    var currentPlatformId by rememberSaveable { mutableStateOf(platformId ?: "") }
    var title by rememberSaveable { mutableStateOf("") }
    var releaseDate by rememberSaveable { mutableStateOf("") }
    var displayDate by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var developer by rememberSaveable { mutableStateOf("") }
    var publisher by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var coverUri by rememberSaveable { mutableStateOf("") }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var condition by rememberSaveable { mutableStateOf("Sealed") }
    val conditionOptions = listOf("Sealed", "Complete in Box (CiB)", "Incomplete (manual missing)", "Medium only")
    var gameEdition by rememberSaveable { mutableStateOf("Retail") }
    val editionOptions = listOf("Retail", "Digital", "Retail (Collector's Edition)", "Digital (Collector's Edition)")
    var purchaseDate by rememberSaveable { mutableStateOf("") }
    var pricePaid by rememberSaveable { mutableStateOf("") }
    var userRating by rememberSaveable { mutableStateOf<Double?>(null) }
    var criticRating by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentIgdbId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateAdded by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var tempImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var bitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var shouldLaunchCamera by rememberSaveable { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    var selectedDay by rememberSaveable { mutableStateOf("") }
    var selectedMonth by rememberSaveable { mutableStateOf("") }
    var selectedYear by rememberSaveable { mutableStateOf("") }
    
    var selectedPurchaseDay by rememberSaveable { mutableStateOf("") }
    var selectedPurchaseMonth by rememberSaveable { mutableStateOf("") }
    var selectedPurchaseYear by rememberSaveable { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val isEditMode = gameId != null
    var isInitialized by remember { mutableStateOf(false) }
    var existingGame by remember { mutableStateOf<Game?>(null) }

    val saveGame: () -> Unit = {
        scope.launch {
            try {
                if (currentIgdbId == null && title.isNotBlank()) {
                    try {
                        val matches = withContext(Dispatchers.IO) {
                            IgdbService.search(title, currentPlatformId)
                        }
                        val match = matches.firstOrNull()
                        if (match != null) {
                            if (userRating == null) userRating = match.rating
                            if (criticRating == null) criticRating = match.aggregatedRating
                            if (currentIgdbId == null) currentIgdbId = match.id
                            if (developer.isBlank()) developer = match.involvedCompanies?.getOrNull(0)?.company?.name ?: ""
                            if (publisher.isBlank()) publisher = match.involvedCompanies?.getOrNull(1)?.company?.name ?: ""
                            if (genre.isBlank()) genre = match.genres?.firstOrNull()?.name ?: ""
                            if (description.isBlank()) description = match.summary ?: ""
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val targetId = existingGame?.id ?: gameId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                val isCurrentlyEditing = !gameId.isNullOrBlank() || existingGame != null
                val originalDateAdded = existingGame?.dateAdded ?: dateAdded

                val game = (existingGame ?: Game(
                    id = targetId,
                    platformId = currentPlatformId,
                    title = title,
                    coverUri = coverUri,
                    releaseDate = releaseDate,
                    description = description,
                    dateAdded = originalDateAdded
                )).copy(
                    id = targetId,
                    platformId = currentPlatformId,
                    title = title,
                    releaseDate = releaseDate,
                    genre = genre,
                    developer = developer,
                    publisher = publisher,
                    description = description,
                    coverUri = coverUri,
                    isFavorite = isFavorite,
                    condition = condition,
                    gameEdition = gameEdition,
                    userRating = userRating,
                    criticRating = criticRating,
                    igdbId = currentIgdbId,
                    dateAdded = originalDateAdded,
                    status = existingGame?.status ?: "Unplayed",
                    purchaseDate = purchaseDate,
                    pricePaid = pricePaid,
                    notes = existingGame?.notes ?: ""
                )
                withContext(NonCancellable) {
                    if (isCurrentlyEditing) {
                        repository.updateGame(game)
                    } else {
                        repository.insertGame(game)
                    }
                }
                onSave(targetId)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(igdbId) {
        if (igdbId != null) {
            currentIgdbId = igdbId
            val igdbGame = IgdbService.getGameById(igdbId)
            igdbGame?.let {
                title = it.name ?: ""
                description = it.summary ?: ""
                genre = it.genres?.firstOrNull()?.name ?: ""
                developer = it.involvedCompanies?.getOrNull(0)?.company?.name ?: ""
                publisher = it.involvedCompanies?.getOrNull(1)?.company?.name ?: ""
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

    LaunchedEffect(title, currentPlatformId) {
        if (title.length >= 3 && currentIgdbId == null) {
            delay(800L)
            if (title.length >= 3 && currentIgdbId == null) {
                try {
                    val matches = withContext(Dispatchers.IO) {
                        IgdbService.search(title, currentPlatformId)
                    }
                    val match = matches.firstOrNull()
                    if (match != null && currentIgdbId == null) {
                        if (userRating == null) userRating = match.rating
                        if (criticRating == null) criticRating = match.aggregatedRating
                        currentIgdbId = match.id
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val savedUri = withContext(Dispatchers.IO) {
                    StorageUtil.saveImageToShelfPalaceDir(context, uri)
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
                        loadBitmapFromUri(context, Uri.parse(uriString))
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
        if (isEditMode && !isInitialized) {
            val game = repository.getGameById(gameId)
            game?.let {
                existingGame = it
                currentPlatformId = it.platformId
                title = it.title
                releaseDate = it.releaseDate
                genre = it.genre
                developer = it.developer
                publisher = it.publisher
                description = it.description
                coverUri = it.coverUri
                isFavorite = it.isFavorite
                condition = it.condition
                gameEdition = it.gameEdition
                purchaseDate = it.purchaseDate
                pricePaid = it.pricePaid
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
                if (it.purchaseDate.isNotEmpty()) {
                    val parts = it.purchaseDate.split("-")
                    if (parts.size == 3) {
                        selectedPurchaseYear = parts[0]
                        selectedPurchaseMonth = parts[1]
                        selectedPurchaseDay = parts[2]
                    }
                }
                isInitialized = true
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

    if (isEditMode && !isInitialized) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
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
                actions = {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.ic_close),
                        onClick = onClose,
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
                .padding(12.dp)
        ) {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                padding = 12.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    
                    DateDropdownPicker(
                        label = stringResource(R.string.label_release_date),
                        selectedDay = selectedDay,
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        onDateChanged = { d, m, y ->
                            selectedDay = d
                            selectedMonth = m
                            selectedYear = y
                            if (d.isNotEmpty() && m.isNotEmpty() && y.isNotEmpty()) {
                                releaseDate = "$y-$m-$d"
                                displayDate = DateUtils.formatDisplayDate(releaseDate)
                            }
                        }
                    )

                    FormDropdownField(
                        label = stringResource(R.string.label_genre),
                        selectedValue = genre,
                        options = GENRES,
                        onOptionSelected = { genre = it },
                        placeholder = "None"
                    )

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
                        value = publisher,
                        onValueChange = { publisher = it },
                        label = { Text("Publisher") },
                        placeholder = { Text("None") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors()
                    )

                    FormDropdownField(
                        label = "Game Condition",
                        selectedValue = condition,
                        options = conditionOptions,
                        onOptionSelected = { condition = it }
                    )

                    FormDropdownField(
                        label = "Game Edition",
                        selectedValue = gameEdition,
                        options = editionOptions,
                        onOptionSelected = { gameEdition = it }
                    )

                    DateDropdownPicker(
                        label = "PURCHASE DATE",
                        selectedDay = selectedPurchaseDay,
                        selectedMonth = selectedPurchaseMonth,
                        selectedYear = selectedPurchaseYear,
                        onDateChanged = { d, m, y ->
                            selectedPurchaseDay = d
                            selectedPurchaseMonth = m
                            selectedPurchaseYear = y
                            if (d.isNotEmpty() && m.isNotEmpty() && y.isNotEmpty()) {
                                purchaseDate = "$y-$m-$d"
                            } else if (y.isNotEmpty()) {
                                purchaseDate = if (m.isNotEmpty()) "$y-$m" else y
                            } else {
                                purchaseDate = ""
                            }
                        }
                    )

                    OutlinedTextField(
                        value = pricePaid,
                        onValueChange = { pricePaid = it },
                        label = { Text("Purchase Price / Paid") },
                        placeholder = { Text("e.g. 29.99 €") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        singleLine = true,
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
                            iconPainter = painterResource(id = R.drawable.camera),
                            onClick = { 
                                shouldLaunchCamera = true
                                if (!cameraPermissionState.status.isGranted) {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            height = 46.dp
                        )

                        NeonButton(
                            text = stringResource(R.string.action_select),
                            icon = Icons.Rounded.AddAPhoto,
                            onClick = { 
                                pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            height = 46.dp
                        )
                    }
                }
            }
            
            if (coverUri.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
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
                    color = MaterialTheme.colorScheme.primary,
                    padding = 8.dp
                ) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = "Cover Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(getAppCorners(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            NeonButton(
                text = stringResource(R.string.action_save_game),
                onClick = {
                    scope.launch {
                        if (title.isNotBlank() && repository.doesGameExistExcludingId(title, currentPlatformId, gameId)) {
                            showDuplicateDialog = true
                        } else {
                            saveGame()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp
            )
        }
    }

    if (showDuplicateDialog) {
        NeonAlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            onConfirm = {
                showDuplicateDialog = false
                saveGame()
            },
            title = "GAME ALREADY EXISTS",
            text = "A game with this title already exists for this console. Do you still want to add it to your library?",
            confirmText = "ADD ANYWAY",
            dismissText = "CANCEL",
            color = MaterialTheme.colorScheme.primary,
            confirmColor = MaterialTheme.colorScheme.primary
        )
    }

    if (showCropDialog && bitmapToCrop != null) {
        ImageCropDialog(
            bitmap = bitmapToCrop!!,
            onCropConfirmed = { rect ->
                scope.launch {
                    val oldUri = coverUri
                    val cropped = withContext(Dispatchers.IO) {
                        val croppedBitmap = StorageUtil.cropBitmap(bitmapToCrop!!, rect)
                        saveBitmapToShelfPalaceDir(context, croppedBitmap, oldUri)
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
    val database = remember { ShelfPalaceDatabase.getDatabase(context) }
    val repository = remember { GameRepository(database.gameDao()) }

    ShelfPalaceTheme {
        Box(modifier = Modifier.background(DarkBackground)) {
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
