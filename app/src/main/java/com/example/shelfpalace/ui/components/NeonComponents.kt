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
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.ui.platform.LocalContext
import com.example.shelfpalace.util.StorageUtil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shelfpalace.R
import com.example.shelfpalace.data.AppTheme
import com.example.shelfpalace.data.CornerStyle
import com.example.shelfpalace.data.Game
import com.example.shelfpalace.data.Movie
import com.example.shelfpalace.data.Music
import com.example.shelfpalace.data.SortOption
import com.example.shelfpalace.ui.theme.LocalAppTheme
import com.example.shelfpalace.ui.theme.LocalCornerStyle
import com.example.shelfpalace.ui.theme.LoadedCardBorder
import com.example.shelfpalace.ui.theme.LoadedEmeraldGreen
import com.example.shelfpalace.ui.theme.LoadedSurfaceNavy
import com.example.shelfpalace.ui.theme.SynthwaveDark
import com.example.shelfpalace.ui.theme.SynthwaveLavender
import com.example.shelfpalace.util.PlatformUtils
import java.util.Calendar
import kotlin.math.abs

@Composable
fun getAppCorners(default: Dp = 20.dp): RoundedCornerShape {
    return when (LocalCornerStyle.current) {
        CornerStyle.ROUNDED -> RoundedCornerShape(if (default < 16.dp) 20.dp else default)
        CornerStyle.OUTLINED -> RoundedCornerShape(6.dp)
        CornerStyle.SQUARE -> RoundedCornerShape(0.dp)
    }
}

