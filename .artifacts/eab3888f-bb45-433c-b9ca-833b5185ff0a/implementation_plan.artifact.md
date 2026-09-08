# Implementation Plan - UI Improvements and Stability Fixes

Address readability, edge-to-edge navigation, and stability issues in RetroVault.

## Proposed Changes

### UI Components & Theming

#### [MODIFY] [NeonComponents.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/components/NeonComponents.kt)
- Add `SectionHeader` composable with bold text, neon color, and shadow/background for high contrast.

#### [MODIFY] [Theme.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/theme/Theme.kt)
- Ensure navigation bar is transparent and configured for edge-to-edge.

### Screen Updates (Readability & Contrast)

#### [MODIFY] [ManufacturerScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/ManufacturerScreen.kt)
- Use `SectionHeader` for "SELECT MANUFACTURER".

#### [MODIFY] [PlatformScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/PlatformScreen.kt)
- Use `SectionHeader` for "SELECT PLATFORM".

#### [MODIFY] [GameListScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/GameListScreen.kt)
- Use `SectionHeader` for "GAMES" title (when search is inactive).
- Use `SynthwaveCyan` and bold style for "NO GAMES FOUND" text.

#### [MODIFY] [SearchScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/SearchScreen.kt)
- Use `SectionHeader` for "TYPE TO SEARCH GAMES".
- Update "NO RESULTS" text color to `SynthwaveCyan`.

### Stability & Scanner Improvements

#### [MODIFY] [ScannerScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/ScannerScreen.kt)
- Improve `cameraExecutor` lifecycle management (ensure it's not recreated unnecessarily).
- Add neon borders to the camera preview overlay.
- Wrap `cameraProviderFuture.get()` in a try-catch to prevent crashes if camera service is unavailable.
- Ensure `onBack` is correctly wired to `navController.popBackStack()`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/MainActivity.kt)
- Verify `NavigationSuiteScaffold` interaction with `NavHost`.
- Ensure `containerColor = Color.Transparent` is applied everywhere.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure it builds.

### Manual Verification
- Verify headers are readable on the "retro room" background.
- Verify navigation bar is transparent and background extends behind it.
- Verify Search and Scanner can be opened without the app exiting.
- Verify Scanner UI maintains the "retro atmosphere" with neon overlays.
