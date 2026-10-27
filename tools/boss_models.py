"""Boss models: geometry and textures described once, written twice.

Each boss is a tree of parts with cubes in absolute model coordinates (y points down, the ground is y = 24).
From that description this script
  - packs every cube's UV net into an atlas and paints it face by face (base + emissive "glow" texture),
  - writes the matching Java LayerDefinitions (BossGeometry.java), so texture and model can't drift apart.

Run through generate_resources.py, or directly: python3 tools/boss_models.py
"""
import math
from pathlib import Path

from PIL import Image

import hd

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "arcanequest"
JAVA = ROOT / "src" / "main" / "java" / "com" / "firerideur" / "arcanequest" / "client" / "boss" / "BossGeometry.java"

CLEAR = (0, 0, 0, 0)


# ---------------------------------------------------------------------------
# Geometry description
# ---------------------------------------------------------------------------
class Cube:
    def __init__(self, tex, origin, size, mirror=False, inflate=0.0):
        self.tex, self.origin, self.size, self.mirror, self.inflate = tex, origin, size, mirror, inflate


class Part:
    def __init__(self, name, pivot=(0, 0, 0), cubes=(), children=(), rot=(0, 0, 0)):
        self.name, self.pivot, self.cubes, self.children, self.rot = name, pivot, list(cubes), list(children), rot


def mirror_x(origin, size):
    """Origin of the same cube on the other side of x = 0."""
    return (-origin[0] - size[0], origin[1], origin[2])


# ---------------------------------------------------------------------------
# Pixel helpers
# ---------------------------------------------------------------------------
def clamp(v):
    return max(0, min(255, int(round(v))))


