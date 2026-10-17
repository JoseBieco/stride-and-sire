#!/usr/bin/env python3
"""Deterministic pixel-art generator for Stride & Sire textures.

Usage:
    python tools/pixelart/generate.py           # (re)generate every owned PNG
    python tools/pixelart/generate.py --check   # validate owned PNGs, exit 1 on any problem

All art is drawn from hand-written pixel maps, fixed palettes and seeded integer-hash noise (no
`random`), so running the script twice produces byte-identical files. Every file the script writes
is listed in MANIFEST; add new outputs there. `--preview DIR` writes debug previews of the 3D
saddlebag textures and the exclusive horse coats (never commit them).
"""
from __future__ import annotations

import argparse
import math
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
# 64x64 horse coat textures (Fase D recessive coats)
# ---------------------------------------------------------------------------

HORSE_COAT_TEXTURE_SIZE = (64, 64)

RGB = tuple[float, float, float]
Rect = tuple[int, int, int, int]  # x, y, width, height


@dataclass(frozen=True)
class CoatBox:
    """One adult-horse cube of AbstractEquineModel.createBodyMesh (MC 26.3): texOffs(u, v), box w (x) x h (y) x d (z)."""
    name: str
    u: int
    v: int
    w: int
    h: int
    d: int

    def faces(self) -> dict[str, Rect]:
        """Minecraft box-UV unfolding (ModelPart.Cube) as (x, y, width, height) per face.

        Row v: top at u+d and bottom at u+d+w, each w x d; in both, the last row (v+d-1) is the front (-z) edge.
        Row v+d (height h, top row = top of the cube): west at u (d wide, last column touches the front), north/front at
        u+d (w wide), east at u+d+w (d wide, first column touches the front), south/back at u+2d+w (w wide).
        """
        u, v, w, h, d = self.u, self.v, self.w, self.h, self.d
        return {
            "top": (u + d, v, w, d),
            "bottom": (u + d + w, v, w, d),
            "west": (u, v + d, d, h),
            "north": (u + d, v + d, w, h),
            "east": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h),
        }


# Every cube of the adult horse (HorseModel = AbstractEquineModel.createBodyMesh; 26.3 has no lower mouth / bridle cubes).
HORSE_BOXES: dict[str, CoatBox] = {box.name: box for box in (
    CoatBox("body", 0, 32, 10, 10, 22),        # texOffs(0, 32)  10 x 10 x 22
    CoatBox("neck", 0, 35, 4, 12, 7),          # head_parts: texOffs(0, 35) 4 x 12 x 7
    CoatBox("head", 0, 13, 6, 5, 7),           # texOffs(0, 13)  6 x 5 x 7
    CoatBox("upper_mouth", 0, 25, 4, 5, 5),    # texOffs(0, 25)  4 x 5 x 5
    CoatBox("ear", 19, 16, 2, 3, 1),           # left_ear and right_ear share texOffs(19, 16) 2 x 3 x 1
    CoatBox("mane", 56, 36, 2, 16, 2),         # texOffs(56, 36) 2 x 16 x 2
    CoatBox("tail", 42, 36, 3, 14, 4),         # texOffs(42, 36) 3 x 14 x 4
    CoatBox("leg", 48, 21, 4, 11, 4),          # all four legs share texOffs(48, 21) 4 x 11 x 4 (left legs mirrored)
)}

# Eye cells on the head side faces (west x 0..6 has its front column at x 6, east x 13..19 at x 13), two texels behind
# the front edge like vanilla: (x, y, role). The east eye mirrors the west one (x -> 19 - x).
_EYE_WEST = ((3, 21, "glint"), (4, 21, "iris"), (3, 22, "iris_dark"), (4, 22, "iris_dark"))
HORSE_EYES = _EYE_WEST + tuple((19 - x, y, role) for x, y, role in _EYE_WEST)


@dataclass(frozen=True)
class CoatPalette:
    coat: str
    muzzle: str
    nostril: str
    ear_inner: str
    iris: str
    iris_dark: str
    glint: str
    mane: tuple[str, str, str]           # dark, mid, light strands
    lower_leg: str
    fetlock: str
    hoof_rim: str
    hoof: str
    noise_rel: float                     # multiplicative hair noise amplitude
    noise_abs: float                     # additive hair noise amplitude (keeps near-black coats from looking flat)
    sheen: str | None = None             # cool highlight added along the back
    dapple: str | None = None            # dapple centre colour; the ring blends halfway to it
    dapple_net: float = 1.0              # multiplier for the darker network between dapples
    shade_k: float = 1.0                 # strength of the top-lit face shading (pale coats need less)


