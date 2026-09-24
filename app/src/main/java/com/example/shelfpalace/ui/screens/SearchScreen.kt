package com.example.shelfpalace.ui.screens

import android.Manifest
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.camera.core.*
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.shelfpalace.R
import com.example.shelfpalace.data.*
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.util.PlatformUtils
import com.example.shelfpalace.util.matchesSearchQuery
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

fun isTitleMatch(localTitle: String, resolvedTitle: String): Boolean {
    if (localTitle.isBlank() || resolvedTitle.isBlank()) return false
    val clean1 = localTitle.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()
    val clean2 = resolvedTitle.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()

    if (clean1 == clean2) return true

    // If either string is purely numeric (like a barcode "4006209000000"), require exact equality
    if (clean2.all { it.isDigit() } || clean1.all { it.isDigit() }) {
        return false
    }

    if (clean2.length >= 4 && clean1.contains(clean2)) return true
    if (clean1.length >= 4 && clean2.contains(clean1)) return true

    val words1 = clean1.split(Regex("\\s+")).filter { it.length >= 2 && !it.all { c -> c.isDigit() } }
    val words2 = clean2.split(Regex("\\s+")).filter { it.length >= 2 && !it.all { c -> c.isDigit() } }

    if (words1.isNotEmpty() && words2.isNotEmpty()) {
        val matchingWords = words1.count { w1 -> words2.contains(w1) }
        val minSize = minOf(words1.size, words2.size)

        if (matchingWords >= 2 && (matchingWords.toFloat() / minSize >= 0.4f)) {
            return true
        }
        if (words1.size == 1 && words2.size == 1 && words1.first() == words2.first()) {
            return true
        }
        if (words1.any { w -> w.length >= 4 && words2.contains(w) }) {
            return true
        }
    }

    return false
}

