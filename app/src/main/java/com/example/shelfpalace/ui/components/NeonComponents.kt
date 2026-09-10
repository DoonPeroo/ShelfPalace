package com.example.shelfpalace.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import android.graphics.Bitmap
import android.graphics.RectF
import com.example.shelfpalace.util.StorageUtil
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.data.CornerStyle
import com.example.shelfpalace.data.Game
import com.example.shelfpalace.data.Movie
import com.example.shelfpalace.data.Music
import com.example.shelfpalace.data.SortOption
import com.example.shelfpalace.ui.theme.LocalCornerStyle
import com.example.shelfpalace.ui.theme.SynthwaveDark
import com.example.shelfpalace.ui.theme.SynthwaveLavender
import com.example.shelfpalace.util.PlatformUtils

@Composable
fun getAppCorners(default: androidx.compose.ui.unit.Dp = 8.dp): RoundedCornerShape {
    return if (LocalCornerStyle.current == CornerStyle.ROUNDED) RoundedCornerShape(default) else RoundedCornerShape(0.dp)
}

@Composable
fun getAppCornerRadius(default: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    return if (LocalCornerStyle.current == CornerStyle.ROUNDED) default else 0.dp
}

@Composable
fun CompactSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        ),
        color = color.copy(alpha = 0.9f),
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun ShelfPalaceLogo(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )

    Image(
        painter = painterResource(id = R.drawable.app_logo),
        contentDescription = "ShelfPalace Logo",
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(top = 0.dp, bottom = 0.dp)
            .scale(scale)
    )
}
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
    height: Dp = 52.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
) {
    val radius = 12.dp
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Use a slightly darker shade of Lavender for the pressed state highlight
    val pressedHighlight = Color(0xFF8A91AB) 
    
    // If the button color is white/grayish, make it pop with the highlight color when pressed
    val isNeutral = Math.abs(color.red - color.green) < 0.1 && Math.abs(color.green - color.blue) < 0.1
    val highlightColor = if (isPressed && isNeutral) pressedHighlight else color
    
    val finalContainerColor = if (isPressed) highlightColor.copy(alpha = 0.15f) else containerColor
    val finalContentColor = if (isPressed) highlightColor else color
    val finalBorderColor = if (isPressed) highlightColor else color.copy(alpha = 0.5f)

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = finalContainerColor,
            contentColor = finalContentColor
        ),
        shape = getAppCorners(radius),
        border = BorderStroke(if (isPressed) 1.5.dp else 1.dp, finalBorderColor),
        contentPadding = contentPadding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null || iconPainter != null) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            if (text.isNotEmpty()) {
                Text(
                    text = text.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun NeonIconButton(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
    contentDescription: String? = null,
    tint: Color? = null,
    iconSize: Dp = 26.dp,
    iconModifier: Modifier = Modifier
) {
    val radius = 12.dp
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressedHighlight = Color(0xFF8A91AB)
    val isNeutral = Math.abs(color.red - color.green) < 0.1 && Math.abs(color.green - color.blue) < 0.1
    val highlightColor = if (isPressed && isNeutral) pressedHighlight else color

    val finalContainerColor = if (isPressed) highlightColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    val finalContentColor = if (isPressed) highlightColor else color
    val finalBorderColor = if (isPressed) highlightColor else color.copy(alpha = 0.5f)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.size(size),
        color = finalContainerColor,
        contentColor = finalContentColor,
        shape = getAppCorners(radius),
        border = BorderStroke(if (isPressed) 1.5.dp else 1.dp, finalBorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize).then(iconModifier)
                )
            } else if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = contentDescription,
                    tint = tint ?: Color.Unspecified,
                    modifier = Modifier.size(iconSize).then(iconModifier)
                )
            }
        }
    }
}

