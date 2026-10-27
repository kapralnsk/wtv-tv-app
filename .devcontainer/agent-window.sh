#!/bin/sh
# Runs on the host. Opens the agent window: a VS Code instance separate from the supervisor's, with
# its own settings and extensions, never signed in to GitHub.
#
# Dev Containers relays git and Docker credential requests from the container to the host whatever
# its settings say, and forwards the host's SSH agent whenever SSH_AUTH_SOCK is set. So VS Code
# starts with an environment in which the host has nothing to answer with: no SSH agent, no git
# config that names a credential helper, and no Docker credential helper on PATH.
set -eu

repo=$(cd "$(dirname "$0")/.." && pwd)
dir="$HOME/.vscode-agent-window"
code="/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code"
docker="$HOME/.orbstack/bin/docker"

mkdir -p "$dir/user-data/User" "$dir/extensions" "$dir/docker/cli-plugins"
ln -sfn /Applications/OrbStack.app/Contents/MacOS/xbin/docker-buildx "$dir/docker/cli-plugins/docker-buildx"
# With no credentials configured at all, the devcontainer CLI falls back to osxkeychain.
printf '%s\n' '{"auths":{"none.invalid":{}}}' > "$dir/docker/config.json"

# Rewritten on every launch, so the settings cannot drift.
cat > "$dir/user-data/User/settings.json" <<EOF
{
  "dev.containers.copyGitConfig": false,
  "dev.containers.gitCredentialHelperConfigLocation": "none",
  "dev.containers.dockerCredentialHelper": false,
  "dev.containers.githubCLILoginWithToken": false,
  "dev.containers.dockerPath": "$docker",
  "remote.defaultExtensionsIfInstalledLocally": [],
  "chat.disableAIFeatures": true
}
EOF

# macOS puts launchd's SSH_AUTH_SOCK back into an app started without one; Dev Containers forwards
# only a non-empty one.
run() {
  env -i HOME="$HOME" USER="$USER" LOGNAME="$LOGNAME" TMPDIR="$TMPDIR" LANG="${LANG:-en_US.UTF-8}" \
    PATH=/usr/bin:/bin:/usr/sbin:/sbin SSH_AUTH_SOCK= \
    DOCKER_CONFIG="$dir/docker" DOCKER_HOST="unix://$HOME/.orbstack/run/docker.sock" \
    GIT_CONFIG_NOSYSTEM=1 GIT_CONFIG_GLOBAL=/dev/null \
    GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=credential.helper GIT_CONFIG_VALUE_0= \
    WTV_AGENT_WINDOW=1 \
    "$code" --user-data-dir "$dir/user-data" --extensions-dir "$dir/extensions" --force-disable-user-env "$@"
}

run --list-extensions | grep -qx ms-vscode-remote.remote-containers \
  || run --install-extension ms-vscode-remote.remote-containers
run "$repo"
