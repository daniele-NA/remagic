#!/usr/bin/env python3
"""Skeleton, authored limbs and frame assembly for the running magician.

Everything works in the original 1024x1024 artwork space: joints are points
measured on the source drawing, and each part is placed by an affine matrix
built from those joints. The legs do not exist in the source (the robe hides
them), so they are drawn from scratch in the same style.
"""
import math

import rig

# --------------------------------------------------------------- matrices ---


class M:
    """2x3 affine matrix, same component order as SVG's matrix(a,b,c,d,e,f)."""

    __slots__ = ("a", "b", "c", "d", "e", "f")

    def __init__(self, a=1, b=0, c=0, d=1, e=0, f=0):
        self.a, self.b, self.c, self.d, self.e, self.f = a, b, c, d, e, f

    def __mul__(self, o):
        return M(
            self.a * o.a + self.c * o.b,
            self.b * o.a + self.d * o.b,
            self.a * o.c + self.c * o.d,
            self.b * o.c + self.d * o.d,
            self.a * o.e + self.c * o.f + self.e,
            self.b * o.e + self.d * o.f + self.f,
        )

    def apply(self, p):
        x, y = p
        return (self.a * x + self.c * y + self.e, self.b * x + self.d * y + self.f)

    def svg(self):
        return (
            f"matrix({self.a:.5f},{self.b:.5f},{self.c:.5f},"
            f"{self.d:.5f},{self.e:.5f},{self.f:.5f})"
        )


def T(dx, dy):
    return M(1, 0, 0, 1, dx, dy)


def R(deg, px=0.0, py=0.0):
    """Rotation by deg (clockwise on screen) about (px, py)."""
    r = math.radians(deg)
    cs, sn = math.cos(r), math.sin(r)
    return T(px, py) * M(cs, sn, -sn, cs, 0, 0) * T(-px, -py)


def S(sx, sy, px=0.0, py=0.0):
    return T(px, py) * M(sx, 0, 0, sy, 0, 0) * T(-px, -py)


def direction(deg):
    """Unit vector for a limb angle: 0 is straight down, positive swings forward.

    R() turns clockwise on screen, which from straight down heads *backward*,
    so a limb posed at `deg` is rotated by R(-deg). Keeping the tables written
    as "positive = forward" and negating here is the only place the two
    conventions meet.
    """
    r = math.radians(deg)
    return (math.sin(r), math.cos(r))


def limb_rot(deg, px=0.0, py=0.0):
    """Rotation matrix matching direction(): positive swings the limb forward."""
    return R(-deg, px, py)


def place_2pt(art_a, art_b, world_a, world_b, thickness=1.0):
    """Matrix mapping art_a->world_a and art_b->world_b.

    Scales along the a->b axis to bridge the distance and independently across
    it, so a short bent sleeve can be stretched into a long reaching arm
    without also becoming twice as fat.
    """
    ax, ay = art_b[0] - art_a[0], art_b[1] - art_a[1]
    wx, wy = world_b[0] - world_a[0], world_b[1] - world_a[1]
    la = math.hypot(ax, ay) or 1.0
    lw = math.hypot(wx, wy) or 1.0
    art_ang = math.degrees(math.atan2(ax, ay))    # from straight down
    world_ang = math.degrees(math.atan2(wx, wy))
    return (
        T(world_a[0], world_a[1])
        * limb_rot(world_ang)
        * M(thickness, 0, 0, lw / la, 0, 0)
        * limb_rot(-art_ang)
        * T(-art_a[0], -art_a[1])
    )


