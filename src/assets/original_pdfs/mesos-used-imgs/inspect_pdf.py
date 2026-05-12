import fitz
from pathlib import Path

pdf = Path(__file__).parent / 'fustelle_front.pdf'
doc = fitz.open(pdf)
print(f'Pagine: {len(doc)}  —  pagina size: {doc[0].rect}')
print()

unique_xrefs = {}   # xref -> info del primo piazzamento
clipped = []

for pi in range(len(doc)):
    page    = doc[pi]
    page_num = pi + 1
    infos   = page.get_image_info(xrefs=True)

    for info in infos:
        xref  = info['xref']
        w, h  = info['width'], info['height']
        bbox  = fitz.Rect(info['bbox'])
        tr    = info['transform']          # (a, b, c, d, e, f)
        a, b, c, d, e, f = tr

        # Rettangolo pieno (senza clip) in coordinate pagina
        full = fitz.Rect(e, f, e + a, f + d)   # valido se no rotazione/shear

        dx = abs(bbox.x0 - full.x0) + abs(bbox.x1 - full.x1)
        dy = abs(bbox.y0 - full.y0) + abs(bbox.y1 - full.y1)
        is_clipped = (dx > 0.5 or dy > 0.5)

        if xref not in unique_xrefs:
            unique_xrefs[xref] = {
                'page': page_num, 'w': w, 'h': h,
                'cs': info['cs-name'], 'has_mask': info['has-mask'],
                'size_kb': info['size'] // 1024,
                'tr': tr, 'bbox': bbox, 'full': full,
                'is_clipped': is_clipped,
            }
        if is_clipped:
            clipped.append((page_num, xref, w, h, bbox, full))

print(f'Piazzamenti totali: {sum(len(doc[i].get_image_info(xrefs=True)) for i in range(len(doc)))}')
print(f'Immagini uniche   : {len(unique_xrefs)}')
print(f'Con clip applicato: {len(clipped)}')
print()

print('=== IMMAGINI UNICHE (per xref) ===')
for xref, d in sorted(unique_xrefs.items()):
    clip_tag = '  *** CLIPPED ***' if d['is_clipped'] else ''
    mask_tag = ' [mask]' if d['has_mask'] else ''
    print(f'  xref={xref:4d}  p{d["page"]}  {d["w"]:5d}x{d["h"]:4d}  {d["cs"]:15s}  {d["size_kb"]:5d} KB{mask_tag}{clip_tag}')

if clipped:
    print()
    print('=== DETTAGLIO CLIP ===')
    for page_num, xref, w, h, bbox, full in clipped:
        print(f'  p{page_num} xref={xref}  native={w}x{h}')
        print(f'    full=({full.x0:.1f},{full.y0:.1f},{full.x1:.1f},{full.y1:.1f})')
        print(f'    bbox=({bbox.x0:.1f},{bbox.y0:.1f},{bbox.x1:.1f},{bbox.y1:.1f})')

doc.close()
