package com.lastbroadcast.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

final class PacketUtil {
    private PacketUtil() {}

    /** Player is close enough to operate the block at pos (and the chunk is loaded). */
    static boolean canReach(ServerPlayer player, BlockPos pos) {
        return player != null && player.level().isLoaded(pos) && player.distanceToSqr(pos.getCenter()) <= 8.5 * 8.5;
    }
}
