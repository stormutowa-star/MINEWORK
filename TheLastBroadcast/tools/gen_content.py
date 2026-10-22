#!/usr/bin/env python3
"""Generates story content JSON (broadcast timelines, documents, deductions, dialogue).
Run from the project root: python3 tools/gen_content.py"""
import json, os

OUT = "src/main/resources/assets/lastbroadcast/content"
os.makedirs(OUT + "/broadcasts", exist_ok=True)
os.makedirs(OUT + "/dialogue", exist_ok=True)

FULL = [0, 0, 160, 120]

def shot(t, bg, crop=None, end=None, actors=(), texts=(), fx=None, trans="cut", td=0.5, tint=None):
    s = {"t": t, "bg": bg, "trans": trans, "transDur": td, "actors": list(actors), "texts": list(texts)}
    if crop: s["crop"] = crop
    if end: s["cropEnd"] = end
    if fx: s["fx"] = fx
    if tint: s["tint"] = tint
    return s

def actor(sprite, x, y, h, talk=None, move=None):
    a = {"sprite": sprite, "x": x, "y": y, "h": h}
    if talk: a["talk"] = talk
    if move: a["moveTo"] = move
    return a

def text(s, x=80, y=10, scale=1.0, color="#ffffff", box=None, t0=0, t1=9999, blink=False, center=True, scene=False):
    d = {"s": s, "x": x, "y": y, "scale": scale, "color": color, "t0": t0, "t1": t1, "blink": blink, "center": center, "scene": scene}
    if box: d["box"] = box
    return d

def lower(s, t0=0, t1=9999):
    return text(s, 6, 104, 0.6, "#ffffff", "#c0182c48", t0, t1, center=False)

def line(t, d, who, s, **kw):
    l = {"t": t, "d": d, "who": who, "text": s}
    l.update(kw)
    return l

def cue(t, sid, vol=1.0, pitch=1.0):
    return {"t": t, "id": sid, "vol": vol, "pitch": pitch}

broadcasts = {}

def bc(id, title, duration, shots, lines=(), sounds=(), mono=False, fx=None, etitle=None, enote=None, **kw):
    b = {"id": id, "title": title, "duration": duration, "mono": mono, "shots": shots, "lines": list(lines), "sounds": list(sounds)}
    if fx: b["fx"] = fx
    if etitle: b["evidenceTitle"] = etitle
    if enote: b["evidenceNote"] = enote
    b.update(kw)
    broadcasts[id] = b

DOT_CU = [16, 28, 76, 57]
# ---------------------------------------------------------------- normal channel 7 (black & white)
bc("ident_wkvr", "WKVR Station Identification", 8, [
    shot(0, "ident_wkvr", [24, 18, 112, 84], FULL, texts=[
        text("CHANNEL 7  -  HALLOWAY FALLS", 80, 96, 0.6, t0=1.2),
        text("THE VALLEY'S WINDOW ON TOMORROW", 80, 106, 0.5, "#e0e0e0", t0=3.0)], trans="static", td=0.6)],
    [line(3.5, 3.6, "announcer", "This is WKVR, Channel 7, Halloway Falls.")],
    [cue(0.2, "jingle_wkvr", 0.9)], mono=True,
    etitle="Recording: WKVR station identification", enote="Routine station ID. Black and white.")

bc("today_tuesday", "Halloway Today (Tuesday)", 48, [
    shot(0, "studio_today", FULL, actors=[actor("dot", 56, 98, 62, "dot")], texts=[text("HALLOWAY TODAY", 80, 8, 1.4, box="#a0202020", t1=4.5)], trans="dissolve", td=0.8),
    shot(5, "studio_today", DOT_CU, [22, 30, 70, 52.5], actors=[actor("dot", 56, 98, 62, "dot")]),
    shot(19.5, "tower_night", [40, 60, 80, 60], [40, 0, 80, 60], trans="dissolve", td=0.8),
    shot(24.5, "studio_today", FULL, actors=[actor("dot", 56, 98, 62, "dot")], trans="dissolve"),
    shot(29.5, "studio_today", DOT_CU, [26, 32, 64, 48], actors=[actor("dot", 56, 98, 62, "dot")]),
    shot(40, "studio_today", FULL, actors=[actor("dot", 56, 98, 62, "dot")], texts=[text("HALLOWAY TODAY", 80, 8, 1.4, box="#a0202020", t0=4)])],
    [line(1.0, 4.0, "dot", "Good morning, Halloway Falls! It's Tuesday, the twenty-third of October."),
     line(5.2, 4.0, "dot", "A chilly one, so bundle up the little ones before the school bus."),
     line(9.6, 4.6, "dot", "The Grange fair opens in Delmar on Saturday. Pie entries close Friday noon."),
     line(14.6, 4.8, "dot", "And Thursday morning, our own chief engineer Arthur Penrose joins me right here..."),
     line(19.8, 4.4, "dot", "...to tell us what his Skyhook tower is cooking up for our big Futura demonstration."),
     line(24.6, 4.6, "dot", "Mr. Brandt promises it'll all be done right by the book."),
     line(29.6, 5.0, "dot", "And one more thing - tomorrow our new color cameras arrive. Can you imagine?"),
     line(35.0, 4.6, "dot", "Halloway Today, in living color. Thursday morning, eight o'clock sharp."),
     line(40.2, 5.0, "dot", "Until then, this is Dot Kimball saying: keep your antenna pointed at tomorrow!")],
    [cue(0.1, "music_morning", 0.7), cue(44.5, "applause", 0.6)], mono=True,
    etitle="Recording: Halloway Today (Tuesday 23 Oct)",
    enote="Dot Kimball, live, Tuesday 23 October, in black and white. Says Penrose will be her guest on THURSDAY morning, and that the colour cameras arrive tomorrow (Wednesday).")

bc("futura_ad_62", "Futura Kitchen advertisement", 26, [
    shot(0, "futura_card", FULL, texts=[text("presents", 80, 92, 0.7, "#ffffff", t0=1)], trans="static", td=0.4),
    shot(3, "kitchen", FULL, actors=[actor("servo", 112, 104, 66, "servo")], texts=[lower("THE FUTURA KITCHEN - MODEL 62", 1)]),
    shot(12.5, "kitchen", [56, 24, 100, 75], [70, 30, 88, 66], actors=[actor("servo", 112, 104, 66, "servo")]),
    shot(21, "futura_card", FULL, texts=[text("MODEL 62 - IN STORES NOW", 80, 92, 0.75, box="#a0000000")], trans="dissolve")],
    [line(3.5, 4.0, "announcer", "Mother deserves the future. Futura delivers it - today."),
     line(8.0, 4.2, "servo", "GOOD MORNING. I HAVE STARTED THE COFFEE AND WASHED THE DISHES."),
     line(12.6, 4.6, "announcer", "The Futura Kitchen Model 62: push-button range, self-rinsing sink, and Servo."),
     line(17.5, 3.8, "announcer", "On display now at Halverson's Appliance, Main Street."),
     line(21.6, 3.6, "servo", "FUTURA. TOMORROW, DELIVERED TODAY.")],
    [cue(0.1, "jingle_futura", 0.8), cue(21.2, "jingle_futura", 0.5, 1.12)], mono=True,
    etitle="Recording: Futura Kitchen ad (Model 62)",
    enote="Regular sponsor spot on Channel 7. Advertises the Futura Kitchen Model 62, 'on display now'. No other model is mentioned.")

