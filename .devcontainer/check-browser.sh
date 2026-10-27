#!/bin/sh
# Checks that the observation browser's spoofed user agent still matches its build. w.tv's WAF
# answers HeadlessChrome with 403s, so .mcp.json sends a regular Chrome user agent, which must name
# the Chromium major that the pinned @playwright/mcp launches; the client hints report that build.
# docs/observing-wtv.md runs it before observation work, so a mismatch stops only observation.
# Exits non-zero on failure. Passive: it only reads files.
set -u

pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; exit 1; }

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
