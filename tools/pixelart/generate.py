#!/usr/bin/env python3
"""Deterministic pixel-art generator for Stride & Sire textures.

Usage:
    python tools/pixelart/generate.py           # (re)generate every owned PNG
    python tools/pixelart/generate.py --check   # validate owned PNGs, exit 1 on any problem

All art is drawn from hand-written pixel maps and fixed palettes (no randomness), so running
the script twice produces byte-identical files. Every file the script writes is listed in
MANIFEST; add new outputs there (e.g. the 3D saddlebag model textures).
"""
from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[2]
TEXTURES = REPO_ROOT / "src" / "main" / "resources" / "assets" / "strideandsire" / "textures"

RGBA = tuple[int, int, int, int]
TRANSPARENT: RGBA = (0, 0, 0, 0)


def hex_rgba(value: str) -> RGBA:
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), 255)


# ---------------------------------------------------------------------------
# Palettes
# ---------------------------------------------------------------------------

# Bag body leather: always brown, light source top-left.
LEATHER = {
    "o": "#2e1608",  # outline
    "k": "#4f2610",  # shadow cast by the flap
    "d": "#6e3818",  # dark leather (right / bottom faces)
    "m": "#8f4f25",  # mid leather
    "l": "#ae6a36",  # light leather (top / left faces)
    "L": "#c9874c",  # highlight (handle, flap top edge)
    "r": "#7a3f1c",  # strap, lit side
    "R": "#5c2d12",  # strap, shaded side
    "s": "#e9d2a0",  # stitching thread
}

# Metal fittings per tier: C/c corner caps (light/dark), B/b buckle (light/dark), W sheen.
METALS = {
    "leather": {"C": "#5c2d12", "c": "#3d1d0b", "B": "#4a2410", "b": "#2a1307", "W": "#a8693c"},
    "iron": {"C": "#d8d8d8", "c": "#8c8c8c", "B": "#f2f2f2", "b": "#6a6a6a", "W": "#ffffff"},
    "gold": {"C": "#fcdb4a", "c": "#c48c12", "B": "#fff27a", "b": "#8a5e0a", "W": "#fffbd0"},
    "diamond": {"C": "#4aedd9", "c": "#1aaaa7", "B": "#a1fbe8", "b": "#0f736e", "W": "#e8fffa"},
    "netherite": {"C": "#6b6168", "c": "#3b3338", "B": "#625468", "b": "#332b30", "W": "#9a82ad"},
}

# Diamond tier colours for the horseshoe recolours, keyed by the grey of the iron source pixel.
DIAMOND_HORSESHOE_ITEM = {0x4A: "#0f6f6a", 0xD8: "#4aedd9"}
DIAMOND_HOOF = {0xA8: "#2ccfbf", 0xDA: "#a1fbe8"}

# Vanilla empty-slot sprites are 1-px outlines in a single grey (saddle uses #7c7c7c).
SLOT_GREY = "#7c7c7c"

# ---------------------------------------------------------------------------
# Pixel maps ('.' = transparent)
# ---------------------------------------------------------------------------

# Saddlebag icon: attachment strap over the top, flap with hem stitching curving into a
# tongue that ends in the buckle, shadow under the flap, metal corner caps.
SADDLEBAG_MAP = [
    "................",
    "......oooo......",
    "......orRo......",
    "..ooooorRooooo..",
    ".oLLLLLrRLLLLLo.",
    "oCWllllrRllllCco",
    "oClmmmmrRmmmmdco",
    "olsmmmmrRmmmmsdo",
    "oooosmmrRmmsoooo",
    "olkkoomrRdookkdo",
    "olmmkoBWBbokmmdo",
    "olmmmkBrRbkmmmdo",
    "oCmsmsbbbbsmsdco",
    "oCCddddddddddcco",
    ".oooooooooooooo.",
    "................",
]

# Glyphs of SADDLEBAG_MAP that form the empty-slot outline (outline + buckle frame).
SADDLEBAG_SLOT_GLYPHS = set("oBbW")


def render_map(pixel_map: list[str], palette: dict[str, str]) -> Image.Image:
    height, width = len(pixel_map), len(pixel_map[0])
    image = Image.new("RGBA", (width, height), TRANSPARENT)
    for y, row in enumerate(pixel_map):
        assert len(row) == width, f"row {y} has {len(row)} pixels, expected {width}"
        for x, glyph in enumerate(row):
            if glyph != ".":
                image.putpixel((x, y), hex_rgba(palette[glyph]))
    return image


