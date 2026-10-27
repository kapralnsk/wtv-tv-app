#!/bin/sh
# Installed as /usr/local/bin/gh. Each call runs as the implementer app with a fresh token, and the
# token lives only in that process's environment.
token=$(wtv-app-token) || exit 1
GH_TOKEN=$token exec /usr/bin/gh "$@"
