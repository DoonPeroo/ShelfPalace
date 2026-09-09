package com.example.shelfpalace.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfpalace.R
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieFormatScreen(
    repository: MovieRepository,
    settingsRepository: SettingsRepository,
    onFormatSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
    val formats = StaticData.movieFormats.filter { !disabledIds.contains(it.id) }
    val allMovies by repository.getAllMovies().collectAsState(initial = emptyList())
    
    val formatCounts = remember(allMovies) {
        allMovies.groupBy { it.formatId }.mapValues { it.value.size }
    }

    val accentColor = MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    NeonHeader(
                        text = stringResource(R.string.header_select_format),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fullWidth = false,
                        color = accentColor
                    )
                },
                navigationIcon = {
                    NeonBackButton(
                        onClick = onBack, 
                        modifier = Modifier.padding(start = 8.dp), 
                        color = accentColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Movie Search Bar with Scan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), getAppCorners(8.dp))
                    .border(1.dp, accentColor, getAppCorners(8.dp))
                    .clickable { onSearchClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "SEARCH MOVIES...".uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentColor.copy(alpha = 0.6f)
                            )
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 12.dp),
                        color = accentColor.copy(alpha = 0.3f)
                    )

                    IconButton(
                        onClick = onScanClick,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "Scan Movie",
                            tint = accentColor
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(formats) { format ->
                    val count = formatCounts[format.id] ?: 0
                    NeonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable { onFormatSelected(format.id) },
                        color = accentColor
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val textColor = Color.White
                            Text(
                                text = format.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                            
                            val badgeBorderRadius = 8.dp
                            Surface(
                                color = accentColor.copy(alpha = 0.2f),
                                shape = getAppCorners(badgeBorderRadius),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                            ) {
                                Text(
                                    text = "$count ${if (count == 1) "MOVIE" else "MOVIES"}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    ),
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
