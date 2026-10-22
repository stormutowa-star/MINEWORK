package com.lastbroadcast.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Positional looping sound that can be faded, re-pitched and stopped from code. */
public class LoopSound extends AbstractTickableSoundInstance {
    private float targetVolume;
    private boolean stopping = false;

    public LoopSound(SoundEvent event, SoundSource source, Vec3 at, float volume, float pitch) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
        this.volume = 0.01f;
        this.targetVolume = volume;
        this.pitch = pitch;
        this.attenuation = Attenuation.LINEAR;
    }

    public void setTargetVolume(float v) {
        targetVolume = v;
    }

    public void setPitch(float p) {
        pitch = p;
    }

    public void moveTo(Vec3 at) {
        x = at.x;
        y = at.y;
        z = at.z;
    }

    public void stopNow() {
        stopping = true;
        stop();
    }

    public void fadeOut() {
        stopping = true;
    }

    @Override
    public void tick() {
        float goal = stopping ? 0f : targetVolume;
        volume += (goal - volume) * 0.25f;
        if (stopping && volume < 0.02f) stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
