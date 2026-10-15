#!/bin/sh
# Verifies inside the container that no host credential is reachable and that the agent cannot
# widen its own isolation, and that the observation browser's user agent matches its build. Exits
# non-zero if any check fails. Every check is passive: none connects to a forwarded socket or runs a
# host credential helper.

failures=0
pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; failures=$((failures + 1)); }

if env -u GH_TOKEN -u GITHUB_TOKEN /usr/bin/gh auth status >/dev/null 2>&1; then
  fail "gh has a stored login (run /usr/bin/gh auth logout)"
else
  pass "gh has no stored login"
fi

if [ "$(command -v gh)" = /usr/local/bin/gh ]; then
  pass "gh resolves to the app-token wrapper"
else
  fail "gh resolves to $(command -v gh), not /usr/local/bin/gh"
fi

if scope=$(wtv-app-token permissions 2>&1); then
  want='{"permissions":{"contents":"write","metadata":"read","pull_requests":"write"},"repositories":["'"$(git -C /workspace remote get-url origin | sed -E 's#^(https://github\.com/|git@github\.com:)##; s#\.git$##')"'"]}'
  if [ "$(printf '%s' "$scope" | jq -cS .)" = "$(printf '%s' "$want" | jq -cS .)" ]; then
    pass "app token is limited to contents and pull requests on one repository"
  else
    fail "app token scope differs from contents/pull_requests write on one repository: $scope"
  fi
else
  fail "no app token: $scope"
fi

# The gh wrapper sets GH_TOKEN only for its own process, so a shell-wide token is a leak.
if [ -n "${GH_TOKEN:-}${GITHUB_TOKEN:-}${GH_ENTERPRISE_TOKEN:-}" ]; then
  fail "a GitHub token is set in the environment"
else
  pass "no GitHub token in the environment"
fi

helpers=$(git config --show-origin --get-regexp 'credential.*helper' | awk '$3 != ""' | grep -v '!wtv-app-token git-credential')
if [ -z "$helpers" ]; then
  pass "wtv-app-token is the only git credential helper"
  filled=$(printf 'protocol=https\nhost=github.com\n\n' | GIT_TERMINAL_PROMPT=0 git credential fill 2>/dev/null | sed -n 's/^password=//p')
  if [ -n "$filled" ] && [ "$filled" = "$(wtv-app-token 2>/dev/null)" ]; then
    pass "git credential fill returns the app token"
  else
    fail "git credential fill does not return the app token"
  fi
else
  fail "other git credential helpers are configured: $helpers"
  fail "git credential fill not run, so that no other helper is queried"
fi

# Each attached VS Code window runs a vscode-remote-containers-server-<session>.js process, through
# which the host answers git and Docker credential requests whatever the Dev Containers settings
# say. Only the host environment that agent-window.sh sets leaves it nothing to answer with, which
# cannot be verified from here without using the channel. So each live channel must belong to a
# window whose extension host, on the same session, carries the launcher's marker. Files left in
# /tmp by a window that has gone serve nothing.
live=0
foreign=""
hosts=$(pgrep -f -- '--type=extensionHost')
for pid in $(pgrep -f '/tmp/vscode-remote-containers-server-'); do
  session=$(tr '\0' '\n' < "/proc/$pid/cmdline" 2>/dev/null | sed -n 's#^/tmp/vscode-remote-containers-server-\(.*\)\.js$#\1#p')
  [ -n "$session" ] || continue
  live=$((live + 1))
  owned=0
  for host in $hosts; do
    if tr '\0' '\n' < "/proc/$host/environ" 2>/dev/null | grep -qx 'WTV_AGENT_WINDOW=1' \
      && tr '\0' '\n' < "/proc/$host/environ" 2>/dev/null | grep -qx "REMOTE_CONTAINERS_IPC=/tmp/vscode-remote-containers-ipc-$session.sock"; then
      owned=1
    fi
  done
  [ "$owned" = 1 ] || foreign="$foreign $session"
done
if [ "$live" -eq 0 ]; then
  pass "no live VS Code host credential channel"
elif [ -z "$foreign" ]; then
  pass "VS Code's host credential channel is live only for windows opened by agent-window.sh"
else
  fail "VS Code's host credential channel is live for a window not opened by agent-window.sh (session$foreign)"
  [ -n "$hosts" ] || echo "      no extension host process found"
  for host in $hosts; do
    if [ -r "/proc/$host/environ" ]; then
      ipc=$(tr '\0' '\n' < "/proc/$host/environ" | grep '^REMOTE_CONTAINERS_IPC=')
      marker=$(tr '\0' '\n' < "/proc/$host/environ" | grep '^WTV_AGENT_WINDOW=')
      echo "      extension host $host: ${ipc:-REMOTE_CONTAINERS_IPC <unset>} ${marker:-WTV_AGENT_WINDOW <unset>}"
    else
      echo "      extension host $host: environment not readable"
    fi
  done
fi

if [ -e "$HOME/.gitconfig" ]; then
  fail "~/.gitconfig exists (dev.containers.copyGitConfig is not off)"
else
  pass "no copied ~/.gitconfig"
fi

identity="$(git config user.name)	$(git config user.email)"
if [ "$identity" = "$(wtv-app-token identity 2>/dev/null)" ]; then
  pass "git identity is the app's bot: $(git config user.name) <$(git config user.email)>"
