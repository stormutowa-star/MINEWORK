package com.lastbroadcast.story;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

/** Persistent, world-wide investigation record (stored with the overworld). */
public class CaseData extends SavedData {
    private static final String NAME = "lastbroadcast_case";

    public final CaseState state = new CaseState();

    public boolean stationBuilt = false;
    public BlockPos origin = BlockPos.ZERO;
    public BlockPos entrance = BlockPos.ZERO;
    public BlockPos towerTop = BlockPos.ZERO;
    public BlockPos consolePos = BlockPos.ZERO;
    public BlockPos phonePos = BlockPos.ZERO;
    public AABB bounds = new AABB(0, 0, 0, 0, 0, 0);
    public long endingStartedAt = -1L;
    public boolean endingScreenSent = false;

    public static CaseData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CaseData::new, CaseData::load, null), NAME);
    }

    private static CaseData load(CompoundTag tag, HolderLookup.Provider provider) {
        CaseData d = new CaseData();
        d.state.load(tag.getCompound("case"));
        d.stationBuilt = tag.getBoolean("built");
        d.origin = readPos(tag, "origin");
        d.entrance = readPos(tag, "entrance");
        d.towerTop = readPos(tag, "tower");
        d.consolePos = readPos(tag, "console");
        d.phonePos = readPos(tag, "phone");
        BlockPos a = readPos(tag, "boundsMin");
        BlockPos b = readPos(tag, "boundsMax");
        d.bounds = new AABB(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        d.endingStartedAt = tag.contains("ending") ? tag.getLong("ending") : -1L;
        d.endingScreenSent = tag.getBoolean("endingSent");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("case", state.save(new CompoundTag()));
        tag.putBoolean("built", stationBuilt);
        tag.put("origin", NbtUtils.writeBlockPos(origin));
        tag.put("entrance", NbtUtils.writeBlockPos(entrance));
        tag.put("tower", NbtUtils.writeBlockPos(towerTop));
        tag.put("console", NbtUtils.writeBlockPos(consolePos));
        tag.put("phone", NbtUtils.writeBlockPos(phonePos));
        tag.put("boundsMin", NbtUtils.writeBlockPos(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ)));
        tag.put("boundsMax", NbtUtils.writeBlockPos(BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)));
        tag.putLong("ending", endingStartedAt);
        tag.putBoolean("endingSent", endingScreenSent);
        return tag;
    }

    private static BlockPos readPos(CompoundTag tag, String key) {
        return NbtUtils.readBlockPos(tag, key).orElse(BlockPos.ZERO);
    }

    /** Copies station geometry into the synced state. */
    public void publishStation() {
        state.stationBuilt = stationBuilt;
        state.station = new int[]{(int) bounds.minX, (int) bounds.minY, (int) bounds.minZ, (int) bounds.maxX, (int) bounds.maxY, (int) bounds.maxZ};
        state.consolePos = consolePos.asLong();
    }

    public boolean inStation(BlockPos pos) {
        return stationBuilt && bounds.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public boolean nearStation(double x, double y, double z, double margin) {
        return stationBuilt && bounds.inflate(margin).contains(x, y, z);
    }
}
