# wtv

A live streaming client for [w.tv](https://w.tv) that runs on **Google TV** (Android WebView) and **Samsung Tizen TV** (native `.wgt`). The entire app is a single TypeScript/Vite project in `web/`; each platform provides a thin wrapper.

## Architecture

```
web/        ← shared TypeScript app (95% of all code)
app/        ← Android/Google TV: full-screen WebView wrapper (~50 lines Kotlin)
tizen-app/  ← Samsung Tizen: config.xml + build.sh that packages web/dist/
```

Video playback is platform-detected at runtime:
- **Tizen** — `webapis.avplay` (native AVPlay)
- **Android WebView / desktop** — hls.js (MSE-based)

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 18 + |
| npm | 9 + |
| Android Studio | Hedgehog + (for Google TV) |
| Tizen Studio CLI | 2.5 + (for Samsung TV) |

---

## Web app (shared)

```bash
cd web
npm install
npm run dev       # dev server at http://localhost:5173
npm test          # run unit tests (Vitest)
npm run build     # production build → web/dist/
```

---

## Google TV / Android

### Build & install (debug)

```bash
# One command — builds the web app, copies dist/ into Android assets, then installs
./gradlew :app:installDebug
```

The Gradle task `buildWebApp` runs `npm run build` inside `web/` automatically before compiling the APK. Node.js and npm must be on `PATH`.

### Deploy to device

Connect the Chromecast with Google TV via ADB over Wi-Fi:

```bash
adb connect <device-ip>:5555
./gradlew :app:installDebug
adb shell am start -n tv.wtv.app/.MainActivity
```

Or use **Run** in Android Studio after pairing the device.

### Release build

```bash
./gradlew :app:assembleRelease
# Sign with your keystore using apksigner or Android Studio
```

---

## Samsung Tizen TV

### Tizen Studio setup

The `tizen` CLI is at `~/tizen-studio/tools/ide/bin/tizen`. Add it (and `sdb`) to PATH permanently:

```bash
# add to ~/.zshrc
export PATH="$PATH:$HOME/tizen-studio/tools/ide/bin:$HOME/tizen-studio/tools"
```

> **Note:** The Tizen TV emulator is not available on Apple Silicon Macs — Samsung ships
> x86-only emulator images. Use the real TV for Tizen testing.

### One-time: create a developer signing certificate

Required once before you can package a WGT:

```bash
# Create a self-signed author certificate
tizen certificate -a wtv -p <password> -f ~/tizen-studio-data/keystore/author/wtv

# Register it as a security profile
tizen security-profiles add -n wtv-dev \
  -a ~/tizen-studio-data/keystore/author/wtv.p12 \
  -p <password>
```

### Build

```bash
bash tizen-app/build.sh
# Produces tizen-app/tv.wtv.app.wgt
```

The build script calls `tizen package -t wgt -s wtv-dev` — make sure the `wtv-dev` profile
exists (see above) and `tizen` is on PATH before running it.

### Install on device

**On the TV (one-time setup):**

1. Open `Smart Hub` → navigate to `Apps`.
2. While on the Apps screen, type `1 2 3 4 5` on the remote number pad (no text field — just press the digits).
3. A Developer Mode dialog appears. Toggle **ON**, enter your PC's IP address, confirm.
4. The TV restarts. After reboot, a "Developer Mode" badge is visible in the Apps screen.

**Deploy via Tizen Studio IDE (Windows):**

> The `tizen install` CLI has a protocol incompatibility with Tizen 5.5 TVs. Use the IDE instead — it ships its own SDB that is compatible.

1. Run `bash tizen-app/build.sh` on your Mac to build the web app and populate `tizen-app/`.
2. Push to git and pull on Windows, or copy the `tizen-app/` folder to the Windows machine.
3. Open **Tizen Studio** on Windows.
4. **File → Import → Tizen → Tizen Project** → select the `tizen-app/` directory → Finish.
5. Make sure the TV is on, in Developer Mode, and on the same network as the PC.
6. Right-click the imported project → **Run As → 1 Tizen Web Application**.
7. Tizen Studio detects the TV automatically via its built-in SDB and installs the app.

### Target device

Samsung UE43T5370AUXRU — 43" FHD, Tizen 5.5, 2020 T-series.

---

## D-pad controls

| Button | Action |
|--------|--------|
| **OK / Enter** | Show stream info bar; when info visible → open quality picker |
| **Up / Down** | Navigate quality picker |
| **Enter** (picker open) | Select focused quality |
| **Back** (picker open) | Close quality picker |

---

## API research

Reverse-engineered endpoint contracts are in `docs/`. Capture scripts used during research are in `capture-specs/`.
