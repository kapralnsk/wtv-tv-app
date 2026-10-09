#!/bin/sh
# Verifies inside the container that no host credential is reachable and that the agent cannot
# widen its own isolation. Exits non-zero if any check fails.

failures=0
pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; failures=$((failures + 1)); }

accounts=$(gh auth status 2>&1 | grep -c 'Logged in to github.com')
if [ "$accounts" -eq 1 ]; then
  pass "gh has one account: $(gh api user --jq .login 2>/dev/null)"
else
  fail "gh has $accounts accounts logged in to github.com (want 1)"
fi

if [ -n "${GH_TOKEN:-}${GITHUB_TOKEN:-}${GH_ENTERPRISE_TOKEN:-}" ]; then
  fail "a GitHub token is set in the environment"
else
  pass "no GitHub token in the environment"
fi

helpers=$(git config --show-origin --get-regexp 'credential.*helper' | awk '$3 != ""' | grep -v '!gh auth git-credential')
if [ -z "$helpers" ]; then
  pass "gh is the only git credential helper"
else
  fail "other git credential helpers are configured: $helpers"
fi

filled=$(printf 'protocol=https\nhost=github.com\n\n' | GIT_TERMINAL_PROMPT=0 git credential fill 2>/dev/null | sed -n 's/^password=//p')
if [ -n "$filled" ] && [ "$filled" = "$(gh auth token 2>/dev/null)" ]; then
  pass "git credential fill returns gh's token"
else
  fail "git credential fill does not return gh's token"
fi

if ls /tmp/vscode-remote-containers-*.js >/dev/null 2>&1 || [ -n "${REMOTE_CONTAINERS_IPC:-}" ]; then
  fail "VS Code's git credential helper is present (/tmp/vscode-remote-containers-*.js or REMOTE_CONTAINERS_IPC)"
else
  pass "no VS Code git credential helper"
fi

name=$(git config user.name)
if [ -n "$name" ] && [ "$name" = "$(gh api user --jq .login 2>/dev/null)" ]; then
  pass "git identity matches gh: $name <$(git config user.email)>"
else
  fail "git identity '$name' does not match the gh account"
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
