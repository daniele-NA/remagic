#!/usr/bin/env python3
"""Cut-out rig for the magician.

The source artwork (magician_stand.xml) is an auto-trace: every black line is a
single 1507-point subpath wrapping the whole figure, so limbs cannot be split by
picking paths. Instead each body part is defined as

    fills  -> the indices of the coloured paths that belong to the part
    region -> a polygon in the 1024x1024 viewport claiming that area

and the shared line-art is sliced with an SVG <mask>: a part keeps its own
region minus the regions of every higher-priority part. That makes the cuts
exact and non-overlapping while staying fully vectorial.

Every part instances the *whole* artwork and lets the mask do all the cutting.
Doing it that way -- rather than picking per-part fills -- is what makes the
pieces reassemble pixel-identical to the original: the source paths overlap each
other (a robe shade is painted over the beard, for instance), so any split by
path membership loses exactly those overlaps.
"""
import math
import re
import xml.etree.ElementTree as ET

SVG_NS = "http://www.w3.org/2000/svg"
SRC = "sprite/svg/magician_stand.svg"
VIEW = 1024

# ---------------------------------------------------------------- palette ---
OUTLINE = "#191021"
ROBE = "#946AC4"
ROBE_LT = "#956BC5"
ROBE_DK = "#5D378C"
ROBE_DK2 = "#56317D"
SKIN = "#FDC081"
SKIN_DK = "#F1823F"
BEARD = "#EDEBE0"
BEARD_DK = "#B9B8C4"
LEATHER = "#A95128"
LEATHER_DK = "#7E361E"

# ------------------------------------------------------------ art loading ---


def load_art():
    """Return (paths, inner_transform) where paths is a list of (d, fill)."""
    root = ET.parse(SRC).getroot()
    g = root[0]
    inner = g.get("transform")
    paths = []
    for p in g.iter(f"{{{SVG_NS}}}path"):
        paths.append((p.get("d"), p.get("fill")))
    return paths, inner


OUTLINE_IDX = 0  # p00 holds the entire black line-art

# ------------------------------------------------------------- part table ---
# Boundary shared by the hat cone and the head. Measured from the isolated brim
# and cone fills: where the cone sits on the brim the line runs through the
# black join between them, and past the cone it clears the brim's own outline —
# including the pointed left tip, which an eyeballed line kept handing to the
# cone and tearing off the hat.
BRIM_TOP = [
    (348, 364), (368, 382), (382, 376), (392, 371), (404, 366), (416, 362),
    (428, 359), (440, 356), (452, 352), (466, 358),
    (480, 366), (505, 366), (528, 366), (550, 368), (572, 371), (594, 375),
    # past the cone the line follows the top of the brim's own outline: the
    # brim ends in a fat rounded lobe, and a straight diagonal across it left
    # the tip hanging off the hat as a thin whisker
    (616, 381), (638, 385), (656, 386), (676, 388), (694, 392), (710, 400),
    (724, 411), (736, 424), (746, 437), (754, 455), (762, 482), (774, 514),
]

CONE_REGION = BRIM_TOP + [(824, 514), (824, 150), (366, 150)]

# Down the right of the brim, round the bottom of the beard and back up its
# left edge -- traced off the measured beard outline so the cut never bites
# into the white.
HEAD_REGION = BRIM_TOP + [
    (750, 522), (716, 514), (686, 524), (672, 556), (664, 592),
    (650, 614), (620, 630), (580, 638), (536, 640), (498, 634),
    (468, 618), (452, 598), (438, 574), (426, 552), (414, 530),
    (409, 502), (414, 472), (382, 440), (358, 400),
]
# The head and cone both run past the staff on the left; the staff outranks
# them, so the overlap is trimmed exactly and no sliver is left unclaimed.

# The frontal face -- skin band, both eyes, moustache -- plus the upper lobe of
# the grey beard on the leading side. All of it is claimed and discarded: a
# symmetric front-facing head reads as looking at the camera no matter which way
# the body runs, so it is replaced by a drawn three-quarter face. The top edge
# follows the underside of the hat brim so the brim itself is left untouched.
FACE_REGION = [
    (428, 400), (460, 401), (500, 401), (520, 403), (540, 405), (560, 409),
    (580, 412), (600, 417), (620, 422), (640, 427), (660, 433),
    (680, 448), (690, 474), (688, 506), (670, 536), (636, 546), (604, 522),
    (566, 494), (520, 496), (472, 486), (438, 468),
]

# The open hand, carved out of the sleeve that surrounds it: the sleeves
# themselves are replaced by drawn arms, but the hands are kept.
HAND_FREE_REGION = [
    (604, 600), (652, 590), (700, 600), (716, 636), (714, 676), (688, 708),
    (640, 716), (608, 690), (598, 644),
]

# Staff = orb + pole + the fist gripping it (they rotate as one rigid piece).
STAFF_REGION = [
    # orb, out to the notch at (373,385) where its outline meets the hat brim
    (236, 400), (244, 348), (300, 334), (352, 344), (368, 362), (376, 384),
    # right side: down the pole, kept clear of the sleeve at x>=390
    (377, 406), (371, 436), (364, 462), (360, 505), (388, 540), (390, 620),
    (398, 700), (410, 780),
    (394, 800), (346, 800),
    # left side: follows the fist bulge measured off the silhouette
    (343, 755), (338, 700), (330, 660), (310, 625), (293, 600), (292, 570),
    (302, 545), (300, 520), (292, 498), (284, 468), (244, 452),
]

