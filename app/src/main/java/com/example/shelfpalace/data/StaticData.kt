package com.example.shelfpalace.data

object StaticData {
    val manufacturers = listOf(
        Manufacturer("sony", "SONY", ""),
        Manufacturer("nintendo", "NINTENDO", ""),
        Manufacturer("microsoft", "MICROSOFT", ""),
        Manufacturer("sega", "SEGA", "")
    )

    val platforms = listOf(
        // Sony
        Platform("sony_ps1", "sony", "Playstation 1"),
        Platform("sony_ps2", "sony", "Playstation 2"),
        Platform("sony_ps3", "sony", "Playstation 3"),
        Platform("sony_ps4", "sony", "Playstation 4"),
        Platform("sony_ps5", "sony", "Playstation 5"),
        Platform("sony_psp", "sony", "PSP"),
        Platform("sony_psvita", "sony", "PlayStation Vita"),
        
        // Nintendo
        Platform("nintendo_nes", "nintendo", "NES"),
        Platform("nintendo_snes", "nintendo", "SNES"),
        Platform("nintendo_n64", "nintendo", "Nintendo 64"),
        Platform("nintendo_gamecube", "nintendo", "GameCube"),
        Platform("nintendo_wii", "nintendo", "Wii"),
        Platform("nintendo_wiiu", "nintendo", "Nintendo Wii U"),
        Platform("nintendo_switch", "nintendo", "Nintendo Switch"),
        Platform("nintendo_switch2", "nintendo", "Nintendo Switch 2"),
        Platform("nintendo_gb", "nintendo", "Game Boy"),
        Platform("nintendo_gbc", "nintendo", "Game Boy Color"),
        Platform("nintendo_gba", "nintendo", "Game Boy Advance"),
        Platform("nintendo_ds", "nintendo", "Nintendo DS"),
        Platform("nintendo_3ds", "nintendo", "Nintendo 3DS"),

        // Microsoft
        Platform("microsoft_xbox", "microsoft", "Xbox"),
        Platform("microsoft_xbox360", "microsoft", "Xbox 360"),
        Platform("microsoft_xboxone", "microsoft", "Xbox One"),
        Platform("microsoft_xboxseriesx", "microsoft", "Xbox Series X"),

        // Sega
        Platform("sega_ms", "sega", "Master System"),
        Platform("sega_md", "sega", "Mega Drive"),
        Platform("sega_saturn", "sega", "Sega Saturn"),
        Platform("sega_dreamcast", "sega", "Dreamcast"),
        Platform("sega_gg", "sega", "Game Gear")
    )

    val movieFormats = listOf(
        MovieFormat("vhs", "VHS", "vhs"),
        MovieFormat("laserdisc", "LaserDisc", "laserdisc"),
        MovieFormat("vcd", "Video CD", "vcd"),
        MovieFormat("dvd", "DVD", "dvd"),
        MovieFormat("hddvd", "HD DVD", "hddvd"),
        MovieFormat("bluray", "Blu-ray", "bluray"),
        MovieFormat("bluray3d", "3D Blu-ray", "bluray3d"),
        MovieFormat("bluray4k", "4K Blu-ray", "bluray4k")
    )

    val musicFormats = listOf(
        MusicFormat("cassette", "Music Cassette", "cassette"),
        MusicFormat("cd", "Music CD", "cd"),
        MusicFormat("vinyl", "Vinyl", "vinyl")
    )
}