COAT_PALETTES: dict[str, CoatPalette] = {
    # Preto Absoluto: blue-black with a cool sheen along the back.
    "obsidian": CoatPalette(
        coat="#1a1b24", muzzle="#121218", nostril="#060609", ear_inner="#0c0c11",
        iris="#6a3e1f", iris_dark="#4a2a14", glint="#d8d2c8",
        mane=("#08080b", "#13141b", "#262a38"),
        lower_leg="#15151d", fetlock="#0d0d12", hoof_rim="#4a4950", hoof="#38373d",
        noise_rel=0.10, noise_abs=3.0, sheen="#2a3a5c"),
    # Albino: cream-white, pink skin showing at the muzzle and ears, red-pink eyes.
    "albino": CoatPalette(
        coat="#f2ede0", muzzle="#ecbcb6", nostril="#c7848a", ear_inner="#efb1b4",
        iris="#d0445e", iris_dark="#92263c", glint="#ffffff",
        mane=("#dcd4c2", "#f1ece0", "#fffdf8"),
        lower_leg="#ece5d4", fetlock="#e4dcc6", hoof_rim="#d9c6a6", hoof="#c4ad8a",
        noise_rel=0.03, noise_abs=0.0, shade_k=0.55),
    # Pangare Prateado: chocolate-grey with round lighter dapples, dark lower legs, silver-flaxen mane and tail.
    "silver_dapple": CoatPalette(
        coat="#625249", muzzle="#463a34", nostril="#211a17", ear_inner="#3a2f2a",
        iris="#2b1e18", iris_dark="#140e0b", glint="#e6e0d6",
        mane=("#b2aa9c", "#d6d0c2", "#f2eee4"),
        lower_leg="#3c312c", fetlock="#2f2622", hoof_rim="#4b4441", hoof="#282321",
        noise_rel=0.05, noise_abs=1.5, dapple="#a08f83", dapple_net=0.92),
}


def _hash01(x: int, y: int, seed: int) -> float:
    """Deterministic integer hash noise in [0, 1)."""
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n ^= n >> 16
    return (n & 0xFFFFFF) / float(0x1000000)


