# wtv-google-tv

Android TV client for **w.tv** (Twitch-like live streaming). Target: Chromecast with Google TV 4th gen, Android 12+, D-pad only.

## MVP scope

In scope: watch specific hardcoded channel stream (found at w.tv/dunduk) → live chat alongside video.  
Out of scope (do not design for): login, search & channel list, VODs, notifications, subscriptions.

## UI / UX reference

Primary: [SmartTwitchTV](https://github.com/fgl27/smarttwitchtv) — follow its patterns for D-pad nav, overlay/sidebar layout, quality picker, TV-first UX.  
Secondary: Official Twitch TV app. SmartTwitchTV wins on conflict.

## Platform constraints

- D-pad only — every interactive element must be focusable
- ~3 m viewing distance — large text, high contrast, low visual noise
- Chat can have high message throughput — avoid heavy allocations during rendering

## Build toolchain

`jvmToolchain` and `compileOptions` are intentionally set to the highest JVM target Kotlin supports (currently 25). When upgrading Kotlin, bump this to the new maximum. Do not downgrade to 17 or 21.

## Code style

- No unnecessary comments — only when the _why_ is non-obvious
- No speculative abstractions — implement what's needed now
- No trailing end-of-response summaries
- Add a test file for every new ViewModel or non-trivial business logic
