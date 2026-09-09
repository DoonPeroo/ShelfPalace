package com.example.shelfpalace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.camera.core.ExperimentalGetImage
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.shelfpalace.data.CornerStyle
import com.example.shelfpalace.data.GameRepository
import com.example.shelfpalace.data.MovieRepository
import com.example.shelfpalace.data.MusicRepository
import com.example.shelfpalace.data.SettingsRepository
import com.example.shelfpalace.data.local.ShelfPalaceDatabase
import com.example.shelfpalace.ui.theme.ShelfPalaceTheme

@ExperimentalGetImage
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = ShelfPalaceDatabase.getDatabase(this)
        val repository = GameRepository(database.gameDao())
        val movieRepository = MovieRepository(database.movieDao())
        val musicRepository = MusicRepository(database.musicDao())
        val settingsRepository = SettingsRepository(this)
        
        setContent {
            val cornerStyle by settingsRepository.cornerStyle.collectAsState(initial = CornerStyle.ROUNDED)
            
            ShelfPalaceTheme(corners = cornerStyle) {
                MainApp(
                    repository = repository,
                    movieRepository = movieRepository,
                    musicRepository = musicRepository,
                    settingsRepository = settingsRepository
                )
            }
        }
    }
}
