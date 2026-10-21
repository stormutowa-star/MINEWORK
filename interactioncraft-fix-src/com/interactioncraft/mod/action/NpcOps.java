package com.interactioncraft.mod.action;

import com.interactioncraft.mod.works.ActionLog;
import com.interactioncraft.mod.works.PickupFilter;
import com.interactioncraft.mod.works.PlanSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Pasos "ride", "dismount" y "drive" de las acciones libres, con ejecucion real: el NPC camina hasta el vehiculo, se monta
 * (y se queda montado entre pasos) y lo maneja el mismo con un piloto automatico que no depende de que el mod del vehiculo
 * sepa de NPC. Se engancha en PlanRunner.exec (devuelve null si el paso no es suyo).
 */
final class NpcOps {
    /** Pasos que se pueden hacer sin bajarse del vehiculo; cualquier otro baja al NPC antes (aterrizando si vuela). */
    private static final Set<String> STAY = Set.of("look", "wait", "say", "emote", "sound", "swing", "command", "equip", "power", "learn",
            "train", "observe", "teleport", "ride", "dismount", "drive", "eat", "crouch");
    private static final String[] AIR_WORDS = {"fighter", "caza", "nave", "aeronave", "avion", "plane", "jet", "heli", "aircraft", "wing", "drone",
            "ufo", "ovni", "saucer", "glider", "planeador", "broom", "escoba", "dragon", "zeppelin", "dirigible", "globo", "balloon", "rocket",
            "cohete", "starship", "spaceship", "spacecraft", "xwing", "ywing", "bwing", "falcon", "interceptor", "bomber", "bombardero",
            "shuttle", "lanzadera", "vuela", "volar", "vuelo", "fly", "flying", "aire", "cielo", "sky", "air", "jetpack", "alfombra", "carpet"};
    private static final Map<UUID, Board> BOARD = new ConcurrentHashMap<>();
    private static final Map<UUID, Drive> DRIVE = new ConcurrentHashMap<>();

    private NpcOps() {
    }

    static final class Board {
        final PlanRunner.Run run;
        final long stepStart;
        UUID vehicle;
        String label = "";
        long walkStart;
        boolean wasRiding;

        Board(PlanRunner.Run run) {
            this.run = run;
            this.stepStart = run.stepStart;
        }
    }

    static final class Drive {
        final PlanRunner.Run run;
        final long stepStart;
        int phase;
        long phaseStart;
        UUID vehicle;
        boolean air;
        boolean wasRiding;
        boolean gravityTouched;
        boolean gravityWas;
        Vec3 center;
        double baseY;
        Vec3 dest;
        UUID destEntity;
        double speed;
        double vy;
        float yaw;
        boolean yawSet;
        int strategy;
        Vec3 last;
        long lastCheck;
        long climbUntil;
        int blockedTicks;
        long landStart;

        Drive(PlanRunner.Run run) {
            this.run = run;
            this.stepStart = run.stepStart;
        }
    }

    private static boolean own(String op) {
        return op.equals("ride") || op.equals("dismount") || op.equals("drive");
    }

    /** "usa X" cuando X es una criatura/vehiculo (no un bloque): el codigo original solo sabe usar bloques. */
    private static boolean useOnEntity(Mob mob, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st) {
        if (!(st.op().equals("use") || st.op().equals("interact")) || st.target().isEmpty() || player == null) {
            return false;
        }
        PlanRunner.Target tg = PlanRunner.target(run, st.target(), mob, player, level);
        return tg != null && tg.block() == null && tg.entity() != null && tg.entity() != player && tg.entity() != mob;
    }

    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static java.lang.reflect.Method execM;

    private static boolean useOnBlock(Mob mob, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st) {
        if (!st.op().equals("use") || st.target().isEmpty() || player == null) {
            return false;
        }
        PlanRunner.Target tg = PlanRunner.target(run, st.target(), mob, player, level);
        return tg != null && tg.block() != null;
    }

