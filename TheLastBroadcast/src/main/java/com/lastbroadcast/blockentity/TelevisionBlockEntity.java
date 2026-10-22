package com.lastbroadcast.blockentity;

import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** A receiver: power state and tuned channel. The picture itself is derived from the schedule. */
public class TelevisionBlockEntity extends SyncedBlockEntity {
    private static final Set<TelevisionBlockEntity> SERVER_SETS = new LinkedHashSet<>();
    private static final Set<TelevisionBlockEntity> CLIENT_SETS = new LinkedHashSet<>();

    private boolean powered = false;
    private int channel = Channels.CH_7;
    /** Game time until which an echo leak forces the picture to channel 7-B. */
    private long surgeUntil = 0L;
    /** Client only: game time of the last power change, for CRT warm-up/collapse animation. */
    public long clientPowerChange = -100L;

    public TelevisionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEVISION.get(), pos, state);
    }

    public static Set<TelevisionBlockEntity> serverSets() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(SERVER_SETS));
    }

    public static Set<TelevisionBlockEntity> clientSets() {
        return CLIENT_SETS;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) (level.isClientSide ? CLIENT_SETS : SERVER_SETS).add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        SERVER_SETS.remove(this);
        CLIENT_SETS.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        SERVER_SETS.remove(this);
        CLIENT_SETS.remove(this);
    }

    public boolean isPowered() {
        return powered;
    }

    public int getChannel() {
        return channel;
    }

    public boolean isSurging(long gameTime) {
        return gameTime < surgeUntil;
    }

    public long getSurgeUntil() {
        return surgeUntil;
    }

    public void setState(boolean powered, int channel) {
        this.powered = powered;
        this.channel = Math.max(0, Math.min(Channels.DIAL.length - 1, channel));
        sync();
    }

    public void surge(long until) {
        this.surgeUntil = until;
        sync();
    }

    public void tuneForEnding() {
        this.powered = true;
        this.channel = Channels.CH_7;
        this.surgeUntil = 0;
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("powered", powered);
        tag.putInt("channel", channel);
        tag.putLong("surge", surgeUntil);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        boolean was = powered;
        powered = tag.getBoolean("powered");
        if (level != null && level.isClientSide && was != powered) clientPowerChange = level.getGameTime();
        channel = tag.contains("channel") ? tag.getInt("channel") : Channels.CH_7;
        surgeUntil = tag.getLong("surge");
    }
}