def mul(c, f):
    return tuple(clamp(x * f) for x in c[:3])


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(clamp(x + (y - x) * t) for x, y in zip(a[:3], b[:3]))


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def hash_noise(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


class Face:
    """One face of a cube's UV net, painted pixel by pixel."""

    def __init__(self, name, w, h, seed):
        self.name, self.w, self.h, self.seed = name, w, h, seed
        self.color = [[CLEAR] * w for _ in range(h)]
        self.glow = [[CLEAR] * w for _ in range(h)]

    @property
    def side(self):
        return self.name in ("north", "south", "east", "west")

    def n(self, x, y, salt=0):
        return hash_noise(x, y, self.seed * 31 + salt)

    def put(self, x, y, color, glow=None):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.color[y][x] = color if len(color) == 4 else rgba(color)
            if glow is not None:
                self.glow[y][x] = glow if len(glow) == 4 else rgba(glow)

    def get(self, x, y):
        return self.color[y][x]

    def fill(self, fn):
        for y in range(self.h):
            for x in range(self.w):
                self.put(x, y, fn(x, y))

    def border(self, x, y):
        return x == 0 or y == 0 or x == self.w - 1 or y == self.h - 1


def material(base, dark_edges=0.72, grain=0.16, gradient=0.18, salt=0):
    """Base fill: noisy, slightly lit from above, darker rim so each cube reads clearly."""

    def paint(face):
        def fn(x, y):
            f = 1.0 + (face.n(x, y, salt) - 0.5) * grain * 2
            if face.side and face.h > 1:
                f *= 1.0 + gradient * (0.5 - y / (face.h - 1))
            elif face.name == "down":
                f *= 0.8
            c = mul(base, f)
            if face.border(x, y) and face.w > 2 and face.h > 2:
                c = mul(c, dark_edges)
            return c

        face.fill(fn)

    return paint


def crack(face, color, glow, seed, length=None, start=None):
    """Random-walk vein across a face (used for glowing cracks)."""
    rnd = hash_noise(seed, 3, face.seed)
    x = start[0] if start else int(rnd * face.w)
    y = start[1] if start else 0
    length = length or face.h
    for i in range(length):
        face.put(x, y, color, glow)
        step = hash_noise(i, seed, face.seed + 7)
        if step < 0.3:
            x -= 1
        elif step > 0.7:
            x += 1
        y += 1
        if not (0 <= x < face.w and 0 <= y < face.h):
            break


# ---------------------------------------------------------------------------
# Skeleton King
# ---------------------------------------------------------------------------
BONE = (222, 214, 188)
BONE_DARK = (150, 138, 112)
CAVITY = (26, 20, 28)
SOUL = (90, 230, 255)
GOLD = (236, 182, 52)
GOLD_DARK = (150, 100, 24)
RUBY = (220, 30, 50)
IRON = (62, 60, 72)
CRIMSON = (118, 20, 32)


def king_parts():
    def side(name, sign, extra=()):
        """Arm with bracer and pauldron, on the right (sign -1) or left (sign 1) of the model."""
        px = 5 * sign
        m = sign > 0

        def at(o, s):
            return mirror_x(o, s) if m else o

        return Part(name + "_arm", (px, 2, 0), [
            Cube("arm", at((-6.5, 0, -1.5), (3, 12, 3)), (3, 12, 3), m),
            Cube("bracer", at((-7, 6, -2), (4, 5, 4)), (4, 5, 4), m),
        ], [
            Part(name + "_pauldron", (px, 2, 0), [Cube("pauldron", at((-8, -1.5, -3), (5, 4, 6)), (5, 4, 6), m)],
                 rot=(0, 0, 0.18 * -sign)),
        ] + list(extra))

    def leg(name, sign):
        m = sign > 0

        def at(o, s):
            return mirror_x(o, s) if m else o

        return Part(name + "_leg", (2 * sign, 12, 0), [
            Cube("leg", at((-3.5, 12, -1.5), (3, 12, 3)), (3, 12, 3), m),
            Cube("greave", at((-4, 17, -2), (4, 5, 4)), (4, 5, 4), m),
        ])

    crown_spikes = [
        Cube("crown_spike", (-0.5, -13, -4.5), (1, 3, 1)),
        Cube("crown_spike_small", (-4.5, -12, -4.5), (1, 2, 1)),
        Cube("crown_spike_small", (3.5, -12, -4.5), (1, 2, 1)),
        Cube("crown_spike_small", (-4.5, -12, 3.5), (1, 2, 1)),
        Cube("crown_spike_small", (3.5, -12, 3.5), (1, 2, 1)),
        Cube("crown_spike", (-4.5, -13, -0.5), (1, 3, 1)),
        Cube("crown_spike", (3.5, -13, -0.5), (1, 3, 1)),
    ]
    head = Part("head", (0, 0, 0), [Cube("skull", (-4, -8, -4), (8, 7, 8))], [
        Part("jaw", (0, -1, 3), [Cube("jaw", (-3.5, -1, -3.8), (7, 2, 7))]),
        Part("crown", (0, -8, 0), [Cube("crown_band", (-4.5, -10, -4.5), (9, 2, 9)),
                                   Cube("crown_gem", (-1, -10, -5), (2, 2, 1))] + crown_spikes),
    ])
    waist = Part("waist", (0, 12, 0), [
        Cube("ribcage", (-4, 0, -2), (8, 8, 4)),
        Cube("spine", (-1, 8, -1), (2, 3, 2)),
        Cube("pelvis", (-3.5, 11, -1.5), (7, 2, 3)),
    ], [
        head,
        side("right", -1),
        side("left", 1),
        Part("cape", (0, 0.5, 2.2), [Cube("cape", (-5, 0.5, 2.2), (10, 20, 1))]),
        Part("tabard", (0, 11, -1.8), [Cube("tabard", (-3, 11, -2.3), (6, 10, 1))]),
    ])
    return Part("base", (0, 0, 0), [], [waist, leg("right", -1), leg("left", 1)])


def paint_king(tex, face):
    f = face
    if tex in ("skull", "jaw", "arm", "leg", "ribcage", "spine", "pelvis"):
        material(BONE, dark_edges=0.8, grain=0.1)(f)
    if tex == "skull":
        if f.name == "north":
            for x, y in ((1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3), (1, 4), (6, 4)):
                f.put(x, y, CAVITY)
            f.put(2, 3, mix(SOUL, (255, 255, 255), 0.4), (200, 250, 255))
            f.put(5, 3, mix(SOUL, (255, 255, 255), 0.4), (200, 250, 255))
            f.put(1, 3, CAVITY, rgba(SOUL, 140))
            f.put(6, 3, CAVITY, rgba(SOUL, 140))
            for x, y in ((3, 4), (4, 4), (3, 5), (4, 5)):
                f.put(x, y, CAVITY if y == 5 or x == 3 else mul(CAVITY, 1.4))
            for x in range(1, 7):
                f.put(x, 6, BONE if x % 2 else BONE_DARK)
            for x, y in ((0, 1), (7, 1), (0, 5), (7, 5)):
                f.put(x, y, BONE_DARK)
        if f.name == "up":
            for x, y in ((3, 1), (4, 2), (4, 3), (3, 4), (5, 5)):
                f.put(x, y, BONE_DARK)
        if f.name in ("east", "west"):
            f.put(3, 3, CAVITY)
            f.put(4, 3, mul(CAVITY, 1.5))
    elif tex == "jaw":
        if f.name == "north":
            for x in range(f.w):
                f.put(x, 0, BONE if x % 2 == 0 else CAVITY)
        if f.name == "down":
            f.fill(lambda x, y: mul(BONE_DARK, 0.8))
    elif tex == "crown_band":
        if f.name in ("up", "down"):
            f.fill(lambda x, y: rgba(GOLD if f.border(x, y) else (0, 0, 0), 255 if f.border(x, y) else 0))
            for x in range(f.w):
                f.put(x, 0, mix(GOLD, (255, 255, 255), 0.25) if f.name == "up" else GOLD_DARK)
        else:
            material(GOLD, dark_edges=0.75, grain=0.08, gradient=0.3)(f)
            for x in range(f.w):
                f.put(x, f.h - 1, GOLD_DARK)
            mid = f.w // 2
            gem = RUBY if f.name in ("east", "west") else (40, 90, 230)
            if f.name != "north":
                f.put(mid, 0, gem, rgba(gem, 120))
            f.put(1, 0, mix(GOLD, (255, 255, 255), 0.5))
    elif tex in ("crown_spike", "crown_spike_small"):
        material(GOLD, dark_edges=0.8, grain=0.06, gradient=0.4)(f)
        if f.side:
            f.put(0, 0, mix(GOLD, (255, 255, 255), 0.5))
    elif tex == "crown_gem":
        f.fill(lambda x, y: mix(RUBY, (255, 160, 170), 0.5 if (x + y) % 3 == 0 else 0.0))
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba(RUBY, 200)
    elif tex == "ribcage":
        if f.name == "north":
            f.fill(lambda x, y: CAVITY if y % 2 == 0 and 0 < y < 7 else BONE if y % 2 else mul(BONE, 0.9))
            for y in range(f.h):
                f.put(3, y, BONE)
                f.put(4, y, BONE_DARK if y % 2 == 0 else BONE)
            for x in range(f.w):
                f.put(x, 7, BONE_DARK)
            # Faint soul light behind the ribs
            for y in (2, 4):
                for x in (1, 2, 5, 6):
                    f.put(x, y, mix(CAVITY, SOUL, 0.25), rgba(SOUL, 70))
        elif f.name in ("east", "west"):
            f.fill(lambda x, y: BONE if y % 2 else CAVITY)
        elif f.name == "south":
            f.fill(lambda x, y: BONE if y % 2 else mul(CAVITY, 1.3))
            for y in range(f.h):
                f.put(3, y, BONE_DARK if y % 2 == 0 else BONE)
                f.put(4, y, BONE_DARK if y % 2 == 0 else BONE)
    elif tex == "spine":
        f.fill(lambda x, y: BONE if y % 2 == 0 else BONE_DARK)
    elif tex == "pelvis":
        if f.name == "north":
            f.put(1, 1, CAVITY)
            f.put(5, 1, CAVITY)
    elif tex in ("arm", "leg"):
        joint = 5 if tex == "arm" else 6
        for x in range(f.w):
            f.put(x, joint, mix(BONE, (255, 255, 255), 0.15))
            f.put(x, joint + 1, BONE_DARK)
        if f.side:
            for y in range(f.h):
                if y not in (joint, joint + 1) and f.w > 2:
                    f.put(f.w - 1, y, mul(f.get(f.w - 1, y), 0.85))
        if tex == "arm" and f.side:
            for x in range(f.w):
                f.put(x, 11, BONE_DARK if x % 2 else BONE)
                f.put(x, 10, BONE if x % 2 else BONE_DARK)
    elif tex in ("bracer", "greave"):
        material(IRON, grain=0.12)(f)
        if f.side:
            for x in range(f.w):
                f.put(x, 0, GOLD)
                f.put(x, f.h - 1, GOLD_DARK)
            f.put(1, 2, mix(GOLD, (255, 255, 255), 0.3))
            f.put(f.w - 2, 2, mix(GOLD, (255, 255, 255), 0.3))
    elif tex == "pauldron":
        material(IRON, grain=0.12, gradient=0.35)(f)
        if f.side:
            for x in range(f.w):
                f.put(x, f.h - 1, GOLD)
            f.put(f.w // 2, 1, GOLD)
        if f.name == "up":
            for y in range(f.h):
                f.put(f.w // 2, y, mix(IRON, (255, 255, 255), 0.2))
            for x in range(f.w):
                f.put(x, 0, GOLD_DARK)
                f.put(x, f.h - 1, GOLD_DARK)
            f.put(1, 1, GOLD)
            f.put(f.w - 2, f.h - 2, GOLD)
    elif tex == "cape":
        def cloth(x, y):
            fold = 0.82 + 0.18 * math.sin(x * 1.4 + 0.5)
            c = mul(CRIMSON, fold * (1.0 - 0.25 * y / f.h) * (0.92 + 0.16 * f.n(x, y)))
            return c if f.name == "south" else mul(c, 0.7)

        f.fill(cloth)
        if f.side:
            for x in range(f.w):
                f.put(x, 0, GOLD)
            for y in range(f.h):
                f.put(0, y, GOLD_DARK if f.name == "south" else f.get(0, y))
                f.put(f.w - 1, y, GOLD_DARK if f.name == "south" else f.get(f.w - 1, y))
            # Ragged hem
            for x in range(f.w):
                tear = int(f.n(x, 0, 5) * 3)
                for y in range(f.h - tear, f.h):
                    f.put(x, y, CLEAR)
            if f.name == "south":
                # Faded royal emblem: a gold crown
                for x, y in ((3, 4), (5, 3), (7, 4), (3, 5), (4, 5), (5, 5), (6, 5), (7, 5), (3, 6), (4, 6), (5, 6),
                             (6, 6), (7, 6)):
                    f.put(x - 0, y, mix(GOLD_DARK, CRIMSON, 0.35))
    elif tex == "tabard":
        f.fill(lambda x, y: mul(CRIMSON, 0.9 + 0.2 * f.n(x, y)))
        if f.side:
            for y in range(f.h):
                f.put(0, y, GOLD_DARK)
                f.put(f.w - 1, y, GOLD_DARK)
            for x in range(f.w):
                f.put(x, 0, GOLD)
            if f.name == "north":
                for x, y in ((2, 2), (3, 2), (1, 3), (2, 3), (3, 3), (4, 3), (2, 4), (3, 4), (2, 5), (3, 5)):
                    f.put(x, y, GOLD if y < 4 else mix(GOLD, RUBY, 0.6))
            for x in range(f.w):
                if x % 2:
                    f.put(x, f.h - 1, CLEAR)


# ---------------------------------------------------------------------------
# Corrupted Archmage
# ---------------------------------------------------------------------------
ROBE = (64, 30, 96)
ROBE_DARK = (38, 16, 58)
VOID = (12, 8, 18)
ARCANE = (226, 92, 255)
ARCANE_CYAN = (110, 230, 255)
SKIN = (150, 128, 168)
WOOD = (58, 36, 26)


def mage_parts():
    def arm(name, sign, extra=()):
        m = sign > 0

        def at(o, s):
            return mirror_x(o, s) if m else o

        return Part(name + "_arm", (5 * sign, 2, 0), [
            Cube("sleeve", at((-7, 0, -2), (4, 12, 4)), (4, 12, 4), m),
            Cube("cuff", at((-7.5, 6, -2.5), (5, 4, 5)), (5, 4, 5), m),
        ], list(extra))

    staff = Part("staff", (-5, 12, -1), [
        Cube("staff", (-5.5, -2, -1.5), (1, 24, 1)),
        Cube("staff_crystal", (-6.5, -7, -2.5), (3, 4, 3)),
        Cube("staff_prong", (-7, -5, -1.5), (1, 3, 1)),
        Cube("staff_prong", (-4, -5, -1.5), (1, 3, 1)),
    ])
    head = Part("head", (0, 0, 0), [
        Cube("face", (-4, -8, -4), (8, 8, 8)),
        Cube("hood", (-4.5, -8.6, -4.6), (9, 9, 9)),
    ], [Part("hood_tip", (0, -7, 4), [Cube("hood_tip", (-1.5, -8.5, 3.5), (3, 3, 4))], rot=(0.5, 0, 0))])
    waist = Part("waist", (0, 12, 0), [
        Cube("torso", (-4, 0, -3), (8, 12, 6)),
        Cube("mantle", (-5, -0.5, -3.5), (10, 4, 7)),
    ], [
        head,
        arm("right", -1, [staff]),
        arm("left", 1),
        Part("robe", (0, 12, 0), [
            Cube("robe_upper", (-4.5, 11, -3.5), (9, 6, 7)),
            Cube("robe_lower", (-5, 17, -4), (10, 7, 8)),
        ]),
    ])
    crystals = [Part(f"crystal{i}", (0, 2, 0), [Cube("crystal", (-1, 0, -1), (2, 4, 2))]) for i in range(3)]
    return Part("base", (0, 0, 0), [], [waist] + crystals)


def paint_mage(tex, face):
    f = face
    if tex == "face":
        f.fill(lambda x, y: mul(VOID, 1.0 + 0.4 * f.n(x, y)))
        if f.name == "north":
            for x in (1, 2, 5, 6):
                f.put(x, 4, mix(ARCANE, (255, 255, 255), 0.35), ARCANE)
            f.put(2, 5, mul(ARCANE, 0.4), rgba(ARCANE, 90))
            f.put(5, 5, mul(ARCANE, 0.4), rgba(ARCANE, 90))
    elif tex in ("hood", "hood_tip"):
        material(ROBE, grain=0.14, gradient=0.25)(f)
        if tex == "hood":
            if f.name == "north":
                for y in range(2, f.h):
                    for x in range(2, f.w - 2):
                        f.put(x, y, CLEAR)
                for y in range(1, f.h):
                    f.put(1, y, mix(GOLD, ROBE, 0.3))
                    f.put(f.w - 2, y, mix(GOLD, ROBE, 0.3))
                for x in range(1, f.w - 1):
                    f.put(x, 1, mix(GOLD, ROBE, 0.3))
            if f.name == "down":
                f.fill(lambda x, y: ROBE_DARK if f.border(x, y) else CLEAR)
            if f.name == "south":
                for y in range(f.h):
                    f.put(f.w // 2, y, ROBE_DARK)
    elif tex == "torso":
        material(ROBE, grain=0.12)(f)
        if f.name == "north":
            for y in range(f.h):
                f.put(3, y, GOLD_DARK)
                f.put(4, y, GOLD_DARK)
            for y in (2, 5, 9):
                f.put(3, y, ARCANE, ARCANE)
                f.put(4, y, ARCANE, ARCANE)
        if f.side:
            for x in range(f.w):
                f.put(x, 7, (44, 28, 20))
                f.put(x, 8, (36, 22, 16))
            if f.name == "north":
                f.put(3, 7, GOLD)
                f.put(4, 7, GOLD)
                f.put(3, 8, GOLD_DARK)
                f.put(4, 8, GOLD_DARK)
        if f.name == "south":
            for x, y in ((3, 2), (4, 2), (2, 3), (5, 3), (3, 4), (4, 4)):
                f.put(x, y, mul(ARCANE, 0.6), rgba(ARCANE, 150))
    elif tex == "mantle":
        material(ROBE_DARK, grain=0.25)(f)
        if f.side:
            for x in range(f.w):
                f.put(x, f.h - 1, GOLD_DARK)
        if f.name == "north":
            f.put(4, 1, GOLD)
            f.put(5, 1, GOLD)
            f.put(4, 2, mix(ARCANE, (255, 255, 255), 0.2), ARCANE)
            f.put(5, 2, GOLD_DARK)
        if f.name == "up":
            for x in range(2, f.w - 2):
                for y in range(2, f.h - 2):
                    f.put(x, y, mul(ROBE_DARK, 0.6))
    elif tex in ("robe_upper", "robe_lower"):
        def cloth(x, y):
            fold = 0.8 + 0.2 * math.sin(x * 1.2 + (0.0 if tex == "robe_upper" else 0.8))
            return mul(ROBE, fold * (0.92 + 0.16 * f.n(x, y)))

        f.fill(cloth)
        if f.side and tex == "robe_lower":
            for x in range(f.w):
                f.put(x, f.h - 3, GOLD_DARK)
                tear = int(f.n(x, 1, 9) * 2.5)
                for y in range(f.h - tear, f.h):
                    f.put(x, y, CLEAR)
            # Glowing runes on the hem
            for x in range(1, f.w - 1, 3):
                f.put(x, f.h - 5, mul(ARCANE_CYAN, 0.7), ARCANE_CYAN)
                f.put(x + 1, f.h - 4, mul(ARCANE_CYAN, 0.5), rgba(ARCANE_CYAN, 160))
        if f.name == "north" and tex == "robe_upper":
            for y in range(f.h):
                f.put(f.w // 2, y, GOLD_DARK)
        if f.name == "down":
            f.fill(lambda x, y: mul(VOID, 1.3))
    elif tex == "sleeve":
        material(ROBE, grain=0.12)(f)
        for y in range(9, f.h):
            for x in range(f.w):
                f.put(x, y, mul(SKIN, 0.9 + 0.2 * f.n(x, y)))
        if f.side:
            f.put(1, 10, mul(SKIN, 0.55))
            f.put(2, 11, mul(SKIN, 0.55))
            for x in range(f.w):
                if x % 2 == 0:
                    f.put(x, 11, mul(SKIN, 0.75))
        if f.name == "down":
            f.fill(lambda x, y: mul(SKIN, 0.8))
    elif tex == "cuff":
        material(ROBE_DARK, grain=0.12)(f)
        if f.side:
            for x in range(f.w):
                f.put(x, 0, GOLD_DARK)
                f.put(x, f.h - 1, GOLD)
            f.put(2, 2, mul(ARCANE, 0.6), rgba(ARCANE, 150))
        if f.name in ("down", "up"):
            f.fill(lambda x, y: ROBE_DARK if f.border(x, y) else CLEAR)
    elif tex == "staff":
        f.fill(lambda x, y: mul(WOOD, 0.85 + 0.3 * f.n(x, y)))
        for y in range(f.h):
            if y % 6 == 2:
                f.put(0, y, GOLD)
        f.put(0, 0, (180, 180, 190))
        f.put(0, 1, (120, 120, 130))
    elif tex == "staff_crystal":
        f.fill(lambda x, y: mix(ARCANE, ARCANE_CYAN, (x + y) / 6.0))
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba(mix(ARCANE, (255, 255, 255), 0.3), 230)
        f.put(1, 1, (255, 255, 255), (255, 255, 255))
    elif tex == "staff_prong":
        material(GOLD, grain=0.06, gradient=0.4)(f)
    elif tex == "crystal":
        f.fill(lambda x, y: mix(ARCANE_CYAN, ARCANE, y / max(1, f.h - 1)))
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba(mix(ARCANE_CYAN, ARCANE, y / max(1, f.h - 1)), 210)
        f.put(0, 0, (255, 255, 255), (255, 255, 255))


# ---------------------------------------------------------------------------
# Adamantite Golem
# ---------------------------------------------------------------------------
STEEL = (70, 64, 78)
STEEL_DARK = (40, 36, 46)
STEEL_LIGHT = (118, 110, 128)
ADAMANT = (40, 150, 255)
MAGMA = (90, 220, 255)


def golem_parts():
    def arm(name, sign):
        m = sign > 0

        def at(o, s):
            return mirror_x(o, s) if m else o

        shoulder_shards = [
            Part(f"{name}_shard{i}", (sign * (14 + i * 2), -10, -1 + i * 3),
                 [Cube("shard_small", (sign * (14 + i * 2) - 1, -15, -2 + i * 3), (2, 5, 2))],
                 rot=(0.25 * (i - 0.5), 0, -0.35 * sign))
            for i in range(2)
        ]
        return Part(name + "_arm", (12 * sign, -5, 0), [
            Cube("upper_arm", at((-16, -6, -3.5), (6, 13, 7)), (6, 13, 7), m),
        ], [
            Part(name + "_forearm", (13 * sign, 7, 0), [
                Cube("fist", at((-17, 7, -4.5), (8, 15, 9)), (8, 15, 9), m),
            ]),
            Part(name + "_shoulder", (13 * sign, -6, 0),
                 [Cube("shoulder", at((-17.5, -10, -4.5), (9, 5, 9)), (9, 5, 9), m)],
                 rot=(0, 0, 0.12 * -sign)),
        ] + shoulder_shards)

    def leg(name, sign):
        m = sign > 0

        def at(o, s):
            return mirror_x(o, s) if m else o

        return Part(name + "_leg", (5 * sign, 11, 0), [Cube("leg", at((-8.5, 11, -3.5), (7, 13, 7)), (7, 13, 7), m)])

    back_shards = []
    for i, (x, y, z, rx, rz, big) in enumerate((
            (-5, -8, 4, -0.5, -0.3, True), (2, -9, 5, -0.6, 0.25, True), (-1, -6, 6, -0.9, 0.0, False),
            (6, -5, 5, -0.7, 0.5, False), (-7, -3, 5, -0.8, -0.6, False))):
        tex, size = ("shard_big", (3, 8, 3)) if big else ("shard_small", (2, 5, 2))
        back_shards.append(Part(f"back_shard{i}", (x, y, z),
                                [Cube(tex, (x - size[0] / 2, y - size[1], z - size[2] / 2), size)], rot=(rx, 0, rz)))

    chest = Part("chest", (0, 6, 0), [
        Cube("chest", (-10, -8, -6), (20, 14, 12)),
        Cube("core", (-2, -4, -6.8), (4, 4, 1)),
    ], [
        Part("head", (0, -5, -6), [
            Cube("head", (-5, -10, -11), (10, 9, 8)),
            Cube("brow", (-5.5, -8, -12), (11, 2, 2)),
            Cube("jaw_plate", (-4, -3, -11.5), (8, 2, 2)),
        ]),
        arm("right", -1),
        arm("left", 1),
    ] + back_shards)
    waist = Part("waist", (0, 11, 0), [Cube("waist", (-5, 5, -3.5), (10, 6, 7))], [chest])
    return Part("base", (0, 0, 0), [], [waist, leg("right", -1), leg("left", 1)])


def plates(f, base, seam_every=5, salt=0):
    material(base, grain=0.12, gradient=0.12, salt=salt)(f)
    if f.side:
        for y in range(f.h):
            for x in range(f.w):
                if y % seam_every == seam_every - 1:
                    f.put(x, y, STEEL_DARK)
                elif y % seam_every == 0 and y > 0:
                    f.put(x, y, mix(f.get(x, y), STEEL_LIGHT, 0.4))
        for y in range(1, f.h, seam_every):
            if f.w > 4:
                f.put(1, y, STEEL_LIGHT)
                f.put(f.w - 2, y, STEEL_LIGHT)


def veins(f, count):
    for i in range(count):
        crack(f, mix(ADAMANT, MAGMA, 0.3), rgba(MAGMA, 170), i + 11)


def paint_golem(tex, face):
    f = face
    if tex == "chest":
        plates(f, STEEL)
        if f.side:
            veins(f, 2 if f.name in ("north", "south") else 1)
        if f.name == "north":
            # Socket around the core
            for x in range(7, 13):
                for y in range(3, 9):
                    edge = x in (7, 12) or y in (3, 8)
                    f.put(x, y, STEEL_DARK if edge else mul(MAGMA, 0.45), None if edge else rgba(MAGMA, 120))
        if f.name == "up":
            for x in range(f.w):
                f.put(x, f.h // 2, STEEL_DARK)
    elif tex == "core":
        f.fill(lambda x, y: mix((220, 250, 255), MAGMA, abs(x - 1.5) / 2 + abs(y - 1.5) / 3))
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba(mix((210, 245, 255), MAGMA, abs(x - 1.5) / 2 + abs(y - 1.5) / 3), 255)
    elif tex == "waist":
        plates(f, mul(STEEL, 0.85), seam_every=3, salt=2)
    elif tex == "head":
        material(STEEL, grain=0.1, gradient=0.3)(f)
        if f.name == "north":
            # Eye slits right under the brow
            for x in range(1, f.w - 1):
                f.put(x, 4, STEEL_DARK)
            for x in (2, 6):
                f.put(x, 4, MAGMA, (170, 240, 255))
                f.put(x + 1, 4, mix(MAGMA, ADAMANT, 0.4), rgba(MAGMA, 220))
            for x in range(1, f.w - 1):
                f.put(x, 5, mul(STEEL_DARK, 1.2))
        if f.name == "up":
            for y in range(f.h):
                f.put(f.w // 2, y, ADAMANT)
    elif tex == "brow":
        material(STEEL_LIGHT, grain=0.1, gradient=0.4)(f)
    elif tex == "jaw_plate":
        material(STEEL, grain=0.1)(f)
        if f.name == "north":
            for x in range(f.w):
                f.put(x, 0, STEEL_LIGHT if x % 2 == 0 else STEEL_DARK)
    elif tex == "upper_arm":
        plates(f, STEEL, seam_every=4, salt=3)
        if f.name in ("west", "east"):
            veins(f, 1)
    elif tex == "fist":
        plates(f, STEEL, seam_every=5, salt=4)
        if f.side:
            for y in range(f.h - 4, f.h):
                for x in range(f.w):
                    f.put(x, y, STEEL_LIGHT if (x // 2 + y) % 2 == 0 else STEEL)
            for x in range(f.w):
                f.put(x, f.h - 5, STEEL_DARK)
        if f.name == "north":
            veins(f, 1)
        if f.name == "down":
            f.fill(lambda x, y: STEEL_LIGHT if x % 3 != 2 else STEEL_DARK)
    elif tex == "shoulder":
        material(STEEL_LIGHT, grain=0.12, gradient=0.3)(f)
        if f.side:
            for x in range(f.w):
                f.put(x, f.h - 1, STEEL_DARK)
                f.put(x, 0, mix(STEEL_LIGHT, (255, 255, 255), 0.25))
            f.put(f.w // 2, 2, ADAMANT, rgba(ADAMANT, 180))
        if f.name == "up":
            for x in range(2, f.w - 2):
                f.put(x, f.h // 2, STEEL)
            f.put(f.w // 2, f.h // 2, ADAMANT, rgba(ADAMANT, 180))
    elif tex == "leg":
        plates(f, STEEL, seam_every=4, salt=5)
        if f.name == "north":
            for x in range(f.w):
                f.put(x, f.h - 1, STEEL_LIGHT if x % 2 == 0 else STEEL_DARK)
            veins(f, 1)
    elif tex in ("shard_big", "shard_small"):
        def crystal(x, y):
            t = y / max(1, f.h - 1)
            c = mix(mix(ADAMANT, (200, 235, 255), 0.45), mul(ADAMANT, 0.55), t)
            return mix(c, (255, 255, 255), 0.35) if x == 0 and f.side else c

        f.fill(crystal)
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba(ADAMANT, int(200 * (1 - y / max(1, f.h))))


# ---------------------------------------------------------------------------
# Shadow soldier (vanilla humanoid layout, 64x64): smoky black armor and glowing blue eyes
# ---------------------------------------------------------------------------
SHADOW = (24, 20, 38)
SHADOW_EDGE = (62, 48, 110)
SHADOW_EYE = (90, 170, 255)

HUMANOID = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 4, 12, 4), "leg": (0, 16, 4, 12, 4)}


def paint_shadow(part, f):
    def smoke(x, y):
        n = f.n(x, y)
        c = mul(SHADOW, 0.8 + 0.5 * n)
        if f.border(x, y) and f.w > 2:
            c = mix(c, SHADOW_EDGE, 0.55)
        return c

    f.fill(smoke)
    if part == "head" and f.name == "north":
        for x in (1, 2, 5, 6):
            f.put(x, 4, mix(SHADOW_EYE, (255, 255, 255), 0.4), SHADOW_EYE)
        f.put(1, 5, mul(SHADOW_EYE, 0.4), rgba(SHADOW_EYE, 90))
        f.put(6, 5, mul(SHADOW_EYE, 0.4), rgba(SHADOW_EYE, 90))
    if part == "head" and f.name == "up":
        # A crest of dark flame
        for x in range(2, 6):
            f.put(x, 3, SHADOW_EDGE)
    if part == "body" and f.side:
        for y in range(f.h):
            if y % 4 == 3:
                for x in range(f.w):
                    f.put(x, y, mul(SHADOW, 0.6))
        if f.name == "north":
            # Glowing sigil on the chest
            for x, y in ((3, 3), (4, 3), (2, 4), (5, 4), (3, 5), (4, 5)):
                f.put(x, y, mul(SHADOW_EYE, 0.5), rgba(SHADOW_EYE, 140))
    if part in ("arm", "leg") and f.side:
        # Wisps fading at the end of the limbs
        for x in range(f.w):
            if f.n(x, 2, 3) > 0.5:
                f.put(x, f.h - 1, CLEAR)


def shadow_soldier(assets: Path):
    color = Image.new("RGBA", (64, 64), CLEAR)
    glow = Image.new("RGBA", (64, 64), CLEAR)
    for i, (part, (u, v, w, h, d)) in enumerate(HUMANOID.items()):
        for name, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
            face = Face(name, fw, fh, seed=200 + i * 7 + len(name))
            paint_shadow(part, face)
            for y in range(fh):
                for x in range(fw):
                    color.putpixel((fx + x, fy + y), face.color[y][x])
                    glow.putpixel((fx + x, fy + y), face.glow[y][x])
    out = assets / "textures" / "entity"
    color.save(out / "shadow_soldier.png")
    glow.save(out / "shadow_soldier_glow.png")


BOSSES = {
    "skeleton_king": ("skeletonKing", king_parts, paint_king),
    "corrupted_archmage": ("corruptedArchmage", mage_parts, paint_mage),
    "adamantite_golem": ("adamantiteGolem", golem_parts, paint_golem),
}


# ---------------------------------------------------------------------------
# Atlas packing, painting and Java output
# ---------------------------------------------------------------------------
def walk(part):
    yield part
    for child in part.children:
        yield from walk(child)


def pack(root):
    """Shelf-packs one UV net per texture key. Returns ({tex: (u, v, size)}, width, height)."""
    nets = {}
    for part in walk(root):
        for cube in part.cubes:
            size = tuple(int(s) for s in cube.size)
            if cube.tex in nets and nets[cube.tex] != size:
                raise ValueError(f"texture {cube.tex} used with two sizes")
            nets[cube.tex] = size
    width = 128
    order = sorted(nets, key=lambda t: (-(nets[t][2] + nets[t][1]), t))
    placed, x, y, shelf = {}, 0, 0, 0
    for tex in order:
        w, h, d = nets[tex]
        nw, nh = 2 * (w + d), d + h
        if x + nw > width:
            x, y, shelf = 0, y + shelf, 0
        placed[tex] = (x, y, nets[tex])
        x += nw
        shelf = max(shelf, nh)
    height = 64
    while height < y + shelf:
        height *= 2
    return placed, width, height


def face_rects(u, v, w, h, d):
    return {
        "up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
        "west": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h),
    }


def paint_atlas(placed, width, height, painter, pattern=None):
    """Paints every face; with {@code pattern} (tex -> material name) the atlas is rendered in HD (see hd.py)."""
    if pattern is None:
        color = Image.new("RGBA", (width, height), CLEAR)
        glow = Image.new("RGBA", (width, height), CLEAR)
        for i, (tex, (u, v, (w, h, d))) in enumerate(sorted(placed.items())):
            for name, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                if fw == 0 or fh == 0:
                    continue
                face = Face(name, fw, fh, seed=i * 13 + len(name))
                painter(tex, face)
                for y in range(fh):
                    for x in range(fw):
                        color.putpixel((fx + x, fy + y), face.color[y][x])
                        glow.putpixel((fx + x, fy + y), face.glow[y][x])
        return color, glow
    import numpy as np
    import hd
    s = hd.SCALE
    color = np.zeros((height * s, width * s, 4), dtype=np.uint8)
    glow = np.zeros((height * s, width * s, 4), dtype=np.uint8)
    for i, (tex, (u, v, (w, h, d))) in enumerate(sorted(placed.items())):
        for name, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
            if fw == 0 or fh == 0:
                continue
            face = Face(name, fw, fh, seed=i * 13 + len(name))
            painter(tex, face)
            hc, hg = hd.upscale_face(face.color, face.glow, pattern(tex), face.side, seed=i * 97 + len(name) * 7)
            color[fy * s:(fy + fh) * s, fx * s:(fx + fw) * s] = hc
            glow[fy * s:(fy + fh) * s, fx * s:(fx + fw) * s] = hg
    return Image.fromarray(color, "RGBA"), Image.fromarray(glow, "RGBA")


def num(v):
    v = round(v, 4)
    return (f"{v:.1f}" if v == int(v) else f"{v:g}") + "F"


def camel(name):
    head, *rest = name.split("_")
    return head + "".join(p.capitalize() for p in rest)


def java_parts(part, parent_var, parent_pivot, placed, lines):
    var = camel(part.name)
    px, py, pz = (part.pivot[i] - parent_pivot[i] for i in range(3))
    builder = ["CubeListBuilder.create()"]
    for cube in part.cubes:
        u, v, _ = placed[cube.tex]
        ox, oy, oz = (cube.origin[i] - part.pivot[i] for i in range(3))
        w, h, d = cube.size
        args = ", ".join(num(a) for a in (ox, oy, oz, w, h, d))
        deform = f", new CubeDeformation({num(cube.inflate)})" if cube.inflate else ""
        mirror_on, mirror_off = (".mirror()", ".mirror(false)") if cube.mirror else ("", "")
        builder.append(f".texOffs({u}, {v}){mirror_on}.addBox({args}{deform}){mirror_off}")
    rx, ry, rz = part.rot
    pose = (f"PartPose.offsetAndRotation({num(px)}, {num(py)}, {num(pz)}, {num(rx)}, {num(ry)}, {num(rz)})"
            if any(part.rot) else f"PartPose.offset({num(px)}, {num(py)}, {num(pz)})")
    cubes = "\n                ".join(builder)
    lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{part.name}\", {cubes},\n"
                 f"                {pose});")
    for child in part.children:
        java_parts(child, var, part.pivot, placed, lines)


BOSS_MATERIALS = {
    "skeleton_king": ("bone", {"crown": "smooth"}),
    "corrupted_archmage": ("cloth", {"head": "skin"}),
    "adamantite_golem": ("metal", {"shard_big": "crystal", "shard_small": "crystal"}),
}


def generate(assets: Path = ASSETS, java: Path = JAVA):
    methods = []
    for key, (method, parts, painter) in BOSSES.items():
        root = parts()
        placed, width, height = pack(root)
        default, overrides = BOSS_MATERIALS[key]
        color, glow = paint_atlas(placed, width, height, painter, hd.pattern_for(default, overrides))
        out = assets / "textures" / "entity" / "boss"
        out.mkdir(parents=True, exist_ok=True)
        color.save(out / f"{key}.png")
        glow.save(out / f"{key}_glow.png")
        lines = []
        java_parts(root, "mesh.getRoot()", (0, 0, 0), placed, lines)
        body = "\n".join(lines)
        methods.append(f"""    public static LayerDefinition {method}() {{
        MeshDefinition mesh = new MeshDefinition();
{body}
        return LayerDefinition.create(mesh, {width}, {height});
    }}""")
    shadow_soldier(assets)
    java.parent.mkdir(parents=True, exist_ok=True)
    java.write_text("""package com.firerideur.arcanequest.client.boss;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Boss model geometry. GENERATED by tools/boss_models.py together with the textures: edit the script, not this file.
 */
@SuppressWarnings("unused")
public final class BossGeometry {
    private BossGeometry() {}

""" + "\n\n".join(methods) + "\n}\n")


if __name__ == "__main__":
    generate()
