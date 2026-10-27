"""Dungeon monsters (found behind the gates): geometry and textures, like tools/boss_models.py.

Writes textures/entity/dungeon/<mob>.png (+ _glow) and client/dungeon/MobGeometry.java.
Biped mobs share the part names head/body/right_arm/left_arm/right_leg/left_leg (animated by BipedMobModel),
four- and six-legged ones use head/body/leg0..legN/tail (QuadMobModel).
"""
from pathlib import Path

from boss_models import (ASSETS, CLEAR, Cube, Part, Face, material, mix, mul, rgba, pack, paint_atlas, java_parts,
                         walk)

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src" / "main" / "java" / "com" / "firerideur" / "arcanequest" / "client" / "dungeon" / "MobGeometry.java"


def find(root, name):
    for part in walk(root):
        if part.name == name:
            return part
    raise KeyError(name)


def biped(head, body, arm, leg, prefix=""):
    """Humanoid skeleton standing on y = 24. Returns (root, anchors)."""
    hw, hh, hd = head
    bw, bh, bd = body
    aw, ah, ad = arm
    lw, lh, ld = leg
    leg_top = 24 - lh
    body_top = leg_top - bh
    lx = max(lw / 2, bw / 2 - lw / 2)
    ax = bw / 2 + aw / 2
    t = prefix
    legs = [
        Part("right_leg", (-lx, leg_top, 0), [Cube(t + "leg", (-lx - lw / 2, leg_top, -ld / 2), leg)]),
        Part("left_leg", (lx, leg_top, 0), [Cube(t + "leg", (lx - lw / 2, leg_top, -ld / 2), leg, True)]),
    ]
    head_part = Part("head", (0, body_top, 0), [Cube(t + "head", (-hw / 2, body_top - hh, -hd / 2), head)])
    arms = [
        Part("right_arm", (-ax, body_top + 1, 0), [Cube(t + "arm", (-ax - aw / 2, body_top, -ad / 2), arm)]),
        Part("left_arm", (ax, body_top + 1, 0), [Cube(t + "arm", (ax - aw / 2, body_top, -ad / 2), arm, True)]),
    ]
    body_part = Part("body", (0, leg_top, 0), [Cube(t + "body", (-bw / 2, body_top, -bd / 2), body)], [head_part] + arms)
    root = Part("base", (0, 0, 0), [], [body_part] + legs)
    anchors = {"leg_top": leg_top, "body_top": body_top, "head_top": body_top - hh, "arm_x": ax,
               "hand_y": body_top + ah - 1, "head_front": -hd / 2, "body_front": -bd / 2, "body_back": bd / 2}
    return root, anchors


# ---------------------------------------------------------------------------
# Painting helpers
# ---------------------------------------------------------------------------
def eyes(f, row, cols, color, glow_alpha=200):
    for x in cols:
        f.put(x, row, color, rgba(color, glow_alpha))


def painter(colors, details=None):
    """Fills each texture with its base color, then lets {@code details} add features."""

    def paint(tex, f):
        base = colors.get(tex, (128, 128, 128))
        material(base, grain=0.14)(f)
        if details:
            details(tex, f)

    return paint


def stripes(f, every, color):
    if f.side:
        for y in range(f.h):
            if y % every == every - 1:
                for x in range(f.w):
                    f.put(x, y, color)


# ---------------------------------------------------------------------------
# Goblin and goblin shaman
# ---------------------------------------------------------------------------
GOBLIN_SKIN = (98, 142, 62)
LEATHER = (96, 64, 38)
WOOD = (92, 62, 34)
BONE = (220, 210, 180)