@Composable
fun SortIconButton(
    currentSortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    showConsoleSort: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        NeonIconButton(
            iconPainter = painterResource(id = R.drawable.sort),
            onClick = { expanded = true },
            color = color,
            contentDescription = "Sort",
            tint = Color.Unspecified,
            iconSize = 32.dp
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .border(1.dp, color, getAppCorners(8.dp))
        ) {
            DropdownMenuItem(
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (currentSortOption == SortOption.NAME) color else Color.Transparent,
                                    getAppCorners(2.dp)
                                )
                                .border(1.dp, if (currentSortOption == SortOption.NAME) color else Color.White.copy(alpha = 0.5f), getAppCorners(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "SORT BY NAME", 
                            color = if (currentSortOption == SortOption.NAME) color else Color.White, 
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                onClick = {
                    onSortOptionSelected(SortOption.NAME)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (currentSortOption == SortOption.RELEASE_DATE) color else Color.Transparent,
                                    getAppCorners(2.dp)
                                )
                                .border(1.dp, if (currentSortOption == SortOption.RELEASE_DATE) color else Color.White.copy(alpha = 0.5f), getAppCorners(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "SORT BY RELEASE DATE", 
                            color = if (currentSortOption == SortOption.RELEASE_DATE) color else Color.White, 
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                onClick = {
                    onSortOptionSelected(SortOption.RELEASE_DATE)
                    expanded = false
                }
            )
            if (showConsoleSort) {
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (currentSortOption == SortOption.PLATFORM) color else Color.Transparent,
                                        getAppCorners(2.dp)
                                    )
                                    .border(1.dp, if (currentSortOption == SortOption.PLATFORM) color else Color.White.copy(alpha = 0.5f), getAppCorners(2.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "SORT BY CONSOLE", 
                                color = if (currentSortOption == SortOption.PLATFORM) color else Color.White, 
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    onClick = {
                        onSortOptionSelected(SortOption.PLATFORM)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NeonBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    NeonIconButton(
        iconPainter = painterResource(id = R.drawable.back),
        onClick = onClick,
        modifier = modifier,
        color = color,
        size = 40.dp,
        contentDescription = "Back"
    )
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    containerAlpha: Float = 0.45f,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val radius = 12.dp
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = containerAlpha), getAppCorners(radius))
            .border(1.dp, color.copy(alpha = 0.4f), getAppCorners(radius))
            .padding(padding)
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    val radius = 8.dp
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = getAppCorners(radius),
        border = BorderStroke(1.dp, color),
        modifier = modifier
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = color
        )
    }
}

@Composable
fun NeonHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    fullWidth: Boolean = true
) {
    val radius = 12.dp
    Box(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            shape = getAppCorners(radius),
            border = BorderStroke(1.dp, color.copy(alpha = 0.8f))
        ) {
            Text(
                text = text.uppercase(),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun synthwaveTextFieldColors(
    color: Color = MaterialTheme.colorScheme.primary
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    focusedBorderColor = color,
    unfocusedBorderColor = color,
    focusedLabelColor = color,
    unfocusedLabelColor = color.copy(alpha = 0.6f),
    focusedLeadingIconColor = color,
    focusedTrailingIconColor = color,
    unfocusedLeadingIconColor = color.copy(alpha = 0.6f),
    unfocusedTrailingIconColor = color.copy(alpha = 0.6f),
    cursorColor = color,
    selectionColors = TextSelectionColors(
        handleColor = color,
        backgroundColor = color.copy(alpha = 0.4f)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun synthwaveDatePickerColors(
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = SynthwaveDark
) = DatePickerDefaults.colors(
    containerColor = containerColor,
    titleContentColor = color,
    headlineContentColor = color,
    weekdayContentColor = color.copy(alpha = 0.7f),
    subheadContentColor = color.copy(alpha = 0.7f),
    yearContentColor = Color.White,
    selectedYearContentColor = Color.White,
    selectedYearContainerColor = color,
    dayContentColor = Color.White,
    selectedDayContentColor = Color.White,
    selectedDayContainerColor = color,
    todayContentColor = color,
    todayDateBorderColor = color,
    currentYearContentColor = color,
    navigationContentColor = color
)

@Composable
fun NeonToggle(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 36.dp,
    padding: Dp = 2.dp
) {
    Row(
        modifier = modifier
            .height(height)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), getAppCorners(24.dp))
            .border(1.dp, color.copy(alpha = 0.5f), getAppCorners(24.dp))
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(padding)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            Surface(
                onClick = { onOptionSelected(option) },
                shape = getAppCorners(20.dp),
                color = if (isSelected) color else Color.Transparent,
                contentColor = if (isSelected) Color.Black else color,
                modifier = Modifier.fillMaxHeight().weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NeonAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String,
    confirmText: String = "CONFIRM",
    dismissText: String = "CANCEL",
    color: Color = MaterialTheme.colorScheme.primary,
    confirmColor: Color? = null
) {
    val finalConfirmColor = confirmColor ?: color
    Dialog(onDismissRequest = onDismissRequest) {
        NeonCard(
            color = color,
            containerAlpha = 0.9f,
            padding = 11.dp,
            modifier = Modifier
                .width(282.dp)
                .wrapContentHeight()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = color,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonButton(
                        text = dismissText,
                        onClick = onDismissRequest,
                        color = Color.White.copy(alpha = 0.85f),
                        containerColor = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f),
                        height = 41.dp
                    )
                    
                    NeonButton(
                        text = confirmText,
                        onClick = onConfirm,
                        color = finalConfirmColor,
                        containerColor = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f),
                        height = 41.dp
                    )
                }
            }
        }
    }
}

@Composable
fun GameGridItem(
    game: Game,
    onClick: () -> Unit,
    aspectRatio: Float = 0.7f,
    showFavoriteBadge: Boolean = true
) {
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(getAppCorners(12.dp))
            ) {
                AsyncImage(
                    model = game.coverUri.ifEmpty { "https://via.placeholder.com/150x200?text=${game.title}" },
                    contentDescription = game.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                val shortPlatform = PlatformUtils.getShortPlatformName(game.platformId)
                if (shortPlatform.isNotEmpty() || (game.isFavorite && showFavoriteBadge)) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (game.isFavorite && showFavoriteBadge) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                contentColor = Color.Black,
                                shape = getAppCorners(6.dp),
                                border = BorderStroke(1.dp, Color.Black)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_filled),
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).size(16.dp),
                                    tint = Color(0xFFAD1457)
                                )
                            }
                        }

                        if (shortPlatform.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                contentColor = Color.Black,
                                shape = getAppCorners(6.dp),
                                border = BorderStroke(1.dp, Color.Black)
                            ) {
                                Text(
                                    text = shortPlatform,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = game.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                color = Color.White
            )
        }
    }
}

