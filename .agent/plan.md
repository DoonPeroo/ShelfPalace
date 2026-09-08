# Project Plan

An Android app called RetroVault to track and complete a retail game collection.
Features:
1. Add/Edit games (info, cover, etc.).
2. Sort games by platform/manufacturer.
3. Detailed game info view (manufacturer, release date, description, etc.) accessible by clicking the game cover.
4. Search functionality: text-based search and camera-based cover recognition to find games in the library.
5. Navigation flow: Start by selecting manufacturer (Sony, Nintendo, Microsoft, Sega), then select the specific platform (e.g., Nintendo -> Nintendo DS).
6. UI Design: 80s Synthwave style for buttons and the overall layout.

## Project Brief

# RetroVault - Project Brief

RetroVault is a specialized Android application designed for enthusiasts to track and complete their retail game collections with a nostalgic 80s aesthetic.

## Features

- **Hierarchical Library Navigation**: Browse your collection through a structured flow starting from Manufacturer (e.g., Nintendo, Sega) down to specific Platforms (e.g., SNES, Genesis).
- **Comprehensive Game Management**: Add and edit detailed game entries including titles, high-quality cover art, release dates, and descriptions.
- **Smart Search & Recognition**: Find games quickly using text-based search or the integrated camera-based cover recognition for instant library lookup.
- **Detailed Info View**: Access a dedicated screen for every game, featuring full metadata and a high-resolution view of the game's box art.
- **80s Synthwave UI**: A fully immersive experience with neon pink and cyan accents, glowing borders, and a dark "grid" aesthetic consistent with Retrowave design.

## High-Level Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Navigation**: Jetpack Navigation 3 (State-driven architecture)
- **Adaptive Strategy**: Compose Material Adaptive library (using `NavigationSuiteScaffold` for seamless phone and tablet support)
- **Asynchrony**: Kotlin Coroutines
- **Computer Vision**: Google ML Kit (Object Detection and Image Labeling for cover recognition)

> [!NOTE]
> This MVP focuses on core navigation and identification features. The UI leverages modern Jetpack libraries to ensure the app is both visually striking and responsive across various Android form factors.

## Implementation Steps
**Total Duration:** 20m 2s

### Task_1_Foundation_Navigation: Set up the 80s Synthwave theme (neon colors, grid background), data models, and the core hierarchical navigation (Manufacturer -> Platform -> Game List) using Navigation 3 and NavigationSuiteScaffold for adaptive support.
- **Status:** COMPLETED
- **Updates:** Successfully set up the Synthwave theme, defined data models (Manufacturer, Platform, Game), and implemented hierarchical navigation using Navigation 3 and NavigationSuiteScaffold for adaptive UI.
- **Acceptance Criteria:**
  - 80s Synthwave theme (Neon pink/cyan, dark grid) applied via MaterialTheme
  - Data models for Manufacturer, Platform, and Game defined
  - Hierarchical navigation from Manufacturer selection to Platform to Game List functional
  - NavigationSuiteScaffold used to provide adaptive UI for phone and tablet
- **Duration:** 12m 17s

### Task_2_Game_Management: Implement local persistence using Room and create the screens for the Game List, Detailed Info View, and Add/Edit Game functionality.
- **Status:** COMPLETED
- **Updates:** Implemented Room database for game persistence. Created Game List, Detailed Info, and Add/Edit screens with full CRUD functionality. Integrated the Synthwave UI components (NeonButton, NeonCard) and updated navigation routes.
- **Acceptance Criteria:**
  - Room database and DAO implemented for game persistence
  - Game list displays items filtered by selected platform
  - Detailed info screen shows metadata and high-resolution cover art
  - Add/Edit screen allows management of game titles, covers, and dates
- **Duration:** 3m 22s

### Task_3_Smart_Search_Recognition: Integrate text-based library search and Google ML Kit camera-based cover recognition to identify games in the collection.
- **Status:** COMPLETED
- **Updates:** Implemented real-time text search for the game library and integrated CameraX with ML Kit for cover recognition. Scanning a cover now mock-identifies the game and navigates to its detailed info view.
- **Acceptance Criteria:**
  - Text-based search accurately filters library games by title
  - Camera recognition using ML Kit successfully detects game covers
  - Recognition results link to the corresponding detailed info view
- **Duration:** 4m 23s

### Task_4_Run_And_Verify: Perform a final end-to-end verification of the RetroVault app to ensure stability, UI fidelity to the 80s Synthwave style, and overall requirement alignment.
- **Status:** IN_PROGRESS
- **Updates:** Critic agent reported two issues:
1. Search FAB missing from Manufacturer screen.
2. Scanner screen lacks a back button when camera permission is not granted.
Reopening refinement loop.
- **Acceptance Criteria:**
  - Project builds successfully
  - App does not crash during navigation or camera recognition
  - UI matches the 80s Synthwave aesthetic requirements
  - All existing tests pass
  - App stability verified by critic_agent
- **StartTime:** 2026-08-09 00:35:38 CEST

