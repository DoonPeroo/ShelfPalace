package com.example.shelfpalace.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
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
import com.example.shelfpalace.data.remote.DiscogsSearchResult
import com.example.shelfpalace.data.remote.DiscogsService
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.theme.DarkBackground
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val ALL_DISCOGS_FORMATS = listOf(
    "Vinyl", "CD", "Cassette", "LP", "Album", "Single", "EP", "12\"", "7\"", "10\"",
    "Flexi-disc", "Shellac", "DVD", "Blu-ray", "SACD", "MiniDisc", "8-Track Cartridge",
    "Reel-to-Reel", "Box Set", "Compilation", "Limited Edition", "Promo", "Reissue",
    "Remastered", "Picture Disc", "Colored Vinyl", "Unofficial Release", "File"
)

val ALL_DISCOGS_LABELS = listOf(
    "Atlantic", "A&M Records", "Blue Note", "Capitol Records", "Columbia", "Decca",
    "Def Jam Recordings", "Deutsche Grammophon", "Elektra", "EMI", "Epic", "Geffen Records",
    "Harvest", "Interscope Records", "Island Records", "Mercury", "Motown", "Parlophone",
    "Polydor", "PolyGram", "RCA", "Reprise Records", "Sony Music", "Sub Pop",
    "Universal Music Group", "Verve Records", "Virgin", "Warner Bros. Records"
)

val ALL_DISCOGS_COUNTRIES = listOf(
    "US", "UK", "Europe", "Germany", "Japan", "France", "Italy", "Canada", "Australia", "Worldwide"
)

val ALL_DISCOGS_YEARS = listOf(
    "2026", "2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017",
    "2016", "2015", "2014", "2013", "2012", "2011", "2010", "2005", "2000", "1995",
    "1990", "1985", "1980", "1975", "1973", "1970", "1965", "1960", "1955", "1950"
)

