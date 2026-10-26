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
  auto mode and bypass-permissions mode is disabled. A deny list refuses the usual ways to merge
  or approve a pull request: `gh pr merge`, `gh pr review`, and their `gh api` forms. It is a
  best-effort layer: it matches command text, so the same call in another form, such as
  `/usr/bin/gh` or `sh -c`, gets past it. The guard is the ruleset on `main` (GitHub setup step
  3): the bot cannot update `main`, and a pull request needs an approval from someone other than
  its last pusher.
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
   to create or approve pull requests. The agent review's `CLAUDE_CODE_OAUTH_TOKEN` has to be a
   repository secret, because the review runs on pull request branches. Only the supervisor can
   change the workflow that reads it, since the implementer can't push workflow files, and the
   review never runs pull request code with it. Keep every other secret in an environment that
   only `main` can use.

## Host setup (once)

1. Docker runs on OrbStack (the active context). Every image and feature is public, so builds
   need no registry login. A `ghcr.io` login in the keychain is looked up on each build anyway;
   to build without it, point `DOCKER_CONFIG` at a directory holding a `cli-plugins/docker-buildx`
   symlink and a `config.json` of `{"auths":{"none.invalid":{}}}`, and set `DOCKER_HOST` to the
   OrbStack socket. The dummy entry matters: with no credentials configured at all, the
   devcontainer CLI falls back to `osxkeychain` on macOS.
2. Two VS Code instances. GitHub sign-ins, and each extension's permission to use them, are
   shared by every window of one instance, so the window that attaches to the container is a
   separate instance:
   - **Supervisor window:** the usual VS Code, with a "Supervisor" profile that holds only the
     GitHub Pull Requests and GitHub Actions extensions, signed in. It never attaches to the agent
     container.
   - **Agent window:** opened only by `.devcontainer/agent-window.sh`, with its own user data and
     extensions in `~/.vscode-agent-window`. It is never signed in to GitHub and Settings Sync
     stays off. Its only local extension is Dev Containers; `devcontainer.json` installs Claude
     Code in the container.

   Settings that govern an attach go in the **user** settings of the instance that attaches;
   `devcontainer.json` cannot set them. The script rewrites the agent window's user settings on
   every launch:

   ```json
   {
     "dev.containers.copyGitConfig": false,
     "dev.containers.gitCredentialHelperConfigLocation": "none",
     "dev.containers.dockerCredentialHelper": false,
     "dev.containers.githubCLILoginWithToken": false,
     "dev.containers.dockerPath": "/Users/<you>/.orbstack/bin/docker",
     "remote.defaultExtensionsIfInstalledLocally": [],
     "chat.disableAIFeatures": true
   }
   ```

   VS Code's default for `remote.defaultExtensionsIfInstalledLocally` installs Copilot, Copilot
   Chat and Pull Requests into every remote where they are installed locally. The Dev Containers
   settings keep `~/.gitconfig`, the git and Docker credential helpers and a `gh` login out of the
   container's config, but they do not close the channel behind them: the host answers git and
   Docker credential requests from the container whatever they say, and forwards its SSH agent
   whenever `SSH_AUTH_SOCK` is set. No setting turns either off. So the script also starts VS Code
   with a minimal environment in which the host has nothing to answer with: an empty
   `SSH_AUTH_SOCK` (when it is unset, macOS puts launchd's back), git ignoring the system and
   global config with credential helpers cleared, and a `PATH` without
   `docker-credential-osxkeychain` or Homebrew's `gpgconf` (hence the absolute `dockerPath`), with
   `DOCKER_CONFIG` pointing at a dummy config as in step 1.
3. The image has no `gpgconf` (the `no-gpg` feature removes it after desktop-lite installs it), so
   VS Code finds no agent socket in the container and forwards no GPG agent.

## Launch

From the repository root on the host, either:

- **CLI:** `npx @devcontainers/cli up --workspace-folder .`, then
  `npx @devcontainers/cli exec --workspace-folder . claude`. This path forwards nothing.
- **VS Code:** `.devcontainer/agent-window.sh`, then in that window Dev Containers: Rebuild and
  Reopen in Container on the first attach, and Reopen in Container afterwards. Run `claude` in its
  terminal. Attach only from this window. A container that any other window attached to may hold
  what that window copied or installed, so rebuild it from the agent window.

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
4. `.devcontainer/check-isolation.sh`, in every path you launch through. Every check must pass
   before handing the container to an agent; run it again after any change to the host setup.

To revoke the agent's access, delete the key in the app's settings, or uninstall the app.

## Logged-in browsing

The default `playwright` server is headless and isolated: no profile survives the session, and
nothing is logged in. w.tv's AWS WAF answers `HeadlessChrome` with 403s on its API hosts, so the
server sends a regular Chrome user agent. Its major version must match the Chromium that the
pinned `@playwright/mcp` launches, so update it whenever the pin changes; `check-isolation.sh`
compares the two. The client hints (`Sec-CH-UA`, `navigator.userAgentData`) are not spoofed and
report the real build; for Chromium 155 that is `Chromium` with no `HeadlessChrome` brand, which
the WAF accepts. If the WAF starts blocking headless anyway, switch the default to headed and
isolated rather than spoofing further. For features that need a w.tv login:

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
- **VS Code's host channel.** While the agent window is attached, its credential channel and VS
  Code server are in the container, reachable from every session there, CLI ones included.
  `check-isolation.sh` confirms each live channel belongs to a window opened by
  `agent-window.sh`, by a marker the agent could forge, and cannot verify from inside that the
  host answers nothing without using the channel. Processes in
  the container all run as `node`, and OrbStack's `kernel.yama.ptrace_scope` is 0, so the agent
  can inspect and alter the VS Code server and its extension host; the agent window keeps no
  GitHub sign-in for them to hold.
- **The Claude Code login.** It is inside the container, readable by the agent.
- **The app's private key.** Also inside and readable. Whoever holds it can mint tokens with the
  app's permissions on its installations: this repository only, without workflows or
  administration.
- **Egress.** Unrestricted. Revisit with a hostname allowlist proxy if sessions ever run
  unattended with permissions bypassed.
- **Build toolchains.** Added in phase 1.
