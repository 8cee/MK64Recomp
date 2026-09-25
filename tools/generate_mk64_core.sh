#!/usr/bin/env bash
set -euo pipefail

EXPECTED_SHA1="579c48e211ae952530ffc8738709f078d5dd215e"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPSTREAM="$ROOT/upstream/MK64Recomp"

ROM="${1:-}"
N64RECOMP="${N64RECOMP:-$ROOT/tools/N64Recomp}"
RSPRECOMP="${RSPRECOMP:-$ROOT/tools/RSPRecomp}"

if [[ -z "$ROM" || ! -f "$ROM" ]]; then
  echo "Usage: $0 /path/to/mario-kart-64-usa.z64"
  echo "Optional: N64RECOMP=/path/to/N64Recomp RSPRECOMP=/path/to/RSPRecomp"
  exit 2
fi

if [[ ! -x "$N64RECOMP" || ! -x "$RSPRECOMP" ]]; then
  echo "N64Recomp/RSPRecomp not found."
  echo "Set N64RECOMP and RSPRECOMP or place them in tools/."
  exit 3
fi

ACTUAL_SHA1="$(sha1sum "$ROM" | awk '{print $1}')"
if [[ "$ACTUAL_SHA1" != "$EXPECTED_SHA1" ]]; then
  echo "Wrong ROM."
  echo "Expected: $EXPECTED_SHA1"
  echo "Actual:   $ACTUAL_SHA1"
  exit 4
fi

if [[ ! -f "$UPSTREAM/us.rev1.toml" ]]; then
  echo "Upstream submodule is missing. Run:"
  echo "git submodule update --init --recursive"
  exit 5
fi

echo "ROM verified."
cp "$ROM" "$UPSTREAM/mk64.us.z64"

cleanup() {
  rm -f "$UPSTREAM/mk64.us.z64"
}
trap cleanup EXIT

pushd "$UPSTREAM" >/dev/null
rm -rf RecompiledFuncs
mkdir -p RecompiledFuncs rsp

"$N64RECOMP" us.rev1.toml
"$RSPRECOMP" aspMain.us.rev1.toml

if [[ -f njpgdspMain.us.rev1.toml ]]; then
  "$RSPRECOMP" njpgdspMain.us.rev1.toml
fi

test -n "$(find RecompiledFuncs -type f -print -quit)"
test -f rsp/aspMain.cpp

echo "Recompiled game sources generated successfully in:"
echo "  $UPSTREAM/RecompiledFuncs"
echo "  $UPSTREAM/rsp"
popd >/dev/null
