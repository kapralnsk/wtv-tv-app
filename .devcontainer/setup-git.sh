#!/bin/sh
# Rewrites the container's global git config on every start, so nothing an editor copied in
# survives. git authenticates and commits as the implementer GitHub App's bot. Once the app is set
# up, it also clones the repository into the workspace volume if that holds none yet.
set -eu

config="$GIT_CONFIG_GLOBAL"
mkdir -p "$(dirname "$config")"
: > "$config"

git config --global safe.directory /workspace
# The empty helper clears any helper set earlier, including system ones.
git config --global credential.helper ''
git config --global --add 'credential.https://github.com.helper' ''
git config --global --add 'credential.https://github.com.helper' '!wtv-app-token git-credential'

if [ ! -r "$HOME/.config/wtv-app/private-key.pem" ] || [ ! -r "$HOME/.config/wtv-app/app-id" ]; then
  echo "The implementer app is not set up. See First run in /opt/wtv-devcontainer/README.md, then run $0"
  exit 0
fi
if ! identity=$(wtv-app-token identity); then
  echo "Reading the bot's identity failed (see above), so git has no commit identity. Run $0 again"
  echo "once api.github.com is reachable."
  exit 1
fi
name=$(printf '%s' "$identity" | cut -f1)
email=$(printf '%s' "$identity" | cut -f2)
git config --global user.name "$name"
git config --global user.email "$email"
echo "git identity: $name <$email>"

# git clone refuses /workspace, which already holds the .vscode mountpoint. So the clone goes
# beside it and its .git moves in only once complete: a start interrupted mid-fetch starts over.
if [ ! -e /workspace/.git ]; then
  tmp=/workspace/.wtv-clone
  rm -rf "$tmp"
  echo "Cloning https://github.com/$WTV_REPO into /workspace"
  git clone -q --no-checkout "https://github.com/$WTV_REPO.git" "$tmp"
  mv "$tmp/.git" /workspace/.git
  rmdir "$tmp"
fi
# git writes the index last, so without one the first checkout never finished, even when a start
# stopped right after the move above. With no index, checkout is an initial checkout, and it
# refuses to overwrite files already in the way, which stops this script with git's error.
if [ ! -e /workspace/.git/index ]; then
  git -C /workspace checkout -q
  echo "Checked out $(git -C /workspace branch --show-current)"
fi
