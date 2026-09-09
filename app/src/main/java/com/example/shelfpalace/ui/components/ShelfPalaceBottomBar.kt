package com.example.shelfpalace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfpalace.R

enum class NavTab { LIBRARY, FAVORITES, STATISTICS, SETTINGS, NONE }

@Composable
fun ShelfPalaceBottomBar(
    onLibraryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    activeTab: NavTab,
    isAddActive: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        color = Color.Black.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem(
                icon = Icons.Rounded.GridView,
                label = stringResource(R.string.nav_library),
                selected = activeTab == NavTab.LIBRARY,
                onClick = onLibraryClick,
                selectedBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00FF88),
                        Color(0xFF00E5FF),
                        Color(0xFF007BFF)
                    )
                )
            )
            
            BottomNavItem(
                iconPainter = painterResource(id = R.drawable.ic_heart_filled),
                label = stringResource(R.string.nav_favorites),
                selected = activeTab == NavTab.FAVORITES,
                onClick = onFavoriteClick,
                selectedColor = Color(0xFFAD1457)
            )
            
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            val addButtonBgColor = Color(0xFF252D3A)
            val addButtonBorderColor = Color(0xFF7F8B9C)
            val primaryColor = MaterialTheme.colorScheme.primary
            
            val isHighlighted = isPressed || isAddActive

            Box(
                modifier = Modifier
                    .size(80.dp, 48.dp)
                    .background(
                        if (isHighlighted) primaryColor.copy(alpha = 0.3f) else addButtonBgColor.copy(alpha = 0.6f), 
                        getAppCorners(12.dp)
                    )
                    .border(
                        1.5.dp, 
                        if (isHighlighted) primaryColor else addButtonBorderColor.copy(alpha = 0.5f), 
                        getAppCorners(12.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onAddClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            BottomNavItem(
                icon = Icons.Rounded.BarChart,
                label = stringResource(R.string.nav_statistics),
                selected = activeTab == NavTab.STATISTICS,
                onClick = onStatisticsClick,
                selectedBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFF00FF),
                        Color(0xFFFFFF00),
                        Color(0xFF00E5FF)
                    )
                )
            )
            
            BottomNavItem(
                icon = Icons.Rounded.Settings,
                label = stringResource(R.string.nav_settings),
                selected = activeTab == NavTab.SETTINGS,
                onClick = onSettingsClick,
                selectedBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFB0BEC5),
                        Color(0xFFFF9800),
                        Color(0xFFFFC107)
                    )
                )
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    selectedBrush: Brush? = null
) {
    val standardSelectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = Color.White.copy(alpha = 0.7f)
    
    val iconColor = if (selected) selectedColor else unselectedColor
    val textColor = if (selected) standardSelectedColor else unselectedColor
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(getAppCorners(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val iconModifier = Modifier.size(26.dp)
        
        if (selected && selectedBrush != null) {
            val painter = if (iconPainter != null) iconPainter else {
                rememberVectorPainter(icon!!)
            }
            Icon(
                painter = painter,
                contentDescription = label,
                tint = Color.White,
                modifier = iconModifier
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = selectedBrush, blendMode = BlendMode.SrcIn)
                    }
            )
        } else {
            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = iconModifier
                )
            } else {
                Icon(
                    imageVector = icon!!,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = iconModifier
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            ),
            color = textColor
        )
    }
}
