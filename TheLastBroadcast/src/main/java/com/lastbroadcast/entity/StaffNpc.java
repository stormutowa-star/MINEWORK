package com.lastbroadcast.entity;

import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DialogueDef;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.network.OpenDialoguePacket;
import com.lastbroadcast.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * WKVR staff member. Each character has a post: seated characters keep their chair and turn to
 * face visitors; walking characters follow a short routine between work positions in their room.
 * Staff are invulnerable story characters and never despawn.
 */
public class StaffNpc extends PathfinderMob {
    private static final EntityDataAccessor<String> CHARACTER = SynchedEntityData.defineId(StaffNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(StaffNpc.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TALKING = SynchedEntityData.defineId(StaffNpc.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WORKING = SynchedEntityData.defineId(StaffNpc.class, EntityDataSerializers.BOOLEAN);

    private BlockPos home = BlockPos.ZERO;
    private float homeYaw = 0f;
    private final List<BlockPos> route = new ArrayList<>();
    private final List<Float> routeYaw = new ArrayList<>();
    private UUID talkingTo;
    private int talkTicks;

    public StaffNpc(EntityType<? extends StaffNpc> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        setCanPickUpLoot(false);
    }

    public static StaffNpc create(ServerLevel level, String character, BlockPos home, float yaw, boolean sitting, List<BlockPos> route, List<Float> routeYaw) {
        StaffNpc npc = new StaffNpc(ModEntities.STAFF.get(), level);
        npc.setCharacter(character);
        npc.home = home.immutable();
        npc.homeYaw = yaw;
        npc.entityData.set(SITTING, sitting);
        npc.route.addAll(route);
        npc.routeYaw.addAll(routeYaw);
        double y = home.getY() + (sitting ? 0.5 : 0.0);
        npc.moveTo(home.getX() + 0.5, y, home.getZ() + 0.5, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        return npc;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARACTER, "rosa");
        builder.define(SITTING, false);
        builder.define(TALKING, false);
        builder.define(WORKING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new ConverseGoal());
        goalSelector.addGoal(1, new RoutineGoal());
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 7.0f, 0.6f));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !isSitting() && super.canUse();
            }
        });
    }

    // ------------------------------------------------------------------ accessors

    public String getCharacter() {
        return entityData.get(CHARACTER);
    }

    public void setCharacter(String c) {
        entityData.set(CHARACTER, c);
        DialogueDef def = ContentLibrary.DIALOGUES.get(c);
        if (def != null) setCustomName(Component.literal(def.name));
    }

    public boolean isSitting() {
        return entityData.get(SITTING);
    }

    public boolean isTalking() {
        return entityData.get(TALKING);
    }

    public boolean isWorking() {
        return entityData.get(WORKING);
    }

    // ------------------------------------------------------------------ conversation

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            talkingTo = player.getUUID();
            talkTicks = 20 * 60 * 3;
            entityData.set(TALKING, true);
            getNavigation().stop();
            Net.toPlayer(sp, new OpenDialoguePacket(getId(), getCharacter()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void endConversation(Player player) {
        if (player.getUUID().equals(talkingTo)) {
            talkingTo = null;
            talkTicks = 0;
            entityData.set(TALKING, false);
        }
    }

    private Player conversationPartner() {
        if (talkingTo == null) return null;
        Player p = level().getPlayerByUUID(talkingTo);
        if (p == null || p.distanceToSqr(this) > 12 * 12) return null;
        return p;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // Client body rotation would otherwise drift after the head; seated staff stay square to their desk.
            if (isSitting()) {
                yBodyRot = getYRot();
                yBodyRotO = yBodyRot;
            }
            return;
        }
        if (talkTicks > 0 && --talkTicks == 0 || (talkingTo != null && conversationPartner() == null)) {
            talkingTo = null;
            talkTicks = 0;
            entityData.set(TALKING, false);
        }
        if (isSitting()) {
            // Seated characters stay put and keep their body square to the desk.
            double y = home.getY() + 0.5;
            if (distanceToSqr(home.getX() + 0.5, y, home.getZ() + 0.5) > 0.04) moveTo(home.getX() + 0.5, y, home.getZ() + 0.5, homeYaw, getXRot());
            setYRot(homeYaw);
            setYBodyRot(homeYaw);
            // never let the head swivel past the shoulders while seated
            setYHeadRot(net.minecraft.util.Mth.rotateIfNecessary(getYHeadRot(), homeYaw, getMaxHeadYRot()));
            setDeltaMovement(Vec3.ZERO);
        } else if (!home.equals(BlockPos.ZERO) && tickCount % 40 == 0 && distanceToSqr(Vec3.atBottomCenterOf(home)) > 30 * 30) {
            getNavigation().stop();
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        }
    }

    @Override
    public int getMaxHeadYRot() {
        return isSitting() ? 70 : 75;
    }

    @Override
    public boolean isNoGravity() {
        return isSitting() || super.isNoGravity();
    }

    // ------------------------------------------------------------------ invulnerability & physics

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Character", getCharacter());
        tag.putBoolean("Sitting", isSitting());
        tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putFloat("HomeYaw", homeYaw);
        ListTag r = new ListTag();
        for (int i = 0; i < route.size(); i++) {
            CompoundTag p = new CompoundTag();
            p.put("Pos", NbtUtils.writeBlockPos(route.get(i)));
            p.putFloat("Yaw", i < routeYaw.size() ? routeYaw.get(i) : 0f);
            r.add(p);
        }
        tag.put("Route", r);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCharacter(tag.getString("Character"));
        entityData.set(SITTING, tag.getBoolean("Sitting"));
        home = NbtUtils.readBlockPos(tag, "Home").orElse(blockPosition());
        homeYaw = tag.getFloat("HomeYaw");
        route.clear();
        routeYaw.clear();
        ListTag r = tag.getList("Route", Tag.TAG_COMPOUND);
        for (int i = 0; i < r.size(); i++) {
            CompoundTag p = r.getCompound(i);
            route.add(NbtUtils.readBlockPos(p, "Pos").orElse(home));
            routeYaw.add(p.getFloat("Yaw"));
        }
        setInvulnerable(true);
    }

    // ------------------------------------------------------------------ goals

    /** Stop and face the person we are talking to. */
    private class ConverseGoal extends Goal {
        ConverseGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isTalking() && conversationPartner() != null;
        }

        @Override
        public void start() {
            getNavigation().stop();
            entityData.set(WORKING, false);
        }

        @Override
        public void tick() {
            Player p = conversationPartner();
            if (p != null) getLookControl().setLookAt(p, 30f, 30f);
        }
    }

    /** Walk between work positions, pausing to work at each one. */
    private class RoutineGoal extends Goal {
        private int index = 0;
        private int dwell = 0;
        private boolean walking = false;

        RoutineGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isSitting() && !route.isEmpty() && !isTalking();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            dwell = 40;
            walking = false;
        }

        @Override
        public void stop() {
            getNavigation().stop();
            entityData.set(WORKING, false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos target = route.get(index);
            Vec3 t = Vec3.atBottomCenterOf(target);
            if (walking) {
                if (distanceToSqr(t) < 0.5 || getNavigation().isDone()) {
                    walking = false;
                    getNavigation().stop();
                    dwell = 160 + getRandom().nextInt(260);
                    if (distanceToSqr(t) > 2.5) dwell = 20;
                }
                return;
            }
            float yaw = index < routeYaw.size() ? routeYaw.get(index) : getYRot();
            setYRot(yaw);
            setYBodyRot(yaw);
            Vec3 look = position().add(-Math.sin(Math.toRadians(yaw)) * 2, getEyeHeight() - 0.4, Math.cos(Math.toRadians(yaw)) * 2);
            getLookControl().setLookAt(look.x, look.y, look.z);
            entityData.set(WORKING, true);
            if (--dwell <= 0) {
                entityData.set(WORKING, false);
                index = (index + 1) % route.size();
                BlockPos next = route.get(index);
                walking = getNavigation().moveTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 0.6);
                if (!walking) dwell = 100;
            }
        }
    }
}
