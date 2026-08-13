#!/usr/bin/env python3
"""Overlay a labelled coordinate grid on a render, to author clip regions."""
import sys
from PIL import Image, ImageDraw

src = sys.argv[1]
dst = sys.argv[2]
step = int(sys.argv[3]) if len(sys.argv) > 3 else 50
zoom = float(sys.argv[4]) if len(sys.argv) > 4 else 1.0

im = Image.open(src).convert("RGBA")
bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
bg.alpha_composite(im)
im = bg
if zoom != 1.0:
    im = im.resize((int(im.width * zoom), int(im.height * zoom)), Image.LANCZOS)
d = ImageDraw.Draw(im)
w, h = im.size
n = 0
for x in range(0, int(w / zoom) + 1, step):
    X = x * zoom
    major = (x % (step * 2) == 0)
    d.line([(X, 0), (X, h)], fill=(255, 0, 0, 200) if major else (0, 150, 255, 90), width=1)
    if major:
        d.text((X + 2, 2), str(x), fill=(200, 0, 0, 255))
for y in range(0, int(h / zoom) + 1, step):
    Y = y * zoom
    major = (y % (step * 2) == 0)
    d.line([(0, Y), (w, Y)], fill=(255, 0, 0, 200) if major else (0, 150, 255, 90), width=1)
    if major:
        d.text((2, Y + 2), str(y), fill=(200, 0, 0, 255))
im.convert("RGB").save(dst)
print("->", dst, im.size)
