package com.example.shelfpalace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.shelfpalace.R
import com.example.shelfpalace.data.remote.BarcodeLookupService
import com.example.shelfpalace.data.*
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.util.PlatformUtils
import com.example.shelfpalace.util.matchesSearchQuery
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

internal val TITLE_STOPWORDS = setOf(
    "the", "a", "an", "and", "or", "of", "for", "in", "on", "at", "to", "with",
    "und", "oder", "der", "die", "das", "des", "dem", "den", "ein", "eine", "einer", "eines",
    "mit", "für", "fuer", "von", "im", "am", "aus", "bei", "zu", "zum", "zur",
    "edition", "version", "game", "games", "spiel", "spiele", "vol", "volume", "collection", "bundle",
    "remastered", "definitive", "ultimate", "deluxe", "limited", "special", "gold",
    "premium", "standard", "cut", "director", "directors", "series", "trilogy",
)

private val GERMAN_TO_ENGLISH_MAP = mapOf(
    "der herr der ringe" to "the lord of the rings",
    "herr der ringe" to "lord of the rings",
    "die eroberung" to "the lord of the rings: conquest",
    "eroberung" to "the lord of the rings: conquest",
    "conquest" to "the lord of the rings: conquest",
    "schlacht um mittelerde" to "the lord of the rings: battle for middle earth",
    "das dritte zeitalter" to "the lord of the rings: the third age",
    "die rückkehr des königs" to "the lord of the rings: return of the king",
    "die rueckkehr des koenigs" to "the lord of the rings: return of the king",
    "die zwei türme" to "the lord of the rings: the two towers",
    "die zwei tuerme" to "the lord of the rings: the two towers",
    "die gefährten" to "the lord of the rings: fellowship of the ring",
    "die gefaehrten" to "the lord of the rings: fellowship of the ring",
    "krieg der sterne" to "star wars",
    "fluch der karibik" to "pirates of the caribbean",
    "die hüter des lichts" to "rise of the guardians",
    "hueter des lichts" to "rise of the guardians",
    "moorhuhn" to "crazy chicken",
    "schlag den raab" to "beat the raab",
    "landwirtschafts simulator" to "farming simulator",
    "landwirtschafts-simulator" to "farming simulator",
    "feuerwehr simulator" to "firefighting simulator",
    "bau simulator" to "construction simulator",
    "die siedler" to "the settlers",
    "die schlümpfe" to "the smurfs",
    "die schlumpfe" to "the smurfs",
    "wickie und die starken männer" to "vicky the viking",
    "die biene maja" to "maya the bee",
    "die simpsons" to "the simpsons",
    "drachenzähmen leicht gemacht" to "how to train your dragon",
    "ich einfach unverbesserlich" to "despicable me",
    "zoomania" to "zootopia",
    "rapunzel neu verföhnt" to "tangled",
    "die eiskönigin" to "frozen",
    "alles steht kopf" to "inside out",
    "findet nemo" to "finding nemo",
    "findet dorie" to "finding dory",
    "die unglaublichen" to "the incredibles",
    "glücksbärchis" to "care bears",
    "der magische stift" to "drawn to life",
    "magische stift" to "drawn to life",
    "das geheimnisvolle dorf" to "curious village",
    "die schatulle der panik" to "diabolical box",
    "die verlorene zukunft" to "unwound future",
    "zusammen durch die zeit" to "partners in time",
    "yogi bär" to "yogi bear",
    "yogi baer" to "yogi bear",
    "spongebob schwammkopf" to "spongebob squarepants",
    "winnie puuh" to "winnie the pooh",
    "das videospiel" to "",
    "das spiel zum film" to "",
    "meine tierarztpraxis" to "pet vet",
    "meine fohlenwelt" to "my horse park",
)

fun translateGermanToEnglish(raw: String): String {
    var title = raw.lowercase()
    GERMAN_TO_ENGLISH_MAP.entries.sortedByDescending { it.key.length }.forEach { (german, english) ->
        val regex = Regex("(?i)\\b${Regex.escape(german)}\\b")
        if (regex.containsMatchIn(title)) {
            title = title.replace(regex, english)
        }
    }
    return title
}

