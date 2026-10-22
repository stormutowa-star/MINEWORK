# THE LAST BROADCAST — Internal Narrative Specification

> DEVELOPER-ONLY. Contains every spoiler. Never ship this text in player-facing material.

## 1. Underlying explanation

WKVR-TV Channel 7 (Halloway Falls, Ohio) transmits from the **Skyhook**, a resonant
lattice tower designed by chief engineer **Arthur Penrose** (ex-Bell Labs). Run at its
licensed 50 kW it is an ordinary transmitter. Run in **overdrive** (~200 kW, a manifold
Penrose added in August 1962) the array pumps the ionospheric F-layer into a resonance
Penrose calls *the mirror*. Signals reflected by the mirror return with **negative group
delay**.

### Rules of the phenomenon (never broken)
1. The echo only ever carries **WKVR's own transmissions**.
2. An echo is only received **while the Skyhook is in overdrive**.
3. Every overdrive run receives the transmissions of the **last** overdrive run the Skyhook will ever make.
4. The echo arrives on Channel 7's own carrier (175.25 MHz), **phase-inverted** (180°),
   with a slightly slow line rate — it hides underneath the station's own picture.
5. The echo leaks into any circuit tied to the station's monitor bus: monitors, the
   tape loop buffer, and the master-control telephone line.
6. The echo is never wrong about what WKVR *will* transmit — unless the people who will
   transmit it learn what it says. (Knowledge is the only thing that changes it.)
7. Once the last run has ended there is nothing left to receive: dead carrier.

### Why 7-B shows Thursday
Overdrive runs have happened every Tuesday night since 18 September (runs 1–5). Tonight,
Tuesday 23 October 1962, is run 6, a pre-demonstration test. The last run — in the original
timeline — is run 7, the **24-hour Futura demonstration** from 06:00 Thursday 25 October. So
every echo, including tonight's, is Thursday's programming: the morning show, the contest, the evening report, and finally
the 9:14 PM emergency bulletin — the last thing the Skyhook will ever transmit.

### What happens Thursday (original timeline)
At 21:10 the overdrive amplifier overheats. Penrose runs the posted *Emergency Shutdown
Procedure Rev. 3*, which says to open **coolant valve 3**. But in October Penrose himself
re-routed the coolant feed to **valve 5** for the overdrive manifold (transmitter log, run
4, 9 October) and never revised the placard. Valve 3 is dead-ended; no coolant flows; when he cuts the
drive the plate arcs over. Fire in the transmitter hall. Penrose dies. Dot Kimball stays on
air in Studio A until the Skyhook fails at 21:14. That bulletin is 7-B's final program.

### The resolution
The player proves the overdrive is real, unlicensed, and the source of the ghosts, then
performs a correct shutdown tonight (valve 5 → drive 0 → plate open → ground). Penrose
withdraws the demonstration. Tonight's run becomes the last run and has just ended, so 7-B
goes to dead carrier (Rule 7). Channel 7 signs off normally. The future is unwritten again.

## 2. Chronology
- 1957 — Penrose leaves Bell Labs, joins WKVR as chief engineer.
- 1959 — Skyhook tower built (licensed 50 kW visual ERP).
- Mar 1962 — Futura Corporation (appliances, "Homes of Tomorrow") courts WKVR as flagship
  sponsor; wants a "single-hop coast-to-coast" demonstration and color telecasting.
- Jun 1962 — Shutdown Procedure Rev. 3 posted.
- Aug 1962 — Penrose builds the overdrive manifold.
- 9 Oct — Run 4: Penrose moves the coolant feed to valve 5 (log note), never revises the placard.
- 18 Sep — Run 1. First ghost reports from viewers (Tuesdays only).
- 2 Oct — Run 3: Penrose first notices the ghost.
- 16 Oct — Run 5. Penrose sees Thursday's programming on the monitor; records a private
  memo, reel threaded backwards for privacy ("A.P. — DO NOT ERASE").
- 19 Oct — Rosa Delgado files an anonymous interference complaint with the FCC.
- 22 Oct — FCC assigns the complaint to the player.
- 23 Oct (Tue night) — Game begins. Run 6 in progress.
- 24 Oct — Futura color cameras arrive (so Thursday's programs are in color; Tuesday's are not).
- 25 Oct — Demonstration; fire at 21:14 (original timeline only).

## 3. Characters
- **The player** — Field Engineer, FCC Detroit District, Field Engineering & Monitoring
  Bureau. Complaint #62-1147. Professional, technical, outsider. Motive: do the job right.
