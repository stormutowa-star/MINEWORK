#!/usr/bin/env python3
"""Synthesises every sound in the mod (machinery, broadcast music, voices) and writes OGG
Vorbis files plus sounds.json. Requires numpy, scipy and ffmpeg.
Run from the project root: python3 tools/gen_sounds.py"""
import json, os, subprocess, tempfile, wave
import numpy as np
from scipy import signal

SR = 32000
A = "src/main/resources/assets/lastbroadcast"
OUT = A + "/sounds"
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(1962)


def t_(dur):
    return np.arange(int(SR * dur)) / SR


def env(n, a=0.01, r=0.1, sustain=1.0):
    e = np.ones(n) * sustain
    na, nr = max(1, int(a * SR)), max(1, int(r * SR))
    e[:na] = np.linspace(0, sustain, na)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def adsr(n, a, d, s, r):
    out = np.zeros(n)
    na, nd, nr = int(a * SR), int(d * SR), int(r * SR)
    ns = max(0, n - na - nd - nr)
    out[:na] = np.linspace(0, 1, na)
    out[na:na + nd] = np.linspace(1, s, nd)
    out[na + nd:na + nd + ns] = s
    out[na + nd + ns:] = np.linspace(s, 0, n - (na + nd + ns))
    return out


def noise(n):
    return rng.uniform(-1, 1, n)


def bp(x, lo, hi, order=2):
    b, a = signal.butter(order, [lo / (SR / 2), min(0.99, hi / (SR / 2))], btype="band")
    return signal.lfilter(b, a, x)


def lp(x, f, order=2):
    b, a = signal.butter(order, min(0.99, f / (SR / 2)), btype="low")
    return signal.lfilter(b, a, x)


def hp(x, f, order=2):
    b, a = signal.butter(order, f / (SR / 2), btype="high")
    return signal.lfilter(b, a, x)


def sine(f, dur, phase=0):
    return np.sin(2 * np.pi * f * t_(dur) + phase)


def note_freq(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def tone(midi, dur, kind="vibes", vol=1.0):
    f = note_freq(midi)
    t = t_(dur)
    if kind == "vibes":
        x = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t * 8)
        x *= np.exp(-t * 3.2) * (1 + 0.15 * np.sin(2 * np.pi * 5.5 * t))
    elif kind == "brass":
        x = sum(np.sin(2 * np.pi * f * k * t) / k ** 0.9 for k in range(1, 8))
        x *= adsr(len(t), 0.04, 0.1, 0.7, 0.12)
        x = lp(x, 2400)
    elif kind == "organ":
        x = sum(np.sin(2 * np.pi * f * m * t) * w for m, w in [(0.5, 0.5), (1, 1), (2, 0.6), (3, 0.3), (4, 0.25)])
        x *= adsr(len(t), 0.01, 0.05, 0.8, 0.05) * (1 + 0.2 * np.sin(2 * np.pi * 6.5 * t))
    elif kind == "strings":
        x = sum(signal.sawtooth(2 * np.pi * f * d * t) for d in (0.997, 1.0, 1.004))
        x = lp(x, 1800) * adsr(len(t), 0.4, 0.2, 0.8, 0.5)
    elif kind == "bass":
        x = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 2 * t)
        x *= np.exp(-t * 4)
    elif kind == "theremin":
        vib = 1 + 0.012 * np.sin(2 * np.pi * 6 * t)
        x = np.sin(2 * np.pi * np.cumsum(f * vib) / SR) * adsr(len(t), 0.08, 0.1, 0.9, 0.15)
    elif kind == "chime":
        x = (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.76 * t) + 0.3 * np.sin(2 * np.pi * f * 5.4 * t)) * np.exp(-t * 2.5)
    elif kind == "piano":
        x = sum(np.sin(2 * np.pi * f * k * t) * np.exp(-t * (2 + k)) / k for k in range(1, 6))
    else:
        x = np.sin(2 * np.pi * f * t)
    return x * vol


