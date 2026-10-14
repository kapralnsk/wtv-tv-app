# Cross-Platform Plan: Web-First (Android TV + Tizen)

## Context

The existing app is Android-native (Kotlin + Jetpack Compose TV + ExoPlayer). The goal is a cross-platform rewrite where a single TypeScript web app runs on both Google TV (via a full-screen WebView wrapper) and Samsung Tizen (packaged as a `.wgt` web app).

This follows the pattern used by SmartTwitchTV (the primary UX reference), YouTube TV, and most other smart TV streaming apps: a web app that can be packaged or wrapped per-platform with minimal platform-specific glue.

**What gets replaced**: the entire `app/` Kotlin/Compose/ExoPlayer implementation is thrown away. `app/` becomes a ~50-line Kotlin WebView wrapper. All logic moves to `web/`.

**Shared code**: ~95%. The only platform-specific code is the video player adapter (AVPlay for Tizen, hls.js for Android WebView) — isolated to a single `player/` module behind a common interface.

**Framework choice**: vanilla TypeScript, no React/Vue/Svelte. Matches SmartTwitchTV's approach, keeps the bundle small (<300 KB), and avoids framework overhead on Tizen 5.5's constrained RAM (~500 MB available for the app). Vite for bundling.

**Target device**: Samsung UE43T5370AUXRU — 43" FHD (1080p), 2020 T-series, Tizen 5.5, ~1.5 GB RAM, CIS/Russia region.

---

## Target Project Structure

```
wtv-google-tv/
│
├── web/                            ← the entire shared app
│   ├── src/
│   │   ├── main.ts                 entry point: init state, wire up nav + UI
│   │   ├── api.ts                  fetch()-based w.tv REST client (4 microservices)
│   │   ├── chat.ts                 WebSocket + IVS frame parser
│   │   ├── state.ts                app state machine + event emitter
│   │   ├── player/
│   │   │   ├── index.ts            VideoPlayer interface + factory (platform detection)
│   │   │   ├── avplay.ts           Tizen AVPlay adapter
│   │   │   └── hlsjs.ts            hls.js adapter (Android WebView + non-Tizen fallback)
│   │   ├── ui/
│   │   │   ├── layout.ts           panel show/hide, 70/30 split
│   │   │   ├── chat.ts             chat DOM rendering
│   │   │   └── overlays.ts         StreamInfoBar, QualityPicker
│   │   └── nav.ts                  D-pad key handler + focus tree
│   ├── index.html
│   ├── app.css
│   ├── package.json                TypeScript, Vite, hls.js
│   └── tsconfig.json
│
├── app/                            ← Android TV: WebView wrapper only
│   └── src/main/java/tv/wtv/app/
│       └── MainActivity.kt         full-screen WebView, loads web/dist/
│       (everything else deleted)
│
└── tizen-app/
    ├── config.xml                  Tizen 5.5 manifest
    └── build.sh                    copies web/dist/ → here, runs tizen package
```

---

## Module Plan

### `web/src/api.ts`
Direct port of the four Retrofit interfaces + `WtvApiClient.kt` using `fetch()`:
- Same four base URLs (`profiles-service.w.tv`, `streams-search-service.w.tv`, etc.)
- `defaultHeaders`: `User-Agent: Mozilla/5.0 (Linux; Android 12; Chromecast with Google TV) ...`, `Origin: https://w.tv`
- Four async functions: `getProfile(slug)`, `getChannel(id)`, `joinStream(id)`, `joinChat(id)` → `token`, `getChatBacklog(id)` → `ChatMessage[]`
- Serialization: `response.json()` + TypeScript interfaces matching `StreamModels.kt`

### `web/src/chat.ts`
- `parseIvsFrame(text)`: direct port of `ChatWebSocket.kt:17–29` — same JSON logic, same field paths
- `ChatWebSocket` class: `connect()` uses `new WebSocket(IVS_CHAT_URL, [token])` — the array form sends `Sec-WebSocket-Protocol: <token>`, which is how IVS auth works; native browser API, works in both Android WebView and Tizen
- 30 s keepalive via `setInterval` ping
- `onMessage` callback, `close()`, non-fatal error handling (chat failure silent)

### `web/src/state.ts`
Port of `PlayerViewModel.kt` state machine as a plain TS class (no lifecycle framework needed):
- States: `Loading | Playing | Error` matching `PlayerUiState`
- Methods: `load(slug)`, `setAvailableQualities(list)`, `selectQuality(q)`, `toggleChat()`, `showMetadata()` (auto-hides after 4 s via `setTimeout`), `close()`
- Events emitted via a simple callback; `ui/` modules subscribe
- Max 200 chat messages, prepend newest (same as Android)

### `web/src/player/index.ts` — VideoPlayer interface + factory
```typescript
interface VideoPlayer {
  open(url: string): Promise<void>;
  play(): void;
  getQualities(): string[];
  selectQuality(index: number): void;
  setListener(cb: { onReady(): void; onError(e: unknown): void }): void;
  destroy(): void;
}

// Runtime platform detection
export function createPlayer(container: HTMLElement): VideoPlayer {
  if (typeof (window as any).webapis?.avplay !== 'undefined')
    return new AvPlayPlayer(container);
  return new HlsJsPlayer(container);
}
```

### `web/src/player/avplay.ts` — Tizen AVPlay
- `webapis.avplay.open(url)` → `play()`
- `webapis.avplay.getTotalTrackInfo()` → map to quality label list
- `webapis.avplay.setCurrentTrack('VIDEO', index)` for quality selection
- Callbacks via `webapis.avplay.setListener({ onEvent, onError, onBufferingComplete })`

