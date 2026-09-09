package com.example.shelfpalace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shelfpalace.R
import com.example.shelfpalace.data.remote.IgdbGame
import com.example.shelfpalace.data.remote.IgdbService
import com.example.shelfpalace.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgdbSearchScreen(
    platformId: String?,
    initialQuery: String? = null,
    onGameSelected: (Long) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
) {
    var searchQuery by rememberSaveable { mutableStateOf(initialQuery ?: "") }
    
    // Custom saver for search results to survive navigation
    val searchResultsSaver = remember {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, IgdbGame::class.java)
        val adapter = moshi.adapter<List<IgdbGame>>(listType)
        
        androidx.compose.runtime.saveable.Saver<List<IgdbGame>, String>(
            save = { list -> adapter.toJson(list) },
            restore = { str -> try { adapter.fromJson(str) } catch(e: Exception) { emptyList() } }
        )
    }
    
    var searchResults by rememberSaveable(stateSaver = searchResultsSaver) { 
        mutableStateOf(emptyList<IgdbGame>()) 
    }
    
    var lastSearchedQuery by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Search trigger
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2 && (searchQuery != lastSearchedQuery || searchResults.isEmpty())) {
            delay(500) 
            isLoading = true
            errorMessage = null
            try {
                val results = IgdbService.search(searchQuery, platformId)
                searchResults = results
                lastSearchedQuery = searchQuery
                if (results.isEmpty()) {
                    errorMessage = "No games found for \"$searchQuery\"."
                }
            } catch (e: Exception) {
                errorMessage = "API Error: ${e.localizedMessage ?: "Unknown error"}"
                e.printStackTrace()
            }
            isLoading = false
        } else if (searchQuery.isEmpty()) {
            searchResults = emptyList()
            lastSearchedQuery = ""
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    NeonHeader(
                        text = "IGDB SEARCH",
                        fullWidth = false
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp))
                },
                actions = {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.ic_close),
                        onClick = onClose,
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
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
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter game title...") },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search), 
                        contentDescription = "Search", 
                        tint = Color.Unspecified,
                        modifier = Modifier.padding(end = 12.dp).size(24.dp)
                    )
                },
                singleLine = true,
                shape = getAppCorners(8.dp),
                colors = synthwaveTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (IgdbService.clientId.isEmpty() || IgdbService.accessToken.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "IGDB API KEYS MISSING\nPlease configure in IgdbService.kt",
                        color = Color.Red.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage!!,
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "SEARCH FOR A GAME" else "NO GAMES FOUND",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(searchResults) { game ->
                        IgdbGameItem(game = game, onClick = { game.id?.let { onGameSelected(it) } })
                    }
                }
            }
        }
    }
}

@Composable
fun IgdbGameItem(game: IgdbGame, onClick: () -> Unit) {
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.primary,
        padding = 8.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val coverUrl = game.cover?.url?.let {
                if (it.startsWith("//")) "https:$it" else it
            }?.replace("t_thumb", "t_cover_big")

            AsyncImage(
                model = coverUrl ?: "https://via.placeholder.com/60x80?text=NO+IMAGE",
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp, 80.dp)
                    .clip(getAppCorners(4.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.name ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    game.firstReleaseDate?.let { timestamp ->
                        val date = java.util.Date(timestamp * 1000)
                        val year = java.util.Calendar.getInstance().apply { time = date }.get(java.util.Calendar.YEAR)
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    val platformNames = game.platforms?.mapNotNull { id ->
                        getPlatformNameById(id)
                    }?.distinct()
                    
                    if (platformNames?.isNotEmpty() == true) {
                        if (game.firstReleaseDate != null) {
                            Text(" • ", color = Color.White.copy(alpha = 0.5f))
                        }
                        Text(
                            text = platformNames.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun getPlatformNameById(id: Long): String? {
    return when (id) {
        7L -> "PS1"
        8L -> "PS2"
        9L -> "PS3"
        48L -> "PS4"
        167L -> "PS5"
        38L -> "PSP"
        46L -> "Vita"
        18L -> "NES"
        19L -> "SNES"
        4L -> "N64"
        21L -> "GCN"
        5L -> "Wii"
        41L -> "Wii U"
        130L -> "Switch"
        33L -> "GB"
        24L -> "GBC"
        22L -> "GBA"
        20L -> "DS"
        37L -> "3DS"
        11L -> "Xbox"
        12L -> "X360"
        49L -> "XONE"
        169L -> "XSX"
        64L -> "MS"
        29L -> "MD"
        32L -> "Saturn"
        23L -> "DC"
        35L -> "GG"
        else -> null
    }
}
