# Replace App Icon with High-Quality Retro Vault Image

This plan replaces the current pixelated app icon with a high-quality stylized image (`RetroVaultIcon.png`) across all density levels and updates the adaptive icon configuration.

## User Review Required

> [!IMPORTANT]
> I located a high-quality icon image named `RetroVaultIcon.png` in your Downloads folder which perfectly matches the "RetroVault" theme. I will use this image as the source for the new app icons. Please confirm if this is the correct image (referred to as `input_file_0.png` in the request).

> [!NOTE]
> Since I do not have a tool to resize images locally, I will copy the high-resolution source image to all density-specific mipmap folders. Modern Android devices will handle the scaling, though for a production release, these should ideally be pre-resized to their respective dimensions (48dp to 192dp).

## Proposed Changes

### Resources

#### [NEW] ic_launcher.png (in all mipmap folders)
- Copy `RetroVaultIcon.png` to:
    - `app/src/main/res/mipmap-mdpi/ic_launcher.png`
    - `app/src/main/res/mipmap-hdpi/ic_launcher.png`
    - `app/src/main/res/mipmap-xhdpi/ic_launcher.png`
    - `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`
    - `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`

#### [NEW] ic_launcher_round.png (in all mipmap folders)
- Copy `RetroVaultIcon.png` to:
    - `app/src/main/res/mipmap-mdpi/ic_launcher_round.png`
    - `app/src/main/res/mipmap-hdpi/ic_launcher_round.png`
    - `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png`
    - `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png`
    - `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png`

#### [NEW] [ic_launcher_foreground_new.png](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/res/drawable/ic_launcher_foreground_new.png)
- Save the source image to `drawable` to be used by the adaptive icon.

### Adaptive Icons

#### [MODIFY] [ic_launcher.xml](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)
#### [MODIFY] [ic_launcher_round.xml](file:///C:/Users/DoonPedroo/AndroidStudioProjects/RetroVault/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml)
- Update the `foreground` tag to use `@drawable/ic_launcher_foreground_new`.
- Keep or adjust the `background` as needed (currently uses a purple grid).

## Verification Plan

### Manual Verification
- Verify the icon appears correctly on the device home screen.
- Ensure the adaptive icon looks good in the launcher (supports different mask shapes).
- Check `AndroidManifest.xml` to ensure it still points to `@mipmap/ic_launcher`.
