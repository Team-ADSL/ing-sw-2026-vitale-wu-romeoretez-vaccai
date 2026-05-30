#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

SERVER_JAR="server/target/mesos-server.jar"
CLIENT_JAR="client/target/mesos-client.jar"
SOCKET_PORT=8080
RMI_PORT=1099
SAVE_DIR="./saved"

echo "=== MESOS — GUI Session (CLEAN: wipes saved games) ==="
echo

echo "[0/3] Cleaning previous session..."
# Kill any leftover server so it can't keep games alive in memory or re-save
# .ser files while we delete them.
pkill -f "mesos-server.jar" 2>/dev/null || true
# Delete the saved-games directory (game_<id>.ser recovery files).
if [ -d "$SAVE_DIR" ]; then
  rm -rf "$SAVE_DIR"
  echo "Deleted $SAVE_DIR."
else
  echo "No $SAVE_DIR directory to delete."
fi
echo

echo "[1/3] Building..."
mvn package -DskipTests
if [ $? -ne 0 ]; then
  echo
  echo "BUILD FAILED. Aborting."
  exit 1
fi

if [ ! -f "$SERVER_JAR" ] || [ ! -f "$CLIENT_JAR" ]; then
  echo
  echo "JAR(s) not found after build. Aborting."
  exit 1
fi

echo
echo "[2/3] Starting server (socket=$SOCKET_PORT, rmi=$RMI_PORT)..."
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && source .env && java -jar $SERVER_JAR $SOCKET_PORT $RMI_PORT $SAVE_DIR\""

echo "Waiting for server to start on port $SOCKET_PORT..."
until nc -z 127.0.0.1 "$SOCKET_PORT" 2>/dev/null; do
  sleep 1
done
echo "Server is up."

echo
echo "[3/3] Opening GUI clients..."
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $CLIENT_JAR --client --socket --gui 127.0.0.1 $SOCKET_PORT\""
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $CLIENT_JAR --client --rmi   --gui 127.0.0.1 $RMI_PORT\""

echo
echo "Done."
