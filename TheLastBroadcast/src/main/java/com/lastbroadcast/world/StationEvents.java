package com.lastbroadcast.world;

import com.lastbroadcast.story.CaseData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * The station is a federal investigation site: survival players cannot break or blast it apart,
 * so no ordinary mistake can destroy evidence or story equipment. Creative players can edit freely.
 */
public final class StationEvents {
    private StationEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(StationEvents::onBreak);
        bus.addListener(StationEvents::onPlace);
        bus.addListener(StationEvents::onExplosion);
        bus.addListener(StationEvents::onMobSpawn);
    }

    /** No natural mob spawns on the station grounds: nothing wanders into the investigation. */
    private static void onMobSpawn(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn event) {
        var type = event.getSpawnType();
        if (type != net.minecraft.world.entity.MobSpawnType.NATURAL && type != net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION
                && type != net.minecraft.world.entity.MobSpawnType.PATROL) return;
        if (!(event.getLevel().getLevel() instanceof ServerLevel level) || level != level.getServer().overworld()) return;
        if (CaseData.get(level.getServer()).nearStation(event.getX(), event.getY(), event.getZ(), 6)) event.setSpawnCancelled(true);
    }

    private static boolean protectedFor(Player player, ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (player == null || player.getAbilities().instabuild) return false;
        if (level != level.getServer().overworld()) return false;
        return CaseData.get(level.getServer()).inStation(pos);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && protectedFor(event.getPlayer(), level, event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("This is an active FCC investigation site. Leave the equipment as you found it."), true);
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Player p && protectedFor(p, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level != level.getServer().overworld()) return;
        CaseData data = CaseData.get(level.getServer());
        if (data.stationBuilt) event.getAffectedBlocks().removeIf(data::inStation);
    }
}
