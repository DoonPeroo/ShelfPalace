package com.example.retrovault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.retrovault.R
import com.example.retrovault.data.Movie
import com.example.retrovault.data.MovieRepository
import com.example.retrovault.data.StaticData
import com.example.retrovault.ui.components.*
import com.example.retrovault.ui.theme.SynthwaveCyan
import com.example.retrovault.ui.theme.SynthwaveDark
import com.example.retrovault.util.DateUtils
import com.example.retrovault.util.StorageUtil
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
import com.example.retrovault.util.StorageUtil.cropBitmap
import com.example.retrovault.util.StorageUtil.loadBitmapFromUri
import com.example.retrovault.util.StorageUtil.saveBitmapToRetroVaultDir
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEditMovieScreen(
    formatId: String?,
    movieId: String?,
    repository: MovieRepository,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
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
    var dateAdded by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var tempImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var bitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var shouldLaunchCamera by rememberSaveable { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    var selectedDay by rememberSaveable { mutableStateOf("") }
    var selectedMonth by rememberSaveable { mutableStateOf("") }
    var selectedYear by rememberSaveable { mutableStateOf("") }
    var dayExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val isEditMode = movieId != null

    val days = remember { (1..31).map { it.toString().padStart(2, '0') } }
    val months = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val years = remember { 
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        (currentYear downTo 1920).map { it.toString() }
    }

    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val savedUri = withContext(Dispatchers.IO) {
                    StorageUtil.saveImageToRetroVaultDir(context, uri)
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

    LaunchedEffect(movieId) {
        if (isEditMode) {
            val movie = repository.getMovieById(movieId)
            movie?.let {
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
                
                if (it.releaseDate.isNotEmpty()) {
                    displayDate = DateUtils.formatDisplayDate(it.releaseDate)
                    val parts = it.releaseDate.split("-")
                    if (parts.size == 3) {
                        selectedYear = parts[0]
                        selectedMonth = parts[1]
                        selectedDay = parts[2]
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
                .padding(16.dp)
        ) {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.label_title)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = getAppCorners(8.dp),
                        colors = synthwaveTextFieldColors()
                    )
                    
                    Text(
                        text = stringResource(R.string.label_release_date).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Day Dropdown
                        ExposedDropdownMenuBox(
                            expanded = dayExpanded,
                            onExpandedChange = { dayExpanded = !dayExpanded },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            OutlinedTextField(
                                value = selectedDay,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("DD", fontSize = 12.sp) },
                                colors = synthwaveTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = getAppCorners(8.dp),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = dayExpanded,
                                onDismissRequest = { dayExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                days.forEach { dayOption ->
                                    DropdownMenuItem(
                                        text = { Text(text = dayOption, color = Color.White) },
                                        onClick = {
                                            selectedDay = dayOption
                                            dayExpanded = false
                                            if (selectedDay.isNotEmpty() && selectedMonth.isNotEmpty() && selectedYear.isNotEmpty()) {
                                                releaseDate = "$selectedYear-$selectedMonth-$selectedDay"
                                                displayDate = DateUtils.formatDisplayDate(releaseDate)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Month Dropdown
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = !monthExpanded },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            OutlinedTextField(
                                value = selectedMonth,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("MM", fontSize = 12.sp) },
                                colors = synthwaveTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = getAppCorners(8.dp),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                months.forEach { monthOption ->
                                    DropdownMenuItem(
                                        text = { Text(text = monthOption, color = Color.White) },
                                        onClick = {
                                            selectedMonth = monthOption
                                            monthExpanded = false
                                            if (selectedDay.isNotEmpty() && selectedMonth.isNotEmpty() && selectedYear.isNotEmpty()) {
                                                releaseDate = "$selectedYear-$selectedMonth-$selectedDay"
                                                displayDate = DateUtils.formatDisplayDate(releaseDate)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Year Dropdown
                        ExposedDropdownMenuBox(
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = !yearExpanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = selectedYear,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("YYYY", fontSize = 12.sp) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded)
                                },
                                colors = synthwaveTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = getAppCorners(8.dp),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, getAppCorners(8.dp))
                            ) {
                                years.forEach { yearOption ->
                                    DropdownMenuItem(
                                        text = { Text(text = yearOption, color = Color.White) },
                                        onClick = {
                                            selectedYear = yearOption
                                            yearExpanded = false
                                            if (selectedDay.isNotEmpty() && selectedMonth.isNotEmpty() && selectedYear.isNotEmpty()) {
                                                releaseDate = "$selectedYear-$selectedMonth-$selectedDay"
                                                displayDate = DateUtils.formatDisplayDate(releaseDate)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

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
                            icon = Icons.Rounded.PhotoCamera,
                            onClick = { 
                                shouldLaunchCamera = true
                                if (!cameraPermissionState.status.isGranted) {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            height = 56.dp
                        )

                        NeonButton(
                            text = stringResource(R.string.action_select),
                            icon = Icons.Rounded.AddAPhoto,
                            onClick = { 
                                pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            height = 56.dp
                        )
                    }
                }
            }
            
            if (coverUri.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
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
                    color = MaterialTheme.colorScheme.primary
                ) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = "Cover Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(getAppCorners(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            NeonButton(
                text = stringResource(R.string.action_save_movie),
                onClick = {
                    scope.launch {
                        val movie = Movie(
                            id = movieId ?: UUID.randomUUID().toString(),
                            formatId = currentFormatId,
                            title = title,
                            releaseDate = releaseDate,
                            genre = genre,
                            director = director,
                            cast = cast,
                            description = description,
                            coverUri = coverUri,
                            isFavorite = isFavorite,
                            dateAdded = dateAdded
                        )
                        if (isEditMode) {
                            repository.updateMovie(movie)
                        } else {
                            repository.insertMovie(movie)
                        }
                        onSave()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp
            )
        }
    }

    if (showCropDialog && bitmapToCrop != null) {
        ImageCropDialog(
            bitmap = bitmapToCrop!!,
            onCropConfirmed = { rect ->
                scope.launch {
                    val oldUri = coverUri
                    val cropped = withContext(Dispatchers.IO) {
                        val croppedBitmap = cropBitmap(bitmapToCrop!!, rect)
                        saveBitmapToRetroVaultDir(context, croppedBitmap, oldUri)
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
