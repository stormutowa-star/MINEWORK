package com.interactioncraft.mod.action;

import com.interactioncraft.mod.npc.NpcRules;
import com.interactioncraft.mod.works.PickupFilter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;

/** Montar y desmontar de verdad: quien monta se queda montado hasta que se le pida bajar o haga otra cosa. */
final class NpcRide {
    /** NPC que se montaron a proposito (paso "ride"/"drive"): Mover.claim no los baja mientras sigan en una accion libre. */
    static final Set<UUID> RIDERS = ConcurrentHashMap.newKeySet();
    private static final Set<String> GENERIC = Set.of("vehiculo", "vehiculos", "vehicle", "nave", "naves", "aeronave", "avion", "helicoptero",
            "coche", "carro", "auto", "moto", "transporte", "montura", "cosa", "cosas", "eso", "esto", "ese", "esta", "este", "esa", "aquello",
            "algo", "ahi", "alli", "aqui", "caza", "cazas", "barco", "bote", "barca", "vagoneta", "caballo", "ship", "plane", "car", "boat",
            "thing", "it", "that", "this", "el", "la", "lo", "los", "las", "un", "una", "de", "del", "al", "en", "a", "tu", "mi", "que", "cercano",
            "cercana", "cerca", "mas", "primero", "primera", "cualquiera", "cualquier");
    private static Field activeField;
    private static boolean activeFailed;

    private NpcRide() {
    }

    /** Sustituye a mob.isPassenger() en Mover.claim: solo baja al que no se monto a proposito en una accion libre. */
    static boolean mustDismount(Mob mob) {
        if (!mob.isPassenger()) {
            RIDERS.remove(mob.getUUID());
            return false;
        }
        if (RIDERS.contains(mob.getUUID()) && planActive(mob)) {
            return false;
        }
        RIDERS.remove(mob.getUUID());
        return true;
    }

    static boolean planActive(Mob mob) {
        if (activeFailed) {
            return true;
        }
        try {
            if (activeField == null) {
                Field f = Actions.class.getDeclaredField("ACTIVE");
                f.setAccessible(true);
                activeField = f;
            }
            Object st = ((Map<?, ?>) activeField.get(null)).get(mob.getUUID());
            return st instanceof Actions.State s && s.mode == Actions.Mode.PLAN;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            activeFailed = true;
            return true;
        }
    }

    static boolean generic(String query) {
        for (String w : PickupFilter.norm(query).split(" ")) {
            if (!w.isEmpty() && !GENERIC.contains(w)) {
                return false;
            }
        }
        return true;
    }

    static String label(Entity e) {
        String n = e.getName().getString();
        return n == null || n.isBlank() ? e.getType().getDescriptionId() : n;
    }

    static int score(String query, Entity e) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        return PickupFilter.score(query, id, label(e));
    }

    /** Entidad que sirve de vehiculo aunque no se llame como la pidieron (naves, coches, barcas, monturas de mods...). */
    static boolean vehicleLike(Entity e, Mob mob) {
        if (e == mob || !e.isAlive() || e.isSpectator() || e instanceof Player || e instanceof ItemEntity || e instanceof Projectile
                || e instanceof HangingEntity || e instanceof FallingBlockEntity || e instanceof PrimedTnt) {
            return false;
        }
        if (e.getPassengers().contains(mob) || mob.getPassengers().contains(e)) {
            return false;
        }
        if (e instanceof Boat || e instanceof AbstractMinecart) {
            return true;
        }
        if (e instanceof LivingEntity le) {
            return le instanceof net.minecraft.world.entity.animal.horse.AbstractHorse || le instanceof net.minecraft.world.entity.Saddleable
                    || le instanceof FlyingAnimal && !NpcRules.isNpcType(le);
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand || path.contains("marker") || path.contains("display")
                || path.contains("interaction") || path.contains("cloud") || path.contains("orb") || path.contains("lightning")) {
            return false;
        }
        return true;
    }

    /** Mejor vehiculo cercano para la peticion (nombre/id con puntaje; si es generica, el mas cercano que parezca vehiculo). */
    static Entity findVehicle(String query, Mob mob, ServerLevel level) {
        List<Entity> near = new ArrayList<>(level.getEntitiesOfClass(Entity.class, mob.getBoundingBox().inflate(48.0, 24.0, 48.0),
                x -> x != mob && x.isAlive() && !(x instanceof Player) && !(x instanceof ItemEntity)));
        boolean any = query == null || query.isBlank() || generic(query);
        Entity best = null;
        int bestScore = 0;
        double bestD = Double.MAX_VALUE;
        if (query != null && !query.isBlank()) {
            for (Entity e : near) {
                if (e.getPassengers().contains(mob)) {
                    continue;
                }
                int sc = score(query, e);
                double d = e.distanceToSqr(mob);
                if (sc > 0 && (sc > bestScore || sc == bestScore && d < bestD)) {
                    bestScore = sc;
                    bestD = d;
                    best = e;
                }
            }
        }
        if (best != null) {
            return best;
        }
        double limit = any ? 48.0 * 48.0 : 24.0 * 24.0;
        boolean bestFree = false;
        int count = 0;
        for (Entity e : near) {
            if (!vehicleLike(e, mob)) {
                continue;
            }
            double d = e.distanceToSqr(mob);
            if (d >= limit) {
                continue;
            }
            ++count;
            boolean free = e.getPassengers().isEmpty();
            if (best == null || free && !bestFree || free == bestFree && d < bestD) {
                bestD = d;
                best = e;
                bestFree = free;
            }
        }
        if (!any && count != 1) {
            return null;
        }
        return best;
    }

    // ------------------------------------------------------------ pasajero oculto dentro de naves
    private static final float HIDE = 0.0001f;
    private static java.lang.reflect.Method getEntityM;
    private static java.lang.reflect.Method getPoseM;
    private static boolean renderFailed;

    /** Un NPC dentro de una nave/cabina (entidad no viva que vuela) no debe asomar por el casco: se dibuja minusculo. */
    static boolean hiddenInside(Object entity) {
        if (!(entity instanceof Mob m)) {
            return false;
        }
        Entity v = m.getVehicle();
        if (v == null || v instanceof LivingEntity || v instanceof Boat || v instanceof AbstractMinecart) {
            return false;
        }
        return NpcOps.decideAir(v, "");
    }

    private static PoseStackHolder poseOf(Object ev) throws ReflectiveOperationException {
        if (getEntityM == null) {
            getEntityM = ev.getClass().getMethod("getEntity");
            getPoseM = ev.getClass().getMethod("getPoseStack");
        }
        Object ent = getEntityM.invoke(ev);
        return hiddenInside(ent) ? new PoseStackHolder((com.mojang.blaze3d.vertex.PoseStack) getPoseM.invoke(ev)) : null;
    }

    private record PoseStackHolder(com.mojang.blaze3d.vertex.PoseStack stack) {
    }

    static void renderPre(Object ev) {
        if (renderFailed) {
            return;
        }
        try {
            PoseStackHolder h = poseOf(ev);
            if (h != null) {
                h.stack().scale(HIDE, HIDE, HIDE);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            renderFailed = true;
        }
    }

    static void renderPost(Object ev) {
        if (renderFailed) {
            return;
        }
        try {
            PoseStackHolder h = poseOf(ev);
            if (h != null) {
                h.stack().scale(1.0f / HIDE, 1.0f / HIDE, 1.0f / HIDE);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            renderFailed = true;
        }
    }
}