    /** "usar" un bloque: lo del juego original y, si no pasa nada, se prueba a pulsarlo por cada una de sus caras (campanas, botones, bloques de mods...). */
    private static PlanRunner.R useBlock(Mob mob, Actions.State s, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st, long now) {
        PlanRunner.R r;
        try {
            if (execM == null) {
                execM = PlanRunner.class.getDeclaredMethod("exec", Mob.class, Actions.State.class, ServerLevel.class, ServerPlayer.class, PlanRunner.Run.class, PlanSpec.Step.class, long.class);
                execM.setAccessible(true);
            }
            BYPASS.set(Boolean.TRUE);
            try {
                r = (PlanRunner.R) execM.invoke(null, mob, s, level, player, run, st, now);
            } finally {
                BYPASS.remove();
            }
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            return PlanRunner.fail(run, "no pudo usarlo");
        }
        if (r == PlanRunner.R.FAIL && run.failMsg.startsWith("no pasó nada al usar")) {
            PlanRunner.Target tg = PlanRunner.target(run, st.target(), mob, player, level);
            if (tg != null && tg.block() != null && pressAllFaces(mob, level, tg.block(), st)) {
                run.failMsg = "";
                return PlanRunner.R.DONE;
            }
        }
        return r;
    }

    private static boolean pressAllFaces(Mob mob, ServerLevel level, BlockPos pos, PlanSpec.Step st) {
        Object fp = PlanRunner.fakePlayerFor(level, mob);
        if (!(fp instanceof Player p)) {
            return false;
        }
        BlockState before = level.getBlockState(pos);
        String itemId = st.item().isEmpty() ? null : PlanRunner.itemFrom(st.item(), mob);
        ItemStack stack = itemId == null ? ItemStack.EMPTY : Builder.stackOf(itemId);
        net.minecraft.core.Direction[] order = {net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST,
            net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN};
        try {
            for (net.minecraft.core.Direction d : order) {
                Vec3 loc = Vec3.atCenterOf(pos).add(d.getStepX() * 0.45, d.getStepY() * 0.45, d.getStepZ() * 0.45);
                net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(loc, d, pos, false);
                if (!stack.isEmpty()) {
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
                    if (before.useItemOn(p.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), level, p, net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction()) {
                        return true;
                    }
                }
                if (before.useWithoutItem(level, p, hit).consumesAction() || !level.getBlockState(pos).equals(before)) {
                    return true;
                }
            }
        } finally {
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        return false;
    }

    /** true = este paso lo ejecuta NpcOps (pasos propios, "usar" una criatura, o cualquier otro mientras el NPC sigue montado). */
    static boolean handles(Mob mob, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st) {
        String op = st.op();
        if (own(op) || mob.isPassenger() && !STAY.contains(op)) {
            return true;
        }
        if (BYPASS.get()) {
            return false;
        }
        return NpcSkills.handles(mob, level, player, run, st) || useOnEntity(mob, level, player, run, st) || useOnBlock(mob, level, player, run, st);
    }

    /** Punto de entrada desde PlanRunner.exec, solo si handles(...) es true. */
    static PlanRunner.R exec(Mob mob, Actions.State s, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st, long now) {
        String op = st.op();
        if (!own(op) && mob.isPassenger() && !STAY.contains(op)) {
            // otro paso mientras sigue montado: primero se baja (aterrizando si vuela) y el paso corre en el tic siguiente
            dismountSafely(mob, level, run, now);
            return PlanRunner.R.RUNNING;
        }
        switch (op) {
            case "ride":
                return ride(mob, level, player, run, st, now);
            case "dismount":
                if (!mob.isPassenger()) {
                    return PlanRunner.R.DONE;
                }
                if (dismountSafely(mob, level, run, now)) {
                    run.notes.add("se bajó de su montura");
                    return PlanRunner.R.DONE;
                }
                return PlanRunner.R.RUNNING;
            case "drive":
                return drive(mob, level, player, run, st, now);
            default:
                if (NpcSkills.handles(mob, level, player, run, st)) {
                    return NpcSkills.exec(mob, s, level, player, run, st, now);
                }
                if (useOnEntity(mob, level, player, run, st)) {
                    return useEntity(mob, s, level, player, run, st, now);
                }
                return useBlock(mob, s, level, player, run, st, now);
        }
    }

    private static PlanRunner.R useEntity(Mob mob, Actions.State s, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st, long now) {
        Board b = BOARD.get(mob.getUUID());
        if (b != null && b.run == run && b.stepStart == run.stepStart) {
            return board(mob, level, player, run, st.target(), now);
        }
        PlanRunner.Target tg = PlanRunner.target(run, st.target(), mob, player, level);
        Entity e = tg == null ? null : tg.entity();
        PlanSpec.Step as = new PlanSpec.Step("interact", st.target(), st.item(), st.n(), st.secs(), st.text());
        PlanRunner.R r = PlanExtras.exec(mob, s, level, player, run, as, now);
        if (r == PlanRunner.R.DONE && e != null && NpcRide.vehicleLike(e, mob)) {
            // un vehiculo "usado" por el jugador falso lo monta el jugador falso: se le baja y se monta el NPC
            Object fp = PlanRunner.fakePlayerFor(level, mob);
            if (fp instanceof Player p && p.getVehicle() != null) {
                p.stopRiding();
                return board(mob, level, player, run, st.target(), now);
            }
        }
        if (r == PlanRunner.R.FAIL && e != null && NpcRide.vehicleLike(e, mob)
                && (run.failMsg.startsWith("no pasó nada") || run.failMsg.startsWith("con eso no interactúa") || run.failMsg.startsWith("no pudo interactuar"))) {
            run.failMsg = "";
            return board(mob, level, player, run, st.target(), now);
        }
        return r;
    }

    /** Llamado al terminar o abortar cualquier paso: suelta la gravedad y posa el vehiculo si seguia en el aire. */
    static void endStep(Mob mob) {
        UUID id = mob.getUUID();
        NpcSkills.clear(mob);
        BOARD.remove(id);
        Drive d = DRIVE.remove(id);
        if (d == null || !d.gravityTouched || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Entity veh = d.vehicle == null ? null : level.getEntity(d.vehicle);
        if (veh == null) {
            return;
        }
        try {
            if (d.air && veh.getY() - groundY(level, veh) > 1.0) {
                Vec3 down = new Vec3(veh.getX(), groundY(level, veh) + 0.05, veh.getZ());
                if (level.noCollision(veh, veh.getBoundingBox().move(down.subtract(veh.position())))) {
                    veh.setPos(down.x, down.y, down.z);
                }
            }
            veh.setNoGravity(d.gravityWas);
            veh.setDeltaMovement(Vec3.ZERO);
            veh.hurtMarked = true;
            veh.fallDistance = 0.0f;
            mob.fallDistance = 0.0f;
        } catch (RuntimeException e) {
            veh.setNoGravity(d.gravityWas);
        }
    }


    // ---------------------------------------------------------------- resolver objetivos (red de seguridad)
    private static java.lang.reflect.Method resolveM;
    private static final Set<String> THING_WORDS = Set.of("maquina", "maquinas", "aparato", "mecanismo", "dispositivo", "artefacto", "objeto", "cosa", "cacharro",
            "trasto", "machine", "device", "thing", "mechanism", "contraption", "invento", "armatoste");
    private static final Set<String> LOOSE_STOP = Set.of("de", "del", "la", "las", "el", "los", "lo", "un", "una", "unos", "unas", "al", "en", "con", "y", "que", "tu", "mi", "este", "esta", "ese", "esa", "aquel");
    private static final java.util.regex.Pattern COORDS = java.util.regex.Pattern.compile("^\\s*(-?\\d+)[ ,]+(-?\\d+)[ ,]+(-?\\d+)\\s*$");

    /** Sustituye a PlanRunner.resolve dentro de PlanRunner.target: primero el original; si no halla nada, coordenadas, coincidencia flexible y "lo que mira el jugador". */
    static PlanRunner.Target resolve(PlanRunner.Run run, String query, Mob mob, ServerPlayer player, ServerLevel level) {
        PlanRunner.Target t = null;
        try {
            if (resolveM == null) {
                resolveM = PlanRunner.class.getDeclaredMethod("resolve", PlanRunner.Run.class, String.class, Mob.class, ServerPlayer.class, ServerLevel.class);
                resolveM.setAccessible(true);
            }
            t = (PlanRunner.Target) resolveM.invoke(null, run, query, mob, player, level);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
        } catch (ReflectiveOperationException e) {
            // sin reflexion no hay original: se sigue con la red de seguridad
        }
        if (t != null) {
            return t;
        }
        try {
            return fallbackTarget(run, query, mob, player, level);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int stems(String a, String b) {
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) {
            ++i;
        }
        return i;
    }

    private static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        for (String w : PickupFilter.norm(text).split(" ")) {
            if (w.length() >= 3 && !LOOSE_STOP.contains(w)) {
                out.add(w);
            }
        }
        return out;
    }

    private static int loose(List<String> q, String text) {
        if (q.isEmpty()) {
            return 0;
        }
        List<String> have = words(text.replace('_', ' ').replace(':', ' '));
        int hits = 0;
        for (String w : q) {
            for (String t : have) {
                if (t.equals(w) || w.length() >= 4 && t.length() >= 4 && stems(w, t) >= Math.min(5, Math.min(w.length(), t.length()))) {
                    ++hits;
                    break;
                }
            }
        }
        return hits * 2 >= q.size() ? hits : 0;
    }

    private static String namesOf(String id, String shown) {
        return shown + " " + id + " " + PickupFilter.spanish(id);
    }

    private static PlanRunner.Target fallbackTarget(PlanRunner.Run run, String query, Mob mob, ServerPlayer player, ServerLevel level) {
        if (query == null || query.isBlank()) {
            return null;
        }
        java.util.regex.Matcher cm = COORDS.matcher(query);
        if (cm.matches()) {
            int x = Integer.parseInt(cm.group(1));
            int y = Integer.parseInt(cm.group(2));
            int z = Integer.parseInt(cm.group(3));
            if (y == -9999 || y == -9999L) {
                y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            }
            BlockPos p = new BlockPos(x, y, z);
            if (level.hasChunkAt(p)) {
                return new PlanRunner.Target(null, p, Vec3.atBottomCenterOf(p), "las coordenadas " + x + " " + y + " " + z);
            }
            return null;
        }
        List<String> q = words(query);
        boolean thing = !q.isEmpty() && THING_WORDS.containsAll(q);
        // 1) coincidencia flexible con criaturas y bloques cercanos
        if (!q.isEmpty() && !thing) {
            Entity bestE = null;
            int bestScore = 0;
            double bestD = Double.MAX_VALUE;
            for (Entity e : level.getEntitiesOfClass(Entity.class, mob.getBoundingBox().inflate(32.0, 12.0, 32.0), x -> x != mob && x.isAlive() && x != player)) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
                int sc = loose(q, namesOf(id, e.getName().getString()));
                double d = e.distanceToSqr(mob);
                if (sc > bestScore || sc == bestScore && sc > 0 && d < bestD) {
                    bestScore = sc;
                    bestD = d;
                    bestE = e;
                }
            }
            BlockPos bestB = null;
            int bestBS = 0;
            double bestBD = Double.MAX_VALUE;
            java.util.HashMap<net.minecraft.world.level.block.Block, Integer> cache = new java.util.HashMap<>();
            for (BlockPos c : new BlockPos[]{mob.blockPosition(), player.blockPosition()}) {
                for (BlockPos p : BlockPos.betweenClosed(c.offset(-13, -4, -13), c.offset(13, 6, 13))) {
                    if (!level.hasChunkAt(p)) {
                        continue;
                    }
                    net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(p);
                    if (bs.isAir()) {
                        continue;
                    }
                    int sc = cache.computeIfAbsent(bs.getBlock(), b -> loose(q, namesOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString(), b.getName().getString())));
                    double d = p.distSqr(mob.blockPosition());
                    if (sc > bestBS || sc == bestBS && sc > 0 && d < bestBD) {
                        bestBS = sc;
                        bestBD = d;
                        bestB = p.immutable();
                    }
                }
            }
            if (bestB != null && bestBS >= bestScore) {
                return new PlanRunner.Target(null, bestB, Vec3.atBottomCenterOf(bestB), level.getBlockState(bestB).getBlock().getName().getString());
            }
            if (bestE != null) {
                return new PlanRunner.Target(bestE, null, null, bestE.getName().getString());
            }
        }
        // 2) "la maquina", "ese aparato": lo que mira el jugador, o lo mas cercano que no sea terreno
        if (thing || q.isEmpty() || PlanSpec.demonstrative(query)) {
            if (run.lookedEntity != null) {
                Entity le = level.getEntity(run.lookedEntity);
                if (le != null && le.isAlive() && le != mob && le != player) {
                    return new PlanRunner.Target(le, null, null, le.getName().getString());
                }
            }
            if (run.anchor != null && !level.getBlockState(run.anchor).isAir()) {
                return new PlanRunner.Target(null, run.anchor, Vec3.atBottomCenterOf(run.anchor), level.getBlockState(run.anchor).getBlock().getName().getString());
            }
        }
        if (thing) {
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (BlockPos c : new BlockPos[]{mob.blockPosition(), player.blockPosition()}) {
                for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, -4, -12), c.offset(12, 6, 12))) {
                    if (!level.hasChunkAt(p)) {
                        continue;
                    }
                    net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(p);
                    if (bs.isAir()) {
                        continue;
                    }
                    boolean modded = !"minecraft".equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bs.getBlock()).getNamespace());
                    if (!(modded || level.getBlockEntity(p) != null)) {
                        continue;
                    }
                    double d = p.distSqr(mob.blockPosition());
                    if (d < bd) {
                        bd = d;
                        best = p.immutable();
                    }
                }
            }
            if (best != null) {
                return new PlanRunner.Target(null, best, Vec3.atBottomCenterOf(best), level.getBlockState(best).getBlock().getName().getString());
            }
            Entity ne = null;
            double nd = Double.MAX_VALUE;
            for (Entity e : level.getEntitiesOfClass(Entity.class, mob.getBoundingBox().inflate(14.0, 6.0, 14.0), x -> x != mob && x != player && x.isAlive() && !(x instanceof LivingEntity) && !(x instanceof net.minecraft.world.entity.item.ItemEntity))) {
                double d = e.distanceToSqr(mob);
                if (d < nd) {
                    nd = d;
                    ne = e;
                }
            }
            if (ne != null) {
                return new PlanRunner.Target(ne, null, null, ne.getName().getString());
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- montar

    private static PlanRunner.R ride(Mob mob, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st, long now) {
        String query = st.target().isEmpty() ? st.item() : st.target();
        return board(mob, level, player, run, query, now);
    }

    private static Board boardState(Mob mob, PlanRunner.Run run) {
        Board b = BOARD.get(mob.getUUID());
        if (b == null || b.run != run || b.stepStart != run.stepStart) {
            b = new Board(run);
            BOARD.put(mob.getUUID(), b);
        }
        return b;
    }

    /** DONE = ya va montado; RUNNING = caminando hacia el vehiculo; FAIL con motivo. */
    static PlanRunner.R board(Mob mob, ServerLevel level, ServerPlayer player, PlanRunner.Run run, String query, long now) {
        Board b = boardState(mob, run);
        Entity cur = mob.getVehicle();
        if (cur != null) {
            Entity root = mob.getRootVehicle();
            if (query.isBlank() || NpcRide.generic(query) || NpcRide.score(query, root) > 0 || NpcRide.score(query, cur) > 0) {
                NpcRide.RIDERS.add(mob.getUUID());
                return PlanRunner.R.DONE;
            }
            mob.stopRiding();
        }
        Entity veh = b.vehicle == null ? null : level.getEntity(b.vehicle);
        if (veh == null || !veh.isAlive()) {
            if (b.vehicle != null) {
                return PlanRunner.fail(run, "el vehículo desapareció");
            }
            veh = NpcRide.findVehicle(query, mob, level);
            if (veh == null) {
                PlanRunner.Target tg = query.isBlank() ? null : PlanRunner.target(run, query, mob, player, level);
                if (tg != null && tg.entity() == null) {
                    int a = PlanRunner.approach(mob, level, run, tg.where(), 1.8, now);
                    return a == 0 ? PlanRunner.R.DONE : (a == 1 ? PlanRunner.R.RUNNING : PlanRunner.fail(run, "no pudo llegar hasta " + tg.label()));
                }
                return PlanRunner.fail(run, query.isBlank() ? "no hay ningún vehículo cerca para montarse" : "no encuentra " + query + " para montarse");
            }
            b.vehicle = veh.getUUID();
            b.label = NpcRide.label(veh);
            b.walkStart = now;
        }
        if (mob.distanceToSqr(veh) > 56.0 * 56.0) {
            return PlanRunner.fail(run, b.label + " está demasiado lejos");
        }
        double reach = Math.max(2.4, veh.getBbWidth() * 0.5 + 1.6);
        double dx = veh.getX() - mob.getX();
        double dz = veh.getZ() - mob.getZ();
        boolean close = Math.sqrt(dx * dx + dz * dz) <= reach + 0.5 && Math.abs(veh.getY() - mob.getY()) <= 4.0;
        if (!close && now - b.walkStart < 160L) {
            int a = PlanRunner.approach(mob, level, run, veh.position(), reach, now);
            if (a == 1) {
                return PlanRunner.R.RUNNING;
            }
        }
        boolean ok = mob.startRiding(veh, true) && mob.getVehicle() != null;
        if (!ok) {
            mob.teleportTo(veh.getX(), veh.getY(), veh.getZ());
            ok = mob.startRiding(veh, true) && mob.getVehicle() != null;
        }
        if (!ok) {
            return PlanRunner.fail(run, "no pudo montarse en " + b.label + " (no admite pasajeros)");
        }
        NpcRide.RIDERS.add(mob.getUUID());
        Mover.stop(mob);
        mob.setYRot(veh.getYRot());
        mob.setYHeadRot(veh.getYRot());
        run.notes.add("se montó en " + b.label);
        ActionLog.add(mob.getName().getString(), "MONTAR", b.label);
        return PlanRunner.R.DONE;
    }

    // ---------------------------------------------------------------- bajar

    private static double groundY(ServerLevel level, Entity e) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, e.getBlockX(), e.getBlockZ());
    }

    private static boolean airborne(ServerLevel level, Entity veh) {
        return !veh.onGround() && !veh.isInWater() && !veh.verticalCollisionBelow && veh.getY() - groundY(level, veh) > 1.5;
    }

    private static Drive driveState(Mob mob, PlanRunner.Run run) {
        Drive d = DRIVE.get(mob.getUUID());
        if (d == null || d.run != run || d.stepStart != run.stepStart) {
            d = new Drive(run);
            d.phaseStart = run.stepStart;
            DRIVE.put(mob.getUUID(), d);
        }
        return d;
    }

    /** true = ya esta en el suelo y desmontado. Si el vehiculo esta en el aire lo posa antes. */
    static boolean dismountSafely(Mob mob, ServerLevel level, PlanRunner.Run run, long now) {
        if (!mob.isPassenger()) {
            NpcRide.RIDERS.remove(mob.getUUID());
            return true;
        }
        Entity veh = mob.getRootVehicle();
        Drive d = driveState(mob, run);
        if (d.landStart == 0L) {
            d.landStart = now;
        }
        if (airborne(level, veh) && now - d.landStart < 600L) {
            if (d.vehicle == null) {
                d.vehicle = veh.getUUID();
                d.air = true;
                d.yawSet = false;
            }
            steer(mob, level, d, veh, veh.getX(), veh.getZ(), groundY(level, veh), 0.3, now, 1.0, true);
            return false;
        }
        BlockPos at = veh.blockPosition();
        mob.stopRiding();
        NpcRide.RIDERS.remove(mob.getUUID());
        mob.fallDistance = 0.0f;
        Mover.hop(mob, level, at);
        return true;
    }

    // ---------------------------------------------------------------- pilotar

    static boolean decideAir(Entity veh, String hint) {
        if (veh instanceof Boat || veh instanceof AbstractMinecart) {
            return false;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(veh.getType()).toString();
        String n = " " + PickupFilter.norm(id + " " + veh.getName().getString() + " " + hint) + " ";
        if (veh.isNoGravity() || veh instanceof FlyingAnimal) {
            return true;
        }
        for (String w : AIR_WORDS) {
            if (n.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static PlanRunner.R drive(Mob mob, ServerLevel level, ServerPlayer player, PlanRunner.Run run, PlanSpec.Step st, long now) {
        Drive d = driveState(mob, run);
        String hint = PickupFilter.norm(st.text() + " " + st.target() + " " + st.item());
        if (d.phase == 0) {
            d.wasRiding = mob.isPassenger();
            if (!mob.isPassenger()) {
                String vq = st.item().isBlank() ? st.target() : st.item();
                PlanRunner.R r = board(mob, level, player, run, vq, now);
                if (r != PlanRunner.R.DONE) {
                    return r;
                }
            }
            if (!mob.isPassenger()) {
                return PlanRunner.fail(run, "no pudo montarse en ningún vehículo");
            }
            Entity veh = mob.getRootVehicle();
            d.vehicle = veh.getUUID();
            d.air = decideAir(veh, hint);
            Vec3 p = veh.position();
            d.baseY = Math.max(p.y, groundY(level, veh));
            d.center = player != null && player.level() == level && player.distanceToSqr(veh) < 50.0 * 50.0 ? player.position() : p;
            String destQ = !st.item().isBlank() || d.wasRiding ? st.target() : "";
            if (!destQ.isBlank() && !NpcRide.generic(destQ) && NpcRide.score(destQ, veh) <= 0) {
                PlanRunner.Target tg = PlanRunner.target(run, destQ, mob, player, level);
                if (tg != null && tg.entity() != veh && tg.entity() != mob) {
                    d.dest = tg.where();
                    d.destEntity = tg.entity() == null ? null : tg.entity().getUUID();
                }
            }
            d.phase = hint.contains("aterriz") || hint.contains("land") ? 2 : 1;
            d.phaseStart = now;
            run.notes.add((d.air ? "pilotó volando " : "manejó ") + NpcRide.label(veh));
            ActionLog.add(mob.getName().getString(), "PILOTAR", NpcRide.label(veh) + (d.air ? " (aire)" : " (suelo)") + (d.dest != null ? " hacia " + d.dest : ""));
        }
        Entity veh = mob.getVehicle() == null ? null : mob.getRootVehicle();
        if (veh == null || !veh.isAlive()) {
            return PlanRunner.fail(run, "se quedó sin vehículo");
        }
        if (d.destEntity != null) {
            Entity de = level.getEntity(d.destEntity);
            if (de != null) {
                d.dest = de.position();
            }
        }
        long secs = st.secs() > 0 ? st.secs() : 25;
        double vmax = d.air ? 0.5 : 0.3;
        double toPlayer = player == null ? 0.0 : Math.hypot(player.getX() - veh.getX(), player.getZ() - veh.getZ());
        if (d.phase == 1) {
            boolean timeUp = now - d.phaseStart >= secs * 20L;
            boolean arrived = d.dest != null && Math.hypot(d.dest.x - veh.getX(), d.dest.z - veh.getZ()) < 4.0;
            if (timeUp || arrived || toPlayer > 85.0) {
                d.phase = 2;
                d.phaseStart = now;
            } else if (d.dest != null) {
                double alt = d.air ? Math.max(d.baseY + 10.0, d.dest.y + 8.0) : veh.getY();
                steer(mob, level, d, veh, d.dest.x, d.dest.z, alt, vmax, now, 12.0, false);
                return PlanRunner.R.RUNNING;
            } else {
                double r = d.air ? 18.0 : 10.0;
                double[] aim = FlightMath.circleAim(d.center.x, d.center.z, r, veh.getX(), veh.getZ(), d.yawSet ? d.yaw : veh.getYRot(), 0.55);
                steer(mob, level, d, veh, aim[0], aim[1], d.baseY + 10.0, vmax, now, 0.0, false);
                return PlanRunner.R.RUNNING;
            }
        }
        // fase 2: volver junto al jugador y posarse
        long back = now - d.phaseStart;
        if (player != null && player.level() == level && back < 400L && toPlayer > 6.0) {
            double dx = veh.getX() - player.getX();
            double dz = veh.getZ() - player.getZ();
            double len = Math.max(0.01, Math.hypot(dx, dz));
            double ax = player.getX() + dx / len * 5.0;
            double az = player.getZ() + dz / len * 5.0;
            steer(mob, level, d, veh, ax, az, d.air ? Math.max(veh.getY(), d.baseY + 6.0) : veh.getY(), vmax, now, 10.0, false);
            return PlanRunner.R.RUNNING;
        }
        if (d.air && airborne(level, veh) && back < 900L) {
            steer(mob, level, d, veh, veh.getX(), veh.getZ(), groundY(level, veh), vmax, now, 1.0, true);
            return PlanRunner.R.RUNNING;
        }
        d.speed = 0.0;
        veh.setDeltaMovement(Vec3.ZERO);
        veh.hurtMarked = true;
        return PlanRunner.R.DONE;
    }

    /** Un tic de piloto automatico hacia (ax, az) a altura alt; landing = solo bajar. */
    private static void steer(Mob mob, ServerLevel level, Drive d, Entity veh, double ax, double az, double alt, double vmax, long now,
            double slowRadius, boolean landing) {
        Vec3 pos = veh.position();
        if (!d.yawSet) {
            d.yaw = veh.getYRot();
            d.yawSet = true;
        }
        double dx = ax - pos.x;
        double dz = az - pos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.6 && !landing) {
            d.yaw = FlightMath.turn(d.yaw, FlightMath.yawToward(dx, dz), d.air ? 9.0f : 14.0f);
        }
        double want = landing ? 0.0 : vmax;
        if (!landing && slowRadius > 0.0 && dist < slowRadius) {
            want = vmax * Math.max(0.15, dist / slowRadius);
        }
        if (now < d.climbUntil) {
            want *= 0.6;
        }
        d.blockedTicks = veh.horizontalCollision ? d.blockedTicks + 1 : 0;
        if (d.blockedTicks > 0) {
            want = Math.min(want, vmax * 0.3);
        }
        if (d.blockedTicks > 12 && !d.air) {
            d.yaw = FlightMath.wrap(d.yaw + 40.0f);
            d.blockedTicks = 0;
        }
        d.speed = FlightMath.ramp(d.speed, want, 0.03);
        double vy;
        if (d.air) {
            double lim = landing ? 0.3 : 0.32;
            vy = FlightMath.clamp((alt - pos.y) * 0.12, -lim, lim);
            if (landing) {
                vy = Math.min(vy, -0.12);
            }
            if (!landing) {
                Vec3 ahead = new Vec3(FlightMath.fx(d.yaw, 5.0), 0.0, FlightMath.fz(d.yaw, 5.0));
                if (!level.noCollision(veh, veh.getBoundingBox().move(ahead)) || d.blockedTicks > 2) {
                    d.climbUntil = now + 25L;
                }
                if (now < d.climbUntil) {
                    vy = Math.max(vy, 0.33);
                }
            }
        } else {
            d.vy = veh.onGround() ? (veh.horizontalCollision ? 0.5 : -0.02) : Math.max(d.vy - 0.08, -1.0);
            vy = d.strategy == 0 ? veh.getDeltaMovement().y : d.vy;
            if (d.strategy == 0 && veh.horizontalCollision && veh.onGround()) {
                vy = 0.5;
            }
        }
        Vec3 vel = new Vec3(FlightMath.fx(d.yaw, d.speed), vy, FlightMath.fz(d.yaw, d.speed));
        if (d.air && !d.gravityTouched) {
            d.gravityWas = veh.isNoGravity();
            d.gravityTouched = true;
        }
        if (d.air) {
            veh.setNoGravity(true);
        }
        veh.setYRot(d.yaw);
        veh.setXRot(d.air ? (float) FlightMath.clamp(-vy * 40.0, -15.0, 15.0) : 0.0f);
        if (veh instanceof LivingEntity le) {
            le.setYBodyRot(d.yaw);
            le.setYHeadRot(d.yaw);
        }
        switch (d.strategy) {
            case 0:
                veh.setDeltaMovement(vel);
                veh.hurtMarked = true;
                break;
            case 1:
                veh.setDeltaMovement(Vec3.ZERO);
                veh.move(MoverType.SELF, vel);
                veh.hurtMarked = true;
                break;
            default:
                if (level.noCollision(veh, veh.getBoundingBox().move(vel))) {
                    veh.setPos(veh.getX() + vel.x, veh.getY() + vel.y, veh.getZ() + vel.z);
                }
                veh.setDeltaMovement(Vec3.ZERO);
                veh.hurtMarked = true;
                break;
        }
        veh.fallDistance = 0.0f;
        mob.fallDistance = 0.0f;
        mob.setYRot(d.yaw);
        mob.setYHeadRot(d.yaw);
        mob.setYBodyRot(d.yaw);
        // si el vehiculo ignora la velocidad, se cambia de metodo (velocidad -> mover -> colocar)
        if (d.last == null || now - d.lastCheck >= 12L) {
            double cmd = vel.length();
            if (d.last != null && cmd > 0.1 && d.strategy < 2 && !veh.horizontalCollision && !veh.verticalCollision) {
                double moved = pos.distanceTo(d.last);
                if (moved < Math.max(0.3, cmd * 12.0 * 0.2)) {
                    ++d.strategy;
                    ActionLog.add(mob.getName().getString(), "PILOTAR", "el vehículo no respondía: método " + d.strategy);
                }
            }
            d.last = pos;
            d.lastCheck = now;
        }
    }
}