fun isBarcodeMatch(itemBarcode: String?, searchBarcode: String): Boolean {
    if (itemBarcode.isNullOrBlank() || searchBarcode.isBlank()) return false
    val cleanItem = itemBarcode.lowercase().replace(Regex("[^a-z0-9]"), "")
    val cleanSearch = searchBarcode.lowercase().replace(Regex("[^a-z0-9]"), "")
    if (cleanItem.isEmpty() || cleanSearch.isEmpty()) return false

    if (cleanItem == cleanSearch) return true
    val strippedItem = cleanItem.trimStart('0')
    val strippedSearch = cleanSearch.trimStart('0')
    if (strippedItem.isNotEmpty() && strippedItem == strippedSearch) return true

    if (strippedItem.length >= 8 && strippedSearch.length >= 8) {
        if (strippedItem.endsWith(strippedSearch) || strippedSearch.endsWith(strippedItem)) return true
    }

    return false
}

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    repository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    settingsRepository: SettingsRepository,
    onGameSelected: (String) -> Unit,
    onMovieSelected: (String) -> Unit,
    onMusicSelected: (String) -> Unit,
    onScanClick: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf(initialQuery) }
    var showPhotoScanner by remember { mutableStateOf(false) }
    
    BackHandler {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else {
            onBack()
        }
    }
    
    val focusRequester = remember { FocusRequester() }
    val currentSortOption by settingsRepository.sortOption.collectAsState(initial = SortOption.NAME)
    val scope = rememberCoroutineScope()

    val allGames by repository.getAllGames().collectAsState(initial = emptyList())
    val allMovies by movieRepository.getAllMovies().collectAsState(initial = emptyList())
    val allMusic by musicRepository.getAllMusic().collectAsState(initial = emptyList())
        
    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())

    val filteredGames = remember(allGames, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_games")) emptyList()
        else {
            val enabledPlatforms = StaticData.platforms
                .filter { !disabledIds.contains(it.id) && !disabledIds.contains(it.manufacturerId) }
                .map { it.id }
                .toSet()

            val cleanQuery = searchQuery.trim()
            val queryDigits = cleanQuery.filter { it.isDigit() }
            val isBarcodeQuery = queryDigits.length >= 6 && queryDigits.length == cleanQuery.length

            val filtered = allGames.filter { game ->
                val barcodeMatches = isBarcodeMatch(game.barcode, cleanQuery)
                val titleMatches = !isBarcodeQuery && (
                    game.title.matchesSearchQuery(searchQuery) ||
                    isTitleMatch(game.title, searchQuery)
                )

                (barcodeMatches || titleMatches) && enabledPlatforms.contains(game.platformId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedWith(
                    compareBy<Game> { game ->
                        val idx = StaticData.platforms.indexOfFirst { it.id == game.platformId }
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }.thenBy { it.title.lowercase() }
                )
                SortOption.PLATFORM_NAME -> filtered.sortedWith(
                    compareBy<Game> { game ->
                        StaticData.platforms.find { it.id == game.platformId }?.name ?: ""
                    }.thenBy { it.title.lowercase() }
                )
            }
        }
    }

    val filteredMovies = remember(allMovies, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_movies")) emptyList()
        else {
            val cleanQuery = searchQuery.trim()
            val queryDigits = cleanQuery.filter { it.isDigit() }
            val isBarcodeQuery = queryDigits.length >= 6 && queryDigits.length == cleanQuery.length

            val filtered = allMovies.filter { movie ->
                val barcodeMatches = isBarcodeMatch(movie.barcode, cleanQuery)
                val titleMatches = !isBarcodeQuery && (
                    movie.title.matchesSearchQuery(searchQuery) ||
                    isTitleMatch(movie.title, searchQuery)
                )

                (barcodeMatches || titleMatches) && !disabledIds.contains(movie.formatId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedWith(
                    compareBy<Movie> { movie ->
                        val idx = StaticData.movieFormats.indexOfFirst { it.id == movie.formatId }
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }.thenBy { it.title.lowercase() }
                )
                SortOption.PLATFORM_NAME -> filtered.sortedWith(
                    compareBy<Movie> { movie ->
                        StaticData.movieFormats.find { it.id == movie.formatId }?.name ?: ""
                    }.thenBy { it.title.lowercase() }
                )
            }
        }
    }

    val filteredMusic = remember(allMusic, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_music")) emptyList()
        else {
            val cleanQuery = searchQuery.trim()
            val queryDigits = cleanQuery.filter { it.isDigit() }
            val isBarcodeQuery = queryDigits.length >= 6 && queryDigits.length == cleanQuery.length

            val filtered = allMusic.filter { music ->
                val barcodeMatches = isBarcodeMatch(music.barcode, cleanQuery)
                val titleMatches = !isBarcodeQuery && (
                    music.title.matchesSearchQuery(searchQuery) ||
                    music.artist.matchesSearchQuery(searchQuery) ||
                    isTitleMatch(music.title, searchQuery) ||
                    isTitleMatch(music.artist, searchQuery)
                )

                (barcodeMatches || titleMatches) && !disabledIds.contains(music.formatId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedWith(
                    compareBy<Music> { music ->
                        val idx = StaticData.musicFormats.indexOfFirst { it.id == music.formatId }
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }.thenBy { it.title.lowercase() }
                )
                SortOption.PLATFORM_NAME -> filtered.sortedWith(
                    compareBy<Music> { music ->
                        StaticData.musicFormats.find { it.id == music.formatId }?.name ?: ""
                    }.thenBy { it.title.lowercase() }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (showPhotoScanner) {
        PhotoScanDialog(
            onTextScanned = { recognizedText ->
                showPhotoScanner = false
                searchQuery = recognizedText.trim()
            },
            onDismiss = { showPhotoScanner = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.msg_search_all_games)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = "Search",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_close),
                                            contentDescription = "Clear",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (onScanClick != null) {
                                    IconButton(onClick = onScanClick) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.camera),
                                            contentDescription = "Scan Barcode",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { showPhotoScanner = true }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.camera),
                                        contentDescription = "Scan Photo / Cover",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = getAppCorners(),
                        colors = synthwaveTextFieldColors(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp))
                },
                actions = {
                    if (searchQuery.isNotEmpty()) {
                        SortIconButton(
                            currentSortOption = currentSortOption,
                            onSortOptionSelected = { scope.launch { settingsRepository.setSortOption(it) } },
                            showConsoleSort = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NeonIconButton(
                            iconPainter = painterResource(id = R.drawable.ic_close),
                            onClick = { searchQuery = "" },
                            contentDescription = stringResource(R.string.action_clear)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (searchQuery.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        SectionHeader(
                            text = stringResource(R.string.msg_type_to_search),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (onScanClick != null) {
                            NeonButton(
                                text = "SCAN BARCODE",
                                iconPainter = painterResource(id = R.drawable.camera),
                                onClick = onScanClick,
                                modifier = Modifier.width(260.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else if (filteredGames.isEmpty() && filteredMovies.isEmpty() && filteredMusic.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val accentColor = MaterialTheme.colorScheme.primary
                    val borderRadius = 12.dp
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        shape = getAppCorners(borderRadius),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(bottom = 150.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "No Results for:".uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = searchQuery,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filteredGames.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            SectionHeader(text = "GAMES", color = MaterialTheme.colorScheme.primary)
                        }
                        items(filteredGames) { game ->
                            GameGridItem(
                                game = game,
                                onClick = { onGameSelected(game.id) },
                                aspectRatio = PlatformUtils.getAspectRatioForPlatform(game.platformId)
                            )
                        }
                    }
                    
                    if (filteredMovies.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(text = "MOVIES", color = MaterialTheme.colorScheme.secondary)
                        }
                        items(filteredMovies) { movie ->
                            MovieGridItem(
                                movie = movie,
                                onClick = { onMovieSelected(movie.id) }
                            )
                        }
                    }

                    if (filteredMusic.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(text = "MUSIC", color = MaterialTheme.colorScheme.secondary)
                        }
                        items(filteredMusic) { music ->
                            MusicGridItem(
                                music = music,
                                onClick = { onMusicSelected(music.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalGetImage
@Composable
fun PhotoScanDialog(
    onTextScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            if (dialogWindow != null) {
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                dialogWindow.setGravity(Gravity.CENTER)
                ViewCompat.setOnApplyWindowInsetsListener(dialogView) { _, _ -> WindowInsetsCompat.CONSUMED }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (cameraPermissionState.status.isGranted) {
                var isScanned by remember { mutableStateOf(false) }
                var detectedText by remember { mutableStateOf("") }
                
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                val previewView = remember { PreviewView(context) }
                var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

                val textRecognizer = remember {
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        cameraExecutor.shutdown()
                    }
                }

                LaunchedEffect(Unit) {
                    try {
                        cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                LaunchedEffect(cameraProvider) {
                    val provider = cameraProvider ?: return@LaunchedEffect
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && !isScanned) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    textRecognizer.process(image)
                                        .addOnSuccessListener { visionText ->
                                            val txt = visionText.text
                                            if (txt.isNotBlank()) {
                                                detectedText = txt
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Neon Scan Frame Overlay for Photo/Cover Scan
                val scanAccentColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val cornerLength = 40.dp.toPx()
                    val rectWidth = 280.dp.toPx()
                    val rectHeight = 360.dp.toPx()
                    val left = (size.width - rectWidth) / 2
                    val top = (size.height - rectHeight) / 2
                    val right = left + rectWidth
                    val bottom = top + rectHeight

                    // Top Left
                    drawLine(scanAccentColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
                    drawLine(scanAccentColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
                    // Top Right
                    drawLine(scanAccentColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
                    drawLine(scanAccentColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
                    // Bottom Left
                    drawLine(scanAccentColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
                    drawLine(scanAccentColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)
                    // Bottom Right
                    drawLine(scanAccentColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
                    drawLine(scanAccentColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)
                }

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonBackButton(onClick = onDismiss)
                    Text(
                        "SCAN PHOTO / COVER",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Bottom scan action button
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (detectedText.isNotBlank()) {
                        val firstLine = detectedText.split("\n").firstOrNull { it.isNotBlank() } ?: ""
                        val displayStr = if (firstLine.length > 25) firstLine.take(25) + "..." else firstLine
                        
                        Text(
                            text = "Reading: $displayStr",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Button(
                            onClick = {
                                if (!isScanned) {
                                    isScanned = true
                                    onTextScanned(firstLine)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .height(52.dp)
                                .width(200.dp)
                        ) {
                            Text(
                                "SCAN PHOTO",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        Text(
                            text = "KEEP COVER / PHOTO IN FRAME",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Camera Permission Required", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
        }
    }
}
