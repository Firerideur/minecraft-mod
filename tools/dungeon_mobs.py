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
        Part("right_horn", (-3.5, ht + 1, 0), [Cube("horn", (-6.5, ht - 1, -1), (4, 2, 2)),
                                               Cube("horn_tip", (-6.5, ht - 5, -1), (2, 4, 2))], rot=(0.15, 0, -0.2)),
        Part("left_horn", (3.5, ht + 1, 0), [Cube("horn", (2.5, ht - 1, -1), (4, 2, 2), True),
                                             Cube("horn_tip", (4.5, ht - 5, -1), (2, 4, 2), True)], rot=(0.15, 0, 0.2)),
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
    if tex in ("head", "body", "arm", "leg", "pauldron") and f.side:
        # Glowing lava veins running down the black armor, with a few branches
        for i in range(1 if f.w < 6 else 2):
            x = int((i + 0.5) * f.w / (1 if f.w < 6 else 2))
            for y in range(f.h):
                f.put(x, y, mix(EMBER, (255, 200, 90), 0.25), rgba(EMBER, 190))
                if f.n(y, i, 23) > 0.8 and 0 < x < f.w - 1:
                    bx = x + (1 if f.n(y, i, 29) > 0.5 else -1)
                    f.put(bx, y, mix(EMBER, (140, 30, 10), 0.4), rgba(EMBER, 130))
                drift = f.n(y, i, 5)
                x = max(0, min(f.w - 1, x + (1 if drift > 0.75 else -1 if drift < 0.25 else 0)))
    if tex == "head" and f.name == "north":
        for x in range(1, 7):
            f.put(x, 3, (10, 6, 8))
        eyes(f, 3, (2, 5), (255, 150, 40), 255)
        for x in range(2, 6):
            f.put(x, 5, (10, 6, 8))
    if tex in ("horn", "horn_tip"):
        f.fill(lambda x, y: mix((40, 30, 30), (200, 190, 170), 1 - y / max(1, f.h - 1)) if tex == "horn_tip"
               else (50, 40, 38))
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


DEMON_COLORS = {"horn_tip": (120, 100, 90), "head": DEMON_IRON, "body": DEMON_IRON, "arm": DEMON_IRON, "leg": DEMON_IRON, "pauldron": (60, 46, 56),
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
        Cube("fang", (-1.5, 12, -15.5), (1, 2, 1)),
        Cube("fang", (0.5, 12, -15.5), (1, 2, 1)),
        Cube("lower_jaw", (-1.5, 13, -15.5), (3, 1, 3)),
    ])
    tail = Part("tail", (0, 9, 7), [Cube("tail", (-1, 8, 7), (2, 2, 8))], rot=(-0.6, 0, 0))
    body = Part("body", (0, 12, 0), [
        Cube("body", (-4, 8, -7), (8, 7, 14)),
        Cube("mane", (-4.5, 7, -8), (9, 9, 5)),
        Cube("plate", (-1, 6, -4), (2, 2, 3)),
        Cube("plate", (-1, 6, 1), (2, 2, 3)),
        Cube("spike", (-3, 4, -7), (1, 3, 1)),
        Cube("spike", (2, 4, -7), (1, 3, 1)),
        Cube("spike", (-0.5, 4, -6), (1, 3, 1)),
        Cube("spike", (-0.5, 4, -1), (1, 3, 1)),
        Cube("spike", (-0.5, 4, 4), (1, 3, 1)),
    ], [head, tail])
    return Part("base", (0, 0, 0), [], [body] + legs)


def wolf_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 2, (1, 5), (255, 50, 40), 230)
    if tex == "spike":
        f.fill(lambda x, y: mix((200, 205, 215), (90, 92, 100), y / max(1, f.h - 1)))
    if tex == "lower_jaw":
        f.fill(lambda x, y: (90, 20, 24) if f.name == "up" else mul(FUR, 0.8))
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


WOLF_COLORS = {"spike": STEEL, "lower_jaw": mul(FUR, 0.8), "head": FUR, "snout": mul(FUR, 0.8), "ear": mul(FUR, 0.9), "fang": STEEL, "tail": FUR, "body": FUR,
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


# ---------------------------------------------------------------------------
# Troll
# ---------------------------------------------------------------------------
TROLL_SKIN = (118, 110, 130)


def troll_parts():
    root, a = biped((7, 7, 7), (12, 11, 7), (5, 15, 5), (5, 9, 5))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [
        Cube("nose", (-1, ht + 2, -4.5), (2, 3, 1)),
        Cube("tusk", (-2.5, ht + 5, -4.5), (1, 2, 1)),
        Cube("tusk", (1.5, ht + 5, -4.5), (1, 2, 1), True),
    ]
    body = find(root, "body")
    body.cubes += [Cube("hump", (-4, a["body_top"] - 1, 2.5), (8, 4, 3)),
                   Cube("loincloth", (-5, a["leg_top"] - 2, -4), (10, 4, 1))]
    for side, sign in (("right", -1), ("left", 1)):
        arm = find(root, side + "_arm")
        x = sign * a["arm_x"]
        arm.cubes.append(Cube("fist", (x - 3, a["hand_y"] - 3, -3), (6, 5, 6), sign > 0))
        arm.cubes.append(Cube("moss", (x - 3, a["body_top"] - 1.5, -3), (6, 2, 6), sign > 0))
    head.cubes.append(Cube("brow", (-4, ht + 1, -4.5), (8, 2, 2)))
    return root


def troll_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 2, (1, 5), (255, 200, 60), 200)
        for x in range(1, 6):
            f.put(x, 5, (40, 36, 30))
    if tex in ("body", "arm", "leg", "head", "hump", "fist", "brow") and f.side:
        for y in range(f.h):
            for x in range(f.w):
                if f.n(x, y, 21) > 0.8:
                    f.put(x, y, (82, 118, 58))
    if tex == "moss" or (tex in ("hump", "head") and f.name == "up"):
        f.fill(lambda x, y: mix((70, 110, 50), (110, 150, 70), f.n(x, y)))
    if tex == "fist" and f.name == "north":
        for x in range(f.w):
            f.put(x, 1, mul(TROLL_SKIN, 0.6))
    if tex == "club":
        f.fill(lambda x, y: mix((80, 56, 34), (110, 80, 50), f.n(x, y)))
        if f.name == "north":
            for x in range(f.w):
                f.put(x, 1, (60, 40, 24))
    if tex == "tusk":
        f.fill(lambda x, y: (232, 224, 196))


TROLL_COLORS = {"fist": mul(TROLL_SKIN, 0.95), "moss": (82, 118, 58), "brow": mul(TROLL_SKIN, 0.85), "head": TROLL_SKIN, "nose": mul(TROLL_SKIN, 0.85), "tusk": (232, 224, 196), "body": TROLL_SKIN,
                "hump": mul(TROLL_SKIN, 0.9), "loincloth": (120, 90, 60), "arm": TROLL_SKIN, "leg": mul(TROLL_SKIN, 0.85),
                "club": (90, 64, 40)}


# ---------------------------------------------------------------------------
# Minotaur
# ---------------------------------------------------------------------------
BULL = (104, 66, 40)
IVORY = (232, 222, 196)


