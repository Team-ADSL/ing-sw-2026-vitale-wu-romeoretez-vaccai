# MESOS — macOS Start Scripts

## Usage

**From IntelliJ:** right-click the script in the Project panel → **Run**.

Alternatively, from the IntelliJ terminal:

```bash
./tools/start/macos/run_tui.sh          # build + TUI session
./tools/start/macos/run_gui.sh          # build + GUI session
./tools/start/macos/run_tui_nobuild.sh  # TUI session (skip build)
./tools/start/macos/run_gui_nobuild.sh  # GUI session (skip build)
```

> If the terminal returns `Permission denied`, run once:
> ```bash
> chmod +x tools/start/macos/run_tui.sh tools/start/macos/run_tui_nobuild.sh
> chmod +x tools/start/macos/run_gui.sh tools/start/macos/run_gui_nobuild.sh
> ```

## What each script does

**`run_tui.sh` / `run_gui.sh`**
1. Builds the multi-module project with `mvn package -DskipTests` (produces `server/target/mesos-server.jar` and `client/target/mesos-client.jar`)
2. Opens a new Terminal window running the server (socket port 8080, RMI port 1099, saves to `./saved`)
3. Waits until the server is actually listening on port 8080
4. Opens two more Terminal windows: one Socket client, one RMI client (TUI or GUI depending on the script)

**`run_tui_nobuild.sh` / `run_gui_nobuild.sh`**
Same as above but skips the Maven build step — useful when the JARs are already up to date. Fails immediately if `server/target/mesos-server.jar` or `client/target/mesos-client.jar` is missing.

## Requirements

- Java 17+
- Maven 3.8+
- `nc` (netcat) — pre-installed on macOS
- Terminal.app (used by `osascript` to open new windows)