bc("report_tuesday", "Channel 7 Report (Tuesday)", 52, [
    shot(0, "newsdesk", FULL, actors=[actor("hal", 80, 92, 72, "hal")], texts=[text("CHANNEL 7 REPORT", 80, 8, 1.2, box="#a0101010", t1=5)], trans="static", td=0.5),
    shot(13, "newsdesk", [36, 16, 88, 66], actors=[actor("hal", 80, 92, 72, "hal")]),
    shot(18, "weather_map", FULL, texts=[lower("WEATHER - OHIO VALLEY")], trans="dissolve"),
    shot(23.4, "tower_night", [30, 50, 100, 75], [30, 10, 100, 75], texts=[lower("THE SKYHOOK - THURSDAY 9 PM")], trans="dissolve"),
    shot(34.4, "newsdesk", [36, 16, 88, 66], actors=[actor("hal", 80, 92, 72, "hal")]),
    shot(45, "ident_wkvr", FULL, texts=[text("CHANNEL 7 REPORT", 80, 96, 0.8)], trans="dissolve", td=1.0)],
    [line(2.5, 4.6, "hal", "Good evening. This is the Channel 7 Report for Tuesday, October twenty-third."),
     line(7.5, 5.0, "hal", "From Washington tonight: the Navy quarantine in the Caribbean remains in force."),
     line(13.0, 4.6, "hal", "Officials ask families to stay calm and keep to their routines."),
     line(18.0, 5.0, "hal", "Locally, a cold front moves in overnight. Frost likely by Thursday morning."),
     line(23.5, 5.0, "hal", "Clear skies Thursday evening - good news for WKVR's Skyhook demonstration."),
     line(29.0, 5.0, "hal", "Guests from Futura Corporation will watch a single-hop telecast, coast to coast."),
     line(34.5, 5.2, "hal", "Station manager Walter Brandt says the tower will operate within its licensed power."),
     line(40.0, 4.8, "hal", "That's the Report. Goodnight, and keep your sets tuned to Channel 7.")],
    [cue(0.1, "music_news", 0.8), cue(45.2, "jingle_wkvr", 0.6)], mono=True,
    etitle="Recording: Channel 7 Report (Tuesday)",
    enote="Hal Whitcomb, Tuesday evening. Thursday's Skyhook demonstration will run 'within its licensed power', according to Walter Brandt.")

bc("testcard_wkvr", "WKVR test pattern", 24, [shot(0, "testcard_wkvr", FULL, fx={"stat": 0.05})], [],
   [cue(0.2, "test_tone", 0.5), cue(12.2, "test_tone", 0.5)], mono=True,
   etitle="Recording: WKVR test pattern", enote="The station's test card and 1,000-cycle tone.")

bc("testcard_ktrx", "KTRX test pattern (Channel 4)", 30, [shot(0, "testcard_ktrx", FULL, fx={"stat": 0.62, "roll": 0.25, "ghost": 0.35, "tear": 0.15})],
   [], [], mono=True, etitle="Recording: KTRX test pattern (Channel 4)", enote="A distant station out of Columbus, barely above the snow. Nothing unusual.")

bc("static_gap", "No carrier", 4, [shot(0, "black", FULL, fx={"stat": 1.0, "tear": 0.4})], unrecordable=True)
bc("dead_carrier", "Dead carrier", 10, [shot(0, "black", FULL, fx={"stat": 0.0, "bars": 0.35, "vignette": 1.0})], unrecordable=True)
bc("surge_frame", "Echo surge", 3, [shot(0, "studio_emergency", [30, 20, 96, 72], actors=[actor("dot", 72, 102, 66)],
   fx={"stat": 0.5, "roll": 0.7, "tear": 0.9, "ghost": 0.4, "flicker": 0.5}, tint="#ffc8b8")], unrecordable=True)

# ---------------------------------------------------------------- phantom 7-B (colour)
PFX = {"ghost": 0.14, "tear": 0.06, "stat": 0.08}
bc("p1_today_thursday", "Halloway Today (Thursday)", 58, [
    shot(0, "studio_today", FULL, actors=[actor("dot", 50, 98, 62, "dot"), actor("penrose", 114, 98, 64, "penrose")],
         texts=[text("HALLOWAY TODAY", 80, 8, 1.4, box="#a0302018", t1=6), text("in LIVING COLOR!", 80, 24, 0.8, "#ffd060", t0=1, t1=6)], trans="static", td=0.8),
    shot(10.5, "studio_today", [10, 28, 76, 57], actors=[actor("dot", 50, 98, 62, "dot"), actor("penrose", 114, 98, 64, "penrose")]),
    shot(15.5, "studio_today", [78, 26, 76, 57], actors=[actor("dot", 50, 98, 62, "dot"), actor("penrose", 114, 98, 64, "penrose")], texts=[lower("ARTHUR PENROSE - CHIEF ENGINEER, WKVR", 0.5, 5)]),
    shot(20, "studio_today", FULL, actors=[actor("dot", 50, 98, 62, "dot"), actor("penrose", 114, 98, 64, "penrose")]),
    shot(25.4, "studio_today", [78, 26, 76, 57], [84, 30, 66, 49.5], actors=[actor("dot", 50, 98, 62, "dot"), actor("penrose", 114, 98, 64, "penrose")]),
    shot(36, "studio_today", [10, 28, 76, 57], actors=[actor("dot", 50, 98, 62, "dot"), actor("penrose", 114, 98, 64, "penrose")]),
    shot(40.4, "studio_today", [80, 28, 72, 54], [88, 32, 60, 45], actors=[actor("dot", 50, 98, 62, "dot"), actor("penrose", 114, 98, 64, "penrose")]),
    shot(46, "studio_today", FULL, actors=[actor("dot", 50, 98, 62, "dot"), actor("penrose", 114, 98, 64, "penrose")]),
    shot(52.5, "tower_night", [20, 0, 120, 90], FULL, texts=[text("HALLOWAY TODAY", 80, 100, 1.0, box="#a0302018")], trans="dissolve", fx={"flicker": 0.2})],
    [line(1.5, 4.5, "dot", "Good morning, Halloway Falls! It's Thursday, October twenty-fifth..."),
     line(6.2, 4.0, "dot", "...and you're seeing us in living color for the very first time!"),
     line(10.6, 4.6, "dot", "My guest is the man who keeps Channel 7 on the air - Arthur Penrose."),
     line(15.6, 4.0, "penrose", "Good morning, Dot. Good morning, Halloway Falls."),
     line(20.1, 5.0, "dot", "Arthur, tonight the whole country is watching your Skyhook. What happens at nine?"),
     line(25.5, 5.2, "penrose", "At six this morning we took her to full output. Tonight we hold it for our Futura guests."),
     line(31.0, 4.8, "penrose", "Two hundred kilowatts, Dot. Coast to coast in a single hop - off the sky itself."),
     line(36.1, 4.0, "dot", "Two hundred! Is that... allowed?"),
     line(40.5, 5.0, "penrose", "Ha! Walter has the paperwork in hand. Tonight, we make history."),
     line(46.1, 5.0, "dot", "Well, I'll be right here in Studio A all evening. Wave to us, America!"),
     line(51.4, 4.8, "dot", "Halloway Today will be right back, after this word from Futura.")],
    [cue(0.1, "static_burst", 0.6), cue(0.8, "music_morning", 0.7), cue(52.6, "jingle_wkvr", 0.5, 0.94)], fx=PFX,
    etitle="Recording: 7-B - 'Halloway Today' (Thursday 25 Oct)",
    enote="Received on the ghost carrier, in COLOUR. Dot says it is Thursday 25 October. Her guest Penrose says the Skyhook went to 'full output' at 6 AM - 'two hundred kilowatts'.")

