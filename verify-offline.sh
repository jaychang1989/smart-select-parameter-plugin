#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
node --check src/main/resources/io/jenkins/plugins/smartselect/smart-select.js
node src/test/offline/widget-smoke.cjs
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" src/main/java/io/jenkins/plugins/smartselect/SelectionCodec.java src/test/offline/SelectionCodecSmoke.java
java -cp "$OUT" SelectionCodecSmoke
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree as ET
for f in [Path('pom.xml'), *Path('src/main/resources').rglob('*.jelly')]:
    ET.parse(f)
print('XML/Jelly well-formedness: passed')
PY
echo 'Offline checks complete. Run mvn -U clean verify for Jenkins integration checks.'
