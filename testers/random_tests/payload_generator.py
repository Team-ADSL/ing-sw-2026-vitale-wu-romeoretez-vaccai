import json
import random
import string
import argparse
import sys

class CustomArgumentParser(argparse.ArgumentParser):
    """Custom parser to override the default error message behavior."""
    def error(self, message):
        sys.stderr.write(f"Error: {message}\n\n")
        sys.stderr.write("Correct usage example:\n")
        sys.stderr.write("  python payload_generator.py schema.json -n 10 -s 3 -o test_data.json\n")
        sys.exit(2)

def generate_random_string(length=8):
    """Generates a random alphanumeric string."""
    characters = string.ascii_letters + string.digits
    return ''.join(random.choices(characters, k=length))

def generate_random_value(schema_value):
    """Parses the schema value and generates the corresponding random data."""
    
    # 1. If it's a list, we need to decide if it's an ENUM or an ARRAY TEMPLATE
    if isinstance(schema_value, list):
        if not schema_value:
            return [] # Empty list in schema returns an empty list
            
        # If the list has exactly 1 item, treat it as a TEMPLATE for an array
        if len(schema_value) == 1:
            # Generate a random length for the array (e.g., between 1 and 4 items)
            array_length = random.randint(1, 4)
            # Recursively generate N items based on the single template item
            return [generate_random_value(schema_value[0]) for _ in range(array_length)]
            
        # If the list has > 1 items, treat it as an ENUM (pick one)
        return random.choice(schema_value)
    
    # 2. If it's a string, check if it's a specific placeholder
    elif isinstance(schema_value, str):
        if schema_value == "<string>":
            return generate_random_string()
        elif schema_value == "<int>":
            return random.randint(0, 1000) 
        elif schema_value == "<float>":
            return round(random.uniform(0.0, 100.0), 2)
        elif schema_value == "<boolean>":
            return random.choice([True, False])
        else:
            # Normal string fallback
            return schema_value
            
    # 3. If it's a dictionary, parse it recursively
    elif isinstance(schema_value, dict):
        return {k: generate_random_value(v) for k, v in schema_value.items()}
        
    # 4. Fallback for other static types
    return schema_value

def generate_payload(schema_list):
    """Picks a random request template from the schema and populates it."""
    template = random.choice(schema_list)
    
    payload = {}
    for key, value in template.items():
         payload[key] = generate_random_value(value)
         
    return payload

def main():
    parser = CustomArgumentParser(description="Random JSON Payload Fuzzer for Socket Client")
    parser.add_argument("schema_file", help="Path to the JSON schema file")
    parser.add_argument("-n", "--number", type=int, default=5, help="Total number of payloads to generate")
    parser.add_argument("-s", "--sockets", type=int, default=2, help="Number of sockets to simulate")
    parser.add_argument("-o", "--output", default="test_payloads.json", help="Output JSON file name")
    args = parser.parse_args()

    # 1. Load the schema
    try:
        with open(args.schema_file, 'r', encoding='utf-8') as f:
            schema = json.load(f)
            
        if not isinstance(schema, list):
            print("Error: The schema root must be a JSON array [...].")
            sys.exit(1)
            
    except FileNotFoundError:
        print(f"Error: Schema file '{args.schema_file}' not found.")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in schema file. Details: {e}")
        sys.exit(1)

    if args.sockets <= 0 or args.number <= 0:
        print("Error: Both sockets and number of payloads must be greater than 0.")
        sys.exit(1)

    print(f"Generating {args.number} payloads across {args.sockets} sockets...\n" + "="*40)

    # 2. Build the output data structure
    output_data = {
        "num_socket": args.sockets,
        "requests": []
    }
    
    for _ in range(args.number):
        random_payload = generate_payload(schema)
        # Assign the payload randomly to one of the available sockets
        assigned_socket = random.randint(1, args.sockets)
        
        request_entry = {
            "socket": assigned_socket,
            "request": random_payload
        }
        output_data["requests"].append(request_entry)

    # 3. Save to output file
    try:
        with open(args.output, 'w', encoding='utf-8') as out_file:
            json.dump(output_data, out_file, indent=4)
        print(f"Success! Generated file saved as '{args.output}'.")
        print("Ready to be used with the Socket Client Tester.")
        print("=" * 40)
    except Exception as e:
        print(f"Error writing output file: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
