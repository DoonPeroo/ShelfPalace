package com.example.shelfpalace.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shelfpalace.R
import com.example.shelfpalace.data.Music
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.remote.DiscogsService
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.util.DateUtils
import com.example.shelfpalace.util.StorageUtil
import com.example.shelfpalace.util.StorageUtil.cropBitmap
import com.example.shelfpalace.util.StorageUtil.loadBitmapFromUri
import com.example.shelfpalace.util.StorageUtil.saveBitmapToShelfPalaceDir
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditMusicScreen(
    formatId: String?,
    musicId: String?,
    discogsId: Long? = null,
    repository: MusicRepository,
    onSave: (String) -> Unit,
    onDiscogsSearch: (query: String, format: String, label: String, year: String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    @Suppress("UNUSED_PARAMETER") onHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val musicColor = MaterialTheme.colorScheme.secondary
    
    var currentFormatId by rememberSaveable { mutableStateOf(formatId ?: "") }
    var title by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var releaseDate by rememberSaveable { mutableStateOf("") }
    var displayDate by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var coverUri by rememberSaveable { mutableStateOf("") }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var purchaseDate by rememberSaveable { mutableStateOf("") }
    var pricePaid by rememberSaveable { mutableStateOf("") }
    var currentDiscogsId by rememberSaveable { mutableStateOf<Long?>(discogsId) }
    var dateAdded by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var tempImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var bitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var shouldLaunchCamera by rememberSaveable { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var selectedDay by rememberSaveable { mutableStateOf("") }
    var selectedMonth by rememberSaveable { mutableStateOf("") }
    var selectedYear by rememberSaveable { mutableStateOf("") }

    var selectedPurchaseDay by rememberSaveable { mutableStateOf("") }
    var selectedPurchaseMonth by rememberSaveable { mutableStateOf("") }
    var selectedPurchaseYear by rememberSaveable { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val isEditMode = musicId != null
    var isInitialized by remember { mutableStateOf(false) }
    var existingMusic by remember { mutableStateOf<Music?>(null) }

    val saveMusic: () -> Unit = {
        scope.launch {
            try {
                val targetId = existingMusic?.id ?: musicId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                val isCurrentlyEditing = !musicId.isNullOrBlank() || existingMusic != null
                val originalDateAdded = existingMusic?.dateAdded ?: dateAdded

                val music = (existingMusic ?: Music(
                    id = targetId,
                    formatId = currentFormatId,
                    title = title,
                    artist = artist,
                    coverUri = coverUri,
                    releaseDate = releaseDate,
                    description = description,
                    dateAdded = originalDateAdded
                )).copy(
                    id = targetId,
                    formatId = currentFormatId,
                    title = title,
                    artist = artist,
                    releaseDate = releaseDate,
                    genre = genre,
                    label = label,
                    description = description,
                    coverUri = coverUri,
                    isFavorite = isFavorite,
                    dateAdded = originalDateAdded,
                    status = existingMusic?.status ?: "Plan to listen",
                    purchaseDate = purchaseDate,
                    pricePaid = pricePaid,
                    notes = existingMusic?.notes ?: ""
                )
                withContext(NonCancellable) {
                    if (isCurrentlyEditing) {
                        repository.updateMusic(music)
                    } else {
                        repository.insertMusic(music)
                    }
                }
                onSave(targetId)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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

    LaunchedEffect(musicId) {
        if (isEditMode && !isInitialized) {
            val music = repository.getMusicById(musicId)
            music?.let {
                existingMusic = it
                currentFormatId = it.formatId
                title = it.title
                artist = it.artist
                releaseDate = it.releaseDate
                genre = it.genre
                label = it.label
                description = it.description
                coverUri = it.coverUri
                isFavorite = it.isFavorite
                dateAdded = it.dateAdded
                purchaseDate = it.purchaseDate
                pricePaid = it.pricePaid
                
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

    LaunchedEffect(discogsId) {
        if (discogsId != null) {
            currentDiscogsId = discogsId
            val release = withContext(Dispatchers.IO) {
                DiscogsService.getReleaseById(discogsId)
            }
            release?.let {
                val parsedArtist = it.artists?.joinToString(", ") { a ->
                    a.name?.replace(Regex("""\s*\(\d+\)$"""), "") ?: ""
                }?.trim() ?: ""

                if (!it.title.isNullOrBlank()) title = it.title
                if (parsedArtist.isNotBlank()) artist = parsedArtist

                val genresList = (it.genres ?: emptyList()) + (it.styles ?: emptyList())
                if (genresList.isNotEmpty()) {
                    genre = genresList.distinct().joinToString(", ")
                }

                it.labels?.firstOrNull()?.name?.let { l ->
                    if (l.isNotBlank()) label = l
                }

                val yearStr = it.year?.toString() ?: it.released?.take(4) ?: ""
                if (yearStr.isNotBlank()) {
                    selectedYear = yearStr
                    val parts = (it.released ?: "").split("-")
                    if (parts.size >= 2 && parts[1].isNotBlank()) selectedMonth = parts[1].padStart(2, '0')
                    if (parts.size >= 3 && parts[2].isNotBlank()) selectedDay = parts[2].padStart(2, '0')
                    releaseDate = if (selectedMonth.isNotBlank() && selectedDay.isNotBlank()) {
                        "$selectedYear-$selectedMonth-$selectedDay"
                    } else if (selectedMonth.isNotBlank()) {
                        "$selectedYear-$selectedMonth-01"
                    } else {
                        "$selectedYear-01-01"
                    }
                    displayDate = DateUtils.formatDisplayDate(releaseDate)
                }

                if (!it.tracklist.isNullOrEmpty()) {
                    val tracksText = it.tracklist.mapNotNull { track ->
                        val pos = track.position?.takeIf { p -> p.isNotBlank() }?.let { p -> "$p. " } ?: ""
                        val trackTitle = track.title?.takeIf { t -> t.isNotBlank() } ?: return@mapNotNull null
                        val dur = track.duration?.takeIf { d -> d.isNotBlank() }?.let { d -> " ($d)" } ?: ""
                        "$pos$trackTitle$dur"
                    }.joinToString("\n")
                    if (tracksText.isNotBlank()) {
                        description = "Tracklist:\n$tracksText"
                    }
                }

                val primaryUri = it.images?.firstOrNull { img -> img.type == "primary" }?.uri
                    ?: it.images?.firstOrNull()?.uri
                    ?: it.images?.firstOrNull()?.resourceUrl

                var bestCoverUrl = primaryUri
                if (bestCoverUrl.isNullOrBlank() || bestCoverUrl.contains("spacer.gif")) {
                    bestCoverUrl = withContext(Dispatchers.IO) {
                        DiscogsService.fetchAlbumCoverFallback(artist, title, discogsId)
                    }
                }

                if (!bestCoverUrl.isNullOrBlank()) {
                    val localUri = withContext(Dispatchers.IO) {
                        StorageUtil.downloadAndSaveImage(context, bestCoverUrl)
                    }
                    coverUri = localUri?.toString() ?: bestCoverUrl
                }
            }
        }
    }

    LaunchedEffect(cameraPermissionState.status, shouldLaunchCamera) {
        if (cameraPermissionState.status.isGranted && shouldLaunchCamera) {
            shouldLaunchCamera = false
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = musicColor)
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    NeonHeader(
                        text = stringResource(if (isEditMode) R.string.header_edit_music else R.string.header_add_music),
                        fullWidth = false,
                        color = musicColor
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp), color = musicColor)
                },
                actions = {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.ic_close),
                        onClick = onClose,
                        modifier = Modifier.padding(end = 8.dp),
                        color = musicColor
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
                color = musicColor,
                padding = 12.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        text = if (isEditMode) "UPDATE DATA VIA DISCOGS" else "IMPORT FROM DISCOGS",
                        icon = Icons.Rounded.Language,
                        onClick = {
                            val initialQuery = when {
                                artist.isNotBlank() && title.isNotBlank() -> "$artist $title"
                                title.isNotBlank() -> title
                                else -> artist
                            }
                            val mappedFormat = when (currentFormatId.lowercase()) {
                                "vinyl" -> "Vinyl"
                                "cd" -> "CD"
                                "cassette" -> "Cassette"
                                else -> ""
                            }
                            val yearPart = selectedYear.ifBlank { releaseDate.take(4) }
                            onDiscogsSearch(initialQuery, mappedFormat, label, yearPart)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = musicColor,
                        height = 40.dp
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.label_title)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors(musicColor)
                    )

                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text(stringResource(R.string.label_artist)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors(musicColor)
                    )

                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text(stringResource(R.string.label_genre)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors(musicColor)
                    )

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.label_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors(musicColor)
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
                        },
                        accentColor = musicColor
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
                        },
                        accentColor = musicColor
                    )

                    OutlinedTextField(
                        value = pricePaid,
                        onValueChange = { pricePaid = it },
                        label = { Text("Purchase Price / Paid") },
                        placeholder = { Text("e.g. 19.99 €") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        singleLine = true,
                        colors = synthwaveTextFieldColors(musicColor)
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.label_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        minLines = 3,
                        colors = synthwaveTextFieldColors(musicColor)
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
                            height = 46.dp,
                            color = musicColor
                        )

                        NeonButton(
                            text = stringResource(R.string.action_select),
                            icon = Icons.Rounded.AddAPhoto,
                            onClick = { 
                                pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            height = 46.dp,
                            color = musicColor
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
                        color = musicColor
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
                        color = musicColor,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    )
                }

                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = musicColor,
                    padding = 8.dp
                ) {
                    val previewImageModel = remember(coverUri) {
                        if (coverUri.startsWith("http://") || coverUri.startsWith("https://")) {
                            ImageRequest.Builder(context)
                                .data(coverUri)
                                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36")
                                .addHeader("Referer", "https://www.discogs.com/")
                                .crossfade(true)
                                .build()
                        } else {
                            coverUri
                        }
                    }

                    AsyncImage(
                        model = previewImageModel,
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
                text = stringResource(R.string.action_save_music),
                onClick = {
                    scope.launch {
                        if (title.isNotBlank() && repository.doesMusicExistExcludingId(title, currentFormatId, musicId)) {
                            showDuplicateDialog = true
                        } else {
                            saveMusic()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                color = musicColor
            )
        }
    }

    if (showDuplicateDialog) {
        NeonAlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            onConfirm = {
                showDuplicateDialog = false
                saveMusic()
            },
            title = "ALBUM ALREADY EXISTS",
            text = "An album with this title already exists for this format. Do you still want to add it to your library?",
            confirmText = "ADD ANYWAY",
            dismissText = "CANCEL",
            color = musicColor,
            confirmColor = musicColor
        )
    }

    if (showCropDialog && bitmapToCrop != null) {
        ImageCropDialog(
            bitmap = bitmapToCrop!!,
            onCropConfirmed = { rect ->
                scope.launch {
                    val oldUri = coverUri
                    val cropped = withContext(Dispatchers.IO) {
                        val croppedBitmap = cropBitmap(bitmapToCrop!!, rect)
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
