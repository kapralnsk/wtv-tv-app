# Roadmap

The supervisor's roadmap (AGENTS.md, Scope). It gives context to `/speckit-specify` and
`/speckit-clarify` and is never a source of requirements: specs are. A spec's Out of Scope section
names the later phase, or the [Not scheduled](#not-scheduled) list, that covers what it leaves out.

Design reference: [SmartTwitchTV](https://github.com/fgl27/smarttwitchtv) (constitution Principle
IV). "Per SmartTwitchTV" below means the spec follows its interaction unless the supervisor decides
otherwise. The SmartTwitchTV behavior cited here was read from its source (`master`, 2026-10-29),
so a spec can re-check it.

| Phase | Outcome                                 | New specs               | Status      |
| ----- | --------------------------------------- | ----------------------- | ----------- |
| Setup | Both targets build, package and install | none                    | not started |
| 1     | Watch one channel live, with its chat   | live-viewing, live-chat | not started |
| 2     | Find a channel and start watching it    | browse, channel-page    | not started |
| 3     | Watch a channel's past broadcasts       | vod-viewing             | not started |
| 4     | Browse live streams by category         | none (changes browse)   | not started |
| 5     | Sign in and follow channels             | account, following      | not started |

Spec names are suggestions; `/speckit-specify` sets the directory name. When a phase changes an
earlier phase's spec, it edits that `spec.md` and re-runs its plan and tasks (AGENTS.md, Workflow).

## Every phase

- **Language.** UI text comes in English and Russian, picked by the device language, with English
  for any other. Numbers and times follow the UI language. w.tv content (titles, chat, category
  names, tags) is shown as delivered, and requests send the matching `user_lang`. For plans:
  Chromium 69 has `Intl.PluralRules`, but not compact number formatting or
  `Intl.RelativeTimeFormat`.
- **Each spec declares** what the constitution requires of it: every screen's focusable elements,
  traversal, initial focus and BACK target, and every failure the user can meet, with its retry or
  BACK (IV); budgets when it touches startup, playback, D-pad input or chat, starting from the
  AGENTS.md defaults (V); each Tizen reduction and the limitation forcing it (III); and what its
  screens refresh on resume (II) and restore after a restart.
- **Focus return.** Going back to a screen puts focus on the element that left it.
- **Pick up where the user left off**, per SmartTwitchTV's "Restore Previous Playback". Whenever
  the app comes back, from the background or from a fresh start after it was closed or the TV
  slept, it shows the last screen in the state the user left it, with BACK leading where it led
  before, and restarts what was playing: a live stream at the live edge, a VOD at its last
  position. After a fresh start, a short notice says the previous session is being restored.
  Saved state never expires. Screens built around typing (search, sign-in) are not reopened; the
  app opens on the start screen instead. It is best effort: when something cannot come back (the
  stream ended, the VOD is gone, the session expired), the app says so and falls back along the
  BACK path to the first screen that still works. Overlays, pickers and screen off are not
  restored. It is always on, since there is no settings screen (Not scheduled).
- **Platform behavior** is observed, never assumed (VIII, `docs/observing-wtv.md`). Each phase
  lists what is captured and what still needs observing. The captures in `docs/*.contract.yml` and
  `docs/wtv-api.yaml` were made for the retired app in June 2026. The first spec that relies on one
  re-verifies it, and knowledge reused across features moves to `docs/platform/`.
- **A phase is done** when its specs are merged, the supervisor has verified the device (D) budgets
  on a Chromecast with Google TV, and has smoke-checked a Tizen 5.5-class TV. The supervisor
  updates Status.

## Setup (no spec)

The definition of done includes building for both targets, so this comes before the first feature
pull request:

- **Tizen packaging.** `tizen-app/` is empty and the dev container has no Tizen tooling. Adding it
  changes `.devcontainer/`, which needs the supervisor's request. The signing certificates are the
  supervisor's, so the supervisor signs and installs packages.
- **Install commands** for the Chromecast and the Tizen TV, recorded in `README.md`, for the
  supervisor's on-device checks.

## Phase 1: Live stream with chat

**Outcome.** The app opens straight into one channel's live stream, with its chat, on both targets.

**Specs.** `live-viewing`, then `live-chat`. `live-viewing` goes first: its plan builds playback in
both shells and chooses the player bridge (AGENTS.md, Not decided yet).

**Scope: `live-viewing`**

- The channel is fixed in the build. The retired app used `dunduk`.
- Full-screen video, as close to live as the stream allows without stalling.
- Waking the TV or reopening the app brings the stream back at the live edge, in the quality the
  user chose (Every phase).
- The TV counts as a viewer, as the web client does (stream join).
- Info overlay: channel name, avatar, stream title, category, tags, live indicator, time live and
  viewer count. It shows for 5 s when playback starts and on request: OK, LEFT, RIGHT or UP opens
  it. Opening it refreshes the viewer count, title and category, and time live ticks while it
  shows. It holds the player controls, per SmartTwitchTV's player panel.
- Quality picker: Auto plus each rendition the stream offers (1080p60 down to 160p30 in the
  capture), with the current choice marked.
- Screen off (audio only), per SmartTwitchTV's "Screen off (Audio only)": a black layer covers the
  screen while the stream plays on unchanged, in the same quality. Any button brings the picture
  back and still does its usual action, BACK included. The screen stays awake, as during
  playback, so the TV's screensaver doesn't start.
- Media keys: play/pause pauses and resumes, and resuming jumps to the live edge; stop closes the
  player.
- When the stream ends, per SmartTwitchTV, a "<channel> has ended" dialog counts down 6 s. It
  offers to stay on the stream, with the app checking periodically until it is live again, or to
  leave. With nothing behind the player in this phase, the countdown ends by staying.
- Failures: channel offline at launch, network loss, playback error.

**Scope: `live-chat`**

- Chat sits over the video on the right, full height and about a quarter of the width:
  SmartTwitchTV's default. Overlay controls move it, resize it and switch to side by side, as in
  SmartTwitchTV. DOWN, or a control in the info overlay, shows and hides it. These choices survive
  a restart.
- Chat opens with recent history, then shows live messages, undelayed.
- Each message shows its time, the sender's nickname and the text, with w.tv emotes drawn as
  images; animated emotes animate. Nickname colors are random per user but the same for every
  message from that user, from a palette that meets the AGENTS.md contrast thresholds. The
  platform's own color is not used, as with SmartTwitchTV's "Readable nick colors".
- Messages w.tv deletes stay, clearly marked as deleted.
- A disconnect shows on screen. Chat reconnects by itself and fills the gap from history without
  duplicates.
- The message buffer and per-frame limits in AGENTS.md apply; chat never costs video frames
  (Principle V).

**D-pad.** The quality picker opens with focus on the current choice. BACK closes the innermost
open layer (picker, then overlay). With nothing open, the first BACK shows "Press BACK again to
exit" for 3 s, and a second BACK closes the player, which in this phase leaves the app
(SmartTwitchTV).

**Platform knowledge**

- Captured: channel slug → channel ID; channel detail with the live stream and its playback URL;
  the HLS master playlist (five renditions, no auth); stream join; the chat token, sent as
  `Sec-WebSocket-Protocol`; chat history (newest first, emotes inline as `:name:`); live `MESSAGE`
  frames. Sources: `docs/load_live_stream.contract.yml`, `docs/load_live_chat.contract.yml`,
  `docs/wtv-api.yaml`.
- To observe: the avatar URL; category and tags on the channel-detail path (captured only in the
  live list); the Russian `user_lang` value; whether the web client refreshes the viewer count;
  what it shows when a stream ends; whether the playback URL expires (open in the contract);
  whether w.tv streams offer a low-latency mode, and how far behind live the web client plays; the
  emote catalog and image URLs; non-`MESSAGE` chat events such as deletions.
- Risk: in June 2026, chat frames were invisible to Playwright, because the chat SDK handles them
  in a worker, and a separate WebSocket probe captured them. If the Playwright MCP still cannot see
  them, escalate per `docs/observing-wtv.md` before planning `live-chat`.

**Budgets.** All AGENTS.md budgets apply.

**Tizen.** AVPlay may not switch to one chosen rendition mid-stream; Auto only, or a restart per
switch, would be a declared reduction. Chat at 20 msg/s on a 2020 TV may need a looser Tizen
budget.

## Phase 2: Find channels

**Outcome.** The app reopens where the user left it, or else on the channels live now. The user can
search channels by name, open a channel page, and start its stream.

**Specs.** `browse` (main menu, Live now, search, reopening the last screen) and `channel-page`.
Changes `live-viewing`: the fixed channel goes away, the player opens from a tile or a channel
page, and BACK returns there.

**Scope**

- A main menu, per SmartTwitchTV's side panel, with Live now, Search and Exit. Exit asks for
  confirmation before leaving the app. Later phases add entries.
- Live now is the start screen when there is nothing to restore: a grid of live streams, most
  popular first, three per row, loading more as focus nears the end. Tile: thumbnail, channel
  name, language, stream title, category, time live, viewer count.
- On a tile, per SmartTwitchTV: OK starts the stream, holding LEFT opens the tile's options (among
  them its channel page), and holding OK reloads the grid.