bc("p2_lucky_dial", "Lucky Dial Jackpot", 52, [
    shot(0, "contest", FULL, actors=[actor("dot", 46, 102, 58, "dot")], texts=[text("LUCKY DIAL JACKPOT!", 80, 6, 1.1, "#ffe070", "#c0401018", blink=True, t1=6)], trans="static", td=0.6),
    shot(6.5, "contest", [64, 8, 88, 66], [74, 14, 72, 54], actors=[actor("dot", 46, 102, 58, "dot")], texts=[lower("CALLER: MRS. ELEANOR VOSS, PINE STREET", 0.5)]),
    shot(15.4, "contest", [8, 26, 80, 60], actors=[actor("dot", 46, 102, 58, "dot")]),
    shot(25.4, "contest", FULL, actors=[actor("dot", 46, 102, 58, "dot")]),
    shot(30, "futura_card", FULL, texts=[text("presents", 80, 92, 0.7, t0=0.8)], trans="static", td=0.4),
    shot(33, "kitchen", FULL, actors=[actor("servo", 112, 104, 66, "servo")], texts=[lower("THE NEW FUTURA KITCHEN - MODEL 63", 0.5)]),
    shot(38, "kitchen", [56, 24, 100, 75], [66, 28, 90, 67.5], actors=[actor("servo", 112, 104, 66, "servo")]),
    shot(43.4, "futura_card", FULL, texts=[text("MODEL 63  -  IN SHOWROOMS THIS SPRING", 80, 92, 0.6, box="#a0000000")], trans="dissolve")],
    [line(1.4, 4.2, "dot", "It's time for Lucky Dial! Phones are ringing all over the valley..."),
     line(6.0, 4.6, "dot", "...and our lucky caller is Mrs. Eleanor Voss, of Pine Street!"),
     line(11.0, 4.0, "voss", "Oh my stars! Is this really the television?"),
     line(15.5, 5.0, "dot", "It is! Mrs. Voss, you've won a Futura Kitchen - the brand-new Model 63!"),
     line(21.0, 4.0, "voss", "A Model 63? I didn't know they'd made one!"),
     line(25.5, 4.0, "dot", "Nobody did until today! Stay with us for a first look."),
     line(33.4, 4.2, "servo", "GOOD EVENING. I AM THE NEW SERVO. I NOW FOLD LAUNDRY."),
     line(38.0, 5.0, "announcer", "Introducing the Futura Kitchen Model 63 - in showrooms this spring."),
     line(43.5, 3.8, "announcer", "Model 63. Because tomorrow can't wait."),
     line(47.6, 3.8, "servo", "FUTURA. TOMORROW, DELIVERED TODAY.")],
    [cue(0.1, "static_burst", 0.5), cue(0.5, "music_contest", 0.75), cue(11.2, "applause", 0.5), cue(30.1, "jingle_futura", 0.8), cue(43.6, "jingle_futura", 0.5, 1.12)], fx=PFX,
    etitle="Recording: 7-B - 'Lucky Dial' + Futura Model 63 ad",
    enote="Received on the ghost carrier, in colour. Dot gives away a Futura Kitchen 'Model 63' and an advertisement announces the Model 63 'in showrooms this spring'.")

bc("p3_report_thursday", "Channel 7 Report (Thursday)", 50, [
    shot(0, "newsdesk", FULL, actors=[actor("hal", 80, 92, 72, "hal")], texts=[text("CHANNEL 7 REPORT", 80, 8, 1.2, box="#a0101828", t1=5)], trans="static", td=0.6),
    shot(12, "tower_night", [30, 60, 100, 75], [30, 0, 100, 75], texts=[lower("LIVE TONIGHT - 9:00 PM", 0.5)], trans="dissolve"),
    shot(23, "newsdesk", [36, 16, 88, 66], actors=[actor("hal", 80, 92, 72, "hal")]),
    shot(33.4, "weather_map", FULL, texts=[lower("THURSDAY NIGHT - CLEAR, COLD")], trans="dissolve"),
    shot(39, "newsdesk", FULL, actors=[actor("hal", 80, 92, 72, "hal")]),
    shot(45, "newsdesk", FULL, actors=[actor("hal", 80, 92, 72, "hal")], fx={"tear": 0.5, "stat": 0.35, "flicker": 0.3})],
    [line(2.0, 4.8, "hal", "Good evening. The Channel 7 Report for Thursday, October twenty-fifth."),
     line(7.4, 4.6, "hal", "Tonight at nine, WKVR's Skyhook tower makes broadcasting history."),
     line(12.5, 5.2, "hal", "Since six this morning the Skyhook has carried our programs at two hundred kilowatts..."),
     line(18.0, 4.6, "hal", "...four times the station's ordinary output, for our guests from Futura Corporation."),
     line(23.1, 5.0, "hal", "Chief engineer Arthur Penrose calls it 'the first true single-hop telecast'."),
     line(28.5, 4.6, "hal", "In Delmar, the Grange fair committee reports record entries for Saturday."),
     line(33.5, 5.0, "hal", "And it's a clear, cold night - perfect, they tell me, for reaching the sky."),
     line(39.1, 5.2, "hal", "Stay with Channel 7 for the demonstration, live from Studio A, at nine.")],
    [cue(0.1, "static_burst", 0.5), cue(0.4, "music_news", 0.8), cue(45.2, "static_burst", 0.6)], fx=PFX,
    etitle="Recording: 7-B - 'Channel 7 Report' (Thursday)",
    enote="Received on the ghost carrier. Hal Whitcomb, Thursday 25 October: the Skyhook has run 'since six this morning' at TWO HUNDRED KILOWATTS - four times the station's ordinary output.")

