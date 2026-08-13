#!/usr/bin/env python3
"""Render every rig part in isolation plus the reassembled figure.

Two checks:
  * each tile shows one part alone -> the cut must follow the anatomy
  * 'REASSEMBLED' must be pixel-identical to the untouched original, proving
    the masks tile the artwork with no gap and no overlap
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(__file__))
import rig

from PIL import Image, ImageChops, ImageDraw

WORK = "sprite/qa/_parts_check"
os.makedirs(WORK, exist_ok=True)
os.makedirs("sprite/qa", exist_ok=True)
TILE = 256


def render(svg, dst, size=TILE):
    p = subprocess.run(
        ["rsvg-convert", "-w", str(size), "-h", str(size), "-o", dst],
        input=svg.encode(), capture_output=True,
    )
    if p.returncode:
        raise RuntimeError(p.stderr.decode())


def doc(body, defs):
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" '
        f'xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{rig.VIEW}" height="{rig.VIEW}" '
        f'viewBox="0 0 {rig.VIEW} {rig.VIEW}">'
        f"<defs>{defs}</defs>{body}</svg>"
    )


def main():
    paths, inner = rig.load_art()
    defs = rig.art_defs(paths, inner) + rig.masks_defs()

    names = [n for n in rig.PRIORITY if n in rig.PARTS]
    tiles = []
    for n in names:
        svg = doc(rig.part_use(n), defs)
        dst = f"{WORK}/{n}.png"
        render(svg, dst)
        tiles.append((n, dst))

    # reassembled, in the priority order reversed so lower priority draws first
    body = "".join(rig.part_use(n) for n in reversed(names))
    render(doc(body, defs), f"{WORK}/_all.png", 1024)
    tiles.append(("REASSEMBLED", f"{WORK}/_all.png"))

    # pixel diff against the untouched original, rendered here rather than read
    # from disk so the reference always matches the SVG being cut up
    pristine = "".join(f'<path d="{d}" fill="{f}"/>' for d, f in paths)
    render(doc(f'<g transform="{inner}">{pristine}</g>', ""), "sprite/qa/base_1024.png", 1024)
    a = Image.open("sprite/qa/base_1024.png").convert("RGBA")
    b = Image.open(f"{WORK}/_all.png").convert("RGBA")
    diff = ImageChops.difference(a, b)
    bbox = diff.convert("L").point(lambda v: 255 if v > 24 else 0).getbbox()
    lost = sum(1 for p in diff.getdata() if max(p) > 24)
    print(f"reassembly diff: {lost} px differ (>24), bbox={bbox}")
    d2 = diff.convert("L").point(lambda v: 255 if v > 24 else 0).convert("RGB")
    d2.save(f"{WORK}/_diff.png")
    tiles.append(("DIFF", f"{WORK}/_diff.png"))

    cols = 5
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * TILE, rows * (TILE + 18)), (250, 250, 252, 255))
    dr = ImageDraw.Draw(sheet)
    for i, (n, t) in enumerate(tiles):
        x, y = (i % cols) * TILE, (i // cols) * (TILE + 18)
        im = Image.open(t).convert("RGBA")
        if im.size != (TILE, TILE):
            im = im.resize((TILE, TILE), Image.LANCZOS)
        sheet.alpha_composite(im, (x, y))
        dr.rectangle([x, y, x + TILE - 1, y + TILE - 1], outline=(200, 200, 210, 255))
        dr.text((x + 5, y + TILE + 3), n, fill=(0, 0, 0, 255))
    sheet.save("sprite/qa/parts_check.png")
    print("-> sprite/qa/parts_check.png")


main()