# The two sleeves are discarded (they are front-view arms), so their regions are
# drawn wide enough to swallow their own black outline as well -- anything left
# behind would be inherited by the torso as loose specks of line-art.
ARM_F_REGION = [
    (370, 524), (432, 514), (474, 530), (480, 596), (474, 668), (432, 690),
    (388, 676), (366, 606),
]
ARM_B_REGION = [
    (596, 520), (668, 502), (726, 516), (760, 560), (766, 646), (752, 712),
    (704, 730), (644, 722), (606, 680), (588, 596),
]

BELT_REGION = [(440, 602), (642, 602), (646, 682), (440, 682)]

BOOT_F_REGION = [(410, 724), (538, 724), (542, 806), (410, 806)]
BOOT_B_REGION = [(574, 724), (682, 724), (686, 806), (574, 806)]

TORSO_REGION = [(360, 452), (772, 452), (782, 806), (360, 806)]

# Priority order: earlier parts claim contested pixels first. The head outranks
# the sleeves so the beard stays whole; the sleeves are claimed but never drawn
# (they are front-view arms, replaced by drawn ones), which is also what removes
# them from the running figure.
PRIORITY = [
    "staff", "hand_free", "boot_front", "boot_back", "belt",
    "face", "cone", "head", "arm_front", "arm_back", "torso",
]
# Claimed so their line-art leaves the figure, but never drawn: the front-view
# sleeves are replaced by drawn arms, and the robe -- which in the source is
# almost entirely hidden behind the beard -- is redrawn as a side-view body.
DISCARD = {"arm_front", "arm_back", "torso", "face"}

PARTS = {
    "cone":       {"region": CONE_REGION},
    "head":       {"region": HEAD_REGION},
    "torso":      {"region": TORSO_REGION},
    "arm_front":  {"region": ARM_F_REGION},
    "arm_back":   {"region": ARM_B_REGION},
    "hand_free":  {"region": HAND_FREE_REGION},
    "face":       {"region": FACE_REGION},
    "belt":       {"region": BELT_REGION},
    "staff":      {"region": STAFF_REGION},
    "boot_front": {"region": BOOT_F_REGION},
    "boot_back":  {"region": BOOT_B_REGION},
}

# ------------------------------------------------------------- svg helpers ---


def pts(poly):
    return " ".join(f"{x},{y}" for x, y in poly)


BLEED = 3.0  # px each part reaches under its neighbours


def offset_polygon(poly, d):
    """Offset a simple polygon by d px (positive = outward).

    Each edge is pushed along its outward normal and consecutive edges are
    re-intersected, so corners stay sharp instead of rounding off.
    """
    n = len(poly)
    area2 = sum(
        poly[i][0] * poly[(i + 1) % n][1] - poly[(i + 1) % n][0] * poly[i][1]
        for i in range(n)
    )
    sign = 1.0 if area2 > 0 else -1.0
    edges = []
    for i in range(n):
        (x1, y1), (x2, y2) = poly[i], poly[(i + 1) % n]
        dx, dy = x2 - x1, y2 - y1
        L = math.hypot(dx, dy)
        if L < 1e-9:
            continue
        nx, ny = sign * dy / L, -sign * dx / L
        edges.append(((x1 + nx * d, y1 + ny * d), (x2 + nx * d, y2 + ny * d)))

    out = []
    m = len(edges)
    for i in range(m):
        (ax, ay), (bx, by) = edges[i - 1]
        (cx, cy), (dx2, dy2) = edges[i]
        r1x, r1y = bx - ax, by - ay
        r2x, r2y = dx2 - cx, dy2 - cy
        den = r1x * r2y - r1y * r2x
        if abs(den) < 1e-9:  # parallel edges: no corner to solve
            out.append((cx, cy))
            continue
        t = ((cx - ax) * r2y - (cy - ay) * r2x) / den
        out.append((ax + t * r1x, ay + t * r1y))
    return out


def masks_defs():
    """One <mask> per part: own region white, higher-priority regions black.

    The white region is dilated by BLEED and the subtracted ones eroded by the
    same amount, so each part keeps a lip tucked under its neighbours. Without
    it two abutting masks each land at ~50% alpha on the shared edge and
    composite to ~75%, leaving a visible hairline down every cut.
    """
    out = []
    for i, name in enumerate(PRIORITY):
        if name not in PARTS:
            continue
        own = offset_polygon(PARTS[name]["region"], BLEED)
        body = [f'<polygon points="{pts(own)}" fill="#fff"/>']
        for other in PRIORITY[:i]:
            if other in PARTS:
                cut = offset_polygon(PARTS[other]["region"], -BLEED)
                body.append(f'<polygon points="{pts(cut)}" fill="#000"/>')
        out.append(
            f'<mask id="m_{name}" maskUnits="userSpaceOnUse" '
            f'x="0" y="0" width="{VIEW}" height="{VIEW}">' + "".join(body) + "</mask>"
        )
    return "\n".join(out)


def art_defs(paths, inner):
    """The complete artwork as one reusable <g>, shared by every part."""
    body = "".join(f'<path d="{d}" fill="{f}"/>' for d, f in paths)
    return f'<g id="art"><g transform="{inner}">{body}</g></g>'


def part_use(name, transform=""):
    """A masked, transformed instance of one part.

    The mask sits on an untransformed inner <g> so its polygon coordinates stay
    in original artwork space while `transform` moves the whole piece.
    """
    t = f' transform="{transform}"' if transform else ""
    return f'<g{t}><g mask="url(#m_{name})"><use href="#art"/></g></g>'