fun isTitleMatch(localTitle: String, resolvedTitle: String): Boolean {
    if (localTitle.isBlank() || resolvedTitle.isBlank()) return false

    val clean1 = translateGermanToEnglish(localTitle).lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()
    val clean2 = translateGermanToEnglish(resolvedTitle).lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()

    if (clean1.isEmpty() || clean2.isEmpty()) return false

    // If either string is purely numeric (like a barcode "4006209000000"), require exact equality
    if (clean1.all { it.isDigit() } || clean2.all { it.isDigit() }) {
        return clean1 == clean2
    }

    // 1. Exact normalized title match
    if (clean1 == clean2) return true

    // 2. Extract meaningful non-stopword tokens
    val words1 = clean1.split(" ").filter { it.length >= 2 && !TITLE_STOPWORDS.contains(it) }.toSet()
    val words2 = clean2.split(" ").filter { it.length >= 2 && !TITLE_STOPWORDS.contains(it) }.toSet()

    val effective1 = words1.ifEmpty { clean1.split(" ").toSet() }
    val effective2 = words2.ifEmpty { clean2.split(" ").toSet() }

    val common = effective1.intersect(effective2)
    if (common.isEmpty()) return false

    val ratio1 = common.size.toDouble() / effective1.size
    val ratio2 = common.size.toDouble() / effective2.size

    return ratio1 >= 0.85 && ratio2 >= 0.85
}

fun isExactLibraryMatch(localTitle: String, scannedTitle: String): Boolean {
    if (localTitle.isBlank() || scannedTitle.isBlank()) return false

    val clean1 = translateGermanToEnglish(localTitle).lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()
    val clean2 = translateGermanToEnglish(scannedTitle).lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()

    if (clean1.isEmpty() || clean2.isEmpty()) return false
    if (clean1 == clean2) return true

    val words1 = clean1.split(" ").filter { it.length >= 2 && !TITLE_STOPWORDS.contains(it) }.toSet()
    val words2 = clean2.split(" ").filter { it.length >= 2 && !TITLE_STOPWORDS.contains(it) }.toSet()

    val effective1 = words1.ifEmpty { clean1.split(" ").toSet() }
    val effective2 = words2.ifEmpty { clean2.split(" ").toSet() }

    val common = effective1.intersect(effective2)
    if (common.isEmpty()) return false

    val ratio1 = common.size.toDouble() / effective1.size
    val ratio2 = common.size.toDouble() / effective2.size

    return ratio1 >= 0.85 && ratio2 >= 0.85
}