- **Rosa Delgado** (34) — night master-control operator; ex-Women's Army Corps radio
  operator. Filed the complaint anonymously. Precise, dry, afraid for her job. Wants the
  truth on the record. Secret: she filed the complaint.
- **Arthur Penrose** (51) — chief engineer. Courtly, brilliant, evasive. Secret: runs the
  overdrive; has watched the echo for a week; fascinated, will not stop. He has never seen
  the 9:14 bulletin (it only exists on tonight's echo, after the evening report).
  Conflict: wants the demonstration; fears losing the Skyhook more than danger.
- **Walter Brandt** (46) — station manager. Folksy salesman. Secret: signed the overdrive
  authorization because the Futura contract keeps WKVR alive. Conflict: money vs. license.
- **Dorothy "Dot" Kimball** (29) — host of *Halloway Today*. Warm, sharp, curious.
  Unaware. In the original timeline she is the voice of the last broadcast.
- **Hal Whitcomb** — news anchor (on-screen only). **Servo** — Futura's robot mascot.

## 4. Investigation and clue dependency graph
```
START (assignment letter: license 50 kW)
  └─ talk Rosa → MET_ROSA, complaint log (Tuesdays), tuning tips
       └─ Signal Analyzer: 175.25 MHz / 180° / H-hold 37 → SIGNAL_LOCKED → 7-B: P1, P2
            ├─ D1 "Two Days Early": tape P1 + program log (alt: tape P1 + tape Tuesday show)
            │     └─ unlock P3
            │          └─ D2 "Over the Limit": tape P3 + assignment letter (alt: + license in log)
            │                └─ horror director on; Rosa gives unlabeled reel (GOT_REEL)
            │                     └─ reel reversed → combination = demo date 1025
            │                          └─ locker → transmitter log
            │                               └─ D3 "Ghosts Keep a Schedule": transmitter log + complaint log
            │                                    └─ unlock P4; Penrose confesses → console unlocked
            │                                         └─ step-frame P4 → frame evidence (valve 3 open)
            │                                              └─ D4 "The Wrong Valve": frame + procedure (alt: frame + transmitter log)
            │                                                   └─ console: valve 5 → drive 0 → plate → ground → ENDING
            └─ optional DX "Next Year's Model": tape P2 + tape Futura ad (alt: tape P2 + Futura letter)
```
The console can be solved without D4 by players who read the transmitter log; failure only
trips the overload relay and resets (no permanent loss). Every document can be re-taken
from its cabinet; blank tape is unlimited (tape stock cabinet, Rosa).

## 5. Broadcast catalog
Normal Channel 7 (black & white): ident, *Halloway Today* (Tue 23), Futura Kitchen Model 62
ad, *Channel 7 Report* (Tue), test card. Channel 4: distant KTRX test card, weak.
Phantom 7-B (color): P1 *Halloway Today* (Thu 25, guest Penrose), P2 *Lucky Dial Jackpot*
+ Futura Model 63 ad, P3 *Channel 7 Report* (Thu 6 PM, "200 kilowatts tonight"),
P4 *Special Bulletin* 9:14 PM (one-frame insert of the console with valve 3 open).
Ending: WKVR sign-off (Tue night, normal).

## 6. Apparent contradictions and their logic
- Color pictures on a black & white station → Thursday, after color cameras arrive.
- A Thursday show that hasn't been taped → echo of the last overdrive run (Thursday).
- "200 kilowatts" → overdrive is real and unlicensed.
- Ghosts only on Tuesdays → overdrive only on Tuesdays (runs 1–7).
- Model 63 nobody has announced → Futura's confidential January launch, aired Thursday.
- Valve 3 open during the fire → placard outdated; feed moved to valve 5.

## 7. Horror progression
1. Calm: ordinary station at night, hum, monitors. Only the ghost is wrong.
2. After D1: the echo is recognized as tomorrow; 7-B gets colder (more ghosting).
3. After D2: the director starts — monitors surge to 7-B for seconds, ceiling lights
   flicker, the master-control phone rings with fragments of the 9:14 bulletin, clocks
   stall at 9:14. All are leaks of the echo (Rule 5); none are attacks.
4. After D3: events more frequent; tower beacon races; P4 exists.
5. Ending: silence; dead carrier on 7-B; a normal sign-off.

## 8. Milestones / ending conditions
Flags: MET_ROSA, SIGNAL_LOCKED, D1, D2, GOT_REEL, D3, PENROSE_CONFESSED, FRAME_FOUND, D4, DX,
CASE_SOLVED. Ending = correct console shutdown while PENROSE_CONFESSED.
