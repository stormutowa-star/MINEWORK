package com.lastbroadcast.blockentity;

import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.item.TapeItem;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.network.NoticePacket;
import com.lastbroadcast.registry.ModBlockEntities;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.CaseData;
import com.lastbroadcast.story.StoryDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reel-to-reel deck wired to the monitor bus. Recording captures the programme on the nearest
 * powered receiver (the station's loop buffer holds the whole segment), taking a few seconds.
 */
public class RecorderBlockEntity extends SyncedBlockEntity {
    public static final int RECORD_TICKS = 70;
    public static final double BUS_RANGE = 10.0;

    private ItemStack tape = ItemStack.EMPTY;
    private boolean recording = false;
    private long recordEnd = 0L;
    private String recordingId = "";

    public RecorderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECORDER.get(), pos, state);
    }

    public ItemStack getTape() {
        return tape;
    }

    public boolean isRecording() {
        return recording;
    }

    public long getRecordEnd() {
        return recordEnd;
    }

    public void insert(ItemStack stack) {
        tape = stack.copyWithCount(1);
        sync();
    }

    public ItemStack eject() {
        ItemStack out = tape;
        tape = ItemStack.EMPTY;
        recording = false;
        sync();
        return out;
    }

    public void dropContents(Level level) {
        if (!tape.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, tape);
        tape = ItemStack.EMPTY;
    }

    /** Server: start recording from the monitor bus. */
    public void startRecording(ServerPlayer player) {
        if (tape.isEmpty() || recording) return;
        if (!TapeItem.isBlank(tape)) {
            Net.toPlayer(player, new NoticePacket(NoticePacket.INFO, com.lastbroadcast.Tx.key("lastbroadcast.msg.reel_recorded")));
            return;
        }
        ServerLevel level = (ServerLevel) this.level;
        TelevisionBlockEntity source = nearestPoweredSet(level);
        if (source == null) {
            Net.toPlayer(player, new NoticePacket(NoticePacket.INFO, com.lastbroadcast.Tx.key("lastbroadcast.msg.bus_dead")));
            return;
        }
        CaseData data = CaseData.get(level.getServer());
        long now = level.getGameTime();
        int channel = source.isSurging(now) ? Channels.CH_7B : source.getChannel();
        Channels.OnAir onAir = Channels.onAir(data.state, channel, now);
        Broadcast b = onAir.programme();
        if (b == null || b.unrecordable) {
            Net.toPlayer(player, new NoticePacket(NoticePacket.INFO, com.lastbroadcast.Tx.key("lastbroadcast.msg.no_picture", Channels.label(channel))));
            return;
        }
        recording = true;
        recordingId = b.id;
        recordEnd = now + RECORD_TICKS;
        level.playSound(null, worldPosition, ModSounds.TAPE_REC.get(), SoundSource.BLOCKS, 0.8f, 1f);
        sync();
    }

    private TelevisionBlockEntity nearestPoweredSet(ServerLevel level) {
        TelevisionBlockEntity best = null;
        double bestD = BUS_RANGE * BUS_RANGE;
        for (TelevisionBlockEntity tv : TelevisionBlockEntity.serverSets()) {
            if (tv.getLevel() != level || !tv.isPowered()) continue;
            double d = tv.getBlockPos().distSqr(worldPosition);
            if (d <= bestD) {
                bestD = d;
                best = tv;
            }
        }
        return best;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RecorderBlockEntity be) {
        if (!be.recording || level.getGameTime() < be.recordEnd) return;
        be.recording = false;
        Broadcast b = com.lastbroadcast.content.ContentLibrary.broadcast(be.recordingId);
        if (b != null && !be.tape.isEmpty()) {
            be.tape = TapeItem.recorded(b.id, b.title);
            StoryDirector.logEvidence(((ServerLevel) level).getServer(), "tape:" + b.id);
        }
        level.playSound(null, pos, ModSounds.TAPE_STOP.get(), SoundSource.BLOCKS, 0.8f, 1f);
        be.sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!tape.isEmpty()) tag.put("tape", tape.save(registries));
        tag.putBoolean("recording", recording);
        tag.putLong("recordEnd", recordEnd);
        tag.putString("recordingId", recordingId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tape = tag.contains("tape") ? ItemStack.parseOptional(registries, tag.getCompound("tape")) : ItemStack.EMPTY;
        recording = tag.getBoolean("recording");
        recordEnd = tag.getLong("recordEnd");
        recordingId = tag.getString("recordingId");
    }
}