else
  fail "git identity '$(git config user.name)' is not the app's bot"
fi

if [ -n "${SSH_AUTH_SOCK:-}" ] || ls /tmp/vscode-ssh-auth-*.sock >/dev/null 2>&1; then
  fail "an SSH agent is reachable"
else
  pass "no SSH agent"
fi

if command -v gpgconf >/dev/null 2>&1; then
  fail "gpgconf is installed, so VS Code can forward the host GPG agent"
else
  pass "no gpgconf, so VS Code forwards no GPG agent"
fi

if ls "$HOME"/.gnupg/S.gpg-agent* >/dev/null 2>&1 || ls /run/user/*/gnupg/S.gpg-agent* >/dev/null 2>&1 \
  || ls /tmp/vscode-gpg-* >/dev/null 2>&1; then
  fail "a GPG agent socket is present (VS Code forwards the host agent)"
elif [ -n "$(ls -A "$HOME/.gnupg/private-keys-v1.d" 2>/dev/null)" ]; then
  fail "~/.gnupg/private-keys-v1.d holds secret keys"
else
  pass "no GPG agent socket or secret keys"
fi

if ls -d "$HOME"/.vscode-server/extensions/[Gg]it[Hh]ub.* >/dev/null 2>&1; then
  fail "GitHub extensions are installed in the container: $(cd "$HOME/.vscode-server/extensions" && ls -d [Gg]it[Hh]ub.* | tr '\n' ' ')"
else
  pass "no GitHub extensions installed in the container"
fi

if grep -rqsF 'Logged in as' "$HOME"/.vscode-server/data/logs/*/exthost*/[Gg]it[Hh]ub.*; then
  fail "a GitHub extension logged a sign-in in the container's extension host"
else
  pass "no GitHub sign-in in the container's extension host logs"
fi

if [ -f "$HOME/.docker/config.json" ]; then
  fail "~/.docker/config.json exists"
else
  pass "no Docker registry credentials"
fi

if [ -e /var/run/docker.sock ] || [ -e /run/docker.sock ]; then
  fail "a Docker socket is mounted"
else
  pass "no Docker socket"
fi

if sudo -n true 2>/dev/null; then
  fail "node can sudo"
else
  pass "node cannot sudo"
fi

if [ -f /etc/claude-code/managed-settings.json ]; then
  pass "managed settings are installed"
else
  fail "managed settings are missing"
fi

for path in .devcontainer .vscode .git/hooks; do
  if touch "/workspace/$path/.isolation-probe" 2>/dev/null; then
    rm -f "/workspace/$path/.isolation-probe"
    fail "$path is writable"
  else
    pass "$path is read-only"
  fi
done

if git -C /workspace config --local isolation.probe 1 2>/dev/null; then
  git -C /workspace config --local --unset isolation.probe
  fail ".git/config is writable"
else
  pass ".git/config is read-only"
fi

# w.tv's WAF answers HeadlessChrome with 403s, so .mcp.json spoofs the user agent. It must name
# the Chromium major that the pinned @playwright/mcp launches; the client hints report that build.
args=$(jq -r '.mcpServers.playwright.args[]' /workspace/.mcp.json 2>/dev/null)
mcp_version=$(printf '%s\n' "$args" | sed -n 's#^@playwright/mcp@##p')
ua_major=$(printf '%s\n' "$args" | sed -n 's#^--user-agent=.* Chrome/\([0-9][0-9]*\)\..*#\1#p')
build=""
revision=""
for modules in "$HOME"/.npm/_npx/*/node_modules; do
  if [ "$(jq -r .version "$modules/@playwright/mcp/package.json" 2>/dev/null)" = "$mcp_version" ]; then
    build=$(jq -r '.browsers[] | select(.name == "chromium") | .browserVersion' "$modules/playwright-core/browsers.json")
    revision=$(jq -r '.browsers[] | select(.name == "chromium") | .revision' "$modules/playwright-core/browsers.json")
  fi
done
if [ -z "$build" ]; then
  fail "@playwright/mcp@${mcp_version:-?} from .mcp.json is not in the npx cache"
elif [ ! -d "/ms-playwright/chromium-$revision" ]; then
  fail "Chromium $build (revision $revision), which @playwright/mcp@$mcp_version launches, is not installed"
elif [ "$ua_major" != "${build%%.*}" ]; then
  fail ".mcp.json's user agent names Chrome ${ua_major:-<none>}, but @playwright/mcp@$mcp_version launches Chromium $build"
else
  pass ".mcp.json's user agent names Chrome $ua_major, matching the Chromium that @playwright/mcp@$mcp_version launches ($build)"
fi

echo "INFO  kernel.yama.ptrace_scope: $(cat /proc/sys/kernel/yama/ptrace_scope 2>/dev/null || echo unavailable)"
echo "INFO  mounts: $(awk '$3 !~ /^(proc|sysfs|tmpfs|devpts|mqueue|cgroup2?|overlay|shm)$/ {print $2}' /proc/mounts | sort | tr '\n' ' ')"

[ "$failures" -eq 0 ] && echo "All checks passed." || echo "$failures check(s) failed."
exit "$failures"
