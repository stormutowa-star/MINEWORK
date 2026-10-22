#!/usr/bin/env python3
"""Generates block/item textures, block models, blockstates, item models, loot tables, the
English language file and NPC skins. Run from the project root: python3 tools/gen_block_assets.py

Front-face textures are drawn as the viewer sees them: texture u = 16 - x, v = 16 - y for the
north face (Minecraft's automatic UV), so models omit explicit UVs on those faces."""
import json, math, os, random
from PIL import Image, ImageDraw

random.seed(25)
A = "src/main/resources/assets/lastbroadcast"
D = "src/main/resources/data/lastbroadcast"
for p in ["textures/block", "textures/item", "textures/entity/staff", "models/block", "models/item", "blockstates", "lang"]:
    os.makedirs(f"{A}/{p}", exist_ok=True)
os.makedirs(f"{D}/loot_table/blocks", exist_ok=True)


# ============================================================ texture helper
class Tex:
    def __init__(self, res=16, base=(0, 0, 0, 0)):
        self.res = res
        self.k = res / 16.0
        if len(base) == 3:
            base = base + (255,)
        self.img = Image.new("RGBA", (res, res), base)
        self.d = ImageDraw.Draw(self.img)

    def P(self, v):
        return int(round(v * self.k))

    def rect(self, u0, v0, u1, v1, c):
        if len(c) == 3:
            c = c + (255,)
        x0, y0 = self.P(u0), self.P(v0)
        x1, y1 = max(x0, self.P(u1) - 1), max(y0, self.P(v1) - 1)
        self.d.rectangle([x0, y0, x1, y1], fill=c)
        return self

    def frame(self, u0, v0, u1, v1, c, w=1):
        for i in range(w):
            self.d.rectangle([self.P(u0) + i, self.P(v0) + i, self.P(u1) - 1 - i, self.P(v1) - 1 - i], outline=c + (255,) if len(c) == 3 else c)
        return self

    def px(self, x, y, c):
        if 0 <= x < self.res and 0 <= y < self.res:
            self.img.putpixel((x, y), c + (255,) if len(c) == 3 else c)
        return self

    def circle(self, cu, cv, r, c, outline=None):
        x0, y0 = (cu - r) * self.k, (cv - r) * self.k
        x1, y1 = (cu + r) * self.k - 1, (cv + r) * self.k - 1
        self.d.ellipse([x0, y0, x1, y1], fill=c + (255,) if len(c) == 3 else c, outline=None if outline is None else outline + (255,))
        return self

    def noise(self, amt, u0=0, v0=0, u1=16, v1=16):
        for y in range(self.P(v0), self.P(v1)):
            for x in range(self.P(u0), self.P(u1)):
                r, g, b, a = self.img.getpixel((x, y))
                if a == 0:
                    continue
                n = random.randint(-amt, amt)
                self.img.putpixel((x, y), (clamp(r + n), clamp(g + n), clamp(b + n), a))
        return self

    def hline(self, v, u0, u1, c):
        return self.rect(u0, v, u1, v + 1 / self.k, c)

    def save(self, name):
        self.img.save(f"{A}/textures/block/{name}.png")
        return self


def clamp(v):
    return max(0, min(255, v))


def shade(c, k):
    return tuple(clamp(int(x * k)) for x in c[:3])


WALNUT = (104, 66, 40)
STEEL = (118, 128, 126)
STEEL_DK = (70, 76, 76)
CHROME = (196, 202, 210)
BAKELITE = (52, 34, 24)
CREAM = (232, 224, 200)
BRASS = (196, 160, 80)
COPPER = (190, 110, 60)


def walnut(res=16, base=WALNUT):
    t = Tex(res, base)
    for x in range(res):
        k = 0.85 + 0.3 * math.sin(x * 1.7 + random.random())
        for y in range(res):
            if random.random() < 0.5:
                t.img.putpixel((x, y), shade(base, k + random.uniform(-0.05, 0.05)) + (255,))
    return t.noise(4)


def metal(base, res=16, amt=6):
    return Tex(res, base).noise(amt)


def chrome_tex(res=16):
    t = Tex(res, CHROME)
    for y in range(res):
        k = 0.8 + 0.35 * math.sin(y / res * math.pi * 2 + 0.6)
        t.rect(0, y / t.k, 16, (y + 1) / t.k, shade(CHROME, k))
    return t.noise(3)


# ============================================================ basic materials
walnut().save("walnut")
t = walnut(16, (96, 60, 38))
for u in (0, 4, 8, 12):
    t.rect(u, 0, u + 1, 16, (54, 34, 22))
t.save("walnut_panel")
chrome_tex().save("chrome")
t = chrome_tex()
t.rect(0, 0, 16, 1, (240, 244, 248)).rect(0, 15, 16, 16, (90, 94, 100)).rect(0, 7.5, 16, 8.5, (120, 124, 130))
t.save("chrome_trim")
metal(STEEL).save("steel")
metal(STEEL_DK).save("steel_dark")
metal(BAKELITE, amt=3).save("bakelite")
metal(BRASS, amt=8).save("brass")
metal(CREAM, amt=4).save("cream_metal")
metal((30, 30, 32), amt=3).save("black")
metal((236, 232, 222), amt=5).save("googie_facade")
# terrazzo: cream with coloured chips
t = Tex(16, (226, 218, 196)).noise(5)
for i in range(26):
    x, y = random.randrange(16), random.randrange(16)
    t.px(x, y, random.choice([(180, 90, 60), (60, 120, 120), (40, 40, 40), (200, 180, 120), (240, 240, 236)]))