def goblin_parts(shaman=False):
    root, a = biped((7, 6, 6), (6, 6, 3), (2, 7, 2), (2, 6, 2))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [
        Cube("ear", (-5.5, ht + 1, -0.5), (2, 2, 1)),
        Cube("ear", (3.5, ht + 1, -0.5), (2, 2, 1), True),
        Cube("nose", (-0.5, ht + 3, -4), (1, 2, 1)),
    ]
    body = find(root, "body")
    body.cubes.append(Cube("loincloth", (-2.5, a["leg_top"] - 1, -2), (5, 3, 1)))
    right = find(root, "right_arm")
    hand = (-a["arm_x"], a["hand_y"])
    if shaman:
        head.cubes.append(Cube("hood", (-4, ht - 1, -3.5), (8, 4, 7)))
        head.cubes.append(Cube("feather", (-0.5, ht - 5, 1), (1, 4, 1)))
        body.cubes.append(Cube("necklace", (-2.5, a["body_top"] + 1, -2), (5, 2, 1)))
        right.children.append(Part("staff", (hand[0], hand[1], 0), [
            Cube("staff", (hand[0] - 0.5, hand[1] - 13, -1.5), (1, 15, 1)),
            Cube("staff_skull", (hand[0] - 1.5, hand[1] - 16, -2.5), (3, 3, 3)),
        ]))
    else:
        right.children.append(Part("club", (hand[0], hand[1], 0), [
            Cube("club_handle", (hand[0] - 0.5, hand[1] - 0.5, -6), (1, 1, 6)),
            Cube("club_head", (hand[0] - 1, hand[1] - 1, -9), (2, 2, 3)),
        ]))
    return root


def goblin_details(shaman):
    def details(tex, f):
        if tex == "head" and f.name == "north":
            eyes(f, 2, (1, 5), (255, 220, 40) if not shaman else (200, 80, 255))
            f.put(2, 2, (20, 20, 10))
            f.put(4, 2, (20, 20, 10))
            for x in range(1, 6):
                f.put(x, 4, (40, 30, 20) if x % 2 else (230, 220, 190))
        if tex == "staff_skull":
            material(BONE, grain=0.1)(f)
            if f.name == "north":
                eyes(f, 1, (0, 2), (190, 80, 255), 255)
        if tex == "body" and f.name == "north" and not shaman:
            for y in range(f.h):
                f.put(0, y, LEATHER)
                f.put(f.w - 1, y, LEATHER)
        if tex == "necklace" and f.side:
            for x in range(0, f.w, 2):
                f.put(x, 1, BONE)
    return details


GOBLIN_COLORS = {"head": GOBLIN_SKIN, "ear": GOBLIN_SKIN, "nose": mul(GOBLIN_SKIN, 0.85), "body": mul(GOBLIN_SKIN, 0.9),
                 "arm": GOBLIN_SKIN, "leg": mul(GOBLIN_SKIN, 0.8), "loincloth": LEATHER, "club_handle": WOOD,
                 "club_head": (110, 80, 50)}
SHAMAN_SKIN = (88, 116, 78)
SHAMAN_COLORS = {"head": SHAMAN_SKIN, "ear": SHAMAN_SKIN, "nose": mul(SHAMAN_SKIN, 0.85), "body": (120, 40, 34),
                 "arm": SHAMAN_SKIN, "leg": mul(SHAMAN_SKIN, 0.8), "loincloth": (90, 30, 26), "hood": (110, 36, 30),
                 "feather": (230, 60, 50), "necklace": (60, 40, 30), "staff": WOOD, "staff_skull": BONE}


# ---------------------------------------------------------------------------
# Ice elf
# ---------------------------------------------------------------------------
ELF_SKIN = (204, 222, 240)
ELF_ARMOR = (226, 236, 248)
ELF_CLOAK = (52, 92, 170)
FROST = (120, 220, 255)


