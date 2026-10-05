# wtv-google-tv

TV client for **w.tv** (Twitch-like live streaming). Two locked targets: **Chromecast with Google
TV** (Android 12+) and **Samsung Tizen**. D-pad only.

Governed by `.specify/memory/constitution.md`. On conflict, the constitution wins and this file
is corrected. This file carries operational detail and standing numbers; it does not create or
relax principles.

## Architecture

Shared TypeScript web core holds all UI, navigation, and domain logic. Platform packages are
shells with no business logic.

- `web/` — the application. All feature work happens here.
- `app/` — Android shell: full-screen WebView host.
- `tizen-app/` — Tizen shell: `config.xml` + packaging into `.wgt`.

Platform differences are reached only through a named adapter with one implementation per
platform. The player is the first such adapter: Tizen AVPlay on Tizen, browser HLS elsewhere.

Android is the reference target. Tizen may ship reduced functionality, but every reduction must
be declared in the feature's spec.

**The Kotlin/Compose/Media3 implementation under `app/src/main/java/` is retired.** It is
reference material only — do not extend it, and do not port from it. The `web-app-port` branch is
a mechanical port of it and is likewise not a starting point. Same for the empty `web/dist/`
leftover.

## Scope

`docs/phases.md` sequences the product. Only the active phase is in scope; do not design ahead of
it. Phase 1 is a hardcoded-channel livestream with quality picker, metadata bar, live chat, and
audio-only mode.

## Workflow

Spec-first, always: `/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`.
Use `/speckit-clarify` before planning when the spec is underspecified, `/speckit-analyze` after
tasks. No behavioral change without a spec.

## UI / UX reference

Primary: [SmartTwitchTV](https://github.com/fgl27/smarttwitchtv) — follow its patterns for D-pad
nav, overlay/sidebar layout, quality picker, TV-first UX.
Secondary: Official Twitch TV app. SmartTwitchTV wins on conflict.

## Legibility thresholds

Design reference is 1920×1080 CSS pixels at ~3 m viewing distance.

| Property | Requirement |
|---|---|
| Body text | ≥ 24px |
| Secondary / chat text | ≥ 20px, never below 18px |
| Text contrast | ≥ 4.5:1; ≥ 7:1 for text drawn over video |
| Focus indicator | ≥ 4px, ≥ 3:1 against adjacent surfaces, never color-only |
| Safe area | 5% inset (96px horizontal, 54px vertical) — nothing interactive outside it |
| Focusable hit target | ≥ 48px on its shorter axis |

## Standing limits

| Limit | Value |
|---|---|
| Chat message buffer | 200 messages, oldest evicted |
| Chat work per animation frame | ≤ 8ms |
| Metadata bar auto-hide | 4s after show |

## Performance budgets

Per-feature budgets belong in specs. These are the standing defaults a spec inherits unless it
declares otherwise. Each names its verifier: **H** = automated harness, **D** = real device,
supervisor-verified.

| Metric | Android ceiling | Verifier |
|---|---|---|
| D-pad keypress → visible focus change | 100ms | D |
| Launch → first video frame | 5s | D |
| Quality switch → first frame | 2s | D |
| Chat message arrival → on screen | 500ms | D |
| Sustained chat rate with no dropped frames | 20 msg/s | D |
| Initial JS bundle, gzipped | 500KB | H |
| Steady-state heap after 4h playback | no monotonic growth | D |

Tizen ceilings are set per spec and are expected to be looser; a looser Tizen figure is a
declared reduction, not a silent divergence.

> These values are initial and unmeasured on real hardware. Revise them against the first
> on-device measurements rather than treating them as validated.

## Build toolchain

The Android shell's `jvmToolchain` and `compileOptions` are intentionally pinned to the highest
JVM target Kotlin supports (currently 25). When upgrading Kotlin, bump to the new maximum. Do not
downgrade to 17 or 21.

## Code style

- No unnecessary comments — only when the _why_ is non-obvious
- No speculative abstractions — implement what's needed now
- No trailing end-of-response summaries
- Add a test file for every state container, protocol parser, and data-transformation module
- Tests must run without a TV, emulator, or network
- Never report a failing or skipped test as passing; mark device-verified budgets as unverified
  rather than inferring they are met
