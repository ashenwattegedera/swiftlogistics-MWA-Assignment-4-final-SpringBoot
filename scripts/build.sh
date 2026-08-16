#!/usr/bin/env bash
# Build the whole multi-module project and run every test.
set -euo pipefail

cd "$(dirname "$0")/.."

mvn -B clean install "$@"
