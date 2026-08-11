# Channels

A minimalist, **Light Phone III–style** YouTube **audio** listener — like a podcast app, but for
any YouTube channel. Black-on-white monochrome UI, long-form only (no Shorts), ad-free, with
background playback and offline downloads.

> Personal project. It plays the audio track of public YouTube videos via
> [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) — no account, no API key.
> This relies on YouTube's public site and sits in a grey area of YouTube's Terms of Service, so
> it is intended for personal use, not distribution.

## Features

- **Search** channels and videos.
- **Star channels** — their newest long-form uploads auto-collect on the **Home** feed
  (refreshed periodically in the background).
- **No Shorts** — a single duration/URL rule filters them everywhere.
- **Background audio** — plays with the screen off and lock-screen / notification controls,
  variable speed, and ±30s skip.
- **Original-language audio** — when a video has dubbed tracks, it picks the original.
- **Offline downloads** — save a show's audio (fast chunked download) and play it with no network.
- **Ad-free** — streams the bare audio, so there's no ad layer.

## Tech

Kotlin · Jetpack Compose · MVVM · Coroutines/Flow · Media3 (ExoPlayer + MediaSession) ·
Room · WorkManager · NewPipeExtractor · OkHttp. `minSdk`/`compileSdk` 34, sized for the
Light Phone III display (1080×1240).

Architecture is layered so the non-UI parts (data/domain/playback) stay portable:

```
com.channels
├─ data        # NewPipe repo, Room db, downloads, feed
├─ domain      # models + the Shorts rule
├─ playback    # Media3 service + player controller
├─ ui          # Compose screens (home, search, channel, player, library) + theme
└─ di          # AppContainer (manual DI)
```

## Build & run

Requires the Android SDK and a **JDK 17–21** (the one bundled with Android Studio works).

- **Android Studio:** open the project and Run.
- **Command line:** `./gradlew assembleDebug` (ensure `JAVA_HOME` points at a JDK 17–21, or set
  `org.gradle.java.home` in your user-level `~/.gradle/gradle.properties`).

Install on a device/emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tests

```bash
./gradlew testDebugUnitTest
```

A network-gated smoke test exercises the live YouTube data path (search → Shorts filtering →
audio resolution); enable it with `-Dchannels.live=1`.

## License

Personal project — no license granted for redistribution.