@Composable
fun MovieGridItem(
    movie: Movie,
    onClick: () -> Unit,
    showFavoriteBadge: Boolean = true
) {
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.secondary
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(getAppCorners(12.dp))
            ) {
                AsyncImage(
                    model = movie.coverUri.ifEmpty { "https://via.placeholder.com/150x214?text=${movie.title}" },
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                val formatLabel = PlatformUtils.getMovieFormatTag(movie.formatId)

                if (formatLabel.isNotEmpty() || (movie.isFavorite && showFavoriteBadge)) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (movie.isFavorite && showFavoriteBadge) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                                contentColor = Color.Black,
                                shape = getAppCorners(6.dp),
                                border = BorderStroke(1.dp, Color.Black)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_filled),
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).size(16.dp),
                                    tint = Color(0xFFAD1457)
                                )
                            }
                        }

                        if (formatLabel.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                                contentColor = Color.Black,
                                shape = getAppCorners(6.dp),
                                border = BorderStroke(1.dp, Color.Black)
                            ) {
                                Text(
                                    text = formatLabel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = movie.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                color = Color.White
            )
        }
    }
}

@Composable
fun MusicGridItem(
    music: Music,
    onClick: () -> Unit,
    showFavoriteBadge: Boolean = true
) {
    val musicColor = MaterialTheme.colorScheme.secondary
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = musicColor
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square for music covers usually
                    .clip(getAppCorners(12.dp))
            ) {
                AsyncImage(
                    model = music.coverUri.ifEmpty { "https://via.placeholder.com/200x200?text=${music.title}" },
                    contentDescription = music.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                val formatLabel = PlatformUtils.getMusicFormatTag(music.formatId)

                if (formatLabel.isNotEmpty() || (music.isFavorite && showFavoriteBadge)) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (music.isFavorite && showFavoriteBadge) {
                            Surface(
                                color = musicColor.copy(alpha = 0.9f),
                                contentColor = Color.Black,
                                shape = getAppCorners(6.dp),
                                border = BorderStroke(1.dp, Color.Black)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_heart_filled),
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).size(16.dp),
                                    tint = Color(0xFFAD1457)
                                )
                            }
                        }

                        if (formatLabel.isNotEmpty()) {
                            Surface(
                                color = musicColor.copy(alpha = 0.9f),
                                contentColor = Color.Black,
                                shape = getAppCorners(6.dp),
                                border = BorderStroke(1.dp, Color.Black)
                            ) {
                                Text(
                                    text = formatLabel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = music.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                color = Color.White
            )
            Text(
                text = music.artist,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LibraryStatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = color.copy(alpha = 0.7f)
        )
    }
}

fun Modifier.neonGlow(
    color: Color,
    borderRadius: Dp = 0.dp,
    blurRadius: Dp = 4.dp
) = this.drawBehind {
    val paint = Paint()
    val frameworkPaint = paint.asFrameworkPaint()
    frameworkPaint.color = Color.Transparent.toArgb()
    frameworkPaint.setShadowLayer(
        blurRadius.toPx(),
        0f, 0f,
        color.copy(alpha = 0.3f).toArgb()
    )
    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = borderRadius.toPx(),
            radiusY = borderRadius.toPx(),
            paint = paint
        )
    }
}

