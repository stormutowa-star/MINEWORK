#!/usr/bin/env python3
"""Paints all television artwork (sets, title cards, presenters) and packs the broadcast atlas.
Everything is drawn procedurally; run from the project root: python3 tools/gen_broadcast_art.py"""
import json, math, os, random
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageOps, ImageChops

random.seed(1962)
OUT_DIR = "src/main/resources/assets/lastbroadcast/textures/broadcast"
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 640, 480          # working resolution (4x scene units)
OW, OH = 320, 240        # stored resolution (2x scene units)
FONT_DIR = "/usr/share/fonts/truetype/"


def font(name, size):
    paths = {"sans": "dejavu/DejaVuSans-Bold.ttf", "serif": "dejavu/DejaVuSerif-Bold.ttf",
             "script": "freefont/FreeSansBoldOblique.ttf", "mono": "dejavu/DejaVuSansMono-Bold.ttf",
             "plain": "dejavu/DejaVuSans.ttf"}
    return ImageFont.truetype(FONT_DIR + paths[name], size)


def S(v):
    return int(round(v * 4))


def new(bg=(0, 0, 0)):
    return Image.new("RGBA", (W, H), bg + (255,))


def vgrad(img, box, top, bottom):
    d = ImageDraw.Draw(img)
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        k = (y - y0) / max(1, (y1 - y0 - 1))
        c = tuple(int(top[i] + (bottom[i] - top[i]) * k) for i in range(3))
        d.line([(x0, y), (x1 - 1, y)], fill=c + (255,))


def grain(img, amount=10):
    px = img.load()
    for y in range(0, img.height):
        for x in range(0, img.width):
            r, g, b, a = px[x, y]
            n = random.randint(-amount, amount)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)), a)


def finish(img):
    small = img.resize((OW, OH), Image.LANCZOS)
    grain(small, 3)
    return small


LANG = os.environ.get("LB_LANG", "en")
# Spanish lettering for the painted artwork (LB_LANG=es writes atlas_es.png; layout is identical to atlas.png)
ES = [("HALLOWAY TODAY", "HALLOWAY HOY"), ("WKVR NEWS", "NOTICIAS WKVR"), ("FUTURA PUSH-BUTTON", "FUTURA DE BOTONES"),
      ("HOMES OF TOMORROW", "HOGARES DEL MAÑANA"), ("LUCKY DIAL", "DIAL DE LA SUERTE"), ("COOLANT FLOW", "REFRIGERANTE"),
      ("PLATE TEMP", "TEMP. PLACA"), ("VALVE ", "VÁLV. "), ("PLEASE", "POR FAVOR"), ("STAND BY", "ESPERE"),
      ("OHIO R.", "R. OHIO"), ("CHANNEL", "CANAL"), ("REPORT", "INFORME")]


def tr(s):
    if LANG != "es":
        return s
    for a, b in ES:
        s = s.replace(a, b)
    return s


def text_center(d, xy, s, f, fill, outline=None, ow=0):
    x, y = xy
    s = tr(s)
    bbox = d.textbbox((0, 0), s, font=f)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    if outline:
        d.text((x - tw / 2 - bbox[0], y - th / 2 - bbox[1]), s, font=f, fill=fill, stroke_width=ow, stroke_fill=outline)
    else:
        d.text((x - tw / 2 - bbox[0], y - th / 2 - bbox[1]), s, font=f, fill=fill)


def starburst(d, cx, cy, r_out, r_in, points, fill, outline=None, rot=0.0):
    pts = []
    for i in range(points * 2):
        r = r_out if i % 2 == 0 else r_in
        a = rot + math.pi * i / points
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    d.polygon(pts, fill=fill, outline=outline)


def boomerang(d, cx, cy, size, angle, fill):
    pts = []
    for i in range(40):
        t = i / 39
        a = angle + (t - 0.5) * 2.2
        rr = size * (0.55 + 0.45 * math.sin(t * math.pi))
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr * 0.55))
    for i in range(40):
        t = 1 - i / 39
        a = angle + (t - 0.5) * 2.2
        rr = size * (0.55 + 0.45 * math.sin(t * math.pi)) - size * 0.22 * math.sin(t * math.pi)
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr * 0.55))
    d.polygon(pts, fill=fill)


def atom(d, cx, cy, r, col, width=4):
    for k in range(3):
        a = k * math.pi / 3
        pts = []
        for i in range(60):
            t = 2 * math.pi * i / 59
            x, y = math.cos(t) * r, math.sin(t) * r * 0.35
            pts.append((cx + x * math.cos(a) - y * math.sin(a), cy + x * math.sin(a) + y * math.cos(a)))
        d.line(pts, fill=col, width=width)
    d.ellipse([cx - r * 0.14, cy - r * 0.14, cx + r * 0.14, cy + r * 0.14], fill=col)


# ============================================================ backgrounds

def bg_black():
    return finish(new((4, 4, 6)))