bc("p4_last_broadcast", "Special Bulletin", 46, [
    shot(0, "standby", FULL, fx={"stat": 0.3, "roll": 0.2}, trans="static", td=0.8),
    shot(4, "studio_emergency", [20, 10, 120, 90], actors=[actor("dot", 72, 104, 66, "dot")], fx={"shake": 1.0, "flicker": 0.3, "tear": 0.15, "stat": 0.15}, tint="#ffb8a8",
         texts=[lower("SPECIAL BULLETIN - WKVR STUDIO A", 0.5, 6)]),
    shot(14, "studio_emergency", [38, 26, 68, 51], actors=[actor("dot", 72, 104, 66, "dot")], fx={"shake": 1.4, "flicker": 0.35, "tear": 0.2, "stat": 0.18}, tint="#ffb0a0"),
    shot(25, "studio_emergency", FULL, actors=[actor("dot", 72, 104, 66, "dot")], fx={"shake": 1.2, "roll": 0.5, "tear": 0.5, "stat": 0.3, "flicker": 0.4}, tint="#ffa898"),
    shot(30, "studio_emergency", [42, 28, 64, 48], actors=[actor("dot", 72, 104, 66, "dot")], fx={"shake": 1.6, "flicker": 0.6, "tear": 0.6, "stat": 0.35}, tint="#ff9888"),
    shot(38.5, "black", FULL, fx={"stat": 0.7, "roll": 2.0, "tear": 1.0}),
    shot(41, "black", FULL, fx={"stat": 0.0, "bars": 0.3, "vignette": 1.0})],
    [line(4.5, 4.2, "dot", "We - we interrupt our program. This is Dot Kimball in Studio A."),
     line(9.0, 4.6, "dot", "There's a fire in our transmitter hall. Please - please don't telephone the station."),
     line(14.0, 5.0, "dot", "Arthur - Mr. Penrose - went in to shut the tower down. He's following the procedure."),
     line(19.4, 4.6, "dot", "He opened the coolant - valve three, it's on the card - but nothing's cooling."),
     line(24.5, 5.0, "dot", "We're staying on the air. Somebody has to tell you what's happening."),
     line(30.0, 4.0, "dot", "It's nine fourteen. The lights are going -"),
     line(34.4, 4.0, "dot", "This is WKVR, Channel 7, Halloway Falls. We -")],
    [cue(0.0, "static_burst", 0.8), cue(4.2, "music_bulletin", 0.6), cue(4.6, "alarm", 0.35, 0.8), cue(6.0, "fire_crackle", 0.5),
     cue(16.0, "fire_crackle", 0.6), cue(26.0, "fire_crackle", 0.7), cue(38.5, "static_burst", 1.0), cue(41.0, "anomaly_tone", 0.7)],
    flash={"t": 22.3, "d": 0.1, "bg": "insert_console", "evidence": "frame:console",
           "caption": "FRAME 446 - one frame of the transmitter console: VALVE 3 lever OPEN, coolant flow ZERO, plate temperature pinned. Wall clock: 9:14."},
    etitle="Recording: 7-B - special bulletin, Thursday 9:14 PM",
    enote="Received on the ghost carrier. Dot Kimball, live, during a FIRE in the transmitter hall. Penrose was following the shutdown procedure: 'valve three, it's on the card'. The picture dies at 9:14 PM. Something flickers in the middle of the tape.")

bc("signoff", "WKVR sign-off", 40, [
    shot(0, "studio_today", FULL, actors=[actor("dot", 56, 98, 62, "dot")], trans="dissolve", td=1.0),
    shot(6, "studio_today", DOT_CU, [24, 30, 68, 51], actors=[actor("dot", 56, 98, 62, "dot")]),
    shot(22, "signoff_card", FULL, texts=[text("GOOD NIGHT FROM WKVR", 80, 92, 0.9, box="#a0000000", t0=0.5)], trans="dissolve", td=1.5),
    shot(29, "testcard_wkvr", FULL, trans="dissolve", td=1.0)],
    [line(1.0, 4.6, "dot", "Well, folks, it's a little past midnight, and that's all from WKVR tonight."),
     line(6.0, 5.0, "dot", "A late change to the schedule: Thursday's Skyhook demonstration is postponed."),
     line(11.5, 5.0, "dot", "Mr. Penrose tells me the old girl needs a long rest. I can't say I blame her."),
     line(17.0, 4.6, "dot", "And to whoever was out there tonight, looking out for us - thank you."),
     line(22.6, 5.4, "announcer", "This is WKVR, Channel 7, Halloway Falls. This concludes our broadcast day.")],
    [cue(0.5, "music_signoff", 0.8), cue(29.5, "test_tone", 0.4)], mono=True,
    etitle="Recording: WKVR sign-off (Tuesday night)", enote="The station signs off normally. Thursday's demonstration is postponed indefinitely.")

bc("memo_penrose", "Unlabeled reel", 32, [shot(0, "black", FULL)],
   [line(1.0, 5.0, "penrose", "Engineering memo, Arthur Penrose. October sixteenth. Overdrive run five, complete.", reversedSpeech=True),
    line(6.6, 5.4, "penrose", "The ghost was on the monitor again. Thursday's programs, plain as day.", reversedSpeech=True),
    line(12.6, 5.0, "penrose", "I've stopped telling Walter what I see. He only asks whether Futura will be pleased.", reversedSpeech=True),
    line(18.2, 5.6, "penrose", "The log stays in my locker. Same combination as the demonstration date - month, then day.", reversedSpeech=True),
    line(24.4, 5.0, "penrose", "God help me, I want to see what happens next. End of memo.", reversedSpeech=True)],
   [], audioOnly=True,
   etitle="Unlabeled reel - 'A.P. - DO NOT ERASE'",
   enote="Rosa found it in the erase bin. Penrose's voice, but played forwards it is gibberish - as if the reel had been threaded backwards when it was recorded.")

