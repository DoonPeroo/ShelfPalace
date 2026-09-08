package com.example.retrovault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.camera.core.ExperimentalGetImage
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.retrovault.data.GameRepository
import com.example.retrovault.data.MovieRepository
import com.example.retrovault.data.MusicRepository
import com.example.retrovault.data.SettingsRepository
import com.example.retrovault.data.local.RetroVaultDatabase
import com.example.retrovault.ui.theme.RetroVaultTheme

@ExperimentalGetImage
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = RetroVaultDatabase.getDatabase(this)
        val repository = GameRepository(database.gameDao())
        val movieRepository = MovieRepository(database.movieDao())
        val musicRepository = MusicRepository(database.musicDao())
        val settingsRepository = SettingsRepository(this)
        
        setContent {
            val cornerStyle by settingsRepository.cornerStyle.collectAsState(initial = com.example.retrovault.data.CornerStyle.ROUNDED)
            
            RetroVaultTheme(corners = cornerStyle) {
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