@Composable
fun getAppCornerRadius(default: Dp = 20.dp): Dp {
    return when (LocalCornerStyle.current) {
        CornerStyle.ROUNDED -> if (default < 16.dp) 20.dp else default
        CornerStyle.OUTLINED -> 6.dp
        CornerStyle.SQUARE -> 0.dp
    }
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
    val isCyberGreen = LocalAppTheme.current == AppTheme.CYBER_GREEN
    val buttonColor = if (isCyberGreen && (color == MaterialTheme.colorScheme.primary || color == SynthwaveLavender)) LoadedEmeraldGreen else color

    val radius = 12.dp
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressedHighlight = Color(0xFF8A91AB) 
    
    val isNeutral = abs(buttonColor.red - buttonColor.green) < 0.1f && abs(buttonColor.green - buttonColor.blue) < 0.1f
    val highlightColor = if (isPressed && isNeutral) pressedHighlight else buttonColor
    
    val finalContainerColor = if (isPressed) highlightColor.copy(alpha = 0.15f) else containerColor
    val finalContentColor = if (isPressed) highlightColor else buttonColor
    val finalBorderColor = if (isPressed) highlightColor else buttonColor.copy(alpha = 0.5f)

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
        border = BorderStroke(2.dp, finalBorderColor),
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
    val isCyberGreen = LocalAppTheme.current == AppTheme.CYBER_GREEN
    val isRounded = LocalCornerStyle.current == CornerStyle.ROUNDED

    val defaultBg = if (isCyberGreen) LoadedSurfaceNavy.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    val defaultTint = if (isCyberGreen) LoadedEmeraldGreen else color
    val defaultBorder = if (isCyberGreen) LoadedEmeraldGreen.copy(alpha = 0.6f) else color.copy(alpha = 0.5f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val finalContainerColor = if (isPressed) defaultBorder.copy(alpha = 0.35f) else defaultBg
    val finalContentColor = if (tint != null && tint != Color.Unspecified) tint else defaultTint
    val finalBorderColor = if (isPressed) defaultTint else defaultBorder

    val iconShape = if (isRounded) CircleShape else getAppCorners(12.dp)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.size(size),
        color = finalContainerColor,
        contentColor = finalContentColor,
        shape = iconShape,
        border = BorderStroke(2.dp, finalBorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = finalContentColor,
                    modifier = Modifier.size(iconSize).then(iconModifier)
                )
            } else if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = contentDescription,
                    colorFilter = if (tint != null && tint != Color.Unspecified) ColorFilter.tint(tint) else null,
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
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (currentSortOption == SortOption.PLATFORM_NAME) color else Color.Transparent,
                                        getAppCorners(2.dp)
                                    )
                                    .border(1.dp, if (currentSortOption == SortOption.PLATFORM_NAME) color else Color.White.copy(alpha = 0.5f), getAppCorners(2.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "SORT BY CONSOLE (NAME)", 
                                color = if (currentSortOption == SortOption.PLATFORM_NAME) color else Color.White, 
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    onClick = {
                        onSortOptionSelected(SortOption.PLATFORM_NAME)
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
    val currentStyle = LocalCornerStyle.current
    val currentTheme = LocalAppTheme.current

    val radius = when (currentStyle) {
        CornerStyle.ROUNDED -> 20.dp
        CornerStyle.OUTLINED -> 6.dp
        CornerStyle.SQUARE -> 0.dp
    }
    val borderWidth = 2.dp

    val (cardBackground, borderColor) = if (currentTheme == AppTheme.CYBER_GREEN) {
        LoadedSurfaceNavy.copy(alpha = 0.95f) to LoadedEmeraldGreen.copy(alpha = 0.6f)
    } else {
        val borderAlpha = if (currentStyle == CornerStyle.OUTLINED) 0.65f else 0.4f
        Color.Black.copy(alpha = containerAlpha) to color.copy(alpha = borderAlpha)
    }

    Box(
        modifier = modifier
            .background(cardBackground, RoundedCornerShape(radius))
            .border(borderWidth, borderColor, RoundedCornerShape(radius))
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
    val radius = 24.dp
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = getAppCorners(radius),
        border = BorderStroke(2.dp, color),
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
    val radius = 24.dp
    Box(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            shape = getAppCorners(radius),
            border = BorderStroke(2.dp, color.copy(alpha = 0.8f))
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
): TextFieldColors {
    val isCyberGreen = LocalAppTheme.current == AppTheme.CYBER_GREEN
    
    val containerColor = if (isCyberGreen) LoadedSurfaceNavy.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val textColor = Color.White
    val unfocusedBorder = if (isCyberGreen) LoadedEmeraldGreen.copy(alpha = 0.6f) else color
    val focusedBorder = if (isCyberGreen) LoadedEmeraldGreen else color
    val iconColor = if (isCyberGreen) LoadedEmeraldGreen else color

    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        focusedBorderColor = focusedBorder,
        unfocusedBorderColor = unfocusedBorder,
        focusedLabelColor = focusedBorder,
        unfocusedLabelColor = unfocusedBorder,
        focusedLeadingIconColor = iconColor,
        focusedTrailingIconColor = iconColor,
        unfocusedLeadingIconColor = iconColor,
        unfocusedTrailingIconColor = iconColor,
        cursorColor = if (isCyberGreen) Color(0xFF1A1F38) else color,
        selectionColors = TextSelectionColors(
            handleColor = focusedBorder,
            backgroundColor = focusedBorder.copy(alpha = 0.4f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
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
            .border(2.dp, color.copy(alpha = 0.5f), getAppCorners(24.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    placeholder: String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue.ifEmpty { placeholder },
            onValueChange = {},
            readOnly = true,
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, fontSize = 12.sp) } } else null,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = synthwaveTextFieldColors(accentColor),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = getAppCorners(8.dp),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.Black.copy(alpha = 0.95f),
            modifier = Modifier.border(1.dp, accentColor, getAppCorners(12.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedValue == option || (selectedValue.isEmpty() && option == placeholder)) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedValue == option || (selectedValue.isEmpty() && option == placeholder)) accentColor else Color.White
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White,
                        trailingIconColor = accentColor
                    )
                )
            }
        }
    }
}

