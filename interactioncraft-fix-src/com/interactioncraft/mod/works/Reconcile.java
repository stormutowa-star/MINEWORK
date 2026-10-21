/*
 * Decompiled with CFR 0.152.
 */
package com.interactioncraft.mod.works;

import com.interactioncraft.mod.works.OrderParser;
import com.interactioncraft.mod.works.PlanSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Reconcile {
    private static final Set<String> PHYSICAL = Set.of("go", "kill", "attack", "fetch", "break", "place", "use", "give", "drop", "pickup", "sleep", "eat", "flee", "power", "equip", "ride", "drive", "dismount");

    private Reconcile() {
    }

    private static boolean has(List<PlanSpec.Step> steps, String ... ops) {
        for (PlanSpec.Step s : steps) {
            for (String o : ops) {
                if (!s.op().equals(o)) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean equivalent(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        Set<String> killish = Set.of("kill", "attack");
        Set<String> giveish = Set.of("give", "fetch", "drop");
        Set<String> useish = Set.of("use", "power");
        Set<String> rideish = Set.of("ride", "drive");
        return killish.contains(a) && killish.contains(b) || rideish.contains(a) && rideish.contains(b) || giveish.contains(a) && giveish.contains(b) || useish.contains(a) && useish.contains(b);
    }

    private static String json(List<PlanSpec.Step> steps) {
        return PlanSpec.toJson(steps);
    }

    public static Fixed fix(OrderParser.Result literal, OrderParser.Order ai) {
        boolean litLoot;
        if (literal == null || literal.orders().isEmpty() || ai == null) {
            return new Fixed(ai, "");
        }
        OrderParser.Order lit = literal.first();
        List<PlanSpec.Step> litSteps = lit.type().equals("plan") ? PlanSpec.parse(lit.steps()) : List.of();
        boolean litKill = Reconcile.has(litSteps, "kill");
        if (ai.type().equals("attack") && (litKill || Reconcile.has(litSteps, "attack"))) {
            return new Fixed(new OrderParser.Order("plan", lit.arg(), 0, "", "", "", false, "", lit.steps()), "el modelo puso attack pero se pidio " + (litKill ? "matar" : "atacar a algo concreto") + ": se usa el plan literal");
        }
        if (ai.type().equals("gather") && Reconcile.has(litSteps, "fetch", "give") && ai.then().isEmpty() && litSteps.size() <= 2) {
            return new Fixed(new OrderParser.Order("gather", ai.arg(), ai.amount(), "give", ai.spec(), ai.where(), ai.fresh(), ai.name(), ai.steps()), "recolectar y luego entregar al jugador");
        }
        if (!ai.type().equals("plan") && lit.type().equals("plan") && Reconcile.has(litSteps, "ride", "drive", "dismount")) {
            return new Fixed(new OrderParser.Order("plan", lit.arg(), 0, "", "", "", false, "", lit.steps()), "el modelo puso " + ai.type() + " pero se pidio montar/pilotar: se usa el plan literal");
        }
        if (!ai.type().equals("plan") || !lit.type().equals("plan")) {
            return new Fixed(ai, "");
        }
        ArrayList<PlanSpec.Step> steps = new ArrayList<PlanSpec.Step>(PlanSpec.parse(ai.steps()));
        if (steps.isEmpty()) {
            return new Fixed(ai, "");
        }
        ArrayList<String> notes = new ArrayList<String>();
        if (litKill && Reconcile.has(steps, "attack") && !Reconcile.has(steps, "kill")) {
            ArrayList<PlanSpec.Step> out = new ArrayList<PlanSpec.Step>();
            String target = "";
            for (PlanSpec.Step step : litSteps) {
                if (!step.op().equals("kill")) continue;
                target = step.target();
                break;
            }
            for (PlanSpec.Step step : steps) {
                out.add(step.op().equals("attack") ? new PlanSpec.Step("kill", step.target().isEmpty() ? target : step.target(), "", Math.max(1, step.n()), 90, "") : step);
            }
            steps = out;
            notes.add("attack -> kill");
        }
        boolean missingAll = true;
        boolean anyPhysical = false;
        for (PlanSpec.Step step : litSteps) {
            if (!PHYSICAL.contains(step.op())) continue;
            anyPhysical = true;
            for (PlanSpec.Step s : steps) {
                if (!Reconcile.equivalent(step.op(), s.op())) continue;
                missingAll = false;
            }
        }
        if (anyPhysical && missingAll) {
            return new Fixed(new OrderParser.Order("plan", ai.name().isBlank() ? lit.arg() : ai.name(), 0, "", "", "", false, ai.name(), lit.steps()), "el plan del modelo no hacia nada de lo pedido: se usa el literal");
        }
        if (litKill && !Reconcile.has(steps, "kill", "attack", "power")) {
            ArrayList<PlanSpec.Step> kills = new ArrayList<PlanSpec.Step>();
            for (PlanSpec.Step l : litSteps) {
                if (!l.op().equals("kill")) continue;
                kills.add(l);
            }
            steps.addAll(0, kills);
            notes.add("se anade matar (el plan no mataba)");
        }
        boolean bl = litLoot = Reconcile.has(litSteps, "pickup") && Reconcile.has(litSteps, "give");
        if (litKill && litLoot) {
            boolean bl2 = Reconcile.has(steps, "pickup", "fetch");
            boolean hasGive = Reconcile.has(steps, "give", "fetch", "drop");
            if (!bl2 && !hasGive) {
                steps.add(new PlanSpec.Step("pickup", "", "", 1, 0, "botin"));
                steps.add(new PlanSpec.Step("give", "jugador", "todo", 0, 0, ""));
                notes.add("se anade recoger el botin y darlo");
            } else if (!hasGive) {
                steps.add(new PlanSpec.Step("give", "jugador", "todo", 0, 0, ""));
                notes.add("se anade darlo");
            } else if (!bl2 && !Reconcile.has(steps, "fetch")) {
                int at = steps.size();
                for (int i = 0; i < steps.size(); ++i) {
                    if (!((PlanSpec.Step)steps.get(i)).op().equals("give")) continue;
                    at = i;
                    break;
                }
                steps.add(at, new PlanSpec.Step("pickup", "", "", 1, 0, "botin"));
                notes.add("se anade recoger el botin");
            }
        }
        if (Reconcile.has(litSteps, "fetch") && !litKill && !Reconcile.has(steps, "give", "fetch", "drop")) {
            for (PlanSpec.Step l : litSteps) {
                if (!l.op().equals("fetch")) continue;
                steps.add(l);
            }
            notes.add("se anade traerlo");
        }
        for (PlanSpec.Step l : litSteps) {
            if (!l.op().equals("ride") && !l.op().equals("drive") && !l.op().equals("dismount")) {
                continue;
            }
            boolean present = false;
            for (PlanSpec.Step s : steps) {
                present |= l.op().equals("drive") ? s.op().equals("drive") : Reconcile.equivalent(l.op(), s.op());
            }
            if (present) {
                continue;
            }
            if (l.op().equals("drive")) {
                steps.add(l);
            } else {
                steps.add(0, l);
            }
            notes.add("se anade " + PlanSpec.describe(l));
        }
        if (notes.isEmpty()) {
            return new Fixed(ai, "");
        }
        return new Fixed(new OrderParser.Order("plan", ai.arg(), ai.amount(), ai.then(), ai.spec(), ai.where(), ai.fresh(), ai.name(), Reconcile.json(steps)), String.join((CharSequence)"; ", notes));
    }

    public record Fixed(OrderParser.Order order, String note) {
    }
}

