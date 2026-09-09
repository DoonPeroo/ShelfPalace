package com.example.shelfpalace.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.util.PlatformUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformScreen(
    manufacturerId: String,
    repository: GameRepository,
    settingsRepository: SettingsRepository,
    onPlatformSelected: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
    val platforms = StaticData.platforms.filter { 
        it.manufacturerId == manufacturerId && !disabledIds.contains(it.id)
    }
    val allGames by repository.getAllGames().collectAsState(initial = emptyList())
    
    val platformCounts = remember(allGames) {
        allGames.groupBy { it.platformId }.mapValues { it.value.size }
    }

    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    NeonHeader(
                        text = stringResource(R.string.header_select_platform),
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
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(platforms) { platform ->
                    val count = platformCounts[platform.id] ?: 0
                    val cardColor = MaterialTheme.colorScheme.secondary
                    NeonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable { onPlatformSelected(platform.id) },
                        color = cardColor
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = platform.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                            
                            val borderRadius = 8.dp
                            Surface(
                                color = cardColor.copy(alpha = 0.2f),
                                shape = getAppCorners(borderRadius),
                                border = BorderStroke(1.dp, cardColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                            ) {
                                Text(
                                    text = "$count ${if (count == 1) "GAME" else "GAMES"}",
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
