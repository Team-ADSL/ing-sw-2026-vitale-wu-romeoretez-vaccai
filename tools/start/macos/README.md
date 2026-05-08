# MESOS — macOS Start Scripts

## First-time setup

Make the scripts executable (only needed once):

```bash
chmod +x tools/start/macos/run_tui.sh
chmod +x tools/start/macos/run_gui.sh
```

## Usage

Run from the **project root**:

```bash
./tools/start/macos/run_tui.sh   # TUI clients (terminal)
./tools/start/macos/run_gui.sh   # GUI clients (JavaFX)
```

Or navigate to this folder first:

```bash
cd tools/start/macos
./run_tui.sh
./run_gui.sh
```

## What each script does

1. Builds the project (`mvn package -DskipTests`)
2. Opens a new Terminal window with the server
3. Waits until the server is actually listening on port 8080
4. Opens two more Terminal windows — one Socket client, one RMI client

## Requirements

- Java 17+
- Maven 3.8+
- `nc` (netcat) — pre-installed on macOS
- Terminal.app (used by `osascript` to open new windows)
