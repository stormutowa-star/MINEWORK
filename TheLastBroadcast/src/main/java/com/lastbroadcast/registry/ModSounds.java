package com.lastbroadcast.registry;

import com.lastbroadcast.LastBroadcast;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LastBroadcast.MODID);
    private static final Map<String, RegistryObject<SoundEvent>> BY_NAME = new LinkedHashMap<>();

    // Machinery and interface
    public static final RegistryObject<SoundEvent> TV_ON = reg("tv_on");
    public static final RegistryObject<SoundEvent> TV_OFF = reg("tv_off");
    public static final RegistryObject<SoundEvent> TV_STATIC = reg("tv_static");
    public static final RegistryObject<SoundEvent> CRT_HUM = reg("crt_hum");
    public static final RegistryObject<SoundEvent> CHANNEL_CLICK = reg("channel_click");
    public static final RegistryObject<SoundEvent> KNOB = reg("knob");
    public static final RegistryObject<SoundEvent> TAPE_INSERT = reg("tape_insert");
    public static final RegistryObject<SoundEvent> TAPE_EJECT = reg("tape_eject");
    public static final RegistryObject<SoundEvent> TAPE_RUN = reg("tape_run");
    public static final RegistryObject<SoundEvent> TAPE_REC = reg("tape_rec");
    public static final RegistryObject<SoundEvent> TAPE_STOP = reg("tape_stop");
    public static final RegistryObject<SoundEvent> TAPE_REWIND = reg("tape_rewind");
    public static final RegistryObject<SoundEvent> RELAY = reg("relay");
    public static final RegistryObject<SoundEvent> BREAKER = reg("breaker");
    public static final RegistryObject<SoundEvent> VALVE = reg("valve");
    public static final RegistryObject<SoundEvent> ARC = reg("arc");
    public static final RegistryObject<SoundEvent> ALARM = reg("alarm");
    public static final RegistryObject<SoundEvent> POWER_DOWN = reg("power_down");
    public static final RegistryObject<SoundEvent> PHONE_RING = reg("phone_ring");
    public static final RegistryObject<SoundEvent> PHONE_PICKUP = reg("phone_pickup");
    public static final RegistryObject<SoundEvent> DIAL_TONE = reg("dial_tone");
    public static final RegistryObject<SoundEvent> TRANSMITTER_HUM = reg("transmitter_hum");
    public static final RegistryObject<SoundEvent> AMBIENCE_STATION = reg("ambience_station");
    public static final RegistryObject<SoundEvent> ANOMALY_WHISPER = reg("anomaly_whisper");
    public static final RegistryObject<SoundEvent> ANOMALY_TONE = reg("anomaly_tone");
    public static final RegistryObject<SoundEvent> ELECTRIC_BUZZ = reg("electric_buzz");
    public static final RegistryObject<SoundEvent> DEDUCTION = reg("deduction");
    public static final RegistryObject<SoundEvent> EVIDENCE = reg("evidence");
    public static final RegistryObject<SoundEvent> PAPER = reg("paper");
    public static final RegistryObject<SoundEvent> LOCKER = reg("locker");
    public static final RegistryObject<SoundEvent> DENIED = reg("denied");
    public static final RegistryObject<SoundEvent> METER_CLICK = reg("meter_click");
    public static final RegistryObject<SoundEvent> TYPEWRITER = reg("typewriter");
    public static final RegistryObject<SoundEvent> STATIC_BURST = reg("static_burst");
    // Broadcast music and beds
    public static final RegistryObject<SoundEvent> JINGLE_WKVR = reg("jingle_wkvr");
    public static final RegistryObject<SoundEvent> JINGLE_FUTURA = reg("jingle_futura");
    public static final RegistryObject<SoundEvent> MUSIC_MORNING = reg("music_morning");
    public static final RegistryObject<SoundEvent> MUSIC_NEWS = reg("music_news");
    public static final RegistryObject<SoundEvent> MUSIC_CONTEST = reg("music_contest");
    public static final RegistryObject<SoundEvent> MUSIC_SIGNOFF = reg("music_signoff");
    public static final RegistryObject<SoundEvent> MUSIC_BULLETIN = reg("music_bulletin");
    public static final RegistryObject<SoundEvent> TEST_TONE = reg("test_tone");
    public static final RegistryObject<SoundEvent> APPLAUSE = reg("applause");
    public static final RegistryObject<SoundEvent> FIRE_CRACKLE = reg("fire_crackle");
    // Voices (syllable timbres; dialogue text is subtitled)
    public static final RegistryObject<SoundEvent> VOICE_DOT = reg("voice_dot");
    public static final RegistryObject<SoundEvent> VOICE_HAL = reg("voice_hal");
    public static final RegistryObject<SoundEvent> VOICE_PENROSE = reg("voice_penrose");
    public static final RegistryObject<SoundEvent> VOICE_ROSA = reg("voice_rosa");
    public static final RegistryObject<SoundEvent> VOICE_BRANDT = reg("voice_brandt");
    public static final RegistryObject<SoundEvent> VOICE_SERVO = reg("voice_servo");
    public static final RegistryObject<SoundEvent> VOICE_REVERSE = reg("voice_reverse");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> reg(String name) {
        RegistryObject<SoundEvent> ro = SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(LastBroadcast.id(name)));
        BY_NAME.put(name, ro);
        return ro;
    }

    /** Sound referenced by name from story content; null when unknown. */
    public static SoundEvent byName(String name) {
        RegistryObject<SoundEvent> ro = name == null ? null : BY_NAME.get(name);
        return ro != null && ro.isPresent() ? ro.get() : null;
    }

    public static Iterable<String> names() {
        return BY_NAME.keySet();
    }
}
