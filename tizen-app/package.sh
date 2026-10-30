#!/usr/bin/env bash
# Builds the web core and zips tizen-app/ and web/dist into an unsigned .wgt.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
out="${1:-$root/tizen-app/build/wtv.wgt}"

(cd "$root/web" && npm run build)
mkdir -p "$(dirname "$out")"
python3 -I - "$root" "$out" <<'PY'
import os
import sys
import zipfile

root, out = sys.argv[1:]
files = {}
for base in (os.path.join(root, "tizen-app"), os.path.join(root, "web", "dist")):
    for directory, subdirs, names in os.walk(base):
        subdirs[:] = [d for d in subdirs if not d.startswith(".") and d != "build"]
        for name in names:
            if name == "package.sh" or name.startswith("."):
                continue
            path = os.path.join(directory, name)
            files[os.path.relpath(path, base).replace(os.sep, "/")] = path

if "config.xml" not in files or "index.html" not in files:
    sys.exit("config.xml and index.html must both be at the package root")

if os.path.exists(out):
    os.remove(out)
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as wgt:
    for arcname in sorted(files):
        info = zipfile.ZipInfo(arcname, (1980, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o644 << 16
        with open(files[arcname], "rb") as f:
            wgt.writestr(info, f.read())
print(out)
PY
