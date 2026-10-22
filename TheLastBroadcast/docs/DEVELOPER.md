# Developer guide

Spoilers: the story bible lives in `docs/INTERNAL_NARRATIVE.md` (developer-only).

## Architecture

| Area | Package / files |
|---|---|
| Registration | `registry/` (blocks, items, block entities, entities, sounds, creative tab) |
| Blocks & machines | `block/`, `blockentity/` |
| Items | `item/` (tape, document, case file, field meter) |
| Story state & persistence | `story/CaseData` (SavedData on the overworld), `story/CaseState` (synced copy), `story/StoryDirector` (milestones, evidence, deductions, echo-leak events, ending), `story/DialogueLogic`, `story/Leads` |
| Broadcast schedule | `broadcast/Channels` — programme on any channel is a pure function of game time + case state, so every set agrees and playback survives save/reload |
| Content | `content/ContentLibrary` loads `assets/lastbroadcast/content/**` on both sides |
| NPCs | `entity/StaffNpc` (seated or routine-walking staff, conversations) |
| World | `world/StationBuilder` (curated station + Skyhook tower), `world/StationEvents` (site protection) |
| Networking | `network/` — every client action is re-validated on the server |
| Client | `client/render` (atlas canvas, broadcast renderer, TV/deck/clock BERs, NPC model), `client/screen` (all GUIs), `client/sound` (positional programme audio, ambience), `client/hud` |

Client-only classes are reached from common code only through `ClientBridge` or packet
handlers, so the dedicated server never loads them (verified by running `runServer`).

## Adding a broadcast

1. Add a definition in `tools/gen_content.py` with `bc(...)`, or hand-write
   `assets/lastbroadcast/content/broadcasts/<id>.json` and list it in `content/index.json`.
2. A broadcast is a timeline (seconds): `shots` (background, camera crop and move, actors,
   captions, effects, transition), `lines` (subtitled speech; speaker ids from `speakers`),
   `sounds` (cue id from `ModSounds`), optional `flash` (single-frame insert that becomes
   evidence when the tape is paused on it).
3. Scene coordinates are 160x120. Actor `x,y` is the bottom centre; `h` the height.
4. Effects (`fx`): `stat` snow, `roll` vertical roll, `tear` line tearing, `ghost`, `flicker`,
   `bars` hum bars, `vignette`, `shake`. `mono: true` uses the `_mono` art variants.
5. Put it on the air: channel 7 rotation is `Channels.CH7_LOOP`; the phantom 7-B rotation is
   built in `StoryDirector.rebuildPhantomPlan` from story flags.
6. New artwork: add a painter to `tools/gen_broadcast_art.py` and include it in `bgs`.

## Adding evidence and deductions

- Documents: add to `documents` in `gen_content.py` (pages < 470 chars each), then place the
  token `doc:<id>` in a cabinet via `StationBuilder.holder(...)` or give it in dialogue.
- Recorded tapes become evidence `tape:<broadcastId>` automatically.
- Deductions: `{id, title, a:[...], b:[...], requires:[flags], text, optional}`. Comparing any
  item of `a` with any item of `b` sets the flag named by `id`.

## Adding a character

1. Dialogue tree in `gen_content.py` (`roots` pick the entry node by flags; choices may
   `set` flags, `give` items, log `evidence`) and list it in `index.json` `dialogues`.
2. Skin: add a `skin(...)` call in `gen_block_assets.py` (64x64 player layout; the mouth and
   eyelid parts read texture rows 0 at u 0-7 and 24-35).
3. Spawn: `StaffNpc.create(level, id, home, yaw, sitting, route, routeYaw)` in
   `StationBuilder.spawnStaff`.

## Adding a location

`StationBuilder` places everything explicitly in local coordinates (x east, z south, floor at
y=0). Add a `buildX()` method, extend the bounds in `build()` so the site stays protected.

## Testing

- `./gradlew runServer` then `/lastbroadcast station|status|flag <F>|evidence <id>|reset`.
- Automated visual walkthrough: copy a world into `run/saves/autotest`, then
  `xvfb-run ./gradlew runClient -Pautotest` — screenshots land in `run/screenshots/`.
