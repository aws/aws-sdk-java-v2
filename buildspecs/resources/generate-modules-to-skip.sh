#!/bin/bash
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
ALLOWLIST="$SCRIPT_DIR/test-modules-publish-allowlist.txt"

if [[ "$1" == "--docs" ]]; then
  echo "$(ls "$REPO_ROOT/test/" | tr '\n' ',' | sed 's/,$//'),v2-migration"
elif [[ "$1" == "--maven" ]]; then
  ls "$REPO_ROOT/test/" | grep -vxFf "$ALLOWLIST" | tr '\n' ',' | sed 's/,$//'
else
  echo "Usage: $0 --maven | --docs" >&2
  exit 1
fi
