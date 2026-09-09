package com.example.shelfpalace.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.shelfpalace.data.BackupData
import com.example.shelfpalace.data.Game
import com.example.shelfpalace.data.Movie
import com.example.shelfpalace.data.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.UUID
import java.util.zip.ZipEntry

object StorageUtil {

    private const val MAX_IMAGE_DIMENSION = 1024
    private const val COMPRESSION_QUALITY = 80

    fun saveImageToShelfPalaceDir(context: Context, sourceUri: Uri): Uri? {
        return try {
            val compressedBytes = compressImageFromUri(context, sourceUri)
            val fileName = "cover_${UUID.randomUUID()}.webp"
            
            if (compressedBytes != null) {
                saveByteArray(context, compressedBytes, fileName)
            } else {
                // Fallback to original stream if compression fails
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                saveInputStream(context, inputStream, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadAndSaveImage(context: Context, urlString: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.doInput = true
            val inputStream = connection.getInputStream()
            val fileName = "cover_${UUID.randomUUID()}.webp"
            saveInputStream(context, inputStream, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToShelfPalaceDir(context: Context, bitmap: Bitmap, oldUriString: String? = null): Uri? {
        return try {
            val fileName = "cover_${UUID.randomUUID()}.webp"
            val resizedBitmap = resizeBitmapIfNeeded(bitmap)
            val outputStream = java.io.ByteArrayOutputStream()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, COMPRESSION_QUALITY, outputStream)
            } else {
                @Suppress("DEPRECATION")
                resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP, COMPRESSION_QUALITY, outputStream)
            }
            
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            
            val inputStream = java.io.ByteArrayInputStream(outputStream.toByteArray())
            val newUri = saveInputStream(context, inputStream, fileName)
            
            // Cleanup old image if successfully saved new one
            if (newUri != null && oldUriString != null) {
                deleteImage(context, oldUriString)
            }
            
            newUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun cropBitmap(bitmap: android.graphics.Bitmap, rect: android.graphics.RectF): android.graphics.Bitmap {
        val left = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (rect.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (rect.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        
        val width = right - left
        val height = bottom - top
        
        return android.graphics.Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): android.graphics.Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { 
                android.graphics.BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createImageUri(context: Context): Uri? {
        return try {
            val fileName = "cover_${UUID.randomUUID()}.webp"
            // Use cacheDir for temporary capture - more reliable and doesn't need permissions
            val tempDir = File(context.cacheDir, "captured_images")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            val imageFile = File(tempDir, fileName)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun writeToUri(context: Context, uri: Uri, content: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteImage(context: Context, uriString: String) {
        if (uriString.isEmpty()) return
        try {
            val uri = Uri.parse(uriString)
            // Handle MediaStore URIs (Android 10+)
            if (uri.scheme == "content") {
                context.contentResolver.delete(uri, null, null)
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createBackupZip(context: Context, backupData: BackupData, includeImages: Boolean): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val outputStream = java.io.ByteArrayOutputStream()
            val zipOutputStream = java.util.zip.ZipOutputStream(outputStream)
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

            fun backupImages(items: List<Any>): List<Any> {
                if (!includeImages) return items
                return items.map { item ->
                    val coverUri = when (item) {
                        is Game -> item.coverUri
                        is Movie -> item.coverUri
                        is Music -> item.coverUri
                        else -> ""
                    }

                    if (coverUri.isNotEmpty()) {
                        val uri = android.net.Uri.parse(coverUri)
                        val fileName = uri.lastPathSegment
                        if (fileName != null) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    val zipEntry = java.util.zip.ZipEntry("images/$fileName")
                                    // Handle duplicate entries if multiple items use the same image
                                    try {
                                        zipOutputStream.putNextEntry(zipEntry)
                                        input.copyTo(zipOutputStream)
                                        zipOutputStream.closeEntry()
                                    } catch (e: java.util.zip.ZipException) {
                                        // Entry already exists, skip
                                    }
                                }
                                return@map when (item) {
                                    is Game -> item.copy(coverUri = fileName)
                                    is Movie -> item.copy(coverUri = fileName)
                                    is com.example.shelfpalace.data.Music -> item.copy(coverUri = fileName)
                                    else -> item
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    item
                }
            }

            @Suppress("UNCHECKED_CAST")
            val gamesForBackup = backupImages(backupData.games) as List<Game>
            @Suppress("UNCHECKED_CAST")
            val moviesForBackup = backupImages(backupData.movies) as List<Movie>
            @Suppress("UNCHECKED_CAST")
            val musicForBackup = backupImages(backupData.music) as List<Music>

            val finalBackupData = backupData.copy(
                games = gamesForBackup,
                movies = moviesForBackup,
                music = musicForBackup
            )

            // Add JSON file
            val backupJsonString = json.encodeToString(BackupData.serializer(), finalBackupData)
            val jsonEntry = ZipEntry("backup.json")
            zipOutputStream.putNextEntry(jsonEntry)
            zipOutputStream.write(backupJsonString.toByteArray())
            zipOutputStream.closeEntry()

            zipOutputStream.close()
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreFromBackupZip(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            var backupJson: String? = null
            val tempImages = mutableMapOf<String, ByteArray>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = java.util.zip.ZipInputStream(inputStream)
                var entry = zipInputStream.getNextEntry()
                while (entry != null) {
                    when {
                        entry.name == "backup.json" || entry.name == "games.json" -> {
                            backupJson = zipInputStream.readBytes().toString(Charsets.UTF_8)
                        }
                        entry.name.startsWith("images/") -> {
                            val fileName = entry.name.substringAfter("images/")
                            if (fileName.isNotEmpty()) {
                                tempImages[fileName] = zipInputStream.readBytes()
                            }
                        }
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.getNextEntry()
                }
            }

            if (backupJson == null) return@withContext null

            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            
            // Handle legacy format (just list of games) or new format (BackupData)
            val backupData = try {
                json.decodeFromString(com.example.shelfpalace.data.BackupData.serializer(), backupJson!!)
            } catch (e: Exception) {
                // Legacy fallback
                val games = json.decodeFromString<List<Game>>(backupJson!!)
                BackupData(games = games)
            }
            
            fun updateUris(items: List<Any>): List<Any> {
                return items.map { item ->
                    val coverUri = when (item) {
                        is com.example.shelfpalace.data.Game -> item.coverUri
                        is com.example.shelfpalace.data.Movie -> item.coverUri
                        is Music -> item.coverUri
                        else -> ""
                    }

                    if (tempImages.containsKey(coverUri)) {
                        val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), coverUri)
                        destFile.writeBytes(tempImages[coverUri]!!)
                        val newUri = Uri.fromFile(destFile).toString()
                        return@map when (item) {
                            is com.example.shelfpalace.data.Game -> item.copy(coverUri = newUri)
                            is com.example.shelfpalace.data.Movie -> item.copy(coverUri = newUri)
                            is Music -> item.copy(coverUri = newUri)
                            else -> item
                        }
                    }
                    item
                }
            }

            @Suppress("UNCHECKED_CAST")
            val finalGames = updateUris(backupData.games) as List<com.example.shelfpalace.data.Game>
            @Suppress("UNCHECKED_CAST")
            val finalMovies = updateUris(backupData.movies) as List<Movie>
            @Suppress("UNCHECKED_CAST")
            val finalMusic = updateUris(backupData.music) as List<Music>

            val finalData = backupData.copy(
                games = finalGames,
                movies = finalMovies,
                music = finalMusic
            )

            json.encodeToString(com.example.shelfpalace.data.BackupData.serializer(), finalData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImageFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            // Check dimensions first without loading full bitmap into memory
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { 
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            }

            // Calculate inSampleSize to resize the image
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            options.inJustDecodeBounds = false

            // Decode the bitmap with inSampleSize
            val bitmap = context.contentResolver.openInputStream(uri)?.use { 
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // Double check if further resizing is needed (inSampleSize is always power of 2)
            val finalBitmap = resizeBitmapIfNeeded(bitmap)
            val outputStream = java.io.ByteArrayOutputStream()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                finalBitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, COMPRESSION_QUALITY, outputStream)
            } else {
                @Suppress("DEPRECATION")
                finalBitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP, COMPRESSION_QUALITY, outputStream)
            }
            
            if (finalBitmap != bitmap) finalBitmap.recycle()
            bitmap.recycle()
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeBitmapIfNeeded(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        if (bitmap.width <= MAX_IMAGE_DIMENSION && bitmap.height <= MAX_IMAGE_DIMENSION) {
            return bitmap
        }

        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val width: Int
        val height: Int

        if (ratio > 1) {
            width = MAX_IMAGE_DIMENSION
            height = (MAX_IMAGE_DIMENSION / ratio).toInt()
        } else {
            height = MAX_IMAGE_DIMENSION
            width = (MAX_IMAGE_DIMENSION * ratio).toInt()
        }

        return android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun saveByteArray(context: Context, data: ByteArray, fileName: String): Uri? {
        return saveToShelfPalaceStorage(context, fileName) { outputStream ->
            outputStream.write(data)
        }
    }

    private fun saveInputStream(context: Context, inputStream: InputStream?, fileName: String): Uri? {
        if (inputStream == null) return null
        return saveToShelfPalaceStorage(context, fileName) { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    private fun saveToShelfPalaceStorage(context: Context, fileName: String, writeBlock: (OutputStream) -> Unit): Uri? {
        return try {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (directory != null && !directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                writeBlock(outputStream)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
