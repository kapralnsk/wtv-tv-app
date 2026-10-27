#!/bin/sh
# Reports whether /workspace holds a clone whose first checkout finished. The managed SessionStart
# hook puts this in every session's context, and the supervisor runs it before handover. git
# writes the index last, so a clone without one is broken: every tracked file shows as a staged
# deletion. Exits non-zero on failure. Passive: it only reads files.
set -u

if [ ! -e /workspace/.git ]; then
  echo "FAIL  /workspace holds no clone yet. Stop, and ask the supervisor to finish First run in"
  echo "      /opt/wtv-devcontainer/README.md."
  exit 1
fi
if [ ! -e /workspace/.git/index ]; then
  echo "FAIL  The first checkout of /workspace didn't finish, so every tracked file shows as a"
  echo "      staged deletion. Stop and report this to the supervisor. Don't commit, reset or"
  echo "      check out files to repair it."
  exit 1
fi
branch=$(git -C /workspace branch --show-current 2>/dev/null)
echo "PASS  /workspace holds a checked-out clone, on ${branch:-a detached HEAD}"
