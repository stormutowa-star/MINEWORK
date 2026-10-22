package com.lastbroadcast.client.sound;

import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.CaseState;
import com.lastbroadcast.story.Flags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Client audio director. Gives the two nearest powered receivers their programme sound
 * (timed cues, presenter voices, snow), and runs the station's ambience: room tone inside the
 * building and the overdrive amplifier's hum near the transmitter hall.
 */
public final class BroadcastAudio {
    private static final int MAX_ACTIVE_SETS = 2;
    private static final double RANGE = 20.0;

    private static final class SetState {
        String programme = "";
        double lastT = -1;
        double nextBlip = 0;
        LoopSound snow;
        boolean surging;
    }

    private static final Map<BlockPos, SetState> states = new HashMap<>();
    private static LoopSound ambience, transmitter;
    private static final RandomSource RANDOM = RandomSource.create();

    private BroadcastAudio() {}

    public static void playAt(String soundId, Vec3 at, float volume, float pitch) {
        SoundEvent ev = ModSounds.byName(soundId);
        if (ev == null) return;
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(ev, SoundSource.RECORDS, volume,
                Math.max(0.5f, Math.min(2f, pitch)), RANDOM, at.x, at.y, at.z));
    }

    public static void playUi(String soundId, float volume, float pitch) {
        SoundEvent ev = ModSounds.byName(soundId);
        if (ev != null) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, volume));
    }

    public static void reset() {
        for (SetState s : states.values()) if (s.snow != null) s.snow.stopNow();
        states.clear();
        if (ambience != null) ambience.stopNow();
        if (transmitter != null) transmitter.stopNow();
        ambience = transmitter = null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!states.isEmpty() || ambience != null) reset();
            return;
        }
        Vec3 me = mc.player.position();
        long gt = mc.level.getGameTime();
        double time = gt;

        // choose the nearest powered sets
        List<TelevisionBlockEntity> sets = new ArrayList<>();
        for (TelevisionBlockEntity tv : TelevisionBlockEntity.clientSets()) {
            if (tv.isRemoved() || tv.getLevel() != mc.level) continue;
            if (!(tv.isPowered() || tv.isSurging(gt))) continue;
            if (tv.getBlockPos().getCenter().distanceToSqr(me) > RANGE * RANGE) continue;
            sets.add(tv);
        }
        sets.sort(Comparator.comparingDouble(t -> t.getBlockPos().getCenter().distanceToSqr(me)));
        Set<BlockPos> active = new HashSet<>();
        for (int i = 0; i < Math.min(MAX_ACTIVE_SETS, sets.size()); i++) {
            TelevisionBlockEntity tv = sets.get(i);
            active.add(tv.getBlockPos());
            tickSet(tv, time, gt);
        }
        Iterator<Map.Entry<BlockPos, SetState>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (!active.contains(e.getKey())) {
                if (e.getValue().snow != null) e.getValue().snow.fadeOut();
                it.remove();
            }
        }
        tickAmbience(mc, me);
    }

    private static void tickSet(TelevisionBlockEntity tv, double time, long gt) {
        SetState st = states.computeIfAbsent(tv.getBlockPos().immutable(), k -> new SetState());
        Vec3 at = tv.getBlockPos().getCenter();
        boolean surging = tv.isSurging(gt);
        if (surging && !st.surging) {
            playAt("static_burst", at, 1.0f, 0.9f);
            playAt("anomaly_tone", at, 0.8f, 1f);
        }
        st.surging = surging;
        Broadcast b;
        double t;
        if (surging) {
            b = null;
            t = 0;
        } else {
            Channels.OnAir onAir = Channels.onAir(CaseState.CLIENT, tv.getChannel(), time);
            b = onAir.programme();
            t = onAir.time();
        }
        boolean snowy = b == null || "static_gap".equals(b.id);
        // snow loop
        if (snowy) {
            if (st.snow == null || st.snow.isStopped()) {
                st.snow = new LoopSound(ModSounds.TV_STATIC.get(), SoundSource.RECORDS, at, surging ? 0.9f : 0.45f, 1f);
                Minecraft.getInstance().getSoundManager().play(st.snow);
            }
        } else if (st.snow != null) {
            st.snow.fadeOut();
            st.snow = null;
        }
        if (b == null) {
            st.programme = "";
            st.lastT = -1;
            return;
        }
        if (!b.id.equals(st.programme) || t < st.lastT || t - st.lastT > 2.0) {
            // tuned in mid-programme: start cleanly from now, but catch an opening cue
            st.programme = b.id;
            st.lastT = t < 0.3 ? -0.01 : t;
        }
        for (Broadcast.SoundCue cue : b.sounds) {
            if (cue.t > st.lastT && cue.t <= t) playAt(cue.id, at, cue.vol, cue.pitch);
        }
        Broadcast.Line line = b.lineAt(t);
        if (line != null && line.who != null && t < line.t + line.d - 0.2) {
            double wall = System.nanoTime() / 1e9;
            if (wall >= st.nextBlip) {
                ContentLibrary.Speaker sp = ContentLibrary.speaker(line.who);
                String voice = line.reversedSpeech ? "voice_reverse" : sp.voice;
                playAt(voice, at, 0.5f, sp.pitch * (0.9f + RANDOM.nextFloat() * 0.2f));
                st.nextBlip = wall + 0.1 + RANDOM.nextFloat() * 0.08;
            }
        }
        st.lastT = t;
    }

    private static void tickAmbience(Minecraft mc, Vec3 me) {
        CaseState cs = CaseState.CLIENT;
        boolean inside = cs.inStation(me.x, me.y, me.z, 0);
        if (inside && (ambience == null || ambience.isStopped())) {
            ambience = new LoopSound(ModSounds.AMBIENCE_STATION.get(), SoundSource.AMBIENT, me, 0.35f, 1f);
            ambience.setTargetVolume(0.35f);
            mc.getSoundManager().play(ambience);
        }
        if (ambience != null) {
            ambience.moveTo(me);
            if (!inside) {
                ambience.fadeOut();
                ambience = null;
            }
        }
        BlockPos console = BlockPos.of(cs.consolePos);
        boolean overdrive = cs.stationBuilt && !cs.has(Flags.CASE_SOLVED);
        double d2 = console.getCenter().distanceToSqr(me);
        if (overdrive && d2 < 26 * 26) {
            if (transmitter == null || transmitter.isStopped()) {
                transmitter = new LoopSound(ModSounds.TRANSMITTER_HUM.get(), SoundSource.BLOCKS, console.getCenter(), 0.8f, 1f);
                mc.getSoundManager().play(transmitter);
            }
            transmitter.setTargetVolume(cs.has(Flags.D3) ? 1.0f : 0.7f);
            transmitter.setPitch(cs.has(Flags.D3) ? 1.06f : 1f);
        } else if (transmitter != null) {
            transmitter.fadeOut();
            transmitter = null;
        }
    }
}
