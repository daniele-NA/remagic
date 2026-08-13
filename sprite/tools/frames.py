#!/usr/bin/env python3
"""Build the 8-frame run cycle and render it.

Usage: frames.py [--size N] [--debug]
"""
import math
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pose
import rig
from pose import M, R, S, T

from PIL import Image, ImageDraw

N = 8
OUT_SVG = "sprite/svg/frames"
OUT_PNG = "sprite/qa/_frames"

# ------------------------------------------------------------ animation ---
# One entry per frame, starting at the near foot's contact with the ground.
# Angles are degrees, clockwise-positive, measured from straight down for the
# limbs and relative to the parent joint.
CYCLE = {
    #            contact  down   pass   push   recover swing  fwd    reach
    "lean":       [  30,  31.5,  30.5,    29,    30,   31.5,  30.5,    29],
    "head":       [ -10,   -12,   -10,    -7,   -10,    -12,   -10,    -7],
    "cone":       [ -28,   -36,   -32,   -20,   -28,    -36,   -32,   -20],
    # free arm: same phase as the near leg (it is the far arm, so it mirrors
    # the far leg, which is the near leg's opposite)
    "arm_far":    [  40,    15,   -25,   -58,   -30,      5,    35,    46],
    # staff arm barely swings -- it is carrying something
    "arm_near":   [  56,    52,    49,    46,    49,     53,    57,    58],
    # direction from grip to orb, measured from straight down
    "staff":      [ 104,   106,   104,   101,   104,    106,   104,   101],
}

ROOT_X, ROOT_Y = -30.0, -30.0  # whole-figure offset inside the canvas
BEND_NEAR = 1.0   # which way each elbow breaks
BEND_FAR = 1.0
KNEE_BEND = -1.0  # knees break forward for a right-facing run

# Hip height per frame, absolute. This is the run's bounce, keyed directly:
# mid on contact, lowest while the leg absorbs, back up through the push, and
# highest in flight. Period 4, so both legs produce the same curve.
HIP_Y = [668, 688, 668, 638, 668, 688, 668, 638]

# Where each foot is, one entry per phase of a single leg; the far leg reads the
# same table four frames later. `None` for dy means "planted", i.e. the ankle
# sits on ANKLE_GROUND and the leg is solved by IK to reach it -- so the foot
# stays nailed to the floor while the body rides over it.
ANKLE_GROUND = 838.0
FOOT_PATH = [
    ( 80, None),   # 0 contact, reaching forward onto the ground
    ( 30, None),   # 1 absorb, body lowest, foot under the hip
    (-30, None),   # 2 push, foot sweeping behind
    (-86,  156),   # 3 toe-off, airborne
    (-96,  100),   # 4 heel up, knee folding
    (-40,   80),   # 5 knee driving forward, foot tucked high
    ( 46,   78),   # 6 shin unfolding forward
    ( 94,  138),   # 7 reach, foot dropping toward contact
]
# Foot angle relative to the shin: heel-first at contact, toe-down through the
# push, toe lifted while swinging through.
FOOT_REL = [-8, 4, 22, 38, 8, -16, -24, -14]

PLANT = [FOOT_PATH[i][1] is None for i in range(N)]


def frame_pose(i):
    return {k: v[i] for k, v in CYCLE.items()}


def root_dy_for(i):
    """Vertical offset that puts the pelvis at the keyed hip height."""
    return HIP_Y[i] - (ROOT_Y + pose.HIP[1])


def solve_root_dy():
    return [root_dy_for(i) for i in range(N)]


