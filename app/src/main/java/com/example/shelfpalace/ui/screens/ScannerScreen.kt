@file:OptIn(ExperimentalPermissionsApi::class)
package com.example.shelfpalace.ui.screens

import android.Manifest
import android.util.Log
import kotlin.OptIn
import androidx.compose.ui.platform.LocalContext
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.shelfpalace.R
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.theme.SynthwaveLavender
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    repository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    onGameRecognized: (String) -> Unit,
    onMovieRecognized: (String) -> Unit,
    onMusicRecognized: (String) -> Unit,
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
                    onGameRecognized = onGameRecognized,
                    onMovieRecognized = onMovieRecognized,
                    onMusicRecognized = onMusicRecognized,
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
    onGameRecognized: (String) -> Unit,
    onMovieRecognized: (String) -> Unit,
    onMusicRecognized: (String) -> Unit,
    onBack: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var detectedObject by remember { mutableStateOf<DetectedObject?>(null) }
    var detectedText by remember { mutableStateOf("") }
    var detectedBarcode by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var showNothingFound by remember { mutableStateOf(false) }
    
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    val objectDetector = remember {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification() 
            .build()
        ObjectDetection.getClient(options)
    }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val barcodeScanner = remember {
        BarcodeScanning.getClient()
    }

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
                    processImageProxy(objectDetector, textRecognizer, barcodeScanner, imageProxy) { obj, text, barcode ->
                        detectedObject = obj
                        detectedText = text ?: ""
                        detectedBarcode = barcode ?: ""
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

    var lastScannedBarcode by remember { mutableStateOf("") }

    val resetScanner = {
        detectedObject = null
        detectedText = ""
        detectedBarcode = ""
        lastScannedBarcode = ""
        isScanning = false
        showNothingFound = false
    }

    LaunchedEffect(detectedBarcode) {
        val cleanBarcode = detectedBarcode.trim()
        if (cleanBarcode.isNotBlank() && !isScanning && cleanBarcode != lastScannedBarcode) {
            isScanning = true
            lastScannedBarcode = cleanBarcode

            try {
                val games = repository.getAllGames().first()
                val movies = movieRepository.getAllMovies().first()
                val musicList = musicRepository.getAllMusic().first()

                var recognizedGameId: String? = null
                var recognizedMovieId: String? = null
                var recognizedMusicId: String? = null

                // 1. Direct local barcode match
                val bGame = games.find { isBarcodeMatch(it.barcode, cleanBarcode) }
                val bMovie = movies.find { isBarcodeMatch(it.barcode, cleanBarcode) }
                val bMusic = musicList.find { isBarcodeMatch(it.barcode, cleanBarcode) }

                if (bGame != null) recognizedGameId = bGame.id
                else if (bMovie != null) recognizedMovieId = bMovie.id
                else if (bMusic != null) recognizedMusicId = bMusic.id

                if (recognizedGameId != null) {
                    delay(300)
                    resetScanner()
                    onGameRecognized(recognizedGameId)
                } else if (recognizedMovieId != null) {
                    delay(300)
                    resetScanner()
                    onMovieRecognized(recognizedMovieId)
                } else if (recognizedMusicId != null) {
                    delay(300)
                    resetScanner()
                    onMusicRecognized(recognizedMusicId)
                } else {
                    delay(300)
                    isScanning = false
                    showNothingFound = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isScanning = false
                showNothingFound = true
            }
        }
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

        // Overlay frame
        val scanAccentColor = SynthwaveLavender
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val cornerLength = 40.dp.toPx()
            val rectWidth = 280.dp.toPx()
            val rectHeight = 380.dp.toPx()
            val left = (size.width - rectWidth) / 2
            val top = (size.height - rectHeight) / 2
            val right = left + rectWidth
            val bottom = top + rectHeight

            // Draw neon corners
            // Top Left
            drawLine(scanAccentColor.copy(alpha = 0.8f), Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
            drawLine(scanAccentColor.copy(alpha = 0.8f), Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

            // Top Right
            drawLine(scanAccentColor.copy(alpha = 0.8f), Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
            drawLine(scanAccentColor.copy(alpha = 0.8f), Offset(right, top), Offset(right, top + cornerLength), strokeWidth)

            // Bottom Left
            drawLine(scanAccentColor.copy(alpha = 0.8f), Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
            drawLine(scanAccentColor.copy(alpha = 0.8f), Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)

            // Bottom Right
            drawLine(scanAccentColor.copy(alpha = 0.8f), Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
            drawLine(scanAccentColor.copy(alpha = 0.8f), Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)
            
            // Subtle neon glow
            val hasDetection = detectedObject != null || detectedText.isNotBlank() || detectedBarcode.isNotBlank()
            if (hasDetection) {
                drawRect(
                    color = scanAccentColor.copy(alpha = 0.12f),
                    topLeft = Offset(left, top),
                    size = Size(rectWidth, rectHeight)
                )
            }
        }

        // Top bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp),
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
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val canScan = detectedObject != null || detectedText.isNotBlank() || detectedBarcode.isNotBlank()

            if (canScan && !isScanning) {
                val displayLabel = if (detectedBarcode.isNotBlank()) {
                    "Barcode: $detectedBarcode"
                } else if (detectedText.isNotBlank()) {
                    val firstLine = detectedText.split("\n").firstOrNull { it.isNotBlank() } ?: ""
                    if (firstLine.length > 20) firstLine.take(20) + "..." else firstLine
                } else {
                    detectedObject?.labels?.firstOrNull()?.text ?: "Cover Photo"
                }
                
                Text(
                    text = displayLabel,
                    color = SynthwaveLavender,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
                
                Button(
                    onClick = {
                        if (!isScanning) {
                            isScanning = true
                            scope.launch {
                                try {
                                    val games = repository.getAllGames().first()
                                    val movies = movieRepository.getAllMovies().first()
                                    val musicList = musicRepository.getAllMusic().first()

                                    var recognizedGameId: String? = null
                                    var recognizedMovieId: String? = null
                                    var recognizedMusicId: String? = null

                                    val cleanBarcode = detectedBarcode.trim()
                                    if (cleanBarcode.isNotBlank()) {
                                        val bGame = games.find { isBarcodeMatch(it.barcode, cleanBarcode) }
                                        val bMovie = movies.find { isBarcodeMatch(it.barcode, cleanBarcode) }
                                        val bMusic = musicList.find { isBarcodeMatch(it.barcode, cleanBarcode) }

                                        if (bGame != null) recognizedGameId = bGame.id
                                        else if (bMovie != null) recognizedMovieId = bMovie.id
                                        else if (bMusic != null) recognizedMusicId = bMusic.id
                                    }

                                    if (cleanBarcode.isBlank() && recognizedGameId == null && recognizedMovieId == null && recognizedMusicId == null && detectedText.isNotBlank()) {
                                        val stopWords = setOf("dvd", "pal", "ntsc", "bluray", "disc", "game", "video", "media", "rated", "official", "nintendo", "playstation", "xbox", "sony", "sega", "made", "japan", "usa", "europe")
                                        val words = detectedText.lowercase().split(Regex("\\s+"))
                                            .map { it.filter { char -> char.isLetterOrDigit() } }
                                            .filter { it.length >= 3 && !stopWords.contains(it) && !it.all { c -> c.isDigit() } }

                                        if (words.isNotEmpty()) {
                                            val gameMatches = games.filter { game ->
                                                words.any { word -> game.title.contains(word, ignoreCase = true) }
                                            }
                                            val movieMatches = movies.filter { movie ->
                                                words.any { word -> movie.title.contains(word, ignoreCase = true) }
                                            }
                                            val musicMatches = musicList.filter { music ->
                                                words.any { word -> music.title.contains(word, ignoreCase = true) || music.artist.contains(word, ignoreCase = true) }
                                            }

                                            if (gameMatches.isNotEmpty() || movieMatches.isNotEmpty() || musicMatches.isNotEmpty()) {
                                                val bestGame = gameMatches.maxByOrNull { game -> words.count { word -> game.title.contains(word, ignoreCase = true) } }
                                                val bestMovie = movieMatches.maxByOrNull { movie -> words.count { word -> movie.title.contains(word, ignoreCase = true) } }
                                                val bestMusic = musicMatches.maxByOrNull { music -> words.count { word -> music.title.contains(word, ignoreCase = true) || music.artist.contains(word, ignoreCase = true) } }

                                                val gameScore = bestGame?.let { g -> words.count { w -> g.title.contains(w, ignoreCase = true) } } ?: 0
                                                val movieScore = bestMovie?.let { m -> words.count { w -> m.title.contains(w, ignoreCase = true) } } ?: 0
                                                val musicScore = bestMusic?.let { m -> words.count { w -> m.title.contains(w, ignoreCase = true) || m.artist.contains(w, ignoreCase = true) } } ?: 0

                                                val maxScore = maxOf(gameScore, movieScore, musicScore)

                                                if (maxScore > 0) {
                                                    if (gameScore == maxScore && bestGame != null) recognizedGameId = bestGame.id
                                                    else if (movieScore == maxScore && bestMovie != null) recognizedMovieId = bestMovie.id
                                                    else if (bestMusic != null) recognizedMusicId = bestMusic.id
                                                }
                                            }
                                        }
                                    }

                                    if (recognizedGameId != null) {
                                        delay(300)
                                        resetScanner()
                                        onGameRecognized(recognizedGameId)
                                    } else if (recognizedMovieId != null) {
                                        delay(300)
                                        resetScanner()
                                        onMovieRecognized(recognizedMovieId)
                                    } else if (recognizedMusicId != null) {
                                        delay(300)
                                        resetScanner()
                                        onMusicRecognized(recognizedMusicId)
                                    } else {
                                        delay(300)
                                        isScanning = false
                                        showNothingFound = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isScanning = false
                                    showNothingFound = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .width(220.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "SCAN PHOTO",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else if (!isScanning) {
                Text(
                    text = "KEEP COVER / PHOTO IN FRAME",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ANALYZING...",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
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
    detector: ObjectDetector,
    textRecognizer: TextRecognizer,
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onDetected: (DetectedObject?, String?, String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        var tasksActive = 3
        var currentObj: DetectedObject? = null
        var currentText: String? = null
        var currentBarcode: String? = null

        fun checkDone() {
            tasksActive--
            if (tasksActive == 0) {
                onDetected(currentObj, currentText, currentBarcode)
                imageProxy.close()
            }
        }

        detector.process(image)
            .addOnSuccessListener { objects ->
                currentObj = objects.firstOrNull()
            }
            .addOnCompleteListener { checkDone() }

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                currentText = visionText.text
            }
            .addOnCompleteListener { checkDone() }

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                currentBarcode = barcodes.firstOrNull()?.rawValue
            }
            .addOnCompleteListener { checkDone() }
    } else {
        imageProxy.close()
    }
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
                    "The scan did not yield any results in the local library.",
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
