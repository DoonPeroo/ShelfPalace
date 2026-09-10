package com.example.shelfpalace

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.data.StaticData
import com.example.shelfpalace.data.local.ShelfPalaceDatabase
import com.example.shelfpalace.navigation.Destinations
import com.example.shelfpalace.ui.components.*
import com.example.shelfpalace.ui.screens.*
import com.example.shelfpalace.ui.theme.ShelfPalaceTheme
import kotlinx.coroutines.launch

@ExperimentalGetImage
@Composable
fun MainApp(
    repository: GameRepository,
    movieRepository: MovieRepository,
    musicRepository: MusicRepository,
    settingsRepository: SettingsRepository,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showPlatformSelection by remember { mutableStateOf(false) }
    var selectedManufacturerFilter by remember { mutableStateOf<String?>(null) }
    var showMovieFormatSelection by remember { mutableStateOf(false) }
    var showMusicFormatSelection by remember { mutableStateOf(false) }

    val isRoot = currentDestination?.hasRoute<Destinations.LibraryDashboard>() == true
    var backPressedTime by remember { mutableStateOf(0L) }
    val context = LocalContext.current

    BackHandler(enabled = isRoot) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            backPressedTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ShelfPalaceBackground()
        
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val visible = currentDestination?.hierarchy?.any { 
                    it.hasRoute<Destinations.LibraryDashboard>() ||
                    it.hasRoute<Destinations.ManufacturerList>() ||
                    it.hasRoute<Destinations.MediaSelection>() ||
                    it.hasRoute<Destinations.Favorites>() ||
                    it.hasRoute<Destinations.Statistics>() ||
                    it.hasRoute<Destinations.Settings>() ||
                    it.hasRoute<Destinations.GameList>() ||
                    it.hasRoute<Destinations.MovieList>() ||
                    it.hasRoute<Destinations.MusicList>() ||
                    it.hasRoute<Destinations.MovieFormatList>() ||
                    it.hasRoute<Destinations.MusicFormatList>() ||
                    it.hasRoute<Destinations.PlatformList>() ||
                    it.hasRoute<Destinations.GameDetail>() ||
                    it.hasRoute<Destinations.MovieDetail>() ||
                    it.hasRoute<Destinations.MusicDetail>()
                } == true

                if (visible) {
                    val activeTab = when {
                        currentDestination?.hierarchy?.any { 
                            it.hasRoute<Destinations.LibraryDashboard>() ||
                            it.hasRoute<Destinations.ManufacturerList>() || 
                            it.hasRoute<Destinations.MediaSelection>() ||
                            it.hasRoute<Destinations.GameList>() ||
                            it.hasRoute<Destinations.MovieList>() ||
                            it.hasRoute<Destinations.MusicList>() ||
                            it.hasRoute<Destinations.MovieFormatList>() ||
                            it.hasRoute<Destinations.MusicFormatList>() ||
                            it.hasRoute<Destinations.PlatformList>() ||
                            it.hasRoute<Destinations.GameDetail>() ||
                            it.hasRoute<Destinations.MovieDetail>() ||
                            it.hasRoute<Destinations.MusicDetail>()
                        } == true -> NavTab.LIBRARY
                        currentDestination?.hierarchy?.any { it.hasRoute<Destinations.Favorites>() } == true -> NavTab.FAVORITES
                        currentDestination?.hierarchy?.any { it.hasRoute<Destinations.Statistics>() } == true -> NavTab.STATISTICS
                        currentDestination?.hierarchy?.any { it.hasRoute<Destinations.Settings>() } == true -> NavTab.SETTINGS
                        else -> NavTab.NONE
                    }

                    val isAddActive = showAddDialog || showPlatformSelection || showMovieFormatSelection || showMusicFormatSelection

                    ShelfPalaceBottomBar(
                        onLibraryClick = {
                            navController.navigate(Destinations.LibraryDashboard) {
                                popUpTo(Destinations.LibraryDashboard) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onFavoriteClick = {
                            navController.navigate(Destinations.Favorites) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAddClick = { 
                            val dest = currentDestination ?: return@ShelfPalaceBottomBar

                            val isHome = dest.hasRoute<Destinations.LibraryDashboard>()
                            
                            val isGameArea = dest.hierarchy.any { 
                                it.hasRoute<Destinations.ManufacturerList>() ||
                                it.hasRoute<Destinations.PlatformList>() ||
                                it.hasRoute<Destinations.GameList>() ||
                                it.hasRoute<Destinations.GameDetail>() ||
                                it.hasRoute<Destinations.AddEditGame>()
                            }
                            
                            val isMovieArea = dest.hierarchy.any { 
                                it.hasRoute<Destinations.MovieFormatList>() ||
                                it.hasRoute<Destinations.MovieList>() ||
                                it.hasRoute<Destinations.MovieDetail>() ||
                                it.hasRoute<Destinations.AddEditMovie>()
                            }
                            
                            val isMusicArea = dest.hierarchy.any { 
                                it.hasRoute<Destinations.MusicFormatList>() ||
                                it.hasRoute<Destinations.MusicList>() ||
                                it.hasRoute<Destinations.MusicDetail>() ||
                                it.hasRoute<Destinations.AddEditMusic>()
                            }

                            if (isHome) {
                                selectedManufacturerFilter = null
                                showAddDialog = true
                            } else if (isGameArea) {
                                if (dest.hasRoute<Destinations.PlatformList>()) {
                                    val entry = navBackStackEntry ?: return@ShelfPalaceBottomBar
                                    val route: Destinations.PlatformList = entry.toRoute()
                                    selectedManufacturerFilter = route.manufacturerId
                                    showPlatformSelection = true
                                } else if (dest.hasRoute<Destinations.GameList>()) {
                                    val entry = navBackStackEntry ?: return@ShelfPalaceBottomBar
                                    val route: Destinations.GameList = entry.toRoute()
                                    if (route.platformId == "all") {
                                        selectedManufacturerFilter = null
                                        showPlatformSelection = true
                                    } else {
                                        navController.navigate(Destinations.AddEditGame(platformId = route.platformId))
                                    }
                                } else if (dest.hasRoute<Destinations.GameDetail>()) {
                                    val entry = navBackStackEntry ?: return@ShelfPalaceBottomBar
                                    val route: Destinations.GameDetail = entry.toRoute()
                                    scope.launch {
                                        val game = repository.getGameById(route.gameId)
                                        game?.let {
                                            navController.navigate(Destinations.AddEditGame(platformId = it.platformId))
                                        }
                                    }
                                } else {
                                    selectedManufacturerFilter = null
                                    showPlatformSelection = true
                                }
                            } else if (isMovieArea) {
                                if (dest.hasRoute<Destinations.MovieList>()) {
                                    val entry = navBackStackEntry ?: return@ShelfPalaceBottomBar
                                    val route: Destinations.MovieList = entry.toRoute()
                                    if (route.formatId == "all") {
                                        showMovieFormatSelection = true
                                    } else {
                                        navController.navigate(Destinations.AddEditMovie(formatId = route.formatId))
                                    }
                                } else if (dest.hasRoute<Destinations.MovieDetail>()) {
                                    val entry = navBackStackEntry ?: return@ShelfPalaceBottomBar
                                    val route: Destinations.MovieDetail = entry.toRoute()
                                    scope.launch {
                                        val movie = movieRepository.getMovieById(route.movieId)
                                        movie?.let {
                                            navController.navigate(Destinations.AddEditMovie(formatId = it.formatId))
                                        }
                                    }
                                } else {
                                    showMovieFormatSelection = true
                                }
                            } else if (isMusicArea) {
                                if (dest.hasRoute<Destinations.MusicList>()) {
                                    val entry = navBackStackEntry ?: return@ShelfPalaceBottomBar
                                    val route: Destinations.MusicList = entry.toRoute()
                                    if (route.formatId == "all") {
                                        showMusicFormatSelection = true
                                    } else {
                                        navController.navigate(Destinations.AddEditMusic(formatId = route.formatId))
                                    }
                                } else if (dest.hasRoute<Destinations.MusicDetail>()) {
                                    val entry = navBackStackEntry ?: return@ShelfPalaceBottomBar
                                    val route: Destinations.MusicDetail = entry.toRoute()
                                    scope.launch {
                                        val music = musicRepository.getMusicById(route.musicId)
                                        music?.let {
                                            navController.navigate(Destinations.AddEditMusic(formatId = it.formatId))
                                        }
                                    }
                                } else {
                                    showMusicFormatSelection = true
                                }
                            } else {
                                selectedManufacturerFilter = null
                                showAddDialog = true 
                            }
                        },
                        onStatisticsClick = {
                            navController.navigate(Destinations.Statistics) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSettingsClick = {
                            navController.navigate(Destinations.Settings) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        activeTab = activeTab,
                        isAddActive = isAddActive
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Destinations.LibraryDashboard,
                modifier = Modifier.padding(padding)
            ) {
                composable<Destinations.LibraryDashboard> {
                    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
                    val dashboardFilter by settingsRepository.dashboardFilter.collectAsState(initial = "Recently Added")
                    val scope = rememberCoroutineScope()
                    
                    LibraryDashboardScreen(
                        gameRepository = repository,
                        movieRepository = movieRepository,
                        musicRepository = musicRepository,
                        onGameClick = { id -> navController.navigate(Destinations.GameDetail(id)) },
                        onMovieClick = { id -> navController.navigate(Destinations.MovieDetail(id)) },
                        onMusicClick = { id -> navController.navigate(Destinations.MusicDetail(id)) },
                        onGamesHeaderClick = { query ->
                            if (query.isNotBlank()) {
                                navController.navigate(Destinations.Search(initialQuery = query))
                            } else {
                                navController.navigate(Destinations.GameList(platformId = "all"))
                            }
                        },
                        onManufacturerSelected = { id -> navController.navigate(Destinations.PlatformList(id)) },
                        onPlatformSelected = { id -> navController.navigate(Destinations.GameList(platformId = id)) },
                        onMoviesHeaderClick = { query ->
                            if (query.isNotBlank()) {
                                navController.navigate(Destinations.Search(initialQuery = query))
                            } else {
                                navController.navigate(Destinations.MovieList(formatId = "all"))
                            }
                        },
                        onMovieFormatSelected = { id -> navController.navigate(Destinations.MovieList(id)) },
                        onMusicHeaderClick = { query ->
                            if (query.isNotBlank()) {
                                navController.navigate(Destinations.Search(initialQuery = query))
                            } else {
                                navController.navigate(Destinations.MusicList(formatId = "all"))
                            }
                        },
                        onMusicFormatSelected = { id -> navController.navigate(Destinations.MusicList(id)) },
                        onScanClick = { navController.navigate(Destinations.Scanner) },
                        disabledIds = disabledIds,
                        filterOption = dashboardFilter,
                        onFilterOptionChange = { scope.launch { settingsRepository.setDashboardFilter(it) } }
                    )
                }
                composable<Destinations.ManufacturerList> {
                    ManufacturerScreen(
                        settingsRepository = settingsRepository,
                        onManufacturerSelected = { id ->
                            navController.navigate(Destinations.PlatformList(id))
                        },
                        onSearchClick = {
                            navController.navigate(Destinations.Search())
                        },
                        onScanClick = {
                            navController.navigate(Destinations.Scanner)
                        },
                        onMoviesClick = {
                            navController.navigate(Destinations.MediaSelection)
                        }
                    )
                }
                composable<Destinations.Settings> {
                    SettingsScreen(
                        repository = settingsRepository,
                        gameRepository = repository,
                        movieRepository = movieRepository,
                        musicRepository = musicRepository,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable<Destinations.PlatformList> { backStackEntry ->
                    val route: Destinations.PlatformList = backStackEntry.toRoute()
                    PlatformScreen(
                        manufacturerId = route.manufacturerId,
                        repository = repository,
                        settingsRepository = settingsRepository,
                        onPlatformSelected = { id ->
                            navController.navigate(Destinations.GameList(id))
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.GameList> { backStackEntry ->
                    val route: Destinations.GameList = backStackEntry.toRoute()
                    GameListScreen(
                        platformId = route.platformId,
                        repository = repository,
                        settingsRepository = settingsRepository,
                        onGameSelected = { gameId ->
                            navController.navigate(Destinations.GameDetail(gameId))
                        },
                        onAddGame = {
                            val id = if (route.platformId == "all") null else route.platformId
                            navController.navigate(Destinations.AddEditGame(platformId = id))
                        },
                        onScanClick = {
                            navController.navigate(Destinations.Scanner)
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.Scanner> {
                    ScannerScreen(
                        repository = repository,
                        movieRepository = movieRepository,
                        musicRepository = musicRepository,
                        onGameRecognized = { gameId ->
                            navController.navigate(Destinations.GameDetail(gameId))
                        },
                        onMovieRecognized = { movieId ->
                            navController.navigate(Destinations.MovieDetail(movieId))
                        },
                        onMusicRecognized = { musicId ->
                            navController.navigate(Destinations.MusicDetail(musicId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable<Destinations.Search> { backStackEntry ->
                    val route: Destinations.Search = backStackEntry.toRoute()
                    SearchScreen(
                        initialQuery = route.initialQuery,
                        repository = repository,
                        movieRepository = movieRepository,
                        musicRepository = musicRepository,
                        settingsRepository = settingsRepository,
                        onGameSelected = { gameId ->
                            navController.navigate(Destinations.GameDetail(gameId))
                        },
                        onMovieSelected = { movieId ->
                            navController.navigate(Destinations.MovieDetail(movieId))
                        },
                        onMusicSelected = { musicId ->
                            navController.navigate(Destinations.MusicDetail(musicId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable<Destinations.GameDetail> { backStackEntry ->
                    val route: Destinations.GameDetail = backStackEntry.toRoute()
                    GameDetailScreen(
                        gameId = route.gameId,
                        repository = repository,
                        onEditGame = { gameId ->
                            navController.navigate(Destinations.AddEditGame(gameId = gameId))
                        },
                        onPlatformClick = { platformId ->
                            navController.navigate(Destinations.GameList(platformId))
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.AddEditGame> { backStackEntry ->
                    val route: Destinations.AddEditGame = backStackEntry.toRoute()
                    val isEditMode = route.gameId != null
                    
                    AddEditGameScreen(
                        platformId = route.platformId,
                        gameId = route.gameId,
                        igdbId = route.igdbId,
                        repository = repository,
                        onSave = { savedGameId ->
                            if (isEditMode) {
                                if (route.igdbId != null) {
                                    navController.popBackStack<Destinations.IgdbSearch>(inclusive = true)
                                    navController.popBackStack()
                                } else {
                                    navController.popBackStack() 
                                }
                            } else {
                                if (route.igdbId != null) {
                                    navController.popBackStack<Destinations.IgdbSearch>(inclusive = true)
                                    navController.popBackStack()
                                } else {
                                    navController.popBackStack()
                                }
                                navController.navigate(Destinations.GameDetail(savedGameId))
                            }
                        },
                        onIgdbSearch = { currentTitle ->
                            navController.navigate(
                                Destinations.IgdbSearch(
                                    platformId = route.platformId, 
                                    gameId = route.gameId, 
                                    initialQuery = currentTitle
                                )
                            )
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            if (isEditMode) {
                                navController.popBackStack()
                            } else {
                                navController.popBackStack(Destinations.LibraryDashboard, false)
                            }
                        }
                    )
                }
                
                composable<Destinations.IgdbSearch> { backStackEntry ->
                    val route: Destinations.IgdbSearch = backStackEntry.toRoute()
                    IgdbSearchScreen(
                        platformId = route.platformId,
                        initialQuery = route.initialQuery,
                        onGameSelected = { igdbId: Long ->
                            navController.navigate(
                                Destinations.AddEditGame(
                                    platformId = route.platformId, 
                                    gameId = route.gameId, 
                                    igdbId = igdbId
                                )
                            )
                        },
                        onBack = { navController.popBackStack() },
                        onClose = {
                            navController.popBackStack<Destinations.AddEditGame>(inclusive = true)
                        }
                    )
                }
                
                composable<Destinations.MediaSelection> {
                    MediaSelectionScreen(
                        movieRepository = movieRepository,
                        musicRepository = musicRepository,
                        settingsRepository = settingsRepository,
                        onMoviesClick = { navController.navigate(Destinations.MovieFormatList) },
                        onMusicClick = { navController.navigate(Destinations.MusicFormatList) },
                        onMovieSelected = { movieId ->
                            navController.navigate(Destinations.MovieDetail(movieId))
                        },
                        onMusicSelected = { musicId ->
                            navController.navigate(Destinations.MusicDetail(musicId))
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { navController.popBackStack(Destinations.LibraryDashboard, false) }
                    )
                }
                
                // Movie Routes
                composable<Destinations.MovieFormatList> {
                    MovieFormatScreen(
                        repository = movieRepository,
                        settingsRepository = settingsRepository,
                        onFormatSelected = { id ->
                            navController.navigate(Destinations.MovieList(id))
                        },
                        onSearchClick = {
                            navController.navigate(Destinations.Search())
                        },
                        onScanClick = {
                            navController.navigate(Destinations.Scanner)
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.MovieList> { backStackEntry ->
                    val route: Destinations.MovieList = backStackEntry.toRoute()
                    MovieListScreen(
                        formatId = route.formatId,
                        repository = movieRepository,
                        settingsRepository = settingsRepository,
                        onMovieSelected = { movieId ->
                            navController.navigate(Destinations.MovieDetail(movieId))
                        },
                        onAddMovie = {
                            val id = if (route.formatId == "all") null else route.formatId
                            navController.navigate(Destinations.AddEditMovie(formatId = id))
                        },
                        onScanClick = {
                            navController.navigate(Destinations.Scanner)
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.MovieDetail> { backStackEntry ->
                    val route: Destinations.MovieDetail = backStackEntry.toRoute()
                    MovieDetailScreen(
                        movieId = route.movieId,
                        repository = movieRepository,
                        onEditMovie = { movieId ->
                            navController.navigate(Destinations.AddEditMovie(movieId = movieId))
                        },
                        onFormatClick = { formatId ->
                            navController.navigate(Destinations.MovieList(formatId))
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.AddEditMovie> { backStackEntry ->
                    val route: Destinations.AddEditMovie = backStackEntry.toRoute()
                    val isEditMode = route.movieId != null
                    
                    AddEditMovieScreen(
                        formatId = route.formatId,
                        movieId = route.movieId,
                        repository = movieRepository,
                        onSave = { savedMovieId ->
                            if (isEditMode) {
                                navController.popBackStack()
                            } else {
                                navController.popBackStack()
                                navController.navigate(Destinations.MovieDetail(savedMovieId))
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                // Music Routes
                composable<Destinations.MusicFormatList> {
                    MusicFormatScreen(
                        repository = musicRepository,
                        settingsRepository = settingsRepository,
                        onFormatSelected = { id ->
                            navController.navigate(Destinations.MusicList(id))
                        },
                        onSearchClick = {
                            navController.navigate(Destinations.Search())
                        },
                        onScanClick = {
                            navController.navigate(Destinations.Scanner)
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.MusicList> { backStackEntry ->
                    val route: Destinations.MusicList = backStackEntry.toRoute()
                    MusicListScreen(
                        formatId = route.formatId,
                        repository = musicRepository,
                        settingsRepository = settingsRepository,
                        onMusicSelected = { musicId ->
                            navController.navigate(Destinations.MusicDetail(musicId))
                        },
                        onAddMusic = {
                            val id = if (route.formatId == "all") null else route.formatId
                            navController.navigate(Destinations.AddEditMusic(formatId = id))
                        },
                        onScanClick = {
                            navController.navigate(Destinations.Scanner)
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.MusicDetail> { backStackEntry ->
                    val route: Destinations.MusicDetail = backStackEntry.toRoute()
                    MusicDetailScreen(
                        musicId = route.musicId,
                        repository = musicRepository,
                        onEditMusic = { musicId ->
                            navController.navigate(Destinations.AddEditMusic(musicId = musicId))
                        },
                        onFormatClick = { formatId ->
                            navController.navigate(Destinations.MusicList(formatId))
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                composable<Destinations.AddEditMusic> { backStackEntry ->
                    val route: Destinations.AddEditMusic = backStackEntry.toRoute()
                    val isEditMode = route.musicId != null
                    
                    AddEditMusicScreen(
                        formatId = route.formatId,
                        musicId = route.musicId,
                        repository = musicRepository,
                        onSave = { savedMusicId ->
                            if (isEditMode) {
                                navController.popBackStack()
                            } else {
                                navController.popBackStack()
                                navController.navigate(Destinations.MusicDetail(savedMusicId))
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onHome = { 
                            navController.popBackStack(Destinations.LibraryDashboard, false)
                        }
                    )
                }
                
                composable<Destinations.Favorites> {
                    FavoritesScreen(
                        gameRepository = repository,
                        movieRepository = movieRepository,
                        musicRepository = musicRepository,
                        settingsRepository = settingsRepository,
                        onGameSelected = { gameId ->
                            navController.navigate(Destinations.GameDetail(gameId))
                        },
                        onMovieSelected = { movieId ->
                            navController.navigate(Destinations.MovieDetail(movieId))
                        },
                        onMusicSelected = { musicId ->
                            navController.navigate(Destinations.MusicDetail(musicId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable<Destinations.Statistics> {
                    StatisticsScreen(
                        gameRepository = repository,
                        movieRepository = movieRepository,
                        musicRepository = musicRepository,
                        settingsRepository = settingsRepository,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddChoiceDialog(
            onDismiss = { showAddDialog = false },
            onAddGame = {
                showAddDialog = false
                showPlatformSelection = true
            },
            onAddMovie = {
                showAddDialog = false
                showMovieFormatSelection = true
            },
            onAddMusic = {
                showAddDialog = false
                showMusicFormatSelection = true
            }
        )
    }

    if (showPlatformSelection) {
        PlatformSelectionDialog(
            manufacturerIdFilter = selectedManufacturerFilter,
            settingsRepository = settingsRepository,
            onDismiss = { 
                showPlatformSelection = false
                selectedManufacturerFilter = null
            },
            onBack = {
                showPlatformSelection = false
                selectedManufacturerFilter = null
                showAddDialog = true
            },
            onPlatformSelected = { platformId ->
                navController.navigate(Destinations.AddEditGame(platformId = platformId))
                showPlatformSelection = false
                selectedManufacturerFilter = null
            }
        )
    }

    if (showMovieFormatSelection) {
        MovieFormatSelectionDialog(
            onDismiss = { showMovieFormatSelection = false },
            onBack = {
                showMovieFormatSelection = false
                showAddDialog = true
            },
            onFormatSelected = { formatId ->
                navController.navigate(Destinations.AddEditMovie(formatId = formatId))
                showMovieFormatSelection = false
            }
        )
    }

    if (showMusicFormatSelection) {
        MusicFormatSelectionDialog(
            onDismiss = { showMusicFormatSelection = false },
            onBack = {
                showMusicFormatSelection = false
                showAddDialog = true
            },
            onFormatSelected = { formatId ->
                navController.navigate(Destinations.AddEditMusic(formatId = formatId))
                showMusicFormatSelection = false
            }
        )
    }
}

@Composable
fun MovieFormatSelectionDialog(
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onFormatSelected: (String) -> Unit
) {
    val formats = StaticData.movieFormats
    
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            color = MaterialTheme.colorScheme.primary,
            containerAlpha = 0.9f,
            padding = 11.dp,
            modifier = Modifier.width(262.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.back),
                        onClick = onBack,
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "SELECT FORMAT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.size(28.dp))
                }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(formats) { format ->
                        NeonButton(
                            text = format.name,
                            onClick = { onFormatSelected(format.id) },
                            modifier = Modifier.fillMaxWidth(),
                            height = 41.dp,
                            color = Color.White.copy(alpha = 0.6f),
                            containerColor = Color.White.copy(alpha = 0.08f)
                        )
                    }
                }
                
                NeonButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.width(110.dp),
                    height = 32.dp,
                    color = Color.White.copy(alpha = 0.6f),
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
            }
        }
    }
}

@Composable
fun MusicFormatSelectionDialog(
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onFormatSelected: (String) -> Unit
) {
    val formats = StaticData.musicFormats
    
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            color = MaterialTheme.colorScheme.primary,
            containerAlpha = 0.9f,
            padding = 11.dp,
            modifier = Modifier.width(262.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.back),
                        onClick = onBack,
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "SELECT FORMAT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.size(28.dp))
                }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(formats) { format ->
                        NeonButton(
                            text = format.name,
                            onClick = { onFormatSelected(format.id) },
                            modifier = Modifier.fillMaxWidth(),
                            height = 41.dp,
                            color = Color.White.copy(alpha = 0.6f),
                            containerColor = Color.White.copy(alpha = 0.08f)
                        )
                    }
                }
                
                NeonButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.width(110.dp),
                    height = 32.dp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun PlatformSelectionDialog(
    manufacturerIdFilter: String? = null,
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onPlatformSelected: (String) -> Unit
) {
    val disabledIds by settingsRepository.disabledIds.collectAsState(initial = emptySet())
    val manufacturers = StaticData.manufacturers.filter { manufacturer ->
        !disabledIds.contains(manufacturer.id) && (manufacturerIdFilter == null || manufacturer.id == manufacturerIdFilter)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            color = MaterialTheme.colorScheme.primary,
            containerAlpha = 0.9f,
            padding = 11.dp,
            modifier = Modifier
                .width(262.dp)
                .fillMaxHeight(0.75f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonIconButton(
                        iconPainter = painterResource(id = R.drawable.back),
                        onClick = onBack,
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "SELECT CONSOLE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.size(28.dp))
                }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    manufacturers.forEach { manufacturer ->
                        item {
                            Text(
                                text = manufacturer.name,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        
                        val platforms = StaticData.platforms.filter { 
                            it.manufacturerId == manufacturer.id && !disabledIds.contains(it.id)
                        }
                        
                        items(platforms) { platform ->
                            NeonButton(
                                text = platform.name,
                                onClick = { onPlatformSelected(platform.id) },
                                modifier = Modifier.fillMaxWidth(),
                                height = 41.dp,
                                color = Color.White.copy(alpha = 0.6f),
                                containerColor = Color.White.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
                
                NeonButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.width(110.dp),
                    height = 32.dp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun AddChoiceDialog(
    onDismiss: () -> Unit,
    onAddGame: () -> Unit,
    onAddMovie: () -> Unit,
    onAddMusic: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            color = MaterialTheme.colorScheme.primary,
            containerAlpha = 0.9f,
            padding = 12.dp,
            modifier = Modifier.width(250.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ADD TO LIBRARY",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                NeonButton(
                    text = "NEW GAME",
                    onClick = onAddGame,
                    modifier = Modifier.fillMaxWidth(),
                    height = 41.dp,
                    color = Color.White.copy(alpha = 0.6f),
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
                
                NeonButton(
                    text = "NEW MOVIE",
                    onClick = onAddMovie,
                    modifier = Modifier.fillMaxWidth(),
                    height = 41.dp,
                    color = Color.White.copy(alpha = 0.6f),
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
                
                NeonButton(
                    text = "NEW MUSIC",
                    onClick = onAddMusic,
                    modifier = Modifier.fillMaxWidth(),
                    height = 41.dp,
                    color = Color.White.copy(alpha = 0.6f),
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
                
                NeonButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.width(110.dp),
                    height = 32.dp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@ExperimentalGetImage
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MainAppPreview() {
    val context = LocalContext.current
    val database = remember { ShelfPalaceDatabase.getDatabase(context) }
    val repository = remember { GameRepository(database.gameDao()) }
    val movieRepository = remember { MovieRepository(database.movieDao()) }
    val musicRepository = remember { MusicRepository(database.musicDao()) }
    val settingsRepository = remember { SettingsRepository(context) }
    
    ShelfPalaceTheme {
        MainApp(
            repository = repository,
            movieRepository = movieRepository,
            musicRepository = musicRepository,
            settingsRepository = settingsRepository
        )
    }
}

@ExperimentalGetImage
@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun MainAppTabletPreview() {
    val context = LocalContext.current
    val database = remember { ShelfPalaceDatabase.getDatabase(context) }
    val repository = remember { GameRepository(database.gameDao()) }
    val movieRepository = remember { MovieRepository(database.movieDao()) }
    val musicRepository = remember { MusicRepository(database.musicDao()) }
    val settingsRepository = remember { SettingsRepository(context) }
    
    ShelfPalaceTheme {
        MainApp(
            repository = repository,
            movieRepository = movieRepository,
            musicRepository = musicRepository,
            settingsRepository = settingsRepository
        )
    }
}
