#!/usr/bin/env bash
# End-to-end smoke test: builds (if needed), starts all four services, submits an order,
# waits for the saga to reach ROUTE_ASSIGNED against the real CMS (SOAP), ROS (REST) and
# WMS (TCP), then has the driver mark it delivered.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Prefer python3 (Linux/macOS); fall back to python (Windows).
if command -v python3 >/dev/null 2>&1; then
  PY=python3
elif command -v python >/dev/null 2>&1; then
  PY=python
else
  echo "ERROR: python3 is required" >&2
  exit 1
fi

MW_JAR="$ROOT/swifttrack-middleware/target/swifttrack-middleware-1.0.0-SNAPSHOT.jar"
if [ ! -f "$MW_JAR" ]; then
  echo "Jars not found - building (skipping tests)..."
  mvn -B -q -DskipTests package
fi

LOGDIR="${TMPDIR:-/tmp}/swifttrack-logs"
mkdir -p "$LOGDIR"
PIDS=()

start_service() {
  local name="$1" jar="$2"
  java -jar "$jar" >"$LOGDIR/$name.out.log" 2>"$LOGDIR/$name.err.log" &
  PIDS+=("$!")
  echo "Started $name (PID ${PIDS[-1]})"
}

wait_port() {
  local port="$1" name="$2" i
  for i in $(seq 1 80); do
    if "$PY" -c "import socket,sys; s=socket.socket(); s.settimeout(1); s.connect(('127.0.0.1', int(sys.argv[1]))); s.close()" "$port" 2>/dev/null; then
      echo "$name ready (port $port)"
      return 0
    fi
    sleep 0.5
  done
  echo "ERROR: $name did not start on port $port" >&2
  return 1
}

cleanup() {
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
}
trap cleanup EXIT

json_field() {
  "$PY" -c 'import sys,json; print(json.load(sys.stdin)[sys.argv[1]])' "$1"
}

start_service wms "$ROOT/wms-service/target/wms-service-1.0.0-SNAPSHOT.jar"
start_service ros "$ROOT/ros-service/target/ros-service-1.0.0-SNAPSHOT.jar"
start_service cms "$ROOT/cms-service/target/cms-service-1.0.0-SNAPSHOT.jar"
sleep 4
start_service middleware "$MW_JAR"

wait_port 9090 "WMS"
wait_port 8082 "ROS"
wait_port 8081 "CMS"
wait_port 8080 "Middleware"
sleep 3

ACK="$(curl -s -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"C-100","clientReference":"REF-CI","recipient":{"name":"Anura Silva","phone":"0771234567"},"address":{"street":"42 Galle Road","city":"Colombo 03","postalCode":"00300","latitude":6.9170,"longitude":79.8500},"items":[{"sku":"SKU-1","description":"Ceylon Tea","quantity":2}]}')"

ORDER_ID="$(printf '%s' "$ACK" | json_field orderId)"
echo "Order accepted: $ORDER_ID"

STATUS=""
for i in $(seq 1 60); do
  STATUS="$(curl -s "http://localhost:8080/api/orders/$ORDER_ID" | json_field status)"
  echo "  status: $STATUS"
  if [ "$STATUS" = "ROUTE_ASSIGNED" ]; then break; fi
  if [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "CANCELLED" ]; then
    echo "ERROR: order failed at $STATUS" >&2
    exit 1
  fi
  sleep 1
done

if [ "$STATUS" != "ROUTE_ASSIGNED" ]; then
  echo "ERROR: order did not reach ROUTE_ASSIGNED (last: $STATUS)" >&2
  exit 1
fi

curl -s -X POST "http://localhost:8080/api/deliveries/$ORDER_ID/deliver" >/dev/null
sleep 1
FINAL="$(curl -s "http://localhost:8080/api/orders/$ORDER_ID" | json_field status)"
echo "Final status: $FINAL"

if [ "$FINAL" != "DELIVERED" ]; then
  echo "ERROR: expected DELIVERED, got $FINAL" >&2
  exit 1
fi

echo "SMOKE TEST PASSED"
