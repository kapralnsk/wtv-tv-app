#!/bin/sh
# Runs on the host. Builds the local base image that the agent Dockerfile starts from, tagged as its
# FROM line names it, so the tag is set in one place. After any change to this directory, bump that
# tag and run this again.
#
# The config is named .devcontainer.json, not devcontainer.json, so that VS Code does not offer it
# when reopening the workspace in a container.
set -eu

repo=$(cd "$(dirname "$0")/../.." && pwd)
tag=$(sed -n 's/^FROM \(wtv-agent-base:[^[:space:]]*\).*/\1/p' "$repo/.devcontainer/Dockerfile")
if [ -z "$tag" ]; then
  echo "build.sh: .devcontainer/Dockerfile has no FROM wtv-agent-base:<n> line" >&2
  exit 1
fi

npx @devcontainers/cli build --workspace-folder "$repo" \
  --config "$repo/.devcontainer/base/.devcontainer.json" --frozen-lockfile --image-name "$tag"
