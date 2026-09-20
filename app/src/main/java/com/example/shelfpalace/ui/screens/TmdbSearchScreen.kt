package com.example.shelfpalace.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shelfpalace.R
import com.example.shelfpalace.data.remote.TmdbMovie
import com.example.shelfpalace.data.remote.TmdbService
import com.example.shelfpalace.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

val ALL_TMDB_SEARCH_YEARS = listOf(
    "2026", "2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017",
    "2016", "2015", "2014", "2013", "2012", "2011", "2010", "2005", "2000", "1995",
    "1990", "1985", "1980", "1975", "1970", "1960", "1950"
)

val TMDB_LANGUAGE_OPTIONS = listOf(
    "English (en-US)",
    "German (de-DE)",
    "Spanish (es-ES)",
    "French (fr-FR)",
    "Italian (it-IT)",
    "Japanese (ja-JP)",
    "Korean (ko-KR)",
    "Portuguese (pt-BR)",
    "Russian (ru-RU)",
    "Chinese (zh-CN)"
)

val TMDB_LANGUAGE_MAP = mapOf(
    "English (en-US)" to "en-US",
    "German (de-DE)" to "de-DE",
    "Spanish (es-ES)" to "es-ES",
    "French (fr-FR)" to "fr-FR",
    "Italian (it-IT)" to "it-IT",
    "Japanese (ja-JP)" to "ja-JP",
    "Korean (ko-KR)" to "ko-KR",
    "Portuguese (pt-BR)" to "pt-BR",
    "Russian (ru-RU)" to "ru-RU",
    "Chinese (zh-CN)" to "zh-CN"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmdbSearchScreen(
    initialQuery: String? = null,
    initialYear: String? = null,
    initialLanguage: String = "en-US",
    onMovieSelected: (Long, String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
) {
    var searchQuery by rememberSaveable { mutableStateOf(initialQuery ?: "") }
    var filterYear by rememberSaveable { mutableStateOf(initialYear ?: "") }
    var filterLanguageLabel by rememberSaveable { 
        mutableStateOf(TMDB_LANGUAGE_MAP.entries.find { it.value == initialLanguage }?.key ?: "English (en-US)") 
    }
    val filterLanguageCode = TMDB_LANGUAGE_MAP[filterLanguageLabel] ?: "en-US"

    var showFilterSection by rememberSaveable { mutableStateOf(filterYear.isNotBlank()) }

    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var totalResults by rememberSaveable { mutableIntStateOf(0) }

    val movieColor = MaterialTheme.colorScheme.secondary

    var searchResults by remember { mutableStateOf(emptyList<TmdbMovie>()) }
    var lastSearchedKey by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(searchQuery, filterYear, filterLanguageCode) {
        currentPage = 1
    }

    LaunchedEffect(searchQuery, filterYear, filterLanguageCode, currentPage) {
        val q = searchQuery.trim()
        val hasQuery = q.length >= 2 || filterYear.isNotBlank()
        val currentKey = "$q|$filterYear|$filterLanguageCode|p$currentPage"

        if (hasQuery && (currentKey != lastSearchedKey || searchResults.isEmpty())) {
            delay(400)
            isLoading = true
            errorMessage = null
            try {
                val pageResult = withContext(Dispatchers.IO) {
                    TmdbService.search(
                        query = q,
                        year = filterYear,
                        page = currentPage,
                        language = filterLanguageCode
                    )
                }
                searchResults = pageResult.results
                totalPages = pageResult.totalPages
                totalResults = pageResult.totalResults
                lastSearchedKey = currentKey
                if (pageResult.results.isEmpty()) {
                    errorMessage = "No movies found matching criteria."
                }
            } catch (e: Exception) {
                errorMessage = "API Error: ${e.localizedMessage ?: "Unknown error"}"
                e.printStackTrace()
            }
            isLoading = false
        } else if (!hasQuery) {
            searchResults = emptyList()
            lastSearchedKey = ""
            errorMessage = null
            totalPages = 1
            totalResults = 0
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    NeonHeader(
                        text = "TMDB MOVIE SEARCH",
                        fullWidth = false,
                        color = movieColor
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp), color = movieColor)
                },
                actions = {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.ic_close),
                        onClick = onClose,
                        modifier = Modifier.padding(end = 8.dp),
                        color = movieColor
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
                placeholder = { Text("Search movie title...") },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )
                },
                singleLine = true,
                shape = getAppCorners(),
                colors = synthwaveTextFieldColors(movieColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showFilterSection = !showFilterSection },
                    color = if (filterYear.isNotBlank()) movieColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                    shape = getAppCorners(8.dp),
                    border = BorderStroke(1.dp, if (filterYear.isNotBlank()) movieColor else Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sort),
                            contentDescription = "Filters",
                            tint = if (filterYear.isNotBlank()) movieColor else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (filterYear.isNotBlank()) "YEAR: $filterYear" else "SEARCH FILTERS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = if (filterYear.isNotBlank()) movieColor else Color.White
                        )
                        Icon(
                            imageVector = if (showFilterSection) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (filterYear.isNotBlank()) {
                    TextButton(
                        onClick = { filterYear = "" },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("RESET YEAR", color = movieColor, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                }
            }

            if (showFilterSection) {
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    color = movieColor,
                    padding = 8.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterDropdownField(
                            label = "Language",
                            value = filterLanguageLabel,
                            onValueChange = { filterLanguageLabel = it },
                            placeholder = "Language",
                            options = TMDB_LANGUAGE_OPTIONS,
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = movieColor
                        )

                        FilterDropdownField(
                            label = "Release Year",
                            value = filterYear,
                            onValueChange = { filterYear = it },
                            placeholder = "Year",
                            options = ALL_TMDB_SEARCH_YEARS,
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = movieColor
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = movieColor)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage!!,
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty() && filterYear.isBlank()) "SEARCH MOVIES" else "NO MOVIES FOUND",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (totalPages > 1) {
                        item {
                            TmdbPaginationBar(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                totalResults = totalResults,
                                onPageSelected = { newPage -> currentPage = newPage },
                                accentColor = movieColor
                            )
                        }
                    }

                    items(searchResults, key = { it.id ?: System.currentTimeMillis() }) { movie ->
                        TmdbMovieSearchItem(
                            movie = movie,
                            onMovieSelected = { selectedId -> onMovieSelected(selectedId, filterLanguageCode) },
                            accentColor = movieColor
                        )
                    }

                    if (totalPages > 1) {
                        item {
                            TmdbPaginationBar(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                totalResults = totalResults,
                                onPageSelected = { newPage -> currentPage = newPage },
                                accentColor = movieColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TmdbPaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalResults: Int,
    onPageSelected: (Int) -> Unit,
    accentColor: Color
) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = getAppCorners(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { if (currentPage > 1) onPageSelected(currentPage - 1) },
                enabled = currentPage > 1,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Previous Page",
                    tint = if (currentPage > 1) accentColor else Color.White.copy(alpha = 0.2f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val pagesToShow = remember(currentPage, totalPages) {
                    val start = maxOf(1, currentPage - 1)
                    val end = minOf(totalPages, start + 2)
                    (start..end).toList()
                }

                if (pagesToShow.first() > 1) {
                    TextButton(
                        onClick = { onPageSelected(1) },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("1", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }
                    if (pagesToShow.first() > 2) {
                        Text("...", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                    }
                }

                pagesToShow.forEach { p ->
                    val isCurrent = p == currentPage
                    Surface(
                        onClick = { onPageSelected(p) },
                        color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.08f),
                        shape = getAppCorners(6.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = p.toString(),
                            color = if (isCurrent) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (pagesToShow.last() < totalPages) {
                    if (pagesToShow.last() < totalPages - 1) {
                        Text("...", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick = { onPageSelected(totalPages) },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(totalPages.toString(), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            IconButton(
                onClick = { if (currentPage < totalPages) onPageSelected(currentPage + 1) },
                enabled = currentPage < totalPages,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Next Page",
                    tint = if (currentPage < totalPages) accentColor else Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun TmdbMovieSearchItem(
    movie: TmdbMovie,
    onMovieSelected: (Long) -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    val posterUrl = movie.posterUrl

    val imageModel = remember(posterUrl) {
        if (!posterUrl.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(posterUrl)
                .crossfade(true)
                .build()
        } else {
            null
        }
    }

    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { movie.id?.let { onMovieSelected(it) } },
        color = accentColor,
        padding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 100.dp)
                    .clip(getAppCorners(6.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = movie.title ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (movie.voteAverage != null && movie.voteAverage > 0.0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", movie.voteAverage),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = Color(0xFFFFC107)
                            )
                        }
                    }
                }

                if (movie.releaseYear.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "RELEASE: " + movie.releaseYear,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = accentColor
                    )
                }

                if (!movie.overview.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = movie.overview,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
