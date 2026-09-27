@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalGetImage::class)
package com.example.shelfpalace.ui.components

import android.Manifest
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import com.example.shelfpalace.ui.theme.SynthwaveLavender
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

import androidx.camera.core.ExperimentalGetImage

@OptIn(ExperimentalPermissionsApi::class, ExperimentalGetImage::class)
@Composable
fun BarcodeScanDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    @OptIn(ExperimentalPermissionsApi::class)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
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
                var candidateBarcode by remember { mutableStateOf("") }
                var candidateCount by remember { mutableStateOf(0) }
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                val previewView = remember { PreviewView(context) }
                var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

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

                DisposableEffect(Unit) {
                    onDispose {
                        cameraExecutor.shutdown()
                    }
                }

                LaunchedEffect(Unit) {
                    try {
                        cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    } catch (e: Exception) {
                        Log.e("BarcodeScanDialog", "Failed to get camera provider", e)
                    }
                }

                @OptIn(ExperimentalGetImage::class)
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
                                if (!isScanned) {
                                    @OptIn(ExperimentalGetImage::class)
                                    processDialogFrame(barcodeScanner, imageProxy) { validBarcode ->
                                        if (validBarcode == candidateBarcode) {
                                            candidateCount++
                                            if (candidateCount >= 2) {
                                                isScanned = true
                                                onBarcodeScanned(validBarcode)
                                            }
                                        } else {
                                            candidateBarcode = validBarcode
                                            candidateCount = 1
                                        }
                                    }
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
                        Log.e("BarcodeScanDialog", "Camera binding failed", e)
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Neon Barcode Frame Overlay
                val scanAccentColor = SynthwaveLavender
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val cornerLength = 40.dp.toPx()
                    val rectWidth = 280.dp.toPx()
                    val rectHeight = 160.dp.toPx()
                    val left = (size.width - rectWidth) / 2
                    val top = (size.height - rectHeight) / 2 - 30.dp.toPx()
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
                    NeonHeader(text = "SCAN BARCODE", fullWidth = false)
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Bottom instruction text
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "HOLD BARCODE IN FRAME",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
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

fun isValidBarcodeChecksum(barcode: String): Boolean {
    val clean = barcode.filter { it.isDigit() }
    if (clean.isBlank()) return false

    if (clean.length == 13) {
        var sum = 0
        for (i in 0 until 12) {
            val digit = clean[i] - '0'
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == (clean[12] - '0')
    }

    if (clean.length == 12) {
        var sum = 0
        for (i in 0 until 11) {
            val digit = clean[i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == (clean[11] - '0')
    }

    if (clean.length == 8) {
        var sum = 0
        for (i in 0 until 7) {
            val digit = clean[i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == (clean[7] - '0')
    }

    return clean.length in 6..18
}

@ExperimentalGetImage
private fun processDialogFrame(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeFound: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val validBarcode = barcodes.mapNotNull { it.rawValue?.trim() }
                    .firstOrNull { code ->
                        val clean = code.filter { it.isDigit() }
                        clean.length >= 6 && isValidBarcodeChecksum(code)
                    }

                if (!validBarcode.isNullOrBlank()) {
                    onBarcodeFound(validBarcode)
                }
            }
            .addOnCompleteListener {
                try { imageProxy.close() } catch (_: Exception) {}
            }
    } else {
        imageProxy.close()
    }
}