def elf_parts():
    root, a = biped((6, 7, 6), (6, 12, 3), (2, 12, 2), (2, 12, 2))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [
        Cube("ear", (-4.5, ht + 3, -0.5), (1, 1, 3)),
        Cube("ear", (3.5, ht + 3, -0.5), (1, 1, 3), True),
        Cube("hair", (-3.5, ht - 0.5, 0.5), (7, 9, 3)),
    ]
    body = find(root, "body")
    body.children.append(Part("cape", (0, a["body_top"] + 0.5, 1.5),
                              [Cube("cape", (-3.5, a["body_top"] + 0.5, 1.5), (7, 16, 1))]))
    left = find(root, "left_arm")
    hx, hy = a["arm_x"], a["hand_y"]
    left.children.append(Part("bow", (hx, hy, 0), [
        Cube("bow", (hx - 0.5, hy - 7, -2.5), (1, 15, 1)),
        Cube("bow_tip", (hx - 0.5, hy - 8, -1.5), (1, 1, 1)),
        Cube("bow_tip", (hx - 0.5, hy + 8, -1.5), (1, 1, 1)),
    ]))
    return root


def elf_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 3, (1, 4), FROST, 230)
        f.put(2, 3, (240, 250, 255))
        f.put(3, 3, (240, 250, 255))
    if tex == "head" and f.name in ("up", "south"):
        f.fill(lambda x, y: mix((235, 240, 250), (200, 210, 230), f.n(x, y)))
    if tex == "body" and f.side:
        for x in range(f.w):
            f.put(x, 0, FROST, rgba(FROST, 120))
        if f.name == "north":
            for y in range(2, 9):
                f.put(f.w // 2, y, mix(ELF_ARMOR, FROST, 0.5))
    if tex == "bow":
        f.fill(lambda x, y: mix((150, 200, 240), (230, 250, 255), y / max(1, f.h - 1)))
    if tex == "cape" and f.side:
        for x in range(f.w):
            if f.n(x, 1, 2) > 0.5:
                f.put(x, f.h - 1, CLEAR)


ELF_COLORS = {"head": ELF_SKIN, "ear": ELF_SKIN, "hair": (236, 240, 250), "body": ELF_ARMOR, "arm": ELF_SKIN,
              "leg": (170, 190, 220), "cape": ELF_CLOAK, "bow": (180, 220, 250), "bow_tip": (240, 250, 255)}


# ---------------------------------------------------------------------------
# High orc
# ---------------------------------------------------------------------------
ORC_SKIN = (104, 124, 82)
ORC_IRON = (88, 84, 82)


def orc_parts():
    root, a = biped((8, 7, 7), (11, 12, 6), (4, 13, 4), (4, 10, 4))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [
        Cube("tusk", (-3, ht + 5, -4.5), (1, 2, 1)),
        Cube("tusk", (2, ht + 5, -4.5), (1, 2, 1), True),
        Cube("topknot", (-1, ht - 3, 0), (2, 3, 2)),
    ]
    body = find(root, "body")
    body.cubes.append(Cube("belt", (-6, a["leg_top"] - 2, -3.5), (12, 2, 7)))
    for side, sign in (("right", -1), ("left", 1)):
        arm = find(root, side + "_arm")
        x = sign * a["arm_x"]
        arm.cubes.append(Cube("pauldron", (x - 3, a["body_top"] - 1.5, -3), (6, 3, 6), sign > 0))
    right = find(root, "right_arm")
    hx, hy = -a["arm_x"], a["hand_y"]
    right.children.append(Part("axe", (hx, hy, 0), [
        Cube("axe_handle", (hx - 0.5, hy - 0.5, -13), (1, 1, 13)),
        Cube("axe_blade", (hx - 1, hy - 4, -14), (2, 7, 4)),
    ]))
    return root


def orc_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 3, (1, 2, 5, 6), (230, 40, 30), 210)
        for x in range(1, 7):
            f.put(x, 2, mul(ORC_SKIN, 0.6))
        for x in range(2, 6):
            f.put(x, 5, (50, 40, 30))
    if tex == "body" and f.side:
        for x in range(f.w):
            if (x + 1) % 4 == 0:
                for y in range(f.h):
                    f.put(x, y, mul(f.get(x, y), 0.8))
        if f.name == "north":
            for y in range(1, 6):
                for x in range(3, 8):
                    f.put(x, y, ORC_IRON if (x + y) % 3 else mul(ORC_IRON, 1.3))
    if tex == "axe_blade":
        f.fill(lambda x, y: mix((170, 170, 175), (90, 90, 95), x / max(1, f.w - 1)))
        if f.name in ("north", "south"):
            for y in range(f.h):
                f.put(0, y, (215, 215, 220))
    if tex == "tusk":
        f.fill(lambda x, y: (235, 228, 200))