def bg_testcard(call="WKVR", ch="7", ktrx=False):
    img = new((128, 128, 128))
    d = ImageDraw.Draw(img)
    if ktrx:
        for y in range(0, H, 40):
            for x in range(0, W, 40):
                if (x // 40 + y // 40) % 2 == 0:
                    d.rectangle([x, y, x + 39, y + 39], fill=(90, 90, 90))
    else:
        vgrad(img, (0, 0, W, H), (150, 150, 150), (110, 110, 110))
    # wedges
    cx, cy = W // 2, H // 2
    for i in range(36):
        a0, a1 = i * 10, i * 10 + 5
        d.pieslice([cx - 230, cy - 230, cx + 230, cy + 230], a0, a1, fill=(40, 40, 40) if i % 2 == 0 else (220, 220, 220))
    d.ellipse([cx - 200, cy - 200, cx + 200, cy + 200], fill=(140, 140, 140), outline=(20, 20, 20), width=6)
    for r in (170, 130, 90):
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(30, 30, 30), width=4)
    # resolution bars
    for i in range(12):
        x = cx - 170 + i * 6
        d.line([(x, cy - 30), (x, cy + 30)], fill=(10, 10, 10), width=2)
        x = cx + 100 + i * 6
        d.line([(x, cy - 30), (x, cy + 30)], fill=(10, 10, 10), width=2)
    # grey steps
    for i in range(6):
        g = int(i * 255 / 5)
        d.rectangle([cx - 150 + i * 50, cy + 110, cx - 100 + i * 50, cy + 150], fill=(g, g, g))
    d.line([(cx, 0), (cx, H)], fill=(20, 20, 20), width=3)
    d.line([(0, cy), (W, cy)], fill=(20, 20, 20), width=3)
    # emblem
    if ktrx:
        d.rectangle([cx - 90, cy - 70, cx + 90, cy + 70], fill=(200, 200, 200), outline=(20, 20, 20), width=6)
        text_center(d, (cx, cy - 20), call, font("serif", 54), (20, 20, 20))
        text_center(d, (cx, cy + 36), "CHANNEL " + ch, font("sans", 26), (20, 20, 20))
    else:
        starburst(d, cx, cy, 110, 52, 8, (235, 235, 235), (20, 20, 20))
        d.ellipse([cx - 58, cy - 58, cx + 58, cy + 58], fill=(30, 30, 30))
        text_center(d, (cx, cy - 4), ch, font("serif", 84), (240, 240, 240))
        d.rectangle([cx - 70, cy - 176, cx + 70, cy - 124], fill=(230, 230, 230), outline=(20, 20, 20), width=3)
        text_center(d, (cx, cy - 150), call, font("sans", 40), (20, 20, 20))
        d.rectangle([cx - 150, cy + 170, cx + 150, cy + 204], fill=(230, 230, 230), outline=(20, 20, 20), width=3)
        text_center(d, (cx, cy + 187), "HALLOWAY FALLS, OHIO", font("sans", 22), (20, 20, 20))
    return finish(img)


def bg_ident():
    img = new()
    vgrad(img, (0, 0, W, H), (12, 26, 58), (24, 60, 92))
    d = ImageDraw.Draw(img)
    for i in range(80):
        x, y = random.randint(0, W), random.randint(0, H)
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(220, 230, 255))
    boomerang(d, 140, 120, 160, 0.4, (232, 120, 52))
    boomerang(d, 520, 380, 150, 3.4, (60, 190, 190))
    cx, cy = W // 2, 200
    atom(d, cx, cy, 200, (120, 220, 220), 3)
    starburst(d, cx, cy, 190, 40, 12, (248, 228, 170), rot=0.13)
    starburst(d, cx, cy, 120, 30, 8, (232, 120, 52), rot=0.0)
    d.ellipse([cx - 80, cy - 80, cx + 80, cy + 80], fill=(18, 40, 78), outline=(248, 228, 170), width=8)
    text_center(d, (cx, cy + 4), "7", font("serif", 130), (248, 236, 200))
    return finish(img)


def chair(d, x, y, col, dark):
    # mid-century lounge chair seen from the front (anchor: seat line at y)
    d.rounded_rectangle([x - 70, y - 120, x + 70, y + 10], 30, fill=col, outline=dark, width=4)
    d.rounded_rectangle([x - 80, y - 10, x + 80, y + 40], 18, fill=col, outline=dark, width=4)
    for lx in (x - 60, x + 60):
        d.line([(lx, y + 40), (lx + (8 if lx > x else -8), y + 90)], fill=(40, 30, 20), width=6)


def studio_set(emergency=False):
    img = new()
    if emergency:
        vgrad(img, (0, 0, W, 360), (40, 14, 12), (70, 24, 18))
    else:
        vgrad(img, (0, 0, W, 360), (64, 168, 170), (40, 128, 134))
    d = ImageDraw.Draw(img)
    # window flat with sunrise
    wx0, wy0, wx1, wy1 = 210, 40, 430, 200
    if emergency:
        d.rectangle([wx0, wy0, wx1, wy1], fill=(30, 12, 10))
    else:
        vgrad(img, (wx0, wy0, wx1, wy1), (250, 200, 120), (250, 150, 90))
        d.ellipse([290, 120, 350, 180], fill=(255, 236, 160))
        d.polygon([(wx0, 200), (260, 150), (320, 185), (380, 140), (wx1, 175), (wx1, 200)], fill=(90, 120, 80))
    d.rectangle([wx0, wy0, wx1, wy1], outline=(235, 235, 225), width=10)
    d.line([(320, wy0), (320, wy1)], fill=(235, 235, 225), width=8)
    d.line([(wx0, 120), (wx1, 120)], fill=(235, 235, 225), width=6)
    accent = (130, 50, 40) if emergency else (238, 128, 56)
    boomerang(d, 90, 80, 90, 0.2, accent)
    boomerang(d, 560, 90, 80, 2.8, accent)
    starburst(d, 120, 230, 40, 10, 8, (90, 40, 30) if emergency else (250, 230, 170))
    starburst(d, 530, 230, 36, 9, 8, (90, 40, 30) if emergency else (250, 230, 170))
    if not emergency:
        text_center(d, (320, 236), "HALLOWAY TODAY", font("script", 34), (250, 240, 210), (30, 70, 80), 3)
    # floor
    vgrad(img, (0, 360, W, H), (70, 60, 56) if not emergency else (30, 16, 14), (40, 34, 32) if not emergency else (14, 8, 8))
    d.line([(0, 360), (W, 360)], fill=(20, 20, 20), width=4)
    seat = (226, 112, 48) if not emergency else (110, 40, 30)
    dark = (90, 40, 20) if not emergency else (40, 14, 10)
    chair(d, S(50) if not emergency else S(72), 392, seat, dark)
    if not emergency:
        chair(d, S(114), 392, seat, dark)
        # coffee table between
        d.ellipse([260, 400, 380, 430], fill=(110, 70, 40), outline=(50, 30, 20), width=3)
        d.line([(280, 425), (270, 470)], fill=(40, 30, 20), width=5)
        d.line([(360, 425), (370, 470)], fill=(40, 30, 20), width=5)
        d.rectangle([306, 388, 334, 404], fill=(240, 240, 230))
    else:
        # fallen lamp, smoke, emergency light
        d.line([(470, 440), (600, 330)], fill=(60, 50, 40), width=8)
        d.ellipse([570, 300, 630, 350], fill=(80, 60, 40))
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow)
        gd.ellipse([-200, -120, 260, 320], fill=(255, 90, 40, 90))
        glow = glow.filter(ImageFilter.GaussianBlur(60))
        img.alpha_composite(glow)
        smoke = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        sd = ImageDraw.Draw(smoke)
        for i in range(30):
            x, y, r = random.randint(0, W), random.randint(0, 260), random.randint(40, 110)
            sd.ellipse([x - r, y - r * 0.6, x + r, y + r * 0.6], fill=(60, 54, 52, 70))
        smoke = smoke.filter(ImageFilter.GaussianBlur(24))
        img.alpha_composite(smoke)
    return finish(img)


