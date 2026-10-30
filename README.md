# wtv-google-tv

A TV client for [w.tv](https://w.tv), a live streaming platform with chat.

**Targets:** Chromecast with Google TV (Android 12+) and Samsung Tizen 5.5+ (2020 models). D-pad
only, 3 m viewing distance.

## Structure

One shared TypeScript web application holds all UI and logic. Each platform is a thin shell around
it: an Android APK (a WebView over a Media3 video surface) and a Tizen web package (`.wgt`, using
AVPlay).

The project is spec-driven. Start with:

- [`.specify/memory/constitution.md`](.specify/memory/constitution.md): the principles that
  govern every change.
- [`AGENTS.md`](AGENTS.md): layout, workflow, standing limits and budgets.

## Development

- **Environment:** agent sessions run in the dev container described in
  [`.devcontainer/README.md`](.devcontainer/README.md), which covers setup, launch and rebuilds.
- **Workflow:** every feature goes spec → plan → tasks → implementation through the spec-kit
  commands listed in [`AGENTS.md`](AGENTS.md#workflow).
- **Prerequisites** outside the dev container: Node 24, JDK 26, and an Android SDK with
  `platforms;android-36` and `build-tools;36.1.0`. On arm64 Linux, see the dev container README for
  the aapt2 override that Android builds need.

Web core, from `web/`:

```sh
npm ci                 # install the locked dependencies
npm run build          # bundle to web/dist/ for Chromium 69
npm test               # unit tests, plus the bundle size and Chromium 69 syntax checks
npm run typecheck      # src/ against ES2018 + DOM libs; tests and configs separately
npm run lint           # ESLint; flags Web APIs missing from Chrome 69 in src/
npm run format:check   # Prettier; npm run format rewrites
```

Android shell, from the repository root:

```sh
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # JVM unit tests
./gradlew spotlessCheck             # ktlint over Kotlin and Gradle scripts; spotlessApply rewrites
```

Both shells package `web/dist`, so run `npm run build` in `web/` before the Android build. Gradle
fails with that instruction when `web/dist` is missing; it never runs npm.

Tizen package, from the repository root:

```sh
tizen-app/package.sh   # builds web/, then zips tizen-app/ and web/dist into tizen-app/build/wtv.wgt
```

The package is unsigned and made without the Tizen CLI, which has no arm64 build. The Build
workflow (`.github/workflows/build.yml`) checks it with the Tizen CLI on an x86_64 runner, and
uploads it with the debug APK as workflow artifacts.

### Installing on a device

The dev container has no route to either device, so run these on your own machine.

Chromecast with Google TV: enable developer options and USB debugging, then network debugging, on
the device.

```sh
adb connect <device-ip>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Samsung Tizen TV (Tizen Studio with its CLI): switch the TV to developer mode (Apps, enter `12345`,
turn Developer mode on and set the host PC's IP), then sign with your own author and distributor
certificates and install.

```sh
sdb connect <tv-ip>
tizen security-profiles list     # or create one with Certificate Manager
tizen package -t wgt -s <profile> -- tizen-app/build/wtv.wgt
tizen install -n wtv.wgt -t <tv-name> -- tizen-app/build
```

`tizen package` signs the `.wgt` in place. `sdb devices` shows `<tv-name>`. Neither install command
has been run on a device yet, and on-device loading of the placeholder is unverified.

## Platform research

w.tv publishes no API documentation, so platform behavior comes only from observing its web client
(constitution Principle VIII).

- [`docs/observing-wtv.md`](docs/observing-wtv.md): how observations are made and recorded.
- [`docs/wtv-api.yaml`](docs/wtv-api.yaml) and `docs/*.contract.yml`: captured API shapes and
  contracts.

## UI reference

Primary: [SmartTwitchTV](https://github.com/fgl27/smarttwitchtv), a Smart TV Twitch client. Its
patterns for focus management, remote navigation, overlay layout, and the quality picker take
priority for TV-specific UX decisions.
