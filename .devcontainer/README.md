# Agent dev container

Agent sessions run here so that the only GitHub credential they can reach is the implementer
GitHub App's private key, which mints short-lived tokens for this repository (constitution, Agent
environment). Only the supervisor changes this
directory, and reviews any diff to it before rebuilding.

## What is inside

- Node 24 LTS, `gh`, `git`, Python 3 (spec-kit), ripgrep, Claude Code (native installer: latest, auto-updating).
- `wtv-app-token`, which mints the app's installation tokens, and a `gh` wrapper at
  `/usr/local/bin/gh` that runs each call with a fresh token. git uses `wtv-app-token` as its only
  credential helper. Tokens are narrowed to contents and pull requests on this repository and
  expire within an hour; no login is stored.
- Playwright MCP pinned in `.mcp.json`, with its Chromium build preinstalled.
- A virtual desktop (desktop-lite) with noVNC on `127.0.0.1:6080`, used only by headed sessions.
- Managed settings (`managed-settings.json`, installed at `/etc/claude-code/`): sessions start in
  auto mode, bypass-permissions mode is disabled, and `gh pr merge` and `gh pr review` are
  denied.
- No firewall: egress is unrestricted, and auto mode is the control.

The workspace is bind-mounted at `/workspace`. `.devcontainer/`, `.vscode/`, `.git/config` and
`.git/hooks/` are mounted read-only over it, because the host executes them (container rebuilds,
VS Code, and every host `git` command). The container's git config sets `push.default=current`,
since `git push -u` cannot record tracking in a read-only `.git/config`.

Named volumes, per workspace, keep state across rebuilds:

| Volume                         | Mounted at                  | Holds                           |
| ------------------------------ | --------------------------- | ------------------------------- |
| `wtv-claude-config-<id>`       | `/home/node/.claude`        | Claude Code login and settings  |
| `wtv-app-key-<id>`             | `/home/node/.config/wtv-app` | The app's private key and ID   |
| `wtv-browser-profile-<id>`     | `/home/node/.browser-profile` | The headed browser's profile  |

## GitHub setup (once)

The agent acts as a GitHub App's bot, so its pushes, pull requests and comments are distinct from
the supervisor's, and the supervisor can approve them.

1. Register a GitHub App under the supervisor's account (Settings → Developer settings → GitHub
   Apps). Installable only on this account; webhook inactive. Repository permissions: Contents
   read and write, Pull requests read and write, Metadata read. Leave Workflows and
   Administration at no access, so the bot can neither push workflow files nor change settings.
2. Install it on this repository only, and generate a private key.
3. Ruleset on `main`: require a pull request with 1 approval, dismiss stale approvals, require
   approval of the most recent push, require conversation resolution, block force pushes and
   deletion; the supervisor bypasses for pull requests only. To keep merging with the supervisor
   even after approval, also restrict updates, with only the repository admin as bypass actor.
4. Repository settings → Actions: workflow permissions read-only, and GitHub Actions not allowed
   to create or approve pull requests. Keep secrets in an environment that only `main` can use.

## Host setup (once)

1. Docker runs on OrbStack (the active context). Every image and feature is public, so builds
   need no registry login. A `ghcr.io` login in the keychain is looked up on each build anyway;
   to build without it, point `DOCKER_CONFIG` at a directory holding a `cli-plugins/docker-buildx`
   symlink and a `config.json` of `{"auths":{"none.invalid":{}}}`, and set `DOCKER_HOST` to the
   OrbStack socket. The dummy entry matters: with no credentials configured at all, the
   devcontainer CLI falls back to `osxkeychain` on macOS.
2. Nothing from the host is forwarded. For the VS Code path, set these in VS Code **user**
   settings on the host, because they cannot be set from `devcontainer.json`:

   ```json
   "dev.containers.copyGitConfig": false,
   "dev.containers.gitCredentialHelperConfigLocation": "none",
   "dev.containers.dockerCredentialHelper": false
   ```

   The container ignores `~/.gitconfig` and rewrites its own git config on every start, but VS
   Code's credential helper talks to the host keychain directly, so only these settings close it.
   `check-isolation.sh` reports whether they took effect.

## Launch

From the repository root on the host, either:

- **CLI:** `npx @devcontainers/cli up --workspace-folder .`, then
  `npx @devcontainers/cli exec --workspace-folder . claude`.
- **VS Code:** Dev Containers: Reopen in Container, then run `claude` in its terminal.

Both use the same container definition and volumes.

## First run

Run these in the container. None of them needs the agent.

1. Put the app's ID and private key into the key volume:
   - `printf '%s\n' <app-id> > ~/.config/wtv-app/app-id`
   - `install -m 600 /dev/stdin ~/.config/wtv-app/private-key.pem`, then paste the key and press
     Ctrl-D.

   Keep the original key somewhere safe, such as a password manager, and delete the downloaded
   file.
2. `.devcontainer/setup-git.sh` to apply the bot's commit identity. It also runs on every
   container start.
3. `claude`, then `/login`.
4. `.devcontainer/check-isolation.sh`. Every check must pass before handing the container to an
   agent; run it again after changing VS Code settings.

To revoke the agent's access, delete the key in the app's settings, or uninstall the app.

## Logged-in browsing

The default `playwright` server is headless and isolated: no profile survives the session, and
nothing is logged in. w.tv's AWS WAF answers `HeadlessChrome` with 403s on its API hosts, so the
server sends a regular Chrome user agent. Keep its major version in step with the Chromium that
the pinned `@playwright/mcp` uses, and if the WAF starts blocking headless anyway, switch the
default to headed and isolated rather than spoofing further. For features that need a w.tv login:

1. Start the session with the headed server added:
   `claude --mcp-config .devcontainer/mcp-headed.json`.
2. Have the agent open `https://w.tv/` with the `playwright-headed` tools.
3. Open `http://localhost:6080` on the host (noVNC, no password, bound to `127.0.0.1`) and log
   in on the browser there.

The profile persists in its volume, so the login survives rebuilds until w.tv expires it. Only
one browser can use the profile at a time. To log out for good, remove the
`wtv-browser-profile-<id>` volume.

## What this does not cover

- **Host execution of workspace files.** The agent can write the rest of the workspace, and the
  host runs some of it: `.claude/settings.json` hooks and `.mcp.json` servers in a host Claude
  session, build scripts such as `gradlew` and `package.json` scripts, and `.idea/` run
  configurations. Run agent sessions only in the container, and review workspace changes before
  running anything from them on the host.
- **The Claude Code login.** It is inside the container, readable by the agent.
- **The app's private key.** Also inside and readable. Whoever holds it can mint tokens with the
  app's permissions on its installations: this repository only, without workflows or
  administration.
- **Egress.** Unrestricted. Revisit with a hostname allowlist proxy if sessions ever run
  unattended with permissions bypassed.
- **Build toolchains.** Added in phase 1.