order = ["ident_wkvr", "today_tuesday", "futura_ad_62", "report_tuesday", "testcard_wkvr", "testcard_ktrx", "static_gap",
         "dead_carrier", "surge_frame", "p1_today_thursday", "p2_lucky_dial", "p3_report_thursday", "p4_last_broadcast", "signoff", "memo_penrose"]
for k in order:
    with open(f"{OUT}/broadcasts/{k}.json", "w") as f:
        json.dump(broadcasts[k], f, indent=1)
for k, b in broadcasts.items():
    end = max([l["t"] + l["d"] for l in b["lines"]] + [0])
    assert end <= b["duration"], (k, end)
    assert all(len(l["text"]) < 120 for l in b["lines"]), k

# ---------------------------------------------------------------- index
speakers = {
    "announcer": {"name": "Announcer", "voice": "voice_hal", "pitch": 0.88},
    "dot": {"name": "Dot Kimball", "voice": "voice_dot", "pitch": 1.0},
    "hal": {"name": "Hal Whitcomb", "voice": "voice_hal", "pitch": 1.0},
    "penrose": {"name": "Arthur Penrose", "voice": "voice_penrose", "pitch": 1.0},
    "servo": {"name": "SERVO", "voice": "voice_servo", "pitch": 1.0},
    "voss": {"name": "Mrs. Voss", "voice": "voice_dot", "pitch": 1.28},
    "rosa": {"name": "Rosa Delgado", "voice": "voice_rosa", "pitch": 1.0},
    "brandt": {"name": "Walter Brandt", "voice": "voice_brandt", "pitch": 1.0},
    "player": {"name": "You", "voice": "voice_hal", "pitch": 1.1},
}
fragments = {
    "call_1": [line(0.4, 3.0, None, "[ static. Far away, a room full of people. ]"),
               line(3.6, 4.0, "dot", "...please - please don't telephone the station..."),
               line(8.0, 3.0, None, "[ the line goes dead ]")],
    "call_2": [line(0.4, 3.8, "penrose", "...valve three is open - why isn't it cooling -"),
               line(4.6, 3.0, None, "[ an alarm bell, muffled, as if through a wall ]"),
               line(8.0, 3.0, "dot", "Arthur? Arthur!")],
    "call_3": [line(0.4, 4.2, "dot", "...it's nine fourteen. We're staying on the air..."),
               line(5.0, 3.6, None, "[ silence. Then, very faintly, a test tone. ]")],
}
evidence = {
    "frame:console": {"title": "Frame 446 of the 9:14 bulletin", "kind": "frame",
                      "note": "A single frame spliced into the bulletin: the Skyhook console mid-fire. Valve 3 lever is OPEN, coolant flow reads ZERO, plate temperature pinned. Clock 9:14."}
}
index = {"broadcasts": order, "dialogues": ["rosa", "penrose", "dot", "brandt"], "speakers": speakers, "evidence": evidence, "fragments": fragments}
with open(f"{OUT}/index.json", "w") as f:
    json.dump(index, f, indent=1)

# ---------------------------------------------------------------- documents
def doc(id, title, kind, header, pages, note, stamp=None):
    d = {"id": id, "title": title, "kind": kind, "header": header, "pages": pages, "note": note}
    if stamp: d["stamp"] = stamp
    for p in pages:
        assert len(p) < 470, (id, len(p))
    return d

