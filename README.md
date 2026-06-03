# wtv-google-tv

A Google TV client for [w.tv](https://w.tv) — a game streaming platform with live chat.

**Target device:** Chromecast with Google TV (4th gen) · D-pad only · 3 m viewing distance

## Features

- Watch live streams with full video playback (HLS via Media3)
- Switch stream quality on the fly
- Live chat panel alongside the video (IVS WebSocket)

## Tech stack

- Kotlin + Jetpack Compose for TV (`androidx.tv`)
- Media3 / ExoPlayer for HLS playback
- Retrofit + kotlinx.serialization for REST
- Native `OkHttp` WebSocket for IVS chat
- MVVM with `ViewModel` + `StateFlow`

## Development

Open in Android Studio (Hedgehog or later). No API keys required — all w.tv endpoints are unauthenticated.

```
./gradlew assembleDebug
```

Deploy to a Chromecast 4th gen or an Android TV emulator (API 31+) via `Run` in Android Studio.

## API research

The `capture-specs/` directory contains Node.js / Playwright scripts used to reverse-engineer the network API. Captured findings live in `docs/`.

## UI reference

Primary: [SmartTwitchTV](https://github.com/fgl27/smarttwitchtv) — a Smart TV Twitch client. Its patterns for focus management, remote control navigation, overlay layout, and quality picker take priority for all TV-specific UX decisions.