t.save("terrazzo")
# acoustic tile
t = Tex(16, (190, 190, 184)).noise(3)
for y in range(1, 16, 3):
    for x in range(1 + (y // 3) % 2, 16, 3):
        t.px(x, y, (120, 120, 116))
t.rect(0, 0, 16, 0.5 * 16 / 16, (160, 160, 154))
t.save("studio_tile")
# atomic wallpaper: teal ground, little starbursts and boomerangs
t = Tex(16, (66, 160, 160)).noise(3)
for (cx, cy, col) in [(4, 4, (248, 230, 170)), (12, 11, (248, 230, 170))]:
    t.px(cx, cy, col)
    for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
        t.px(cx + dx, cy + dy, col)
    for dx, dy in [(-2, -2), (2, 2), (-2, 2), (2, -2)]:
        t.px(cx + dx, cy + dy, shade(col, 0.85))
for (x, y) in [(10, 3), (11, 3), (12, 4), (2, 12), (3, 13), (4, 13), (5, 12)]:
    t.px(x, y, (232, 120, 56))
t.save("atomic_wallpaper")
metal((206, 104, 50), amt=6).save("chair_fabric")
t = metal(COPPER, amt=6)
for y in range(0, 16, 2):
    t.rect(0, y, 16, y + 1, shade(COPPER, 0.7))
t.save("coil")
metal((240, 238, 230), amt=3).save("porcelain")

# ============================================================ television
t = walnut(32)
t.rect(1.5, 2.5, 12.5, 11, CHROME).frame(1.5, 2.5, 12.5, 11, shade(CHROME, 0.6))
t.rect(2, 3, 12, 10.5, (14, 18, 16))
t.rect(12.6, 2.5, 15, 13, (60, 40, 26))
for v in range(7, 12):
    t.rect(12.9, v + 0.25, 14.7, v + 0.6, (190, 160, 100))
t.rect(12.9, 2.9, 14.7, 4.2, (220, 200, 150))
t.rect(2, 11.6, 12, 12.4, BRASS)
t.save("tv_front")
t = Tex(16, (26, 36, 32))
for y in range(16):
    for x in range(16):
        dx, dy = (x - 7.5) / 8, (y - 7.5) / 8
        k = 1.0 - 0.35 * (dx * dx + dy * dy)
        t.img.putpixel((x, y), shade((34, 46, 42), k) + (255,))
t.px(3, 3, (90, 110, 104)).px(4, 3, (70, 90, 84)).px(3, 4, (70, 90, 84))
t.save("tv_screen")
t = walnut(16, (80, 52, 32))
for y in range(3, 13, 2):
    t.rect(4, y, 12, y + 1, (30, 20, 14))
t.save("tv_back")

# ============================================================ analyzer (animated scope)
frames = []
for f in range(8):
    t = metal(STEEL, 32, 4)
    t.rect(0.5, 0.5, 15.5, 2, (228, 222, 204))
    t.rect(1, 0.8, 9, 1.7, (60, 56, 50))
    # round scope (left)
    t.circle(5, 7, 4.2, (20, 26, 22), (190, 196, 204))
    t.circle(5, 7, 3.6, (10, 40, 18))
    for i in range(14):
        x = 1.6 + i * 0.5
        y = 7 + math.sin(i * 0.9 + f * 0.8) * 1.6 * (0.6 if i % 4 == 0 else 1)
        t.rect(x, y, x + 0.5, y + 0.5, (120, 255, 150))
    # meter
    t.rect(10, 3, 15, 6.5, (236, 228, 204)).frame(10, 3, 15, 6.5, (60, 50, 40))
    na = -0.7 + 0.25 * math.sin(f * 0.8)
    for s in range(6):
        t.rect(12.5 + math.sin(na) * s * 0.45, 6 - math.cos(na) * s * 0.45, 12.5 + math.sin(na) * s * 0.45 + 0.5, 6.5 - math.cos(na) * s * 0.45, (40, 20, 20))
    for i, kx in enumerate((10.5, 12.5, 14.5)):
        t.circle(kx, 9.5, 0.9, (58, 40, 30), (180, 186, 194))
    t.rect(2, 12.5, 14, 14, (60, 66, 66))
    t.circle(13.5, 12.3, 0.6, (120, 255, 140) if f % 4 < 2 else (40, 90, 50))
    frames.append(t.img)
strip = Image.new("RGBA", (32, 32 * len(frames)))
for i, fr in enumerate(frames):
    strip.paste(fr, (0, 32 * i))
strip.save(f"{A}/textures/block/analyzer_front.png")
json.dump({"animation": {"frametime": 3}}, open(f"{A}/textures/block/analyzer_front.png.mcmeta", "w"))

# ============================================================ tape deck
t = metal((206, 202, 190), 32, 3)
t.rect(1, 1, 15, 1.6, (150, 146, 136))
for (cu, cv) in [(4.6, 5.2), (11.4, 5.2)]:
    t.circle(cu, cv, 3.2, (48, 48, 50))
    t.circle(cu, cv, 0.9, CHROME, (90, 90, 96))
t.rect(7.2, 8.6, 8.8, 10.4, (60, 60, 64)).rect(7.5, 9, 8.5, 10, (180, 140, 60))
t.rect(2, 10.4, 5.6, 12.6, (236, 228, 204)).frame(2, 10.4, 5.6, 12.6, (60, 50, 40))
t.rect(3.6, 11, 3.9, 12.4, (40, 20, 20))
for i in range(5):
    t.rect(6.4 + i * 1.5, 11.6, 7.6 + i * 1.5, 13.4, (236, 236, 230) if i != 2 else (210, 60, 40))
t.rect(13.2, 10.6, 14.2, 11.6, (90, 30, 26))
t.rect(1, 14.2, 15, 15, (150, 146, 136))
t.save("recorder_front")

# ============================================================ case board
t = Tex(32, (176, 130, 84)).noise(14)
t.frame(0, 1, 16, 15, (96, 60, 38), 2)
for (u0, v0, u1, v1, c) in [(1.5, 2.5, 5.5, 6.5, (240, 236, 222)), (6.5, 2.2, 10.5, 5.6, (250, 248, 240)),
                            (11, 3, 14.5, 8, (236, 232, 210)), (2, 8, 6.5, 13, (230, 226, 210)), (8, 8.5, 12.5, 13.5, (200, 210, 220))]:
    t.rect(u0, v0, u1, v1, c)
    for v in range(int(v0 * 2) + 2, int(v1 * 2) - 1, 2):
        t.rect(u0 + 0.5, v / 2, u1 - 0.5, v / 2 + 0.25, (120, 120, 120))
t.rect(8.5, 9, 12, 12.8, (60, 70, 80))
for (u, v) in [(3.5, 2.8), (8.5, 2.5), (12.8, 3.3), (4.2, 8.3), (10.2, 8.8)]:
    t.circle(u, v, 0.4, (200, 40, 30))
d = t.d
d.line([(7, 5.6), (17, 6.6), (25, 10), (8.4 * 2, 17.6), (7, 5.6)], fill=(200, 30, 30), width=1)
t.save("case_board_front")

# ============================================================ filing cabinet, tape shelf, notice board, locker
t = metal((92, 112, 86), 16, 5)
for v in (1, 5, 9, 13):
    t.rect(1, v, 15, v + 3.4, (104, 126, 98)).frame(1, v, 15, v + 3.4, (60, 74, 56))
    t.rect(6, v + 0.6, 10, v + 1.4, (230, 226, 210))
    t.rect(6.5, v + 2, 9.5, v + 2.6, CHROME)
t.save("file_cabinet_front")
t = walnut(16)
for v in (1, 8):
    t.rect(1, v, 15, v + 6, (40, 28, 20))
    x = 1.2
    while x < 14.8:
        w = random.choice([1.2, 1.6, 2.0])
        c = random.choice([(220, 210, 180), (180, 60, 50), (60, 90, 140), (230, 230, 220), (200, 160, 60)])
        t.rect(x, v + 0.5, min(14.8, x + w - 0.2), v + 6, c)
        t.rect(x + 0.2, v + 2, min(14.6, x + w - 0.4), v + 2.6, shade(c, 0.6))
        x += w
t.save("tape_shelf_front")
t = Tex(32, (176, 130, 84)).noise(12)
t.frame(1, 2, 15, 14, (96, 60, 38), 2)
t.rect(3.5, 3, 12.5, 13.4, (250, 248, 236))
t.rect(3.5, 3, 12.5, 4.6, (200, 40, 30))
for v in range(6, 13):
    t.rect(4.2, v, 4.2 + random.uniform(5, 7.8), v + 0.35, (60, 60, 60))
t.circle(8, 3.2, 0.5, (40, 40, 160))
t.save("notice_front")
t = metal((150, 156, 160), 32, 5)
t.frame(1, 0, 15, 16, (90, 96, 100))
for v in (2, 3, 4, 12, 13, 14):
    t.rect(4, v, 12, v + 0.5, (70, 76, 80))
t.circle(8, 8, 2.2, (40, 40, 44), CHROME)
for i in range(8):
    a = i * math.pi / 4
    t.rect(8 + math.cos(a) * 1.6 - 0.2, 8 + math.sin(a) * 1.6 - 0.2, 8 + math.cos(a) * 1.6 + 0.2, 8 + math.sin(a) * 1.6 + 0.2, (220, 220, 220))
t.rect(2.5, 7, 3.5, 10, CHROME)
t.rect(5.5, 10.6, 10.5, 11.6, (230, 226, 210))
t.save("locker_front")

# ============================================================ skyhook console
t = metal((112, 126, 114), 32, 4)
for i in range(6):
    u = 1.3 + i * 2.3
    t.rect(u, 1.5, u + 1.8, 6.5, (40, 40, 40))
    t.rect(u + 0.3, 4.5 if i != 2 else 2, u + 1.5, 5.8 if i != 2 else 3.3, (200, 50, 40))
    t.circle(u + 0.9, 7.2, 0.35, (60, 200, 90) if i == 2 else (30, 60, 40))
t.save("console_low_front")
t = metal((112, 126, 114), 32, 4)
for i in range(8):
    t.rect(1 + i * 1.8, 1.5, 2.2 + i * 1.8, 2.7, [(220, 220, 214), (200, 50, 40), (220, 220, 214), (60, 120, 200)][i % 4])
t.rect(1, 4, 15, 4.4, (60, 66, 60))
t.save("console_desk")
t = metal((104, 118, 106), 32, 4)
for gu in (3.5, 9.5):
    t.circle(gu, 3.2, 2.2, (236, 232, 214), (40, 40, 40))
    t.rect(gu - 0.2, 1.6, gu + 0.3, 3.3, (40, 20, 20))
for i, c in enumerate([(255, 60, 40), (255, 190, 40), (60, 230, 90), (60, 230, 90)]):
    t.circle(13.3, 1.5 + i * 1.3, 0.45, c)
t.rect(1, 5.6, 15, 6, (60, 66, 60))
t.save("console_high_front")

# ============================================================ telephone, clock
t = metal((24, 22, 22), 16, 3)
t.circle(8, 8, 5.6, (36, 34, 34), (90, 90, 90))
for i in range(10):
    a = math.radians(-60 + i * 30)
    t.circle(8 + math.cos(a) * 3.8, 8 + math.sin(a) * 3.8, 0.75, (220, 216, 200))
t.circle(8, 8, 1.6, (230, 226, 210))
t.save("telephone_dial")
t = Tex(32, (0, 0, 0, 0))
for i in range(12):
    a = i * math.pi / 6 + 0.26
    r0, r1 = 5.2, 7.6 if i % 2 == 0 else 6.6
    for s in range(12):
        rr = r0 + (r1 - r0) * s / 11
        t.rect(8 + math.cos(a) * rr - 0.3, 8 + math.sin(a) * rr - 0.3, 8 + math.cos(a) * rr + 0.3, 8 + math.sin(a) * rr + 0.3, (200, 170, 90))
t.circle(8, 8, 5.2, (60, 46, 30))
t.circle(8, 8, 4.7, (244, 238, 220))
for i in range(12):
    a = i * math.pi / 6
    t.rect(8 + math.sin(a) * 4 - 0.25, 8 - math.cos(a) * 4 - 0.25, 8 + math.sin(a) * 4 + 0.25, 8 - math.cos(a) * 4 + 0.25, (40, 30, 24))
t.save("wall_clock_face")

# ============================================================ lights
for lit in (True, False):
    sfx = "on" if lit else "off"
    t = Tex(16, (250, 248, 236) if lit else (150, 150, 146))
    for x in (4, 8, 12):
        t.rect(x - 0.3, 0, x + 0.3, 16, (220, 220, 210) if lit else (120, 120, 116))
    t.save("ceiling_light_" + sfx)
    t = Tex(16, (30, 30, 30))
    t.circle(8, 8, 6.5, (255, 246, 210) if lit else (90, 88, 80), (60, 60, 60))
    for r in (2.5, 4.5):
        t.circle(8, 8, r, (255, 236, 180) if lit else (110, 108, 100))
    t.save("studio_lens_" + sfx)
    t = Tex(16, (250, 236, 200) if lit else (170, 160, 140))
    for y in range(0, 16, 3):
        t.rect(0, y, 16, y + 1, (240, 214, 160) if lit else (150, 140, 120))
    t.save("googie_shade_" + sfx)
    t = Tex(32, (40, 16, 14))
    t.rect(0.6, 0.6, 15.4, 5.4, (200, 30, 24) if lit else (70, 24, 20))
    t.frame(0, 0, 16, 6, (180, 186, 194))
    img = t.img
    dd = ImageDraw.Draw(img)
    # pixel lettering "ON AIR" (3x5 font at 2px per unit)
    glyphs = {"O": ["111", "101", "101", "101", "111"], "N": ["101", "111", "111", "111", "101"], "A": ["010", "101", "111", "101", "101"],
              "I": ["111", "010", "010", "010", "111"], "R": ["110", "101", "110", "101", "101"], " ": ["000"] * 5}
    x = 4
    for ch in "ON AIR":
        g = glyphs[ch]
        for gy, row in enumerate(g):
            for gx, bit in enumerate(row):
                if bit == "1":
                    img.putpixel((x + gx, 2 + gy), (255, 240, 220, 255) if lit else (120, 70, 60, 255))
        x += 4
    # place the 6-unit-tall sign art at v 5..11 of a 16-unit face (sign spans y 5..11)
    full = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    full.paste(img.crop((0, 0, 32, 12)), (0, 10))
    full.save(f"{A}/textures/block/on_air_{sfx}.png")
    t = Tex(16, (230, 40, 30) if lit else (90, 30, 26))
    t.circle(8, 6, 3, (255, 160, 140) if lit else (120, 50, 44))
    t.save("beacon_" + sfx)

# ============================================================ tube bank (animated glow)
frames = []
for f in range(4):
    t = metal(STEEL_DK, 16, 4)
    t.rect(1, 1, 15, 15, (24, 22, 22))
    for row in (2, 9):
        for i, u in enumerate((2, 6, 10)):
            glow = 0.75 + 0.25 * math.sin(f * 1.6 + i * 2.1 + row)
            t.rect(u + 0.5, row, u + 3.5, row + 5.5, (60, 60, 64))
            t.rect(u + 1, row + 0.5, u + 3, row + 5, shade((255, 150, 60), glow * 0.65))
            t.rect(u + 1.5, row + 2.5, u + 2.5, row + 4, shade((255, 210, 120), glow))
    frames.append(t.img)
strip = Image.new("RGBA", (16, 64))
for i, fr in enumerate(frames):
    strip.paste(fr, (0, 16 * i))
strip.save(f"{A}/textures/block/tube_bank_front.png")
json.dump({"animation": {"frametime": 4, "interpolate": True}}, open(f"{A}/textures/block/tube_bank_front.png.mcmeta", "w"))
t = metal(STEEL_DK, 16, 4)
for v in range(2, 15, 3):
    t.rect(3, v, 13, v + 1, (40, 44, 44))
t.save("vent_panel")

# ============================================================ camera, control panel, control desk
t = metal((90, 96, 104), 16, 4)
t.rect(3, 5, 13, 9, (150, 40, 34))
t.rect(4, 6, 12, 8, (235, 225, 200))
t.save("camera_body")
t = Tex(16, (20, 20, 22))
t.circle(8, 8, 6.5, (40, 40, 44), (120, 120, 126))
t.circle(8, 8, 4, (60, 80, 110))
t.circle(6.5, 6.5, 1.2, (170, 190, 220))
t.save("camera_lens")
t = metal((112, 122, 120), 32, 4)
t.frame(0, 0, 16, 16, (70, 76, 76))
for (u, v) in [(2, 2), (6, 2), (10, 2)]:
    t.rect(u, v, u + 3.5, v + 2.5, (236, 228, 204)).frame(u, v, u + 3.5, v + 2.5, (40, 40, 40))
    t.rect(u + 1.6, v + 0.5, u + 1.9, v + 2.2, (40, 20, 20))
for i in range(6):
    t.circle(2.2 + i * 2.4, 7, 0.8, (58, 40, 30), (180, 186, 194))
for r in range(3):
    for c in range(12):
        t.circle(1.8 + c * 1.12, 10 + r * 1.5, 0.35, (20, 20, 20))
for i, c in enumerate([(255, 60, 40), (255, 190, 40), (60, 230, 90), (240, 240, 240)]):
    t.rect(2 + i * 3, 14, 3.4 + i * 3, 15, c)
t.save("control_panel_front")
t = metal((112, 122, 120), 32, 4)
for r in range(2):
    for c in range(10):
        col = random.choice([(236, 232, 220), (200, 50, 40), (60, 120, 200), (240, 200, 60)])
        t.rect(1 + c * 1.45, 0.6 + r * 1.2, 2 + c * 1.45, 1.4 + r * 1.2, col)
t.save("control_desk_panel")

# ============================================================ items
def item(name, draw):
    t = Tex(16, (0, 0, 0, 0))
    draw(t)
    t.img.save(f"{A}/textures/item/{name}.png")

def tape_icon(t, recorded):
    t.rect(1, 2, 15, 14, (30, 60, 110) if not recorded else (130, 40, 34))
    t.frame(1, 2, 15, 14, (10, 20, 40) if not recorded else (60, 16, 14))
    t.circle(8, 8, 5, (90, 56, 34))
    t.circle(8, 8, 5, (90, 56, 34), (200, 206, 214))
    t.circle(8, 8, 1.4, (220, 220, 220))
    if recorded:
        t.rect(3, 11.5, 13, 13.5, (240, 236, 220))
        t.rect(4, 12.2, 11, 12.7, (40, 40, 40))
item("magnetic_tape", lambda t: tape_icon(t, False))
item("magnetic_tape_recorded", lambda t: tape_icon(t, True))
def doc_icon(t):
    t.rect(3, 1, 13, 15, (244, 240, 226)).frame(3, 1, 13, 15, (150, 140, 120))
    for v in range(3, 14, 2):
        t.rect(4.5, v, 4.5 + random.uniform(5, 7.5), v + 0.8, (90, 90, 96))
    t.rect(9, 11, 12, 14, (200, 50, 40))
item("document", doc_icon)
def journal_icon(t):
    t.rect(1, 2, 15, 15, (210, 180, 110)).frame(1, 2, 15, 15, (120, 90, 50))
    t.rect(1, 1, 7, 3, (210, 180, 110))
    t.rect(3, 6, 13, 7, (90, 70, 40))
    t.rect(3, 9, 11, 10, (90, 70, 40))
    t.rect(10, 11, 14, 14, (200, 40, 30))
item("case_journal", journal_icon)
def meter_icon(t):
    t.rect(2, 3, 14, 15, (52, 34, 24)).frame(2, 3, 14, 15, (20, 14, 10))
    t.rect(3, 4, 13, 9, (236, 228, 204))
    t.rect(7.5, 5, 8.5, 8.5, (200, 40, 30))
    t.circle(5, 12, 1.3, (180, 186, 194))
    t.circle(11, 12, 1.3, (180, 186, 194))
    t.rect(11, 0, 12, 3, (160, 166, 176))
item("field_meter", meter_icon)

# ============================================================ models
ALL = ["north", "south", "east", "west", "up", "down"]


def el(f, to, tex, faces=None, rot=None, uv=None, shade_=True):
    """faces: dict side -> texture var (or (var, uv)); default: all sides use tex."""
    fs = {}
    sides = faces if faces is not None else {s: tex for s in ALL}
    for s, v in sides.items():
        if v is None:
            continue
        if isinstance(v, tuple):
            fs[s] = {"texture": "#" + v[0], "uv": v[1]}
        else:
            fs[s] = {"texture": "#" + v}
        if uv and s in uv:
            fs[s]["uv"] = uv[s]
    e = {"from": f, "to": to, "faces": fs}
    if rot:
        e["rotation"] = rot
    if not shade_:
        e["shade"] = False
    return e


def model(name, textures, elements, cutout=False, parent="block/block", ao=True):
    m = {"parent": parent, "textures": textures, "elements": elements}
    if cutout:
        m["render_type"] = "minecraft:cutout"
    if not ao:
        m["ambientocclusion"] = False
    json.dump(m, open(f"{A}/models/block/{name}.json", "w"), indent=1)


def tx(**kw):
    return {k: ("lastbroadcast:block/" + v if ":" not in v else v) for k, v in kw.items()}


def sides(front, side, top=None, bottom=None, back=None):
    return {"north": front, "south": back or side, "east": side, "west": side, "up": top or side, "down": bottom or side}


# television
model("television", tx(particle="walnut", front="tv_front", wood="walnut", back="tv_back", screen="tv_screen", black="black", brass="brass", knob="bakelite", chrome="chrome"), [
    el([1, 3, 3], [15, 15, 14], None, sides("front", "wood", back="back")),
    el([4, 5.5, 2.5], [14, 13, 3], None, {"north": "screen", "east": "black", "west": "black", "up": "black", "down": "black"}),
    el([1.75, 10, 2.25], [3.25, 11.5, 3], "knob"),
    el([1.75, 7.5, 2.25], [3.25, 9, 3], "knob"),
    el([2, 0, 4], [3, 3, 5], "brass"), el([13, 0, 4], [14, 3, 5], "brass"), el([2, 0, 12], [3, 3, 13], "brass"), el([13, 0, 12], [14, 3, 13], "brass"),
    el([6.5, 15, 7], [9.5, 16, 10], "knob"),
    el([7.75, 16, 8.25], [8.25, 23, 8.75], "chrome", rot={"origin": [8, 16, 8.5], "axis": "z", "angle": 22.5}),
    el([7.75, 16, 8.25], [8.25, 23, 8.75], "chrome", rot={"origin": [8, 16, 8.5], "axis": "z", "angle": -22.5}),
])
model("signal_analyzer", tx(particle="steel", front="analyzer_front", steel="steel", vent="vent_panel", chrome="chrome"), [
    el([1, 0, 2], [15, 11, 14], None, sides("front", "steel", back="vent")),
    el([0.5, 10, 4], [1, 11, 12], "chrome"), el([15, 10, 4], [15.5, 11, 12], "chrome"),
])
model("tape_recorder", tx(particle="cream_metal", front="recorder_front", body="cream_metal", dark="steel_dark"), [
    el([1, 0, 4], [15, 15, 12], None, sides("front", "body", back="dark", bottom="dark")),
])
model("case_board", tx(particle="walnut", front="case_board_front", wood="walnut"), [
    el([0, 1, 14], [16, 15, 16], None, sides("front", "wood")),
])
model("file_cabinet", tx(particle="file_cabinet_front", front="file_cabinet_front", side="steel"), [
    el([1, 0, 1], [15, 16, 15], None, sides("front", "side")),
])
model("tape_shelf", tx(particle="walnut", front="tape_shelf_front", wood="walnut"), [
    el([0, 0, 2], [16, 16, 16], None, sides("front", "wood")),
])
model("notice_board", tx(particle="walnut", front="notice_front", wood="walnut"), [
    el([1, 2, 15], [15, 14, 16], None, sides("front", "wood")),
])
model("engineering_locker", tx(particle="locker_front", front="locker_front", side="steel"), [
    el([1, 0, 1], [15, 16, 15], None, sides("front", "side")),
])
model("skyhook_console", tx(particle="steel", low="console_low_front", desk="console_desk", high="console_high_front", steel="steel", dark="steel_dark"), [
    el([0, 0, 2], [16, 8, 16], None, {"north": "low", "south": "dark", "east": "steel", "west": "steel", "up": "desk", "down": "dark"}),
    el([0, 8, 7], [16, 14, 16], None, {"north": "high", "south": "dark", "east": "steel", "west": "steel", "up": "steel", "down": "dark"}),
])
model("telephone", tx(particle="black", body="black", dial="telephone_dial", chrome="chrome"), [
    el([4.5, 0, 4], [11.5, 3.5, 11], None, {"north": "body", "south": "body", "east": "body", "west": "body", "up": "dial", "down": "body"}),
    el([4, 3.5, 6.5], [12, 4.75, 8.5], "body"),
    el([4, 3.25, 6], [5.5, 5.25, 9], "body"),
    el([10.5, 3.25, 6], [12, 5.25, 9], "body"),
    el([7.5, 3.5, 10], [8.5, 4, 11], "chrome"),
])
model("wall_clock", tx(particle="chrome", face="wall_clock_face", chrome="chrome"), [
    el([0, 0, 15], [16, 16, 16], None, {"north": "face", "south": ("face", [16, 0, 0, 16])}),
    el([3, 3, 15.5], [13, 13, 16], "chrome", {"south": "chrome", "east": "chrome", "west": "chrome", "up": "chrome", "down": "chrome"}),
], cutout=True)
for lit in (True, False):
    s = "on" if lit else "off"
    model("ceiling_light_" + s, tx(particle="chrome", panel="ceiling_light_" + s, chrome="chrome"), [
        el([2, 14, 2], [14, 16, 14], None, {"north": "chrome", "south": "chrome", "east": "chrome", "west": "chrome", "up": "chrome", "down": "panel"}),
    ])
    model("studio_light_" + s, tx(particle="black", lens="studio_lens_" + s, body="black", chrome="chrome"), [
        el([5, 0, 5], [11, 1, 11], "body"),
        el([7.5, 1, 7.5], [8.5, 9, 8.5], "chrome"),
        el([7, 8.5, 6.5], [9, 9.5, 10.5], "chrome"),
        el([4.5, 9, 5], [11.5, 15, 12], None, {"north": "lens", "south": "body", "east": "body", "west": "body", "up": "body", "down": "body"}),
        el([4, 14.5, 4], [12, 15, 5], "body"),
        el([4, 9, 4], [12, 9.5, 5], "body"),
    ])
    model("googie_lamp_" + s, tx(particle="brass", shade="googie_shade_" + s, brass="brass", black="black"), [
        el([5, 0, 5], [11, 1, 11], "black"),
        el([7.5, 1, 7.5], [8.5, 11, 8.5], "brass"),
        el([5, 11, 5], [11, 16, 11], "shade"),
        el([6, 10.5, 6], [10, 11, 10], "brass"),
    ])
    model("on_air_sign_" + s, tx(particle="chrome", sign="on_air_" + s, chrome="chrome"), [
        el([1, 5, 14], [15, 11, 16], None, {"north": "sign", "south": "chrome", "east": "chrome", "west": "chrome", "up": "chrome", "down": "chrome"}),
    ])
    model("tower_beacon_" + s, tx(particle="beacon_" + s, glass="beacon_" + s, base="steel_dark"), [
        el([5, 0, 5], [11, 2, 11], "base"),
        el([5.5, 2, 5.5], [10.5, 7, 10.5], "glass"),
    ])
model("tube_bank", tx(particle="vent_panel", front="tube_bank_front", side="vent_panel"), [
    el([1, 0, 1], [15, 16, 15], None, sides("front", "side")),
])
model("transmitter_coil", tx(particle="coil", coil="coil", porcelain="porcelain"), [
    el([5, 2, 5], [11, 14, 11], "coil"),
    el([4, 2, 6], [12, 14, 10], "coil"),
    el([6, 2, 4], [10, 14, 12], "coil"),
    el([3, 0, 3], [13, 2, 13], "porcelain"),
    el([3, 14, 3], [13, 16, 13], "porcelain"),
])
model("studio_camera", tx(particle="camera_body", body="camera_body", lens="camera_lens", black="black", chrome="chrome"), [
    el([7, 0, 3], [9, 1, 13], "black"), el([3, 0, 7], [13, 1, 9], "black"),
    el([7, 1, 7], [9, 8, 9], "chrome"),
    el([4, 8, 4], [12, 14, 13], None, {"north": "black", "south": "black", "east": "body", "west": "body", "up": "black", "down": "black"}),
    el([5.5, 9.5, 2.5], [10.5, 12.5, 4], None, {"north": "lens", "south": "black", "east": "black", "west": "black", "up": "black", "down": "black"}),
    el([5, 14, 9], [11, 16, 13], "black"),
    el([3, 10, 12], [4, 11, 14], "chrome"), el([12, 10, 12], [13, 11, 14], "chrome"),
])
model("control_panel", tx(particle="steel", front="control_panel_front", steel="steel"), [
    el([0, 0, 0], [16, 16, 16], None, sides("front", "steel")),
])
model("control_desk", tx(particle="steel", panel="control_desk_panel", steel="steel", wood="walnut", dark="steel_dark"), [
    el([0, 0, 1], [16, 9, 16], None, {"north": "steel", "south": "dark", "east": "steel", "west": "steel", "up": "wood", "down": "dark"}),
    el([0, 9, 8], [16, 11, 16], None, {"north": "panel", "south": "dark", "east": "steel", "west": "steel", "up": "steel", "down": "dark"}),
])
model("lounge_chair", tx(particle="chair_fabric", fabric="chair_fabric", leg="black"), [
    el([2, 5, 2], [14, 8, 14], "fabric"),
    el([2, 8, 12], [14, 16, 14], "fabric"),
    el([2.5, 0, 2.5], [3.5, 5, 3.5], "leg"), el([12.5, 0, 2.5], [13.5, 5, 3.5], "leg"),
    el([2.5, 0, 12.5], [3.5, 5, 13.5], "leg"), el([12.5, 0, 12.5], [13.5, 5, 13.5], "leg"),
])
for cube in ["terrazzo", "studio_tile", "atomic_wallpaper", "chrome_trim", "walnut_panel", "googie_facade"]:
    json.dump({"parent": "block/cube_all", "textures": {"all": "lastbroadcast:block/" + cube}}, open(f"{A}/models/block/{cube}.json", "w"), indent=1)

# ============================================================ blockstates
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
FACING = ["television", "signal_analyzer", "tape_recorder", "case_board", "file_cabinet", "tape_shelf", "notice_board", "engineering_locker",
          "skyhook_console", "wall_clock", "tube_bank", "transmitter_coil", "studio_camera", "control_panel", "control_desk", "lounge_chair"]
for b in FACING:
    variants = {f"facing={f}": ({"model": f"lastbroadcast:block/{b}", "y": r} if r else {"model": f"lastbroadcast:block/{b}"}) for f, r in ROT.items()}
    json.dump({"variants": variants}, open(f"{A}/blockstates/{b}.json", "w"), indent=1)
variants = {}
for f, r in ROT.items():
    for ring in ("true", "false"):
        v = {"model": "lastbroadcast:block/telephone"}
        if r:
            v["y"] = r
        variants[f"facing={f},ringing={ring}"] = v
json.dump({"variants": variants}, open(f"{A}/blockstates/telephone.json", "w"), indent=1)
for b in ["studio_light", "on_air_sign"]:
    variants = {}
    for f, r in ROT.items():
        for lit in ("true", "false"):
            v = {"model": f"lastbroadcast:block/{b}_{'on' if lit == 'true' else 'off'}"}
            if r:
                v["y"] = r
            variants[f"facing={f},lit={lit}"] = v
    json.dump({"variants": variants}, open(f"{A}/blockstates/{b}.json", "w"), indent=1)
for b in ["ceiling_light", "googie_lamp", "tower_beacon"]:
    json.dump({"variants": {"lit=true": {"model": f"lastbroadcast:block/{b}_on"}, "lit=false": {"model": f"lastbroadcast:block/{b}_off"}}},
              open(f"{A}/blockstates/{b}.json", "w"), indent=1)
for cube in ["terrazzo", "studio_tile", "atomic_wallpaper", "chrome_trim", "walnut_panel", "googie_facade"]:
    json.dump({"variants": {"": {"model": f"lastbroadcast:block/{cube}"}}}, open(f"{A}/blockstates/{cube}.json", "w"), indent=1)

# ============================================================ item models
BLOCKS = FACING + ["telephone", "ceiling_light", "studio_light", "googie_lamp", "on_air_sign", "tower_beacon",
                   "terrazzo", "studio_tile", "atomic_wallpaper", "chrome_trim", "walnut_panel", "googie_facade"]
LIT = {"ceiling_light", "studio_light", "googie_lamp", "on_air_sign", "tower_beacon"}
for b in BLOCKS:
    parent = f"lastbroadcast:block/{b}_on" if b in LIT else f"lastbroadcast:block/{b}"
    json.dump({"parent": parent}, open(f"{A}/models/item/{b}.json", "w"), indent=1)
json.dump({"parent": "item/generated", "textures": {"layer0": "lastbroadcast:item/magnetic_tape"},
           "overrides": [{"predicate": {"lastbroadcast:recorded": 1}, "model": "lastbroadcast:item/magnetic_tape_recorded"}]},
          open(f"{A}/models/item/magnetic_tape.json", "w"), indent=1)
for it in ["magnetic_tape_recorded", "document", "case_journal", "field_meter"]:
    json.dump({"parent": "item/generated", "textures": {"layer0": f"lastbroadcast:item/{it}"}}, open(f"{A}/models/item/{it}.json", "w"), indent=1)

# ============================================================ loot tables
for b in BLOCKS:
    json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"lastbroadcast:{b}"}],
                                                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
              open(f"{D}/loot_table/blocks/{b}.json", "w"), indent=1)