@Composable
fun DateDropdownPicker(
    label: String,
    selectedDay: String,
    selectedMonth: String,
    selectedYear: String,
    onDateChanged: (day: String, month: String, year: String) -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Column {
        if (label.isNotEmpty()) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FormDropdownField(
                label = "",
                selectedValue = selectedDay,
                options = (1..31).map { it.toString().padStart(2, '0') },
                onOptionSelected = { day -> onDateChanged(day, selectedMonth, selectedYear) },
                modifier = Modifier.weight(0.8f),
                accentColor = accentColor,
                placeholder = "DD"
            )
            FormDropdownField(
                label = "",
                selectedValue = selectedMonth,
                options = (1..12).map { it.toString().padStart(2, '0') },
                onOptionSelected = { month -> onDateChanged(selectedDay, month, selectedYear) },
                modifier = Modifier.weight(0.8f),
                accentColor = accentColor,
                placeholder = "MM"
            )
            FormDropdownField(
                label = "",
                selectedValue = selectedYear,
                options = run {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    (currentYear downTo 1950).map { it.toString() }
                },
                onOptionSelected = { year -> onDateChanged(selectedDay, selectedMonth, year) },
                modifier = Modifier.weight(1.2f),
                accentColor = accentColor,
                placeholder = "YYYY"
            )
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
fun MediaGridItemCard(
    title: String,
    coverUri: String,
    tag: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    color: Color,
    aspectRatio: Float = 0.7f,
    showFavoriteBadge: Boolean = true,
    subtitle: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = color
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(getAppCorners(12.dp))
            ) {
                val context = LocalContext.current
                val imageModel = remember(coverUri, title) {
                    val url = coverUri.ifEmpty { "https://via.placeholder.com/150x200?text=$title" }
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        ImageRequest.Builder(context)
                            .data(url)
                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36")
                            .addHeader("Referer", "https://www.discogs.com/")
                            .crossfade(true)
                            .build()
                    } else {
                        url
                    }
                }

                AsyncImage(
                    model = imageModel,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )

                if (tag.isNotEmpty() || (isFavorite && showFavoriteBadge)) {
                    val isRounded = LocalCornerStyle.current == CornerStyle.ROUNDED
                    val badgeShape = if (isRounded) CircleShape else getAppCorners(6.dp)

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isFavorite && showFavoriteBadge) {
                            Surface(
                                color = color.copy(alpha = 0.95f),
                                contentColor = Color.Black,
                                shape = badgeShape,
                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.3f))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_heart_filled),
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).size(16.dp),
                                    tint = Color(0xFFAD1457)
                                )
                            }
                        }

                        if (tag.isNotEmpty()) {
                            Surface(
                                color = color.copy(alpha = 0.95f),
                                contentColor = Color.Black,
                                shape = badgeShape,
                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    ),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                color = Color.White
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = Color.White.copy(alpha = 0.7f)
                )
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
    MediaGridItemCard(
        title = game.title,
        coverUri = game.coverUri,
        tag = PlatformUtils.getShortPlatformName(game.platformId),
        isFavorite = game.isFavorite,
        onClick = onClick,
        color = MaterialTheme.colorScheme.primary,
        aspectRatio = aspectRatio,
        showFavoriteBadge = showFavoriteBadge
    )
}

@Composable
fun MovieGridItem(
    movie: Movie,
    onClick: () -> Unit,
    showFavoriteBadge: Boolean = true
) {
    MediaGridItemCard(
        title = movie.title,
        coverUri = movie.coverUri,
        tag = PlatformUtils.getMovieFormatTag(movie.formatId),
        isFavorite = movie.isFavorite,
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondary,
        aspectRatio = 0.7f,
        showFavoriteBadge = showFavoriteBadge
    )
}

@Composable
fun MusicGridItem(
    music: Music,
    onClick: () -> Unit,
    showFavoriteBadge: Boolean = true
) {
    MediaGridItemCard(
        title = music.title,
        subtitle = music.artist,
        coverUri = music.coverUri,
        tag = PlatformUtils.getMusicFormatTag(music.formatId),
        isFavorite = music.isFavorite,
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondary,
        aspectRatio = 1f,
        showFavoriteBadge = showFavoriteBadge,
        contentScale = ContentScale.Crop
    )
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
    @Suppress("DEPRECATION")
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
    var left by remember { mutableFloatStateOf(0.1f) }
    var top by remember { mutableFloatStateOf(0.1f) }
    var right by remember { mutableFloatStateOf(0.9f) }
    var bottom by remember { mutableFloatStateOf(0.9f) }

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
                    
                        val borderAccent = SynthwaveLavender
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


