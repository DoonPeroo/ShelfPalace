@file:OptIn(ExperimentalPermissionsApi::class)
package com.example.retrovault.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.retrovault.R
import com.example.retrovault.data.GameRepository
import com.example.retrovault.data.MovieRepository
import com.example.retrovault.data.MusicRepository
import com.example.retrovault.ui.components.*
import com.example.retrovault.ui.theme.SynthwaveCyan
import com.example.retrovault.ui.theme.SynthwaveLavender
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
    
    var detectedObject by remember { mutableStateOf<com.google.mlkit.vision.objects.DetectedObject?>(null) }
    var detectedText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var showNothingFound by remember { mutableStateOf(false) }
    var internetSearching by remember { mutableStateOf(false) }
    
    // Track recently recognized games to simulate variety
    val recognizedHistory = remember { mutableStateListOf<String>() }
    
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
                    processImageProxy(objectDetector, textRecognizer, imageProxy) { obj, text ->
                        detectedObject = obj
                        detectedText = text ?: ""
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
        detectedObject = null
        detectedText = ""
        isScanning = false
        internetSearching = false
        showNothingFound = false
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

        // Overlay for detected object
        val scanAccentColor = SynthwaveLavender
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val cornerLength = 40.dp.toPx()
            val rectWidth = 300.dp.toPx()
            val rectHeight = 400.dp.toPx()
            val left = (size.width - rectWidth) / 2
            val top = (size.height - rectHeight) / 2
            val right = left + rectWidth
            val bottom = top + rectHeight

            // Draw neon corners
            // Top Left
            drawLine(scanAccentColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left + cornerLength, top), strokeWidth)
            drawLine(scanAccentColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + cornerLength), strokeWidth)

            // Top Right
            drawLine(scanAccentColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right - cornerLength, top), strokeWidth)
            drawLine(scanAccentColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right, top + cornerLength), strokeWidth)

            // Bottom Left
            drawLine(scanAccentColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left + cornerLength, bottom), strokeWidth)
            drawLine(scanAccentColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left, bottom - cornerLength), strokeWidth)

            // Bottom Right
            drawLine(scanAccentColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right - cornerLength, bottom), strokeWidth)
            drawLine(scanAccentColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right, bottom - cornerLength), strokeWidth)
            
            // Subtle neon glow for the whole scan area if detected
            if (detectedObject != null || detectedText.isNotBlank()) {
                drawRect(
                    color = scanAccentColor.copy(alpha = 0.1f),
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonBackButton(onClick = onBack)
                
                NeonHeader(
                    text = "RETRO SCANNER",
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

        // Bottom Scan Button and Home Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val canScan = detectedObject != null || detectedText.isNotBlank()

                if (canScan && !isScanning && !internetSearching) {
                    val displayLabel = if (detectedText.isNotBlank()) {
                        val firstLine = detectedText.split("\n").firstOrNull { it.isNotBlank() } ?: ""
                        if (firstLine.length > 20) firstLine.take(20) + "..." else firstLine
                    } else {
                        detectedObject?.labels?.firstOrNull()?.text ?: "Object"
                    }
                    
                    val readingColor = SynthwaveLavender
                    Text(
                        text = "Reading: $displayLabel",
                        color = readingColor,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), getAppCorners(20.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    
                    Button(
                        onClick = {
                            isScanning = true
                            scope.launch {
                                // IMAGE SEARCH LOGIC
                                val words = detectedText.split(Regex("\\s+"))
                                    .map { it.filter { char -> char.isLetterOrDigit() } }
                                    .filter { it.length >= 2 }
                                
                                val games = repository.getAllGames().first()
                                val movies = movieRepository.getAllMovies().first()
                                val musicList = musicRepository.getAllMusic().first()
                                
                                var recognizedGameId: String? = null
                                var recognizedMovieId: String? = null
                                var recognizedMusicId: String? = null

                                if (words.isNotEmpty()) {
                                    val gameMatches = games.filter { game ->
                                        words.any { word -> 
                                            game.title.contains(word, ignoreCase = true) 
                                        }
                                    }
                                    
                                    val movieMatches = movies.filter { movie ->
                                        words.any { word -> 
                                            movie.title.contains(word, ignoreCase = true) 
                                        }
                                    }

                                    val musicMatches = musicList.filter { music ->
                                        words.any { word -> 
                                            music.title.contains(word, ignoreCase = true) || 
                                            music.artist.contains(word, ignoreCase = true)
                                        }
                                    }
                                    
                                    if (gameMatches.isNotEmpty() || movieMatches.isNotEmpty() || musicMatches.isNotEmpty()) {
                                        val bestGame = gameMatches.maxByOrNull { game ->
                                            words.count { word -> game.title.contains(word, ignoreCase = true) }
                                        }
                                        val bestMovie = movieMatches.maxByOrNull { movie ->
                                            words.count { word -> movie.title.contains(word, ignoreCase = true) }
                                        }
                                        val bestMusic = musicMatches.maxByOrNull { music ->
                                            words.count { word -> music.title.contains(word, ignoreCase = true) || music.artist.contains(word, ignoreCase = true) }
                                        }
                                        
                                        val gameScore = bestGame?.let { g -> words.count { w -> g.title.contains(w, ignoreCase = true) } } ?: 0
                                        val movieScore = bestMovie?.let { m -> words.count { w -> m.title.contains(w, ignoreCase = true) } } ?: 0
                                        val musicScore = bestMusic?.let { m -> words.count { w -> m.title.contains(w, ignoreCase = true) || m.artist.contains(w, ignoreCase = true) } } ?: 0
                                        
                                        val maxScore = maxOf(gameScore, movieScore, musicScore)
                                        
                                        if (maxScore > 0) {
                                            if (gameScore == maxScore && bestGame != null) {
                                                recognizedGameId = bestGame.id
                                            } else if (movieScore == maxScore && bestMovie != null) {
                                                recognizedMovieId = bestMovie.id
                                            } else if (bestMusic != null) {
                                                recognizedMusicId = bestMusic.id
                                            }
                                        }
                                    }
                                }

                                if (recognizedGameId != null) {
                                    kotlinx.coroutines.delay(800)
                                    resetScanner()
                                    onGameRecognized(recognizedGameId)
                                } else if (recognizedMovieId != null) {
                                    kotlinx.coroutines.delay(800)
                                    resetScanner()
                                    onMovieRecognized(recognizedMovieId)
                                } else if (recognizedMusicId != null) {
                                    kotlinx.coroutines.delay(800)
                                    resetScanner()
                                    onMusicRecognized(recognizedMusicId)
                                } else {
                                    kotlinx.coroutines.delay(1000)
                                    isScanning = false
                                    showNothingFound = true
                                }
                            }
                        },
                        modifier = Modifier
                            .height(64.dp)
                            .width(200.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.action_scan), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        
        if (isScanning || internetSearching) {
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
                        if (internetSearching) "SEARCHING INTERNET..." else "ANALYZING...",
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
    imageProxy: ImageProxy,
    onDetected: (com.google.mlkit.vision.objects.DetectedObject?, String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        // Use a counter to close imageProxy only after both tasks complete
        var tasksActive = 2
        var currentObj: com.google.mlkit.vision.objects.DetectedObject? = null
        var currentText: String? = null

        fun checkDone() {
            tasksActive--
            if (tasksActive == 0) {
                onDetected(currentObj, currentText)
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
        com.example.retrovault.ui.components.NeonCard(
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
                    "The scan did not yield any results in the local library or the cyberspace.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

