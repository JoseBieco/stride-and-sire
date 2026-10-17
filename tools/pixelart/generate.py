#!/usr/bin/env python3
"""Deterministic pixel-art generator for Stride & Sire textures.

Usage:
    python tools/pixelart/generate.py           # (re)generate every owned PNG
    python tools/pixelart/generate.py --check   # validate owned PNGs, exit 1 on any problem

All art is drawn from hand-written pixel maps and fixed palettes (no randomness), so running
the script twice produces byte-identical files. Every file the script writes is listed in
MANIFEST; add new outputs there. `--preview DIR` writes debug previews of the 3D saddlebag
textures (never commit them).
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
# 3D saddlebag model textures (client.SaddlebagModel)
# ---------------------------------------------------------------------------

# Must equal SaddlebagRendering.MODEL_TEXTURE_WIDTH/HEIGHT (pinned by AssetCompletenessTest).
BAG_MODEL_TEXTURE_SIZE = (32, 16)

# Faces of the two boxes per bag, keyed by their top-left texel in the standard box unwrap
# (d = z-length, w = x-thickness, h = height; top at (u+d, v), bottom at (u+d+w, v), then
# inner/front/outer/back side faces in the row v+d). In side faces the left column is the
# front (toward the head) and the top row is the top; the right bag is mirrored, so both
# outer faces read the same texels.
# The under-chest variant (chested Donkey/Mule) is the same boxes moved down: same texels.
#
# Bag box: texOffs(0, 0), 2 (w) x 5 (h) x 6 (d).
BAG_MODEL_FACES: dict[tuple[int, int], list[str]] = {
    (6, 0): ["ll"] * 6,                       # top (2 x 6, x inner->outer by z)
    (8, 0): ["cc"] + ["dd"] * 4 + ["cc"],     # bottom: metal corner caps at both ends
    (0, 6): ["kkkkkk"] * 5,                   # inner face, against the body
    (6, 6): ["lm", "lm", "md", "md", "cW"],   # front
    (8, 6): [                                 # outer face; rows 0-2 sit under the flap
        "mmmmmm",
        "mmmmmm",
        "kkkkkk",                              # shadow cast by the flap
        "lmmmmd",
        "Wddddc",                              # bottom edge with corner caps (lit front corner)
    ],
    (14, 6): ["ml", "ml", "dm", "dm", "Cc"],  # back
}
# Flap box: texOffs(16, 0), 1 (w) x 3 (h) x 6 (d), deformed to 0.5 thick.
FLAP_MODEL_FACES: dict[tuple[int, int], list[str]] = {
    (22, 0): ["L"] * 6,                       # top fold
    (23, 0): ["k"] * 6,                       # underside
    (16, 6): ["kkkkkk"] * 3,                  # inner face, against the bag
    (22, 6): ["L", "m", "C"],                 # front edge, metal-tipped corner
    (23, 6): [                                # outer face
        "LLLLLL",
        "lmmmmd",
        "WsBbsC",                              # hem stitching, buckle, metal corner tips (lit front tip)
    ],
    (29, 6): ["L", "m", "C"],                 # back edge
}


def draw_saddlebag_model(tier: str) -> Image.Image:
    palette = {**LEATHER, **METALS[tier]}
    image = Image.new("RGBA", BAG_MODEL_TEXTURE_SIZE, TRANSPARENT)
    for faces in (BAG_MODEL_FACES, FLAP_MODEL_FACES):
        for (u, v), rows in faces.items():
            image.paste(render_map(rows, palette), (u, v))
    return image


def draw_saddlebag_model_preview(tier: str) -> Image.Image:
    """Debug only (never committed): the texture atlas x8 next to the bag as seen from the side
    (outer face + flap, in quarter-pixel samples; the under-chest variant uses the same texels) on a horse-brown flank, x8."""
    tex = draw_saddlebag_model(tier)
    q = 4  # samples per model pixel
    z0, z1, y0, y1 = -1.0, 7.0, -7.0, 0.0  # body-local window around the bag
    side = Image.new("RGBA", (int((z1 - z0) * q), int((y1 - y0) * q)), hex_rgba("#6b4a2f"))
    for sy in range(side.height):
        for sx in range(side.width):
            z, y = z0 + (sx + 0.5) / q, y0 + (sy + 0.5) / q
            if -0.25 <= z < 6.25 and -6.25 <= y < -3.25:   # flap outer face: 6 texels over 6.5 px
                texel = (23 + int((z + 0.25) / 6.5 * 6), 6 + int(y + 6.25))
            elif 0 <= z < 6 and -6 <= y < -1:              # bag outer face
                texel = (8 + int(z), 6 + int(y + 6))
            else:
                continue
            side.putpixel((sx, sy), tex.getpixel(texel))
    atlas = tex.resize((tex.width * 8, tex.height * 8), Image.NEAREST)
    side = side.resize((side.width * 2 * 8 // q * 2, side.height * 2 * 8 // q * 2), Image.NEAREST)
    out = Image.new("RGBA", (atlas.width + side.width + 16, max(atlas.height, side.height)), hex_rgba("#cfcfcf"))
    out.paste(atlas, (0, 0), atlas)
    out.paste(side, (atlas.width + 16, 0))
    return out


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
    *[Output(f"entity/saddlebag/{tier}.png", BAG_MODEL_TEXTURE_SIZE, (lambda t=tier: draw_saddlebag_model(t)))
      for tier in ("leather", "iron", "gold", "diamond", "netherite")],
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
    parser.add_argument("--preview", metavar="DIR", type=Path,
                        help="write debug previews of the 3D saddlebag textures to DIR (outside the repo) and exit")
    args = parser.parse_args(argv)
    if args.check:
        return check()
    if args.preview:
        args.preview.mkdir(parents=True, exist_ok=True)
        for tier in METALS:
            draw_saddlebag_model_preview(tier).save(args.preview / f"saddlebag_model_{tier}.png")
            print(f"preview {args.preview / f'saddlebag_model_{tier}.png'}")
        return 0
    generate()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
