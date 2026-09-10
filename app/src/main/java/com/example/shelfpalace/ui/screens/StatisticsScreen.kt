package com.example.shelfpalace.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfpalace.R
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    gameRepository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    settingsRepository: SettingsRepository? = null,
    onBack: () -> Unit,
) {
    val disabledIds by (settingsRepository?.disabledIds?.collectAsState(initial = emptySet()) ?: remember { mutableStateOf(emptySet()) })

    val rawGames by gameRepository.getAllGames().collectAsState(initial = emptyList())
    val rawMovies by movieRepository.getAllMovies().collectAsState(initial = emptyList())
    val rawMusic by musicRepository.getAllMusic().collectAsState(initial = emptyList())

    val allGames = remember(rawGames, disabledIds) {
        if (disabledIds.contains("media_games")) emptyList()
        else rawGames.filter { game ->
            if (disabledIds.contains(game.platformId)) false
            else {
                val platform = StaticData.platforms.find { it.id == game.platformId }
                platform == null || !disabledIds.contains(platform.manufacturerId)
            }
        }
    }

    val allMovies = remember(rawMovies, disabledIds) {
        if (disabledIds.contains("media_movies")) emptyList()
        else rawMovies.filter { !disabledIds.contains(it.formatId) }
    }

    val allMusic = remember(rawMusic, disabledIds) {
        if (disabledIds.contains("media_music")) emptyList()
        else rawMusic.filter { !disabledIds.contains(it.formatId) }
    }
    
    val manufacturerStats = remember(allGames) {
        val counts = allGames.groupBy { game ->
            StaticData.platforms.find { it.id == game.platformId }?.manufacturerId ?: "unknown"
        }.mapValues { it.value.size }
        
        counts.asSequence().map { (id, count) ->
            val name = StaticData.manufacturers.find { it.id == id }?.name ?: id.uppercase()
            PieChartData(name, count.toFloat(), getManufacturerColor(id))
        }.sortedByDescending { it.value }.toList()
    }

    val platformStats = remember(allGames) {
        val counts = allGames.groupBy { it.platformId }.mapValues { it.value.size }
        
        counts.asSequence().map { (id, count) ->
            val baseName = StaticData.platforms.find { it.id == id }?.name ?: id
            val name = if (id == "nintendo_wiiu") "Nintendo Wii U" else baseName
            val color = when (id) {
                "nintendo_3ds" -> Color(0xFF40E0D0) // Turquoise
                "nintendo_wiiu" -> Color(0xFF00AEEF) // Specified blue
                else -> getRandomNeonColor(id)
            }
            PieChartData(name, count.toFloat(), color)
        }.sortedByDescending { it.value }.toList()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    NeonHeader(
                        text = "COLLECTION STATS",
                        fullWidth = false,
                    ) 
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (allGames.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "NO DATA TO DISPLAY",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }
            } else {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(500))
                ) {
                    val color = MaterialTheme.colorScheme.primary
                    Column {
                        CompactSectionHeader(text = "Games by Manufacturer", color = color)
                        NeonCard(
                            modifier = Modifier.fillMaxWidth(),
                            color = color,
                            containerAlpha = 0.6f,
                            padding = 8.dp
                        ) {
                            PieChart(data = manufacturerStats)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 150)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = 150))
                ) {
                    val color = MaterialTheme.colorScheme.primary
                    Column {
                        CompactSectionHeader(text = "Games by Console", color = color)
                        NeonCard(
                            modifier = Modifier.fillMaxWidth(),
                            color = color,
                            containerAlpha = 0.6f,
                            padding = 8.dp
                        ) {
                            PieChart(data = platformStats)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Summary Counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val summaryItems = listOf(
                        Triple("GAMES", allGames.size, primaryColor),
                        Triple("MOVIES", allMovies.size, secondaryColor),
                        Triple("MUSIC", allMusic.size, secondaryColor)
                    )

                    summaryItems.forEach { (label, count, color) ->
                        NeonCard(
                            modifier = Modifier.weight(1f),
                            color = color,
                            containerAlpha = 0.4f
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = color.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PieChartData(val name: String, val value: Float, val color: Color)

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    
    val animateFloat = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. The 3D Donut Chart
        Box(
            modifier = Modifier
                .size(100.dp)
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val depth = 12.dp.toPx()
                val thickness = 22.dp.toPx()
                val chartSize = Size(canvasWidth - thickness, canvasHeight - thickness)
                val chartOffset = androidx.compose.ui.geometry.Offset(thickness / 2, thickness / 2)
                
                withTransform({
                    scale(scaleX = 1f, scaleY = 0.75f, pivot = center)
                }) {
                    // 0. Draw a soft "ground" shadow for the whole donut
                    drawArc(
                        color = Color.Black.copy(alpha = 0.3f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = chartOffset + androidx.compose.ui.geometry.Offset(0f, depth + 4.dp.toPx()),
                        size = chartSize,
                        style = Stroke(width = thickness)
                    )

                    // 1. Draw the 3D Depth (Shadows/Sides)
                    var currentAngle = -90f
                    data.forEach { slice ->
                        val sweepAngle = (slice.value / total) * 360f * animateFloat.value
                        if (sweepAngle > 0f) {
                            val depthColor = slice.color.copy(
                                red = slice.color.red * 0.3f,
                                green = slice.color.green * 0.3f,
                                blue = slice.color.blue * 0.3f,
                                alpha = 0.9f
                            )
                            
                            for (i in 1..depth.toInt() step 2) {
                                drawArc(
                                    color = depthColor,
                                    startAngle = currentAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = chartOffset + androidx.compose.ui.geometry.Offset(0f, i.toFloat()),
                                    size = chartSize,
                                    style = Stroke(width = thickness)
                                )
                            }
                        }
                        currentAngle += (slice.value / total) * 360f
                    }

                    // 2. Draw the Top Layer
                    currentAngle = -90f
                    data.forEach { slice ->
                        val sweepAngle = (slice.value / total) * 360f * animateFloat.value
                        if (sweepAngle > 0f) {
                            drawArc(
                                color = slice.color,
                                startAngle = currentAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = chartOffset,
                                size = chartSize,
                                style = Stroke(width = thickness)
                            )
                            
                            drawArc(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                                    center = androidx.compose.ui.geometry.Offset(canvasWidth / 2, canvasHeight / 2),
                                    radius = canvasWidth / 2
                                ),
                                startAngle = currentAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = chartOffset,
                                size = chartSize,
                                style = Stroke(width = thickness)
                            )
                        }
                        currentAngle += (slice.value / total) * 360f
                    }
                    
                    // 3. Draw a glossy highlight over the whole donut
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                            startY = 0f,
                            endY = canvasHeight
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = chartOffset,
                        size = chartSize,
                        style = Stroke(width = thickness)
                    )
                }
            }
        }

        // 2. The Compact Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val legendColor = MaterialTheme.colorScheme.primary
            data.forEach { slice ->
                val percentage = ((slice.value / total) * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(slice.color, getAppCorners(2.dp))
                                .then(if (percentage > 0) Modifier.neonGlow(slice.color, 1.dp) else Modifier)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = slice.name.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = legendColor,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${slice.value.toInt()} ($percentage%)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = slice.color
                    )
                }
            }
        }
    }
}

fun getManufacturerColor(id: String): Color {
    return when (id) {
        "sony" -> Color(0xFF2E6FF2)      // Brighter PlayStation Blue
        "nintendo" -> Color(0xFFFF2D44)  // Neon Nintendo Red
        "microsoft" -> Color(0xFF107C10) // Xbox Green
        "sega" -> Color(0xFF00A3FF)      // Brighter SEGA Blue
        else -> Color.Gray
    }
}

fun getRandomNeonColor(seed: String): Color {
    val colors = listOf(
        Color(0xFF00F3FF), // Cyan
        Color(0xFFFF00FF), // Magenta
        Color(0xFFADFF2F), // Green
        Color(0xFFFFD700), // Gold
        Color(0xFFFF4500), // OrangeRed
        Color(0xFF7B68EE), // MediumSlateBlue
        Color(0xFF00FF7F), // SpringGreen
        Color(0xFFFF69B4)  // HotPink
    )
    val index = seed.hashCode().let { if (it < 0) -it else it } % colors.size
    return colors[index]
}
