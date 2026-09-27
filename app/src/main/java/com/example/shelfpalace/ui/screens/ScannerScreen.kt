@file:OptIn(ExperimentalPermissionsApi::class)
package com.example.shelfpalace.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.shelfpalace.data.CornerStyle
import com.example.shelfpalace.ui.theme.LocalCornerStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.shelfpalace.R
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.remote.BarcodeLookupService
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.theme.SynthwaveLavender
import com.example.shelfpalace.ui.theme.SynthwaveCyan
import com.example.shelfpalace.util.ImageSimilarityUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    repository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    initialTab: Int = 0,
    onGameRecognized: (String) -> Unit,
    onMovieRecognized: (String) -> Unit,
    onMusicRecognized: (String) -> Unit,
    onSearchTitle: (query: String, barcode: String?) -> Unit = { _, _ -> },
    onBack: () -> Unit,
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (cameraPermissionState.status.isGranted) {
                ScannerContent(
                    repository = repository,
                    movieRepository = movieRepository,
                    musicRepository = musicRepository,
                    initialTab = initialTab,
                    onGameRecognized = onGameRecognized,
                    onMovieRecognized = onMovieRecognized,
                    onMusicRecognized = onMusicRecognized,
                    onSearchTitle = onSearchTitle,
                    onBack = onBack
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    NeonBackButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(top = 16.dp, start = 16.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.msg_camera_permission),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.msg_grant_permission))
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalGetImage
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerContent(
    repository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    initialTab: Int = 0,
    onGameRecognized: (String) -> Unit,
    onMovieRecognized: (String) -> Unit,
    onMusicRecognized: (String) -> Unit,
    onSearchTitle: (query: String, barcode: String?) -> Unit = { _, _ -> },
    onBack: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // 0 = COVER SCAN, 1 = BARCODE SCAN
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedText by remember { mutableStateOf("") }
    var detectedBarcode by remember { mutableStateOf("") }
    var candidateBarcode by remember { mutableStateOf("") }
    var candidateCount by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("ANALYZING...") }
    var flashEnabled by remember { mutableStateOf(false) }
    var showNothingFound by remember { mutableStateOf(false) }
    
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val barcodeScannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39
            )
            .build()
    }
    val barcodeScanner = remember { BarcodeScanning.getClient(barcodeScannerOptions) }

    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(cameraProviderFuture) {
        try {
            cameraProvider = cameraProviderFuture.get()
        } catch (e: Exception) {
            Log.e("ScannerScreen", "Failed to get camera provider", e)
        }
    }

    LaunchedEffect(cameraProvider, lifecycleOwner) {
        val provider = cameraProvider ?: return@LaunchedEffect
        
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(textRecognizer, barcodeScanner, imageProxy) { text, barcode, bitmap ->
                        if (!text.isNullOrBlank()) detectedText = text
                        capturedBitmap = bitmap

                        if (!barcode.isNullOrBlank()) {
                            val cleanBarcode = barcode.trim()
                            if (cleanBarcode == candidateBarcode) {
                                candidateCount++
                                if (candidateCount >= 2) {
                                    detectedBarcode = cleanBarcode
                                }
                            } else {
                                candidateBarcode = cleanBarcode
                                candidateCount = 1
                            }
                        }
                    }
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("ScannerScreen", "Use case binding failed", exc)
        }
    }

    LaunchedEffect(flashEnabled, camera) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }

    val resetScanner = {
        detectedText = ""
        detectedBarcode = ""
        capturedBitmap = null
        isScanning = false
        showNothingFound = false
        statusMessage = "ANALYZING..."
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraProvider != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Overlay Viewfinder Frame adapting to Settings CornerStyle
        val isBarcodeTab = selectedTab == 1
        val scanAccentColor = if (isBarcodeTab) SynthwaveCyan else SynthwaveLavender
        val cornerStyle = LocalCornerStyle.current

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerRadius = when (cornerStyle) {
                CornerStyle.ROUNDED -> 24.dp.toPx()
                CornerStyle.OUTLINED -> 8.dp.toPx()
                CornerStyle.SQUARE -> 0.dp.toPx()
            }

            val strokeWidth = 4.dp.toPx()
            val cornerLength = 40.dp.toPx()

            val rectWidth = if (isBarcodeTab) 290.dp.toPx() else 285.dp.toPx()
            val rectHeight = if (isBarcodeTab) 150.dp.toPx() else 360.dp.toPx()
            val centerYOffset = if (isBarcodeTab) 15.dp.toPx() else 18.dp.toPx()

            val left = (size.width - rectWidth) / 2
            val top = (size.height - rectHeight) / 2 + centerYOffset
            val right = left + rectWidth
            val bottom = top + rectHeight

            // Subtle outer border adapting to selected CornerStyle
            drawRoundRect(
                color = scanAccentColor.copy(alpha = 0.25f),
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Corner Accents adapting to selected CornerStyle
            if (cornerRadius > 0f) {
                val capStyle = if (cornerStyle == CornerStyle.ROUNDED) StrokeCap.Round else StrokeCap.Square

                // Top-Left Arc Corner
                val pathTL = Path().apply {
                    moveTo(left + cornerLength, top)
                    lineTo(left + cornerRadius, top)
                    arcTo(
                        rect = Rect(left, top, left + cornerRadius * 2, top + cornerRadius * 2),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    lineTo(left, top + cornerLength)
                }
                drawPath(pathTL, scanAccentColor.copy(alpha = 0.9f), style = Stroke(strokeWidth, cap = capStyle))

                // Top-Right Arc Corner
                val pathTR = Path().apply {
                    moveTo(right - cornerLength, top)
                    lineTo(right - cornerRadius, top)
                    arcTo(
                        rect = Rect(right - cornerRadius * 2, top, right, top + cornerRadius * 2),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(right, top + cornerLength)
                }
                drawPath(pathTR, scanAccentColor.copy(alpha = 0.9f), style = Stroke(strokeWidth, cap = capStyle))

                // Bottom-Left Arc Corner
                val pathBL = Path().apply {
                    moveTo(left + cornerLength, bottom)
                    lineTo(left + cornerRadius, bottom)
                    arcTo(
                        rect = Rect(left, bottom - cornerRadius * 2, left + cornerRadius * 2, bottom),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(left, bottom - cornerLength)
                }
                drawPath(pathBL, scanAccentColor.copy(alpha = 0.9f), style = Stroke(strokeWidth, cap = capStyle))

                // Bottom-Right Arc Corner
                val pathBR = Path().apply {
                    moveTo(right - cornerLength, bottom)
                    lineTo(right - cornerRadius, bottom)
                    arcTo(
                        rect = Rect(right - cornerRadius * 2, bottom - cornerRadius * 2, right, bottom),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    lineTo(right, bottom - cornerLength)
                }
                drawPath(pathBR, scanAccentColor.copy(alpha = 0.9f), style = Stroke(strokeWidth, cap = capStyle))
            } else {
                // Square Corner Lines (SQUARE style)
                // Top Left
                drawLine(scanAccentColor.copy(alpha = 0.9f), Offset(left, top), Offset(left + cornerLength, top), strokeWidth, cap = StrokeCap.Square)
                drawLine(scanAccentColor.copy(alpha = 0.9f), Offset(left, top), Offset(left, top + cornerLength), strokeWidth, cap = StrokeCap.Square)

                // Top Right
                drawLine(scanAccentColor.copy(alpha = 0.9f), Offset(right, top), Offset(right - cornerLength, top), strokeWidth, cap = StrokeCap.Square)
                drawLine(scanAccentColor.copy(alpha = 0.9f), Offset(right, top), Offset(right, top + cornerLength), strokeWidth, cap = StrokeCap.Square)

                // Bottom Left
                drawLine(scanAccentColor.copy(alpha = 0.9f), Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth, cap = StrokeCap.Square)
                drawLine(scanAccentColor.copy(alpha = 0.9f), Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth, cap = StrokeCap.Square)

                // Bottom Right
                drawLine(scanAccentColor.copy(alpha = 0.9f), Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth, cap = StrokeCap.Square)
                drawLine(scanAccentColor.copy(alpha = 0.9f), Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth, cap = StrokeCap.Square)
            }

            val hasDetection = if (isBarcodeTab) detectedBarcode.isNotBlank() else (detectedText.isNotBlank() || capturedBitmap != null)
            if (hasDetection) {
                drawRoundRect(
                    color = scanAccentColor.copy(alpha = 0.12f),
                    topLeft = Offset(left, top),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }

        // Top bar & Tab Navigation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonBackButton(onClick = onBack)
                
                NeonHeader(
                    text = "SHELF SCANNER",
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        if (flashEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        contentDescription = stringResource(R.string.content_desc_toggle_flash),
                        tint = Color.White
                    )
                }
            }

            // Mode Switcher Tabs: COVER SCAN vs BARCODE SCAN
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(Color.Black.copy(alpha = 0.7f), getAppCorners(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), getAppCorners(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Tab 0: COVER SCAN
                Surface(
                    onClick = {
                        selectedTab = 0
                        resetScanner()
                    },
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = getAppCorners(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.camera),
                            contentDescription = "Cover Scan",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COVER SCAN",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Tab 1: BARCODE SCAN
                Surface(
                    onClick = {
                        selectedTab = 1
                        resetScanner()
                    },
                    color = if (selectedTab == 1) SynthwaveCyan else Color.Transparent,
                    shape = getAppCorners(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.barcode_scan),
                            contentDescription = "Barcode Scan",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BARCODE SCAN",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedTab == 1) Color.Black else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Bottom Controls & Action Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isBarcodeTab) {
                // BARCODE TAB CONTROLS
                if (detectedBarcode.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = getAppCorners(20.dp),
                        border = BorderStroke(1.dp, SynthwaveCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.barcode_scan),
                                contentDescription = "Barcode",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "BARCODE: $detectedBarcode",
                                color = SynthwaveCyan,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "HOLD BARCODE IN FRAME",
                        color = SynthwaveCyan,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), getAppCorners(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Button(
                    onClick = {
                        if (!isScanning) {
                            isScanning = true
                            statusMessage = "SEARCH BARCODE..."
                            scope.launch {
                                try {
                                    val games = repository.getAllGames().first()
                                    val movies = movieRepository.getAllMovies().first()
                                    val musicList = musicRepository.getAllMusic().first()

                                    val barcodeToSearch = detectedBarcode.trim()

                                    var recognizedGameId: String? = null
                                    var recognizedMovieId: String? = null
                                    var recognizedMusicId: String? = null

                                    // 1. Local Database Check by Barcode
                                    if (barcodeToSearch.isNotBlank()) {
                                        val matchedGame = repository.getGameByBarcode(barcodeToSearch)
                                            ?: games.find { isBarcodeMatch(it.barcode, barcodeToSearch) }
                                        val matchedMovie = movieRepository.getMovieByBarcode(barcodeToSearch)
                                            ?: movies.find { isBarcodeMatch(it.barcode, barcodeToSearch) }
                                        val matchedMusic = musicRepository.getMusicByBarcode(barcodeToSearch)
                                            ?: musicList.find { isBarcodeMatch(it.barcode, barcodeToSearch) }

                                        if (matchedGame != null) recognizedGameId = matchedGame.id
                                        else if (matchedMovie != null) recognizedMovieId = matchedMovie.id
                                        else if (matchedMusic != null) recognizedMusicId = matchedMusic.id
                                    }

                                    if (recognizedGameId != null) {
                                        delay(200)
                                        resetScanner()
                                        onGameRecognized(recognizedGameId)
                                        return@launch
                                    } else if (recognizedMovieId != null) {
                                        delay(200)
                                        resetScanner()
                                        onMovieRecognized(recognizedMovieId)
                                        return@launch
                                    } else if (recognizedMusicId != null) {
                                        delay(200)
                                        resetScanner()
                                        onMusicRecognized(recognizedMusicId)
                                        return@launch
                                    }

                                    // 2. Online Barcode Search API (via BarcodeLookupService)
                                    if (barcodeToSearch.isNotBlank()) {
                                        statusMessage = "SEARCH BARCODE..."
                                        val resolvedTitle = BarcodeLookupService.lookupBarcodeTitle(barcodeToSearch)
                                        
                                        if (!resolvedTitle.isNullOrBlank()) {
                                            // Check if resolved title matches anything in local library
                                            val matchedGame = games.find { isExactLibraryMatch(it.title, resolvedTitle) }
                                            val matchedMovie = movies.find { isExactLibraryMatch(it.title, resolvedTitle) }
                                            val matchedMusic = musicList.find { isExactLibraryMatch(it.title, resolvedTitle) }

                                            if (matchedGame != null) {
                                                delay(200)
                                                resetScanner()
                                                onGameRecognized(matchedGame.id)
                                                return@launch
                                            } else if (matchedMovie != null) {
                                                delay(200)
                                                resetScanner()
                                                onMovieRecognized(matchedMovie.id)
                                                return@launch
                                            } else if (matchedMusic != null) {
                                                delay(200)
                                                resetScanner()
                                                onMusicRecognized(matchedMusic.id)
                                                return@launch
                                            }

                                            // Navigate to search screen with resolved product title & original barcode
                                            delay(200)
                                            resetScanner()
                                            onSearchTitle(resolvedTitle, barcodeToSearch)
                                            return@launch
                                        } else {
                                            // Navigate to search screen with raw barcode
                                            delay(200)
                                            resetScanner()
                                            onSearchTitle(barcodeToSearch, barcodeToSearch)
                                            return@launch
                                        }
                                    }

                                    delay(200)
                                    isScanning = false
                                    showNothingFound = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isScanning = false
                                    showNothingFound = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .width(230.dp),
                    shape = getAppCorners(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SynthwaveCyan)
                ) {
                    Text(
                        "SEARCH BARCODE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                }
            } else {
                // COVER SCAN TAB CONTROLS
                val displayLabel = if (detectedText.isNotBlank()) {
                    val firstLine = detectedText.split("\n").firstOrNull { it.isNotBlank() } ?: ""
                    if (firstLine.length > 25) firstLine.take(25) + "..." else firstLine
                } else {
                    "HOLD COVER / PHOTO IN FRAME"
                }

                Text(
                    text = displayLabel,
                    color = SynthwaveLavender,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), getAppCorners(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Button(
                    onClick = {
                        if (!isScanning) {
                            isScanning = true
                            statusMessage = "ANALYZING COVER..."
                            scope.launch {
                                try {
                                    val games = repository.getAllGames().first()
                                    val movies = movieRepository.getAllMovies().first()
                                    val musicList = musicRepository.getAllMusic().first()

                                    var recognizedGameId: String? = null
                                    var recognizedMovieId: String? = null
                                    var recognizedMusicId: String? = null

                                    // 1. High-precision Title match from detectedText
                                    if (detectedText.isNotBlank()) {
                                        val bestGame = games.map { game -> game to scoreItemTitle(game.title, "", detectedText) }.maxByOrNull { it.second }
                                        val bestMovie = movies.map { movie -> movie to scoreItemTitle(movie.title, "", detectedText) }.maxByOrNull { it.second }
                                        val bestMusic = musicList.map { music -> music to scoreItemTitle(music.title, music.artist, detectedText) }.maxByOrNull { it.second }

                                        val gameScore = bestGame?.second ?: 0.0
                                        val movieScore = bestMovie?.second ?: 0.0
                                        val musicScore = bestMusic?.second ?: 0.0

                                        val maxScore = maxOf(gameScore, movieScore, musicScore)

                                        if (maxScore >= 0.50) {
                                            when {
                                                gameScore == maxScore && bestGame != null -> recognizedGameId = bestGame.first.id
                                                movieScore == maxScore && bestMovie != null -> recognizedMovieId = bestMovie.first.id
                                                bestMusic != null -> recognizedMusicId = bestMusic.first.id
                                            }
                                        }
                                    }

                                    // 2. Cover Image Similarity if title match failed
                                    if (recognizedGameId == null && recognizedMovieId == null && recognizedMusicId == null && capturedBitmap != null) {
                                        val currentBitmap = capturedBitmap
                                        if (currentBitmap != null) {
                                            val scannedFp = ImageSimilarityUtils.computeFingerprint(currentBitmap)

                                            suspend fun getCoverSimilarity(coverUri: String): Double {
                                                if (coverUri.isBlank()) return 0.0
                                                val coverBitmap = ImageSimilarityUtils.loadBitmapFromUriOrUrl(context, coverUri) ?: return 0.0
                                                val coverFp = ImageSimilarityUtils.computeFingerprint(coverBitmap)
                                                val sim = ImageSimilarityUtils.calculateSimilarity(scannedFp, coverFp)
                                                try { coverBitmap.recycle() } catch (_: Exception) {}
                                                return sim
                                            }

                                            val gameMatches = games.map { game -> game to getCoverSimilarity(game.coverUri) }
                                            val movieMatches = movies.map { movie -> movie to getCoverSimilarity(movie.coverUri) }
                                            val musicMatches = musicList.map { music -> music to getCoverSimilarity(music.coverUri) }

                                            val bestGame = gameMatches.maxByOrNull { it.second }
                                            val bestMovie = movieMatches.maxByOrNull { it.second }
                                            val bestMusic = musicMatches.maxByOrNull { it.second }

                                            val gameScore = bestGame?.second ?: 0.0
                                            val movieScore = bestMovie?.second ?: 0.0
                                            val musicScore = bestMusic?.second ?: 0.0

                                            val maxScore = maxOf(gameScore, movieScore, musicScore)

                                            if (maxScore >= 0.65) {
                                                when {
                                                    gameScore == maxScore && bestGame != null -> recognizedGameId = bestGame.first.id
                                                    movieScore == maxScore && bestMovie != null -> recognizedMovieId = bestMovie.first.id
                                                    bestMusic != null -> recognizedMusicId = bestMusic.first.id
                                                }
                                            }
                                        }
                                    }

                                    if (recognizedGameId != null) {
                                        delay(200)
                                        resetScanner()
                                        onGameRecognized(recognizedGameId)
                                        return@launch
                                    } else if (recognizedMovieId != null) {
                                        delay(200)
                                        resetScanner()
                                        onMovieRecognized(recognizedMovieId)
                                        return@launch
                                    } else if (recognizedMusicId != null) {
                                        delay(200)
                                        resetScanner()
                                        onMusicRecognized(recognizedMusicId)
                                        return@launch
                                    }

                                    // 3. Fallback to SearchScreen with recognized text
                                    if (detectedText.isNotBlank()) {
                                        val firstLine = detectedText.split("\n").firstOrNull { it.isNotBlank() }?.trim() ?: ""
                                        val cleaned = cleanScannerTitle(firstLine)
                                        if (cleaned.length >= 3) {
                                            delay(200)
                                            resetScanner()
                                            onSearchTitle(cleaned, null)
                                            return@launch
                                        }
                                    }

                                    delay(200)
                                    isScanning = false
                                    showNothingFound = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isScanning = false
                                    showNothingFound = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .width(230.dp),
                    shape = getAppCorners(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "SCAN COVER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
        
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = if (isBarcodeTab) SynthwaveCyan else MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        statusMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp)
                    )
                }
            }
        }

        if (showNothingFound) {
            NothingFoundWindow(onTryAgain = resetScanner)
        }
    }
}

@ExperimentalGetImage
private fun processImageProxy(
    textRecognizer: TextRecognizer,
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onDetected: (String?, String?, Bitmap?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val capturedBitmap = try {
            val rawBitmap = imageProxy.toBitmap()
            if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }
        } catch (e: Exception) {
            null
        }

        val image = InputImage.fromMediaImage(mediaImage, rotation)

        val taskText = textRecognizer.process(image)
        val taskBarcode = barcodeScanner.process(image)

        Tasks.whenAllComplete(taskText, taskBarcode)
            .addOnCompleteListener {
                try {
                    val visionText = if (taskText.isSuccessful) taskText.result?.text else null
                    val barcodes = if (taskBarcode.isSuccessful) taskBarcode.result else null
                    val rawBarcode = barcodes?.filter { isBarcodeInViewfinder(it, mediaImage.width, mediaImage.height) }
                        ?.mapNotNull { it.rawValue?.trim() }
                        ?.firstOrNull { code ->
                            val clean = code.filter { it.isDigit() }
                            clean.length >= 6 && isValidBarcodeChecksum(code)
                        }

                    onDetected(visionText, rawBarcode, capturedBitmap)
                } catch (e: Exception) {
                    Log.e("ScannerScreen", "Error processing ML Kit frame", e)
                } finally {
                    try { imageProxy.close() } catch (_: Exception) {}
                }
            }
    } else {
        imageProxy.close()
    }
}

private fun isBarcodeInViewfinder(barcode: Barcode, imageWidth: Int, imageHeight: Int): Boolean {
    val box = barcode.boundingBox ?: return true
    val centerX = box.centerX().toFloat()
    val centerY = box.centerY().toFloat()

    val minX = imageWidth * 0.05f
    val maxX = imageWidth * 0.95f
    val minY = imageHeight * 0.05f
    val maxY = imageHeight * 0.95f

    return centerX in minX..maxX && centerY in minY..maxY
}

private fun cleanScannerTitle(text: String): String {
    return text.lowercase()
        .replace(Regex("(?i)\\b(spiel|spiele|videospiel|videospiele|game|games|videogame|videogames|film|filme|kinofilm|movie|movies|video|media|disc|disk|cd|dvd|bluray|blu-ray|4k|uhd|hd|musik|music|album|audio|soundtrack|nintendo|switch|playstation|ps1|ps2|ps3|ps4|ps5|xbox|pc|edition|version|pal|ntsc|ovp|neu|gebraucht|deutsch|import)\\b"), "")
        .replace(Regex("[^a-z0-9]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun scoreItemTitle(title: String, artist: String = "", detectedText: String): Double {
    val cleanText = cleanScannerTitle(detectedText)
    if (cleanText.isBlank()) return 0.0

    val cleanTitle = cleanScannerTitle(title)
    if (cleanTitle.isBlank()) return 0.0

    val titleWords = cleanTitle.split(" ").filter { it.length >= 2 && !TITLE_STOPWORDS.contains(it) }.toSet()
    val textWords = cleanText.split(" ").filter { it.length >= 2 && !TITLE_STOPWORDS.contains(it) }.toSet()

    if (titleWords.isEmpty() || textWords.isEmpty()) return 0.0

    val common = titleWords.intersect(textWords)
    if (common.isEmpty()) return 0.0

    val titleRatio = common.size.toDouble() / titleWords.size
    val textRatio = common.size.toDouble() / textWords.size

    if (titleRatio >= 0.85 && textRatio >= 0.65) {
        var score = 0.90 + titleRatio * 0.1
        if (artist.isNotBlank()) {
            val cleanArtist = cleanScannerTitle(artist)
            if (cleanArtist.isNotBlank() && cleanText.contains(cleanArtist)) {
                score += 0.10
            }
        }
        return score
    }

    return titleRatio * 0.4
}

@Composable
fun NothingFoundWindow(
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        NeonCard(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "NOTHING FOUND",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    "The scan did not yield any results in the local library or online barcode databases.",
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                NeonButton(
                    text = "RETRY",
                    onClick = onTryAgain,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