# ------------------------------------------------------------------ joints ---
# All measured on the 1024x1024 render of the source artwork.
HIP = (556, 676)          # pelvis, root of the torso rotation
NECK = (548, 556)         # head pivot
HAT_BASE = (562, 380)     # where the cone meets the brim
FIST = (352, 580)         # centre of the fist wrapped around the staff
ORB = (303, 405)          # centre of the staff orb
HIP_NEAR = (566, 668)
HIP_FAR = (542, 658)
BOOT_NEAR_ANCHOR = (622, 742)   # top centre of the original right boot
BOOT_FAR_ANCHOR = (476, 742)    # top centre of the original left boot
# Ball of the foot, in each boot's own artwork space. This -- not the ankle --
# is the point pinned to the ground, because the boot swings under the ankle as
# the foot angle changes.
BOOT_NEAR_SOLE = (640, 788)
BOOT_FAR_SOLE = (458, 788)

# Sleeve anchors: (top of sleeve, centre of hand) as drawn in the source.
SLEEVE_F = ((424, 552), (408, 636))   # sleeve that grips the staff
SLEEVE_B = ((664, 548), (658, 660))   # sleeve with the open hand

# Where the arms actually hinge once the figure is read as a side view: both
# shoulders sit at the top of the torso rather than at the front-view sleeves.
SHOULDER_NEAR = (610, 545)
SHOULDER_FAR = (548, 520)

THIGH_LEN = 114.0
SHIN_LEN = 108.0
THIGH_R = (51.0, 43.0)    # radius at hip, at knee
SHIN_R = (41.0, 33.0)     # radius at knee, at ankle
ARM_LEN_NEAR = 152.0      # shoulder -> staff grip, kept inside the IK reach
ARM_LEN_FAR = 146.0      # shoulder -> free hand
STAFF_SPAN = math.hypot(FIST[0] - ORB[0], FIST[1] - ORB[1])

# The robe, drawn rather than reused: in the source the chest is entirely
# covered by the beard and the skirt is a front view, so there is no side-view
# body to cut out. Authored in artwork space and carried by the torso matrix,
# so the forward lean sweeps the hem backwards on its own.
ROBE_PATH = (
    "M492,496 "
    "C456,514 440,556 444,596 "        # back edge, tucked in at the waist
    "C447,630 426,660 434,688 "        # flaring out again toward the hem
    "C464,708 506,700 546,706 "        # hem, given a slight wave
    "C590,712 658,706 694,688 "
    "C704,660 684,630 688,596 "
    "C692,556 678,514 646,496 "
    "C598,478 538,478 492,496 Z"
)
ROBE_CENTER = (566, 592)
ROBE_LIGHT_SCALE = 0.90
ROBE_LIGHT_OFF = (16, -14)


def robe_svg(matrix):
    """Outline, dark body, then an inset lighter copy: same recipe as the limbs."""
    light = (
        matrix
        * T(*ROBE_LIGHT_OFF)
        * S(ROBE_LIGHT_SCALE, ROBE_LIGHT_SCALE, *ROBE_CENTER)
    )
    return (
        f'<g transform="{matrix.svg()}" fill="{rig.OUTLINE}" '
        f'stroke="{rig.OUTLINE}" stroke-width="20" stroke-linejoin="round">'
        f'<path d="{ROBE_PATH}"/></g>'
        f'<g transform="{matrix.svg()}"><path d="{ROBE_PATH}" '
        f'fill="{rig.ROBE_DK}"/></g>'
        f'<g transform="{light.svg()}"><path d="{ROBE_PATH}" '
        f'fill="{rig.ROBE}"/></g>'
    )