def recolour_by_grey(source: Path, ramp: dict[int, str]) -> Image.Image:
    """Keep the exact opaque-pixel mask of `source`; map each pixel to the ramp entry whose
    reference grey is closest to the pixel's grey value."""
    src = Image.open(source).convert("RGBA")
    out = Image.new("RGBA", src.size, TRANSPARENT)
    keys = sorted(ramp)
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            grey = (r + g + b) // 3
            key = min(keys, key=lambda k: (abs(k - grey), k))
            cr, cg, cb, _ = hex_rgba(ramp[key])
            out.putpixel((x, y), (cr, cg, cb, a))
    return out


# ---------------------------------------------------------------------------
# Drawings
# ---------------------------------------------------------------------------

def draw_saddlebag(tier: str) -> Image.Image:
    return render_map(SADDLEBAG_MAP, {**LEATHER, **METALS[tier]})


def draw_diamond_horseshoe_item() -> Image.Image:
    # Same silhouette as the other horseshoe icons: recolour the iron icon.
    return recolour_by_grey(TEXTURES / "item" / "iron_horseshoe.png", DIAMOND_HORSESHOE_ITEM)


def draw_diamond_hoof() -> Image.Image:
    # Same UV layout as the other tiers: reuse iron's opaque-pixel mask exactly.
    return recolour_by_grey(TEXTURES / "entity" / "horseshoe" / "iron.png", DIAMOND_HOOF)


def outline_of(mask: list[list[bool]]) -> list[list[bool]]:
    """Pixels of `mask` that touch a non-mask pixel (4-neighbourhood) or the border."""
    h, w = len(mask), len(mask[0])

    def inside(x: int, y: int) -> bool:
        return 0 <= x < w and 0 <= y < h and mask[y][x]

    return [[mask[y][x] and not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
             for x in range(w)] for y in range(h)]


def draw_slot_horseshoe() -> Image.Image:
    src = Image.open(TEXTURES / "item" / "iron_horseshoe.png").convert("RGBA")
    mask = [[src.getpixel((x, y))[3] > 0 for x in range(src.width)] for y in range(src.height)]
    edge = outline_of(mask)
    out = Image.new("RGBA", src.size, TRANSPARENT)
    grey = hex_rgba(SLOT_GREY)
    for y, row in enumerate(edge):
        for x, on in enumerate(row):
            if on:
                out.putpixel((x, y), grey)
    return out


def draw_slot_saddlebag() -> Image.Image:
    rows = ["".join("g" if c in SADDLEBAG_SLOT_GLYPHS else "." for c in row) for row in SADDLEBAG_MAP]
    return render_map(rows, {"g": SLOT_GREY})


# ---------------------------------------------------------------------------
# Manifest of owned outputs
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class Output:
    path: str  # relative to TEXTURES
    size: tuple[int, int]
    draw: Callable[[], Image.Image]


def hoof_size() -> tuple[int, int]:
    with Image.open(TEXTURES / "entity" / "horseshoe" / "iron.png") as im:
        return im.size


MANIFEST: list[Output] = [
    *[Output(f"item/{tier}_saddlebag.png", (16, 16), (lambda t=tier: draw_saddlebag(t)))
      for tier in ("leather", "iron", "gold", "diamond", "netherite")],
    Output("item/diamond_horseshoe.png", (16, 16), draw_diamond_horseshoe_item),
    Output("entity/horseshoe/diamond.png", hoof_size(), draw_diamond_hoof),
    Output("gui/sprites/container/slot/horseshoe.png", (16, 16), draw_slot_horseshoe),
    Output("gui/sprites/container/slot/saddlebag.png", (16, 16), draw_slot_saddlebag),
]


def generate() -> None:
    for out in MANIFEST:
        image = out.draw()
        assert image.size == out.size, f"{out.path}: drew {image.size}, expected {out.size}"
        target = TEXTURES / out.path
        target.parent.mkdir(parents=True, exist_ok=True)
        image.save(target, format="PNG")
        print(f"wrote {out.path} {out.size[0]}x{out.size[1]}")


def check() -> int:
    problems = 0
    for out in MANIFEST:
        target = TEXTURES / out.path
        expected = f"{out.size[0]}x{out.size[1]}"
        if not target.is_file():
            print(f"MISSING {out.path} (expected {expected})")
            problems += 1
            continue
        try:
            with Image.open(target) as im:
                size, pixels = im.size, im.convert("RGBA").tobytes()
        except OSError as exc:
            print(f"BROKEN  {out.path}: {exc}")
            problems += 1
            continue
        if size != out.size:
            print(f"SIZE    {out.path}: {size[0]}x{size[1]} (expected {expected})")
            problems += 1
        elif pixels != out.draw().tobytes():
            print(f"STALE   {out.path}: differs from the generator; rerun without --check")
            problems += 1
        else:
            print(f"ok      {out.path} {expected}")
    print("OK" if problems == 0 else f"FAILED: {problems} problem(s)")
    return 0 if problems == 0 else 1


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="validate owned PNGs instead of writing them")
    args = parser.parse_args(argv)
    if args.check:
        return check()
    generate()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
