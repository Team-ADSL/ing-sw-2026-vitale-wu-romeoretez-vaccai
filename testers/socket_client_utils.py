import socket
import json
import sys
import argparse

class CustomArgumentParser(argparse.ArgumentParser):
    """Common parser to handle error messages and usage examples."""
    def __init__(self, example_usage, **kwargs):
        self.example_usage = example_usage
        super().__init__(**kwargs)

    def error(self, message):
        sys.stderr.write(f"Error: {message}\n\n")
        sys.stderr.write("Correct usage example:\n")
        sys.stderr.write(f"  {self.example_usage}\n")
        sys.exit(2)

def connect_socket(host, port, sock_id):
    """Creates, connects and checks for an initial banner."""
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((host, port))
        print(f"Socket {sock_id} connecting to server ({host}:{port})")
        
        sock.settimeout(1.0)
        try:
            banner = sock.recv(4096)
            if banner:
                print(f"Connection response: {banner.decode('utf-8').strip()}")
        except socket.timeout:
            pass
        
        sock.settimeout(None)
        print("-" * 40)
        return sock
    except Exception as e:
        print(f"Unable to connect socket {sock_id}: {e}")
        return None

def receive_json_response(sock):
    """Reads response from server and returns it as a dict (if JSON)."""
    try:
        response_bytes = sock.recv(4096)
        if not response_bytes:
            return None
        
        raw_response = response_bytes.decode('utf-8').strip()
        try:
            return json.loads(raw_response)
        except json.JSONDecodeError:
            return raw_response
    except Exception as e:
        print(f"Error receiving data: {e}")
        return None

def send_json_request(sock, sock_id, data):
    """Sends JSON data and returns the parsed response."""
    try:
        req_str = json.dumps(data, indent=4)
        print(f"Socket {sock_id} sending:\n{req_str}")
        
        payload = json.dumps(data) + '\n'
        sock.sendall(payload.encode('utf-8'))
        
        response = receive_json_response(sock)
        resp_str = json.dumps(response, indent=4) if isinstance(response, dict) else str(response)
        print(f"Server:\n{resp_str}\n")
        print("-" * 40)
        return response
    except Exception as e:
        print(f"Communication error on socket {sock_id}: {e}")
        return None
