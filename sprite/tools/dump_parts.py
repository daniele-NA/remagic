#!/usr/bin/env python3
"""Render a contact sheet highlighting each individual path of the SVG.

Each tile shows the full magician in light gray with one path in magenta, so
the paths can be visually assigned to body parts (hat, beard, arm, boot, ...).
Also prints the bounding box of every path.
"""
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw

SVG_NS = "http://www.w3.org/2000/svg"
ET.register_namespace("", SVG_NS)

SRC = sys.argv[1] if len(sys.argv) > 1 else "sprite/svg/magician_stand.svg"
OUT = sys.argv[2] if len(sys.argv) > 2 else "sprite/qa/parts_sheet.png"
WORK = "sprite/qa/_parts"
TILE = 200
COLS = 7

os.makedirs(WORK, exist_ok=True)


def paths_of(tree):
    return tree.getroot().iter(f"{{{SVG_NS}}}path")


def render(svg_text, dst, size):
    p = subprocess.run(
        ["rsvg-convert", "-w", str(size), "-h", str(size), "-o", dst],
        input=svg_text.encode(),
        capture_output=True,
    )
    if p.returncode:
        raise RuntimeError(p.stderr.decode())


def bbox_of_alpha(img):
    return img.getchannel("A").getbbox()


def main():
    tree = ET.parse(SRC)
    plist = list(paths_of(tree))
    n = len(plist)
    print(f"{n} paths")

    tiles = []
    for i in range(n):
        t2 = ET.parse(SRC)
        pl2 = list(paths_of(t2))
        for j, p in enumerate(pl2):
            if j == i:
                p.set("fill", "#FF00AA")
                p.set("stroke", "none")
            else:
                p.set("fill", "#DDDDDD")
                p.set("stroke", "none")
        svg = ET.tostring(t2.getroot(), encoding="unicode")
        dst = f"{WORK}/p{i:02d}.png"
        render(svg, dst, TILE)
        tiles.append(dst)

        # isolated render for bbox measurement (at viewport resolution)
        t3 = ET.parse(SRC)
        pl3 = list(paths_of(t3))
        for j, p in enumerate(pl3):
            if j != i:
                p.set("fill", "none")
                p.set("stroke", "none")
        svg3 = ET.tostring(t3.getroot(), encoding="unicode")
        iso = f"{WORK}/iso{i:02d}.png"
        render(svg3, iso, 512)
        bb = bbox_of_alpha(Image.open(iso).convert("RGBA"))
        fill = pl3[i].get("fill")
        pid = pl3[i].get("id")
        area = (bb[2] - bb[0]) * (bb[3] - bb[1]) if bb else 0
        print(f"{pid:>4} fill={fill} bbox={bb} area={area}")

    rows = (n + COLS - 1) // COLS
    sheet = Image.new("RGBA", (COLS * TILE, rows * (TILE + 18)), (255, 255, 255, 255))
    d = ImageDraw.Draw(sheet)
    for i, t in enumerate(tiles):
        x = (i % COLS) * TILE
        y = (i // COLS) * (TILE + 18)
        sheet.alpha_composite(Image.open(t).convert("RGBA"), (x, y))
        d.text((x + 6, y + TILE + 3), f"p{i:02d}", fill=(0, 0, 0, 255))
    sheet.save(OUT)
    print("sheet ->", OUT, sheet.size)


main()
