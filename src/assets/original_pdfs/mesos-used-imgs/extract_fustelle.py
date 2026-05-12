#!/usr/bin/env python3
"""
extract_fustelle.py — Estrae le 14 immagini uniche da fustelle_front.pdf.

Gestisce:
  - CMYK e Indexed(CMYK) → RGB/RGBA
  - Soft mask (has-mask) → canale alpha nel PNG di output
  - Qualità nativa: nessun ridimensionamento

Output: cartella fustelle_images/ accanto al PDF.

Uso:
    py extract_fustelle.py
    py extract_fustelle.py --out /altra/cartella
    py extract_fustelle.py --xrefs 59 61 63   # solo questi xref
"""
import sys
import argparse
from pathlib import Path
import fitz

from PIL import Image as PILImage


def pix_to_pil(pix: fitz.Pixmap) -> PILImage.Image:
    """Converte Pixmap RGB/Gray in PIL Image."""
    if pix.n == 1:
        return PILImage.frombytes('L', (pix.width, pix.height), pix.samples)
    return PILImage.frombytes('RGB', (pix.width, pix.height), pix.samples)


def to_rgb_pix(pix: fitz.Pixmap) -> fitz.Pixmap:
    """Converte CMYK / Indexed / qualsiasi colorspace in sRGB."""
    cs = pix.colorspace
    if cs is None or cs == fitz.csRGB:
        return pix
    return fitz.Pixmap(fitz.csRGB, pix)


def extract_all(pdf_path: Path, out_dir: Path, only_xrefs: set | None) -> None:
    doc = fitz.open(pdf_path)
    out_dir.mkdir(parents=True, exist_ok=True)

    seen   = set()
    saved  = 0
    failed = 0

    for pi in range(len(doc)):
        page     = doc[pi]
        page_num = pi + 1
        infos    = page.get_image_info(xrefs=True)

        for info in infos:
            xref = info['xref']
            if xref in seen:
                continue
            seen.add(xref)

            if only_xrefs and xref not in only_xrefs:
                continue

            w        = info['width']
            h        = info['height']
            has_mask = info['has-mask']
            cs_name  = info['cs-name']

            try:
                # Colore: CMYK/Indexed → RGB
                pix_color = fitz.Pixmap(doc, xref)
                pix_color = to_rgb_pix(pix_color)
                img = pix_to_pil(pix_color)

                # Soft mask → canale alpha
                alpha_tag = ''
                if has_mask:
                    base = doc.extract_image(xref)
                    smask_xref = base.get('smask', 0)
                    if smask_xref > 0:
                        pix_mask = fitz.Pixmap(doc, smask_xref)
                        if pix_mask.colorspace != fitz.csGRAY:
                            pix_mask = fitz.Pixmap(fitz.csGRAY, pix_mask)
                        mask = pix_to_pil(pix_mask)
                        # Ridimensiona la maschera se differisce (raro)
                        if mask.size != img.size:
                            mask = mask.resize(img.size, PILImage.LANCZOS)
                        img = img.convert('RGBA')
                        img.putalpha(mask)
                        alpha_tag = ' +alpha'

                out_name = f'p{page_num}_xref{xref}_{w}x{h}.png'
                out_path = out_dir / out_name
                img.save(str(out_path), optimize=True)

                print(f'  xref={xref:4d}  {w}x{h}  {cs_name}{alpha_tag}  ->  {out_name}')
                saved += 1

            except Exception as exc:
                print(f'  xref={xref:4d}  ERRORE: {exc}')
                failed += 1

    doc.close()
    print()
    print(f'Completato: {saved} immagini salvate in "{out_dir}"')
    if failed:
        print(f'Errori: {failed}')


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--out',   default=None, help='Cartella di output')
    parser.add_argument('--xrefs', nargs='*', type=int, default=None,
                        help='Estrai solo questi xref (es. --xrefs 59 61)')
    args = parser.parse_args()

    pdf_path = Path(__file__).parent / 'fustelle_front.pdf'
    out_dir  = Path(args.out).resolve() if args.out else pdf_path.parent / 'fustelle_images'
    only_xrefs = set(args.xrefs) if args.xrefs else None

    print(f'PDF    : {pdf_path}')
    print(f'Output : {out_dir}')
    if only_xrefs:
        print(f'xrefs  : {sorted(only_xrefs)}')
    print()

    extract_all(pdf_path, out_dir, only_xrefs)


if __name__ == '__main__':
    main()
