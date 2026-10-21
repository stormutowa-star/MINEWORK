package com.interactioncraft.mod.action;

import com.interactioncraft.mod.works.ActionLog;
import com.interactioncraft.mod.works.PickupFilter;
import com.interactioncraft.mod.works.PlanSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Oficios que no son un solo gesto: sembrar/plantar, cocinar y pescar. Se ejecutan de verdad (el NPC va al sitio, usa su mochila,
 * pasa el tiempo y el resultado llega a la mochila) en vez de quedarse en una frase de la IA.
 */
final class NpcSkills {
    private static final Map<UUID, Job> JOBS = new ConcurrentHashMap<>();

    private NpcSkills() {
    }

    static final class Job {
        final PlanRunner.Run run;
        final long stepStart;
        boolean plantable;
        boolean decided;
        String itemId = "";
        Block block;
        boolean crop;
        List<BlockPos> spots;
        boolean[] till;
        int idx;
        int done;
        BlockPos heat;
        BlockPos water;
        BlockPos stand;
        long nextAt;
        long lastSwing;
        boolean tookRod;
        String rawId = "";

        Job(PlanRunner.Run run) {
            this.run = run;
            this.stepStart = run.stepStart;
        }
    }

    static void clear(Mob mob) {
        Job j = JOBS.remove(mob.getUUID());
        if (j != null && j.tookRod) {
            Backpack.add(mob, "minecraft:fishing_rod", 1);
        }
    }

    private static Job job(Mob mob, PlanRunner.Run run) {
        Job j = JOBS.get(mob.getUUID());
        if (j == null || j.run != run || j.stepStart != run.stepStart) {
            if (j != null && j.tookRod) {
                Backpack.add(mob, "minecraft:fishing_rod", 1);
            }
            j = new Job(run);
            JOBS.put(mob.getUUID(), j);
        }
        return j;
    }

    private static boolean plantableItem(String itemId) {
        if (itemId == null) {
            return false;
        }
        ItemStack st = Builder.stackOf(itemId);
        return st.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BushBlock;
    }

    /** Elige que plantar: lo pedido, o lo plantable que lleve (semillas primero). */
    private static String pickPlantable(Mob mob, PlanSpec.Step st) {
        String query = st.item().isEmpty() ? st.text() : st.item();
        if (!query.isBlank() && !com.interactioncraft.mod.works.ItemWords.pronounOnly(query)) {
            String id = PlanRunner.itemFrom(query, mob);
            if (id != null && plantableItem(id)) {
                return id;
            }
        }
        String best = null;
        int bestRank = -1;
        for (Map.Entry<String, Integer> en : Backpack.of(mob).entrySet()) {
            if (en.getValue() <= 0 || !plantableItem(en.getKey())) {
                continue;
            }
            int rank = en.getKey().contains("seeds") || en.getKey().contains("potato") || en.getKey().contains("carrot") ? 3 : en.getKey().contains("sapling") ? 2 : 1;
            if (rank > bestRank) {
                bestRank = rank;
                best = en.getKey();
            }
        }
        return best;
    }

    /** true = lo ejecuta NpcSkills. */
    static boolean handles(Mob mob, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st) {
        String op = st.op();
        if (op.equals("cook") || op.equals("fish") || op.equals("plant")) {
            return true;
        }
        if (!op.equals("place")) {
            return false;
        }
        Job j = job(mob, run);
        if (!j.decided) {
            j.decided = true;
            String query = st.item().isEmpty() ? st.text() : st.item();
            String id = query.isBlank() ? null : PlanRunner.itemFrom(query, mob);
            j.plantable = id != null && plantableItem(id);
            if (j.plantable) {
                j.itemId = id;
            }
        }
        return j.plantable;
    }

    static PlanRunner.R exec(Mob mob, Actions.State s, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st, long now) {
        switch (st.op()) {
            case "cook":
                return cook(mob, level, run, st, now);
            case "fish":
                return fish(mob, s, level, run, st, now);
            default:
                return plant(mob, level, player, run, st, now);
        }
    }

    // ---------------------------------------------------------------- sembrar / plantar

    private static boolean freeSpot(ServerLevel level, BlockPos p) {
        BlockState cur = level.getBlockState(p);
        return (cur.isAir() || cur.canBeReplaced()) && cur.getFluidState().isEmpty();
    }