def minotaur_parts():
    root, a = biped((7, 7, 7), (10, 12, 6), (4, 12, 4), (4, 12, 4))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [
        Cube("snout", (-2.5, ht + 4, -5.5), (5, 3, 2)),
        Cube("ear", (-5, ht + 2, -0.5), (2, 1, 2)),
        Cube("ear", (3, ht + 2, -0.5), (2, 1, 2), True),
    ]
    head.children += [
        Part("right_horn", (-3.5, ht + 1, 0), [Cube("horn", (-8.5, ht, -1.5), (5, 3, 3)),
                                               Cube("horn_mid", (-10.5, ht - 3, -1.5), (3, 4, 3)),
                                               Cube("horn_tip", (-10, ht - 6, -1), (2, 3, 2))]),
        Part("left_horn", (3.5, ht + 1, 0), [Cube("horn", (3.5, ht, -1.5), (5, 3, 3), True),
                                             Cube("horn_mid", (7.5, ht - 3, -1.5), (3, 4, 3), True),
                                             Cube("horn_tip", (8, ht - 6, -1), (2, 3, 2), True)]),
    ]
    body = find(root, "body")
    body.cubes += [Cube("strap", (-5.5, a["body_top"], -3.5), (11, 2, 7)),
                   Cube("belt", (-5.5, a["leg_top"] - 2, -3.5), (11, 3, 7))]
    right = find(root, "right_arm")
    hx, hy = -a["arm_x"], a["hand_y"]
    right.children.append(Part("axe", (hx, hy, 0), [
        Cube("axe_handle", (hx - 0.5, hy - 0.5, -14), (1, 1, 14)),
        Cube("double_blade", (hx - 1, hy - 5, -15), (2, 10, 4)),
    ]))
    return root


