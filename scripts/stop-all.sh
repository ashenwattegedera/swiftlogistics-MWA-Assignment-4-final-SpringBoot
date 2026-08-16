#!/usr/bin/env bash
# Stops every service started by scripts/run-all.sh.
set -uo pipefail

LOGDIR="${TMPDIR:-/tmp}/swifttrack-logs"

if [ ! -d "$LOGDIR" ]; then
  echo "No service PIDs found (nothing to stop)."
  exit 0
fi

for pidfile in "$LOGDIR"/*.pid; do
  [ -f "$pidfile" ] || continue
  name="$(basename "$pidfile" .pid)"
  pid="$(cat "$pidfile")"
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null && echo "Stopped $name (PID $pid)"
  else
    echo "$name already stopped"
  fi
  rm -f "$pidfile"
done