- Live now reloads when shown if its data is over an hour old, in the background while the app is
  visible, and after resume.
- Search: channels by name, typed on the platform's on-screen keyboard and run on submit, per
  SmartTwitchTV. Results: avatar, name and live indicator, six per row, loading more as focus nears
  the end. A result opens the channel page.
- Channel page: name, avatar, follower count, description, live indicator. When live, a preview
  block (thumbnail, title, category, tags, time live, viewer count) that starts the stream. When
  offline, the block shows the channel's offline image and says it is offline; selecting it opens
  the channel's chat and waits for the stream to come back.
- The end dialog's countdown now closes the player back to the screen behind it, and the dialog
  gains an option to open the channel page.
- Failures and empty states: nothing live, no results, channel not found, network loss.

**D-pad.** With nothing to restore, the app opens on Live now with focus on its first tile. LEFT
from a grid's first column opens the menu. BACK from a channel page returns to the screen that
opened it, and so does the player's second BACK (Phase 1). BACK on a top-level screen opens the
menu and BACK in the menu closes it, so leaving the app always goes through Exit (SmartTwitchTV).

**Platform knowledge**

- Captured: the live streams list (thumbnail, title, viewers, tags, category, channel; `limit` 50
  in the capture, sorted by score) and channel detail (name, followers, live state, live stream).
