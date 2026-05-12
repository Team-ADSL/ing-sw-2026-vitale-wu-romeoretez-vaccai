#!/usr/bin/env python3
"""
Ritaglia le 7 offer tile dal panorama unificato (offer_tiles_1 + offer_tile_d + offer_tiles_3).

Centri delle carte nel panorama (4143px):
  A=313  B=880  C=1443  D=2073  E=2698  F=3265  G=3831

Tagli = mezzeria tra centri consecutivi:
  [0, 596, 1161, 1758, 2385, 2981, 3548, 4143]
"""
from PIL import Image
from pathlib import Path

BASE = Path(__file__).parent

STRIPS  = ['offer_tiles_1.png', 'offer_tile_d.png', 'offer_tiles_3.png']
CENTERS = [313, 880, 1443, 2073, 2698, 3265, 3831]
NAMES   = ['offer_tile_a.png', 'offer_tile_b.png', 'offer_tile_c.png',
           'offer_tile_d.png', 'offer_tile_e.png', 'offer_tile_f.png', 'offer_tile_g.png']

# Costruisce il panorama unificato
strips = [Image.open(BASE / s).convert('RGBA') for s in STRIPS]
total_w = sum(i.width for i in strips)
h = strips[0].height
panorama = Image.new('RGBA', (total_w, h))
x = 0
for img in strips:
    panorama.paste(img, (x, 0))
    x += img.width

# Tagli
cuts = [0] + [(CENTERS[i] + CENTERS[i+1]) // 2 for i in range(len(CENTERS)-1)] + [total_w]

for i, name in enumerate(NAMES):
    x0, x1 = cuts[i], cuts[i+1]
    crop = panorama.crop((x0, 0, x1, h))
    crop.save(BASE / name, optimize=True)
    print(f'[{x0}:{x1}]  ->  {name}  ({crop.width}x{h})')

print('Done.')
