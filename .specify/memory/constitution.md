# w.tv TV Client Constitution

## Core Principles

### I. Spec-Driven Development (NON-NEGOTIABLE)

Every behavioral change MUST originate from a written spec before code exists. The order is
spec → plan → tasks → implementation; no step may be skipped and no implementation may
introduce behavior absent from its spec.

- Code that has no spec lineage MUST be removed or retroactively specified before merge.
- When implementation reveals the spec is wrong, the spec is amended first, then the code.
- Ported, inherited, or previously written code carries no authority. It MAY be consulted as
  reference but MUST be re-specified before it enters the codebase.

Rationale: implementation is delegated to an AI agent working across sessions with no durable
memory. The spec is the only persistent statement of intent, so it is the artifact of record —
not the code, and not a prior branch.

### II. Shared Web Core, Thin Platform Shells

All UI, navigation, and domain logic MUST live in a single shared TypeScript web application.
Platform packages are shells only.

- A platform shell MUST contain nothing but packaging, host permissions, lifecycle glue, and
  launch of the shared app. Business logic in a shell is a constitutional violation.
- Platform-specific behavior in the shared core MUST be reached only through a named adapter
  with one interface and one implementation per platform (for example, the video player:
  Tizen AVPlay vs. HLS-in-browser). Scattered platform conditionals are forbidden.
- Adding a platform MUST mean adding a shell and adapter implementations, never forking the core.

Rationale: two hand-written native clients double every feature's cost and guarantee drift.
A shared core keeps the single supervising developer reviewing one implementation of each
behavior.

### III. Android Is Reference, Tizen May Degrade Explicitly

Chromecast with Google TV (Android) is the reference target and defines correct behavior.
Samsung Tizen MUST run the app but MAY ship reduced functionality.

- Any Tizen reduction MUST be declared in the feature's spec, with the platform limitation that
  forces it. Undeclared divergence is a defect.
- A degraded path MUST fail legibly on-screen, never silently and never with a dead-end screen.
- Tizen MUST NOT constrain Android's feature set. Capability absent on Tizen is a reason to
  degrade there, never a reason to withhold it on Android.

Rationale: the Tizen web runtime lags its Android counterpart by years. Pretending otherwise
either blocks Android features or hides broken Tizen behavior; declaring the gap does neither.

### IV. D-Pad-First Ten-Foot UX

The app is operated by a directional pad from roughly three metres away. SmartTwitchTV
(github.com/fgl27/smarttwitchtv) is the primary design reference; the official Twitch TV app is
secondary and loses on conflict.

- Every interactive element MUST be reachable and actuatable by D-pad alone. Pointer, touch, and
  keyboard-text affordances MUST NOT be the only route to any function.
- Focus MUST be visible at all times, MUST never be lost or trapped, and BACK MUST always lead
  somewhere predictable.
- Text, contrast, and hit targets MUST be legible at viewing distance; dense or low-contrast
  layouts are rejected regardless of how they look on a desktop monitor. Concrete legibility
  thresholds live in `AGENTS.md`, not here.
- Any spec introducing or restructuring UI MUST declare its focusable elements, the D-pad
  traversal between them, the initial focus, and where BACK leads. A plan whose new UI leaves
  these undeclared fails the constitution check.
- When SmartTwitchTV has solved an interaction (overlay and sidebar layout, quality picker,
  chat placement), follow it rather than inventing an alternative.

Rationale: TV input is the hardest constraint to retrofit, and the chosen reference has already
validated these patterns on the same hardware class.

### V. Performance Under Live Load

The app MUST stay responsive on low-powered TV hardware during sustained live playback with
high-throughput chat.

- Chat rendering MUST operate under a bounded message buffer and MUST NOT grow memory without
  limit during a long session.
- Per-message work in the chat hot path MUST avoid allocation-heavy and layout-thrashing
  patterns; steady-state cost per message is a review criterion, not an afterthought.
- Video playback MUST take priority: no feature may introduce stutter, dropped frames, or input
  lag during playback.
- Any spec touching startup, playback, stream or quality switching, D-pad input handling, or the
  chat hot path MUST declare performance budgets. A budget is a named metric, a ceiling, and the
  condition and target device it is measured under — an unquantified intention is not a budget,
  and silence is not an exemption. A plan missing a required budget fails the constitution check.
- Budgets MUST name their target device, and MAY differ between Android and Tizen. A Tizen budget
  looser than its Android counterpart is a declared reduction under Principle III.
- Each budget MUST state who verifies it. Budgets measurable in an automated harness MUST be
  tested under Principle VI; budgets requiring real hardware MUST be marked for supervisor
  verification and MUST NOT be reported as met on inference.
- Concrete budget values live in specs, and standing limits in `AGENTS.md`. This constitution
  fixes the obligation to declare and verify them, never the numbers, which change with hardware
  and product phase.

Rationale: a Chromecast dongle and a Samsung TV SoC have a fraction of a phone's headroom, and
chat is an unbounded inbound stream.

