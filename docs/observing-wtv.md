# Observing w.tv

Platform behavior is taken only from observing the w.tv web client (constitution Principle VIII).
The observation instrument is the **Playwright MCP** (`mcp__playwright__*` tools).

Before any observation work, check that the MCP works: open `https://w.tv/` with
`browser_navigate` and confirm with `browser_snapshot` that the main page rendered.

- Fix what is within your control (e.g. a stale page or a closed browser) and re-check.
- Escalate anything outside your control to the supervisor. This includes an MCP server that is
  not configured or not connecting, a missing browser install, and w.tv being unreachable or
  blocking automation. Name the failing step, quote the error, and state the exact action
  needed. Then stop observation work. Do not fall back to assumptions, other tools, or retired
  code.
- If observation needs a logged-in session, ask the supervisor to log in to the MCP browser.
  Never handle credentials.
