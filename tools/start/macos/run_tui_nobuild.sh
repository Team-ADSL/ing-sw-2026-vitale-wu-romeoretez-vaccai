#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

JAR="target/mesos.jar"
SOCKET_PORT=8080
RMI_PORT=1099
SAVE_DIR="./saved"

echo "=== MESOS — TUI Session (no build) ==="
echo

if [ ! -f "$JAR" ]; then
    echo "JAR not found: $JAR. Run run_tui.sh first to build."
    exit 1
fi

echo "[1/2] Starting server (socket=$SOCKET_PORT, rmi=$RMI_PORT)..."
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $JAR --server $SOCKET_PORT $RMI_PORT $SAVE_DIR\""

echo "Waiting for server to start on port $SOCKET_PORT..."
until nc -z 127.0.0.1 "$SOCKET_PORT" 2>/dev/null; do
    sleep 1
done
echo "Server is up."

echo
echo "[2/2] Opening TUI clients..."
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $JAR --client --socket --tui 127.0.0.1 $SOCKET_PORT\""
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $JAR --client --rmi   --tui 127.0.0.1 $RMI_PORT\""

echo
echo "Done."
