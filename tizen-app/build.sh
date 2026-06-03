#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Build the web app
cd "$ROOT/web"
npm run build

# Copy dist into tizen-app/ alongside config.xml and icon.png
rm -rf "$SCRIPT_DIR/dist"
cp -r "$ROOT/web/dist/." "$SCRIPT_DIR/"

echo "Done. Import tizen-app/ into Tizen Studio IDE and use Run As → Tizen Web Application to deploy."
