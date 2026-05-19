import argparse
import sys
import time
import json
import threading
import queue

from socket_client_utils import CustomArgumentParser, connect_socket

def listener_thread(sock, player_name, msg_queue):
    """
    Background thread that continuously reads from the socket.
    Uses makefile() to read line by line and avoid issues with concatenated packets.
    """
    try:
        # Create a text wrapper on the socket to read up to '\n'
        with sock.makefile('r', encoding='utf-8') as stream:
            for line in stream:
                line = line.strip()
                if not line:
                    continue

                try:
                    data = json.loads(line)
                    msg_type = data.get("type")

                    # Transparent handling of PING
                    if msg_type == "PING":
                        # Silently ignore (or print for debug purposes)
                        # print(f"[{player_name}] <-- PING received and discarded.")
                        continue

                    # For all other messages, put them in the queue for the Main Thread
                    msg_queue.put(data)

                except json.JSONDecodeError:
                    print(f"[{player_name}] Parsing error: received malformed JSON.")

    except Exception as e:
        print(f"\n[{player_name}] Disconnected from server: {e}")
        # Insert None in the queue to signal to the Main Thread that the connection dropped
        msg_queue.put(None)

def send_data(sock, player_name, data):
    """Sends data to the server without listening (which is delegated to the thread)."""
    try:
        req_str = json.dumps(data)
        print(f"[{player_name}] --> {req_str}")
        sock.sendall((req_str + '\n').encode('utf-8'))
    except Exception as e:
        print(f"[{player_name}] Send error: {e}")

def wait_for_move_response(player_name, q):
    """
    Reads from the queue until it finds the outcome of the move.
    Returns True if GAME_UPDATE, False if ERROR_RESPONSE.
    """
    while True:
        try:
            # Block until a message is in the queue (with timeout for safety)
            resp = q.get(timeout=15)

            if resp is None:
                print(f"[{player_name}] Connection terminated unexpectedly.")
                sys.exit(1)

            msg_type = resp.get("type")

            if msg_type == "ERROR_RESPONSE":
                print(f"[{player_name}] <-- ERROR_RESPONSE received.")
                return False
            elif msg_type == "GAME_UPDATE":
                print(f"[{player_name}] <-- GAME_UPDATE received! Valid move.")
                return True
            else:
                # Ignore other messages not related to the move (e.g. global notifications)
                pass

        except queue.Empty:
            print(f"[{player_name}] Timeout: server did not respond to the move.")
            sys.exit(1)

def main():
    example_usage = "python3 socket_auto_plays.py -p1 Alice -p2 Bob -r 5"
    parser = CustomArgumentParser(example_usage, description="Auto-player for 2-player game.")
    
    parser.add_argument("-p1", type=str, required=True, help="Name of Player 1")
    parser.add_argument("-p2", type=str, required=True, help="Name of Player 2")
    parser.add_argument("-r", dest="round_to_reach", type=int, required=True, help="Number of rounds to advance from the current round")
    
    parser.add_argument("-H", "--host", type=str, default="127.0.0.1", help="Server IP (default: 127.0.0.1)")
    parser.add_argument("-P", "--port", type=int, default=8080, help="Server port (default: 8080)")

    args = parser.parse_args()

    print("=" * 50)
    print(f"Starting automation - Round target: {args.round_to_reach}")
    print("=" * 50)

    # 1. Connection
    sock1 = connect_socket(args.host, args.port, "p1")
    sock2 = connect_socket(args.host, args.port, "p2")

    if not sock1 or not sock2:
        print("Unable to connect both players. Exiting.")
        sys.exit(1)

    # 2. Setup Queues and Threads
    queue_p1 = queue.Queue()
    queue_p2 = queue.Queue()

    thread_p1 = threading.Thread(target=listener_thread, args=(sock1, "p1", queue_p1), daemon=True)
    thread_p2 = threading.Thread(target=listener_thread, args=(sock2, "p2", queue_p2), daemon=True)
    
    thread_p1.start()
    thread_p2.start()

    # 3. Initial Phase: Login and Enter Game
    send_data(sock1, "p1", {"type": "LOGIN", "username": args.p1})
    send_data(sock2, "p2", {"type": "LOGIN", "username": args.p2})

    # Give the server time to process the game entry
    time.sleep(1)

    # Flush the queues of any accumulated Login/EnterGame response messages
    while not queue_p1.empty(): queue_p1.get(timeout=3)
    while not queue_p2.empty(): queue_p2.get(timeout=3)

    # 4. Game Loop
    current_round = 0
    
    while current_round != args.round_to_reach:
        print(f"\n--- Start Round {current_round} ---")

        # Player 1 and 2 send their initial OFFERs
        send_data(sock1, "p1", {"type": "MOVE", "moves": [{"rowIndex": 0, "row": "OFFER"}]})
        wait_for_move_response("p1", queue_p1)
        send_data(sock2, "p2", {"type": "MOVE", "moves": [{"rowIndex": 1, "row": "OFFER"}]})
        wait_for_move_response("p2", queue_p2)

        time.sleep(1)

        while not queue_p1.empty(): queue_p1.get(timeout=3)
        while not queue_p2.empty(): queue_p2.get(timeout=3)

        # --- Player 1: LOWER loop ---
        i_p1 = 0
        while True:
            move_payload = {"type": "MOVE", "moves": [{"rowIndex": i_p1, "row": "LOWER"}]}
            send_data(sock1, "p1", move_payload)

            success = wait_for_move_response("p1", queue_p1)
            if success:
                break  # Exit the loop if we receive GAME_UPDATE
            else:
                i_p1 += 1  # ERROR_RESPONSE, try the next index
                time.sleep(0.5)

        # --- Player 2: UPPER loop ---
        while not queue_p1.empty(): queue_p1.get(timeout=3)
        while not queue_p2.empty(): queue_p2.get(timeout=3)
        i_p2 = 0
        while True:
            move_payload = {"type": "MOVE", "moves": [{"rowIndex": i_p2, "row": "UPPER"}]}
            send_data(sock2, "p2", move_payload)
            
            success = wait_for_move_response("p2", queue_p2)
            if success:
                break
            else:
                i_p2 += 1
                time.sleep(0.5)

        while not queue_p1.empty(): queue_p1.get(timeout=3)
        while not queue_p2.empty(): queue_p2.get(timeout=3)

        print(f">>> Round {current_round} completed successfully.")
        current_round += 1

    print("=" * 50)
    print(f"Target round {args.round_to_reach} reached. Closing script.")
    
    sock1.close()
    sock2.close()

if __name__ == "__main__":
    main()
