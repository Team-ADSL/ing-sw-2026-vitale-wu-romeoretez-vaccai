import json
import socket
import sys

def connect_socket(sock, sock_id, host, port):
    """Connects the socket to the server and checks for an initial banner/response."""
    sock.connect((host, port))
    print(f"Socket {sock_id} connecting to server ({host}:{port})")
    
    # Tries to read a possible welcome message from the server
    sock.settimeout(1.0) 
    try:
        banner = sock.recv(4096)
        if banner:
            print(f"Connection response: {banner.decode('utf-8').strip()}")
    except socket.timeout:
        # No message received on connection (normal for many servers)
        pass
    
    # Reset timeout to infinite for normal read operations
    sock.settimeout(None)
    print("-" * 40)

def receive_response(sock):
    """Reads the response from the server and tries to format it as JSON."""
    try:
        response_bytes = sock.recv(4096)
        if not response_bytes:
            return "Connection closed by the server."
        
        raw_response = response_bytes.decode('utf-8').strip()
        
        # Tries to parse and format nicely if it's a valid JSON
        try:
            resp_json = json.loads(raw_response)
            return json.dumps(resp_json, indent=4)
        except json.JSONDecodeError:
            # If it's not a JSON, return the raw string
            return raw_response
    except Exception as e:
        return f"Error receiving data: {e}"

def send_request(sock, sock_id, request_data):
    """Sends the request to the server and prints the response."""
    try:
        # Formats the request for screen printing
        req_str = json.dumps(request_data, indent=4)
        print(f"Socket {sock_id} sending:\n{req_str}")
        
        # Sends data to the server (adding \n as a payload delimiter)
        payload = json.dumps(request_data) + '\n'
        sock.sendall(payload.encode('utf-8'))
        
        # Waits and prints the response
        response_str = receive_response(sock)
        print(f"Server:\n{response_str}\n")
        print("-" * 40)
        
    except Exception as e:
        print(f"Communication error on socket {sock_id}: {e}")

def infinite_sending(sockets, num_socket, host, port):
    while True:
    try:
        user_input = input("> ")
        if not user_input.strip():
            continue
        
        # Splits the input at the first space
        parts = user_input.split(" ", 1)
        if len(parts) < 2:
            print("Wrong format. Use: <socket_id> <json_request>")
            continue
        
        try:
            sock_id_input = int(parts[0])
        except ValueError:
            print("The socket ID must be an integer.")
            continue
        
        json_str = parts[1]
        
        if sock_id_input not in sockets:
            print(f"Invalid socket {sock_id_input}. Valid sockets are from 1 to {num_socket}.")
            continue
        
        # Validate JSON format before sending
        try:
            request_data_cli = json.loads(json_str)
        except json.JSONDecodeError:
            print("Error: The provided string is not a valid JSON.")
            continue
        
        sock = sockets[sock_id_input]
        
        # Connect on the first use of the socket (if not connected during JSON phase)
        if sock_id_input not in connected_sockets:
            try:
                connect_socket(sock, sock_id_input, host, port)
                connected_sockets.add(sock_id_input)
            except Exception as e:
                print(f"Unable to connect socket {sock_id_input}: {e}")
                continue

        # Send the request and receive the response
        send_request(sock, sock_id_input, request_data_cli)
        
    except KeyboardInterrupt:
        print("\nClosing script and sockets...")
        for s in sockets.values():
            s.close()
        sys.exit(0)

