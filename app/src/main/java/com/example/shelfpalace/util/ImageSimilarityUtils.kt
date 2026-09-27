package com.example.shelfpalace.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sqrt

data class ImageFingerprint(
    val redChannel: FloatArray,
    val greenChannel: FloatArray,
    val blueChannel: FloatArray,
    val dHash: BooleanArray
)

object ImageSimilarityUtils {

    private const val GRID_SIZE = 16

    // Weight map for 16x16 grid to ignore USK / PEGI / ESRB age rating logos in bottom-left corner
    private val CELL_WEIGHTS: FloatArray by lazy {
        val weights = FloatArray(GRID_SIZE * GRID_SIZE)
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                val idx = y * GRID_SIZE + x
                weights[idx] = when {
                    // Bottom-left corner where USK / PEGI / ESRB age rating badges reside
                    x <= 4 && y >= 10 -> 0.02f

                    // Bottom edge center/right
                    y >= 14 -> 0.3f

                    // Top platform header banner (e.g. PS4/PS5/Switch banner)
                    y <= 1 -> 0.4f

                    // Main Artwork & Game Title area (Center & Top-Center)
                    y in 2..10 && x in 2..13 -> 1.4f

                    // Default cover area
                    else -> 1.0f
                }
            }
        }
        weights
    }

    fun computeFingerprint(bitmap: Bitmap): ImageFingerprint {
        val resized = Bitmap.createScaledBitmap(bitmap, GRID_SIZE, GRID_SIZE, true)
        
        val pixels = IntArray(GRID_SIZE * GRID_SIZE)
        resized.getPixels(pixels, 0, GRID_SIZE, 0, 0, GRID_SIZE, GRID_SIZE)

        val red = FloatArray(GRID_SIZE * GRID_SIZE)
        val green = FloatArray(GRID_SIZE * GRID_SIZE)
        val blue = FloatArray(GRID_SIZE * GRID_SIZE)
        val dHash = BooleanArray((GRID_SIZE - 1) * GRID_SIZE)

        var hashIdx = 0
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                val idx = y * GRID_SIZE + x
                val pixel = pixels[idx]

                val r = Color.red(pixel) / 255.0f
                val g = Color.green(pixel) / 255.0f
                val b = Color.blue(pixel) / 255.0f

                red[idx] = r
                green[idx] = g
                blue[idx] = b

                if (x < GRID_SIZE - 1) {
                    val nextPixel = pixels[y * GRID_SIZE + x + 1]
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    val nextLum = 0.299f * (Color.red(nextPixel) / 255.0f) + 0.587f * (Color.green(nextPixel) / 255.0f) + 0.114f * (Color.blue(nextPixel) / 255.0f)
                    dHash[hashIdx++] = lum > nextLum
                }
            }
        }

        if (resized != bitmap) {
            resized.recycle()
        }

        return ImageFingerprint(red, green, blue, dHash)
    }

    fun calculateSimilarity(fp1: ImageFingerprint, fp2: ImageFingerprint): Double {
        val size = fp1.redChannel.size
        var weightedColorDiffSum = 0.0
        var totalColorWeight = 0.0

        for (i in 0 until size) {
            val w = CELL_WEIGHTS[i]
            val dr = fp1.redChannel[i] - fp2.redChannel[i]
            val dg = fp1.greenChannel[i] - fp2.greenChannel[i]
            val db = fp1.blueChannel[i] - fp2.blueChannel[i]
            val diff = sqrt((dr * dr + dg * dg + db * db).toDouble())

            weightedColorDiffSum += diff * w
            totalColorWeight += w * sqrt(3.0)
        }

        val colorSimilarity = if (totalColorWeight > 0) {
            (1.0 - (weightedColorDiffSum / totalColorWeight)).coerceIn(0.0, 1.0)
        } else 0.0

        var weightedDHashMatches = 0.0
        var totalDHashWeight = 0.0

        var hashIdx = 0
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE - 1) {
                val cellIdx = y * GRID_SIZE + x
                val w = CELL_WEIGHTS[cellIdx]

                if (fp1.dHash[hashIdx] == fp2.dHash[hashIdx]) {
                    weightedDHashMatches += w
                }
                totalDHashWeight += w
                hashIdx++
            }
        }

        val hashSimilarity = if (totalDHashWeight > 0) {
            (weightedDHashMatches / totalDHashWeight).coerceIn(0.0, 1.0)
        } else 0.0

        return 0.5 * colorSimilarity + 0.5 * hashSimilarity
    }

    suspend fun loadBitmapFromUriOrUrl(context: Context, uriString: String): Bitmap? = withContext(Dispatchers.IO) {
        if (uriString.isBlank()) return@withContext null
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "http" || uri.scheme == "https") {
                val connection = URL(uriString).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.doInput = true
                connection.connect()
                connection.inputStream.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