- To observe: channel search (request and results); channel description and avatar; paging past
  50 live streams; the live thumbnail on the channel page; how often thumbnails change; the
  channel's offline image; whether chat can be joined while the channel is offline.

**Budgets.** Launch now ends on Live now or a restored screen. The spec sets ceilings for launch →
Live now usable and for tile → first video frame; a restored stream keeps the AGENTS.md launch →
first video frame ceiling. The 100 ms focus budget holds in grids while thumbnails load.

**Risks.** The platform keyboards differ between targets, and the browser traversal test cannot
drive them; the spec says what that test covers on the search screen. Image-heavy grids may need a
looser Tizen budget.

## Phase 3: Past broadcasts

**Outcome.** The user browses a channel's past broadcasts (VODs) and watches them with pause and
seek.

**Specs.** `vod-viewing`. Changes `channel-page`: a VOD section joins its layout and traversal. The
player adapter gains pause, seek, position and duration on both backends.

**Scope**

- Channel page: the latest VODs in a grid, loading more as focus nears the end. Tile: thumbnail,
  title, category, tags, duration, date.
- VOD player: play and pause; a progress bar showing position and duration; the quality picker and
  screen off, as in live; an info overlay with the VOD's details.
- Seeking, per SmartTwitchTV: LEFT and RIGHT seek even with the overlay hidden. The step starts at
  5 s and grows each second the key is held (10 s, 30 s, then 1, 2, 5, 10, 15, 20 and 30 min, up
  to 1 h), and resets after 1 s without presses.