### `web/src/player/hlsjs.ts` — hls.js (Android WebView)
- `new Hls()` → `hls.loadSource(url)` → `hls.attachMedia(videoEl)`
- `Hls.Events.MANIFEST_PARSED`: extract level list → quality labels `"${h}p"`
- `hls.currentLevel = index` for quality selection
- Handles Android WebView (Chromium-based, no native AVPlay)

### `web/src/nav.ts`
- D-pad key codes: `UP=38, DOWN=40, LEFT=37, RIGHT=39, ENTER=13, BACK=10009`
- On Tizen: register keys via `tizen.tvinputdevice.registerKey(...)` before use
- `FocusManager`: registry of named focus nodes with `{up, down, left, right}` neighbors; `moveFocus(dir)` walks the graph; active node gets CSS class `focused`
- Focus graph mirrors Android layout: player area → quality picker items → chat toggle

### `web/src/ui/`
Mirrors `PlayerScreen.kt`'s composable tree as DOM functions:
- `layout.ts`: video container flex `7`, chat panel flex `3`; `hidden` attr to toggle
- `chat.ts`: `renderMessage(msg)` prepends `<li>` with hashed nickname color (same 12-color palette from Android); caps list at 200 `<li>` nodes
- `overlays.ts`: `StreamInfoBar` (channel name, title, uptime via `setInterval` 1 s); `QualityPicker` (list of `<div>` items, `.focused` class on selected)

Nickname colors (from `PlayerScreen.kt:366–370`):
```
#FF4500, #2E8B57, #1E90FF, #DAA520, #FF69B4, #9ACD32,
#D2691E, #5F9EA0, #B22222, #00FA9A, #9B59B6, #FF7F50
```
Selected by `abs(nickname.hashCode()) % 12` — port the same hash.

### `web/index.html`
```html
<div id="app">
  <div id="video-container">
    <video id="video" playsinline></video>
    <object id="avplay" type="application/avplayer"></object>
  </div>
  <aside id="chat-panel" hidden><ul id="chat-messages"></ul></aside>
  <div id="stream-info-bar" hidden></div>
  <div id="quality-picker" hidden></div>
</div>
<script src="tizen.js" onerror="void 0"></script>
<script src="webapis-all.js" onerror="void 0"></script>
<script type="module" src="/src/main.ts"></script>
```

### `web/app.css`
- `#app`: `display:flex; height:100vh; background:#000`
- `#video-container`: `flex:7`; `flex:10` when `#chat-panel[hidden]`
- `#chat-panel`: `flex:3; overflow:hidden`
- `.focused`: `outline: 2px solid #fff`
- Overlay backgrounds: `rgba(0,0,0,0.65)` (65% opacity, same as Android)
- Chat text: `font-size:0.85rem; color:rgba(255,255,255,0.9)`

---

## Android TV wrapper (`app/`)

Delete everything except `AndroidManifest.xml` (keep TV leanback category) and rewrite `MainActivity.kt`:

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent) =
        if (webView.dispatchKeyEvent(event)) true
        else super.onKeyDown(keyCode, event)
}
```

`app/build.gradle.kts`: add Gradle task that runs `npm run build` in `web/` and copies `web/dist/` into `app/src/main/assets/` before `processAssets`. Remove: `media3-*`, `retrofit-*`, `okhttp-*`, `coil-*`, `compose-*`, `tv-*`, `lifecycle-*`, `navigation-*`. Keep: `androidx.appcompat`, `androidx.core-ktx`.

---

## Tizen packaging (`tizen-app/`)

`config.xml`: app id `tv.wtv.app`, `tizen:profile name="tv"`, `tizen:required-version="5.5"`, privilege `http://tizen.org/privilege/internet`

`build.sh`:
```bash
cd web && npm run build
cp -r dist/ ../tizen-app/
tizen package -t wgt -o ../tizen-app/ -- ../tizen-app/
```

---

## New Dependencies

`web/package.json`:
```json
{
  "devDependencies": { "typescript": "^5.x", "vite": "^5.x" },
  "dependencies": { "hls.js": "^1.x" }
}
```

---

## Migration Order

1. Scaffold `web/` with Vite + TypeScript; get `index.html` loading in browser
2. Implement `api.ts`; test against live w.tv endpoints in browser DevTools
3. Implement `chat.ts`; verify IVS WebSocket connects and `parseIvsFrame` parses messages
4. Implement `player/hlsjs.ts`; verify HLS stream plays in browser
5. Implement `state.ts` + `ui/` + `nav.ts`; full app working in desktop browser
6. Implement `player/avplay.ts`; test on Tizen Studio 5.5 emulator
7. Simplify `app/` to WebView wrapper; verify full flow on Android TV
8. Package and install `tizen-app/` on device

---

## Verification

- **Browser (dev)**: `vite dev` — full app in desktop Chrome; simulate D-pad via keydown events in DevTools console
- **Android TV**: `./gradlew :app:installDebug` → ADB; verify stream, chat, quality picker, D-pad nav
- **Tizen emulator**: Tizen Studio 5.5 TV emulator → `build.sh` → install `.wgt`
- **Tizen device**: `tizen install -n wtv.wgt -t <serial>` via USB
- **Manual checks (both platforms)**:
  - Stream loads and plays `dunduk` channel HLS
  - D-pad Enter shows StreamInfoBar, auto-hides after 4 s
  - Chat panel toggles, Cyrillic renders, 200-message cap enforced
  - Quality picker navigable, selection updates player
  - Chat WebSocket failure does not interrupt video
