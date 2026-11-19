#!/bin/bash
# Generates a comma-separated list of modules to skip during publishing.
#
# Usage:
#   generate-modules-to-skip.sh --release   Modules to skip for Maven Central release publishing.
#                                            Skips all test/ modules except those in test-modules-publish-allowlist.txt.
#   generate-modules-to-skip.sh --docs       Modules to skip for javadoc generation.
#                                            Skips all test/ modules + v2-migration.
set -euo pipefail
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
ALLOWLIST="$SCRIPT_DIR/test-modules-publish-allowlist.txt"

if [[ "$1" == "--docs" ]]; then
  echo "$(ls "$REPO_ROOT/test/" | tr '\n' ',' | sed 's/,$//'),v2-migration"
elif [[ "$1" == "--release" ]]; then
  ls "$REPO_ROOT/test/" | grep -vxFf "$ALLOWLIST" | tr '\n' ',' | sed 's/,$//'
else
  echo "Usage: $0 --release | --docs" >&2
  exit 1
fi