- Media keys: play/pause pauses and resumes, stop closes the player, next and previous jump 30 s,
  and fast-forward and rewind seek like RIGHT and LEFT.
- When w.tv has chat replay, the VOD plays with it, as in SmartTwitchTV.
- At the end, the end dialog offers replay and the channel page, counting down 6 s to closing the
  player.
- A live stream's end dialog gains an option to open the broadcast as a VOD, when w.tv has one.
- A VOD that was playing when the app closed comes back at its position (Every phase).
- Failures: VOD unavailable, network loss, playback error.

**Platform knowledge.** Nothing captured. To observe: whether and where w.tv lists a channel's
VODs (request, paging, fields such as views); the VOD playback URL and any auth; VOD states such
as processing or removed; whether VODs have chat replay.

**Budgets.** New metrics: VOD start → first frame, seek → first frame, keypress → progress bar
update.

**Tizen.** AVPlay supports seek and pause; no reduction expected.

## Phase 4: Categories

**Outcome.** The user browses live streams by category.

**Specs.** Changes `browse`.

**Scope**

- A Categories menu entry: a grid of categories, most watched first, five per row, loading more as
  focus nears the end. Tile: image, name, and its viewer and live-channel counts.
- Category page: the category's live streams, in Live now tiles.
- Search finds categories by name too, through a search-type choice next to channels, per
  SmartTwitchTV.
- End dialogs gain an option to open the stream's category.

**Platform knowledge.** Nothing captured. To observe: the category list (order, images, viewer
counts, paging); category search; live streams filtered by category; whether category names
follow `user_lang`.

**Budgets.** Phase 2's grid budgets apply.

## Phase 5: Sign-in and following

**Outcome.** The user signs in to w.tv, sees the channels they follow, and follows or unfollows
from a channel page.

**Specs.** `account` (sign in, sign out, session), then `following` (Following entry, follow
button). Changes `browse` (menu entries) and `channel-page` (follow button).

**Observe first.** How the web client signs in: methods, captcha, two-factor, any code or QR flow,
the session token, its lifetime and refresh. Only the supervisor can log in, in a headed session
(`docs/observing-wtv.md`). `account` is specified after this.

**Scope**

- Sign in with the most TV-friendly method w.tv offers: a code or QR confirmed on another device
  if one exists, otherwise credentials typed on the on-screen keyboard.
- The session survives restarts. Sign out from the menu. An expired session says so and offers
  sign-in again.
- Following, a menu entry with two lists, per SmartTwitchTV: followed channels live now, most
  viewers first, in live tiles; and all followed channels A–Z, offline ones included.
- Follow and unfollow on the channel page. A failed change reverts and says so. Signed out, the
  button stays, and pressing it says signing in is needed, per SmartTwitchTV.
- Tokens are never logged or put in URLs or error reports (constitution, Secrets). The core keeps
  them, since a shell's scope has no storage (Principle II).

**Risk.** If signing in needs a captcha or a browser-only step the app cannot host, the supervisor
decides between a reduced flow and follows kept on the device without an account.

**Platform knowledge.** Nothing captured. To observe, signed in: followed channels (fields, live
state, paging); follow and unfollow requests; what else changes for a signed-in client.

## Not scheduled

Out of every phase until the supervisor adds them. Specs cite this list in Out of Scope.

- Sending chat messages, chat badges, moderation.
- Clips, multistream, picture-in-picture.
- A settings screen, and preferences kept beyond what a phase states.
- Watch history, and offering to resume a VOD the user opens again later. The VOD playing when the
  app closed is still restored (Every phase).
- Notifications, Google TV home-screen rows, voice search, deep links.
- Languages other than English and Russian.