def bg_newsdesk():
    img = new()
    vgrad(img, (0, 0, W, 368), (60, 78, 112), (36, 50, 80))
    d = ImageDraw.Draw(img)
    # globe map
    cx, cy, r = 320, 160, 140
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(70, 100, 140), outline=(200, 210, 230), width=4)
    for lat in (-60, -30, 0, 30, 60):
        yy = cy + math.sin(math.radians(lat)) * r
        ww = math.cos(math.radians(lat)) * r
        d.line([(cx - ww, yy), (cx + ww, yy)], fill=(150, 170, 200), width=2)
    for k in range(-2, 3):
        d.ellipse([cx - abs(k) * 30 - 6, cy - r, cx + abs(k) * 30 + 6, cy + r], outline=(150, 170, 200), width=2)
    random.seed(7)
    for i in range(14):
        x, y = cx + random.randint(-110, 90), cy + random.randint(-100, 90)
        d.ellipse([x, y, x + random.randint(20, 60), y + random.randint(14, 40)], fill=(150, 170, 110))
    random.seed(1962)
    atom(d, 90, 90, 60, (230, 200, 120), 3)
    atom(d, 550, 90, 60, (230, 200, 120), 3)
    text_center(d, (90, 200), "CHANNEL", font("sans", 22), (230, 220, 190))
    text_center(d, (550, 200), "REPORT", font("sans", 22), (230, 220, 190))
    text_center(d, (90, 236), "7", font("serif", 60), (240, 200, 110))
    text_center(d, (550, 236), "7", font("serif", 60), (240, 200, 110))
    # desk
    d.rectangle([0, 368, W, H], fill=(96, 62, 38))
    d.rectangle([0, 368, W, 380], fill=(210, 214, 220))
    d.rectangle([60, 392, 580, 470], fill=(80, 50, 30), outline=(40, 26, 16), width=4)
    d.rectangle([230, 400, 410, 450], fill=(20, 40, 70))
    text_center(d, (320, 425), "WKVR NEWS", font("sans", 26), (240, 220, 150))
    return finish(img)


def bg_weather():
    img = new()
    vgrad(img, (0, 0, W, H), (54, 92, 120), (34, 62, 90))
    d = ImageDraw.Draw(img)
    land = [(90, 60), (520, 50), (560, 160), (540, 300), (470, 400), (330, 420), (200, 390), (110, 300), (70, 170)]
    d.polygon(land, fill=(150, 170, 120), outline=(240, 240, 220))
    d.line([(100, 380), (220, 330), (300, 360), (420, 300), (560, 330)], fill=(80, 130, 200), width=8)
    text_center(d, (440, 330), "OHIO R.", font("plain", 16), (40, 70, 140))
    starburst(d, 300, 220, 16, 6, 5, (230, 60, 40))
    text_center(d, (300, 252), "HALLOWAY FALLS", font("sans", 18), (40, 30, 30))
    d.ellipse([140, 110, 200, 170], outline=(30, 60, 160), width=6)
    text_center(d, (170, 140), "H", font("sans", 36), (30, 60, 160))
    d.ellipse([450, 120, 510, 180], outline=(200, 40, 40), width=6)
    text_center(d, (480, 150), "L", font("sans", 36), (200, 40, 40))
    pts = [(380, 40), (360, 120), (330, 200), (300, 300), (280, 400)]
    d.line(pts, fill=(40, 70, 200), width=6)
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2
        d.polygon([(mx - 12, my - 6), (mx - 12, my + 10), (mx - 30, my + 2)], fill=(40, 70, 200))
    for (x, y, t) in [(200, 220, "34"), (420, 230, "38"), (330, 360, "36"), (480, 80, "31")]:
        text_center(d, (x, y), t + "°", font("sans", 26), (250, 250, 240), (30, 30, 30), 2)
    return finish(img)


