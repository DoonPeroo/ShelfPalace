# Match Dropdown Background to Description Field

This plan aims to achieve absolute color parity between the "GENRE" dropdown menu and the "Description" input field in `AddEditGameScreen.kt`.

## Proposed Changes

### [Component Name] UI Components

#### [MODIFY] [NeonComponents.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/components/NeonComponents.kt)
- Update `synthwaveTextFieldColors` to explicitly set `focusedContainerColor` and `unfocusedContainerColor` to `Color.Black.copy(alpha = 0.6f)`, matching the `NeonCard` background.
- Update border colors to use `MaterialTheme.colorScheme.secondary` (Cyan) instead of `primary` (Pink) to be consistent with the `NeonCard` and the user's request for "neon cyan border".

### [Component Name] Add/Edit Game Screen

#### [MODIFY] [AddEditGameScreen.kt](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/java/com/example/retrovault/ui/screens/AddEditGameScreen.kt)
- Update `ExposedDropdownMenu` `containerColor` to `Color.Black.copy(alpha = 0.6f)` to match the text fields.
- Update `ExposedDropdownMenu` border to `SynthwaveCyan` (alpha 1.0) for a sharper neon look.
- Ensure `DropdownMenuItem` colors remain consistent with "white/pink text".

## Verification Plan

### Manual Verification
- Open the "Add New Game" screen.
- Focus on the "Description" field and observe its background and border.
- Open the "Genre" dropdown and verify its background and border match the "Description" field exactly.
- Verify the dropdown items have white text and pink text for selected items.