def place(buf, x, at):
    i = int(at * SR)
    end = min(len(buf), i + len(x))
    buf[i:end] += x[:end - i]
    return buf


def seq(dur, events):
    """events: (time, midi, length, kind, vol)"""
    buf = np.zeros(int(SR * dur))
    for (at, m, ln, kind, vol) in events:
        place(buf, tone(m, ln, kind, vol), at)
    return buf


def brush(dur, times, vol=0.2):
    buf = np.zeros(int(SR * dur))
    for at in times:
        n = int(0.12 * SR)
        place(buf, hp(noise(n), 3000) * np.exp(-np.arange(n) / SR * 30) * vol, at)
    return buf


def reverb(x, wet=0.25, decay=0.35):
    out = x.copy()
    for d, g in [(0.031, 0.6), (0.047, 0.5), (0.071, 0.4), (0.113, 0.3), (0.167, 0.2)]:
        k = int(d * SR)
        delayed = np.zeros_like(x)
        delayed[k:] = x[:-k] * g * decay * 2
        out += delayed * wet
    return out


def loopify(x, fade=0.25):
    n = int(fade * SR)
    head, tail = x[:n].copy(), x[-n:].copy()
    ramp = np.linspace(0, 1, n)
    x = x[:-n].copy()
    x[:n] = head * ramp + tail * (1 - ramp)
    return x


def write(name, x, peak=0.85):
    x = np.asarray(x, dtype=np.float64)
    m = np.max(np.abs(x)) or 1.0
    x = x / m * peak
    pcm = (x * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        tmp = f.name
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "3", f"{OUT}/{name}.ogg"], check=True)
    os.unlink(tmp)


def click(dur=0.05, f=2500, vol=1.0):
    n = int(dur * SR)
    return bp(noise(n), f * 0.5, f * 1.5) * np.exp(-np.arange(n) / SR * 120) * vol


def thump(dur=0.15, f=90, vol=1.0):
    t = t_(dur)
    return np.sin(2 * np.pi * f * t * (1 - t * 2)) * np.exp(-t * 25) * vol


