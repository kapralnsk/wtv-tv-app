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
- [`AGENTS.md`](AGENTS.md): layout, workflow, standing limits and budgets, and the build
  commands once they are set up.

## UI reference

Primary: [SmartTwitchTV](https://github.com/fgl27/smarttwitchtv), a Smart TV Twitch client. Its
patterns for focus management, remote navigation, overlay layout, and the quality picker take
priority for TV-specific UX decisions.