ORC_COLORS = {"head": ORC_SKIN, "tusk": (235, 228, 200), "topknot": (30, 26, 24), "body": (110, 76, 50),
              "arm": ORC_SKIN, "leg": (80, 60, 44), "belt": (60, 40, 26), "pauldron": ORC_IRON,
              "axe_handle": WOOD, "axe_blade": (150, 150, 155)}


# ---------------------------------------------------------------------------
# Demon knight
# ---------------------------------------------------------------------------
DEMON_IRON = (46, 36, 44)
EMBER = (255, 96, 30)


def demon_parts():
    root, a = biped((8, 8, 8), (9, 12, 5), (4, 12, 4), (4, 12, 4))
    head = find(root, "head")
    ht = a["head_top"]
    head.children += [
        Part("right_horn", (-3.5, ht + 1, 0), [Cube("horn", (-4.5, ht - 4, -1), (2, 5, 2))], rot=(0.2, 0, -0.45)),
        Part("left_horn", (3.5, ht + 1, 0), [Cube("horn", (2.5, ht - 4, -1), (2, 5, 2), True)], rot=(0.2, 0, 0.45)),
    ]
    body = find(root, "body")
    body.children.append(Part("cape", (0, a["body_top"] + 0.5, 2.5),
                              [Cube("cape", (-4.5, a["body_top"] + 0.5, 2.5), (9, 19, 1))]))
    for side, sign in (("right", -1), ("left", 1)):
        arm = find(root, side + "_arm")
        x = sign * a["arm_x"]
        arm.cubes.append(Cube("pauldron", (x - 2.5, a["body_top"] - 2, -3), (5, 4, 6), sign > 0))
    right = find(root, "right_arm")
    hx, hy = -a["arm_x"], a["hand_y"]
    right.children.append(Part("sword", (hx, hy, 0), [
        Cube("sword_grip", (hx - 0.5, hy - 0.5, -2), (1, 1, 3)),
        Cube("sword_guard", (hx - 1.5, hy - 1, -3), (3, 2, 1)),
        Cube("sword_blade", (hx - 0.5, hy - 1, -16), (1, 2, 13)),
    ]))
    return root


def demon_details(tex, f):
    if tex in ("head", "body", "arm", "leg", "pauldron"):
        # Glowing cracks in the black armor
        for i in range(1 if f.w < 6 else 2):
            x = int(f.n(i, 7, 3) * f.w)
            for y in range(f.h):
                if f.n(x, y, 11) > 0.35:
                    f.put(x, y, mix(EMBER, (120, 20, 10), 0.4), rgba(EMBER, 150))
                x = max(0, min(f.w - 1, x + (1 if f.n(y, i, 5) > 0.6 else -1 if f.n(y, i, 5) < 0.3 else 0)))
    if tex == "head" and f.name == "north":
        for x in range(1, 7):
            f.put(x, 3, (10, 6, 8))
        eyes(f, 3, (2, 5), (255, 150, 40), 255)
        for x in range(2, 6):
            f.put(x, 5, (10, 6, 8))
    if tex == "horn":
        f.fill(lambda x, y: mix((40, 30, 30), (200, 190, 170), 1 - y / max(1, f.h - 1)))
    if tex == "sword_blade":
        f.fill(lambda x, y: mix((90, 20, 20), EMBER, f.n(x, y)))
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba(EMBER, 140)
    if tex == "cape" and f.side:
        for x in range(f.w):
            tear = int(f.n(x, 2, 4) * 3)
            for y in range(f.h - tear, f.h):
                f.put(x, y, CLEAR)