# ================================================================ machinery & interface
def gen_machinery():
    # TV on: switch click, rising whine, static swell
    d = 0.8
    x = np.zeros(int(SR * d))
    place(x, click(0.04, 1800) + 0, 0)
    t = t_(0.7)
    whine = np.sin(2 * np.pi * np.cumsum(2000 + 4000 * t / 0.7) / SR) * 0.15 * env(len(t), 0.05, 0.3)
    place(x, whine, 0.05)
    place(x, lp(noise(int(0.6 * SR)), 6000) * env(int(0.6 * SR), 0.2, 0.3) * 0.4, 0.15)
    write("tv_on", x)
    x = np.zeros(int(SR * 0.5))
    place(x, click(0.04, 1600), 0)
    t = t_(0.4)
    place(x, np.sin(2 * np.pi * np.cumsum(5000 - 4500 * t / 0.4) / SR) * 0.3 * np.exp(-t * 6), 0.03)
    write("tv_off", x)
    st = lp(noise(int(SR * 3.2)), 7000) * 0.6
    st += bp(noise(len(st)), 60, 200) * 0.3
    for i in range(40):
        place(st, click(0.01, 4000, 0.6), rng.uniform(0, 3.1))
    write("tv_static", loopify(st), 0.6)
    t = t_(3.25)
    hum = (np.sin(2 * np.pi * 60 * t) + 0.5 * np.sin(2 * np.pi * 120 * t) + 0.2 * np.sin(2 * np.pi * 180 * t)) * 0.5 + np.sin(2 * np.pi * 7867 * t) * 0.02
    write("crt_hum", loopify(hum), 0.5)
    x = click(0.06, 1200) + np.pad(thump(0.1, 120, 0.6), (0, 0))[:int(0.06 * SR)]
    write("channel_click", np.concatenate([x, thump(0.12, 90, 0.5)]))
    write("knob", click(0.035, 3200, 0.8))
    x = np.zeros(int(SR * 0.45))
    place(x, click(0.03, 2000), 0)
    place(x, click(0.05, 900), 0.12)
    place(x, thump(0.12, 140, 0.5), 0.12)
    write("tape_insert", x)
    x = np.zeros(int(SR * 0.6))
    place(x, thump(0.15, 80), 0)
    place(x, click(0.05, 1200), 0.02)
    t = t_(0.4)
    place(x, np.sin(2 * np.pi * np.cumsum(300 + 200 * t) / SR) * 0.15 * env(len(t), 0.02, 0.2), 0.12)
    write("tape_eject", x)
    t = t_(2.25)
    motor = (signal.sawtooth(2 * np.pi * 50 * t) * 0.15 + np.sin(2 * np.pi * 100 * t) * 0.2) + hp(noise(len(t)), 4000) * 0.12
    motor *= 1 + 0.05 * np.sin(2 * np.pi * 3 * t)
    write("tape_run", loopify(lp(motor, 5000)), 0.5)
    x = np.zeros(int(SR * 0.6))
    place(x, click(0.04, 1500), 0)
    place(x, thump(0.1, 110, 0.6), 0)
    place(x, sine(880, 0.25) * env(int(0.25 * SR), 0.01, 0.05) * 0.3, 0.15)
    write("tape_rec", x)
    x = np.zeros(int(SR * 0.4))
    place(x, thump(0.2, 70), 0)
    place(x, click(0.06, 700), 0)
    write("tape_stop", x)
    t = t_(1.2)
    rw = np.sin(2 * np.pi * np.cumsum(200 + 900 * (t / 1.2) ** 0.5) / SR) * 0.3 + hp(noise(len(t)), 2000) * 0.15
    write("tape_rewind", rw * env(len(t), 0.1, 0.3))
    x = np.zeros(int(SR * 0.2))
    place(x, click(0.02, 3000), 0)
    place(x, click(0.02, 2600), 0.045)
    write("relay", x)
    x = np.zeros(int(SR * 0.5))
    place(x, thump(0.2, 60), 0)
    place(x, click(0.08, 900, 1.2), 0)
    place(x, sine(1700, 0.4) * np.exp(-t_(0.4) * 12) * 0.15, 0.01)
    write("breaker", x)
    t = t_(0.55)
    sq = np.sin(2 * np.pi * np.cumsum(600 + 300 * np.sin(t * 9)) / SR) * 0.25 * env(len(t), 0.05, 0.2)
    write("valve", sq + bp(noise(len(t)), 800, 3000) * 0.2 * env(len(t), 0.05, 0.2))
    t = t_(1.3)
    arc = bp(noise(len(t)), 1500, 9000) * (0.4 + 0.6 * (rng.random(len(t)) > 0.97)) + signal.square(2 * np.pi * 120 * t) * 0.2
    arc *= env(len(t), 0.005, 0.4)
    x = np.zeros(int(SR * 1.4))
    place(x, thump(0.3, 50, 1.2), 0)
    place(x, arc, 0)
    write("arc", x)
    t = t_(2.0)
    bell = (np.sin(2 * np.pi * 950 * t) + np.sin(2 * np.pi * 1190 * t)) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 18 * t)))
    write("alarm", bell * env(len(t), 0.01, 0.2) * 0.6)
    t = t_(3.2)
    fd = 120 * np.exp(-t * 0.9) + 20
    pd = (signal.sawtooth(2 * np.pi * np.cumsum(fd) / SR) * 0.4 + np.sin(2 * np.pi * np.cumsum(fd * 2) / SR) * 0.3) * np.exp(-t * 0.7)
    x = np.zeros(len(t))
    place(x, thump(0.3, 50, 1.0), 0)
    write("power_down", lp(pd + x, 3000))
    t = t_(2.0)
    strike = (np.sin(2 * np.pi * 1000 * t) + 0.7 * np.sin(2 * np.pi * 1290 * t) + 0.3 * np.sin(2 * np.pi * 2650 * t))
    gate = (np.sin(2 * np.pi * 20 * t) > 0).astype(float)
    ring = strike * gate * env(len(t), 0.01, 0.1)
    ring[int(1.6 * SR):] *= np.linspace(1, 0, len(ring) - int(1.6 * SR))
    write("phone_ring", reverb(ring, 0.3), 0.75)
    x = np.zeros(int(SR * 0.35))
    place(x, click(0.04, 1100, 1.2), 0)
    place(x, thump(0.1, 160, 0.5), 0.0)
    place(x, click(0.03, 2200, 0.6), 0.15)
    write("phone_pickup", x)
    t = t_(2.6)
    write("dial_tone", (np.sin(2 * np.pi * 350 * t) + np.sin(2 * np.pi * 440 * t)) * env(len(t), 0.02, 0.2), 0.45)
    t = t_(4.2)
    th = sum(np.sin(2 * np.pi * 60 * k * t) / k for k in range(1, 9)) * (1 + 0.25 * np.sin(2 * np.pi * 1.5 * t))
    th += bp(noise(len(t)), 100, 400) * 0.4 + np.sin(2 * np.pi * 3150 * t) * 0.03
    write("transmitter_hum", loopify(lp(th, 2500)), 0.7)
    t = t_(6.25)
    amb = lp(noise(len(t)), 300) * 1.4 + bp(noise(len(t)), 300, 1200) * 0.08 + np.sin(2 * np.pi * 60 * t) * 0.05
    amb *= 1 + 0.15 * np.sin(2 * np.pi * 0.2 * t)
    write("ambience_station", loopify(amb, 0.5), 0.5)
    t = t_(3.0)
    wh = np.zeros(len(t))
    for fr in (500, 900, 1400):
        wh += bp(noise(len(t)), fr * 0.9, fr * 1.1) * (0.5 + 0.5 * np.sin(2 * np.pi * (0.7 + fr / 1000) * t))
    wh *= np.linspace(0, 1, len(t)) ** 2 * env(len(t), 0.01, 0.3)
    write("anomaly_whisper", reverb(wh, 0.5), 0.6)
    t = t_(3.2)
    at = (np.sin(2 * np.pi * 440 * t) + np.sin(2 * np.pi * 443.5 * t) + 0.5 * np.sin(2 * np.pi * 659 * t)) * (t / 3.2) ** 1.5 * env(len(t), 0.01, 0.4)
    write("anomaly_tone", at, 0.6)
    t = t_(0.9)
    bz = (signal.square(2 * np.pi * 120 * t) * 0.3 + bp(noise(len(t)), 2000, 6000) * 0.3) * (rng.random(len(t)) > 0.3)
    write("electric_buzz", lp(bz, 6000) * env(len(t), 0.01, 0.2), 0.6)
    x = seq(1.8, [(0, 72, 1.6, "vibes", 0.6), (0.12, 76, 1.5, "vibes", 0.6), (0.24, 79, 1.4, "vibes", 0.6), (0.36, 84, 1.4, "vibes", 0.7)])
    write("deduction", reverb(x, 0.3), 0.7)
    x = np.zeros(int(SR * 0.7))
    place(x, click(0.03, 3500), 0)
    place(x, tone(96, 0.6, "chime", 0.4), 0.05)
    write("evidence", x, 0.6)
    n = int(0.45 * SR)
    pap = hp(noise(n), 2500) * (np.abs(np.sin(np.arange(n) / SR * 40)) ** 3) * env(n, 0.02, 0.1)
    write("paper", pap, 0.5)
    x = np.zeros(int(SR * 0.5))
    place(x, click(0.04, 1400, 1.2), 0)
    place(x, thump(0.2, 100, 0.8), 0.05)
    place(x, sine(2200, 0.3) * np.exp(-t_(0.3) * 15) * 0.2, 0.05)
    write("locker", x)
    t = t_(0.4)
    write("denied", signal.square(2 * np.pi * 110 * t) * 0.3 * env(len(t), 0.01, 0.05), 0.5)
    write("meter_click", click(0.025, 4200, 0.8), 0.5)
    write("typewriter", np.concatenate([click(0.03, 2500, 1.0), thump(0.05, 200, 0.4)]), 0.6)
    t = t_(0.9)
    write("static_burst", lp(noise(len(t)), 7000) * env(len(t), 0.005, 0.5), 0.8)
    t = t_(3.2)
    ap = np.zeros(len(t))
    for i in range(240):
        at_ = rng.uniform(0, 2.9)
        place(ap, bp(noise(int(0.02 * SR)), 800, 4000) * np.exp(-np.arange(int(0.02 * SR)) / SR * 200) * rng.uniform(0.3, 1.0), at_)
    write("applause", ap * env(len(t), 0.3, 0.8), 0.5)
    t = t_(4.2)
    fire = lp(noise(len(t)), 400) * 0.8
    for i in range(70):
        place(fire, click(0.015, rng.uniform(1500, 5000), rng.uniform(0.3, 1.2)), rng.uniform(0, 4.1))
    write("fire_crackle", fire * env(len(t), 0.3, 0.6), 0.6)
    t = t_(4.0)
    write("test_tone", np.sin(2 * np.pi * 1000 * t) * env(len(t), 0.02, 0.05), 0.5)