def bg_kitchen():
    img = new()
    vgrad(img, (0, 0, W, 330), (246, 236, 214), (236, 222, 196))
    d = ImageDraw.Draw(img)
    # window
    d.rounded_rectangle([60, 50, 230, 190], 20, fill=(150, 210, 240), outline=(250, 250, 250), width=10)
    d.line([(145, 50), (145, 190)], fill=(250, 250, 250), width=6)
    # cabinets
    for x in range(250, 600, 90):
        d.rounded_rectangle([x, 40, x + 82, 150], 12, fill=(120, 206, 196), outline=(70, 140, 136), width=4)
        d.ellipse([x + 60, 90, x + 70, 100], fill=(230, 230, 236))
    # fridge
    d.rounded_rectangle([20, 200, 150, 450], 30, fill=(250, 246, 236), outline=(160, 160, 160), width=5)
    d.line([(20, 300), (150, 300)], fill=(170, 170, 170), width=4)
    d.rectangle([125, 220, 135, 280], fill=(200, 200, 210))
    # counter and range
    d.rectangle([150, 260, W, 300], fill=(240, 120, 110))
    d.rectangle([150, 300, W, 450], fill=(120, 206, 196), outline=(70, 140, 136), width=4)
    d.rounded_rectangle([250, 200, 420, 262], 10, fill=(230, 232, 236), outline=(140, 140, 150), width=4)
    for i in range(6):
        cx = 272 + i * 26
        d.ellipse([cx - 9, 222, cx + 9, 240], fill=[(240, 80, 70), (250, 200, 60), (90, 190, 120)][i % 3], outline=(60, 60, 60), width=2)
    text_center(d, (335, 252), "FUTURA PUSH-BUTTON", font("sans", 11), (60, 60, 60))
    # floor
    for y in range(450, H, 15):
        for x in range(0, W, 30):
            c = (30, 30, 34) if ((x // 30) + (y // 15)) % 2 == 0 else (236, 236, 230)
            d.rectangle([x, y, x + 29, y + 14], fill=c)
    starburst(d, 520, 210, 30, 8, 8, (238, 128, 56))
    return finish(img)


def bg_futura():
    img = new()
    vgrad(img, (0, 0, W, H), (120, 18, 30), (50, 8, 20))
    d = ImageDraw.Draw(img)
    for i in range(120):
        x, y = random.randint(0, W), random.randint(0, H)
        s = random.choice((1, 1, 2))
        d.ellipse([x - s, y - s, x + s, y + s], fill=(255, 230, 220))
    atom(d, 320, 170, 150, (250, 220, 150), 5)
    text_center(d, (320, 300), "FUTURA", font("script", 92), (250, 240, 220), (90, 10, 20), 4)
    text_center(d, (320, 368), "HOMES OF TOMORROW", font("sans", 24), (250, 200, 150))
    return finish(img)


def bg_contest():
    img = new()
    vgrad(img, (0, 0, W, H), (90, 20, 50), (40, 10, 30))
    d = ImageDraw.Draw(img)
    # curtains
    for x in range(0, W, 40):
        d.rectangle([x, 0, x + 20, 380], fill=(140, 30, 60))
    # spotlights
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.polygon([(120, 0), (40, 420), (260, 420)], fill=(255, 240, 200, 60))
    gd.polygon([(520, 0), (380, 420), (620, 420)], fill=(255, 240, 200, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(20))
    img.alpha_composite(glow)
    d = ImageDraw.Draw(img)
    # giant dial
    cx, cy, r = S(110), S(46), 130
    d.ellipse([cx - r - 14, cy - r - 14, cx + r + 14, cy + r + 14], fill=(240, 200, 80))
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(30, 30, 34))
    for i in range(10):
        a = math.radians(-60 + i * 28)
        hx, hy = cx + math.cos(a) * r * 0.72, cy + math.sin(a) * r * 0.72
        d.ellipse([hx - 22, hy - 22, hx + 22, hy + 22], fill=(250, 246, 236))
        text_center(d, (hx, hy), str((i + 1) % 10), font("sans", 22), (30, 30, 30))
    d.ellipse([cx - 40, cy - 40, cx + 40, cy + 40], fill=(240, 200, 80))
    text_center(d, (cx, cy), "$", font("serif", 44), (90, 20, 50))
    # stage
    d.rectangle([0, 380, W, H], fill=(60, 30, 40))
    d.rectangle([0, 380, W, 390], fill=(240, 200, 80))
    for x in range(20, W, 50):
        d.ellipse([x - 6, 400, x + 6, 412], fill=(255, 230, 150))
    text_center(d, (S(46), 440), "LUCKY DIAL", font("script", 30), (255, 230, 150), (90, 20, 50), 2)
    return finish(img)


def bg_tower(color=True):
    img = new()
    vgrad(img, (0, 0, W, H), (8, 10, 28), (30, 32, 70))
    d = ImageDraw.Draw(img)
    for i in range(160):
        x, y = random.randint(0, W), random.randint(0, 330)
        d.point((x, y), fill=(230, 230, 255))
    d.ellipse([500, 40, 560, 100], fill=(240, 236, 210))
    d.ellipse([515, 34, 575, 94], fill=(14, 18, 40))
    # hills
    d.polygon([(0, 380), (120, 340), (260, 370), (400, 330), (560, 360), (W, 340), (W, H), (0, H)], fill=(14, 20, 24))
    # tower lattice
    base_y, top_y = 430, 30
    cx = 320
    for side in (-1, 1):
        d.line([(cx + side * 70, base_y), (cx + side * 6, top_y + 40)], fill=(150, 150, 160), width=5)
    y = base_y
    w = 70
    while y > top_y + 50:
        y2 = y - 34
        w2 = 6 + (w - 6) * (y2 - top_y - 40) / (base_y - top_y - 40)
        d.line([(cx - w, y), (cx + w2, y2)], fill=(120, 120, 130), width=2)
        d.line([(cx + w, y), (cx - w2, y2)], fill=(120, 120, 130), width=2)
        d.line([(cx - w2, y2), (cx + w2, y2)], fill=(130, 130, 140), width=2)
        y, w = y2, w2
    d.line([(cx, top_y + 40), (cx, top_y - 10)], fill=(170, 170, 180), width=4)
    for (bx, by, br) in [(cx, top_y - 12, 9), (cx - 30, 250, 6), (cx + 30, 250, 6)]:
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(glow).ellipse([bx - br * 4, by - br * 4, bx + br * 4, by + br * 4], fill=(255, 40, 30, 120))
        img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(14)))
        d = ImageDraw.Draw(img)
        d.ellipse([bx - br, by - br, bx + br, by + br], fill=(255, 60, 40))
    # station building with lit windows
    d.rectangle([60, 400, 300, 470], fill=(210, 210, 200))
    d.polygon([(40, 400), (320, 400), (300, 386), (60, 386)], fill=(180, 180, 176))
    for x in range(80, 290, 30):
        d.rectangle([x, 420, x + 18, 440], fill=(255, 220, 140))
    text_center(d, (180, 456), "WKVR", font("sans", 18), (120, 40, 30))
    return finish(img)