documents = [
    doc("assignment", "FCC Field Assignment", "letter", "Federal Communications Commission - Detroit District", [
        "TO: Field Engineer, Monitoring Unit 14\nFROM: R. Haskell, District Engineer\nDATE: October 22, 1962\nRE: Complaint 62-1147, WKVR-TV, Halloway Falls, Ohio\n\nAn anonymous complainant reports intermittent interference on Channel 7: a second, faint picture showing programs WKVR says it never aired.",
        "Proceed to WKVR-TV and determine the source. The night operator has agreed to assist.\n\nFor reference: WKVR-TV is licensed for 50 KILOWATTS effective radiated visual power on Channel 7 (visual carrier 175.25 Mc). Any operation above that figure violates the license and Commission rules.",
        "You carry the Commission's authority to inspect the transmitter plant, its logs and its records. Report your findings in writing.\n\nR. Haskell\nDistrict Engineer"],
        "Complaint 62-1147: a second faint picture on Channel 7. WKVR-TV is licensed for 50 kilowatts visual power. Anything above that is a violation.", "OFFICIAL"),
    doc("complaint_log", "Night Operator's Interference Log", "log", "WKVR Master Control - kept by R. Delgado", [
        "VIEWER CALLS - 'GHOST PICTURE' ON CH. 7\n\nTue Sep 18 - 3 calls. Faint second picture, 'like a negative'.\nTue Sep 25 - 7 calls. Ghost shows a game show we don't carry.\nTue Oct 2 - 4 calls. Ghost picture rolls slowly upward.\nTue Oct 9 - 11 calls. Dot Kimball on the ghost - 'in color'.",
        "Tue Oct 16 - 9 calls. Mr. P. stayed late in the hall again.\nTue Oct 23 - tonight. Ghost on the monitors since 10 PM.\n\nNOTES: Never on any other night. Sits right under our own carrier, phase-inverted. Our picture swamps it unless you null ours out. H-hold has to come way down to stop the roll.\n- R.D."],
        "Ghost pictures reported ONLY on Tuesday nights: Sep 18, Sep 25, Oct 2, Oct 9, Oct 16 and tonight. Tuning: right under Channel 7's own carrier, phase-inverted, rolling - null our carrier, bring the H-hold well down."),
    doc("program_log", "WKVR Program Log", "log", "Week of October 22 - master control copy", [
        "TUE 10/23\n 8:00 AM Halloway Today (D. Kimball) - live\n 6:00 PM Channel 7 Report (H. Whitcomb)\n 6:20 PM Futura Kitchen spot - Model 62\n12:05 AM Sign-off\n\nWED 10/24\n Color camera delivery - Studio A dark 1-5 PM",
        "THU 10/25\n 6:00 AM SKYHOOK DEMONSTRATION BEGINS\n 8:00 AM Halloway Today - guest A. Penrose\n   ** TO BE TELECAST LIVE - not yet produced **\n 6:00 PM Channel 7 Report\n 9:00 PM Futura demonstration special, Studio A\n\nSponsor copy for 'Model 63': NONE ON FILE - ask W.B."],
        "Thursday 25 Oct: 'Halloway Today' with guest A. Penrose will be telecast live - it has NOT been produced yet. No sponsor copy exists for any 'Model 63'."),
    doc("program_guide", "TV Week clipping", "clipping", "Halloway Courier, Saturday October 20", [
        "TV WEEK - CHANNEL 7 HIGHLIGHTS\n\nTUESDAY: Halloway Today, 8 AM. Dot Kimball previews the Delmar Grange fair.\n\nTHURSDAY: Halloway Today, 8 AM. Special guest - WKVR's own Arthur Penrose on the Skyhook tower! Then at 9 PM, the Futura Coast-to-Coast Spectacular, live from Studio A."],
        "Newspaper TV listings: Penrose is Dot's guest on THURSDAY's 'Halloway Today' - two days from now."),
    doc("staff_roster", "WKVR Staff Roster", "log", "October 1962", [
        "Walter Brandt - Station Manager\nArthur Penrose - Chief Engineer (transmitter plant, Skyhook)\nRosa Delgado - Master Control, nights\nDorothy Kimball - Host, Halloway Today\nHal Whitcomb - News\n\nTRANSMITTER KEYS: A. Penrose only.\nNight operation of the Skyhook: Chief Engineer only.\n- W.B."],
        "Only Penrose holds the transmitter keys. Night operation of the Skyhook is by the chief engineer only."),
    doc("futura_letter", "Letter from Futura Corporation", "letter", "Futura Corporation - Dayton, Ohio - October 3, 1962", [
        "Dear Mr. Brandt,\n\nOur continued sponsorship of WKVR is contingent upon Thursday's demonstration achieving reliable single-hop reception in Los Angeles. Our engineers estimate that this will require at least 200 kilowatts.",
        "We trust Mr. Penrose's 'special arrangement' will be in place.\n\nThe Model 63 kitchen remains STRICTLY CONFIDENTIAL until our January launch. Please ensure no mention of it is made on the air.\n\nR. Maddox\nVice President, Advertising"],
        "Futura wants 200 kilowatts for Thursday. The 'Model 63' kitchen is confidential until January - nobody may mention it on air.", "CONFIDENTIAL"),
    doc("skyhook_brochure", "Skyhook Dedication Program", "clipping", "WKVR-TV - June 1959", [
        "WKVR proudly dedicates the SKYHOOK RESONANT ARRAY, designed by our chief engineer Arthur Penrose, formerly of Bell Laboratories.\n\nHeight: 480 feet\nLicensed power: 50,000 watts\n\n'A tower that reaches the sky - and is wise enough to stop there.'\n- A. Penrose"],
        "The Skyhook was dedicated in 1959. Licensed power: 50,000 watts (50 kW)."),
    doc("transmitter_log", "Skyhook Transmitter Log", "log", "A. Penrose - PRIVATE - DO NOT REMOVE", [
        "OVERDRIVE RUNS (approx. 200 kW visual)\n\n1 - Tue 9/18 - 11 PM to 2 AM\n2 - Tue 9/25 - 11 PM to 3 AM\n3 - Tue 10/2 - 10 PM to 3 AM\n4 - Tue 10/9 - 10 PM to 4 AM\n5 - Tue 10/16 - 10 PM to 4 AM\n6 - Tue 10/23 - 10 PM - (tonight)\n7 - Thu 10/25 - 6 AM - FULL DEMONSTRATION, 24 hr",
        "NOTES\n\nRun 3: a picture on the monitor that is NOT ours. Same carrier, inverted.\nRun 4: moved the coolant feed to VALVE 5 for the overdrive manifold. Valve 3 line now dead-ended. MUST revise the posted shutdown card!\nRun 5: the ghost is Thursday 10/25. Our own programs, two days early.",
        "Every run receives the SAME night: Thursday. The last run there will ever be. Why the last? What happens after it?\n\nW.B. signed for all of the above.\n- A.P."],
        "Penrose has run the Skyhook in overdrive (about 200 kW) every Tuesday night since 18 September - and tonight. Run 4: coolant feed moved to VALVE 5; valve 3 is dead-ended. The posted shutdown card was never revised.", "PRIVATE"),
    doc("procedure", "Emergency Shutdown Procedure", "notice", "Skyhook Overdrive - Rev. 3 - June 1962 - POST AT CONSOLE", [
        "IN AN EMERGENCY:\n\n1. Open COOLANT VALVE 3. Confirm flow on the gauge.\n2. Reduce FINAL DRIVE to zero.\n3. Open the PLATE BREAKER.\n4. Close the ANTENNA GROUND.\n\nNEVER open the plate breaker under drive.\nNEVER ground a live antenna.\n\nA. Penrose, Chief Engineer"],
        "Posted at the console (Rev. 3, June 1962): 1) open coolant VALVE 3, 2) final drive to zero, 3) open plate breaker, 4) close antenna ground.", "REV. 3"),
]
with open(f"{OUT}/documents.json", "w") as f:
    json.dump(documents, f, indent=1)

deductions = [
    {"id": "D1", "title": "Two Days Early", "a": ["tape:p1_today_thursday"], "b": ["doc:program_log", "doc:program_guide", "tape:today_tuesday"],
     "text": "7-B carried Thursday's 'Halloway Today', with Penrose as guest - a programme WKVR hasn't made yet, and in colour the station doesn't have yet. The ghost is no stray station. It is WKVR, two days from now."},
    {"id": "D2", "title": "Over the Limit", "a": ["tape:p3_report_thursday", "tape:p1_today_thursday"], "b": ["doc:assignment", "doc:skyhook_brochure"], "requires": ["D1"],
     "text": "Thursday's broadcasts describe the Skyhook at 200 kilowatts since six in the morning - four times WKVR's licensed 50 kW. Someone here means to break the licence, and only the chief engineer runs the Skyhook."},
    {"id": "D3", "title": "Ghosts Keep a Schedule", "a": ["doc:transmitter_log"], "b": ["doc:complaint_log"], "requires": ["D2"],
     "text": "Every night viewers saw the ghost, Penrose had the Skyhook in overdrive - and only then. The overdrive is the receiver - and every run picks up the last one the Skyhook will ever make: Thursday."},
    {"id": "D4", "title": "The Wrong Valve", "a": ["frame:console"], "b": ["doc:procedure", "doc:transmitter_log"], "requires": ["D3"],
     "text": "In the 9:14 frame valve 3 is open and nothing flows. The card on the wall says valve 3, but Penrose moved the coolant feed to valve 5 in October and never fixed it. Followed to the letter, the procedure starts the fire."},
    {"id": "DX", "title": "Next Year's Model", "a": ["tape:p2_lucky_dial"], "b": ["tape:futura_ad_62", "doc:futura_letter"], "optional": True,
     "text": "Futura's Model 63 is confidential until January, and the only ad on file is for the Model 62 - yet 7-B gives a Model 63 away on air. Whatever 7-B shows, WKVR will broadcast."},
]
with open(f"{OUT}/deductions.json", "w") as f:
    json.dump(deductions, f, indent=1)

