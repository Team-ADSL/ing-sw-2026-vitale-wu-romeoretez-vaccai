#!/usr/bin/env python3
"""
extract_pdf_images.py — Estrae tutte le immagini embedded da un PDF alla risoluzione nativa.

Requisito:
    pip install pymupdf

Uso:
    python extract_pdf_images.py file.pdf
    python extract_pdf_images.py file.pdf --out cartella_output
    python extract_pdf_images.py file.pdf --page 3          # solo pagina 3
    python extract_pdf_images.py file.pdf --min-size 100   # ignora immagini < 100x100 px
"""

import sys
import argparse
from pathlib import Path

try:
    import fitz  # PyMuPDF
except ImportError:
    print("Errore: PyMuPDF non installato.")
    print("Installa con:  pip install pymupdf")
    sys.exit(1)


def extract_images(pdf_path: Path, out_dir: Path, only_page: int | None, min_size: int) -> None:
    doc = fitz.open(pdf_path)
    out_dir.mkdir(parents=True, exist_ok=True)

    saved = 0
    skipped = 0
    seen_xrefs = set()

    pages = range(only_page - 1, only_page) if only_page else range(len(doc))

    for page_idx in pages:
        page = doc[page_idx]
        page_num = page_idx + 1
        img_list = page.get_images(full=True)

        if not img_list:
            print(f"  Pag. {page_num}: nessuna immagine")
            continue

        print(f"  Pag. {page_num}: {len(img_list)} immagini trovate")

        for img_idx, img_info in enumerate(img_list):
            xref = img_info[0]

            # Evita duplicati (stessa immagine su più pagine)
            if xref in seen_xrefs:
                print(f"    IMG {img_idx+1}: duplicato, salto")
                skipped += 1
                continue
            seen_xrefs.add(xref)

            try:
                base_img = doc.extract_image(xref)
                img_bytes = base_img["image"]
                width     = base_img["width"]
                height    = base_img["height"]
                colorspace = base_img.get("colorspace", "?")

                if width < min_size or height < min_size:
                    print(f"    IMG {img_idx+1}: {width}×{height} px — troppo piccola, salto")
                    skipped += 1
                    continue

                # Salva sempre in PNG per qualità massima
                out_path = out_dir / f"img_p{page_num}_{img_idx+1}_{width}x{height}.png"

                # Se l'immagine è già PNG, scrivi direttamente
                if base_img["ext"] == "png":
                    out_path.write_bytes(img_bytes)
                else:
                    # Converti in PNG tramite pixmap per qualità senza perdita
                    pix = fitz.Pixmap(doc, xref)
                    if pix.alpha:
                        pix = fitz.Pixmap(fitz.csRGB, pix)  # rimuovi alpha se CMYK ecc.
                    pix.save(str(out_path))

                print(f"    IMG {img_idx+1}: {width}×{height} px ({colorspace}) → {out_path.name}")
                saved += 1

            except Exception as e:
                print(f"    IMG {img_idx+1}: errore — {e}")
                skipped += 1

    doc.close()
    print()
    print("─" * 50)
    print(f"Completato: {saved} immagini salvate in '{out_dir}'")
    if skipped:
        print(f"Saltate: {skipped} (duplicati o troppo piccole)")


def main() -> None:
    parser = argparse.ArgumentParser(description="Estrae immagini embedded da un PDF.")
    parser.add_argument("pdf", help="Percorso al file PDF")
    parser.add_argument("--out", default=None, help="Cartella di output (default: <nome_pdf>_images/)")
    parser.add_argument("--page", type=int, default=None, help="Estrai solo questa pagina")
    parser.add_argument("--min-size", type=int, default=32, help="Dimensione minima in px (default: 32)")
    args = parser.parse_args()

    pdf_path = Path(args.pdf).resolve()
    if not pdf_path.exists():
        print(f"Errore: file '{pdf_path}' non trovato.")
        sys.exit(1)

    out_dir = Path(args.out).resolve() if args.out else pdf_path.parent / (pdf_path.stem + "_images")

    print(f"PDF    : {pdf_path}")
    print(f"Output : {out_dir}")
    if args.page:
        print(f"Pagina : {args.page}")
    print(f"Min px : {args.min_size}×{args.min_size}")
    print()

    extract_images(pdf_path, out_dir, args.page, args.min_size)


if __name__ == "__main__":
    main()
