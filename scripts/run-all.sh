#!/usr/bin/env bash
# Starts all four services in the background and prints the entry points.
# The middleware runs an embedded AMQP broker by default, so RabbitMQ/Docker is NOT required.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGDIR="${TMPDIR:-/tmp}/swifttrack-logs"
mkdir -p "$LOGDIR"

start_service() {
  local name="$1" jar="$2"
  if [ ! -f "$jar" ]; then
    echo "ERROR: jar not found: $jar - run scripts/build.sh first" >&2
    exit 1
  fi
  java -jar "$jar" >"$LOGDIR/$name.out.log" 2>"$LOGDIR/$name.err.log" &
  echo $! >"$LOGDIR/$name.pid"
  echo "Started $name (PID $(cat "$LOGDIR/$name.pid"))"
}

start_service wms "$ROOT/wms-service/target/wms-service-1.0.0-SNAPSHOT.jar"
start_service ros "$ROOT/ros-service/target/ros-service-1.0.0-SNAPSHOT.jar"
start_service cms "$ROOT/cms-service/target/cms-service-1.0.0-SNAPSHOT.jar"
sleep 4
start_service middleware "$ROOT/swifttrack-middleware/target/swifttrack-middleware-1.0.0-SNAPSHOT.jar"

echo ""
echo "Portal:   http://localhost:8080"
echo "Driver:   http://localhost:8080/driver.html"
echo "CMS WSDL: http://localhost:8081/ws/cms.wsdl"
echo "Logs:     $LOGDIR"
echo "Stop:     scripts/stop-all.sh"