def bg_insert_console():
    img = new()
    vgrad(img, (0, 0, W, H), (90, 100, 92), (60, 66, 60))
    d = ImageDraw.Draw(img)
    # clock
    d.ellipse([500, 20, 620, 140], fill=(240, 236, 220), outline=(40, 40, 40), width=6)
    for i in range(12):
        a = math.radians(i * 30)
        d.line([(560 + math.sin(a) * 48, 80 - math.cos(a) * 48), (560 + math.sin(a) * 56, 80 - math.cos(a) * 56)], fill=(30, 30, 30), width=4)
    hour = (9 + 14 / 60) / 12 * 2 * math.pi
    minute = 14 / 60 * 2 * math.pi
    d.line([(560, 80), (560 + math.sin(hour) * 30, 80 - math.cos(hour) * 30)], fill=(20, 20, 20), width=8)
    d.line([(560, 80), (560 + math.sin(minute) * 46, 80 - math.cos(minute) * 46)], fill=(20, 20, 20), width=5)
    # gauges
    for gx, label, val in [(120, "COOLANT FLOW", 0.0), (330, "PLATE TEMP", 1.0)]:
        d.pieslice([gx - 90, 40, gx + 90, 220], 180, 360, fill=(240, 236, 220), outline=(30, 30, 30), width=5)
        d.pieslice([gx - 90, 40, gx + 90, 220], 320, 360, fill=(220, 70, 60))
        a = math.radians(-90 + (-60 + 120 * val))
        d.line([(gx, 130), (gx + math.cos(a) * 80, 130 + math.sin(a) * 80)], fill=(20, 20, 20), width=6)
        text_center(d, (gx, 150), label, font("sans", 18), (240, 236, 220))
    # valve levers
    for i in range(6):
        x = 50 + i * 95
        d.rectangle([x, 230, x + 70, 420], fill=(40, 40, 40), outline=(20, 20, 20), width=4)
        open_ = (i == 2)
        ly = 250 if open_ else 360
        d.rectangle([x + 12, ly, x + 58, ly + 46], fill=(210, 50, 40), outline=(90, 20, 10), width=4)
        lamp = (80, 255, 120) if open_ else (30, 60, 40)
        d.ellipse([x + 24, 428, x + 46, 450], fill=lamp)
        text_center(d, (x + 35, 465), "VALVE " + str(i + 1), font("sans", 15), (240, 236, 220))
    d.rectangle([228, 222, 334, 458], outline=(255, 240, 120), width=5)
    # heat and smoke
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glow).rectangle([0, 0, W, H], fill=(255, 80, 30, 50))
    img.alpha_composite(glow)
    smoke = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(smoke)
    for i in range(18):
        x, y, r = random.randint(0, W), random.randint(0, 200), random.randint(30, 80)
        sd.ellipse([x - r, y - r * 0.6, x + r, y + r * 0.6], fill=(50, 46, 44, 80))
    img.alpha_composite(smoke.filter(ImageFilter.GaussianBlur(18)))
    return finish(img)


def bg_standby():
    img = new()
    vgrad(img, (0, 0, W, H), (30, 40, 70), (20, 26, 50))
    d = ImageDraw.Draw(img)
    boomerang(d, 120, 380, 140, 0.4, (232, 120, 52))
    boomerang(d, 520, 100, 140, 3.4, (60, 190, 190))
    d.rounded_rectangle([140, 90, 500, 390], 40, fill=(248, 236, 210), outline=(232, 120, 52), width=10)
    # little TV with a wrench
    d.rounded_rectangle([260, 120, 380, 210], 16, fill=(110, 70, 40), outline=(40, 26, 16), width=4)
    d.rounded_rectangle([272, 132, 352, 198], 10, fill=(150, 170, 160))
    d.line([(300, 150), (330, 180)], fill=(60, 60, 60), width=10)
    d.ellipse([290, 140, 310, 160], outline=(60, 60, 60), width=6)
    text_center(d, (320, 260), "PLEASE", font("serif", 52), (40, 30, 30))
    text_center(d, (320, 320), "STAND BY", font("serif", 52), (200, 50, 40))
    text_center(d, (320, 368), "WKVR  CHANNEL 7", font("sans", 18), (40, 30, 30))
    return finish(img)


def bg_signoff():
    img = new()
    vgrad(img, (0, 0, W, H), (6, 8, 24), (24, 26, 60))
    d = ImageDraw.Draw(img)
    for i in range(200):
        x, y = random.randint(0, W), random.randint(0, 300)
        d.point((x, y), fill=(230, 230, 255))
    d.ellipse([80, 50, 150, 120], fill=(244, 240, 220))
    # skyline
    x = 0
    while x < W:
        w = random.randint(40, 90)
        h = random.randint(40, 120)
        d.rectangle([x, H - 90 - h, x + w, H], fill=(16, 18, 30))
        for wy in range(H - 80 - h, H - 30, 20):
            for wx in range(x + 8, x + w - 8, 16):
                if random.random() < 0.35:
                    d.rectangle([wx, wy, wx + 6, wy + 8], fill=(255, 220, 140))
        x += w + random.randint(2, 10)
    # Skyhook
    d.line([(470, H - 90), (500, 60)], fill=(120, 120, 130), width=4)
    d.line([(530, H - 90), (500, 60)], fill=(120, 120, 130), width=4)
    for yy in range(80, H - 90, 30):
        k = (yy - 60) / (H - 150)
        d.line([(500 - 30 * k, yy), (500 + 30 * k, yy)], fill=(110, 110, 120), width=2)
    d.ellipse([494, 48, 506, 60], fill=(255, 60, 40))
    return finish(img)


# ============================================================ presenters

SK = (238, 196, 160)
OUTLINE = (52, 32, 24)


