package com.example.shelfpalace.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.shelfpalace.R
import com.example.shelfpalace.ui.theme.ShelfPalaceTheme

@Composable
fun ShelfPalaceBackground(
    modifier: Modifier = Modifier,
    scrimColor: Color = Color(0xFF121216).copy(alpha = 0.25f)
) {
    val backgroundPic = R.drawable.background_pic
    
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundPic),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ShelfPalaceBackgroundPreview() {
    ShelfPalaceTheme {
        ShelfPalaceBackground()
    }
}
