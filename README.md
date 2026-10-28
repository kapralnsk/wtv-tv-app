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

## Development

- **Environment:** agent sessions run in the dev container described in
  [`.devcontainer/README.md`](.devcontainer/README.md), which covers setup, launch and rebuilds.
- **Workflow:** every feature goes spec → plan → tasks → implementation through the spec-kit
  commands listed in [`AGENTS.md`](AGENTS.md#workflow).
- **Build, test and lint:** not set up yet; issue #10 adds the web core and Android toolchains and
  their commands.

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
