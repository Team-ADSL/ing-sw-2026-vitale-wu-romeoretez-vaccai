import argparse
import os
import sys
import subprocess
import socket
import time
import shutil

def is_port_open(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(("127.0.0.1", port)) == 0

def kill_previous_server():
    if sys.platform == "win32":
        subprocess.run('taskkill /FI "WINDOWTITLE eq MESOS Server*" /T /F', shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    else:
        subprocess.run('pkill -f "mesos-server"', shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def clean_save_directory(save_dir):
    if os.path.exists(save_dir):
        try:
            shutil.rmtree(save_dir)
            print(f"[CLEAN] Deleted directory: {save_dir}")
        except Exception as e:
            print(f"[CLEAN] Warning: could not delete {save_dir}: {e}")

def main():
    parser = argparse.ArgumentParser(description="MESOS Launch Session Orchestrator")
    parser.add_argument("--tui", type=int, nargs="?", const=1, default=0,
                        help="Number of TUI clients to launch")
    parser.add_argument("--gui", type=int, nargs="?", const=1, default=0,
                        help="Number of GUI clients to launch")
    parser.add_argument("--clean", action="store_true", help="Kill active servers and delete saved games folder before starting")
    parser.add_argument("--nobuild", action="store_true", help="Skip Maven compilation (mvn package)")
    
    args = parser.parse_args()

    # If no clients are specified, default to 1 TUI and 1 GUI client
    if args.tui == 0 and args.gui == 0:
        args.tui = 1
        args.gui = 1

    num_players = args.tui + args.gui
    
    if num_players < 2 or num_players > 5:
        print(f"\n[ERROR] Total number of clients ({num_players}) must be between 2 and 5.")
        print("Usage examples:")
        print("  run.bat --tui 2 --gui 3      (2 TUI, 3 GUI - total 5)")
        print("  run.bat --tui 3              (3 TUI, 0 GUI - total 3)")
        print("  run.bat                      (0 TUI, 2 GUI - total 2)")
        sys.exit(1)

    # Define paths relative to project root
    # Locate project root (start.py is in tools/start/)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    os.chdir(project_root)

    server_jar = "server/target/mesos-server.jar"
    client_jar = "client/target/mesos-client.jar"
    socket_port = 8080
    rmi_port = 1099
    save_dir = "./saved"

    print("==================================================")
    print(f" MESOS Launcher | TUI: {args.tui} | GUI: {args.gui} | Total Players: {num_players}")
    print("==================================================")

    # 1. Clean previous session
    if args.clean:
        print("\n[0/3] Cleaning previous session...")
        kill_previous_server()
        clean_save_directory(save_dir)

    # 2. Build
    if not args.nobuild:
        print("\n[1/3] Compiling project (mvn package)...")
        mvn_cmd = "mvn.cmd" if sys.platform == "win32" else "mvn"
        result = subprocess.run([mvn_cmd, "package", "-DskipTests"])
        if result.returncode != 0:
            print("\n[ERROR] Maven compilation failed. Aborting.")
            sys.exit(1)
    else:
        print("\n[1/3] Skipping build (--nobuild)...")

    # Verify JARs exist
    if not os.path.exists(server_jar) or not os.path.exists(client_jar):
        print(f"\n[ERROR] Required JARs not found:\n- {server_jar}\n- {client_jar}\nPlease run without --nobuild first.")
        sys.exit(1)

    # 3. Start Server
    print(f"\n[2/3] Starting server (Socket: {socket_port}, RMI: {rmi_port})...")
    if sys.platform == "win32":
        cmd = f'start "MESOS Server" cmd /k "java -jar {server_jar} {socket_port} {rmi_port} {save_dir}"'
        subprocess.Popen(cmd, shell=True)
    else:
        applescript = f'tell application "Terminal" to do script "cd \'{project_root}\' && source .env 2>/dev/null || true; java -jar {server_jar} {socket_port} {rmi_port} {save_dir}"'
        subprocess.run(["osascript", "-e", applescript])

    print("Waiting for server to listen on port 8080...")
    attempts = 0
    while not is_port_open(socket_port):
        time.sleep(0.5)
        attempts += 1
        if attempts > 30:
            print("[ERROR] Server startup timed out. Check the server console.")
            sys.exit(1)
    print("Server is up and running.")

    # 4. Open Clients
    print(f"\n[3/3] Launching {num_players} clients ({args.tui} TUI, {args.gui} GUI)...")
    
    clients = []
    # Add TUI clients first
    for _ in range(args.tui):
        clients.append("tui")
    # Add GUI clients next
    for _ in range(args.gui):
        clients.append("gui")

    for i, mode in enumerate(clients, 1):
        proto = "socket" if i % 2 == 1 else "rmi"
        port = socket_port if proto == "socket" else rmi_port
        print(f"- Spawning Client {i} ({proto.upper()} - {mode.upper()})")
        if sys.platform == "win32":
            cmd = f'start "MESOS {proto.upper()} Client {i} ({mode.upper()})" cmd /k "java -jar {client_jar} --client --{proto} --{mode} 127.0.0.1 {port}"'
            subprocess.Popen(cmd, shell=True)
        else:
            applescript = f'tell application "Terminal" to do script "cd \'{project_root}\' && java -jar {client_jar} --client --{proto} --{mode} 127.0.0.1 {port}"'
            subprocess.run(["osascript", "-e", applescript])

    print("\nAll processes successfully launched!")

if __name__ == "__main__":
    main()