# ---------------------------------------------------------------- dialogue
def node(lines, choices=(), pose="gesture", enter=()):
    return {"lines": [{"who": w, "text": t} for w, t in lines], "choices": list(choices), "pose": pose, "enter": list(enter)}

def choice(text, next=None, actions=(), ifs=(), ifnot=(), ifev=()):
    return {"text": text, "next": next, "actions": list(actions), "if": list(ifs), "ifNot": list(ifnot), "ifEvidence": list(ifev)}

def root(node_id, ifs=(), ifnot=()):
    return {"node": node_id, "if": list(ifs), "ifNot": list(ifnot)}

TAPE = choice("Could I have more blank tape?", "tape_given", [{"give": "tape:blank", "count": 2}])
dialogues = {}
dialogues["rosa"] = {"name": "Rosa Delgado", "role": "Master Control, nights", "roots": [
    root("intro", ifnot=["MET_ROSA"]), root("end", ["CASE_SOLVED"]), root("late", ["D3"]), root("d2b", ["D2", "GOT_REEL"]),
    root("d2", ["D2"]), root("d1", ["D1"]), root("locked", ["SIGNAL_LOCKED"]), root("tuning")], "nodes": {
    "intro": node([("rosa", "You must be the engineer from the FCC. Rosa Delgado, master control. Come in, mind the cables."),
                   ("rosa", "I'll save you some trouble: I'm the one who wrote to you. Please don't say so in front of Mr. Brandt."),
                   ("player", "Your letter mentioned a second picture on Channel 7. Programs you never aired."),
                   ("rosa", "Every Tuesday night since September. The viewers call me, and I keep a log. It's all in here.")],
                  [choice("Tell me about the ghost picture.", "intro2", [{"set": "MET_ROSA"}, {"give": "doc:complaint_log"}])], "nervous"),
    "intro2": node([("rosa", "It hides right under our own carrier. Same frequency, but phase-inverted - like a photographic negative."),
                    ("rosa", "Our picture swamps it. Null ours out on the analyzer and it's there. And it rolls - bring the H-hold way down."),
                    ("rosa", "The analyzer is on the bench by the far wall. Get a clean lock and patch it through to the house monitors.")],
                   [choice("Can I record what's on it?", "tape_info"), choice("I'll get to work.")], "point"),
    "tape_info": node([("rosa", "The deck on the bench records off the monitor bus. Thread a reel, switch on a set nearby, press REC."),
                       ("rosa", "Blank stock is on the shelves in the tape library, across the corridor. Nobody counts it but me.")],
                      [choice("Thanks.")]),
    "tape_given": node([("rosa", "Here - two reels. Don't tell Brandt how much we go through.")], [choice("Thank you.")], "idle"),
    "tuning": node([("rosa", "Any luck on the analyzer? Same frequency as ours. Null the phase. Slow the roll.")],
                   [choice("Remind me how to record it.", "tape_info"), TAPE, choice("Back to work.")], "idle"),
    "locked": node([("rosa", "There it is. Channel 7-B, you're calling it? Look at it. That's Dot. That's our set."),
                    ("rosa", "And it's in color. We don't own a color camera. They're delivered tomorrow.")],
                   [choice("What's on the schedule this week?", "schedule"), TAPE, choice("I'll keep watching.")], "point"),
    "schedule": node([("rosa", "The program log is in the cabinet by the case board - master control copy. Thursday is the big Futura day.")], [choice("Thanks.")]),
    "d1": node([("rosa", "Thursday's show, on a Tuesday night. You know what you're telling me?"),
                ("rosa", "Then I'll say the other part. If the ghost is us - Thursday us - then whatever it shows is coming.")],
               [TAPE, choice("Keep the monitors on 7-B.")], "nervous"),
    "d2": node([("rosa", "Two hundred kilowatts. It has to be Mr. Penrose. Nobody else touches the Skyhook."),
                ("rosa", "Tuesdays he sends me home early and stays in the hall. Then the calls start."),
                ("rosa", "There's something else. I found this reel in the erase bin last week. His initials. It sounds like nonsense.")],
               [choice("Let me hear it.", "reel_hint", [{"set": "GOT_REEL"}, {"give": "reel:memo"}])], "nervous"),
    "reel_hint": node([("rosa", "Forwards it's gibberish. But he's an old radio ham - they do tricks with tape. Try every button on the deck.")], [choice("I will.")]),
    "d2b": node([("rosa", "Did the reel make sense? Engineers think backwards sometimes.")], [TAPE, choice("Back to work.")], "idle"),
    "late": node([("rosa", "Thursday night. A bulletin. Dot, and then a fire, and then nothing at all."),
                  ("rosa", "Talk to Mr. Penrose. Make him stop. He'll listen to the law if he won't listen to me.")],
                 [TAPE, choice("I'm going.")], "nervous"),
    "end": node([("rosa", "7-B's gone. Nothing but carrier hum. I keep looking at it anyway."),
                 ("rosa", "I'll put in a good word for you with the Commission. Off the record, of course.")], [choice("Goodnight, Rosa.")]),
}}
dialogues["penrose"] = {"name": "Arthur Penrose", "role": "Chief Engineer", "roots": [
    root("end", ["CASE_SOLVED"]), root("after", ["PENROSE_CONFESSED"]), root("confront", ["D3"]), root("defensive", ["D2"]),
    root("small", ["MET_PENROSE"]), root("intro")], "nodes": {
    "intro": node([("penrose", "Ah - the Commission's engineer. Arthur Penrose. Forgive the noise; the Skyhook is warming her feet tonight."),
                   ("penrose", "Routine maintenance. Tuesdays are quiet. Is there anything I can show you?")],
                  [choice("What is the Skyhook, exactly?", "skyhook", [{"set": "MET_PENROSE"}]), choice("Not yet.", None, [{"set": "MET_PENROSE"}])]),
    "skyhook": node([("penrose", "A resonant array. Tune them properly and the tower and the sky behave like a single instrument."),
                     ("penrose", "Fifty kilowatts, all perfectly proper. The licence is framed in Walter's office.")], [choice("Thank you.")], "point"),
    "small": node([("penrose", "Still poking about? The tube banks don't bite. Mostly.")],
                  [choice("What is the Skyhook?", "skyhook"), choice("Never mind.")], "idle"),
    "defensive": node([("penrose", "Two hundred kilowatts? From a ghost on a monitor? You can't put a ghost in a report."),
                       ("penrose", "My logs are in order. And my locker is my own business.")], [choice("We'll see.")], "nervous"),
    "confront": node([("penrose", "You've read my log."),
                      ("player", "Every night the ghost appeared, you had the Skyhook in overdrive. Tonight too."),
                      ("penrose", "...Yes. Since September. I saw the first picture on run three and I could not stop looking."),
                      ("penrose", "It's us, you see. Thursday. Every run shows the last one. Walter needs Thursday. Futura needs Thursday.")],
                     [choice("There's a new programme on 7-B. A bulletin from Thursday night.", "confess2")], "nervous"),
    "confess2": node([("penrose", "A bulletin? I only ever watched the mornings. What does it show?"),
                      ("player", "Dot, on the air, during a fire in your transmitter hall. At 9:14."),
                      ("penrose", "So that's why it's always Thursday. It's the last run because afterwards there's nothing left to run."),
                      ("penrose", "Here. The console key. Shut her down tonight, properly, and I'll cancel Thursday myself.")],
                     [choice("Take the key.", "key", [{"set": "PENROSE_CONFESSED"}])], "gesture"),
    "key": node([("penrose", "Follow the card on the wall beside the console. It's all there. I wrote it myself.")], [choice("All right.")], "point"),
    "after": node([("penrose", "The console is live. Coolant first, then the drive. Never rush the breaker.")],
                  [choice("It arced. I followed your card exactly.", "arc", ifs=["ARC_FAULT_SEEN"]),
                   choice("What if it goes wrong?", "risk"), choice("Ready.")], "point"),
    "arc": node([("penrose", "Valve three... the card says valve three. But in October I moved the feed. For the overdrive manifold."),
                 ("penrose", "Valve five. Dear God, it's valve five. I never changed the card.")], [choice("Then that's what happens on Thursday.")], "nervous"),
    "risk": node([("penrose", "The relay trips and resets. Tonight, at least. On Thursday, I suppose, it didn't.")], [choice("Understood.")], "nervous"),
    "end": node([("penrose", "She's cold. First time since September."),
                 ("penrose", "I'll write to the Commission myself in the morning. And I'll repaint that card.")], [choice("Goodnight, Mr. Penrose.")]),
}}
dialogues["dot"] = {"name": "Dot Kimball", "role": "Host, Halloway Today", "roots": [
    root("end", ["CASE_SOLVED"]), root("late", ["D3"]), root("d1", ["D1"]), root("small", ["MET_DOT"]), root("intro")], "nodes": {
    "intro": node([("dot", "Hi! You're from the government? Oh - the FCC. Dot Kimball. I do Halloway Today."),
                   ("dot", "I'm running lines for Thursday. Arthur's my guest. He's going to explain the whole sky to us.")],
                  [choice("Thursday?", "thursday", [{"set": "MET_DOT"}]), choice("Don't let me interrupt.", None, [{"set": "MET_DOT"}])]),
    "thursday": node([("dot", "Our first show in color! Walter says the whole country might be watching on Thursday night."),
                      ("dot", "I'll be in Studio A all evening. Somebody has to keep talking while the engineers sweat.")], [choice("Good luck.")]),
    "small": node([("dot", "Do I look all right? They say color cameras are much less forgiving.")], [choice("You look fine.")], "idle"),
    "d1": node([("dot", "Rosa says there's a picture of me on the monitors. From Thursday. That's a joke, right?"),
                ("player", "I don't think it's a joke."),
                ("dot", "Then I'd like to know how Thursday goes, please. All of it.")], [choice("Not yet.")], "nervous"),
    "late": node([("dot", "You've seen something. It's all over your face. Is it the tower?"),
                  ("dot", "If something happened on Thursday night... I'd stay on the air. I know I would.")], [choice("It won't come to that.")], "nervous"),
    "end": node([("dot", "Thursday's postponed! Walter's in a mood and Arthur's whistling. I'll take it."),
                 ("dot", "Come back for the Grange fair. The pie's on me.")], [choice("Goodnight, Dot.")]),
}}
dialogues["brandt"] = {"name": "Walter Brandt", "role": "Station Manager", "roots": [
    root("end", ["CASE_SOLVED"]), root("late", ["D3"]), root("d2", ["D2"]), root("small", ["MET_BRANDT"]), root("intro")], "nodes": {
    "intro": node([("brandt", "Walter Brandt, station manager. The Commission, at this hour! We run a clean shop, I promise you."),
                   ("brandt", "Anything you need, Rosa will help. Just keep the noise down near Studio A, would you?")],
                  [choice("Tell me about Thursday.", "thursday", [{"set": "MET_BRANDT"}]), choice("Of course.", None, [{"set": "MET_BRANDT"}])]),
    "thursday": node([("brandt", "The Futura demonstration. Coast to coast from little Halloway Falls! That contract keeps sixty people working."),
                      ("brandt", "Within our licence, naturally.")], [choice("Naturally.")]),
    "small": node([("brandt", "Still here? Coffee's in the lobby. It's terrible.")],
                  [choice("Tell me about Thursday.", "thursday"), choice("Futura's 'Model 63' was advertised on 7-B.", "dx", ifs=["DX"]), choice("Never mind.")], "idle"),
    "d2": node([("brandt", "Two hundred kilowatts. Look - Futura wanted a picture in Los Angeles, and Arthur said he could do it."),
                ("brandt", "I signed what he put in front of me. If that's a crime, it's the crime of keeping the lights on.")],
               [choice("And the Model 63?", "dx", ifs=["DX"]), choice("We'll talk later.")], "nervous"),
    "dx": node([("brandt", "The sixty-three? That's confidential until January. Their letter is in my cabinet. How in God's name -"),
                ("brandt", "If that goes out on my air, Futura walks. And then there is no WKVR.")], [choice("Then don't air it.")], "nervous"),
    "late": node([("brandt", "Arthur came to me white as a sheet. Says you know what happens on Thursday. Do I want to know?")], [choice("No. You don't.")], "nervous"),
    "end": node([("brandt", "No demonstration. Probably no Futura. A fine - and a station still standing."),
                 ("brandt", "...Thank you. Don't tell anybody I said that.")], [choice("Goodnight, Mr. Brandt.")]),
}}
for k, d in dialogues.items():
    for nid, n in d["nodes"].items():
        for l in n["lines"]:
            assert len(l["text"]) <= 140, (k, nid, len(l["text"]), l["text"])
        for c in n["choices"]:
            assert c["next"] is None or c["next"] in d["nodes"], (k, nid, c["next"])
            assert len(c["text"]) <= 64, (k, c["text"])
    for r in d["roots"]:
        assert r["node"] in d["nodes"]
    with open(f"{OUT}/dialogue/{k}.json", "w") as f:
        json.dump(d, f, indent=1)
print("content ok:", len(broadcasts), "broadcasts,", len(documents), "documents,", len(deductions), "deductions")
