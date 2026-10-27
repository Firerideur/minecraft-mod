"""HD pass for entity textures.

Faces are first painted at 1 texel per model pixel (the shapes, colors and features), then upscaled
SCALE times and given surface detail at the higher resolution: fur strands, scales, stone grain and cracks,
chitin gloss, brushed metal, cloth folds, wood grain, leaves, bone, skin... plus a sculpted bevel on every
face and a highlight in glowing eyes. The model geometry doesn't change: Minecraft maps UVs relative to the
declared texture size, so a texture SCALE times bigger simply has more detail.
"""
import numpy as np

SCALE = 4

PATTERNS = ("fur", "scales", "stone", "chitin", "metal", "cloth", "wood", "leaf", "bone", "skin", "crystal",
            "membrane", "smooth")


def _noise(h, w, seed):
    rng = np.random.default_rng(seed)
    return rng.random((h, w))


def _smooth_noise(h, w, seed, cell):
    """Blocky value noise smoothed by bilinear upsampling (cell = texels per noise cell)."""
    gh, gw = h // cell + 2, w // cell + 2
    grid = _noise(gh, gw, seed)
    ys = np.arange(h) / cell
    xs = np.arange(w) / cell
    y0 = ys.astype(int)
    x0 = xs.astype(int)
    fy = (ys - y0)[:, None]
    fx = (xs - x0)[None, :]
    a = grid[y0][:, x0]
    b = grid[y0][:, x0 + 1]
    c = grid[y0 + 1][:, x0]
    d = grid[y0 + 1][:, x0 + 1]
    return a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy


def _crack_mask(h, w, seed, count):
    mask = np.zeros((h, w), dtype=bool)
    rng = np.random.default_rng(seed)
    for _ in range(count):
        x, y = rng.integers(0, w), rng.integers(0, h)
        for _ in range(rng.integers(h // 2 + 2, h + w)):
            if 0 <= x < w and 0 <= y < h:
                mask[y, x] = True
            step = rng.integers(0, 4)
            x += (-1, 1, 0, 0)[step] if rng.random() < 0.5 else 0
            y += (0, 0, 1, -1)[step] if rng.random() < 0.7 else 1
    return mask


def shading(pattern, h, w, side, seed):
    """Brightness multiplier (h x w) for a face of this pattern, at high resolution."""
    s = SCALE
    yy, xx = np.mgrid[0:h, 0:w]
    fine = _noise(h, w, seed)
    m = np.ones((h, w))
    if pattern == "fur":
        strands = ((xx + (yy // 2) * 3 + (fine * 3).astype(int)) % 5 == 0)
        m *= 0.9 + 0.2 * _smooth_noise(h, w, seed + 1, s)
        m[strands] *= 0.78
        m[fine > 0.93] *= 1.25
    elif pattern == "scales":
        row = yy // 3
        cx = (xx + (row % 2) * 2) % 4
        cy = yy % 3
        m *= np.where(cy == 2, 0.72, np.where((cy == 0) & (cx < 2), 1.18, 1.0))
        m *= 0.94 + 0.12 * fine
    elif pattern == "stone":
        m *= 0.82 + 0.3 * _smooth_noise(h, w, seed + 2, s)
        m *= 0.92 + 0.16 * fine
        m[_crack_mask(h, w, seed + 3, max(1, (h * w) // 400))] *= 0.6
    elif pattern == "chitin":
        grad = 1.25 - 0.45 * (yy / max(1, h - 1)) if side else np.full((h, w), 1.08)
        m *= grad
        band = (yy % (s * 3) == 0) & side
        m[band] *= 0.7
        m *= 0.96 + 0.08 * fine
    elif pattern == "metal":
        streak = _noise(1, w, seed + 4)[0]
        m *= 0.9 + 0.2 * streak[None, :]
        m *= 0.97 + 0.06 * fine
        for (ry, rx) in ((s, s), (s, w - s - 1), (h - s - 1, s), (h - s - 1, w - s - 1)):
            if 0 <= ry < h and 0 <= rx < w and h > 3 * s and w > 3 * s:
                m[ry, rx] *= 1.5
                if ry + 1 < h and rx + 1 < w:
                    m[ry + 1, rx + 1] *= 0.6
    elif pattern == "cloth":
        folds = 0.88 + 0.12 * np.sin(xx * (np.pi / (s * 1.5)) + _smooth_noise(h, w, seed + 5, s * 2) * 2)
        m *= folds if side else 1.0
        m *= 0.95 + 0.1 * ((xx + yy) % 2) * fine
    elif pattern == "wood":
        grain = np.sin(xx * 0.9 + np.sin(yy * 0.15 + seed) * 2.0)
        m *= 0.86 + 0.14 * (grain > 0.6) * -1 + 0.14
        m *= 0.92 + 0.16 * _smooth_noise(h, w, seed + 6, s)
        m[((xx + seed) % (s * 3) == 0)] *= 0.75
    elif pattern == "leaf":
        blobs = _smooth_noise(h, w, seed + 7, max(2, s // 2))
        m *= 0.7 + 0.6 * blobs
        m[fine > 0.9] *= 1.3
    elif pattern == "bone":
        m *= 0.94 + 0.12 * _smooth_noise(h, w, seed + 8, s)
        m[_crack_mask(h, w, seed + 9, max(1, (h * w) // 900))] *= 0.75
    elif pattern == "skin":
        m *= 0.93 + 0.14 * _smooth_noise(h, w, seed + 10, s)
        m *= 0.98 + 0.04 * fine
    elif pattern == "crystal":
        facets = ((xx + yy) % (s * 2) < 2) | ((xx - yy) % (s * 3) == 0)
        m *= 0.9 + 0.2 * _smooth_noise(h, w, seed + 11, s)
        m[facets] *= 1.3
    elif pattern == "membrane":
        veins = (xx % (s * 3) == 0) | (((xx + yy) % (s * 5)) == 0)
        m *= 0.92 + 0.12 * _smooth_noise(h, w, seed + 12, s)
        m[veins] *= 0.65
    else:  # smooth
        m *= 0.96 + 0.08 * fine
    # Sculpted bevel: light from the top left
    if h > 2 * s and w > 2 * s:
        m[0, :] *= 1.18
        m[:, 0] *= 1.1
        m[-1, :] *= 0.75
        m[:, -1] *= 0.85
    return m


def upscale_face(color, glow, pattern, side, seed):
    """color/glow: low-res lists of RGBA tuples. Returns (hi color array, hi glow array) uint8 (H, W, 4)."""
    c = np.array(color, dtype=np.float64)
    g = np.array(glow, dtype=np.float64)
    h, w = c.shape[0] * SCALE, c.shape[1] * SCALE
    c = c.repeat(SCALE, axis=0).repeat(SCALE, axis=1)
    g = g.repeat(SCALE, axis=0).repeat(SCALE, axis=1)
    glowing = g[:, :, 3] > 0
    m = shading(pattern, h, w, side, seed)
    # Glowing features (eyes, runes) keep their color, with a bright highlight
    m[glowing] = 1.0
    c[:, :, :3] = np.clip(c[:, :, :3] * m[:, :, None], 0, 255)
    low_glow = np.array(glow)[:, :, 3] > 0 if len(glow) and len(glow[0]) else np.zeros((0, 0), bool)
    for ly, lx in zip(*np.nonzero(low_glow)):
        y, x = ly * SCALE, lx * SCALE
        c[y + 1, x + 1, :3] = np.clip(c[y + 1, x + 1, :3] * 0.4 + 255 * 0.6, 0, 255)
        g[y + 1, x + 1, :3] = np.clip(g[y + 1, x + 1, :3] * 0.5 + 255 * 0.5, 0, 255)
    return c.astype(np.uint8), g.astype(np.uint8)


KEYWORDS = [
    (("leaf", "leaves", "foliage"), "leaf"),
    (("wing",), "membrane"),
    (("cape", "robe", "hood", "cloth", "loincloth", "rags", "tabard", "sleeve", "cuff", "mantle", "hair", "necklace"), "cloth"),
    (("crystal", "gem", "shard", "core", "staff_skull"), "crystal"),
    (("blade", "sword", "axe", "spear", "dagger", "bow", "handle", "grip", "guard", "tip", "latch", "jewel", "stinger",
      "fang", "tusk", "teeth", "tongue", "crown", "prong"), "smooth"),
    (("pauldron", "bracer", "greave", "shoulder", "shield", "plate", "belt", "brow", "jaw_plate"), "metal"),
    (("horn", "spike", "crest"), "bone"),
    (("club", "staff", "twig", "lid", "box"), "wood"),
]


def pattern_for(mob_default, overrides=None):
    """Picks the pattern of a texture from its name, falling back to the mob's main material."""

    def pick(tex):
        if overrides and tex in overrides:
            return overrides[tex]
        for words, pattern in KEYWORDS:
            if any(word in tex for word in words):
                return pattern
        return mob_default

    return pick
