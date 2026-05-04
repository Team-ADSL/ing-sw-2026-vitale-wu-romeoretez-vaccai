import client_utils
import socket
import json
import argparse
import sys

class CustomArgumentParser(argparse.ArgumentParser):
    """Custom parser to override the default error message behavior."""
    def error(self, message):
        sys.stderr.write(f"Error: {message}\n\n")
        sys.stderr.write("Correct usage example:\n")
        sys.stderr.write("  python socket_sender.py 127.0.0.1 8080 payload.json\n")
        sys.exit(2)

def main():
    parser = CustomArgumentParser(description="JSON Socket Client Tester")
    parser.add_argument("host", help="Server IP address")
    parser.add_argument("port", type=int, help="Server port")
    parser.add_argument("file", help="Path to the input JSON file")
    args = parser.parse_args()

    # 1. Reading the JSON file
    try:
        with open(args.file, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"Error: The file {args.file} does not exist.")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: The file does not contain a valid JSON. Details: {e}")
        sys.exit(1)

    num_socket = data.get("num_socket", 0)
    requests = data.get("requests", [])

    if num_socket <= 0:
        print("Error: num_socket must be greater than 0.")
        sys.exit(1)

    # 2. Sockets creation
    sockets = {}
    connected_sockets = set()
    
    for i in range(1, num_socket + 1):
        sockets[i] = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    
    print(f"Initialized {num_socket} sockets.\n" + "="*40)

    # 3. Iterating over the requests array
    for req in requests:
        sock_id = req.get("socket")
        request_data = req.get("request")

        if sock_id not in sockets:
            print(f"Error: the request specifies socket {sock_id} which does not exist.")
            continue
        
        sock = sockets[sock_id]

        # Connect on the first use of the socket
        if sock_id not in connected_sockets:
            try:
                connect_socket(sock, sock_id, args.host, args.port)
                connected_sockets.add(sock_id)
            except Exception as e:
                print(f"Unable to connect socket {sock_id}: {e}")
                continue

        # Send the request and receive the response
        send_request(sock, sock_id, request_data)

    print("JSON file processing completed. Entering interactive mode.")
    print("Expected format: <socket_id> <json_request> (e.g., 1 {\"action\": \"ping\"})")
    print("Press Ctrl+C to exit.\n" + "="*40)

    # 4. Infinite while loop for command line input
    infinite_sending(sockets, num_socket, args.host, args.port):

if __name__ == "__main__":
    main()
