#!/usr/bin/env python3
"""Convert an Android VectorDrawable XML into a plain SVG.

Handles the subset used by magician_stand.xml: a <vector> with viewport
dimensions, optional <group> wrappers with translate/scale/rotate/pivot, and
<path> children with fillColor / fillAlpha / strokeColor / strokeWidth.
"""
import os
import sys
import xml.etree.ElementTree as ET

A = "{http://schemas.android.com/apk/res/android}"


def attr(el, name, default=None):
    return el.get(A + name, default)


def group_transform(el):
    tx = float(attr(el, "translateX", 0))
    ty = float(attr(el, "translateY", 0))
    sx = float(attr(el, "scaleX", 1))
    sy = float(attr(el, "scaleY", 1))
    rot = float(attr(el, "rotation", 0))
    px = float(attr(el, "pivotX", 0))
    py = float(attr(el, "pivotY", 0))
    # Android applies: T(tx,ty) T(px,py) R(rot) S(sx,sy) T(-px,-py)
    parts = [f"translate({tx},{ty})"]
    if px or py:
        parts.append(f"translate({px},{py})")
    if rot:
        parts.append(f"rotate({rot})")
    if sx != 1 or sy != 1:
        parts.append(f"scale({sx},{sy})")
    if px or py:
        parts.append(f"translate({-px},{-py})")
    return " ".join(parts)


def path_svg(el, index):
    d = attr(el, "pathData", "")
    fill = attr(el, "fillColor", "none") or "none"
    stroke = attr(el, "strokeColor", "none") or "none"
    sw = attr(el, "strokeWidth", "0")
    fill_alpha = attr(el, "fillAlpha")
    name = attr(el, "name") or f"p{index:02d}"
    bits = [f'id="{name}"', f'd="{d}"', f'fill="{fill}"']
    if fill_alpha:
        bits.append(f'fill-opacity="{fill_alpha}"')
    if stroke != "none":
        bits.append(f'stroke="{stroke}" stroke-width="{sw}"')
    bits.append('fill-rule="nonzero"')
    return "  <path " + " ".join(bits) + " />"


def walk(el, out, depth, counter):
    for child in el:
        tag = child.tag.split("}")[-1]
        if tag == "group":
            out.append("  " * depth + f'<g transform="{group_transform(child)}">')
            walk(child, out, depth + 1, counter)
            out.append("  " * depth + "</g>")
        elif tag == "path":
            out.append("  " * depth + path_svg(child, counter[0]).strip())
            counter[0] += 1


def convert(src, dst):
    os.makedirs(os.path.dirname(dst) or ".", exist_ok=True)
    root = ET.parse(src).getroot()
    vw = attr(root, "viewportWidth", "1024")
    vh = attr(root, "viewportHeight", "1024")
    body = []
    walk(root, body, 1, [0])
    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{vw}" height="{vh}" '
        f'viewBox="0 0 {vw} {vh}">\n' + "\n".join(body) + "\n</svg>\n"
    )
    with open(dst, "w") as f:
        f.write(svg)
    print(f"{dst}: {len(body)} nodes, viewport {vw}x{vh}")


if __name__ == "__main__":
    convert(sys.argv[1], sys.argv[2])
