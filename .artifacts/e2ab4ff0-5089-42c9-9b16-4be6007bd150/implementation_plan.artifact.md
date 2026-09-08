# Implementation Plan - Local Cover Art Selection

The goal is to allow users to select game cover art from their device's gallery in the `AddEditGameScreen`. This URI will be saved to the database and displayed using Coil in the game list and detail screens.

## Proposed Changes

### UI Components

#### [MODIFY] [AddEditGameScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/AddEditGameScreen.kt)
- Add `ActivityResultLauncher` using `PickVisualMedia`.
- Implement `takePersistableUriPermission` to ensure the app retains access to the selected image.
- Add a visual cover art preview at the top of the form.
- Make the preview clickable to trigger the image picker.
- Replace or update the `coverUri` text field to reflect the selected image.
- Ensure the selected URI is correctly passed to the `Game` data object on save.

### Data Verification

#### [VERIFY] [GameListScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/GameListScreen.kt)
- Confirm `AsyncImage` correctly renders `content://` URIs.

#### [VERIFY] [GameDetailScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/GameDetailScreen.kt)
- Confirm `AsyncImage` correctly renders `content://` URIs.

## Verification Plan

### Automated Tests
- I will run `./gradlew :app:assembleDebug` to ensure the project still builds.

### Manual Verification
1.  Open `AddEditGameScreen`.
2.  Click on the cover art placeholder.
3.  Select an image from the gallery.
4.  Verify the preview appears in the screen.
5.  Save the game.
6.  Verify the image appears in `GameListScreen`.
7.  Verify the image appears in `GameDetailScreen`.
8.  Restart the app and verify the image is still visible (checks persistable permission).