def sole_ground():
    """Y of the floor, measured from the frames rather than assumed.

    ANKLE_GROUND pins the ankle; the sole sits a boot's height below it, so the
    real ground line is read back off the planted feet and used to align every
    exported frame.
    """
    dy = solve_root_dy()
    vals = []
    for i in range(N):
        _, legs = build_frame(i, dy[i])
        for s in ("near", "far"):
            ph = i if s == "near" else (i + N // 2) % N
            if PLANT[ph]:
                vals.append(legs[s][3][1])
    return sum(vals) / len(vals)


def build_frame(i, root_dy=0.0, debug=False):
    p = frame_pose(i)

    root = T(ROOT_X, ROOT_Y + root_dy)
    torso = root * R(p["lean"], *pose.HIP)
    head = torso * R(p["head"], *pose.NECK)
    cone = head * R(p["cone"], *pose.HAT_BASE)

    # Arms are re-aimed rather than merely rotated: the source sleeves belong to
    # a front view, so each is stretched from the side-view shoulder to where
    # its hand has to be.
    sh_n = torso.apply(pose.SHOULDER_NEAR)
    sh_f = torso.apply(pose.SHOULDER_FAR)
    ang_n = p["arm_near"] + p["lean"]
    ang_f = p["arm_far"] + p["lean"]
    grip = (
        sh_n[0] + pose.ARM_LEN_NEAR * pose.direction(ang_n)[0],
        sh_n[1] + pose.ARM_LEN_NEAR * pose.direction(ang_n)[1],
    )
    hand_f = (
        sh_f[0] + pose.ARM_LEN_FAR * pose.direction(ang_f)[0],
        sh_f[1] + pose.ARM_LEN_FAR * pose.direction(ang_f)[1],
    )
    arm_n_solid, arm_n_light, _ = pose.arm_shapes(sh_n, grip, bend=BEND_NEAR)
    arm_f_solid, arm_f_light, elbow_f = pose.arm_shapes(
        sh_f, hand_f, bend=BEND_FAR
    )
    # the free hand rides on the end of its forearm
    fore_ang = math.degrees(
        math.atan2(hand_f[0] - elbow_f[0], hand_f[1] - elbow_f[1])
    )
    hand_free = (
        T(hand_f[0], hand_f[1])
        * pose.limb_rot(fore_ang)
        * T(-pose.HAND_FREE_ANCHOR[0], -pose.HAND_FREE_ANCHOR[1])
    )

    # staff: gripped at the fist, orb leading the run
    ux, uy = pose.direction(p["staff"])
    orb_w = (grip[0] + pose.STAFF_SPAN * ux, grip[1] + pose.STAFF_SPAN * uy)
    staff = pose.place_2pt(pose.FIST, pose.ORB, grip, orb_w)

    legs = {}
    for side, boot_anchor, boot_sole, mirror in (
        ("near", pose.BOOT_NEAR_ANCHOR, pose.BOOT_NEAR_SOLE, False),
        ("far", pose.BOOT_FAR_ANCHOR, pose.BOOT_FAR_SOLE, True),
    ):
        ph = i if side == "near" else (i + N // 2) % N
        hip_w = torso.apply(pose.HIP_NEAR if side == "near" else pose.HIP_FAR)
        fdx, fdy = FOOT_PATH[ph]
        ankle = (
            hip_w[0] + fdx,
            ANKLE_GROUND if fdy is None else hip_w[1] + fdy,
        )
        solid, light, knee, shin_deg = pose.leg_ik(hip_w, ankle, KNEE_BEND)
        boot = T(ankle[0], ankle[1]) * pose.limb_rot(shin_deg + FOOT_REL[ph])
        if mirror:  # the source left boot points the wrong way for a right run
            boot = boot * S(-1, 1)
        boot = boot * T(-boot_anchor[0], -boot_anchor[1])
        legs[side] = (pose.draw_limb(solid, light), boot, ankle, boot.apply(boot_sole))

    layers = [
        ("arm_far", pose.draw_limb(arm_f_solid, arm_f_light)),
        ("hand_free", rig.part_use("hand_free", hand_free.svg())),
        ("leg_far", legs["far"][0]),
        ("boot_far", rig.part_use("boot_far", legs["far"][1].svg())),
        ("leg_near", legs["near"][0]),
        ("boot_near", rig.part_use("boot_near", legs["near"][1].svg())),
        ("robe", pose.robe_svg(torso)),
        ("belt", rig.part_use("belt", torso.svg())),
        ("cone", rig.part_use("cone", cone.svg())),
        ("face", pose.face_svg(head)),
        ("head", rig.part_use("head", head.svg())),
        ("arm_near", pose.draw_limb(arm_n_solid, arm_n_light)),
        ("staff", rig.part_use("staff", staff.svg())),
    ]
    if debug:
        marks = [(sh_n, "#f0f"), (sh_f, "#0ff"), (grip, "#ff0"), (hand_f, "#f80")]
        marks += [(legs[s][3], "#0f0") for s in ("near", "far")]
        layers.append((
            "debug",
            "".join(
                f'<circle cx="{q[0]:.0f}" cy="{q[1]:.0f}" r="7" fill="{c}"/>'
                for q, c in marks
            ),
        ))
    return layers, legs


# boot_far/boot_near are aliases of the source boots, remapped for the run
rig.PARTS["boot_near"] = rig.PARTS["boot_back"]
rig.PARTS["boot_far"] = rig.PARTS["boot_front"]
rig.PRIORITY[:] = [
    {"boot_front": "boot_far", "boot_back": "boot_near"}.get(n, n)
    for n in rig.PRIORITY
]
del rig.PARTS["boot_front"], rig.PARTS["boot_back"]


def document(body, defs, view=rig.VIEW):
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" '
        f'xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{view}" height="{view}" viewBox="0 0 {view} {view}">'
        f"<defs>{defs}</defs>{body}</svg>"
    )


def render(svg, dst, w, h=None):
    args = ["rsvg-convert", "-w", str(w), "-h", str(h or w), "-o", dst]
    r = subprocess.run(args, input=svg.encode(), capture_output=True)
    if r.returncode:
        raise RuntimeError(r.stderr.decode())


def main():
    size = 1024
    debug = "--debug" in sys.argv
    if "--size" in sys.argv:
        size = int(sys.argv[sys.argv.index("--size") + 1])

    os.makedirs(OUT_SVG, exist_ok=True)
    os.makedirs(OUT_PNG, exist_ok=True)

    paths, inner = rig.load_art()
    defs = rig.art_defs(paths, inner) + rig.masks_defs()

    root_dy = solve_root_dy()
    ankles = []
    for i in range(N):
        layers, legs = build_frame(i, root_dy[i], debug)
        svg = document("".join(b for _, b in layers), defs)
        with open(f"{OUT_SVG}/run_{i}.svg", "w") as f:
            f.write(svg)
        render(svg, f"{OUT_PNG}/run_{i}.png", size)
        ankles.append((legs["near"][3][1], legs["far"][3][1]))

    print("planted sole Y per frame (must be constant while planted):")
    for i, (a, b) in enumerate(ankles):
        mark = ("near" if PLANT[i] else "") + ("far" if PLANT[(i + N // 2) % N] else "")
        print(f"  f{i}: near={a:7.1f}  far={b:7.1f}   plant={mark}")

    # contact sheet
    tile = 256
    sheet = Image.new("RGBA", (4 * tile, 2 * (tile + 16)), (252, 252, 254, 255))
    d = ImageDraw.Draw(sheet)
    for i in range(N):
        im = Image.open(f"{OUT_PNG}/run_{i}.png").convert("RGBA")
        im = im.resize((tile, tile), Image.LANCZOS)
        x, y = (i % 4) * tile, (i // 4) * (tile + 16)
        sheet.alpha_composite(im, (x, y))
        d.rectangle([x, y, x + tile - 1, y + tile - 1], outline=(210, 210, 220, 255))
        d.text((x + 5, y + tile + 2), f"f{i}", fill=(0, 0, 0, 255))
    sheet.save("sprite/qa/run_sheet.png")
    print("-> sprite/qa/run_sheet.png")


if __name__ == "__main__":
    main()
