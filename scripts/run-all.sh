#!/usr/bin/env bash
# Starts all four services in the background and prints the entry points.
# By default the middleware runs an embedded AMQP broker (no RabbitMQ/Docker required).
# Pass "rabbitmq" as the first argument to use the real RabbitMQ container instead:
#   docker compose up -d rabbitmq
#   bash scripts/run-all.sh rabbitmq
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGDIR="${TMPDIR:-/tmp}/swifttrack-logs"
mkdir -p "$LOGDIR"

start_service() {
  local name="$1" jar="$2"
  shift 2
  if [ ! -f "$jar" ]; then
    echo "ERROR: jar not found: $jar - run scripts/build.sh first" >&2
    exit 1
  fi
  java -jar "$jar" "$@" >"$LOGDIR/$name.out.log" 2>"$LOGDIR/$name.err.log" &
  echo $! >"$LOGDIR/$name.pid"
  echo "Started $name (PID $(cat "$LOGDIR/$name.pid"))"
}

MW_JAR="$ROOT/swifttrack-middleware/target/swifttrack-middleware-1.0.0-SNAPSHOT.jar"

start_service wms "$ROOT/wms-service/target/wms-service-1.0.0-SNAPSHOT.jar"
start_service ros "$ROOT/ros-service/target/ros-service-1.0.0-SNAPSHOT.jar"
start_service cms "$ROOT/cms-service/target/cms-service-1.0.0-SNAPSHOT.jar"
sleep 4
if [ "${1:-}" = "rabbitmq" ]; then
  start_service middleware "$MW_JAR" --spring.profiles.active=rabbitmq
else
  start_service middleware "$MW_JAR"
fi

echo ""
echo "Portal:   http://localhost:8080"
echo "Driver:   http://localhost:8080/driver.html"
echo "CMS WSDL: http://localhost:8081/ws/cms.wsdl"
echo "Logs:     $LOGDIR"
echo "Stop:     scripts/stop-all.sh"