# ============================================================ language
names = {
    "television": "Console Television", "signal_analyzer": "Video Signal Analyzer", "tape_recorder": "Reel-to-Reel Tape Deck",
    "case_board": "Evidence Board", "file_cabinet": "Filing Cabinet", "tape_shelf": "Tape Library Shelf", "notice_board": "Notice Board",
    "engineering_locker": "Engineering Locker", "skyhook_console": "Skyhook Overdrive Console", "telephone": "Desk Telephone",
    "wall_clock": "Atomic Wall Clock", "ceiling_light": "Fluorescent Ceiling Panel", "studio_light": "Studio Fresnel Lamp",
    "googie_lamp": "Sputnik Floor Lamp", "on_air_sign": "ON AIR Sign", "tower_beacon": "Tower Beacon", "tube_bank": "Vacuum Tube Bank",
    "transmitter_coil": "Transmitter Coil", "studio_camera": "Studio Camera", "control_panel": "Control Panel", "control_desk": "Master Control Desk",
    "lounge_chair": "Lounge Chair", "terrazzo": "Terrazzo", "studio_tile": "Acoustic Studio Tile", "atomic_wallpaper": "Atomic Wallpaper",
    "chrome_trim": "Chrome Trim", "walnut_panel": "Walnut Paneling", "googie_facade": "Googie Facade",
}
lang = {f"block.lastbroadcast.{k}": v for k, v in names.items()}
lang.update({
    "item.lastbroadcast.magnetic_tape": "Reel of Magnetic Tape",
    "item.lastbroadcast.magnetic_tape.recorded": "Recorded Tape",
    "item.lastbroadcast.document": "Document",
    "item.lastbroadcast.case_journal": "FCC Case File",
    "item.lastbroadcast.field_meter": "Field-Strength Meter",
    "entity.lastbroadcast.staff": "WKVR Staff",
    "itemGroup.lastbroadcast": "The Last Broadcast",
})
json.dump(lang, open(f"{A}/lang/en_us.json", "w"), indent=1)


