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
  needed. Then stop observation work. Do not fall back to assumptions, other tools, or retired
  code.
- The `playwright` server is headless and isolated, with no w.tv login. If observation needs a
  logged-in session, stop and ask the supervisor to start a headed session (`playwright-headed`,
  see `.devcontainer/README.md`) and log in. Never handle credentials.