# --------------------------------------------------------------- the face ---
# The source face is dead-on frontal: two symmetric eyes, a centred nose. Rotated
# into a run it still looks straight at the camera, so it is redrawn here as a
# three-quarter head turned the way the body travels -- a cheek wedge with the
# nose pushed out to the leading edge, both eyes shifted forward, and the
# moustache sweeping back into the beard that is still the original artwork.
# Authored in artwork space and carried by the head matrix.
# Hair filling the back of the head, under the brim. Without it the skin would
# have to span the whole width of the head to close the gap, and a full-width
# face reads frontal again however the features are arranged. It is drawn behind
# the original beard, which it continues upward.
FACE_HAIR = [
    '<ellipse cx="500" cy="442" rx="72" ry="42"/>',
    '<ellipse cx="548" cy="442" rx="44" ry="38"/>',
    '<ellipse cx="626" cy="508" rx="48" ry="32"/>',   # moustache below the nose
]
FACE_HAIR_LIGHT = [
    '<ellipse cx="494" cy="446" rx="62" ry="34"/>',
    '<ellipse cx="542" cy="446" rx="36" ry="30"/>',
    '<ellipse cx="622" cy="512" rx="41" ry="25"/>',
]
# Skin: a cheek wedge kept to the leading half of the head, and a nose lobe
# pushed out past it -- the silhouette, not the features, is what carries the
# direction at 240 px.
FACE_SKIN = [
    '<ellipse cx="596" cy="452" rx="60" ry="38"/>',
    '<circle cx="654" cy="484" r="32"/>',
]
# same shapes nudged down and shrunk: leaves the darker band along the brow,
# exactly how the source shades the face under the hat brim
FACE_LIGHT = [
    '<ellipse cx="598" cy="462" rx="54" ry="29"/>',
    '<circle cx="655" cy="490" r="27"/>',
]
# eyes: narrowed and slanted into a scowl, both carried toward the leading side
FACE_EYES = [
    '<ellipse cx="588" cy="454" rx="12" ry="17" transform="rotate(18,588,454)"/>',
    '<ellipse cx="630" cy="461" rx="13" ry="19" transform="rotate(18,630,461)"/>',
]
# brow ridge and the moustache curling back off the nose into the beard
FACE_LINES = (
    '<path d="M566,430 C594,422 624,430 650,444" fill="none" stroke-width="15"/>'
)


def face_svg(matrix):
    """Draw the three-quarter face: hair, then skin, brow shade and features."""
    t = matrix.svg()
    hair, hair_lt = "".join(FACE_HAIR), "".join(FACE_HAIR_LIGHT)
    skin, skin_lt = "".join(FACE_SKIN), "".join(FACE_LIGHT)
    return (
        f'<g transform="{t}" fill="{rig.OUTLINE}" stroke="{rig.OUTLINE}" '
        f'stroke-width="20" stroke-linejoin="round">{hair}</g>'
        f'<g transform="{t}" fill="{rig.BEARD_DK}">{hair}</g>'
        f'<g transform="{t}" fill="{rig.BEARD}">{hair_lt}</g>'
        f'<g transform="{t}" fill="{rig.OUTLINE}" stroke="{rig.OUTLINE}" '
        f'stroke-width="20" stroke-linejoin="round">{skin}</g>'
        f'<g transform="{t}" fill="{rig.SKIN_DK}">{skin}</g>'
        f'<g transform="{t}" fill="{rig.SKIN}">{skin_lt}</g>'
        f'<g transform="{t}" fill="{rig.OUTLINE}" stroke="{rig.OUTLINE}" '
        f'stroke-linecap="round">{"".join(FACE_EYES)}{FACE_LINES}</g>'
    )


# ------------------------------------------------------------- limb shapes ---


def _capsule(p1, p2, r1, r2, off=(0.0, 0.0), dr=0.0):
    """A tapered capsule as a polygon plus a disc at each end.

    Emitted as separate shapes on purpose: the leg is drawn in passes (outline,
    then fill), so overlapping pieces merge into one clean silhouette instead of
    showing a seam at the knee. `off`/`dr` shift and shrink the capsule, which
    is how the lighter inner fill is derived from the same geometry.
    """
    (x1, y1), (x2, y2) = p1, p2
    x1, y1 = x1 + off[0], y1 + off[1]
    x2, y2 = x2 + off[0], y2 + off[1]
    r1, r2 = max(r1 - dr, 1.0), max(r2 - dr, 1.0)
    dx, dy = x2 - x1, y2 - y1
    L = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / L, dx / L
    quad = (
        f"M{x1 + nx * r1:.1f},{y1 + ny * r1:.1f} "
        f"L{x2 + nx * r2:.1f},{y2 + ny * r2:.1f} "
        f"L{x2 - nx * r2:.1f},{y2 - ny * r2:.1f} "
        f"L{x1 - nx * r1:.1f},{y1 - ny * r1:.1f} Z"
    )
    return [
        f'<path d="{quad}"/>',
        f'<circle cx="{x1:.1f}" cy="{y1:.1f}" r="{r1:.1f}"/>',
        f'<circle cx="{x2:.1f}" cy="{y2:.1f}" r="{r2:.1f}"/>',
    ]


