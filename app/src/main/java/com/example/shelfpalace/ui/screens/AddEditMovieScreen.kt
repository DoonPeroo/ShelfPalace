package com.example.shelfpalace.ui.screens

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.data.Movie
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.data.remote.TmdbService
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.theme.SynthwaveCyan
import com.example.shelfpalace.ui.theme.SynthwaveDark
import com.example.shelfpalace.util.DateUtils
import com.example.shelfpalace.util.StorageUtil
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import android.graphics.Bitmap
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.ui.res.painterResource
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
fun AddEditMovieScreen(
    formatId: String?,
    movieId: String?,
    tmdbId: Long? = null,
    language: String = "en-US",
    repository: MovieRepository,
    onSave: (String) -> Unit,
    onTmdbSearch: (query: String, year: String, language: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    @Suppress("UNUSED_PARAMETER") onHome: () -> Unit,
) {
    val context = LocalContext.current
    
    var currentFormatId by rememberSaveable { mutableStateOf(formatId ?: "") }
    var title by rememberSaveable { mutableStateOf("") }
    var releaseDate by rememberSaveable { mutableStateOf("") }
    var displayDate by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var director by rememberSaveable { mutableStateOf("") }
    var cast by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var coverUri by rememberSaveable { mutableStateOf("") }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var purchaseDate by rememberSaveable { mutableStateOf("") }
    var pricePaid by rememberSaveable { mutableStateOf("") }
    var dateAdded by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var currentLanguage by rememberSaveable { mutableStateOf(language) }
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
    val isEditMode = movieId != null
    var isInitialized by remember { mutableStateOf(false) }
    var existingMovie by remember { mutableStateOf<Movie?>(null) }
    var loadedTmdbId by rememberSaveable { mutableStateOf<Long?>(null) }
    var loadedLanguage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(movieId, tmdbId, language) {
        if (!isInitialized) {
            if (isEditMode && movieId != null) {
                val movie = repository.getMovieById(movieId)
                movie?.let {
                    existingMovie = it
                    currentFormatId = it.formatId
                    title = it.title
                    releaseDate = it.releaseDate
                    genre = it.genre
                    director = it.director
                    cast = it.cast
                    description = it.description
                    coverUri = it.coverUri
                    isFavorite = it.isFavorite
                    dateAdded = it.dateAdded
                    purchaseDate = it.purchaseDate
                    pricePaid = it.pricePaid
                    if (it.language.isNotBlank()) currentLanguage = it.language
                    
                    if (it.releaseDate.isNotEmpty()) {
                        displayDate = DateUtils.formatDisplayDate(it.releaseDate)
                        val parts = it.releaseDate.split("-")
                        if (parts.size == 3) {
                            selectedYear = parts[0]
                            selectedMonth = parts[1].padStart(2, '0')
                            selectedDay = parts[2].padStart(2, '0')
                        } else if (parts.size == 1) {
                            selectedYear = parts[0]
                        }
                    }
                    if (it.purchaseDate.isNotEmpty()) {
                        val parts = it.purchaseDate.split("-")
                        if (parts.size == 3) {
                            selectedPurchaseYear = parts[0]
                            selectedPurchaseMonth = parts[1].padStart(2, '0')
                            selectedPurchaseDay = parts[2].padStart(2, '0')
                        } else if (parts.size == 1) {
                            selectedPurchaseYear = parts[0]
                        }
                    }
                }
            }
            isInitialized = true
        }

        if (tmdbId != null && (tmdbId != loadedTmdbId || language != loadedLanguage)) {
            val details = withContext(Dispatchers.IO) {
                TmdbService.getMovieDetails(tmdbId, language = language)
            }
            if (details != null) {
                if (!details.title.isNullOrBlank()) title = details.title
                if (!details.releaseDate.isNullOrBlank()) {
                    releaseDate = details.releaseDate
                    displayDate = DateUtils.formatDisplayDate(releaseDate)
                    val parts = releaseDate.split("-")
                    if (parts.size == 3) {
                        selectedYear = parts[0]
                        selectedMonth = parts[1].padStart(2, '0')
                        selectedDay = parts[2].padStart(2, '0')
                    } else if (parts.size == 1) {
                        selectedYear = parts[0]
                        selectedMonth = ""
                        selectedDay = ""
                    }
                }
                if (details.genreNames.isNotBlank()) genre = details.genreNames
                if (details.directorName.isNotBlank()) director = details.directorName
                if (details.castNames.isNotBlank()) cast = details.castNames
                if (!details.overview.isNullOrBlank()) description = details.overview

                val posterUrl = details.posterUrl
                if (!posterUrl.isNullOrBlank()) {
                    val downloadedUri = withContext(Dispatchers.IO) {
                        StorageUtil.downloadAndSaveImage(context, posterUrl)
                    }
                    if (downloadedUri != null) {
                        coverUri = downloadedUri.toString()
                    }
                }
            }
            loadedTmdbId = tmdbId
            loadedLanguage = language
            currentLanguage = language
        }
    }

    val saveMovie: () -> Unit = {
        scope.launch {
            try {
                val targetId = existingMovie?.id ?: movieId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                val isCurrentlyEditing = existingMovie != null || !movieId.isNullOrBlank()
                val originalDateAdded = existingMovie?.dateAdded ?: dateAdded

                val movieToSave = Movie(
                    id = targetId,
                    formatId = currentFormatId,
                    title = title,
                    releaseDate = releaseDate,
                    genre = genre,
                    director = director,
                    cast = cast,
                    description = description,
                    coverUri = coverUri,
                    isFavorite = isFavorite,
                    dateAdded = originalDateAdded,
                    status = existingMovie?.status ?: "Plan to watch",
                    purchaseDate = purchaseDate,
                    pricePaid = pricePaid,
                    notes = existingMovie?.notes ?: "",
                    language = currentLanguage,
                    tomatometer = existingMovie?.tomatometer,
                    popcornmeter = existingMovie?.popcornmeter,
                    tmdbRating = existingMovie?.tmdbRating
                )
                withContext(NonCancellable) {
                    if (isCurrentlyEditing) {
                        repository.updateMovie(movieToSave)
                    } else {
                        repository.insertMovie(movieToSave)
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
            modifier = Modifier.fillMaxSize(),
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
                        text = stringResource(if (isEditMode) R.string.header_edit_movie else R.string.header_add_movie),
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
                        text = "IMPORT MOVIE (TMDB)",
                        icon = Icons.Rounded.Movie,
                        onClick = {
                            val yearPart = selectedYear.takeIf { it.isNotBlank() } ?: ""
                            onTmdbSearch(title, yearPart, language)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        height = 42.dp
                    )

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

                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text(stringResource(R.string.label_genre)) },
                        placeholder = { Text("Action, Drama, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors()
                    )

                    OutlinedTextField(
                        value = director,
                        onValueChange = { director = it },
                        label = { Text(stringResource(R.string.label_director)) },
                        placeholder = { Text("None") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors()
                    )

                    OutlinedTextField(
                        value = cast,
                        onValueChange = { cast = it },
                        label = { Text(stringResource(R.string.label_cast)) },
                        placeholder = { Text("None") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors()
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
                        placeholder = { Text("e.g. 19.99 €") },
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
                                    loadBitmapFromUri(context, android.net.Uri.parse(coverUri))
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
                text = stringResource(R.string.action_save_movie),
                onClick = {
                    scope.launch {
                        if (title.isNotBlank() && repository.doesMovieExistExcludingId(title, currentFormatId, movieId)) {
                            showDuplicateDialog = true
                        } else {
                            saveMovie()
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
                saveMovie()
            },
            title = "MOVIE ALREADY EXISTS",
            text = "A movie with this title already exists for this format. Do you still want to add it to your library?",
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
