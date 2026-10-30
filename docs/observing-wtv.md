# Observing w.tv

Platform behavior is taken only from observing the w.tv web client (constitution Principle VIII).
The observation instrument is the **Playwright MCP** (`mcp__playwright__*` tools).

Before any observation work, in the dev container run `/opt/wtv-devcontainer/check-browser.sh`.
It fails when the spoofed user agent no longer matches the browser build, which w.tv's WAF may
answer with 403s on its API hosts even while the page renders; escalate that as below. Then check
that the MCP works: open `https://w.tv/` with `browser_navigate` and confirm with
`browser_snapshot` that the main page rendered.

- Fix what is within your control (e.g. a stale page or a closed browser) and re-check.
- Escalate anything outside your control to the supervisor. This includes an MCP server that is
  not configured or not connecting, a missing browser install, and w.tv being unreachable or
  blocking automation. Name the failing step, quote the error, and state the exact action
  needed (AGENTS.md, Blocked work). Then stop observation work. Do not fall back to assumptions,
  instruments other than those below, or retired code.
- The `playwright` server is headless and isolated, with no w.tv login. If observation needs a
  logged-in session, stop and ask the supervisor to start a headed session (`playwright-headed`,
  see `.devcontainer/README.md`) and log in. Never handle credentials.

## Other instruments

The Playwright MCP is the default. Two more instruments are allowed, only for what it cannot see,
such as chat frames that the chat SDK handles in a web worker:

1. **A worker-level CDP session**, opened through the Playwright MCP's `browser_run_code_unsafe`
   tool and attached to the worker that holds the connection, recording its WebSocket frames. Try
   this first.
2. **A Node.js WebSocket probe** run in the dev container. It repeats a handshake observed in the
   browser in the same session, with the same URL and headers, and records the frames it receives.
   It never makes up a handshake. Pass the token in an environment variable, never in a file. A
   probe worth keeping goes in the feature's `specs/` directory, with no token in it.

Each observation names the instrument that made it (constitution Principle VIII), and its captures
are scrubbed before they become fixtures.

## Channels

Observe live behavior, such as streams, chat, viewer counts and the end of a stream, on any channel
that is live at the time. The channel fixed in a build doesn't have to be the one observed. Record
which channel each observation used.