# how far the lighter fill is pushed toward the front of the leg
LIGHT_OFF = (8.0, -5.0)
LIGHT_DR = 7.0


def leg_ik(hip, ankle, bend=-1.0):
    """Return (solid, light, knee, shin_deg) for a leg solved onto `ankle`.

    Driving the leg from the foot rather than from hip/knee angles is what lets
    the planted foot stay nailed to the floor while the body rides over it.
    """
    knee = ik2(hip, ankle, THIGH_LEN, SHIN_LEN, bend)
    shin_deg = math.degrees(
        math.atan2(ankle[0] - knee[0], ankle[1] - knee[1])
    )
    solid = _capsule(hip, knee, *THIGH_R) + _capsule(knee, ankle, *SHIN_R)
    light = (
        _capsule(hip, knee, *THIGH_R, off=LIGHT_OFF, dr=LIGHT_DR)
        + _capsule(knee, ankle, *SHIN_R, off=LIGHT_OFF, dr=LIGHT_DR)
    )
    return solid, light, knee, shin_deg


UPPER_ARM = 84.0
FORE_ARM = 80.0
UPPER_R = (31.0, 26.0)
FORE_R = (25.0, 21.0)
HAND_FREE_ANCHOR = (658, 650)   # centre of the open hand in the source


def ik2(root, target, l1, l2, bend=1.0):
    """Two-bone IK: return the joint between root and target.

    `bend` picks which side the elbow/knee breaks toward. The target distance is
    clamped so a slightly out-of-reach pose straightens the limb instead of
    producing a NaN.
    """
    dx, dy = target[0] - root[0], target[1] - root[1]
    d = math.hypot(dx, dy)
    d = min(max(d, abs(l1 - l2) + 1e-3), l1 + l2 - 1e-3)
    cos_a = max(-1.0, min(1.0, (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d)))
    a = math.acos(cos_a) * (1.0 if bend >= 0 else -1.0)
    base = math.atan2(dy, dx)
    return (root[0] + l1 * math.cos(base + a), root[1] + l1 * math.sin(base + a))


def arm_shapes(shoulder, hand, bend=1.0):
    """Return (solid, light, elbow) for a drawn arm reaching `hand`."""
    elbow = ik2(shoulder, hand, UPPER_ARM, FORE_ARM, bend)
    solid = _capsule(shoulder, elbow, *UPPER_R) + _capsule(elbow, hand, *FORE_R)
    light = (
        _capsule(shoulder, elbow, *UPPER_R, off=LIGHT_OFF, dr=7.0)
        + _capsule(elbow, hand, *FORE_R, off=LIGHT_OFF, dr=6.0)
    )
    return solid, light, elbow


def draw_limb(solid, light):
    """Outline pass, dark fill, then light fill: one solid two-tone limb."""
    a, b = "".join(solid), "".join(light)
    return (
        f'<g fill="{rig.OUTLINE}" stroke="{rig.OUTLINE}" stroke-width="20" '
        f'stroke-linejoin="round">{a}</g>'
        f'<g fill="{rig.ROBE_DK}" stroke="none">{a}</g>'
        f'<g fill="{rig.ROBE}" stroke="none">{b}</g>'
    )
