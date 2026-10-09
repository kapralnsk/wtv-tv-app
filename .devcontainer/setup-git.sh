#!/bin/sh
# Rewrites the container's global git config on every start, so nothing an editor copied in
# survives. The commit identity follows whichever account gh is logged in as.
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
git config --global --add 'credential.https://github.com.helper' '!gh auth git-credential'

if user=$(gh api user --jq '[.id, .login] | @tsv' 2>/dev/null); then
  id=$(printf '%s' "$user" | cut -f1)
  login=$(printf '%s' "$user" | cut -f2)
  git config --global user.name "$login"
  git config --global user.email "$id+$login@users.noreply.github.com"
  echo "git identity: $login"
else
  echo "gh is not logged in. Run gh auth login, then $0"
fi