def person(frame, skin, hair, hair_style, clothes, clothes_style, extras=(), lips=None, mono=False):
    """Waist-up figure on a transparent 240x320 canvas (stored 120x160)."""
    img = Image.new("RGBA", (240, 320), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = 120
    # torso / clothes
    d.rounded_rectangle([20, 200, 220, 360], 48, fill=clothes, outline=OUTLINE, width=4)
    # neck
    d.rectangle([102, 168, 138, 212], fill=skin, outline=OUTLINE, width=3)
    if clothes_style == "suit":
        d.polygon([(100, 205), (120, 250), (140, 205), (120, 215)], fill=(245, 245, 240), outline=OUTLINE)
        d.polygon([(70, 206), (112, 206), (120, 300), (90, 300)], fill=tuple(int(c * 0.82) for c in clothes), outline=OUTLINE)
        d.polygon([(170, 206), (128, 206), (120, 300), (150, 300)], fill=tuple(int(c * 0.82) for c in clothes), outline=OUTLINE)
    elif clothes_style == "dress":
        d.pieslice([90, 180, 150, 240], 0, 180, fill=skin, outline=OUTLINE, width=3)
    elif clothes_style == "labcoat":
        d.polygon([(100, 205), (120, 240), (140, 205)], fill=(245, 245, 240), outline=OUTLINE)
        d.polygon([(64, 206), (110, 206), (118, 320), (70, 320)], fill=(244, 244, 236), outline=OUTLINE)
        d.polygon([(176, 206), (130, 206), (122, 320), (170, 320)], fill=(244, 244, 236), outline=OUTLINE)
        d.rectangle([140, 250, 166, 262], fill=(200, 200, 196))
    if hair_style == "bob":
        d.ellipse([cx - 68, 30, cx + 68, 176], fill=hair, outline=OUTLINE, width=4)
        for side in (-1, 1):
            d.ellipse([cx + side * 62 - 22, 150, cx + side * 62 + 22, 186], fill=hair, outline=OUTLINE, width=4)
    # head
    d.ellipse([cx - 52, 40, cx + 52, 176], fill=skin, outline=OUTLINE, width=4)
    d.ellipse([cx - 62, 96, cx - 44, 128], fill=skin, outline=OUTLINE, width=3)
    d.ellipse([cx + 44, 96, cx + 62, 128], fill=skin, outline=OUTLINE, width=3)
    # hair
    if hair_style == "bob":
        # side-swept fringe with a part on the left
        d.polygon([(cx - 56, 96), (cx - 50, 50), (cx - 10, 32), (cx + 40, 40), (cx + 58, 80), (cx + 54, 100),
                   (cx + 20, 70), (cx - 20, 64), (cx - 40, 84)], fill=hair, outline=OUTLINE)
        d.line([(cx - 30, 44), (cx + 30, 62)], fill=tuple(min(255, c + 30) for c in hair), width=5)
    elif hair_style == "slick":
        d.chord([cx - 54, 30, cx + 54, 120], 180, 360, fill=hair, outline=OUTLINE, width=4)
        d.line([(cx - 20, 44), (cx + 30, 60)], fill=tuple(min(255, c + 40) for c in hair), width=4)
    elif hair_style == "grey":
        d.chord([cx - 54, 34, cx + 54, 110], 180, 360, fill=hair, outline=OUTLINE, width=4)
        d.rounded_rectangle([cx - 56, 70, cx - 44, 112], 6, fill=hair)
        d.rounded_rectangle([cx + 44, 70, cx + 56, 112], 6, fill=hair)
    # eyes
    ey = 112
    for side in (-1, 1):
        ex = cx + side * 22
        if frame == 2:
            d.arc([ex - 11, ey - 6, ex + 11, ey + 8], 10, 170, fill=OUTLINE, width=3)
        else:
            d.ellipse([ex - 11, ey - 8, ex + 11, ey + 8], fill=(255, 255, 255), outline=OUTLINE, width=2)
            d.ellipse([ex - 5, ey - 6, ex + 5, ey + 6], fill=(60, 50, 40))
            d.ellipse([ex - 2, ey - 4, ex + 1, ey - 1], fill=(255, 255, 255))
        d.arc([ex - 14, ey - 24, ex + 14, ey - 8], 200, 340, fill=tuple(int(c * 0.6) for c in hair), width=4)
    # nose
    d.line([(cx + 2, 118), (cx - 3, 136), (cx + 4, 138)], fill=tuple(int(c * 0.75) for c in skin), width=3)
    # mouth
    lipc = lips or (170, 80, 70)
    if frame == 1:
        d.ellipse([cx - 14, 146, cx + 14, 164], fill=(90, 20, 24), outline=lipc, width=4)
        d.rectangle([cx - 8, 147, cx + 8, 151], fill=(250, 250, 250))
    else:
        d.arc([cx - 16, 138, cx + 16, 156], 20, 160, fill=lipc, width=5)
    # cheeks
    if lips:
        for side in (-1, 1):
            d.ellipse([cx + side * 32 - 9, 132, cx + side * 32 + 9, 142], fill=(240, 160, 150))
    for e in extras:
        if e == "pearls":
            for i in range(9):
                a = math.radians(200 + i * 17.5)
                px, py = cx + math.cos(a) * 34 * -1, 214 - math.sin(a) * 18 * -1
                d.ellipse([px - 5, py - 5, px + 5, py + 5], fill=(250, 248, 240), outline=(150, 140, 130))
        elif e == "glasses":
            for side in (-1, 1):
                ex = cx + side * 22
                d.ellipse([ex - 16, ey - 13, ex + 16, ey + 13], outline=(30, 30, 30), width=4)
            d.line([(cx - 6, ey - 2), (cx + 6, ey - 2)], fill=(30, 30, 30), width=3)
        elif e == "bowtie":
            d.polygon([(cx, 208), (cx - 22, 196), (cx - 22, 222)], fill=(160, 30, 40), outline=OUTLINE)
            d.polygon([(cx, 208), (cx + 22, 196), (cx + 22, 222)], fill=(160, 30, 40), outline=OUTLINE)
        elif e == "tie":
            d.polygon([(cx - 8, 212), (cx + 8, 212), (cx + 12, 280), (cx, 296), (cx - 12, 280)], fill=(150, 40, 40), outline=OUTLINE)
        elif e == "mustache":
            d.chord([cx - 20, 132, cx + 20, 148], 180, 360, fill=(90, 60, 40))
    if mono:
        img = mono_of(img)
    return img.resize((120, 160), Image.LANCZOS)


def robot(frame, mono=False):
    img = Image.new("RGBA", (240, 320), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = 120
    chrome = (196, 204, 214)
    d.rounded_rectangle([30, 196, 210, 360], 40, fill=chrome, outline=(60, 64, 70), width=4)
    for i, c in enumerate([(240, 80, 70), (250, 200, 60), (90, 190, 120)]):
        d.ellipse([70 + i * 40, 240, 96 + i * 40, 266], fill=c, outline=(60, 60, 60), width=3)
    d.rectangle([60, 284, 180, 300], fill=(150, 156, 166))
    d.rectangle([104, 176, 136, 200], fill=(150, 156, 166), outline=(60, 64, 70), width=3)
    d.chord([cx - 66, 50, cx + 66, 220], 180, 360, fill=chrome, outline=(60, 64, 70), width=4)
    d.rectangle([cx - 66, 134, cx + 66, 182], fill=chrome, outline=(60, 64, 70), width=4)
    d.line([(cx, 50), (cx, 18)], fill=(80, 80, 90), width=5)
    d.ellipse([cx - 10, 6, cx + 10, 26], fill=(255, 80, 60))
    for side in (-1, 1):
        ex = cx + side * 30
        glow = (120, 230, 255) if frame != 2 else (40, 70, 90)
        d.ellipse([ex - 18, 96, ex + 18, 132], fill=(30, 30, 40), outline=(60, 64, 70), width=4)
        d.ellipse([ex - 10, 104, ex + 10, 124], fill=glow)
    for i in range(5):
        lit = frame == 1 and i % 2 == 0
        d.rectangle([cx - 36 + i * 15, 146, cx - 26 + i * 15, 170], fill=(250, 220, 120) if lit else (60, 64, 70))
    if mono:
        img = mono_of(img)
    return img.resize((120, 160), Image.LANCZOS)


def mono_of(img):
    a = img.getchannel("A")
    g = ImageOps.grayscale(img.convert("RGB"))
    g = ImageOps.autocontrast(g, cutoff=1)
    rgb = Image.merge("RGB", (g, g, g.point(lambda v: min(255, int(v * 1.04)))))
    rgb.putalpha(a)
    return rgb


# ============================================================ utility images

def noise_tile(seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (256, 256))
    px = img.load()
    for y in range(256):
        streak = rnd.random() < 0.05
        for x in range(256):
            v = rnd.randint(0, 255)
            if streak:
                v = min(255, v + 80)
            px[x, y] = (v, v, v, 255)
    return img


def scanlines():
    img = Image.new("RGBA", (OW, OH), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(0, OH, 2):
        d.line([(0, y), (OW, y)], fill=(0, 0, 0, 110))
    return img


def vignette():
    img = Image.new("RGBA", (OW, OH), (0, 0, 0, 0))
    px = img.load()
    for y in range(OH):
        for x in range(OW):
            dx, dy = (x - OW / 2) / (OW / 2), (y - OH / 2) / (OH / 2)
            r = math.sqrt(dx * dx * 0.9 + dy * dy)
            a = max(0.0, min(1.0, (r - 0.55) / 0.75))
            px[x, y] = (0, 0, 0, int(a * a * 230))
    return img


def screen_mask():
    img = Image.new("RGBA", (OW, OH), (0, 0, 0, 0))
    m = Image.new("L", (OW * 4, OH * 4), 255)
    ImageDraw.Draw(m).rounded_rectangle([6, 6, OW * 4 - 7, OH * 4 - 7], 70, fill=0)
    m = m.resize((OW, OH), Image.LANCZOS)
    black = Image.new("RGBA", (OW, OH), (6, 6, 6, 255))
    img = Image.composite(black, img, m)
    # glass reflection highlight
    hl = Image.new("RGBA", (OW, OH), (0, 0, 0, 0))
    ImageDraw.Draw(hl).ellipse([20, 8, 150, 60], fill=(255, 255, 255, 22))
    hl = hl.filter(ImageFilter.GaussianBlur(10))
    img.alpha_composite(hl)
    return img


def knob():
    s = 256
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([8, 8, s - 8, s - 8], fill=(190, 196, 204), outline=(70, 74, 80), width=6)
    for i in range(36):
        a = math.radians(i * 10)
        d.line([(s / 2 + math.cos(a) * 104, s / 2 + math.sin(a) * 104), (s / 2 + math.cos(a) * 118, s / 2 + math.sin(a) * 118)], fill=(120, 124, 130), width=4)
    d.ellipse([40, 40, s - 40, s - 40], fill=(58, 40, 30), outline=(20, 14, 10), width=6)
    d.ellipse([70, 60, 140, 110], fill=(96, 72, 56))
    d.rectangle([s / 2 - 8, 44, s / 2 + 8, 120], fill=(240, 230, 200))
    return img.resize((64, 64), Image.LANCZOS)


def meter_face():
    img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, 511, 255], 24, fill=(240, 232, 206), outline=(60, 50, 40), width=8)
    cx, cy, r = 256, 236, 200
    d.arc([cx - r, cy - r, cx + r, cy + r], 220, 320, fill=(40, 30, 20), width=6)
    d.arc([cx - r, cy - r, cx + r, cy + r], 300, 320, fill=(200, 50, 40), width=14)
    for i in range(11):
        a = math.radians(220 + i * 10)
        l = 26 if i % 5 == 0 else 14
        d.line([(cx + math.cos(a) * r, cy + math.sin(a) * r), (cx + math.cos(a) * (r - l), cy + math.sin(a) * (r - l))], fill=(40, 30, 20), width=5)
    return img.resize((128, 64), Image.LANCZOS)


def reel(full):
    s = 256
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = s / 2
    pack = 118 if full else 62
    d.ellipse([c - pack, c - pack, c + pack, c + pack], fill=(86, 50, 30))
    for rr in range(40, int(pack), 10):
        d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=(100, 62, 40), width=2)
    d.ellipse([c - 124, c - 124, c + 124, c + 124], outline=(200, 206, 214), width=8)
    for k in range(3):
        a = math.radians(k * 120)
        pts = [(c + math.cos(a - 0.35) * 40, c + math.sin(a - 0.35) * 40), (c + math.cos(a - 0.25) * 110, c + math.sin(a - 0.25) * 110),
               (c + math.cos(a + 0.25) * 110, c + math.sin(a + 0.25) * 110), (c + math.cos(a + 0.35) * 40, c + math.sin(a + 0.35) * 40)]
        d.polygon(pts, fill=(0, 0, 0, 0))
    hub = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hub)
    hd.ellipse([c - 36, c - 36, c + 36, c + 36], fill=(210, 214, 220), outline=(80, 84, 90), width=4)
    for k in range(3):
        a = math.radians(k * 120 + 90)
        hd.line([(c, c), (c + math.cos(a) * 30, c + math.sin(a) * 30)], fill=(80, 84, 90), width=8)
    hd.ellipse([c - 8, c - 8, c + 8, c + 8], fill=(40, 40, 40))
    img.alpha_composite(hub)
    # window cutouts in the flange (show the spokes)
    for k in range(3):
        a = math.radians(k * 120 + 30)
        x, y = c + math.cos(a) * 84, c + math.sin(a) * 84
        d.ellipse([x - 22, y - 22, x + 22, y + 22], fill=(160, 166, 176, 255) if not full else (110, 70, 44, 255))
    return img.resize((64, 64), Image.LANCZOS)