    private static boolean tillable(ServerLevel level, BlockPos q) {
        BlockState b = level.getBlockState(q);
        return (b.is(Blocks.GRASS_BLOCK) || b.is(Blocks.DIRT) || b.is(Blocks.DIRT_PATH) || b.is(Blocks.COARSE_DIRT) || b.is(Blocks.ROOTED_DIRT)) && freeSpot(level, q.above());
    }

    private static PlanRunner.R plant(Mob mob, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st, long now) {
        Job j = job(mob, run);
        if (j.spots == null) {
            String id = j.itemId.isEmpty() ? pickPlantable(mob, st) : j.itemId;
            if (id == null) {
                return PlanRunner.fail(run, "no lleva nada que plantar (semillas, brotes, flores...)");
            }
            j.itemId = id;
            j.block = ((BlockItem) Builder.stackOf(id).getItem()).getBlock();
            j.crop = j.block instanceof CropBlock || id.contains("seeds") || id.contains("potato") || id.contains("carrot") || id.contains("beetroot");
            BlockPos center = mob.blockPosition();
            if (!st.target().isEmpty()) {
                PlanRunner.Target tg = PlanRunner.target(run, st.target(), mob, player, level);
                if (tg != null) {
                    center = tg.block() != null ? tg.block() : (tg.entity() != null ? tg.entity().blockPosition() : BlockPos.containing(tg.pos()));
                }
            }
            int want = Math.max(1, st.n());
            int have = Backpack.of(mob).getOrDefault(id, 0);
            if (want > have) {
                want = Math.max(1, have);
            }
            List<BlockPos> spots = new ArrayList<>();
            List<Boolean> tills = new ArrayList<>();
            BlockState probe = j.block.defaultBlockState();
            final BlockPos c0 = center;
            List<BlockPos> cand = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-8, -3, -8), center.offset(8, 3, 8))) {
                if (level.hasChunkAt(p) && freeSpot(level, p)) {
                    cand.add(p.immutable());
                }
            }
            cand.sort(Comparator.comparingDouble(p -> p.distSqr(c0)));
            for (BlockPos p : cand) {
                if (spots.size() >= want) {
                    break;
                }
                if (probe.canSurvive(level, p)) {
                    spots.add(p);
                    tills.add(false);
                }
            }
            if (spots.size() < want && j.crop) {
                for (BlockPos p : cand) {
                    if (spots.size() >= want) {
                        break;
                    }
                    BlockPos q = p.below();
                    if (!spots.contains(p) && tillable(level, q) && !Backpack.of(mob).isEmpty()) {
                        spots.add(p);
                        tills.add(true);
                    }
                }
            }
            if (spots.isEmpty()) {
                return PlanRunner.fail(run, "no hay un sitio donde " + Materials(id) + " pueda crecer cerca");
            }
            j.spots = spots;
            j.till = new boolean[tills.size()];
            for (int i = 0; i < tills.size(); ++i) {
                j.till[i] = tills.get(i);
            }
            run.notes.add("planta " + Materials(id) + " en " + spots.size() + " sitio(s)");
            ActionLog.add(mob.getName().getString(), "PLANTAR", Materials(id) + " x" + spots.size() + " cerca de " + center.toShortString());
        }
        if (j.idx >= j.spots.size()) {
            return j.done > 0 ? PlanRunner.R.DONE : PlanRunner.fail(run, "no pudo plantar nada");
        }
        BlockPos pos = j.spots.get(j.idx);
        int a = PlanRunner.approach(mob, level, run, Vec3.atBottomCenterOf(pos), 3.0, now);
        if (a == 2) {
            ++j.idx;
            return PlanRunner.R.RUNNING;
        }
        if (a == 1) {
            return PlanRunner.R.RUNNING;
        }
        if (now - run.last < 8L) {
            return PlanRunner.R.RUNNING;
        }
        run.last = now;
        Mover.lookAt(mob, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
        if (!freeSpot(level, pos)) {
            ++j.idx;
            return PlanRunner.R.RUNNING;
        }
        if (j.till[j.idx]) {
            BlockPos q = pos.below();
            if (!tillable(level, q)) {
                ++j.idx;
                return PlanRunner.R.RUNNING;
            }
            level.setBlock(q, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
            level.playSound(null, q, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        if (!Backpack.take(mob, j.itemId, 1)) {
            return j.done > 0 ? PlanRunner.R.DONE : PlanRunner.fail(run, "se quedó sin " + Materials(j.itemId));
        }
        mob.swing(InteractionHand.MAIN_HAND);
        Builder.placeBlock(level, pos, j.block.defaultBlockState(), null, null);
        level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
        ++j.done;
        ++j.idx;
        return j.idx >= j.spots.size() ? PlanRunner.R.DONE : PlanRunner.R.RUNNING;
    }

    private static String Materials(String id) {
        return com.interactioncraft.mod.works.Materials.human(id);
    }

    // ---------------------------------------------------------------- cocinar

    private static boolean isHeat(BlockState b) {
        return b.is(Blocks.FURNACE) || b.is(Blocks.SMOKER) || b.is(Blocks.BLAST_FURNACE) || b.is(Blocks.CAMPFIRE) || b.is(Blocks.SOUL_CAMPFIRE)
                || b.getBlock() instanceof BaseFireBlock;
    }

    private static ItemStack cooked(ServerLevel level, String rawId) {
        ItemStack raw = Builder.stackOf(rawId);
        if (raw.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Optional<RecipeHolder<SmeltingRecipe>> r = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(raw), level);
        return r.map(h -> h.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
    }

    private static PlanRunner.R cook(Mob mob, ServerLevel level, PlanRunner.Run run, PlanSpec.Step st, long now) {
        Job j = job(mob, run);
        String query = st.item().isEmpty() ? st.text() : st.item();
        boolean all = query.isBlank() || PlanSpec.allWords(query) || com.interactioncraft.mod.works.ItemWords.pronounOnly(query);
        if (j.heat == null) {
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(mob.blockPosition().offset(-16, -5, -16), mob.blockPosition().offset(16, 5, 16))) {
                if (level.hasChunkAt(p) && isHeat(level.getBlockState(p))) {
                    double d = p.distSqr(mob.blockPosition());
                    if (d < bd) {
                        bd = d;
                        best = p.immutable();
                    }
                }
            }
            if (best == null) {
                return PlanRunner.fail(run, "no hay horno ni fogata cerca para cocinar");
            }
            j.heat = best;
        }
        int a = PlanRunner.approach(mob, level, run, Vec3.atBottomCenterOf(j.heat), 3.2, now);
        if (a == 1) {
            return PlanRunner.R.RUNNING;
        }
        if (a == 2) {
            return PlanRunner.fail(run, "no pudo llegar hasta el fuego");
        }
        Mover.hold(mob);
        Mover.lookAt(mob, j.heat.getX() + 0.5, j.heat.getY() + 0.5, j.heat.getZ() + 0.5);
        if (now < j.nextAt) {
            if (now % 6L == 0L) {
                level.sendParticles(ParticleTypes.SMOKE, j.heat.getX() + 0.5, j.heat.getY() + 1.1, j.heat.getZ() + 0.5, 3, 0.15, 0.1, 0.15, 0.01);
                level.sendParticles(ParticleTypes.FLAME, j.heat.getX() + 0.5, j.heat.getY() + 0.8, j.heat.getZ() + 0.5, 1, 0.15, 0.05, 0.15, 0.01);
            }
            return PlanRunner.R.RUNNING;
        }
        if (j.rawId.isEmpty()) {
            String pick = null;
            for (Map.Entry<String, Integer> en : Backpack.of(mob).entrySet()) {
                if (en.getValue() <= 0 || cooked(level, en.getKey()).isEmpty()) {
                    continue;
                }
                if (!all && PickupFilter.score(query, en.getKey(), com.interactioncraft.mod.works.Materials.human(en.getKey())) <= 0) {
                    continue;
                }
                pick = en.getKey();
                break;
            }
            if (pick == null) {
                return j.done > 0 ? PlanRunner.R.DONE : PlanRunner.fail(run, query.isBlank() ? "no lleva nada que se pueda cocinar o fundir" : "no lleva " + query + " que se pueda cocinar");
            }
            j.rawId = pick;
        }
        int limit = all ? 32 : Math.max(1, st.n());
        if (j.done >= limit || Backpack.of(mob).getOrDefault(j.rawId, 0) <= 0) {
            if (all && j.done < limit) {
                j.rawId = "";
                String before = j.rawId;
                boolean more = false;
                for (Map.Entry<String, Integer> en : Backpack.of(mob).entrySet()) {
                    more |= en.getValue() > 0 && !cooked(level, en.getKey()).isEmpty();
                }
                if (more) {
                    return PlanRunner.R.RUNNING;
                }
            }
            return j.done > 0 ? PlanRunner.R.DONE : PlanRunner.fail(run, "no pudo cocinar nada");
        }
        ItemStack out = cooked(level, j.rawId);
        if (out.isEmpty() || !Backpack.take(mob, j.rawId, 1)) {
            j.rawId = "";
            return PlanRunner.R.RUNNING;
        }
        Backpack.add(mob, BuiltInRegistries.ITEM.getKey(out.getItem()).toString(), Math.max(1, out.getCount()));
        mob.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, j.heat, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0f, 1.0f);
        run.notes.add("cocinó " + Materials(j.rawId) + " -> " + out.getHoverName().getString());
        ++j.done;
        j.nextAt = now + 40L;
        return PlanRunner.R.RUNNING;
    }

    // ---------------------------------------------------------------- pescar

    private static PlanRunner.R fish(Mob mob, Actions.State s, ServerLevel level, PlanRunner.Run run, PlanSpec.Step st, long now) {
        Job j = job(mob, run);
        if (j.water == null) {
            if (Backpack.count(mob, "minecraft:fishing_rod") <= 0) {
                int made = Backpack.takeOrCraft(mob, "minecraft:fishing_rod");
                if (made < 0) {
                    return PlanRunner.fail(run, "no lleva una caña de pescar (ni materiales para hacerla)");
                }
                j.tookRod = true;
            } else {
                Backpack.take(mob, "minecraft:fishing_rod", 1);
                j.tookRod = true;
            }
            BlockPos best = null;
            BlockPos bestStand = null;
            double bd = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(mob.blockPosition().offset(-18, -6, -18), mob.blockPosition().offset(18, 4, 18))) {
                if (!level.hasChunkAt(p) || !level.getFluidState(p).is(FluidTags.WATER) || !level.getBlockState(p.above()).isAir()) {
                    continue;
                }
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos stand = p.relative(d).above();
                    if (level.getBlockState(stand).isAir() && level.getBlockState(stand.below()).isSolid() && level.getFluidState(stand).isEmpty()) {
                        double dd = stand.distSqr(mob.blockPosition());
                        if (dd < bd) {
                            bd = dd;
                            best = p.immutable();
                            bestStand = stand.immutable();
                        }
                    }
                }
            }
            if (best == null) {
                return PlanRunner.fail(run, "no hay agua donde pescar cerca");
            }
            j.water = best;
            j.stand = bestStand;
            Actions.hold(mob, s, "minecraft:fishing_rod");
        }
        int a = PlanRunner.approach(mob, level, run, Vec3.atBottomCenterOf(j.stand), 1.6, now);
        if (a == 1) {
            return PlanRunner.R.RUNNING;
        }
        if (a == 2) {
            return PlanRunner.fail(run, "no pudo llegar hasta el agua");
        }
        Mover.hold(mob);
        Mover.lookAt(mob, j.water.getX() + 0.5, j.water.getY() + 0.9, j.water.getZ() + 0.5);
        if (j.nextAt == 0L) {
            j.nextAt = now + 80L + level.random.nextInt(140);
        }
        if (now - j.lastSwing >= 30L) {
            j.lastSwing = now;
            level.sendParticles(ParticleTypes.FISHING, j.water.getX() + 0.5, j.water.getY() + 1.0, j.water.getZ() + 0.5, 2, 0.2, 0.0, 0.2, 0.02);
        }
        if (now < j.nextAt) {
            return PlanRunner.R.RUNNING;
        }
        ItemStack rod = Builder.stackOf("minecraft:fishing_rod");
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(j.water))
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .create(LootContextParamSets.FISHING);
        List<ItemStack> loot = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params);
        mob.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.SPLASH, j.water.getX() + 0.5, j.water.getY() + 1.0, j.water.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.1);
        level.playSound(null, j.water, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.8f, 1.0f);
        StringBuilder got = new StringBuilder();
        for (ItemStack it : loot) {
            Backpack.add(mob, BuiltInRegistries.ITEM.getKey(it.getItem()).toString(), it.getCount());
            got.append(got.length() == 0 ? "" : ", ").append(it.getHoverName().getString());
        }
        run.notes.add("pescó " + (got.length() == 0 ? "nada" : got.toString()));
        ++j.done;
        j.nextAt = 0L;
        return j.done >= Math.max(1, Math.min(st.n(), 12)) ? PlanRunner.R.DONE : PlanRunner.R.RUNNING;
    }
}