def _hair(x: int, y: int, seed: int) -> float:
    """Hair-like noise in [-1, 1]: per-texel grain plus short vertical streaks."""
    return 0.6 * (2 * _hash01(x, y, seed) - 1) + 0.4 * (2 * _hash01(x, y // 3, seed + 7) - 1)


def _rgb(value: str) -> RGB:
    r, g, b, _ = hex_rgba(value)
    return (float(r), float(g), float(b))


def _mix(a: RGB, b: RGB, t: float) -> RGB:
    t = max(0.0, min(1.0, t))
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t)


def _scale(c: RGB, k: float) -> RGB:
    return (c[0] * k, c[1] * k, c[2] * k)


def _add(c: RGB, k: RGB, t: float = 1.0) -> RGB:
    return (c[0] + k[0] * t, c[1] + k[1] * t, c[2] + k[2] * t)


def _lit(c: RGB, light: float) -> RGB:
    """Apply a light factor: multiply when darkening; when brightening, lift proportionally to the headroom left, so
    near-white coats get a highlight instead of clipping flat to #ffffff."""
    if light <= 1:
        return _scale(c, light)
    return tuple(v + (light - 1) * 1.6 * min(v, 255 - v) for v in c)  # type: ignore[return-value]


def _to_rgba(c: RGB) -> RGBA:
    r, g, b = (max(0, min(255, int(round(v)))) for v in c)
    return (r, g, b, 255)


# Lit from above: tops brighter, bottoms darker, sides fade from (1 + 0.05) at the top row by `gradient` towards the bottom.
_SIDE_GRADIENT = {"body": 0.30, "neck": 0.18, "head": 0.07, "upper_mouth": 0.10, "ear": 0.10,
                  "mane": 0.12, "tail": 0.16, "leg": 0.14}
_FACE_TINT = {"top": 1.12, "bottom": 0.70, "west": 1.0, "east": 1.0, "north": 0.95, "south": 0.95}
_NOISE_SCALE = {"head": 0.55, "upper_mouth": 0.6, "ear": 0.6}


def _light(part: str, face: str, fy: float, k: float = 1.0) -> float:
    if face in ("top", "bottom"):
        light = _FACE_TINT[face]
    else:
        light = _FACE_TINT[face] * (1.05 - _SIDE_GRADIENT[part] * fy)
    return 1 + (light - 1) * k


def _dapple_centres(rect: Rect) -> list[tuple[int, int]]:
    """Staggered, hash-jittered dapple centres in face-local coordinates."""
    x0, y0, w, h = rect
    centres = []
    for gy, cy in enumerate(range(1, h, 4)):
        start = 1 + (2 if gy % 2 else 0)
        for cx in range(start, w, 5):
            jx = int(_hash01(x0 + cx, y0 + cy, 91) * 3) - 1
            jy = int(_hash01(x0 + cx, y0 + cy, 92) * 2)
            centres.append((cx + jx, cy + jy))
    return centres


def _dapple_weight(centres: list[tuple[int, int]], col: int, row: int) -> float:
    best = 9.0
    for cx, cy in centres:
        best = min(best, (cx - col) ** 2 + (cy - row) ** 2)
    if best == 0:
        return 1.0       # light centre
    if best == 1:
        return 0.6       # ring
    if best == 2:
        return 0.3       # soft corners -> round spot
    return 0.0


def _strand(pal: CoatPalette, x: int, y: int, seed: int) -> RGB:
    """2-3-tone mane/tail strands running along the hair (vertical in UV), broken every few texels."""
    seg = (y + int(_hash01(x, 0, seed) * 5)) // 4
    t = _hash01(x, seg, seed + 1)
    dark, mid, light = (_rgb(c) for c in pal.mane)
    return mid if t < 0.42 else dark if t < 0.77 else light


def draw_horse_coat(coat: str) -> Image.Image:
    """Draw one exclusive adult-horse coat face by face on the MC 26.3 horse box-UV layout (HORSE_BOXES).

    Texels no cube reads are filled with the plain coat colour so the texture stays fully opaque.
    """
    pal = COAT_PALETTES[coat]
    seed = {"obsidian": 11, "albino": 23, "silver_dapple": 37}[coat]
    base = _rgb(pal.coat)
    image = Image.new("RGBA", HORSE_COAT_TEXTURE_SIZE, _to_rgba(base))
    px = image.load()
    claimed: set[tuple[int, int]] = set()
    dapple_faces = {("body", "top"), ("body", "west"), ("body", "east"), ("body", "south"),
                    ("neck", "west"), ("neck", "east")}

    for box in HORSE_BOXES.values():
        for face, rect in box.faces().items():
            x0, y0, w, h = rect
            centres = _dapple_centres(rect) if pal.dapple and (box.name, face) in dapple_faces else []
            for row in range(h):
                for col in range(w):
                    x, y = x0 + col, y0 + row
                    assert (x, y) not in claimed, f"UV overlap at {x},{y} ({box.name} {face})"
                    claimed.add((x, y))
                    fy = row / (h - 1) if h > 1 else 0.0
                    front = col / (w - 1) if face == "west" and w > 1 else (
                        (w - 1 - col) / (w - 1) if face == "east" and w > 1 else 0.0)
                    colour = _texel(pal, box, face, rect, col, row, fy, front, centres, base, x, y, seed)
                    px[x, y] = _to_rgba(colour)

    for x, y, role in HORSE_EYES:
        px[x, y] = _to_rgba(_rgb(getattr(pal, role)))
    return image


def _texel(pal: CoatPalette, box: CoatBox, face: str, rect: Rect, col: int, row: int, fy: float, front: float,
           centres: list[tuple[int, int]], base: RGB, x: int, y: int, seed: int) -> RGB:
    part = box.name
    if part in ("mane", "tail"):
        colour = _strand(pal, x, y, seed + (0 if part == "mane" else 50))
        if part == "tail" and face not in ("top", "bottom") and row >= box.h - 3:
            colour = _mix(colour, _rgb(pal.mane[2]), 0.25)  # wispy tip
        light = _light(part, face, fy, pal.shade_k) if face not in ("top", "bottom") else 1.02
        return _add(_lit(colour, light * (1 + 0.5 * pal.noise_rel * _hair(x, y, seed + 3))), (1, 1, 1),
                    pal.noise_abs * 0.5 * _hair(y, x, seed + 4))

    colour = base
    if part == "leg":
        if face == "bottom":
            colour = _scale(_rgb(pal.hoof), 0.8 if 0 < col < 3 and 0 < row < 3 else 0.92)    # sole, darker frog
            return _add(colour, (1, 1, 1), 2 * (2 * _hash01(x, y, seed) - 1))
        if face != "top":
            if row >= box.h - 2:      # hoof: coronet rim row, then the hoof wall with vertical streaks
                colour = _rgb(pal.hoof_rim if row == box.h - 2 else pal.hoof)
                return _scale(colour, (0.97 if face in ("north", "south") else 1.0) * (1 + 0.08 * (2 * _hash01(x, 0, seed + 5) - 1)))
            colour = _mix(base, _rgb(pal.lower_leg), (row - 2) / 5)
            if row >= box.h - 4:      # fetlock band just above the hoof
                colour = _mix(colour, _rgb(pal.fetlock), 0.85)
    elif part == "upper_mouth":
        if face == "north":
            m = 1.0
        elif face == "bottom":
            m = 0.85
        elif face == "top":
            m = 0.75 * row / (box.d - 1)
        elif face in ("west", "east"):
            m = 0.85 * front ** 1.5 + 0.25 * fy
        else:
            m = 0.0
        colour = _mix(base, _rgb(pal.muzzle), m)
    elif part == "ear" and face == "north" and row >= 1:
        colour = _rgb(pal.ear_inner)  # inner ear faces forward

    if centres:
        w = _dapple_weight(centres, col, row)
        if face in ("west", "east"):
            w *= max(0.0, min(1.0, 1.35 - 1.5 * fy))  # dapples fade out towards the belly
        colour = _scale(colour, pal.dapple_net) if w == 0 else _mix(_scale(colour, pal.dapple_net), _rgb(pal.dapple), w)

    light = _light(part, face, fy, pal.shade_k)
    noise = _hair(x, y, seed) * _NOISE_SCALE.get(part, 1.0)
    colour = _add(_lit(colour, light * (1 + pal.noise_rel * noise)), (1, 1, 1), pal.noise_abs * noise)

    if pal.sheen and part in ("body", "neck", "head"):
        if face == "top":
            spine = 1 - abs(col - (box.w - 1) / 2) / box.w   # brightest along the spine
            colour = _add(colour, _rgb(pal.sheen), 0.75 * spine)
        elif face in ("west", "east", "south") and part != "head":
            colour = _add(colour, _rgb(pal.sheen), max(0.0, 0.55 - 1.6 * fy))

    if part == "upper_mouth":
        nostril = _rgb(pal.nostril)
        if face == "north" and row in (1, 2) and col in (0, box.w - 1):
            colour = _mix(colour, nostril, 0.9 if row == 1 else 0.65)
        elif face == "north" and row == 3:
            colour = _mix(colour, nostril, 0.45)                   # lip line
        elif face in ("west", "east") and row == 3 and front > 0.6:
            colour = _mix(colour, nostril, 0.35)                   # corner of the mouth
    return colour


def draw_horse_coat_obsidian() -> Image.Image:
    """Preto Absoluto: blue-black coat with a cool sheen along the back, dark brown eyes, near-black mane and tail."""
    return draw_horse_coat("obsidian")


def draw_horse_coat_albino() -> Image.Image:
    """Albino: cream-white coat, pink skin at muzzle and inner ears, red-pink eyes, white mane and tail, pale hooves."""
    return draw_horse_coat("albino")


def draw_horse_coat_silver_dapple() -> Image.Image:
    """Pangare Prateado: chocolate-grey with round dapples, darker lower legs, silver-flaxen mane and tail."""
    return draw_horse_coat("silver_dapple")


# Side-view preview: (box, part pivot (y, z), x rotation, box origin (y, z) in part space), back to front.
_PREVIEW_PARTS = (
    ("tail", (6.0, 7.0), math.pi / 6, (0.0, 0.0)),
    ("body", (11.0, 5.0), 0.0, (-8.0, -17.0)),
    ("leg", (14.0, 7.0), 0.0, (-1.01, -1.0)),
    ("leg", (14.0, -10.0), 0.0, (-1.01, -1.9)),
    ("mane", (4.0, -12.0), math.pi / 6, (-11.0, 5.01)),
    ("neck", (4.0, -12.0), math.pi / 6, (-6.0, -2.0)),
    ("head", (4.0, -12.0), math.pi / 6, (-11.0, -2.0)),
    ("upper_mouth", (4.0, -12.0), math.pi / 6, (-11.0, -7.0)),
    ("ear", (4.0, -12.0), math.pi / 6, (-13.0, 4.0)),
)


def draw_horse_coat_preview(coat: str) -> Image.Image:
    """Debug only (never committed): the atlas x8 with every face outlined, next to an orthographic side view (east faces,
    neck/tail tilted as in the model), x8."""
    tex = draw_horse_coat(coat)
    scale = 8
    atlas = tex.resize((64 * scale, 64 * scale), Image.NEAREST)
    apx = atlas.load()
    for box in HORSE_BOXES.values():
        for x0, y0, w, h in box.faces().values():
            for i in range(w * scale):
                apx[x0 * scale + i, y0 * scale] = (255, 0, 255, 255)
                apx[x0 * scale + i, (y0 + h) * scale - 1] = (255, 0, 255, 255)
            for j in range(h * scale):
                apx[x0 * scale, y0 * scale + j] = (255, 0, 255, 255)
                apx[(x0 + w) * scale - 1, y0 * scale + j] = (255, 0, 255, 255)

    q = 4  # samples per model unit
    z_min, z_max, y_min, y_max = -22.0, 16.0, -13.0, 25.0
    side = Image.new("RGBA", (int((z_max - z_min) * q), int((y_max - y_min) * q)), hex_rgba("#8fb7d9"))
    spx = side.load()
    for sy in range(side.height):
        for sx in range(side.width):
            wz, wy = z_min + (sx + 0.5) / q, y_min + (sy + 0.5) / q
            for name, (py, pz), rot, (oy, oz) in _PREVIEW_PARTS:
                box = HORSE_BOXES[name]
                c, s = math.cos(rot), math.sin(rot)
                dy, dz = wy - py, wz - pz
                ly, lz = dy * c + dz * s, -dy * s + dz * c
                if oy <= ly < oy + box.h and oz <= lz < oz + box.d:
                    ex, ey, _, _ = box.faces()["east"]
                    spx[sx, sy] = tex.getpixel((ex + int(lz - oz), ey + int(ly - oy)))
    side = side.resize((side.width * scale // q * 2, side.height * scale // q * 2), Image.NEAREST)
    out = Image.new("RGBA", (atlas.width + side.width + 16, max(atlas.height, side.height)), hex_rgba("#cfcfcf"))
    out.paste(atlas, (0, 0))
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
    Output("entity/horse/obsidian.png", HORSE_COAT_TEXTURE_SIZE, draw_horse_coat_obsidian),
    Output("entity/horse/albino.png", HORSE_COAT_TEXTURE_SIZE, draw_horse_coat_albino),
    Output("entity/horse/silver_dapple.png", HORSE_COAT_TEXTURE_SIZE, draw_horse_coat_silver_dapple),
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
                        help="write debug previews of the 3D saddlebag textures and horse coats to DIR (outside the repo) and exit")
    args = parser.parse_args(argv)
    if args.check:
        return check()
    if args.preview:
        args.preview.mkdir(parents=True, exist_ok=True)
        for tier in METALS:
            draw_saddlebag_model_preview(tier).save(args.preview / f"saddlebag_model_{tier}.png")
            print(f"preview {args.preview / f'saddlebag_model_{tier}.png'}")
        for coat in COAT_PALETTES:
            draw_horse_coat_preview(coat).save(args.preview / f"horse_coat_{coat}.png")
            print(f"preview {args.preview / f'horse_coat_{coat}.png'}")
        return 0
    generate()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
