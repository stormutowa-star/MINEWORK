# PROGRESS — The Last Broadcast

## Target
Minecraft 1.21.1 · Forge 52.1.20 (works on 52.x) · Java 21 · ForgeGradle 7 / Gradle 9.3.1 wrapper.

## What works (implemented and exercised in-game)
- **Station**: built automatically in front of spawn on first join (lobby, manager's office, tape
  library, master control, Studio A, transmitter hall, Skyhook tower, lot and pylon sign). Site is
  protected from survival breaking/placing and explosions; natural mob spawns are blocked on the grounds.
- **Broadcast framework**: JSON timelines (shots, camera moves, presenters with lip-flap/blinks,
  captions, transitions, faults: snow/roll/tear/ghost/flicker/hum bars, single-frame inserts).
  Rendered on in-world TV tubes (block entity renderer) and in GUIs; schedule derived from game time.
- **Channels**: 7 (WKVR rotation), 4 (distant KTRX), 7-B (ghost carrier, unlocked by the analyzer,
  rotation grows with the story), snow elsewhere; sign-off after the ending; 7-B dead carrier.
- **Analyzer puzzle** (frequency / phase null / H-hold) with live scope, meter and preview.
- **Tape deck**: record from nearest receiver, playback with reverse, half speed, x4, frame step;
  reverse reveals backwards speech; pausing on the insert frame logs evidence.
- **Case file**: evidence list, compare two items → deductions (4 key + 1 optional), current lead.
- **Documents** (9), **locker** combination lock, **telephone** fragments, **Skyhook console**
  shutdown puzzle with arc fault + relay reset, **ending** (sign-off broadcast + case report).
- **NPCs**: Rosa (seated), Brandt (seated), Penrose and Dot (walking routines), branching dialogue
  with flag conditions, gestures, lip movement and blinking.
- **Echo leaks** after D2: TV surges, light flicker, ringing phone, clocks stalled at 9:14, whispers.
- **Audio**: 51 original synthesized sound events (machinery, music beds, jingles, voice timbres).
- **Persistence**: case state in world SavedData; machines in block entities; NPCs persist.

## Verification (last run 2026-10-22)
- `./gradlew build` — BUILD SUCCESSFUL; jar `build/libs/TheLastBroadcast-1.0.0-forge-1.21.1.jar`.
- Dev dedicated server (`runServer`) — loads, builds station, commands work.
- **Production Forge 52.1.20 dedicated server** (installer) + jar — loads, builds station on hilly
  terrain, block loot drops fine, no errors.
- **Dev client under Xvfb** (`runClient -Pautotest`): scripted walkthrough with 22 screenshots
  (exterior, rooms, in-world TV pictures, all GUIs, dialogue, HUD, ending) and an end-to-end logic run:
  all 5 deductions → wrong shutdown arcs and resets → correct shutdown → CASE_SOLVED → ending screen.
- Not verified: audio playback (container has no audio device), multiplayer with 2+ clients.

## Known limitations
- Dev-environment only: in the ForgeGradle dev client, Forge never fires `AddReloadListenerEvent`, so
  vanilla loot (death drops, water breaking blocks) throws inside Forge. Reproduced with this mod made
  inert — not caused by the mod; production server is unaffected.
- Dialogue and broadcast speech are subtitled with synthesized voice timbres, not recorded voice acting.
- Presenter artwork is 2D illustrated sprites composited per shot (no 3D cinematography on screen).

## Next actions
- Optional: more 7-B programmes and a second location using the same systems.
