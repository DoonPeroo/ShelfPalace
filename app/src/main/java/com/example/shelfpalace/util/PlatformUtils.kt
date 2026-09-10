package com.example.shelfpalace.util

import androidx.compose.ui.graphics.Color
import com.example.shelfpalace.data.StaticData

object PlatformUtils {
    /**
     * Returns the aspect ratio (width / height) for a game cover based on the platform ID.
     */
    fun getAspectRatioForPlatform(platformId: String): Float {
        return when {
            // Square (1:1)
            platformId.contains("gb") || 
            platformId.contains("ds") || 
            platformId.contains("3ds") || 
            platformId.contains("gg") -> 1.0f
            
            // Horizontal (4:3)
            platformId.contains("nes") || 
            platformId.contains("snes") || 
            platformId.contains("n64") || 
            platformId.contains("md") || 
            platformId.contains("ms") -> 1.33f
            
            // Vertical (0.7f) - Default for modern and CD-based systems
            else -> 0.7f
        }
    }

    fun getShortPlatformName(platformId: String): String {
        return when (platformId) {
            "sony_ps1" -> "PS1"
            "sony_ps2" -> "PS2"
            "sony_ps3" -> "PS3"
            "sony_ps4" -> "PS4"
            "sony_ps5" -> "PS5"
            "sony_psp" -> "PSP"
            "sony_psvita" -> "VITA"
            "nintendo_nes" -> "NES"
            "nintendo_snes" -> "SNES"
            "nintendo_n64" -> "N64"
            "nintendo_gamecube" -> "GC"
            "nintendo_wii" -> "WII"
            "nintendo_wiiu" -> "WII U"
            "nintendo_switch" -> "NSW"
            "nintendo_switch2" -> "NSW 2"
            "nintendo_gb" -> "GB"
            "nintendo_gbc" -> "GBC"
            "nintendo_gba" -> "GBA"
            "nintendo_ds" -> "DS"
            "nintendo_3ds" -> "3DS"
            "microsoft_xbox" -> "XBOX"
            "microsoft_xbox360" -> "X360"
            "microsoft_xboxone" -> "XONE"
            "microsoft_xboxseriesx" -> "XSX"
            "sega_ms" -> "MS"
            "sega_md" -> "MD"
            "sega_saturn" -> "SAT"
            "sega_dreamcast" -> "DC"
            "sega_gg" -> "GG"
            else -> ""
        }
    }

    fun getPlatformName(platformId: String): String {
        return StaticData.platforms.find { it.id == platformId }?.name ?: platformId
    }

    fun getMovieFormatTag(formatId: String): String {
        return when (formatId) {
            "vhs" -> "VHS"
            "laserdisc" -> "LASERDISC"
            "vcd" -> "VCD"
            "dvd" -> "DVD"
            "hddvd" -> "HD-DVD"
            "bluray" -> "BLU-RAY"
            "bluray3d" -> "3D BLU-RAY"
            "bluray4k" -> "4K BLU-RAY"
            else -> formatId.uppercase()
        }
    }

    fun getMusicFormatTag(formatId: String): String {
        return when (formatId) {
            "cassette" -> "CASSETTE"
            "cd" -> "CD"
            "vinyl" -> "VINYL"
            else -> formatId.uppercase()
        }
    }
}

fun String.matchesSearchQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val tokens = query.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return tokens.all { token -> this.contains(token, ignoreCase = true) }
}
