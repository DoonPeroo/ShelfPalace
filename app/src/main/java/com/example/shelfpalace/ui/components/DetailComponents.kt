package com.example.shelfpalace.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.ui.theme.DarkBackground
import kotlinx.coroutines.launch

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary,
    valueColor: Color = Color.White,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = valueColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DetailTabSelector(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    accentColor: Color
) {
    NeonCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = accentColor,
        containerAlpha = 0.3f,
        padding = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(getAppCorners(10.dp))
                        .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) accentColor else Color.Transparent,
                            shape = getAppCorners(10.dp)
                        )
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun NotesDialog(
    initialNotes: String,
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit,
    accentColor: Color
) {
    var notes by remember { mutableStateOf(initialNotes) }

    Dialog(onDismissRequest = onDismissRequest) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            color = accentColor,
            containerAlpha = 0.9f
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EDIT NOTES",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                    color = accentColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = synthwaveTextFieldColors(accentColor),
                    minLines = 5,
                    shape = getAppCorners(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeonButton(
                        text = "CANCEL",
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    NeonButton(
                        text = "SAVE",
                        onClick = { onSave(notes) },
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun FullscreenImageDialog(
    screenshots: List<String>, 
    initialIndex: Int, 
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { screenshots.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { pageIndex ->
                val isActive = pagerState.currentPage == pageIndex
                ZoomableImage(imageUrl = screenshots[pageIndex], isActive = isActive)
            }

            // Header UI (Matching Top Bar Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Counter (Styled like Top Bar Header)
                val counterCorners = 12.dp
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), getAppCorners(counterCorners))
                        .border(1.dp, accentColor.copy(alpha = 0.8f), getAppCorners(counterCorners))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${screenshots.size}",
                        color = accentColor,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                // Close Button (Using NeonIconButton)
                NeonIconButton(
                    iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_close),
                    onClick = onDismiss,
                    color = accentColor,
                    size = 40.dp,
                    contentDescription = "Close"
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(imageUrl: String, isActive: Boolean) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Reset zoom when the page becomes inactive
    LaunchedEffect(isActive) {
        if (!isActive) {
            scale.snapTo(1f)
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        scope.launch {
                            if (scale.value > 1.1f) {
                                // Smooth zoom out to 1x
                                launch { scale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            } else {
                                // Zoom in to 3x towards the tapped point
                                val targetScale = 3f
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                
                                val targetX = (centerX - tapOffset.x) * (targetScale - 1f)
                                val targetY = (centerY - tapOffset.y) * (targetScale - 1f)

                                launch { scale.animateTo(targetScale, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetX.animateTo(targetX, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { offsetY.animateTo(targetY, spring(stiffness = Spring.StiffnessMediumLow)) }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        
                        // Only consume and apply if there is an actual transform happening
                        // This allows taps (which have zoom=1 and pan=0) to pass through to detectTapGestures
                        if (zoom != 1f || pan != Offset.Zero) {
                            val currentScale = scale.value
                            val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                            
                            if (currentScale > 1.01f || newScale > 1.01f) {
                                scope.launch {
                                    scale.snapTo(newScale)
                                    offsetX.snapTo(offsetX.value + pan.x)
                                    offsetY.snapTo(offsetY.value + pan.y)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Fullscreen Screenshot",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    translationX = offsetX.value,
                    translationY = offsetY.value
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = stringResource(R.string.msg_delete_confirmation_title),
    text: String = stringResource(R.string.msg_delete_confirmation_text),
    confirmText: String = stringResource(R.string.action_delete),
    dismissText: String = stringResource(R.string.action_cancel)
) {
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            color = MaterialTheme.colorScheme.primary,
            containerAlpha = 0.9f,
            padding = 16.dp,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.Red,
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeonButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        color = Color.White.copy(alpha = 0.85f),
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )

                    NeonButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        height = 44.dp,
                        color = Color.Red,
                        containerColor = Color.Red.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