def minotaur_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 2, (1, 5), (230, 30, 30), 230)
    if tex == "snout" and f.name == "north":
        f.put(1, 1, (30, 20, 16))
        f.put(3, 1, (30, 20, 16))
        f.put(2, 2, (230, 180, 60))
    if tex == "leg" and f.side:
        for y in range(f.h - 3, f.h):
            for x in range(f.w):
                f.put(x, y, (40, 34, 30))
    if tex in ("horn", "horn_mid", "horn_tip"):
        dark = {"horn": 0.0, "horn_mid": 0.15, "horn_tip": 0.45}[tex]
        f.fill(lambda x, y: mix(IVORY, (50, 40, 32), dark + 0.1 * f.n(x, y)))
    if tex == "strap":
        f.fill(lambda x, y: (70, 46, 28) if (x + y) % 6 else (170, 150, 90))
    if tex == "double_blade":
        f.fill(lambda x, y: mix((180, 180, 186), (110, 110, 116), abs(y - f.h / 2) / (f.h / 2)))
    if tex == "body" and f.name == "north":
        for y in range(2, f.h - 2):
            f.put(f.w // 2, y, mul(BULL, 0.75))


MINOTAUR_COLORS = {"horn_mid": IVORY, "strap": (70, 46, 28), "belt": (60, 40, 24), "head": BULL, "snout": mul(BULL, 1.15), "ear": BULL, "horn": IVORY, "horn_tip": IVORY,
                   "body": mul(BULL, 0.95), "arm": BULL, "leg": mul(BULL, 0.85), "axe_handle": WOOD,
                   "double_blade": (160, 160, 165)}


# ---------------------------------------------------------------------------
# Gargoyle
# ---------------------------------------------------------------------------
STONE = (122, 122, 128)


def gargoyle_parts():
    root, a = biped((7, 7, 6), (8, 11, 5), (3, 12, 3), (3, 9, 3))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [Cube("small_horn", (-3, ht - 2, -1), (1, 2, 1)), Cube("small_horn", (2, ht - 2, -1), (1, 2, 1), True)]
    body = find(root, "body")
    bt = a["body_top"]
    body.children += [
        Part("right_wing", (-1.5, bt + 1, 2.5), [Cube("wing", (-15.5, bt - 6, 2.5), (14, 17, 1))], rot=(0, 0.6, 0.15)),
        Part("left_wing", (1.5, bt + 1, 2.5), [Cube("wing", (1.5, bt - 6, 2.5), (14, 17, 1), True)], rot=(0, -0.6, -0.15)),
    ]
    return root


def gargoyle_details(tex, f):
    if tex in ("head", "body", "arm", "leg", "wing"):
        crack_x = int(f.n(1, 2, 5) * f.w)
        for y in range(f.h):
            if f.n(crack_x, y, 9) > 0.45:
                f.put(crack_x, y, mul(STONE, 0.55))
    if tex == "head" and f.name == "north":
        eyes(f, 3, (1, 5), (190, 90, 255), 255)
        for x in range(1, 6):
            f.put(x, 5, (40, 40, 44) if x % 2 else (190, 190, 190))
    if tex == "wing" and f.side:
        for x in range(f.w):
            for y in range(f.h):
                if y > f.h - 1 - (x % 4):
                    f.put(x, y, CLEAR)
        for y in range(0, f.h, 3):
            for x in range(f.w):
                f.put(x, y, mul(STONE, 0.7))


GARGOYLE_COLORS = {"head": STONE, "small_horn": mul(STONE, 0.7), "body": STONE, "arm": STONE, "leg": mul(STONE, 0.9),
                   "wing": mul(STONE, 0.85)}


# ---------------------------------------------------------------------------
# Lich
# ---------------------------------------------------------------------------
LICH_ROBE = (58, 28, 92)
SOUL_GREEN = (200, 100, 255)


def lich_parts():
    root, a = biped((8, 8, 8), (8, 12, 4), (3, 12, 3), (3, 12, 3))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [
        Cube("crown", (-4.5, ht - 2, -4.5), (9, 2, 9)),
        Cube("crown_spike", (-0.5, ht - 4, -4.5), (1, 2, 1)),
        Cube("crown_spike", (-4.5, ht - 4, -0.5), (1, 2, 1)),
        Cube("crown_spike", (3.5, ht - 4, -0.5), (1, 2, 1)),
    ]
    body = find(root, "body")
    body.cubes.append(Cube("robe", (-4.5, a["leg_top"] - 1, -2.5), (9, 12, 5)))
    right = find(root, "right_arm")
    hx, hy = -a["arm_x"], a["hand_y"]
    right.children.append(Part("staff", (hx, hy, 0), [
        Cube("staff", (hx - 0.5, hy - 15, -1.5), (1, 19, 1)),
        Cube("staff_gem", (hx - 1.5, hy - 18, -2.5), (3, 3, 3)),
    ]))
    return root


def lich_details(tex, f):
    if tex == "head":
        material((214, 206, 182), grain=0.1)(f)
        if f.name == "north":
            for x, y in ((1, 3), (2, 3), (5, 3), (6, 3), (1, 4), (2, 4), (5, 4), (6, 4)):
                f.put(x, y, (20, 16, 22))
            eyes(f, 4, (2, 5), SOUL_GREEN, 255)
            for x in range(1, 7):
                f.put(x, 6, (214, 206, 182) if x % 2 else (30, 24, 30))
    if tex in ("arm", "leg"):
        material((200, 192, 168), grain=0.1)(f)
    if tex in ("body", "robe") and f.side:
        for y in range(f.h):
            f.put(0, y, (214, 168, 60))
            f.put(f.w - 1, y, (214, 168, 60))
        if f.name == "north":
            for y in range(f.h):
                f.put(f.w // 2, y, (214, 168, 60))
        if tex == "robe":
            for x in range(f.w):
                tear = int(f.n(x, 3, 7) * 3)
                for y in range(f.h - tear, f.h):
                    f.put(x, y, CLEAR)
            for x in range(1, f.w - 1, 3):
                f.put(x, f.h - 5, mul(SOUL_GREEN, 0.6), rgba(SOUL_GREEN, 170))
    if tex == "staff_gem":
        f.fill(lambda x, y: mix(SOUL_GREEN, (255, 255, 255), 0.3 if (x + y) % 2 else 0.0))
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba(SOUL_GREEN, 230)
    if tex in ("crown", "crown_spike"):
        material((220, 176, 64), grain=0.1, gradient=0.3)(f)


LICH_COLORS = {"head": (214, 206, 182), "crown": (120, 110, 60), "crown_spike": (120, 110, 60), "body": LICH_ROBE,
               "robe": LICH_ROBE, "arm": (200, 192, 168), "leg": (200, 192, 168), "staff": (190, 150, 60),
               "staff_gem": SOUL_GREEN}


# ---------------------------------------------------------------------------
# Naga (no legs: a long snake tail)
# ---------------------------------------------------------------------------
NAGA_SCALES = (40, 116, 96)
NAGA_SKIN = (120, 160, 120)
GOLD = (230, 180, 60)


def naga_parts():
    head = Part("head", (0, -4, 0), [Cube("head", (-3.5, -11, -3.5), (7, 7, 7)),
                                     Cube("hair", (-3.5, -11.5, 0.5), (7, 9, 3)),
                                     Cube("fin", (-5.5, -11, -1), (2, 5, 4)),
                                     Cube("fin", (3.5, -11, -1), (2, 5, 4), True),
                                     Cube("fin_crest", (-0.5, -14, -2), (1, 3, 6))])
    arms = [
        Part("right_arm", (-5.5, -3, 0), [Cube("arm", (-7, -4, -1.5), (3, 11, 3))], [
            Part("spear", (-5.5, 6, 0), [Cube("spear", (-6, 5.5, -14), (1, 1, 18)),
                                         Cube("spear_tip", (-6.5, 5, -17), (2, 2, 3))])]),
        Part("left_arm", (5.5, -3, 0), [Cube("arm", (4, -4, -1.5), (3, 11, 3), True)]),
    ]
    tail4 = Part("tail4", (0, 20, 21), [Cube("tail4", (-2, 20, 21), (4, 4, 8))])
    tail3 = Part("tail3", (0, 20, 11), [Cube("tail3", (-3, 18, 11), (6, 6, 10))], [tail4])
    tail2 = Part("tail2", (0, 16, 0), [Cube("tail2", (-4, 16, -3), (8, 8, 14)), Cube("fin_crest", (-0.5, 14, 1), (1, 3, 6))],
                 [tail3])
    tail = Part("tail", (0, 8, 0), [Cube("tail1", (-4.5, 8, -3), (9, 8, 6))], [tail2])
    body = Part("body", (0, 8, 0), [Cube("body", (-4, -4, -2), (8, 12, 4)),
                                    Cube("jewel", (-2, -3, -2.5), (4, 2, 1))], [head] + arms + [tail])
    return Part("base", (0, 0, 0), [], [body])


def naga_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 3, (1, 5), (255, 230, 40), 230)
        f.put(2, 3, (20, 20, 10))
        f.put(4, 3, (20, 20, 10))
    if tex.startswith("tail") or tex == "body":
        for y in range(f.h):
            for x in range(f.w):
                if (x + y * 2) % 4 == 0:
                    f.put(x, y, mul(f.get(x, y), 0.8))
        if f.name in ("down", "north") and tex.startswith("tail"):
            f.fill(lambda x, y: mix((220, 210, 150), (180, 170, 110), (y % 3) / 2))
    if tex in ("fin", "fin_crest"):
        f.fill(lambda x, y: mix((60, 200, 170), (20, 90, 90), y / max(1, f.h - 1)) if (x + y) % 3 else (230, 200, 90))
    if tex == "jewel" or tex == "spear_tip":
        material(GOLD, grain=0.08, gradient=0.4)(f)


NAGA_COLORS = {"fin": (60, 200, 170), "fin_crest": (60, 200, 170), "head": NAGA_SKIN, "hair": (20, 30, 26), "arm": NAGA_SKIN, "body": NAGA_SKIN, "jewel": GOLD,
               "tail1": NAGA_SCALES, "tail2": NAGA_SCALES, "tail3": NAGA_SCALES, "tail4": mul(NAGA_SCALES, 0.9),
               "spear": WOOD, "spear_tip": GOLD}


# ---------------------------------------------------------------------------
# Mimic (a chest with teeth)
# ---------------------------------------------------------------------------
CHEST_WOOD = (150, 104, 54)
IRON = (70, 70, 74)


def mimic_parts():
    lid = Part("lid", (0, 15, 7), [
        Cube("lid", (-7, 10, -7), (14, 5, 14)),
        Cube("teeth_top", (-6, 15, -6.5), (12, 1, 1)),
        Cube("latch", (-1, 12, -8), (2, 4, 1)),
    ])
    body = Part("body", (0, 15, 0), [
        Cube("box", (-7, 15, -7), (14, 9, 14)),
        Cube("teeth_bottom", (-6, 14, -6.5), (12, 1, 1)),
        Cube("tongue", (-2, 14.5, -5), (4, 1, 9)),
    ], [lid])
    return Part("base", (0, 0, 0), [], [body])


def mimic_details(tex, f):
    if tex in ("lid", "box") and f.side:
        for y in range(f.h):
            for x in range(f.w):
                if x in (0, f.w - 1) or y in (0, f.h - 1):
                    f.put(x, y, IRON)
    if tex == "lid" and f.name == "down":
        f.fill(lambda x, y: (90, 20, 26))
    if tex == "box" and f.name == "up":
        f.fill(lambda x, y: (70, 14, 20))
    if tex in ("teeth_top", "teeth_bottom"):
        f.fill(lambda x, y: (240, 236, 220) if x % 2 == 0 else CLEAR)
    if tex == "tongue":
        f.fill(lambda x, y: (200, 60, 80))
    if tex == "latch":
        material((200, 190, 120), grain=0.05)(f)
        if f.name == "north":
            f.put(0, 2, (255, 60, 40), (255, 60, 40))
            f.put(1, 2, (255, 60, 40), (255, 60, 40))


MIMIC_COLORS = {"lid": CHEST_WOOD, "box": CHEST_WOOD, "teeth_top": (240, 236, 220), "teeth_bottom": (240, 236, 220),
                "tongue": (200, 60, 80), "latch": (200, 190, 120)}


# ---------------------------------------------------------------------------
# Treant (walking tree)
# ---------------------------------------------------------------------------
BARK = (92, 66, 42)
LEAVES = (62, 122, 52)


def treant_parts():
    root, a = biped((8, 8, 7), (9, 16, 7), (4, 16, 4), (4, 8, 4))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes.append(Cube("foliage", (-6, ht - 5, -5), (12, 7, 10)))
    for side, sign in (("right", -1), ("left", 1)):
        arm = find(root, side + "_arm")
        x = sign * a["arm_x"]
        arm.cubes.append(Cube("leaves", (x - 3.5, a["body_top"] - 3, -3), (7, 5, 6), sign > 0))
        arm.cubes.append(Cube("twig", (x - 0.5, a["hand_y"] + 1, -2), (1, 3, 1), sign > 0))
    return root


def treant_details(tex, f):
    if tex in ("body", "arm", "leg", "head", "twig") and f.side:
        for x in range(f.w):
            if x % 3 == 1:
                for y in range(f.h):
                    f.put(x, y, mul(BARK, 0.75))
        for y in range(f.h):
            for x in range(f.w):
                if f.n(x, y, 31) > 0.85:
                    f.put(x, y, (70, 110, 50))
    if tex == "head" and f.name == "north":
        for x in range(1, 7):
            f.put(x, 3, (30, 20, 12))
        eyes(f, 3, (2, 5), (130, 255, 80), 240)
        for x in range(2, 6):
            f.put(x, 6, (30, 20, 12))
    if tex in ("foliage", "leaves"):
        f.fill(lambda x, y: mix(LEAVES, (100, 160, 70), f.n(x, y)) if f.n(x, y, 4) > 0.15 else CLEAR)
    if tex == "leg" and f.side:
        for x in range(f.w):
            f.put(x, f.h - 1, (60, 44, 28))


TREANT_COLORS = {"head": BARK, "foliage": LEAVES, "leaves": LEAVES, "body": BARK, "arm": BARK, "leg": mul(BARK, 0.9),
                 "twig": BARK}


# ---------------------------------------------------------------------------
# Basilisk (huge lizard with a crest)
# ---------------------------------------------------------------------------
LIZARD = (44, 76, 48)


def basilisk_parts():
    legs = []
    for i, (x, z) in enumerate(((-4, -6), (4, -6), (-4, 6), (4, 6))):
        legs.append(Part(f"leg{i}", (x, 16, z), [Cube("leg", (x - 1.5, 16, z - 1.5), (3, 8, 3), x > 0)],
                         rot=(0, 0, 0.35 if x < 0 else -0.35)))
    head = Part("head", (0, 14, -9), [
        Cube("head", (-3.5, 11, -16), (7, 5, 7)),
        Cube("jaw", (-3, 16, -16), (6, 2, 6)),
        Cube("crest", (-0.5, 7, -15), (1, 4, 9)),
        Cube("frill", (-5.5, 8, -10), (11, 7, 1)),
        Cube("horn", (-3.5, 8, -13), (1, 3, 1)),
        Cube("horn", (2.5, 8, -13), (1, 3, 1)),
    ])
    wings = [Part("right_wing", (-3, 12, -3), [Cube("wing", (-17, 4, -3), (14, 10, 1))], rot=(0.3, 0.3, -0.5)),
             Part("left_wing", (3, 12, -3), [Cube("wing", (3, 4, -3), (14, 10, 1), True)], rot=(0.3, -0.3, 0.5))]
    tail = Part("tail", (0, 14, 9), [Cube("tail", (-2.5, 12, 9), (5, 4, 10))], [
        Part("tail_end", (0, 14, 19), [Cube("tail_end", (-1.5, 13, 19), (3, 3, 10))])])
    body = Part("body", (0, 15, 0), [
        Cube("body", (-4, 12, -9), (8, 6, 18)),
        Cube("spike", (-1, 10, -6), (2, 2, 3)),
        Cube("spike", (-1, 10, -1), (2, 2, 3)),
        Cube("spike", (-1, 10, 4), (2, 2, 3)),
    ], [head, tail] + legs + wings)
    return Part("base", (0, 0, 0), [], [body])


def basilisk_details(tex, f):
    if tex == "head" and f.name in ("east", "west"):
        eyes(f, 1, (2,), (255, 230, 40), 255)
    if tex == "head" and f.name == "north":
        eyes(f, 1, (1, 5), (255, 230, 40), 255)
    if tex in ("body", "tail", "tail_end", "head") and f.side:
        for y in range(f.h):
            for x in range(f.w):
                if (x + y) % 3 == 0:
                    f.put(x, y, mul(f.get(x, y), 0.8))
    if tex in ("body", "tail", "jaw") and f.name == "down":
        f.fill(lambda x, y: (170, 170, 110))
    if tex == "frill" and f.side:
        f.fill(lambda x, y: mix((60, 110, 60), (170, 50, 40), y / max(1, f.h - 1)) if (x % 3) else (30, 50, 30))
    if tex == "wing" and f.side:
        f.fill(lambda x, y: mix((40, 70, 44), (90, 130, 70), y / max(1, f.h - 1)) if y < f.h - (x % 4) else CLEAR)
    if tex in ("crest", "spike"):
        f.fill(lambda x, y: mix((200, 40, 40), (120, 20, 20), y / max(1, f.h - 1)))


BASILISK_COLORS = {"frill": (60, 110, 60), "wing": (50, 80, 50), "head": LIZARD, "jaw": mul(LIZARD, 0.9), "crest": (190, 40, 40), "body": LIZARD,
                   "spike": (190, 40, 40), "horn": (200, 190, 160), "tail": LIZARD, "tail_end": mul(LIZARD, 0.9), "leg": mul(LIZARD, 0.85)}


# ===========================================================================
# Spiders, insects, beastmen, ghouls and demons
# ===========================================================================
def legs_sideways(count_per_side, x, y, zs, length, drop, prefix="leg"):
    """Long legs sticking out sideways and bending to the ground (spiders)."""
    import math
    parts = []
    angle = math.asin(min(0.95, drop / length))
    i = 0
    for z in zs:
        for sign in (-1, 1):
            px = x * sign
            o = (px - length, y - 1, z - 1) if sign < 0 else (px, y - 1, z - 1)
            parts.append(Part(f"{prefix}{i}", (px, y, z), [Cube("leg", o, (length, 2, 2), sign > 0)],
                              rot=(0, 0, -angle if sign < 0 else angle)))
            i += 1
    return parts


# --------------------------------------------------------------------------- giant spider
SPIDER = (54, 44, 40)


def spider_parts():
    legs = legs_sideways(4, 4, 16, (-4, -1.5, 1, 3.5), 14, 8)
    head = Part("head", (0, 15, -5), [
        Cube("head", (-4, 12, -12), (8, 6, 7)),
        Cube("fang", (-2.5, 17, -12.5), (1, 3, 1)),
        Cube("fang", (1.5, 17, -12.5), (1, 3, 1), True),
    ])
    abdomen = Part("tail", (0, 14, 3), [Cube("abdomen", (-6, 9, 3), (12, 10, 14))], rot=(0.2, 0, 0))
    body = Part("body", (0, 15, 0), [Cube("thorax", (-4, 13, -5), (8, 6, 8))], [head, abdomen] + legs)
    return Part("base", (0, 0, 0), [], [body])


def spider_details(tex, f):
    if tex == "head" and f.name == "north":
        for x, y in ((1, 1), (2, 1), (5, 1), (6, 1), (2, 2), (5, 2), (3, 0), (4, 0)):
            f.put(x, y, (200, 20, 30), rgba((255, 40, 40), 230))
    if tex == "abdomen":
        for y in range(f.h):
            for x in range(f.w):
                if f.n(x, y, 3) > 0.75:
                    f.put(x, y, mul(SPIDER, 1.4))
        if f.name == "up":
            for y, w in ((3, 1), (4, 2), (5, 3), (6, 2), (7, 1), (8, 2), (9, 3), (10, 2)):
                for x in range(f.w // 2 - w, f.w // 2 + w):
                    f.put(x, y, (170, 20, 24), rgba((200, 20, 30), 90))
    if tex == "leg" and f.side:
        for x in range(f.w):
            if x % 4 == 3:
                f.put(x, 0, mul(SPIDER, 1.5))
                f.put(x, 1, mul(SPIDER, 1.5))
    if tex == "fang":
        f.fill(lambda x, y: (30, 26, 24))


SPIDER_COLORS = {"head": SPIDER, "fang": (30, 26, 24), "abdomen": mul(SPIDER, 0.9), "thorax": SPIDER,
                 "leg": mul(SPIDER, 0.8)}


# --------------------------------------------------------------------------- mantis
MANTIS = (110, 180, 70)


def mantis_parts():
    legs = []
    for i, (x, z) in enumerate(((-2, 0), (2, 0), (-2, 5), (2, 5))):
        legs.append(Part(f"leg{i}", (x, 14, z), [Cube("leg", (x - 0.5, 14, z - 0.5), (1, 11, 1), x > 0)],
                         rot=(0, 0, 0.5 if x < 0 else -0.5)))
    claws = []
    for side, sign in (("right", -1), ("left", 1)):
        x = 2 * sign
        o1 = (x - 0.5 + sign * 0.5, 6, -9)
        claws.append(Part(f"{side}_claw", (x, 6, -3), [
            Cube("claw_arm", (x - 0.5 + 0.5 * sign - 0.5, 6, -9), (1, 2, 6), sign > 0),
            Cube("claw_blade", (x - 0.5 + 0.5 * sign - 0.5, 8, -9), (1, 6, 1), sign > 0),
        ]))
    head = Part("head", (0, 4, -2), [
        Cube("head", (-3, 0, -5), (6, 4, 4)),
        Cube("eye", (-3.5, 0.5, -4), (1, 2, 2)),
        Cube("eye", (2.5, 0.5, -4), (1, 2, 2), True),
        Cube("antenna", (-1.5, -3, -4), (1, 3, 1)),
        Cube("antenna", (0.5, -3, -4), (1, 3, 1), True),
    ])
    abdomen = Part("tail", (0, 14, 1), [Cube("abdomen", (-3, 12, 1), (6, 5, 12))], rot=(-0.15, 0, 0))
    body = Part("body", (0, 14, 0), [Cube("thorax", (-2, 4, -3), (4, 11, 4))], [head, abdomen] + claws + legs)
    return Part("base", (0, 0, 0), [], [body])


def mantis_details(tex, f):
    if tex == "eye":
        f.fill(lambda x, y: (200, 255, 120))
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba((180, 255, 100), 200)
    if tex == "claw_blade" and f.side:
        for y in range(f.h):
            f.put(0, y, (230, 240, 200) if y % 2 else mul(MANTIS, 0.6))
    if tex in ("abdomen", "thorax") and f.side:
        stripes(f, 3, mul(MANTIS, 0.75))


MANTIS_COLORS = {"head": MANTIS, "eye": (200, 255, 120), "antenna": mul(MANTIS, 0.8), "thorax": MANTIS,
                 "abdomen": mul(MANTIS, 1.1), "claw_arm": MANTIS, "claw_blade": mul(MANTIS, 0.9), "leg": mul(MANTIS, 0.8)}


# --------------------------------------------------------------------------- armored beetle
BEETLE = (34, 40, 70)


def beetle_parts():
    legs = []
    i = 0
    for z in (-3, 1, 5):
        for sign in (-1, 1):
            x = 5 * sign
            legs.append(Part(f"leg{i}", (x, 16, z), [Cube("leg", (x - 0.5, 16, z - 0.5), (1, 9, 1), sign > 0)],
                             rot=(0, 0, 0.6 * -sign)))
            i += 1
    head = Part("head", (0, 14, -6), [
        Cube("head", (-3, 12, -10), (6, 5, 4)),
        Cube("mandible", (-3, 16, -12), (1, 1, 3)),
        Cube("mandible", (2, 16, -12), (1, 1, 3), True),
    ], [Part("horn", (0, 12, -10), [Cube("horn", (-1, 5, -11), (2, 7, 2))], rot=(-0.5, 0, 0))])
    body = Part("body", (0, 14, 0), [Cube("shell", (-6, 9, -6), (12, 9, 14))], [head] + legs)
    return Part("base", (0, 0, 0), [], [body])


def beetle_details(tex, f):
    if tex == "shell":
        f.fill(lambda x, y: mix(BEETLE, (40, 120, 90), 0.5 * f.n(x, y, 2) + 0.2 * (y / max(1, f.h))))
        if f.name == "up":
            for y in range(f.h):
                f.put(f.w // 2, y, (14, 16, 30))
                f.put(f.w // 2 - 1, y, (14, 16, 30))
    if tex == "head" and f.name == "north":
        eyes(f, 1, (0, 5), (255, 160, 40), 200)
    if tex == "horn":
        f.fill(lambda x, y: mix((20, 20, 26), (90, 90, 100), 1 - y / max(1, f.h - 1)))


BEETLE_COLORS = {"shell": BEETLE, "head": mul(BEETLE, 0.8), "mandible": (20, 20, 26), "horn": (20, 20, 26),
                 "leg": mul(BEETLE, 0.7)}


# --------------------------------------------------------------------------- giant wasp
WASP_YELLOW = (240, 190, 40)
WASP_BLACK = (30, 26, 22)


def wasp_parts():
    legs = []
    for i, (x, z) in enumerate(((-2, -2), (2, -2), (-2, 0), (2, 0), (-2, 2), (2, 2))):
        legs.append(Part(f"leg{i}", (x, 15, z), [Cube("leg", (x - 0.5, 15, z - 0.5), (1, 5, 1), x > 0)]))
    head = Part("head", (0, 12, -3), [Cube("head", (-2.5, 9, -7), (5, 5, 4)),
                                      Cube("mandible", (-1.5, 13, -8), (3, 1, 1))])
    abdomen = Part("tail", (0, 12, 2), [Cube("abdomen", (-3, 10, 2), (6, 6, 9)),
                                        Cube("stinger", (-0.5, 12.5, 11), (1, 1, 3))], rot=(0.3, 0, 0))
    wings = [Part("right_wing", (-1, 10, -1), [Cube("wing", (-10, 9.5, -2), (9, 1, 5))], rot=(0, 0, 0.2)),
             Part("left_wing", (1, 10, -1), [Cube("wing", (1, 9.5, -2), (9, 1, 5), True)], rot=(0, 0, -0.2))]
    body = Part("body", (0, 12, 0), [Cube("thorax", (-2.5, 10, -3), (5, 5, 5))], [head, abdomen] + wings + legs)
    return Part("base", (0, 0, 0), [], [body])


def wasp_details(tex, f):
    if tex == "abdomen" and f.side:
        f.fill(lambda x, y: WASP_YELLOW if (y // 2) % 2 == 0 else WASP_BLACK)
    if tex == "head" and f.name == "north":
        for x in (0, 1, 3, 4):
            f.put(x, 1, (200, 30, 20), rgba((255, 40, 30), 220))
    if tex == "wing":
        f.fill(lambda x, y: (220, 230, 240, 150) if not f.border(x, y) else (150, 150, 160, 200))
    if tex == "stinger":
        f.fill(lambda x, y: (20, 20, 20))


WASP_COLORS = {"head": WASP_BLACK, "mandible": (60, 40, 20), "thorax": WASP_BLACK, "abdomen": WASP_YELLOW,
               "stinger": (20, 20, 20), "wing": (220, 230, 240), "leg": WASP_BLACK}


# --------------------------------------------------------------------------- werewolf
WEREWOLF = (78, 66, 58)


def werewolf_parts():
    root, a = biped((7, 7, 7), (10, 12, 6), (4, 14, 4), (4, 11, 4))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [
        Cube("snout", (-2, ht + 3, -6.5), (4, 3, 3)),
        Cube("ear", (-3, ht - 2, -1), (2, 3, 1)),
        Cube("ear", (1, ht - 2, -1), (2, 3, 1), True),
    ]
    body = find(root, "body")
    body.cubes.append(Cube("mane", (-5.5, a["body_top"] - 1, -3.5), (11, 6, 8)))
    body.children.append(Part("tail", (0, a["leg_top"] - 2, 3), [Cube("tail", (-1, a["leg_top"] - 3, 3), (2, 2, 9))],
                              rot=(-0.7, 0, 0)))
    return root


def werewolf_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 3, (1, 5), (255, 220, 40), 240)
    if tex == "snout" and f.name == "north":
        f.put(1, 0, (20, 16, 14))
        f.put(2, 0, (20, 16, 14))
        for x in range(f.w):
            f.put(x, f.h - 1, (240, 236, 220) if x % 2 == 0 else (60, 20, 20))
    if tex == "arm" and f.side:
        for x in range(f.w):
            f.put(x, f.h - 1, (230, 226, 210) if x % 2 == 0 else WEREWOLF)
    if tex in ("body", "mane", "arm", "leg") and f.side:
        for y in range(f.h):
            for x in range(f.w):
                if f.n(x, y, 13) > 0.78:
                    f.put(x, y, mul(WEREWOLF, 1.35))


WEREWOLF_COLORS = {"head": WEREWOLF, "snout": mul(WEREWOLF, 1.1), "ear": mul(WEREWOLF, 0.9), "body": WEREWOLF,
                   "mane": mul(WEREWOLF, 0.75), "tail": WEREWOLF, "arm": WEREWOLF, "leg": mul(WEREWOLF, 0.9)}


# --------------------------------------------------------------------------- rat man
RAT = (110, 98, 86)
RAGS = (86, 70, 50)


def ratman_parts():
    root, a = biped((6, 6, 6), (6, 8, 4), (2, 8, 2), (2, 7, 2))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [
        Cube("snout", (-1.5, ht + 3, -5), (3, 2, 2)),
        Cube("ear", (-3.5, ht - 1, 0), (2, 2, 1)),
        Cube("ear", (1.5, ht - 1, 0), (2, 2, 1), True),
    ]
    body = find(root, "body")
    body.children.append(Part("tail", (0, a["leg_top"] - 1, 2), [Cube("tail", (-0.5, a["leg_top"] - 1, 2), (1, 1, 11))],
                              rot=(-0.3, 0, 0)))
    right = find(root, "right_arm")
    hx, hy = -a["arm_x"], a["hand_y"]
    right.children.append(Part("dagger", (hx, hy, 0), [Cube("dagger", (hx - 0.5, hy - 0.5, -5), (1, 1, 4))]))
    return root


def ratman_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 2, (1, 4), (255, 40, 40), 220)
    if tex == "snout" and f.name == "north":
        f.put(1, 0, (230, 140, 150))
    if tex in ("ear", "tail"):
        f.fill(lambda x, y: (220, 150, 150))
    if tex == "body" and f.side:
        for y in range(f.h):
            for x in range(f.w):
                f.put(x, y, mix(RAGS, (60, 50, 36), f.n(x, y)))
    if tex == "dagger":
        f.fill(lambda x, y: (150, 110, 80) if f.n(x, y) > 0.5 else (120, 120, 126))


RATMAN_COLORS = {"head": RAT, "snout": mul(RAT, 1.1), "ear": (220, 150, 150), "tail": (220, 150, 150), "body": RAGS,
                 "arm": RAT, "leg": mul(RAT, 0.85), "dagger": (130, 130, 136)}


# --------------------------------------------------------------------------- lizardman
LIZARD_GREEN = (70, 130, 60)


def lizardman_parts():
    root, a = biped((6, 6, 8), (8, 12, 5), (3, 12, 3), (3, 12, 3))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes.append(Cube("crest", (-0.5, ht - 2, -2), (1, 2, 6)))
    body = find(root, "body")
    lt = a["leg_top"]
    body.children.append(Part("tail", (0, lt - 1, 2.5), [Cube("tail", (-1.5, lt - 2, 2.5), (3, 3, 10))], [
        Part("tail2", (0, lt - 0.5, 12.5), [Cube("tail_end", (-1, lt - 1, 12.5), (2, 2, 7))])], rot=(-0.35, 0, 0)))
    right = find(root, "right_arm")
    hx, hy = -a["arm_x"], a["hand_y"]
    right.children.append(Part("spear", (hx, hy, 0), [Cube("spear", (hx - 0.5, hy - 0.5, -17), (1, 1, 19)),
                                                      Cube("spear_tip", (hx - 1, hy - 1, -20), (2, 2, 3))]))
    left = find(root, "left_arm")
    ax = a["arm_x"]
    left.cubes.append(Cube("shield", (ax + 1.5, a["body_top"] + 2, -4), (1, 9, 8)))
    return root


def lizardman_details(tex, f):
    if tex == "head" and f.name in ("north", "east", "west"):
        eyes(f, 2, (1, f.w - 2) if f.name == "north" else (2,), (255, 200, 40), 220)
    if tex in ("body", "arm", "leg", "head", "tail", "tail_end") and f.side:
        for y in range(f.h):
            for x in range(f.w):
                if (x + 2 * y) % 4 == 0:
                    f.put(x, y, mul(f.get(x, y), 0.8))
    if tex == "body" and f.name == "north":
        for y in range(f.h):
            for x in range(2, f.w - 2):
                f.put(x, y, (200, 190, 120) if y % 2 else (180, 170, 100))
    if tex == "crest":
        f.fill(lambda x, y: (210, 70, 40))
    if tex == "shield":
        f.fill(lambda x, y: (120, 84, 50) if not f.border(x, y) else (180, 140, 60))
        if f.name in ("east",):
            f.put(f.w // 2, f.h // 2, (200, 60, 40))
    if tex == "spear_tip":
        material((190, 150, 70), grain=0.05, gradient=0.4)(f)


LIZARDMAN_COLORS = {"head": LIZARD_GREEN, "crest": (210, 70, 40), "body": LIZARD_GREEN, "arm": LIZARD_GREEN,
                    "leg": mul(LIZARD_GREEN, 0.85), "tail": LIZARD_GREEN, "tail_end": mul(LIZARD_GREEN, 0.9),
                    "spear": WOOD, "spear_tip": (190, 150, 70), "shield": (120, 84, 50)}


# --------------------------------------------------------------------------- ghoul
GHOUL = (150, 158, 138)


def ghoul_parts():
    root, a = biped((7, 7, 7), (7, 11, 4), (3, 15, 3), (3, 11, 3))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes.append(Cube("jaw", (-3, ht + 7, -3.5), (6, 2, 5)))
    body = find(root, "body")
    body.cubes.append(Cube("rags", (-4, a["leg_top"] - 3, -2.5), (8, 6, 5)))
    return root


def ghoul_details(tex, f):
    if tex == "head" and f.name == "north":
        for x, y in ((1, 2), (2, 2), (4, 2), (5, 2), (1, 3), (5, 3)):
            f.put(x, y, (20, 18, 16))
        eyes(f, 3, (2, 4), (230, 255, 230), 240)
        for x in range(1, 6):
            f.put(x, 5, (90, 20, 20))
            f.put(x, 6, (240, 236, 220) if x % 2 else (90, 20, 20))
    if tex == "jaw" and f.name == "north":
        for x in range(f.w):
            f.put(x, 0, (240, 236, 220) if x % 2 == 0 else (90, 20, 20))
    if tex == "body" and f.name == "north":
        for y in range(1, f.h - 2, 2):
            for x in range(1, f.w - 1):
                f.put(x, y, mul(GHOUL, 0.7))
    if tex == "arm" and f.side:
        for x in range(f.w):
            f.put(x, f.h - 1, (40, 36, 30))
            f.put(x, f.h - 2, (40, 36, 30) if x % 2 else GHOUL)
    if tex == "rags":
        f.fill(lambda x, y: mix((70, 60, 50), (50, 44, 36), f.n(x, y)) if not (y == f.h - 1 and x % 2) else CLEAR)


GHOUL_COLORS = {"head": GHOUL, "jaw": mul(GHOUL, 0.9), "body": mul(GHOUL, 0.95), "rags": (70, 60, 50), "arm": GHOUL,
                "leg": mul(GHOUL, 0.9)}


# --------------------------------------------------------------------------- imp
IMP = (190, 50, 40)


def imp_parts():
    root, a = biped((6, 6, 6), (5, 6, 3), (2, 6, 2), (2, 6, 2))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes += [Cube("horn", (-3, ht - 2, -1), (1, 2, 1)), Cube("horn", (2, ht - 2, -1), (1, 2, 1), True)]
    body = find(root, "body")
    bt = a["body_top"]
    body.children += [
        Part("right_wing", (-1, bt + 1, 1.5), [Cube("wing", (-7, bt - 1, 1.5), (6, 6, 1))], rot=(0, 0.5, 0)),
        Part("left_wing", (1, bt + 1, 1.5), [Cube("wing", (1, bt - 1, 1.5), (6, 6, 1), True)], rot=(0, -0.5, 0)),
        Part("tail", (0, a["leg_top"] - 1, 1.5), [Cube("tail", (-0.5, a["leg_top"] - 1, 1.5), (1, 1, 7))], rot=(-0.4, 0, 0)),
    ]
    return root


def imp_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 2, (1, 4), (255, 230, 60), 255)
        for x in range(1, 5):
            f.put(x, 4, (60, 10, 10) if x % 2 else (240, 230, 210))
    if tex == "horn":
        f.fill(lambda x, y: (30, 24, 24))
    if tex == "wing" and f.side:
        f.fill(lambda x, y: mix((90, 20, 20), (140, 40, 30), y / max(1, f.h - 1)) if y < f.h - (x % 3) else CLEAR)


IMP_COLORS = {"head": IMP, "horn": (30, 24, 24), "body": IMP, "arm": IMP, "leg": mul(IMP, 0.85), "wing": (110, 26, 24),
              "tail": IMP}


# --------------------------------------------------------------------------- succubus
SUCCUBUS_SKIN = (176, 110, 150)
LEATHER_DARK = (46, 20, 38)


def succubus_parts():
    root, a = biped((6, 7, 6), (6, 12, 3), (2, 12, 2), (2, 12, 2))
    head = find(root, "head")
    ht = a["head_top"]
    head.cubes.append(Cube("hair", (-3.5, ht - 0.5, 0.5), (7, 11, 3)))
    head.children += [
        Part("right_horn", (-2.5, ht + 1, 0), [Cube("horn", (-3.5, ht - 2, -1), (2, 3, 2))], rot=(-0.4, 0, -0.3)),
        Part("left_horn", (2.5, ht + 1, 0), [Cube("horn", (1.5, ht - 2, -1), (2, 3, 2), True)], rot=(-0.4, 0, 0.3)),
    ]
    body = find(root, "body")
    bt = a["body_top"]
    body.children += [
        Part("right_wing", (-1, bt + 1, 1.5), [Cube("wing", (-11, bt - 3, 1.5), (10, 13, 1))], rot=(0, 0.5, 0)),
        Part("left_wing", (1, bt + 1, 1.5), [Cube("wing", (1, bt - 3, 1.5), (10, 13, 1), True)], rot=(0, -0.5, 0)),
        Part("tail", (0, a["leg_top"] - 1, 1.5), [Cube("tail", (-0.5, a["leg_top"] - 1, 1.5), (1, 1, 10)),
                                                    Cube("tail_tip", (-1, a["leg_top"] - 1.5, 11.5), (2, 2, 1))],
             rot=(-0.3, 0, 0)),
    ]
    return root


def succubus_details(tex, f):
    if tex == "head" and f.name == "north":
        eyes(f, 3, (1, 4), (255, 80, 200), 255)
        f.put(2, 5, (150, 30, 70))
        f.put(3, 5, (150, 30, 70))
    if tex == "body" and f.side:
        for y in range(f.h):
            for x in range(f.w):
                if y < 4 or y > 7:
                    f.put(x, y, LEATHER_DARK if y > 7 or f.name != "north" or y > 1 else f.get(x, y))
    if tex == "leg" and f.side:
        for y in range(4, f.h):
            for x in range(f.w):
                f.put(x, y, LEATHER_DARK)
    if tex == "wing" and f.side:
        f.fill(lambda x, y: mix((60, 20, 50), (110, 30, 80), y / max(1, f.h - 1)) if y < f.h - (x % 4) else CLEAR)
        for x in range(0, f.w, 3):
            for y in range(f.h - 3):
                f.put(x, y, (30, 10, 26))
    if tex in ("horn",):
        f.fill(lambda x, y: (30, 20, 30))
    if tex == "hair":
        f.fill(lambda x, y: mix((20, 12, 24), (60, 30, 70), f.n(x, y)))


SUCCUBUS_COLORS = {"head": SUCCUBUS_SKIN, "hair": (20, 12, 24), "horn": (30, 20, 30), "body": SUCCUBUS_SKIN,
                   "arm": SUCCUBUS_SKIN, "leg": SUCCUBUS_SKIN, "wing": (90, 26, 70), "tail": SUCCUBUS_SKIN,
                   "tail_tip": (60, 20, 50)}


# --------------------------------------------------------------------------- greater demon
DEMON_SKIN = (88, 24, 20)


def greater_demon_parts():
    root, a = biped((8, 8, 8), (14, 14, 8), (6, 16, 6), (6, 12, 6))
    head = find(root, "head")
    ht = a["head_top"]
    head.children += [
        Part("right_horn", (-3.5, ht + 2, 0), [Cube("big_horn", (-5, ht - 6, -1.5), (3, 8, 3))], rot=(-0.3, 0, -0.5)),
        Part("left_horn", (3.5, ht + 2, 0), [Cube("big_horn", (2, ht - 6, -1.5), (3, 8, 3), True)], rot=(-0.3, 0, 0.5)),
    ]
    head.cubes.append(Cube("jaw", (-3.5, ht + 6, -4.5), (7, 3, 2)))
    body = find(root, "body")
    bt = a["body_top"]
    body.children += [
        Part("right_wing", (-2, bt + 2, 4), [Cube("big_wing", (-20, bt - 6, 4), (18, 22, 1))], rot=(0, 0.45, 0)),
        Part("left_wing", (2, bt + 2, 4), [Cube("big_wing", (2, bt - 6, 4), (18, 22, 1), True)], rot=(0, -0.45, 0)),
        Part("tail", (0, a["leg_top"] - 2, 4), [Cube("demon_tail", (-1.5, a["leg_top"] - 3, 4), (3, 3, 14))], rot=(-0.5, 0, 0)),
    ]
    right = find(root, "right_arm")
    hx, hy = -a["arm_x"], a["hand_y"]
    right.children.append(Part("sword", (hx, hy, 0), [
        Cube("flame_grip", (hx - 1, hy - 1, -3), (2, 2, 4)),
        Cube("flame_blade", (hx - 1, hy - 1.5, -24), (2, 3, 21)),
    ]))
    return root


def greater_demon_details(tex, f):
    if tex in ("body", "arm", "leg", "head"):
        for i in range(2):
            x = int(f.n(i, 4, 9) * f.w)
            for y in range(f.h):
                if f.n(x, y, 17) > 0.3:
                    f.put(x, y, mix(EMBER, (255, 220, 120), 0.3), rgba(EMBER, 200))
                x = max(0, min(f.w - 1, x + (1 if f.n(y, i, 2) > 0.6 else -1 if f.n(y, i, 2) < 0.3 else 0)))
    if tex == "head" and f.name == "north":
        eyes(f, 3, (1, 2, 5, 6), (255, 200, 60), 255)
    if tex == "jaw" and f.name == "north":
        for x in range(f.w):
            f.put(x, 0, (240, 230, 200) if x % 2 == 0 else (40, 10, 10))
    if tex == "big_horn":
        f.fill(lambda x, y: mix((20, 18, 18), (130, 120, 110), 1 - y / max(1, f.h - 1)))
    if tex == "big_wing" and f.side:
        f.fill(lambda x, y: mix((40, 10, 10), (90, 20, 16), y / max(1, f.h - 1)) if y < f.h - (x % 5) else CLEAR)
        for x in range(0, f.w, 4):
            for y in range(f.h - 4):
                f.put(x, y, (20, 6, 6))
    if tex == "flame_blade":
        f.fill(lambda x, y: mix((255, 120, 30), (255, 230, 120), f.n(x, y)))
        for y in range(f.h):
            for x in range(f.w):
                f.glow[y][x] = rgba((255, 140, 40), 230)


GREATER_DEMON_COLORS = {"head": DEMON_SKIN, "big_horn": (20, 18, 18), "jaw": mul(DEMON_SKIN, 0.8), "body": DEMON_SKIN,
                        "arm": DEMON_SKIN, "leg": mul(DEMON_SKIN, 0.85), "big_wing": (60, 14, 12),
                        "demon_tail": DEMON_SKIN, "flame_grip": (30, 20, 20), "flame_blade": (255, 140, 40)}


import hd

MATERIALS = {
    "goblin": ("skin", {}), "goblin_shaman": ("skin", {}), "ice_elf": ("skin", {"body": "metal", "leg": "metal"}),
    "high_orc": ("skin", {"body": "cloth", "leg": "cloth"}), "demon_knight": ("metal", {}),
    "steelfang_wolf": ("fur", {}), "ant_soldier": ("chitin", {}), "troll": ("stone", {"moss": "leaf", "loincloth": "cloth"}),
    "minotaur": ("fur", {"strap": "smooth", "belt": "metal"}), "gargoyle": ("stone", {"wing": "stone"}),
    "lich": ("bone", {"body": "cloth", "staff": "metal"}), "naga": ("scales", {"head": "skin", "arm": "skin", "body": "skin", "fin": "membrane"}),
    "mimic": ("wood", {}), "treant": ("wood", {}), "basilisk": ("scales", {"frill": "membrane"}),
    "giant_spider": ("fur", {"abdomen": "chitin"}), "mantis": ("chitin", {}), "beetle": ("chitin", {}),
    "giant_wasp": ("chitin", {}), "werewolf": ("fur", {}), "ratman": ("fur", {"body": "cloth"}),
    "lizardman": ("scales", {"shield": "wood"}), "ghoul": ("skin", {}), "imp": ("skin", {}), "succubus": ("skin", {"body": "smooth"}),
    "greater_demon": ("stone", {}),
}

MOBS = {
    "goblin": ("goblin", lambda: goblin_parts(False), painter(GOBLIN_COLORS, goblin_details(False))),
    "goblin_shaman": ("goblinShaman", lambda: goblin_parts(True), painter(SHAMAN_COLORS, goblin_details(True))),
    "ice_elf": ("iceElf", elf_parts, painter(ELF_COLORS, elf_details)),
    "high_orc": ("highOrc", orc_parts, painter(ORC_COLORS, orc_details)),
    "demon_knight": ("demonKnight", demon_parts, painter(DEMON_COLORS, demon_details)),
    "steelfang_wolf": ("steelfangWolf", wolf_parts, painter(WOLF_COLORS, wolf_details)),
    "ant_soldier": ("antSoldier", ant_parts, painter(ANT_COLORS, ant_details)),
    "troll": ("troll", troll_parts, painter(TROLL_COLORS, troll_details)),
    "minotaur": ("minotaur", minotaur_parts, painter(MINOTAUR_COLORS, minotaur_details)),
    "gargoyle": ("gargoyle", gargoyle_parts, painter(GARGOYLE_COLORS, gargoyle_details)),
    "lich": ("lich", lich_parts, painter(LICH_COLORS, lich_details)),
    "naga": ("naga", naga_parts, painter(NAGA_COLORS, naga_details)),
    "mimic": ("mimic", mimic_parts, painter(MIMIC_COLORS, mimic_details)),
    "treant": ("treant", treant_parts, painter(TREANT_COLORS, treant_details)),
    "basilisk": ("basilisk", basilisk_parts, painter(BASILISK_COLORS, basilisk_details)),
    "giant_spider": ("giantSpider", spider_parts, painter(SPIDER_COLORS, spider_details)),
    "mantis": ("mantis", mantis_parts, painter(MANTIS_COLORS, mantis_details)),
    "beetle": ("beetle", beetle_parts, painter(BEETLE_COLORS, beetle_details)),
    "giant_wasp": ("giantWasp", wasp_parts, painter(WASP_COLORS, wasp_details)),
    "werewolf": ("werewolf", werewolf_parts, painter(WEREWOLF_COLORS, werewolf_details)),
    "ratman": ("ratman", ratman_parts, painter(RATMAN_COLORS, ratman_details)),
    "lizardman": ("lizardman", lizardman_parts, painter(LIZARDMAN_COLORS, lizardman_details)),
    "ghoul": ("ghoul", ghoul_parts, painter(GHOUL_COLORS, ghoul_details)),
    "imp": ("imp", imp_parts, painter(IMP_COLORS, imp_details)),
    "succubus": ("succubus", succubus_parts, painter(SUCCUBUS_COLORS, succubus_details)),
    "greater_demon": ("greaterDemon", greater_demon_parts, painter(GREATER_DEMON_COLORS, greater_demon_details)),
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
        default, overrides = MATERIALS.get(key, ("smooth", {}))
        color, glow = paint_atlas(placed, width, height, paint, hd.pattern_for(default, overrides))
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
