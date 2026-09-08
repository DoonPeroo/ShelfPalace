# Implement New Bottom Navigation Bar

Replace the standard icons (Favorite, Statistics, Settings) with a new custom bottom navigation bar as seen in the provided screenshot.

## Proposed Changes

### [UI Components]

#### [NEW] [RetroVaultBottomBar.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault_0.70/app/src/main/java/com/example/retrovault/ui/components/RetroVaultBottomBar.kt)
Create a new component for the bottom navigation bar with the following items:
- **Library**: Grid icon, navigates to the home screen.
- **Wishlist**: Heart icon, navigates to favorites.
- **Add (+)**: Central button in a rounded box, opens a dialog to add Game, Movie, or Music.
- **Statistics**: Bar chart icon, navigates to statistics.
- **Settings**: Gear icon, navigates to settings.

### [Main Application Shell]

#### [MODIFY] [MainApp.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault_0.70/app/src/main/java/com/example/retrovault/MainApp.kt)
- Integrate `RetroVaultBottomBar` into the main application shell using a `Scaffold`.
- Implement navigation logic for each bottom bar item.
- Add a state to handle the "Add" button dialog (to choose between adding Game, Movie, or Music).

#### [MODIFY] [ManufacturerScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault_0.70/app/src/main/java/com/example/retrovault/ui/screens/ManufacturerScreen.kt)
- Remove the existing bottom icons as they will be replaced by the global bottom navigation bar.

### [Other Screens]
Review other screens to ensure they work well with the new bottom bar or hide it where necessary (e.g., detail screens).

## Verification Plan

### Manual Verification
- Deploy the app and verify the bottom bar is visible.
- Click on "Library" and ensure it returns to the home screen.
- Click on "+" and verify the add options appear.
- Click on "Wishlist", "Statistics", and "Settings" to verify navigation.
- Check the visual styling against the provided screenshot.