DEMON_COLORS = {"head": DEMON_IRON, "body": DEMON_IRON, "arm": DEMON_IRON, "leg": DEMON_IRON, "pauldron": (60, 46, 56),
                "horn": (40, 30, 30), "cape": (100, 14, 20), "sword_grip": (40, 30, 30), "sword_guard": (90, 70, 40),
                "sword_blade": (140, 30, 20)}


# ---------------------------------------------------------------------------
# Steelfang wolf
# ---------------------------------------------------------------------------
FUR = (64, 64, 72)
STEEL = (158, 162, 172)


def wolf_parts():
    legs = []
    for i, (x, z) in enumerate(((-2.5, -4.5), (2.5, -4.5), (-2.5, 5.5), (2.5, 5.5))):
        legs.append(Part(f"leg{i}", (x, 15, z), [Cube("leg", (x - 1.5, 15, z - 1.5), (3, 9, 3), x > 0)]))
    head = Part("head", (0, 10, -7), [
        Cube("head", (-3.5, 6, -13), (7, 6, 6)),
        Cube("snout", (-2, 9, -16), (4, 3, 3)),
        Cube("ear", (-3, 4, -11), (2, 2, 1)),
        Cube("ear", (1, 4, -11), (2, 2, 1), True),
        Cube("fang", (-1.5, 12, -15.5), (1, 1, 1)),
        Cube("fang", (0.5, 12, -15.5), (1, 1, 1)),
    ])
    tail = Part("tail", (0, 9, 7), [Cube("tail", (-1, 8, 7), (2, 2, 8))], rot=(-0.6, 0, 0))
    body = Part("body", (0, 12, 0), [
        Cube("body", (-4, 8, -7), (8, 7, 14)),
        Cube("mane", (-4.5, 7, -8), (9, 9, 5)),
        Cube("plate", (-1, 6, -4), (2, 2, 3)),
        Cube("plate", (-1, 6, 1), (2, 2, 3)),
    ], [head, tail])
    return Part("base", (0, 0, 0), [], [body] + legs)


def wolf_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 2, (1, 5), (255, 50, 40), 230)
    if tex in ("plate", "fang"):
        material(STEEL, grain=0.08, gradient=0.4)(f)
    if tex in ("body", "mane") and f.side:
        for y in range(f.h):
            for x in range(f.w):
                if f.n(x, y, 9) > 0.8:
                    f.put(x, y, mul(FUR, 1.4))
    if tex == "body" and f.name == "up":
        for y in range(f.h):
            f.put(f.w // 2, y, STEEL)


WOLF_COLORS = {"head": FUR, "snout": mul(FUR, 0.8), "ear": mul(FUR, 0.9), "fang": STEEL, "tail": FUR, "body": FUR,
               "mane": mul(FUR, 0.8), "plate": STEEL, "leg": mul(FUR, 0.85)}


# ---------------------------------------------------------------------------
# Ant soldier
# ---------------------------------------------------------------------------
CHITIN = (108, 38, 28)


def ant_parts():
    legs = []
    for i, z in enumerate((-3, 0, 3)):
        for side, sign in ((0, -1), (1, 1)):
            x = 3 * sign
            legs.append(Part(f"leg{i * 2 + side}", (x, 14, z),
                             [Cube("leg", (x - 0.5, 14, z - 0.5), (1, 14, 1), sign > 0)], rot=(0, 0, 0.8 * -sign)))
    head = Part("head", (0, 13, -4), [
        Cube("head", (-3, 10, -9), (6, 5, 5)),
        Cube("mandible", (-3, 13, -12), (1, 1, 3)),
        Cube("mandible", (2, 13, -12), (1, 1, 3), True),
    ], [
        Part("antennae", (0, 10, -8), [Cube("antenna", (-2, 6, -8.5), (1, 4, 1)),
                                       Cube("antenna", (1, 6, -8.5), (1, 4, 1), True)], rot=(-0.5, 0, 0)),
    ])
    abdomen = Part("tail", (0, 13, 3), [Cube("abdomen", (-4, 10, 3), (8, 7, 10))], rot=(0.25, 0, 0))
    body = Part("body", (0, 14, 0), [Cube("thorax", (-3, 12, -4), (6, 5, 7))], [head, abdomen] + legs)
    return Part("base", (0, 0, 0), [], [body])


def ant_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 1, (0, 1, 4, 5), (255, 140, 30), 220)
    if tex in ("thorax", "abdomen", "head") and f.side:
        stripes(f, 3, mul(CHITIN, 0.6))
        for x in range(f.w):
            f.put(x, 0, mix(CHITIN, (230, 120, 90), 0.4))
    if tex == "mandible":
        f.fill(lambda x, y: (40, 20, 16))