# ============================================================ pack the atlas

def main():
    regions = {}
    atlas = Image.new("RGBA", (2048, 2048), (0, 0, 0, 0))

    def put(name, img, x, y):
        atlas.paste(img, (x, y))
        regions[name] = [x, y, img.width, img.height]

    bgs = [
        ("black", bg_black(), False),
        ("testcard_wkvr", bg_testcard(), True),
        ("testcard_ktrx", bg_testcard("KTRX", "4", True), True),
        ("ident_wkvr", bg_ident(), True),
        ("studio_today", studio_set(False), True),
        ("studio_emergency", studio_set(True), False),
        ("newsdesk", bg_newsdesk(), True),
        ("weather_map", bg_weather(), True),
        ("kitchen", bg_kitchen(), True),
        ("futura_card", bg_futura(), True),
        ("contest", bg_contest(), False),
        ("tower_night", bg_tower(), True),
        ("insert_console", bg_insert_console(), False),
        ("standby", bg_standby(), False),
        ("signoff_card", bg_signoff(), True),
    ]
    images = []
    for name, img, mono in bgs:
        images.append((name, img))
        if mono:
            images.append((name + "_mono", mono_of(img)))
    col, row = 0, 0
    for name, img in images:
        x, y = 2 + col * (OW + 4), 2 + row * (OH + 4)
        put(name, img, x, y)
        col += 1
        if col == 6:
            col, row = 0, row + 1
    y_sprites = 2 + (row + 1) * (OH + 4)

    sprites = []
    for f in range(3):
        sprites.append((f"dot_{f}", person(f, SK, (236, 206, 120), "bob", (236, 120, 150), "dress", ["pearls"], lips=(200, 40, 60))))
        sprites.append((f"hal_{f}", person(f, (230, 190, 150), (60, 44, 34), "slick", (80, 86, 100), "suit", ["tie"])))
        sprites.append((f"penrose_{f}", person(f, (232, 190, 160), (200, 200, 204), "grey", (120, 90, 60), "labcoat", ["glasses", "bowtie"])))
        sprites.append((f"servo_{f}", robot(f)))
        sprites.append((f"dot_mono_{f}", person(f, SK, (236, 206, 120), "bob", (236, 120, 150), "dress", ["pearls"], lips=(200, 40, 60), mono=True)))
        sprites.append((f"hal_mono_{f}", person(f, (230, 190, 150), (60, 44, 34), "slick", (80, 86, 100), "suit", ["tie"], mono=True)))
        sprites.append((f"servo_mono_{f}", robot(f, True)))
    sx, sy = 2, y_sprites
    for name, img in sprites:
        if sx + img.width > 2046:
            sx, sy = 2, sy + 164
        put(name, img, sx, sy)
        sx += img.width + 4
    y_misc = sy + 164

    for i in range(4):
        put(f"noise_{i}", noise_tile(100 + i), 2 + i * 260, y_misc)
    put("scanlines", scanlines(), 1044, y_misc)
    put("vignette", vignette(), 1368, y_misc)
    put("screen_mask", screen_mask(), 1044, y_misc + 244)
    y_small = y_misc + 262
    put("knob", knob(), 2, y_small)
    put("meter_face", meter_face(), 70, y_small)
    put("reel_full", reel(True), 202, y_small)
    put("reel_empty", reel(False), 270, y_small)
    white = Image.new("RGBA", (4, 4), (255, 255, 255, 255))
    put("white", white, 340, y_small)
    put("missing", Image.new("RGBA", (4, 4), (255, 0, 255, 255)), 350, y_small)
    assert y_small + 70 < 2048 and y_misc + 244 + 240 < 2048, (y_small, y_misc)

    sfx = "_es" if LANG == "es" else ""
    atlas.save(OUT_DIR + "/atlas" + sfx + ".png", optimize=True)
    if LANG != "es":
        with open(OUT_DIR + "/atlas.json", "w") as f:
            json.dump(regions, f, indent=0)
    # previews for review (not shipped)
    prev = "build/art_preview"
    os.makedirs(prev, exist_ok=True)
    atlas.save(prev + "/atlas_preview" + sfx + ".png")
    print("atlas regions:", len(regions), "misc row at", y_misc)


if __name__ == "__main__":
    main()