### VI. Test-Backed Logic

Non-trivial logic MUST ship with automated tests that run without a TV, an emulator, or a
network.

- Every state container, protocol parser, and data-transformation module MUST have a test file.
- Protocol and API handling MUST be tested against captured real payloads, not hand-waved shapes.
- Presentational markup and styling need no unit tests; logic hiding inside them MUST be
  extracted until it is testable.
- A failing or skipped test MUST NOT be reported as a passing change.

Rationale: the agent cannot see the television. Tests are its only unsupervised feedback loop,
and the supervisor's only cheap verification.

### VII. Agent-Executed, Human-Supervised

Claude performs implementation; a single human developer holds all authority. The codebase MUST
therefore stay reviewable by one person in limited time.

- Work MUST land in increments small enough to review in one sitting, each tied to its spec tasks.
- No speculative abstraction. Build what the current spec requires; generality is added when a
  second real caller exists.
- No narrating comments. Comment only where the *why* is non-obvious from the code.
- Scope MUST NOT expand beyond the active spec. Discovered adjacent work is recorded as a future
  spec, not silently implemented.
- Out-of-scope areas declared in a spec or in AGENTS.md MUST NOT be designed for in advance.

Rationale: the bottleneck is human review capacity, not code production. Volume the supervisor
cannot read is a liability, not progress.

## Technology & Platform Constraints

- **Product**: w.tv — a live streaming platform client (Twitch-like) for television.
- **Shared core**: TypeScript, bundled as a static web application. It is the single home for
  UI, navigation, state, and API and chat protocol handling.
- **Targets (exhaustive)**: Chromecast with Google TV (Android 12+) and Samsung Tizen. The app is
  locked to these two; phone, tablet, and desktop are not supported targets and MUST NOT shape
  design decisions.
- **Android shell**: a full-screen WebView host. It MUST hold no application logic.
- **Tizen shell**: a packaged Tizen web application (`.wgt`). It MUST hold no application logic.
- **Video**: delivered through the player adapter of Principle II — Tizen's native AVPlay on
  Tizen, browser-based HLS playback elsewhere.
- **Retired stack**: the native Kotlin/Compose/Media3 implementation is decommissioned. No new
  Kotlin application logic may be added; the Android shell is the only permitted native code.
- **Dependencies**: each third-party dependency MUST be justified against TV runtime cost and
  Tizen compatibility. Prefer no dependency over a convenient one.
- **Secrets**: no credentials or private tokens in the repository. Observed protocol details
  derived from captured traffic belong in documentation, not in committed captures of personal
  session data.

## Development Workflow & Quality Gates

- **Spec Kit flow**: `/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`.
  `/speckit-clarify` resolves underspecification before planning; `/speckit-analyze` checks
  cross-artifact consistency after tasks are generated.
- **Phase roadmap**: `docs/phases.md` sequences the product. A feature outside the active phase
  MUST NOT be implemented ahead of it, even when it is cheap.
- **Definition of done** for any task:
  1. Behavior matches its spec, including any declared Tizen reduction.
  2. Tests required by Principle VI exist and pass.
  3. The change builds for both targets.
  4. D-pad reachability and focus behavior are stated or demonstrated for new UI.
- **Verification honesty**: the agent MUST report what it actually ran. Untested changes are
  labeled untested; failures are surfaced with their output.
- **On-device checks**: changes to playback, focus traversal, or performance MUST be verified on
  real Android hardware before being called complete. When the agent cannot run that check, it
  MUST say so and hand the verification to the supervisor rather than inferring success.
- **Review authority**: the supervising developer approves every merge. Agent self-approval does
  not exist.

## Governance

This constitution supersedes all other development practice in this repository. Where a spec,
plan, task list, skill, or habit conflicts with it, this document wins, and the conflicting
artifact is corrected.

**Amendment procedure**: amendments are proposed as an edit to this file, justified in the Sync
Impact Report at its top, and adopted only by the supervising developer. Any amendment that
invalidates existing specs MUST state how those specs are migrated. Principles MUST NOT be
weakened silently — a relaxation is an amendment like any other.

**Versioning policy**: semantic versioning of governance.
- MAJOR — a principle is removed or redefined in a backward-incompatible way.
- MINOR — a principle or section is added, or guidance is materially expanded.
- PATCH — clarification, wording, or non-semantic refinement.

**Compliance review**: every plan MUST pass a constitution check before tasks are generated, and
every review MUST verify compliance with the principles above. Complexity, duplication, and
speculative generality MUST be justified against Principles II and VII or removed. Repeated
violation of a principle is treated as evidence the principle needs amending or the work needs
re-specifying — never as an acceptable exception.

**Runtime guidance**: `AGENTS.md` carries day-to-day operational detail (project layout, build
commands, code style) and MUST remain consistent with this constitution. On conflict, this
constitution governs and `AGENTS.md` is corrected.

**Version**: 1.1.0 | **Ratified**: 2026-10-05 | **Last Amended**: 2026-10-05
