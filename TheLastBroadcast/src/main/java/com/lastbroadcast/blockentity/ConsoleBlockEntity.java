package com.lastbroadcast.blockentity;

import com.lastbroadcast.network.Net;
import com.lastbroadcast.network.NoticePacket;
import com.lastbroadcast.registry.ModBlockEntities;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.Flags;
import com.lastbroadcast.story.StoryDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Skyhook overdrive control console. Shutdown rules (consistent with the placard, the
 * transmitter log and the 9:14 bulletin):
 * <ul>
 *   <li>Coolant only flows through valve 5 (the overdrive manifold); valve 3 is dead-ended.</li>
 *   <li>Lowering drive without coolant flow arcs the final amplifier.</li>
 *   <li>Opening the plate breaker with drive above zero arcs.</li>
 *   <li>Grounding the antenna with the plate closed arcs.</li>
 * </ul>
 * An arc trips the overload relay, which restores the overdrive state after a few seconds —
 * the player can always try again.
 */
public class ConsoleBlockEntity extends SyncedBlockEntity {
    public static final int FLOW_VALVE = 4; // valve 5, zero-based
    public static final int MAX_DRIVE = 10;
    public static final int A_VALVE = 0, A_DRIVE_DOWN = 1, A_DRIVE_UP = 2, A_PLATE = 3, A_GROUND = 4;

    private int valves = 1 << 2; // valve 3 open, per Rev. 3 placard
    private int drive = MAX_DRIVE;
    private boolean plateClosed = true;
    private boolean grounded = false;
    private long faultUntil = 0L;
    private boolean shutDown = false;

    public ConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSOLE.get(), pos, state);
    }

    public int getValves() { return valves; }
    public int getDrive() { return drive; }
    public boolean isPlateClosed() { return plateClosed; }
    public boolean isGrounded() { return grounded; }
    public boolean isShutDown() { return shutDown; }
    public long getFaultUntil() { return faultUntil; }

    public boolean coolantFlowing() {
        return (valves & (1 << FLOW_VALVE)) != 0;
    }

    public void action(ServerPlayer player, int action, int index) {
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) return;
        if (!StoryDirector.has(level.getServer(), Flags.PENROSE_CONFESSED)) {
            Net.toPlayer(player, new NoticePacket(NoticePacket.INFO, "The console key switch is locked. Only the chief engineer has the key."));
            return;
        }
        long now = level.getGameTime();
        if (shutDown || now < faultUntil) return;
        switch (action) {
            case A_VALVE -> {
                if (index < 0 || index > 5) return;
                valves ^= (1 << index);
                level.playSound(null, worldPosition, ModSounds.VALVE.get(), SoundSource.BLOCKS, 0.8f, 0.9f + index * 0.04f);
            }
            case A_DRIVE_DOWN -> {
                if (drive <= 0) return;
                if (!coolantFlowing()) {
                    fault(level, player, "The final amplifier screams as the drive drops with no coolant behind it.");
                    return;
                }
                drive--;
                level.playSound(null, worldPosition, ModSounds.KNOB.get(), SoundSource.BLOCKS, 0.8f, 0.8f);
            }
            case A_DRIVE_UP -> {
                if (drive >= MAX_DRIVE || !plateClosed) return;
                drive++;
                level.playSound(null, worldPosition, ModSounds.KNOB.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            case A_PLATE -> {
                if (plateClosed) {
                    if (drive > 0) {
                        fault(level, player, "The plate breaker opens under load. A blue-white arc jumps the contacts.");
                        return;
                    }
                    plateClosed = false;
                } else {
                    if (grounded) return;
                    plateClosed = true;
                }
                level.playSound(null, worldPosition, ModSounds.BREAKER.get(), SoundSource.BLOCKS, 1f, 1f);
            }
            case A_GROUND -> {
                if (!grounded) {
                    if (plateClosed) {
                        fault(level, player, "Grounding a live antenna feed. The relay rack lights up like a flashbulb.");
                        return;
                    }
                    grounded = true;
                } else {
                    grounded = false;
                }
                level.playSound(null, worldPosition, ModSounds.BREAKER.get(), SoundSource.BLOCKS, 1f, 0.8f);
            }
            default -> {
                return;
            }
        }
        if (coolantFlowing() && drive == 0 && !plateClosed && grounded) {
            shutDown = true;
            level.playSound(null, worldPosition, ModSounds.POWER_DOWN.get(), SoundSource.BLOCKS, 1.4f, 1f);
            StoryDirector.solveCase(level);
        }
        sync();
    }

    private void fault(ServerLevel level, ServerPlayer player, String message) {
        faultUntil = level.getGameTime() + 70;
        level.playSound(null, worldPosition, ModSounds.ARC.get(), SoundSource.BLOCKS, 1.5f, 1f);
        level.playSound(null, worldPosition, ModSounds.ALARM.get(), SoundSource.BLOCKS, 1.2f, 1f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 40, 0.4, 0.3, 0.4, 0.4);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.01);
        StoryDirector.flickerLights(level, player);
        StoryDirector.setFlag(level.getServer(), Flags.ARC_FAULT_SEEN);
        Net.toPlayer(player, new NoticePacket(NoticePacket.WARNING, message + " The overload relay trips and slams everything back to overdrive."));
        sync();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConsoleBlockEntity be) {
        if (be.faultUntil != 0 && level.getGameTime() >= be.faultUntil) {
            be.faultUntil = 0;
            be.valves = 1 << 2;
            be.drive = MAX_DRIVE;
            be.plateClosed = true;
            be.grounded = false;
            be.sync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("valves", valves);
        tag.putInt("drive", drive);
        tag.putBoolean("plate", plateClosed);
        tag.putBoolean("ground", grounded);
        tag.putLong("fault", faultUntil);
        tag.putBoolean("done", shutDown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("valves")) {
            valves = tag.getInt("valves");
            drive = tag.getInt("drive");
            plateClosed = tag.getBoolean("plate");
            grounded = tag.getBoolean("ground");
            faultUntil = tag.getLong("fault");
            shutDown = tag.getBoolean("done");
        }
    }
}
