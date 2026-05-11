#!/usr/bin/env bash
# Produce cropped copies of every card PNG by shaving a fixed pixel padding
# from each side. Originals stay untouched under src/assets/cards_{front,back};
# results land under src/assets/cards_{front,back}_cropped/ and are what the
# GUI consumes at runtime.
set -euo pipefail

PAD=61
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ASSETS="$REPO_ROOT/src/assets"

if ! command -v sips >/dev/null; then
    echo "ERROR: sips not found (macOS-only built-in). Install ImageMagick and adapt the script for other platforms." >&2
    exit 1
fi

shave_dir() {
    local src="$1" dst="$2"
    [[ -d "$src" ]] || { echo "SKIP: $src does not exist"; return; }
    mkdir -p "$dst"
    echo "Shaving ${PAD}px from each side: $src -> $dst"
    shopt -s nullglob
    for f in "$src"/*.png; do
        local name w h new_w new_h out
        name="$(basename "$f")"
        out="$dst/$name"
        cp "$f" "$out"
        w=$(sips -g pixelWidth "$out"  | awk '/pixelWidth/  {print $2}')
        h=$(sips -g pixelHeight "$out" | awk '/pixelHeight/ {print $2}')
        new_w=$(( w - 2 * PAD ))
        new_h=$(( h - 2 * PAD ))
        if (( new_w <= 0 || new_h <= 0 )); then
            echo "  SKIP $name: ${w}x${h} too small to shave ${PAD}px each side"
            rm "$out"
            continue
        fi
        sips -c "$new_h" "$new_w" "$out" >/dev/null
        echo "  $name: ${w}x${h} -> ${new_w}x${new_h}"
    done
    shopt -u nullglob
}

shave_dir "$ASSETS/cards_front" "$ASSETS/cards_front_cropped"
shave_dir "$ASSETS/cards_back"  "$ASSETS/cards_back_cropped"
echo "Done."
