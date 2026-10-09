#!/bin/sh
# Rewrites the container's global git config on every start, so nothing an editor copied in
# survives. git authenticates and commits as the implementer GitHub App's bot.
set -eu

config="$GIT_CONFIG_GLOBAL"
mkdir -p "$(dirname "$config")"
: > "$config"

git config --global safe.directory /workspace
# .git/config is read-only in the container: no upstream tracking, so push the current branch.
git config --global push.default current
# The empty helper clears any helper set earlier, including system ones.
git config --global credential.helper ''
git config --global --add 'credential.https://github.com.helper' ''
git config --global --add 'credential.https://github.com.helper' '!wtv-app-token git-credential'

if identity=$(wtv-app-token identity); then
  name=$(printf '%s' "$identity" | cut -f1)
  email=$(printf '%s' "$identity" | cut -f2)
  git config --global user.name "$name"
  git config --global user.email "$email"
  echo "git identity: $name <$email>"
else
  echo "The implementer app is not set up. See .devcontainer/README.md, then run $0"
fi
