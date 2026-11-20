# CLEAN weight plate launcher icon

Approved raster reference: clean-weight-plate-approved.png (SHA256.txt verifies this source).
Installed design: crisp vector recreation of the ivory C plate, inset rim, and outlined 45 LB lettering on charcoal. launcher-preview.png shows the adaptive foreground placement on its background.

## Android resources

- drawable/ic_launcher_foreground.xml: foreground vector with outlined lettering; no runtime font dependency.
- drawable/ic_launcher_monochrome.xml: themed icon silhouette with lettering cut out.
- values/launcher_colors.xml: solid charcoal background.
- mipmap-anydpi-v26/ic_launcher.xml: adaptive launcher icon.
- mipmap-mdpi through mipmap-xxxhdpi/ic_launcher.png: legacy launcher icons.
- AndroidManifest.xml points icon and roundIcon to mipmap/ic_launcher.

The original generated PNG is retained as a visual reference and is not packaged into the app.

## Regeneration

From the project root in Windows PowerShell, run:

    & .\assets\approved\clean-weight-plate\tools\generate-launcher.ps1

The generator uses Windows System.Drawing and Arial Bold to produce outlined paths and legacy PNGs. Android requires no font or additional library. The generated XML and PNG files are checked-in source assets, so building the app does not run this script.

## Manual verification

Run the app in Android Studio. Open the emulator home screen, swipe up to open its app list, and inspect Workout Logger. Check the 45 LB label and the C-shaped opening. Check themed icons on a launcher that supports them. If a previous icon remains visible, restart the emulator before retesting; do not uninstall to refresh it, because uninstalling deletes local workouts.