data class AlbumGroup(
    val key: String,
    val masterId: Long?,
    val artist: String,
    val title: String,
    val initialVersions: List<DiscogsSearchResult>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscogsSearchScreen(
    formatId: String?,
    initialQuery: String? = null,
    initialFormat: String? = null,
    initialLabel: String? = null,
    initialCountry: String? = null,
    initialYear: String? = null,
    onMusicSelected: (Long) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
) {
    var searchQuery by rememberSaveable { mutableStateOf(initialQuery ?: "") }

    val defaultFormat = initialFormat?.takeIf { it.isNotBlank() } ?: when (formatId?.lowercase()) {
        "vinyl" -> "Vinyl"
        "cd" -> "CD"
        "cassette" -> "Cassette"
        else -> ""
    }

    var filterFormat by rememberSaveable { mutableStateOf(defaultFormat) }
    var filterLabel by rememberSaveable { mutableStateOf(initialLabel ?: "") }
    var filterCountry by rememberSaveable { mutableStateOf(initialCountry ?: "") }
    var filterYear by rememberSaveable { mutableStateOf(initialYear ?: "") }

    val hasInitialFilters = listOf(filterFormat, filterLabel, filterCountry, filterYear).any { it.isNotBlank() }
    var showFilterSection by rememberSaveable { mutableStateOf(hasInitialFilters) }

    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var totalItems by rememberSaveable { mutableIntStateOf(0) }

    val musicColor = MaterialTheme.colorScheme.secondary
    val activeFilterCount = listOf(filterFormat, filterLabel, filterCountry, filterYear).count { it.isNotBlank() }

    // Custom saver for Discogs search results to survive navigation
    val searchResultsSaver = remember {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val listType = Types.newParameterizedType(List::class.java, DiscogsSearchResult::class.java)
        val adapter = moshi.adapter<List<DiscogsSearchResult>>(listType)

        Saver<List<DiscogsSearchResult>, String>(
            save = { list -> adapter.toJson(list) },
            restore = { str -> try { adapter.fromJson(str) } catch (e: Exception) { emptyList() } }
        )
    }

    var searchResults by rememberSaveable(stateSaver = searchResultsSaver) {
        mutableStateOf(emptyList<DiscogsSearchResult>())
    }

    var lastSearchedQuery by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reset currentPage when filters or query changes
    LaunchedEffect(searchQuery, filterFormat, filterLabel, filterCountry, filterYear) {
        currentPage = 1
    }

    // Search trigger with active filters & pagination
    LaunchedEffect(searchQuery, filterFormat, filterLabel, filterCountry, filterYear, currentPage) {
        val hasQuery = searchQuery.length >= 2 || filterFormat.isNotBlank() || filterLabel.isNotBlank() || filterCountry.isNotBlank() || filterYear.isNotBlank()
        val currentFilterKey = "$searchQuery|$filterFormat|$filterLabel|$filterCountry|$filterYear|p$currentPage"

        if (hasQuery && (currentFilterKey != lastSearchedQuery || searchResults.isEmpty())) {
            delay(400)
            isLoading = true
            errorMessage = null
            try {
                val pageResult = withContext(Dispatchers.IO) {
                    DiscogsService.search(
                        query = searchQuery,
                        format = filterFormat,
                        label = filterLabel,
                        country = filterCountry,
                        year = filterYear,
                        page = currentPage
                    )
                }
                searchResults = pageResult.results
                totalPages = pageResult.totalPages
                totalItems = pageResult.totalItems
                lastSearchedQuery = currentFilterKey
                if (pageResult.results.isEmpty()) {
                    errorMessage = "No music releases found matching criteria."
                }
            } catch (e: Exception) {
                errorMessage = "API Error: ${e.localizedMessage ?: "Unknown error"}"
                e.printStackTrace()
            }
            isLoading = false
        } else if (!hasQuery) {
            searchResults = emptyList()
            lastSearchedQuery = ""
            errorMessage = null
            totalPages = 1
            totalItems = 0
        }
    }

    val albumGroups = remember(searchResults) {
        searchResults.groupBy { result ->
            if (result.masterId != null && result.masterId != 0L) {
                "master_${result.masterId}"
            } else {
                val a = result.parsedArtist.lowercase().trim()
                val t = result.parsedTitle.lowercase().trim()
                if (a.isNotBlank() && t.isNotBlank()) "title_${a}_$t" else "id_${result.id}"
            }
        }.map { (key, groupResults) ->
            val bestCover = groupResults.firstNotNullOfOrNull { it.validCoverUrl }
            val updatedGroup = groupResults.map { res ->
                if (res.validCoverUrl.isNullOrBlank() && !bestCover.isNullOrBlank()) {
                    res.copy(coverImage = bestCover)
                } else {
                    res
                }
            }
            val first = updatedGroup.first()
            val masterId = first.masterId?.takeIf { it != 0L }
            AlbumGroup(
                key = key,
                masterId = masterId,
                artist = first.parsedArtist,
                title = first.parsedTitle.ifBlank { first.title ?: "Unknown" },
                initialVersions = updatedGroup
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    NeonHeader(
                        text = "DISCOGS SEARCH",
                        fullWidth = false,
                        color = musicColor
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp), color = musicColor)
                },
                actions = {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.ic_close),
                        onClick = onClose,
                        modifier = Modifier.padding(end = 8.dp),
                        color = musicColor
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
                placeholder = { Text("Enter artist & title, or barcode...") },
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
                shape = getAppCorners(8.dp),
                colors = synthwaveTextFieldColors(musicColor)
            )

            // FILTER HEADER BUTTON & TOGGLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showFilterSection = !showFilterSection },
                    color = if (activeFilterCount > 0) musicColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                    shape = getAppCorners(8.dp),
                    border = BorderStroke(1.dp, if (activeFilterCount > 0) musicColor else Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sort),
                            contentDescription = "Filters",
                            tint = if (activeFilterCount > 0) musicColor else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (activeFilterCount > 0) "FILTERS ($activeFilterCount ACTIVE)" else "SEARCH FILTERS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = if (activeFilterCount > 0) musicColor else Color.White
                        )
                        Icon(
                            imageVector = if (showFilterSection) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (activeFilterCount > 0) {
                    TextButton(
                        onClick = {
                            filterFormat = ""
                            filterLabel = ""
                            filterCountry = ""
                            filterYear = ""
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("RESET FILTERS", color = musicColor, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                }
            }

            // COMPACT EXPANDABLE FILTER DROPDOWNS SECTION
            if (showFilterSection) {
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    color = musicColor,
                    padding = 8.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Compact 2x2 Grid: Row 1 (Format & Labels)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterDropdownField(
                                label = "Format",
                                value = filterFormat,
                                onValueChange = { filterFormat = it },
                                placeholder = "Format",
                                options = ALL_DISCOGS_FORMATS,
                                modifier = Modifier.weight(1f),
                                accentColor = musicColor
                            )

                            FilterDropdownField(
                                label = "Labels & Companies",
                                value = filterLabel,
                                onValueChange = { filterLabel = it },
                                placeholder = "Labels & Companies",
                                options = ALL_DISCOGS_LABELS,
                                modifier = Modifier.weight(1f),
                                accentColor = musicColor
                            )
                        }

                        // Compact 2x2 Grid: Row 2 (Country & Year)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterDropdownField(
                                label = "Country",
                                value = filterCountry,
                                onValueChange = { filterCountry = it },
                                placeholder = "Country",
                                options = ALL_DISCOGS_COUNTRIES,
                                modifier = Modifier.weight(1f),
                                accentColor = musicColor
                            )

                            FilterDropdownField(
                                label = "Year",
                                value = filterYear,
                                onValueChange = { filterYear = it },
                                placeholder = "Year",
                                options = ALL_DISCOGS_YEARS,
                                modifier = Modifier.weight(1f),
                                accentColor = musicColor
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = musicColor)
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
            } else if (albumGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty() && activeFilterCount == 0) "SEARCH DISCOGS FOR MUSIC" else "NO RELEASES FOUND",
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
                            DiscogsPaginationBar(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                totalItems = totalItems,
                                onPageSelected = { newPage -> currentPage = newPage },
                                accentColor = musicColor
                            )
                        }
                    }

                    items(albumGroups, key = { it.key }) { group ->
                        DiscogsSearchItem(group = group, onMusicSelected = onMusicSelected, accentColor = musicColor)
                    }

                    if (totalPages > 1) {
                        item {
                            DiscogsPaginationBar(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                totalItems = totalItems,
                                onPageSelected = { newPage -> currentPage = newPage },
                                accentColor = musicColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscogsPaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
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
            // Previous Page Button
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

            // Page Number Buttons
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

            // Next Page Button
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdownField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.secondary
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (value.isNotEmpty()) accentColor else Color.White.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = synthwaveTextFieldColors(accentColor),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = getAppCorners(6.dp),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = DarkBackground,
                modifier = Modifier
                    .border(1.dp, accentColor.copy(alpha = 0.6f), getAppCorners(8.dp))
                    .heightIn(max = 240.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "All / Reset",
                            color = if (value.isEmpty()) accentColor else Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        )
                    },
                    onClick = {
                        onValueChange("")
                        expanded = false
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                options.forEach { option ->
                    val isSelected = value.equals(option, ignoreCase = true)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (isSelected) accentColor else Color.White,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscogsSearchItem(
    group: AlbumGroup,
    onMusicSelected: (Long) -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVersion by remember(group.key) { mutableStateOf(group.initialVersions.first()) }
    var versionList by remember(group.key) { mutableStateOf(group.initialVersions) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isFetchingMasterVersions by remember { mutableStateOf(false) }
    var hasFetchedMasterVersions by remember { mutableStateOf(false) }

    var fetchedCoverUrl by remember { mutableStateOf<String?>(null) }
    val initialCoverUrl = selectedVersion.validCoverUrl
    val effectiveCoverUrl = fetchedCoverUrl ?: initialCoverUrl

    // Fetch release cover art or fallback artwork if missing or spacer.gif
    LaunchedEffect(selectedVersion.id, initialCoverUrl, group.artist, group.title) {
        if (initialCoverUrl == null || initialCoverUrl.contains("spacer.gif")) {
            val coverUrl = withContext(Dispatchers.IO) {
                DiscogsService.fetchAlbumCoverFallback(group.artist, group.title, selectedVersion.id)
            }
            if (!coverUrl.isNullOrBlank()) {
                fetchedCoverUrl = coverUrl
            }
        }
    }

    val imageModel = remember(effectiveCoverUrl) {
        if (!effectiveCoverUrl.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(effectiveCoverUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36")
                .addHeader("Referer", "https://www.discogs.com/")
                .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .crossfade(true)
                .build()
        } else {
            null
        }
    }

    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor,
        padding = 10.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedVersion.id?.let { onMusicSelected(it) } },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(getAppCorners(6.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = selectedVersion.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cat_music),
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (group.artist.isNotBlank()) {
                        Text(
                            text = group.artist,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }

                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val activeFormat = selectedVersion.format?.joinToString(", ")?.takeIf { it.isNotBlank() } ?: "Release"
                    val activeYear = selectedVersion.year?.takeIf { it.isNotBlank() } ?: ""
                    val activeCountry = selectedVersion.country?.takeIf { it.isNotBlank() } ?: ""

                    val summaryParts = listOfNotNull(
                        activeYear.takeIf { it.isNotBlank() },
                        activeFormat.takeIf { it.isNotBlank() },
                        activeCountry.takeIf { it.isNotBlank() }
                    )

                    if (summaryParts.isNotEmpty()) {
                        Text(
                            text = summaryParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // VERSION DROPDOWN SELECTOR
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isDropdownExpanded = !isDropdownExpanded
                            if (!hasFetchedMasterVersions && group.masterId != null && group.masterId != 0L) {
                                scope.launch {
                                    isFetchingMasterVersions = true
                                    try {
                                        val masterVersions = withContext(Dispatchers.IO) {
                                            DiscogsService.getMasterVersions(group.masterId)
                                        }
                                        if (masterVersions.isNotEmpty()) {
                                            val converted = masterVersions.map { v ->
                                                v.toSearchResult(group.title, selectedVersion.validCoverUrl)
                                            }
                                            val combined = (versionList + converted).distinctBy { it.id }
                                            versionList = combined
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    isFetchingMasterVersions = false
                                    hasFetchedMasterVersions = true
                                }
                            }
                        },
                    color = Color.White.copy(alpha = 0.08f),
                    shape = getAppCorners(6.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val activeFormat = selectedVersion.format?.joinToString(", ")?.takeIf { it.isNotBlank() } ?: "Release"
                            val activeYear = selectedVersion.year?.takeIf { it.isNotBlank() } ?: ""
                            val activeCountry = selectedVersion.country?.takeIf { it.isNotBlank() } ?: ""
                            val activeLabel = selectedVersion.label?.firstOrNull()?.takeIf { it.isNotBlank() } ?: ""

                            val details = listOfNotNull(
                                activeFormat.takeIf { it.isNotBlank() },
                                activeYear.takeIf { it.isNotBlank() },
                                activeCountry.takeIf { it.isNotBlank() },
                                activeLabel.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")

                            Icon(
                                painter = painterResource(id = R.drawable.ic_sort),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = "VERSION: " + (details.ifBlank { "Default Version" }),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isFetchingMasterVersions) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isDropdownExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Dropdown",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                        .border(1.dp, accentColor.copy(alpha = 0.6f), getAppCorners(8.dp))
                        .heightIn(max = 240.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "ALBUM VERSIONEN (${versionList.size})".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = accentColor,
                                    fontSize = 11.sp
                                )
                            )
                        },
                        onClick = {},
                        enabled = false
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                    versionList.forEach { ver ->
                        val isSelected = ver.id == selectedVersion.id
                        val verFormat = ver.format?.joinToString(", ")?.takeIf { it.isNotBlank() } ?: "Release"
                        val verYear = ver.year?.takeIf { it.isNotBlank() } ?: ""
                        val verCountry = ver.country?.takeIf { it.isNotBlank() } ?: ""
                        val verLabel = ver.label?.firstOrNull()?.takeIf { it.isNotBlank() } ?: ""

                        val verSummary = listOfNotNull(
                            verYear.takeIf { it.isNotBlank() },
                            verFormat.takeIf { it.isNotBlank() },
                            verCountry.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")

                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Larger 62dp Cover Image
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(getAppCorners(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val verCoverUrl = ver.validCoverUrl ?: effectiveCoverUrl
                                        if (!verCoverUrl.isNullOrBlank()) {
                                            val verImageModel = remember(verCoverUrl) {
                                                ImageRequest.Builder(context)
                                                    .data(verCoverUrl)
                                                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36")
                                                    .addHeader("Referer", "https://www.discogs.com/")
                                                    .crossfade(true)
                                                    .build()
                                            }
                                            AsyncImage(
                                                model = verImageModel,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_cat_music),
                                                contentDescription = null,
                                                tint = accentColor.copy(alpha = 0.6f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                                )
                                            }
                                            if (group.artist.isNotBlank()) {
                                                Text(
                                                    text = group.artist,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                    color = accentColor
                                                )
                                            }
                                        }

                                        Text(
                                            text = ver.parsedTitle.ifBlank { group.title },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        if (verSummary.isNotEmpty()) {
                                            Text(
                                                text = verSummary,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color.White.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (verLabel.isNotBlank()) {
                                            Text(
                                                text = verLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color.White.copy(alpha = 0.5f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                selectedVersion = ver
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
