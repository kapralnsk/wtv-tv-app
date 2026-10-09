#!/bin/sh
# Verifies inside the container that no host credential is reachable and that the agent cannot
# widen its own isolation. Exits non-zero if any check fails.

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
else
  fail "other git credential helpers are configured: $helpers"
fi

filled=$(printf 'protocol=https\nhost=github.com\n\n' | GIT_TERMINAL_PROMPT=0 git credential fill 2>/dev/null | sed -n 's/^password=//p')
if [ -n "$filled" ] && [ "$filled" = "$(wtv-app-token 2>/dev/null)" ]; then
  pass "git credential fill returns the app token"
else
  fail "git credential fill does not return the app token"
fi

if ls /tmp/vscode-remote-containers-*.js >/dev/null 2>&1 || [ -n "${REMOTE_CONTAINERS_IPC:-}" ]; then
  fail "VS Code's git credential helper is present (/tmp/vscode-remote-containers-*.js or REMOTE_CONTAINERS_IPC)"
else
  pass "no VS Code git credential helper"
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

if ls "$HOME"/.gnupg/S.gpg-agent* /tmp/vscode-gpg-* >/dev/null 2>&1; then
  fail "a GPG agent socket is present (VS Code forwards the host agent)"
elif [ -n "$(gpg --no-autostart --list-secret-keys 2>/dev/null)" ]; then
  fail "gpg lists secret keys"
else
  pass "no GPG agent socket or secret keys"
fi

if [ -f "$HOME/.docker/config.json" ]; then
  fail "~/.docker/config.json exists: $(cat "$HOME/.docker/config.json")"
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

echo "INFO  mounts: $(awk '$3 !~ /^(proc|sysfs|tmpfs|devpts|mqueue|cgroup2?|overlay|shm)$/ {print $2}' /proc/mounts | sort | tr '\n' ' ')"

[ "$failures" -eq 0 ] && echo "All checks passed." || echo "$failures check(s) failed."
exit "$failures"