# ================================================================ music
def gen_music():
    # WKVR jingle: four bright notes (W-K-V-R) then a chord
    ev = [(0.0, 79, 0.5, "vibes", 0.8), (0.28, 76, 0.5, "vibes", 0.8), (0.56, 72, 0.5, "vibes", 0.8), (0.84, 79, 0.6, "vibes", 0.8),
          (1.2, 60, 2.4, "brass", 0.4), (1.2, 64, 2.4, "brass", 0.35), (1.2, 67, 2.4, "brass", 0.35), (1.2, 72, 2.4, "brass", 0.3),
          (1.2, 84, 2.0, "chime", 0.4), (1.2, 36, 1.5, "bass", 0.6)]
    write("jingle_wkvr", reverb(seq(4.0, ev), 0.3), 0.8)
    ev = [(0.0, 72, 0.5, "theremin", 0.5), (0.45, 77, 0.5, "theremin", 0.5), (0.9, 81, 0.9, "theremin", 0.5),
          (1.9, 84, 1.4, "theremin", 0.5), (0.0, 53, 3.5, "organ", 0.15), (1.9, 60, 1.8, "chime", 0.4), (2.1, 64, 1.6, "chime", 0.35), (2.3, 67, 1.4, "chime", 0.3)]
    write("jingle_futura", reverb(seq(3.8, ev), 0.35), 0.8)
    # morning lounge: walking bass + vibes melody + brushes (10 s, 120 bpm)
    beat = 0.5
    bass = [41, 45, 48, 50, 46, 50, 53, 50, 43, 47, 50, 53, 48, 47, 45, 43, 41, 45, 48, 50]
    ev = [(i * beat, n, 0.45, "bass", 0.7) for i, n in enumerate(bass)]
    mel = [(0, 72, 1), (1, 74, 0.5), (1.5, 76, 1.5), (3, 79, 1), (4, 77, 0.5), (4.5, 76, 0.5), (5, 74, 2), (7, 72, 0.5), (7.5, 74, 0.5),
           (8, 76, 1), (9, 72, 1)]
    ev += [(b * beat * 2 * 0.5 + 0.0, n, l, "vibes", 0.45) for b, n, l in mel]
    ev += [(i * 2.0, c, 1.9, "piano", 0.12) for i, c in enumerate([65, 67, 69, 67, 65])]
    x = seq(10.2, ev) + brush(10.2, [i * beat + 0.25 for i in range(20)], 0.15)
    write("music_morning", reverb(x, 0.2), 0.75)
    # news: teletype rhythm + fanfare
    tt = np.zeros(int(SR * 4.2))
    for i in range(28):
        place(tt, click(0.02, 2800, 0.5 if i % 2 else 0.8), i * 0.125)
    ev = [(0, 60, 0.3, "brass", 0.5), (0.3, 64, 0.3, "brass", 0.5), (0.6, 67, 0.3, "brass", 0.5), (0.9, 72, 1.4, "brass", 0.6),
          (0.9, 64, 1.4, "brass", 0.35), (0.9, 48, 1.4, "bass", 0.6), (2.6, 67, 0.2, "brass", 0.4), (2.8, 72, 1.2, "brass", 0.5)]
    write("music_news", reverb(seq(4.2, ev) + tt, 0.2), 0.8)
    # contest organ (8 s)
    mel = [72, 76, 79, 76, 77, 81, 79, 77, 76, 79, 84, 79, 77, 74, 72, 72]
    ev = [(i * 0.5, n, 0.45, "organ", 0.45) for i, n in enumerate(mel)]
    ev += [(i * 1.0, n, 0.9, "organ", 0.25) for i, n in enumerate([48, 53, 55, 48, 53, 55, 50, 48])]
    x = seq(8.2, ev) + brush(8.2, [i * 0.5 for i in range(16)], 0.12)
    write("music_contest", reverb(x, 0.25), 0.75)
    # sign-off: slow chord progression
    chords = [[60, 64, 67], [57, 60, 64], [53, 57, 60], [55, 59, 62], [60, 64, 67, 72]]
    ev = []
    for i, ch in enumerate(chords):
        for n in ch:
            ev.append((i * 2.0, n, 2.6, "strings", 0.3))
    ev += [(i * 2.0 + 0.2, n, 1.6, "chime", 0.25) for i, n in enumerate([76, 72, 69, 71, 72])]
    write("music_signoff", reverb(seq(10.5, ev), 0.35), 0.75)
    # bulletin drone
    t = t_(8.0)
    dr = (np.sin(2 * np.pi * 55 * t) + 0.6 * np.sin(2 * np.pi * 58.3 * t) + 0.3 * signal.sawtooth(2 * np.pi * 110 * t)) * 0.5
    pulse = (np.sin(2 * np.pi * 1.0 * t) > 0.6).astype(float)
    dr += np.sin(2 * np.pi * 880 * t) * pulse * 0.08
    write("music_bulletin", lp(dr, 1500) * env(len(t), 1.0, 1.0), 0.6)


