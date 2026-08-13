#!/usr/bin/env python3
"""Emit drop-in Android resources for the run cycle.

Copies the 240px frames under a drawable-friendly name and writes one
<animation-list> per facing. 240 matches GameConstants.MAGICIAN_FRAME_SIDE, so
the frames need no runtime scaling.
"""
import os
import shutil

N = 8
FRAME_MS = 70          # ~14 fps, one full stride in 560 ms
ROOT = "sprite/android"


def main():
    drawable = f"{ROOT}/res/drawable-nodpi"
    shutil.rmtree(ROOT, ignore_errors=True)
    os.makedirs(drawable)

    for facing in ("right", "left"):
        for i in range(N):
            shutil.copy(
                f"sprite/frames/run_{facing}_240/magician_run_{facing}_{i}.png",
                f"{drawable}/magician_run_{facing}_{i}.png",
            )
        items = "\n".join(
            f'    <item android:drawable="@drawable/magician_run_{facing}_{i}" '
            f'android:duration="{FRAME_MS}" />'
            for i in range(N)
        )
        xml = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<animation-list xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:oneshot="false">\n'
            f"{items}\n"
            "</animation-list>\n"
        )
        with open(f"{drawable}/magician_run_{facing}.xml", "w") as f:
            f.write(xml)
        print(f"  magician_run_{facing}.xml + {N} frames")

    print(f"-> {drawable}")


if __name__ == "__main__":
    main()