# ============================================================ skins (64x64 player layout)
def skin(name, skin_c, hair_c, shirt, trousers, shoes, jacket=None, extras=()):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(u, v, w, h, dd, col, top=None, front=None):
        # standard box unwrap at texOffs(u,v)
        d.rectangle([u + dd, v, u + dd + w - 1, v + dd - 1], fill=top or col)          # top
        d.rectangle([u + dd + w, v, u + dd + 2 * w - 1, v + dd - 1], fill=col)          # bottom
        d.rectangle([u, v + dd, u + 2 * dd + 2 * w - 1, v + dd + h - 1], fill=col)      # sides
        if front:
            d.rectangle([u + dd, v + dd, u + dd + w - 1, v + dd + h - 1], fill=front)

    def noise_region(x0, y0, x1, y1, amt=6):
        for y in range(y0, y1):
            for x in range(x0, x1):
                p = img.getpixel((x, y))
                if p[3]:
                    n = random.randint(-amt, amt)
                    img.putpixel((x, y), (clamp(p[0] + n), clamp(p[1] + n), clamp(p[2] + n), 255))

    # head
    box(0, 0, 8, 8, 8, skin_c)
    d.rectangle([8, 0, 15, 7], fill=hair_c)            # top of head
    d.rectangle([0, 8, 7, 10], fill=hair_c)            # right side hair
    d.rectangle([16, 8, 23, 10], fill=hair_c)          # left side hair
    d.rectangle([24, 8, 31, 13], fill=hair_c)          # back hair
    d.rectangle([8, 8, 15, 9], fill=hair_c)            # fringe on face rows 0-1
    # face (front 8..15, 8..15): row 3 eyes, row 5 mouth
    for ex in (9, 10, 13, 14):
        img.putpixel((ex, 11), (250, 250, 250, 255))
    img.putpixel((10, 11), (50, 40, 30, 255))
    img.putpixel((13, 11), (50, 40, 30, 255))
    for ex in (9, 10, 13, 14):
        img.putpixel((ex, 10), shade(hair_c, 0.7) + (255,))
    img.putpixel((11, 12), shade(skin_c, 0.85) + (255,))
    img.putpixel((12, 12), shade(skin_c, 0.85) + (255,))
    for mx in (10, 11, 12, 13):
        img.putpixel((mx, 13), (176, 90, 80, 255))
    # mouth interior (model part at texOffs 0,0: north 0..3, south 4..7 on row 0) and eyelids (24..35 row 0)
    for x in range(0, 8):
        img.putpixel((x, 0), (80, 20, 24, 255))
    for x in range(24, 36):
        img.putpixel((x, 0), skin_c + (255,))
    # body
    box(16, 16, 8, 12, 4, shirt)
    box(40, 16, 4, 12, 4, shirt)   # right arm
    box(32, 48, 4, 12, 4, shirt)   # left arm
    for (x0, y0) in [(40, 20), (32, 52)]:
        d.rectangle([x0, y0 + 10, x0 + 15, y0 + 11], fill=skin_c)   # hands
    box(0, 16, 4, 12, 4, trousers)
    box(16, 48, 4, 12, 4, trousers)
    for (x0, y0) in [(0, 20), (16, 52)]:
        d.rectangle([x0, y0 + 10, x0 + 15, y0 + 11], fill=shoes)
    if jacket:
        box(16, 32, 8, 12, 4, jacket)
        box(40, 32, 4, 12, 4, jacket)
        box(48, 48, 4, 12, 4, jacket)
        d.rectangle([20, 36, 27, 47], fill=(0, 0, 0, 0))   # open front of the coat shows the shirt
        d.rectangle([20, 36, 21, 47], fill=jacket)
        d.rectangle([26, 36, 27, 47], fill=jacket)
        for (x0, y0) in [(40, 36), (48, 52)]:
            d.rectangle([x0, y0 + 9, x0 + 15, y0 + 11], fill=(0, 0, 0, 0))
    for e in extras:
        if e == "tie":
            d.rectangle([23, 20, 24, 27], fill=(150, 36, 40))
        elif e == "bowtie":
            d.rectangle([22, 20, 25, 21], fill=(160, 30, 40))
        elif e == "pearls":
            for x in range(21, 27):
                img.putpixel((x, 21), (248, 246, 236, 255))
        elif e == "glasses":
            for (x, y) in [(9, 11), (10, 11), (13, 11), (14, 11)]:
                pass
            d.rectangle([8, 10, 15, 10], fill=(40, 40, 40))
            img.putpixel((11, 11), (40, 40, 40, 255))
            img.putpixel((12, 11), (40, 40, 40, 255))
        elif e == "bob":
            # hair volume on the hat layer (texOffs 32,0): sides and back, front face left clear
            d.rectangle([40, 0, 47, 7], fill=hair_c)
            d.rectangle([32, 8, 39, 14], fill=hair_c)
            d.rectangle([48, 8, 55, 14], fill=hair_c)
            d.rectangle([56, 8, 63, 15], fill=hair_c)
            d.rectangle([40, 8, 47, 9], fill=hair_c)
        elif e == "bun":
            d.rectangle([40, 0, 47, 7], fill=hair_c)
            d.rectangle([58, 9, 61, 12], fill=shade(hair_c, 0.8))
        elif e == "skirt":
            for (x0, y0) in [(0, 20), (16, 52)]:
                d.rectangle([x0, y0, x0 + 15, y0 + 5], fill=trousers)
                d.rectangle([x0, y0 + 6, x0 + 15, y0 + 9], fill=skin_c)
        elif e == "mustache":
            for mx in (10, 11, 12, 13):
                img.putpixel((mx, 12), shade(hair_c, 0.9) + (255,))
    noise_region(16, 16, 56, 32, 5)
    noise_region(0, 16, 16, 32, 4)
    noise_region(16, 32, 56, 48, 4)
    img.save(f"{A}/textures/entity/staff/{name}.png")


skin("rosa", (214, 166, 126), (40, 28, 24), (70, 120, 90), (60, 60, 70), (30, 24, 20), extras=["bun", "skirt"])
skin("dot", (240, 204, 172), (236, 206, 120), (236, 120, 150), (236, 120, 150), (180, 40, 60), extras=["bob", "pearls", "skirt"])
skin("penrose", (232, 192, 160), (200, 200, 204), (236, 236, 230), (110, 84, 60), (40, 30, 24), jacket=(244, 244, 238), extras=["bowtie", "glasses"])
skin("brandt", (226, 184, 150), (96, 70, 50), (90, 94, 104), (80, 84, 94), (24, 20, 18), extras=["tie", "mustache"])
print("block assets ok:", len(BLOCKS), "blocks")