fun isBarcodeMatch(itemBarcode: String?, searchBarcode: String): Boolean {
    if (itemBarcode.isNullOrBlank() || searchBarcode.isBlank()) return false
    val cleanItem = itemBarcode.lowercase().replace(Regex("[^a-z0-9]"), "")
    val cleanSearch = searchBarcode.lowercase().replace(Regex("[^a-z0-9]"), "")
    if (cleanItem.isEmpty() || cleanSearch.isEmpty()) return false

    if (cleanItem == cleanSearch) return true

    val strippedItem = cleanItem.trimStart('0')
    val strippedSearch = cleanSearch.trimStart('0')
    if (strippedItem.isNotEmpty() && strippedItem == strippedSearch) return true

    if (cleanItem.length == 12 && cleanSearch.length == 13 && cleanSearch == "0$cleanItem") return true
    if (cleanSearch.length == 12 && cleanItem.length == 13 && cleanItem == "0$cleanSearch") return true

    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    initialBarcode: String? = null,
    repository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    settingsRepository: SettingsRepository,
    onGameSelected: (String) -> Unit,
    onMovieSelected: (String) -> Unit,
    onMusicSelected: (String) -> Unit,
    onAddGame: (query: String, platformId: String?, barcode: String?) -> Unit = { _, _, _ -> },
    onAddMovie: (query: String, formatId: String?, barcode: String?) -> Unit = { _, _, _ -> },
    onAddMusic: (query: String, formatId: String?, barcode: String?) -> Unit = { _, _, _ -> },
    onScanClick: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf(initialQuery) }
    var searchBarcode by rememberSaveable { mutableStateOf(initialBarcode) }
    var selectingMediaType by remember { mutableStateOf<SelectMediaType?>(null) }

    LaunchedEffect(searchQuery) {
        val clean = searchQuery.trim()
        if (clean.all { it.isDigit() } && clean.length >= 6) {
            searchBarcode = clean
        }
    }
    

    val currentSortOption by settingsRepository.sortOption.collectAsState(initial = SortOption.NAME)
    val scope = rememberCoroutineScope()

    val allGames by repository.getAllGames().collectAsState(initial = emptyList())
    val allMovies by movieRepository.getAllMovies().collectAsState(initial = emptyList())
    val allMusic by musicRepository.getAllMusic().collectAsState(initial = emptyList())
        
    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())

    val filteredGames = remember(allGames, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_games")) emptyList()
        else {
            val enabledPlatforms = StaticData.platforms
                .filter { !disabledIds.contains(it.id) && !disabledIds.contains(it.manufacturerId) }
                .map { it.id }
                .toSet()

            val cleanQuery = searchQuery.trim()
            val queryDigits = cleanQuery.filter { it.isDigit() }
            val isBarcodeQuery = queryDigits.length >= 6 && queryDigits.length == cleanQuery.length

            val filtered = allGames.filter { game ->
                val barcodeMatches = isBarcodeMatch(game.barcode, cleanQuery)
                val titleMatches = !isBarcodeQuery && (
                    game.title.matchesSearchQuery(searchQuery) ||
                    isTitleMatch(game.title, searchQuery)
                )

                (barcodeMatches || titleMatches) && enabledPlatforms.contains(game.platformId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedWith(
                    compareBy<Game> { game ->
                        val idx = StaticData.platforms.indexOfFirst { it.id == game.platformId }
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }.thenBy { it.title.lowercase() }
                )
                SortOption.PLATFORM_NAME -> filtered.sortedWith(
                    compareBy<Game> { game ->
                        StaticData.platforms.find { it.id == game.platformId }?.name ?: ""
                    }.thenBy { it.title.lowercase() }
                )
            }
        }
    }

    val filteredMovies = remember(allMovies, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_movies")) emptyList()
        else {
            val cleanQuery = searchQuery.trim()
            val queryDigits = cleanQuery.filter { it.isDigit() }
            val isBarcodeQuery = queryDigits.length >= 6 && queryDigits.length == cleanQuery.length

            val filtered = allMovies.filter { movie ->
                val barcodeMatches = isBarcodeMatch(movie.barcode, cleanQuery)
                val titleMatches = !isBarcodeQuery && (
                    movie.title.matchesSearchQuery(searchQuery) ||
                    isTitleMatch(movie.title, searchQuery)
                )

                (barcodeMatches || titleMatches) && !disabledIds.contains(movie.formatId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedWith(
                    compareBy<Movie> { movie ->
                        val idx = StaticData.movieFormats.indexOfFirst { it.id == movie.formatId }
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }.thenBy { it.title.lowercase() }
                )
                SortOption.PLATFORM_NAME -> filtered.sortedWith(
                    compareBy<Movie> { movie ->
                        StaticData.movieFormats.find { it.id == movie.formatId }?.name ?: ""
                    }.thenBy { it.title.lowercase() }
                )
            }
        }
    }

    val filteredMusic = remember(allMusic, searchQuery, disabledIds, currentSortOption) {
        if (searchQuery.isEmpty() || disabledIds.contains("media_music")) emptyList()
        else {
            val cleanQuery = searchQuery.trim()
            val queryDigits = cleanQuery.filter { it.isDigit() }
            val isBarcodeQuery = queryDigits.length >= 6 && queryDigits.length == cleanQuery.length

            val filtered = allMusic.filter { music ->
                val barcodeMatches = isBarcodeMatch(music.barcode, cleanQuery)
                val titleMatches = !isBarcodeQuery && (
                    music.title.matchesSearchQuery(searchQuery) ||
                    music.artist.matchesSearchQuery(searchQuery) ||
                    isTitleMatch(music.title, searchQuery) ||
                    isTitleMatch(music.artist, searchQuery)
                )

                (barcodeMatches || titleMatches) && !disabledIds.contains(music.formatId)
            }
            
            when (currentSortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
                SortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate }
                SortOption.PLATFORM -> filtered.sortedWith(
                    compareBy<Music> { music ->
                        val idx = StaticData.musicFormats.indexOfFirst { it.id == music.formatId }
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }.thenBy { it.title.lowercase() }
                )
                SortOption.PLATFORM_NAME -> filtered.sortedWith(
                    compareBy<Music> { music ->
                        StaticData.musicFormats.find { it.id == music.formatId }?.name ?: ""
                    }.thenBy { it.title.lowercase() }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    NeonHeader(
                        text = "SEARCH RESULTS",
                        fullWidth = false
                    )
                },
                navigationIcon = {
                    NeonBackButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp))
                },
                actions = {
                    SortIconButton(
                        currentSortOption = currentSortOption,
                        onSortOptionSelected = { scope.launch { settingsRepository.setSortOption(it) } },
                        showConsoleSort = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (filteredGames.isEmpty() && filteredMovies.isEmpty() && filteredMusic.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "NO SEARCH QUERY" else "NO LOCAL RESULTS FOR:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )

                        if (searchQuery.isNotBlank()) {
                            val displayTitle = remember(searchQuery) {
                                val clean = searchQuery.trim()
                                if (clean.all { it.isDigit() } && clean.length >= 6) {
                                    clean
                                } else {
                                    val translated = translateGermanToEnglish(clean)
                                    BarcodeLookupService.extractGameNameFromWebTitle(translated)
                                }
                            }

                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            NeonButton(
                                text = "+ ADD GAME VIA IGDB",
                                onClick = { selectingMediaType = SelectMediaType.GAME },
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                NeonButton(
                                    text = "+ MOVIE",
                                    onClick = { selectingMediaType = SelectMediaType.MOVIE },
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                NeonButton(
                                    text = "+ MUSIC",
                                    onClick = { selectingMediaType = SelectMediaType.MUSIC },
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filteredGames.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            SectionHeader(text = "GAMES", color = MaterialTheme.colorScheme.primary)
                        }
                        items(filteredGames) { game ->
                            GameGridItem(
                                game = game,
                                onClick = { onGameSelected(game.id) },
                                aspectRatio = PlatformUtils.getAspectRatioForPlatform(game.platformId)
                            )
                        }
                    }
                    
                    if (filteredMovies.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(text = "MOVIES", color = MaterialTheme.colorScheme.secondary)
                        }
                        items(filteredMovies) { movie ->
                            MovieGridItem(
                                movie = movie,
                                onClick = { onMovieSelected(movie.id) }
                            )
                        }
                    }

                    if (filteredMusic.isNotEmpty()) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(text = "MUSIC", color = MaterialTheme.colorScheme.secondary)
                        }
                        items(filteredMusic) { music ->
                            MusicGridItem(
                                music = music,
                                onClick = { onMusicSelected(music.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectingMediaType != null) {
        val currentType = selectingMediaType!!
        val titleText = when (currentType) {
            SelectMediaType.GAME -> "SELECT CONSOLE / PLATFORM"
            SelectMediaType.MOVIE -> "SELECT MOVIE FORMAT"
            SelectMediaType.MUSIC -> "SELECT MUSIC FORMAT"
        }

        val availablePlatforms = remember(disabledIds) {
            StaticData.platforms.filter { !disabledIds.contains(it.id) && !disabledIds.contains(it.manufacturerId) }
                .ifEmpty { StaticData.platforms }
        }
        val availableMovieFormats = remember(disabledIds) {
            StaticData.movieFormats.filter { !disabledIds.contains(it.id) }
                .ifEmpty { StaticData.movieFormats }
        }
        val availableMusicFormats = remember(disabledIds) {
            StaticData.musicFormats.filter { !disabledIds.contains(it.id) }
                .ifEmpty { StaticData.musicFormats }
        }

        val options = when (currentType) {
            SelectMediaType.GAME -> availablePlatforms.map { it.name }
            SelectMediaType.MOVIE -> availableMovieFormats.map { it.name }
            SelectMediaType.MUSIC -> availableMusicFormats.map { it.name }
        }

        val accentColor = when (currentType) {
            SelectMediaType.GAME -> MaterialTheme.colorScheme.primary
            SelectMediaType.MOVIE -> MaterialTheme.colorScheme.secondary
            SelectMediaType.MUSIC -> MaterialTheme.colorScheme.secondary
        }

        SelectFormatDialog(
            title = titleText,
            options = options,
            initialSelected = options.firstOrNull() ?: "",
            accentColor = accentColor,
            onConfirm = { selectedName ->
                selectingMediaType = null
                val effectiveTitle = if (searchQuery.all { it.isDigit() } && searchQuery.length >= 6) {
                    searchQuery
                } else {
                    val translated = translateGermanToEnglish(searchQuery.trim())
                    BarcodeLookupService.extractGameNameFromWebTitle(translated)
                }

                when (currentType) {
                    SelectMediaType.GAME -> {
                        val platform = availablePlatforms.find { it.name == selectedName } ?: StaticData.platforms.find { it.name == selectedName }
                        onAddGame(effectiveTitle, platform?.id, searchBarcode)
                    }
                    SelectMediaType.MOVIE -> {
                        val format = availableMovieFormats.find { it.name == selectedName } ?: StaticData.movieFormats.find { it.name == selectedName }
                        onAddMovie(effectiveTitle, format?.id, searchBarcode)
                    }
                    SelectMediaType.MUSIC -> {
                        val format = availableMusicFormats.find { it.name == selectedName } ?: StaticData.musicFormats.find { it.name == selectedName }
                        onAddMusic(effectiveTitle, format?.id, searchBarcode)
                    }
                }
            },
            onDismiss = { selectingMediaType = null }
        )
    }
}

enum class SelectMediaType {
    GAME, MOVIE, MUSIC
}

@Composable
fun SelectFormatDialog(
    title: String,
    options: List<String>,
    initialSelected: String,
    accentColor: Color,
    onConfirm: (selectedName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(initialSelected.ifBlank { options.firstOrNull() ?: "" }) }

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            color = accentColor,
            containerAlpha = 0.95f,
            padding = 16.dp,
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = accentColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Please select target console/format:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                FormDropdownField(
                    label = "Console / Format",
                    selectedValue = selectedOption,
                    options = options,
                    onOptionSelected = { selectedOption = it },
                    placeholder = "Select...",
                    accentColor = accentColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        color = Color.White.copy(alpha = 0.85f),
                        containerColor = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )

                    NeonButton(
                        text = "CONTINUE",
                        onClick = {
                            if (selectedOption.isNotBlank()) {
                                onConfirm(selectedOption)
                            }
                        },
                        color = accentColor,
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )
                }
            }
        }
    }
}

