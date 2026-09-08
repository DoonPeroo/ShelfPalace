package com.example.retrovault.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.retrovault.R
import com.example.retrovault.data.SettingsRepository
import com.example.retrovault.data.StaticData
import com.example.retrovault.ui.theme.*
import com.example.retrovault.ui.components.*
import com.example.retrovault.util.PlatformUtils

@Composable
fun ManufacturerScreen(
    settingsRepository: SettingsRepository,
    onManufacturerSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onMoviesClick: () -> Unit,
) {
    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
    val manufacturers = StaticData.manufacturers.filter { !disabledIds.contains(it.id) }
    val isMoviesEnabled = !disabledIds.contains("media_movies")
    val isMusicEnabled = !disabledIds.contains("media_music")
    val isMediaEnabled = isMoviesEnabled || isMusicEnabled

    Scaffold(
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                RetroVaultLogo()

                // Section Toggle (Games / Media)
                if (isMediaEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeonButton(
                            text = stringResource(R.string.header_games),
                            onClick = { /* Already here */ },
                            modifier = Modifier.weight(1f),
                            height = 44.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        NeonButton(
                            text = stringResource(R.string.header_media),
                            onClick = onMoviesClick,
                            modifier = Modifier.weight(1f),
                            height = 44.dp,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Styled Search Bar - Matching Screenshot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                        .background(Color.Black.copy(alpha = 0.4f), getAppCorners(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), getAppCorners(12.dp))
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
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "SEARCH GAMES...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(vertical = 12.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        )

                        IconButton(
                            onClick = onScanClick,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = stringResource(R.string.content_desc_scan_game),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                NeonHeader(
                    text = stringResource(R.string.header_select_manufacturer),
                    color = MaterialTheme.colorScheme.primary
                )
                
                val columns = if (manufacturers.size == 1) GridCells.Fixed(1) else GridCells.Fixed(2)
                val gridModifier = if (manufacturers.size == 1) {
                    Modifier.fillMaxWidth(0.6f).align(Alignment.CenterHorizontally)
                } else {
                    Modifier.fillMaxWidth()
                }

                LazyVerticalGrid(
                    columns = columns,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = gridModifier
                ) {
                    items(manufacturers) { manufacturer ->
                        NeonCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clickable { onManufacturerSelected(manufacturer.id) },
                            color = MaterialTheme.colorScheme.primary,
                            containerAlpha = 0.3f
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = manufacturer.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