# ================================================================ voices (syllable timbres)
VOWELS = {"a": (800, 1200, 2500), "e": (500, 1900, 2600), "o": (500, 900, 2400), "u": (350, 800, 2300), "i": (300, 2200, 3000)}


def syllable(f0, vowel, dur=0.12, breath=0.05, robot=False, rev=False, jitter=0.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    glide = f0 * (1 + 0.08 * np.sin(np.pi * t / dur) + jitter * rng.uniform(-1, 1))
    if robot:
        src = signal.square(2 * np.pi * np.cumsum(glide) / SR) * 0.6
    else:
        src = signal.sawtooth(2 * np.pi * np.cumsum(glide) / SR)
    src = src + noise(n) * breath
    out = np.zeros(n)
    for i, f in enumerate(VOWELS[vowel]):
        out += bp(src, f * 0.88, f * 1.12) * [1.0, 0.6, 0.25][i]
    if robot:
        out *= np.sin(2 * np.pi * 70 * t)
    e = adsr(n, 0.012, 0.03, 0.75, 0.04)
    if rev:
        e = e[::-1]
        out = out[::-1]
    return out * e


def gen_voices():
    voices = {"voice_dot": dict(f0=230), "voice_rosa": dict(f0=195), "voice_hal": dict(f0=112), "voice_penrose": dict(f0=132, breath=0.18),
              "voice_brandt": dict(f0=104, breath=0.08), "voice_servo": dict(f0=150, robot=True), "voice_reverse": dict(f0=140, rev=True, breath=0.15)}
    files = {}
    for name, kw in voices.items():
        files[name] = []
        for i, v in enumerate(["a", "e", "o", "u"]):
            fname = f"{name}_{i + 1}"
            write(fname, syllable(vowel=v, dur=0.11 + 0.02 * i, **kw), 0.7)
            files[name].append(fname)
    return files


def main():
    gen_machinery()
    gen_music()
    voice_files = gen_voices()
    streams = {"music_morning", "music_contest", "music_signoff", "music_bulletin", "ambience_station", "transmitter_hum"}
    subtitles = {
        "tv_on": "Television switches on", "tv_off": "Television switches off", "tv_static": "Television snow", "channel_click": "Channel dial clicks",
        "tape_insert": "Tape threaded", "tape_eject": "Tape ejected", "tape_run": "Tape transport runs", "tape_rec": "Deck starts recording",
        "tape_stop": "Tape stops", "tape_rewind": "Tape rewinds", "relay": "Relay clicks", "breaker": "Breaker thrown", "valve": "Valve turns",
        "arc": "Electrical arc", "alarm": "Alarm bell", "power_down": "Transmitter powers down", "phone_ring": "Telephone rings",
        "phone_pickup": "Receiver lifted", "dial_tone": "Dial tone", "transmitter_hum": "Overdrive amplifier hums", "anomaly_whisper": "Something whispers",
        "anomaly_tone": "A pure tone swells", "electric_buzz": "Lights buzz", "deduction": "A connection is made", "evidence": "Evidence logged",
        "locker": "Locker latch", "denied": "Lock refuses", "static_burst": "Burst of static", "fire_crackle": "Fire crackles",
        "jingle_wkvr": "WKVR jingle", "jingle_futura": "Futura jingle", "music_morning": "Morning show music", "music_news": "News fanfare",
        "music_contest": "Game show organ", "music_signoff": "Sign-off music", "music_bulletin": "Tense drone", "test_tone": "Test tone",
        "applause": "Studio applause", "voice_dot": "Dot speaks", "voice_hal": "Newsreader speaks", "voice_penrose": "Penrose speaks",
        "voice_rosa": "Rosa speaks", "voice_brandt": "Brandt speaks", "voice_servo": "Robot speaks", "voice_reverse": "Backwards speech",
    }
    names = ["tv_on", "tv_off", "tv_static", "crt_hum", "channel_click", "knob", "tape_insert", "tape_eject", "tape_run", "tape_rec", "tape_stop",
             "tape_rewind", "relay", "breaker", "valve", "arc", "alarm", "power_down", "phone_ring", "phone_pickup", "dial_tone", "transmitter_hum",
             "ambience_station", "anomaly_whisper", "anomaly_tone", "electric_buzz", "deduction", "evidence", "paper", "locker", "denied",
             "meter_click", "typewriter", "static_burst", "jingle_wkvr", "jingle_futura", "music_morning", "music_news", "music_contest",
             "music_signoff", "music_bulletin", "test_tone", "applause", "fire_crackle"]
    sj = {}
    for n in names:
        e = {"sounds": [{"name": f"lastbroadcast:{n}", "stream": n in streams}]}
        if n in subtitles:
            e["subtitle"] = f"subtitles.lastbroadcast.{n}"
        sj[n] = e
    for vname, files in voice_files.items():
        sj[vname] = {"sounds": [f"lastbroadcast:{f}" for f in files], "subtitle": f"subtitles.lastbroadcast.{vname}"}
    json.dump(sj, open(A + "/sounds.json", "w"), indent=1)
    lang_path = A + "/lang/en_us.json"
    lang = json.load(open(lang_path))
    for k, v in subtitles.items():
        lang[f"subtitles.lastbroadcast.{k}"] = v
    json.dump(lang, open(lang_path, "w"), indent=1)
    print("sounds ok:", len(sj), "events")


if __name__ == "__main__":
    main()
