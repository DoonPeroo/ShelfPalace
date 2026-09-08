# Add Genre and Developer fields to Games

This plan adds "Genre" and "Developer" fields to the game data model, database, and UI.

## Proposed Changes

### Data Layer

#### [MODIFY] [Models.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/data/Models.kt)
- Add `genre: String` and `developer: String` to `Game` data class.

#### [MODIFY] [GameEntity.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/data/local/GameEntity.kt)
- Add `genre: String` and `developer: String` to `GameEntity`.
- Update `toExternalModel()` and `toEntity()` mappers.

#### [MODIFY] [RetroVaultDatabase.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/data/local/RetroVaultDatabase.kt)
- Increment database version to 2.
- Enable `fallbackToDestructiveMigration()` for simplicity in this development phase.

### UI Layer

#### [MODIFY] [AddEditGameScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/AddEditGameScreen.kt)
- Add `genre` and `developer` state variables.
- Add `OutlinedTextField` for Genre and Developer following the requested layout flow.
- Update the save logic to include these new fields.

#### [MODIFY] [GameDetailScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/GameDetailScreen.kt)
- Display Genre and Developer information in the game details card.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.
- Run the app and verify that the new fields are visible and functional.

### Manual Verification
1. Open "Add New Game" screen.
2. Verify "Genre" and "Developer" fields are present between "Release Date" and "Description".
3. Save a game with these fields.
4. Open the game details and verify the info is displayed.
5. Edit the game and verify the fields are correctly populated and can be updated.
