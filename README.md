# Xion Mobile

Minecraft: Java Edition on Android, tuned for ordinary phones. Part of [Xion](https://anisbabu.github.io/xion-launcher/), by Taseen Rahman.

> **Beta.** Best on 64-bit phones with Android 8+ and 4 GB of RAM or more.

## What Xion Mobile adds

- **Nightfall UI** — the Xion mountain backdrop with a slow drift, glass panels, a big Play button and quick tiles.
- **Lite mode (on by default)** — the first launch of each game folder gets render distance 6, fast graphics, no clouds, minimal particles, no shadows/smooth lighting and a 60 FPS cap. Toggle it from the home screen.
- **Phone performance pack** — Fabric/Quilt profiles automatically get FerriteCore, ModernFix, Lithium, Entity Culling, Krypton, Dynamic FPS and BadOptimizations from Modrinth. Sodium is deliberately excluded (the mobile renderers don't support it).
- Tuned for **1.20.1 and 1.21.x**; other versions still launch.

Xion-specific code lives in `app_pojavlauncher/src/main/java/net/xion/mobile/` and the `xion_*` resources.

## Download

Get the APK from [Releases](https://github.com/anisbabu/xion-mobile/releases) or the [Xion website](https://anisbabu.github.io/xion-launcher/#android).

## Building

```
git clone --recursive https://github.com/anisbabu/xion-mobile.git
cd xion-mobile
./gradlew :app_pojavlauncher:assembleDebug
```

Needs the Android SDK (platform 37, build-tools 37) and NDK 27.3.13750724. See `README.amethyst.md` for the upstream build details.

## License and credits

Xion Mobile is a modified version of **[Amethyst](https://github.com/AngelAuraMC/Amethyst-Android)** (based on upstream commit `330c6ea`), which itself builds on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) and [Boardwalk](https://github.com/zhuowei/Boardwalk). It is licensed under the **GNU LGPL v3** — see [LICENSE](LICENSE). All upstream credits and third-party licenses (OpenJDK, LWJGL3, GL4ES, MobileGlues, ANGLE and others) are listed in `README.amethyst.md` and in the app's licence screen.

Not affiliated with Mojang Studios or Microsoft. Minecraft is a trademark of Mojang Synergies AB.
