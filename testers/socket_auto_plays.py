import argparse
import sys
import time
import json
import threading
import queue

from socket_client_utils import CustomArgumentParser, connect_socket

def listener_thread(sock, player_name, msg_queue):
    """
    Thread in background che legge continuamente dalla socket.
    Usa makefile() per leggere riga per riga ed evitare problemi di pacchetti incollati.
    """
    try:
        # Crea un wrapper testuale sulla socket per leggere fino al '\n'
        with sock.makefile('r', encoding='utf-8') as stream:
            for line in stream:
                line = line.strip()
                if not line:
                    continue
                
                try:
                    data = json.loads(line)
                    msg_type = data.get("type")
                    
                    # Gestione trasparente del PING
                    if msg_type == "PING":
                        # Ignora silenziosamente (o stampa a scopo di debug)
                        # print(f"[{player_name}] <-- PING ricevuto e scartato.")
                        continue
                    
                    # Per tutti gli altri messaggi, mettili in coda per il Main Thread
                    msg_queue.put(data)
                    
                except json.JSONDecodeError:
                    print(f"[{player_name}] Errore di parsing: ricevuto JSON malformato.")
                    
    except Exception as e:
        print(f"\n[{player_name}] Disconnesso dal server: {e}")
        # Inserisce None in coda per segnalare al Main Thread che la connessione è caduta
        msg_queue.put(None)

def send_data(sock, player_name, data):
    """Invia dati al server senza mettersi in ascolto (che è delegato al thread)."""
    try:
        req_str = json.dumps(data)
        print(f"[{player_name}] --> {req_str}")
        sock.sendall((req_str + '\n').encode('utf-8'))
    except Exception as e:
        print(f"[{player_name}] Errore di invio: {e}")

def wait_for_move_response(player_name, q):
    """
    Legge dalla coda finché non trova l'esito della mossa.
    Ritorna True se GAME_UPDATE, False se ERROR_RESPONSE.
    """
    while True:
        try:
            # Blocca finché non c'è un messaggio in coda (con timeout per sicurezza)
            resp = q.get(timeout=15)
            
            if resp is None:
                print(f"[{player_name}] Connessione terminata inaspettatamente.")
                sys.exit(1)
                
            msg_type = resp.get("type")
            
            if msg_type == "ERROR_RESPONSE":
                print(f"[{player_name}] <-- ERROR_RESPONSE ricevuto.")
                return False
            elif msg_type == "GAME_UPDATE":
                print(f"[{player_name}] <-- GAME_UPDATE ricevuto! Mossa valida.")
                return True
            else:
                # Ignora altri messaggi non pertinenti alla mossa (es. notifiche globali)
                pass
                
        except queue.Empty:
            print(f"[{player_name}] Timeout: il server non ha risposto alla mossa.")
            sys.exit(1)

def main():
    example_usage = "python3 socket_auto_plays.py -g 123 -p1 Alice -p2 Bob -r 5"
    parser = CustomArgumentParser(example_usage, description="Auto-player for 2-player game.")
    
    parser.add_argument("-g", dest="gameId", type=int, required=True, help="Game ID to join")
    parser.add_argument("-p1", type=str, required=True, help="Name of Player 1")
    parser.add_argument("-p2", type=str, required=True, help="Name of Player 2")
    parser.add_argument("-r", dest="round_to_reach", type=int, required=True, help="Target round to reach")
    
    parser.add_argument("-H", "--host", type=str, default="127.0.0.1", help="Server IP (default: 127.0.0.1)")
    parser.add_argument("-P", "--port", type=int, default=8080, help="Server port (default: 8080)")

    args = parser.parse_args()

    print("=" * 50)
    print(f"Avvio automazione - Game ID: {args.gameId} | Round target: {args.round_to_reach}")
    print("=" * 50)

    # 1. Connessione
    sock1 = connect_socket(args.host, args.port, "p1")
    sock2 = connect_socket(args.host, args.port, "p2")

    if not sock1 or not sock2:
        print("Impossibile connettere entrambi i giocatori. Uscita.")
        sys.exit(1)

    # 2. Setup Code (Queue) e Threads
    queue_p1 = queue.Queue()
    queue_p2 = queue.Queue()

    thread_p1 = threading.Thread(target=listener_thread, args=(sock1, "p1", queue_p1), daemon=True)
    thread_p2 = threading.Thread(target=listener_thread, args=(sock2, "p2", queue_p2), daemon=True)
    
    thread_p1.start()
    thread_p2.start()

    # 3. Fase Iniziale: Login ed Enter Game
    send_data(sock1, "p1", {"type": "LOGIN", "username": args.p1})
    send_data(sock1, "p1", {"type": "ENTER_GAME", "gameId": args.gameId})

    send_data(sock2, "p2", {"type": "LOGIN", "username": args.p2})
    send_data(sock2, "p2", {"type": "ENTER_GAME", "gameId": args.gameId})

    # Diamo al server il tempo di processare l'entrata in partita
    time.sleep(1)

    # Svuotiamo le code da eventuali messaggi di risposta di Login/EnterGame accumulati
    while not queue_p1.empty(): queue_p1.get()
    while not queue_p2.empty(): queue_p2.get()

    # 4. Ciclo della Partita
    current_round = 0
    
    while current_round != args.round_to_reach:
        print(f"\n--- Inizio Round {current_round} ---")

        # Player 1 e 2 mandano le loro OFFER iniziali
        send_data(sock1, "p1", {"type": "MOVE", "moves": [{"rowIndex": 0, "row": "OFFER"}]})
        wait_for_move_response("p1", queue_p1)
        send_data(sock2, "p2", {"type": "MOVE", "moves": [{"rowIndex": 1, "row": "OFFER"}]})
        wait_for_move_response("p2", queue_p2)

        time.sleep(1)

        while not queue_p1.empty(): queue_p1.get()
        while not queue_p2.empty(): queue_p2.get()

        # --- Player 1: Loop LOWER ---
        i_p1 = 0
        while True:
            move_payload = {"type": "MOVE", "moves": [{"rowIndex": i_p1, "row": "LOWER"}]}
            send_data(sock1, "p1", move_payload)
            
            success = wait_for_move_response("p1", queue_p1)
            if success:
                break # Usciamo dal loop se riceviamo GAME_UPDATE
            else:
                i_p1 += 1 # ERROR_RESPONSE, proviamo il prossimo indice
                time.sleep(0.5)

        # --- Player 2: Loop UPPER ---
        while not queue_p1.empty(): queue_p1.get()
        while not queue_p2.empty(): queue_p2.get()
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

        while not queue_p1.empty(): queue_p1.get()
        while not queue_p2.empty(): queue_p2.get()

        print(f">>> Round {current_round} completato con successo.")
        current_round += 1

    print("=" * 50)
    print(f"Target round {args.round_to_reach} raggiunto. Chiusura script.")
    
    sock1.close()
    sock2.close()

if __name__ == "__main__":
    main()