@Composable
fun ImageCropDialog(
    bitmap: Bitmap,
    onCropConfirmed: (RectF) -> Unit,
    onDismiss: () -> Unit
) {
    val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Normalized coordinates (0.0 to 1.0) relative to the image
    var left by remember { mutableStateOf(0.1f) }
    var top by remember { mutableStateOf(0.1f) }
    var right by remember { mutableStateOf(0.9f) }
    var bottom by remember { mutableStateOf(0.9f) }

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            color = MaterialTheme.colorScheme.primary,
            containerAlpha = 0.45f,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            padding = 16.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ADJUST COVER AREA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageAspectRatio.coerceIn(0.5f, 2.0f)) // Prevent extreme ratios from breaking layout
                        .background(Color.Black)
                        .onGloballyPositioned { canvasSize = it.size }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures

                                val dx = dragAmount.x / canvasSize.width
                                val dy = dragAmount.y / canvasSize.height
                                
                                val touchX = change.position.x / canvasSize.width
                                val touchY = change.position.y / canvasSize.height
                                
                                // Detect if we are touching near a corner (edge-based resizing)
                                val threshold = 0.2f
                                val nearLeft = Math.abs(touchX - left) < threshold
                                val nearRight = Math.abs(touchX - right) < threshold
                                val nearTop = Math.abs(touchY - top) < threshold
                                val nearBottom = Math.abs(touchY - bottom) < threshold

                                when {
                                    nearLeft && nearTop -> {
                                        left = (left + dx).coerceIn(0f, right - 0.1f)
                                        top = (top + dy).coerceIn(0f, bottom - 0.1f)
                                    }
                                    nearRight && nearTop -> {
                                        right = (right + dx).coerceIn(left + 0.1f, 1f)
                                        top = (top + dy).coerceIn(0f, bottom - 0.1f)
                                    }
                                    nearLeft && nearBottom -> {
                                        left = (left + dx).coerceIn(0f, right - 0.1f)
                                        bottom = (bottom + dy).coerceIn(top + 0.1f, 1f)
                                    }
                                    nearRight && nearBottom -> {
                                        right = (right + dx).coerceIn(left + 0.1f, 1f)
                                        bottom = (bottom + dy).coerceIn(top + 0.1f, 1f)
                                    }
                                    nearLeft -> left = (left + dx).coerceIn(0f, right - 0.1f)
                                    nearRight -> right = (right + dx).coerceIn(left + 0.1f, 1f)
                                    nearTop -> top = (top + dy).coerceIn(0f, bottom - 0.1f)
                                    nearBottom -> bottom = (bottom + dy).coerceIn(top + 0.1f, 1f)
                                    else -> {
                                        // Move entire selection
                                        val moveX = dx.coerceIn(-left, 1f - right)
                                        val moveY = dy.coerceIn(-top, 1f - bottom)
                                        left += moveX
                                        right += moveX
                                        top += moveY
                                        bottom += moveY
                                    }
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds // Fill the ratio-accurate box
                    )
                    
                        val borderAccent = com.example.shelfpalace.ui.theme.SynthwaveLavender
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        
                        // 1. Dimmed overlay
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(w, 0f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                            
                            moveTo(left * w, top * h)
                            lineTo(right * w, top * h)
                            lineTo(right * w, bottom * h)
                            lineTo(left * w, bottom * h)
                            close()
                        }
                        drawPath(path, Color.Black.copy(alpha = 0.7f))
                        
                        // 2. Selection Border
                        drawRect(
                            color = borderAccent,
                            topLeft = Offset(left * w, top * h),
                            size = androidx.compose.ui.geometry.Size((right - left) * w, (bottom - top) * h),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        
                        // 3. Corner Handles (Visual only, logic is in pointerInput)
                        val handleSize = 6.dp.toPx()
                        val corners = listOf(
                            Offset(left * w, top * h),
                            Offset(right * w, top * h),
                            Offset(left * w, bottom * h),
                            Offset(right * w, bottom * h)
                        )
                        corners.forEach { pos ->
                            drawCircle(borderAccent, radius = handleSize, center = pos)
                            drawCircle(Color.Black, radius = handleSize / 2, center = pos)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "DRAG CORNERS TO RESIZE • CENTER TO MOVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeonButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f),
                        height = 48.dp
                    )
                    NeonButton(
                        text = "DONE",
                        onClick = { 
                            onCropConfirmed(RectF(left, top, right, bottom))
                        },
                        modifier = Modifier.weight(1f),
                        height = 48.dp
                    )
                }
            }
        }
    }
}
