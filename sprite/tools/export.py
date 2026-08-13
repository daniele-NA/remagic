#!/usr/bin/env python3
"""Render, crop and export the run cycle.

All frames share one crop window (the union of their silhouettes) and one
ground line, so the sprite never jitters or drifts between frames. Produces
per-frame PNGs at several sizes, horizontal sprite sheets, and GIF previews,
for both facing directions.
"""
import os
import shutil
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as F
import rig

from PIL import Image

RENDER = 2048                 # working resolution before the crop
SIZES = [240, 480, 96]        # 240 matches GameConstants.MAGICIAN_FRAME_SIDE
GROUND_FRAC = 0.92            # where the ground line sits in the output frame
MARGIN_FRAC = 0.04            # clear space left and right
ROOT = "sprite"


def render_raw():
    """Render every frame at RENDER px on the full 1024 canvas."""
    paths, inner = rig.load_art()
    defs = rig.art_defs(paths, inner) + rig.masks_defs()
    root_dy = F.solve_root_dy()
    out = []
    tmp = f"{ROOT}/qa/_raw"
    os.makedirs(tmp, exist_ok=True)
    for i in range(F.N):
        layers, _ = F.build_frame(i, root_dy[i])
        svg = F.document("".join(b for _, b in layers), defs)
        with open(f"{ROOT}/svg/frames/run_{i}.svg", "w") as f:
            f.write(svg)
        dst = f"{tmp}/{i}.png"
        F.render(svg, dst, RENDER)
        out.append(Image.open(dst).convert("RGBA"))
    return out


def union_bbox(images):
    box = None
    for im in images:
        b = im.getchannel("A").getbbox()
        box = b if box is None else (
            min(box[0], b[0]), min(box[1], b[1]),
            max(box[2], b[2]), max(box[3], b[3]),
        )
    return box


def layout(images):
    """Common crop window: content fills the width, ground line at GROUND_FRAC.

    Derived once from the union of all frames rather than per frame, which is
    what keeps the character locked in place while only the limbs move.
    """
    x0, y0, x1, y1 = union_bbox(images)
    scale = RENDER / rig.VIEW
    ground = F.sole_ground() * scale
    content_w = x1 - x0
    side = content_w / (1.0 - 2 * MARGIN_FRAC)
    # the frame must also fit everything above the ground line, plus clear space
    # -- the tallest frame otherwise lands exactly on the top edge
    need_above = (ground - y0) / (GROUND_FRAC - MARGIN_FRAC)
    side = max(side, need_above, (y1 - y0) / 0.99)
    cx = (x0 + x1) / 2
    left = cx - side / 2
    top = ground - side * GROUND_FRAC
    return tuple(round(v) for v in (left, top, left + side, top + side))


def export_idle(box):
    """Re-frame the untouched standing pose onto the run's ground line.

    The source stand art plants its feet at a different height and scale than
    the run frames, so dropping the cycle in beside it makes the magician hop
    when he starts and stops. Rendering the same artwork through the run's crop
    window fixes both at once, and the idle pose stays the original drawing.
    """
    paths, inner = rig.load_art()
    body = "".join(f'<path d="{d}" fill="{f}"/>' for d, f in paths)

    def render_at(dx, dy, dst):
        svg = (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{rig.VIEW}" '
            f'height="{rig.VIEW}" viewBox="0 0 {rig.VIEW} {rig.VIEW}">'
            f'<g transform="translate({dx},{dy})"><g transform="{inner}">'
            f"{body}</g></g></svg>"
        )
        F.render(svg, dst, RENDER)
        return Image.open(dst).convert("RGBA")

    tmp = f"{ROOT}/qa/_idle.png"
    probe = render_at(0, 0, tmp)
    x0, y0, x1, y1 = probe.getchannel("A").getbbox()
    scale = RENDER / rig.VIEW
    dy = F.sole_ground() - y1 / scale                 # feet onto the same floor
    dx = ((box[0] + box[2]) / 2 - (x0 + x1) / 2) / scale   # centred in the window
    im = render_at(round(dx, 1), round(dy, 1), tmp).crop(box)
    assert im.getchannel("A").getbbox() is not None, "idle cropped to nothing"
    for size in SIZES:
        d = f"{ROOT}/frames/idle_{size}"
        shutil.rmtree(d, ignore_errors=True)
        os.makedirs(d)
        im.resize((size, size), Image.LANCZOS).save(f"{d}/magician_idle.png")
    os.remove(tmp)
    print(f"  idle: re-framed by ({dx:+.0f},{dy:+.0f}) onto the run's ground")


def export():
    images = render_raw()
    box = layout(images)
    print(f"crop window {box}  side={box[2] - box[0]}")
    export_idle(box)

    cropped = [im.crop(box) for im in images]
    for im in cropped:
        b = im.getchannel("A").getbbox()
        assert b is not None, "frame cropped to nothing"

    for facing in ("right", "left"):
        seq = cropped if facing == "right" else [
            im.transpose(Image.FLIP_LEFT_RIGHT) for im in cropped
        ]
        for size in SIZES:
            d = f"{ROOT}/frames/run_{facing}_{size}"
            shutil.rmtree(d, ignore_errors=True)
            os.makedirs(d)
            small = [im.resize((size, size), Image.LANCZOS) for im in seq]
            for i, im in enumerate(small):
                im.save(f"{d}/magician_run_{facing}_{i}.png")
            sheet = Image.new("RGBA", (size * F.N, size), (0, 0, 0, 0))
            for i, im in enumerate(small):
                sheet.paste(im, (i * size, 0))
            os.makedirs(f"{ROOT}/sheets", exist_ok=True)
            sheet.save(
                f"{ROOT}/sheets/magician_run_{facing}_{size}x{F.N}.png"
            )
        gif_frames = [im.resize((240, 240), Image.LANCZOS) for im in seq]
        save_gif(gif_frames, f"{ROOT}/qa/preview_{facing}.gif")
        print(f"  {facing}: {F.N} frames x {SIZES}")


def save_gif(seq, dst, bg=(250, 250, 252)):
    flat = []
    for im in seq:
        b = Image.new("RGB", im.size, bg)
        b.paste(im, mask=im.getchannel("A"))
        flat.append(b.convert("P", palette=Image.ADAPTIVE, colors=128))
    flat[0].save(
        dst, save_all=True, append_images=flat[1:], duration=70, loop=0,
        optimize=True,
    )
    print("  ->", dst)


if __name__ == "__main__":
    export()