ANT_COLORS = {"head": CHITIN, "mandible": (40, 20, 16), "antenna": mul(CHITIN, 0.7), "thorax": CHITIN,
              "abdomen": mul(CHITIN, 1.1), "leg": mul(CHITIN, 0.6)}


MOBS = {
    "goblin": ("goblin", lambda: goblin_parts(False), painter(GOBLIN_COLORS, goblin_details(False))),
    "goblin_shaman": ("goblinShaman", lambda: goblin_parts(True), painter(SHAMAN_COLORS, goblin_details(True))),
    "ice_elf": ("iceElf", elf_parts, painter(ELF_COLORS, elf_details)),
    "high_orc": ("highOrc", orc_parts, painter(ORC_COLORS, orc_details)),
    "demon_knight": ("demonKnight", demon_parts, painter(DEMON_COLORS, demon_details)),
    "steelfang_wolf": ("steelfangWolf", wolf_parts, painter(WOLF_COLORS, wolf_details)),
    "ant_soldier": ("antSoldier", ant_parts, painter(ANT_COLORS, ant_details)),
}


def gate_texture(assets: Path):
    """Swirling vortex (white, tinted by the gate's color when rendered), oval with soft edges."""
    import math
    from PIL import Image
    size = 64
    img = Image.new("RGBA", (size, size), CLEAR)
    for y in range(size):
        for x in range(size):
            dx = (x + 0.5 - size / 2) / (size / 2 * 0.62)
            dy = (y + 0.5 - size / 2) / (size / 2 * 0.95)
            r = math.sqrt(dx * dx + dy * dy)
            if r > 1.0:
                continue
            angle = math.atan2(dy, dx)
            swirl = 0.5 + 0.5 * math.sin(angle * 3 + r * 9.0)
            core = max(0.0, 1.0 - r * 2.2)
            edge = max(0.0, (r - 0.8) / 0.2)
            v = min(1.0, 0.25 + 0.55 * swirl * (1 - core) + core + edge * 0.8)
            alpha = int(255 * min(1.0, 0.35 + 0.65 * v) * (1.0 - max(0.0, r - 0.92) / 0.08))
            c = int(255 * v)
            img.putpixel((x, y), (c, c, c, max(0, alpha)))
    out = assets / "textures" / "entity"
    img.save(out / "gate.png")


def generate(assets: Path = ASSETS, java: Path = JAVA):
    gate_texture(assets)
    methods = []
    out = assets / "textures" / "entity" / "dungeon"
    out.mkdir(parents=True, exist_ok=True)
    for key, (method, parts, paint) in MOBS.items():
        root = parts()
        placed, width, height = pack(root)
        color, glow = paint_atlas(placed, width, height, paint)
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
    java.parent.mkdir(parents=True, exist_ok=True)
    java.write_text("""package com.firerideur.arcanequest.client.dungeon;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Dungeon monster geometry. GENERATED by tools/dungeon_mobs.py together with the textures: edit the script, not this file.
 */
@SuppressWarnings("unused")
public final class MobGeometry {
    private MobGeometry() {}

""" + "\n\n".join(methods) + "\n}\n")


if __name__ == "__main__":
    generate()
