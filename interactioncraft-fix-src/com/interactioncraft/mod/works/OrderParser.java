/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package com.interactioncraft.mod.works;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.interactioncraft.mod.works.BuildIntent;
import com.interactioncraft.mod.works.GatherSpec;
import com.interactioncraft.mod.works.ItemWords;
import com.interactioncraft.mod.works.PickupFilter;
import com.interactioncraft.mod.works.PlanSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OrderParser {
    private static final Result NONE = new Result(List.of(), false, false, "");
    private static final String[] PLACE_CUES = new String[]{"encima de", "sobre", "junto a", "al lado de", "delante de", "detras de", "dentro de", "fuera de", "por toda", "por todo", "por todas", "por todos", "alrededor de", "dentro", "adentro", "afuera", "aqui", "aca", "ahi", "alli", "alla", "donde", "contra", "en", "a tus pies", "debajo de", "cerca de", "pegado a", "pegada a"};
    private static final String[] PRONOUNS = new String[]{"me", "te", "nos", "lo", "la", "los", "las", "le", "les", "melo", "mela", "melos", "melas", "noslo", "nosla", "noslos", "noslas"};
    private static final Map<String, V> IMPERATIVE = new HashMap<String, V>();
    private static final Map<String, V> REQUESTED = new HashMap<String, V>();
    private static final Map<String, V> PRESENT = new HashMap<String, V>();
    private static final Map<String, Set<String>> NEEDS_NEXT = new HashMap<String, Set<String>>();
    private static final Set<String> LEAD_FILLER;
    private static final Set<String> NOT_NAME;
    private static final Set<String> CONNECTORS;
    private static final Set<String> CHAIN_SKIP;
    private static final Set<String> TRAIL;
    private static final Set<String> ARTICLES;
    private static final Set<String> NEGATORS;
    private static final List<String> LEAD_INS;
    private static final List<List<String>> LEAD_TOKENS;
    private static final Map<String, Integer> NUMBERS;
    private static final Set<String> POWER_WORDS;
    private static final Set<String> PLAYER_TOKENS;
    private static final Set<String> KNOWN_KEYWORDS;

    private OrderParser() {
    }

    private static Set<String> set(String ... words) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(words)));
    }

    private static void imp(V v, boolean enclitic, String ... bases) {
        for (String b : bases) {
            IMPERATIVE.put(b, v);
            if (!enclitic) continue;
            for (String p : PRONOUNS) {
                IMPERATIVE.putIfAbsent(b + p, v);
            }
        }
    }

    private static void exact(V v, String ... forms) {
        for (String f : forms) {
            IMPERATIVE.put(f, v);
        }
    }

    private static void req(V v, String ... forms) {
        for (String f : forms) {
            REQUESTED.put(f, v);
            for (String p : new String[]{"me", "te", "nos", "lo", "la", "los", "las", "le", "les", "melo", "mela", "telo", "tela"}) {
                if (!f.endsWith("r")) continue;
                REQUESTED.putIfAbsent(f + p, v);
            }
        }
    }

    private static void pres(V v, String ... forms) {
        for (String f : forms) {
            PRESENT.put(f, v);
        }
    }

    private static void needs(String form, String ... next) {
        NEEDS_NEXT.put(form, OrderParser.set(next));
    }

    private static List<String> tokens(String s) {
        ArrayList<String> out = new ArrayList<String>();
        for (String t : PickupFilter.norm(s).split(" ")) {
            if (t.isEmpty()) continue;
            out.add(t);
        }
        return out;
    }

    private static List<String> tokensWithBreaks(String s) {
        ArrayList<String> out = new ArrayList<String>();
        for (String part : s.split("[,;.!?\u00bf\u00a1:\\n\\r\u2026]+")) {
            List<String> t = OrderParser.tokens(part);
            if (t.isEmpty()) continue;
            if (!out.isEmpty()) {
                out.add(",");
            }
            out.addAll(t);
        }
        return out;
    }

    static int number(List<String> toks) {
        for (String t : toks) {
            if (t.matches("\\d{1,4}")) {
                return Integer.parseInt(t);
            }
            Integer n = NUMBERS.get(t);
            if (n == null) continue;
            return n;
        }
        for (String t : toks) {
            if (!t.equals("un") && !t.equals("una") && !t.equals("uno")) continue;
            return 1;
        }
        return -1;
    }



    /** "torre en la nave" -> {torre, nave}; "nave hacia la torre" -> {torre, nave}. [0] = destino, [1] = vehiculo. */
    private static String[] splitVehicle(String dest, String veh) {
        String d = " " + dest + " ";
        String v = " " + veh + " ";
        for (String sep : new String[]{" en ", " con ", " sobre ", " dentro de "}) {
            int at = d.indexOf(sep);
            if (at >= 0) {
                String tail = d.substring(at + sep.length()).trim();
                return new String[]{d.substring(0, at).trim(), tail.isEmpty() ? veh : OrderParser.vehicleObject(OrderParser.tokens(tail))};
            }
        }
        for (String sep : new String[]{" hacia ", " hasta ", " a ", " al "}) {
            int at = v.indexOf(sep);
            if (at >= 0) {
                String head = v.substring(0, at).trim();
                String tail = OrderParser.vehicleObject(OrderParser.tokens(v.substring(at + sep.length())));
                return new String[]{tail, head};
            }
        }
        return new String[]{dest.trim(), veh.trim()};
    }

    private static boolean vehicleVerb(V v) {
        return v == V.RIDE || v == V.DISMOUNT || v == V.DRIVE || v == V.TRY;
    }

    private static final Set<String> VEH_PREPS = Set.of("en", "a", "al", "sobre", "encima", "dentro", "hacia", "hasta", "con", "para", "de", "del", "por");
    private static final Set<String> VEH_PRONOUNS = Set.of("el", "ella", "ello", "lo", "la", "eso", "esto", "ahi", "alli", "aqui", "este", "esta", "ese", "esa", "mi", "ti");

    /** Nombre del vehiculo/destino de una clausula: sin preposiciones ni articulos iniciales; vacio si solo es un pronombre. */
    private static String vehicleObject(List<String> toks) {
        int i = 0;
        while (i < toks.size() && (VEH_PREPS.contains(toks.get(i)) || ARTICLES.contains(toks.get(i)) && !toks.get(i).equals("mi"))) {
            ++i;
        }
        List<String> rest = new ArrayList<String>(toks.subList(i, toks.size()));
        while (!rest.isEmpty() && (TRAIL.contains(rest.get(rest.size() - 1)) || Set.of("durante", "por", "unos", "unas", "aprox", "aproximadamente", "como").contains(rest.get(rest.size() - 1)) || rest.get(rest.size() - 1).matches("\\d{1,4}")
                || rest.get(rest.size() - 1).startsWith("segundo") || rest.get(rest.size() - 1).startsWith("minuto"))) {
            rest.remove(rest.size() - 1);
        }
        while (!rest.isEmpty() && rest.get(0).matches("\\d{1,4}")) {
            rest.remove(0);
        }
        if (rest.isEmpty() || VEH_PRONOUNS.containsAll(rest)) {
            return "";
        }
        return String.join(" ", rest);
    }

    private static boolean isVerbForm(String t) {
        return IMPERATIVE.containsKey(t) || REQUESTED.containsKey(t) || PRESENT.containsKey(t);
    }

    private static boolean negatedBefore(List<String> toks, int at) {
        for (int k = Math.max(0, at - 3); k < at; ++k) {
            if (!NEGATORS.contains(toks.get(k))) continue;
            return true;
        }
        return false;
    }

    private static boolean atStart(List<String> toks, int i) {
        int k;
        int unknown = 0;
        int from = 0;
        for (k = 0; k < i; ++k) {
            if (!toks.get(k).equals(",")) continue;
            from = k + 1;
        }
        for (k = from; k < i; ++k) {
            String w = toks.get(k);
            if (LEAD_FILLER.contains(w) || CONNECTORS.contains(w)) continue;
            if (NOT_NAME.contains(w) || OrderParser.isVerbForm(w)) {
                return false;
            }
            ++unknown;
        }
        return unknown <= 2;
    }

    private static boolean matches(List<String> toks, int s, List<String> p) {
        if (s < 0 || s + p.size() > toks.size()) {
            return false;
        }
        for (int k = 0; k < p.size(); ++k) {
            if (toks.get(s + k).equals(p.get(k))) continue;
            return false;
        }
        return true;
    }

    private static boolean afterLeadIn(List<String> toks, int i) {
        for (int s = Math.max(0, i - 6); s < i; ++s) {
            for (List<String> p : LEAD_TOKENS) {
                if (s + p.size() > i || !OrderParser.matches(toks, s, p)) continue;
                boolean clean = true;
                for (int k = s + p.size(); k < i; ++k) {
                    String w = toks.get(k);
                    if (w.equals("que") || w.equals("me") || w.equals("te") || w.equals("nos") || w.equals("lo") || w.equals("la") || w.equals("ya") || w.equals("a") || w.equals("ahora") || w.equals("favor") || w.equals("por") || w.equals("mismo") || w.equals("de") || w.equals("el") || w.equals("un") || w.equals("poco") || w.equals("no")) continue;
                    clean = false;
                    break;
                }
                if (!clean) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean INFINITIVE_ENDINGS(String t) {
        return t.endsWith("r") || t.endsWith("rme") || t.endsWith("rte") || t.endsWith("rle") || t.endsWith("rlo") || t.endsWith("rla") || t.endsWith("rnos");
    }

    private static boolean nextAllowed(List<String> toks, int i, String form) {
        String next;
        Set<String> nx = NEEDS_NEXT.get(form);
        if (nx == null) {
            return true;
        }
        String string = next = i + 1 < toks.size() ? toks.get(i + 1) : "";
        if ((form.equals("pon") || form.equals("pone")) && (next.matches("\\d{1,3}") || NUMBERS.containsKey(next))) {
            return true;
        }
        return nx.contains(next);
    }

    private static List<Verb> verbs(List<String> toks, boolean question) {
        ArrayList<Verb> out = new ArrayList<Verb>();
        boolean chain = false;
        int i = 0;
        while (i < toks.size()) {
            String t = toks.get(i);
            if (t.equals(",")) {
                chain = true;
                ++i;
                continue;
            }
            boolean inOrder = !out.isEmpty();
            boolean start = chain || OrderParser.atStart(toks, i);
            boolean lead = OrderParser.afterLeadIn(toks, i);
            int nxt = i + 1;
            String n1 = nxt < toks.size() ? toks.get(nxt) : "";
            String n2 = nxt + 1 < toks.size() ? toks.get(nxt + 1) : "";
            boolean handled = false;
            if ((t.equals("acaba") || t.equals("acabar") || t.equals("acabes")) && n1.equals("con") && (start || lead)) {
                out.add(new Verb(i, 2, V.KILL, OrderParser.negatedBefore(toks, i), "acabar con"));
                i += 2;
                handled = true;
            } else if ((t.equals("ve") || t.equals("ir") || t.equals("anda") || t.equals("vayas") || t.equals("ven") || t.equals("vete")) && n1.equals("a") && (n2.equals("por") || n2.equals("buscar") || n2.equals("traer") || n2.equals("conseguir")) && (start || lead)) {
                out.add(new Verb(i, 3, V.FETCH, OrderParser.negatedBefore(toks, i), "ir a por"));
                i += 3;
                handled = true;
            } else if ((t.equals("deja") || t.equals("dejar") || t.equals("dejes") || t.equals("para") || t.equals("parar") || t.equals("pares")) && n1.equals("de") && (start || lead)) {
                out.add(new Verb(i, 2, V.STOP, OrderParser.negatedBefore(toks, i), "dejar de"));
                i += 2;
                handled = true;
            } else if (t.equals("no") && (n1.equals("sigas") || n1.equals("sigan"))) {
                out.add(new Verb(i + 1, 1, V.STOP, false, "no sigas"));
                i += 2;
                handled = true;
            } else if (t.equals("no") && n1.equals("me") && n2.equals("sigas")) {
                out.add(new Verb(i + 2, 1, V.STOP, false, "no me sigas"));
                i += 3;
                handled = true;
            } else if (t.equals("da") && (n1.equals("vueltas") || n1.equals("una") && (n2.equals("vuelta") || n2.equals("giro"))) && (start || lead)) {
                out.add(new Verb(i, 1, V.SPIN, OrderParser.negatedBefore(toks, i), "da vueltas"));
                ++i;
                handled = true;
            } else if (t.equals("da") && (n1.equals("saltos") || n1.equals("un") && n2.equals("salto")) && (start || lead)) {
                out.add(new Verb(i, 1, V.JUMP, OrderParser.negatedBefore(toks, i), "da saltos"));
                ++i;
                handled = true;
            } else if (t.equals("di") && n1.equals("hola") && (start || lead)) {
                out.add(new Verb(i, 2, V.GREET, false, "di hola"));
                i += 2;
                handled = true;
            }
            if (handled) {
                chain = false;
                continue;
            }
            V v = null;
            V imp = IMPERATIVE.get(t);
            V rq = REQUESTED.get(t);
            V pr = PRESENT.get(t);
            if (imp != null && (start || lead) && OrderParser.nextAllowed(toks, i, t)) {
                v = imp;
            } else if (imp != null && OrderParser.vehicleVerb(imp) && !out.isEmpty() && OrderParser.vehicleVerb(out.get(out.size() - 1).v()) && OrderParser.nextAllowed(toks, i, t)) {
                v = imp;
            } else if (rq != null && (lead || chain && inOrder)) {
                v = rq;
            } else if (pr != null && (chain && inOrder && !question || i > 0 && toks.get(i - 1).equals("me") && OrderParser.atStart(toks, i - 1))) {
                v = pr;
            } else if (rq != null && i > 0 && NEGATORS.contains(toks.get(i - 1)) && !OrderParser.INFINITIVE_ENDINGS(t)) {
                out.add(new Verb(i, 1, rq, true, t));
                ++i;
                continue;
            }
            if (v != null) {
                out.add(new Verb(i, 1, v, OrderParser.negatedBefore(toks, i), t));
                chain = false;
                ++i;
                continue;
            }
            if (CONNECTORS.contains(t)) {
                chain = true;
            } else if (!CHAIN_SKIP.contains(t)) {
                chain = false;
            }
            ++i;
        }
        return out;
    }

    private static boolean isPower(String obj) {
        String padded = " " + obj + " ";
        if (padded.contains(" contra ")) {
            return true;
        }
        for (String w : obj.split(" ")) {
            if (!POWER_WORDS.contains(w) || w.equals("ataque") || w.equals("especial")) continue;
            return true;
        }
        return padded.contains(" ataque especial ");
    }

    private static List<String> clauseText(List<String> toks, List<Verb> verbs, int idx) {
        Verb v = verbs.get(idx);
        int from = Math.min(v.at() + v.len(), toks.size());
        int to = idx + 1 < verbs.size() ? verbs.get(idx + 1).at() : toks.size();
        ArrayList<String> out = new ArrayList<String>(toks.subList(from, Math.max(from, to)));
        while (!out.isEmpty() && TRAIL.contains(out.get(out.size() - 1))) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private static String object(List<String> toks) {
        ArrayList<String> out = new ArrayList<String>();
        boolean started = false;
        for (String t : toks) {
            if (!started && (ARTICLES.contains(t) || t.matches("\\d{1,4}") || NUMBERS.containsKey(t) && !t.equals("par") && !t.equals("docena"))) continue;
            started = true;
            out.add(t);
        }
        while (!out.isEmpty() && TRAIL.contains(out.get(out.size() - 1))) {
            out.remove(out.size() - 1);
        }
        return String.join((CharSequence)" ", out);
    }

    private static boolean pronounOnly(List<String> toks) {
        for (String t : toks) {
            if (t.equals("eso") || t.equals("esto") || t.equals("todo") || t.equals("ello") || ARTICLES.contains(t)) continue;
            return false;
        }
        return true;
    }

    private static JsonObject step(String op, String key, String value) {
        JsonObject o = new JsonObject();
        o.addProperty("op", op);
        if (key != null && value != null && !value.isEmpty()) {
            o.addProperty(key, value);
        }
        return o;
    }

    private static JsonObject step(String op, String key, String value, String numKey, int num) {
        JsonObject o = OrderParser.step(op, key, value);
        o.addProperty(numKey, (Number)num);
        return o;
    }

    private static boolean has(String flat, String ... words) {
        String n = " " + flat + " ";
        for (String w : words) {
            if (!n.contains(w)) continue;
            return true;
        }
        return false;
    }

    private static void addGo(JsonArray steps, List<String> names, String obj, String flat, boolean motionVerb) {
        boolean player;
        String target = obj;
        if (motionVerb && PickupFilter.norm(flat).matches("(ahi|alli|alla|aya|aca)( .*)?")) {
            target = "donde miro";
        }
        if (!(player = target.isEmpty())) {
            String[] parts = target.split(" ");
            player = parts.length <= 3;
            for (String p : parts) {
                player &= PLAYER_TOKENS.contains(p) || p.equals("donde") || p.equals("estoy") || p.equals("estes") || p.equals("yo");
            }
        }
        steps.add((JsonElement)OrderParser.step("go", "target", player ? "jugador" : target));
        names.add((String)(player ? "ir contigo" : "ir a " + target));
    }

    private static boolean materialWord(String text) {
        GatherSpec g = GatherSpec.parse(text);
        return g.kind != GatherSpec.Kind.KEYWORD || KNOWN_KEYWORDS.contains(g.keyword);
    }

    private static int secondsIn(List<String> toks) {
        int i = 0;
        while (i + 1 < toks.size()) {
            int v;
            String t = toks.get(i);
            int n = v = t.matches("\\d{1,4}") ? Integer.parseInt(t) : NUMBERS.getOrDefault(t, -1);
            if (v > 0) {
                String u = toks.get(i + 1);
                if (u.startsWith("segundo") || u.equals("seg") || u.equals("s")) {
                    return v;
                }
                if (u.startsWith("minuto") || u.equals("min")) {
                    return v * 60;
                }
            }
            ++i;
        }
        return -1;
    }

    public static Result parse(String message) {
        boolean planFirst;
        if (message == null || message.isBlank()) {
            return NONE;
        }
        boolean question = message.contains("?");
        List<String> toks = OrderParser.tokensWithBreaks(message);
        if (toks.isEmpty() || toks.size() > 80) {
            return NONE;
        }
        List<Verb> all = OrderParser.verbs(toks, question);
        boolean negated = false;
        ArrayList<Verb> live = new ArrayList<Verb>();
        for (Verb v : all) {
            if (v.negated() && v.v() != V.STOP) {
                negated = true;
                continue;
            }
            live.add(v);
        }
        if (live.isEmpty()) {
            return negated ? new Result(List.of(), false, true, "") : NONE;
        }
        JsonArray steps = new JsonArray();
        ArrayList<Order> singles = new ArrayList<Order>();
        ArrayList<String> names = new ArrayList<String>();
        boolean lastKill = false;
        String carry = "";
        String carryVehicle = "";
        String buildSpec = null;
        boolean buildFresh = false;
        block29: for (int idx = 0; idx < live.size(); ++idx) {
            Verb verb = (Verb)live.get(idx);
            List<String> raw = OrderParser.clauseText(toks, live, idx);
            ArrayList<String> text = new ArrayList<String>(raw);
            text.removeIf(w -> w.equals(","));
            String obj = OrderParser.object((List<String>)text);
            int n = OrderParser.number((List<String>)text);
            String flat = String.join((CharSequence)" ", (Iterable<? extends CharSequence>)text);
            boolean more = idx + 1 < live.size();
            switch (verb.v().ordinal()) {
                case 0: 
                case 1: {
                    boolean aboutMe;
                    boolean kill = verb.v() == V.KILL;
                    String padded = " " + flat + " ";
                    String o2 = obj.replace("contra ", "").replace("a ", "").trim();
                    boolean bl = aboutMe = obj.isEmpty() || o2.equals("mi") || o2.equals("conmigo") || o2.equals("contigo") || o2.equals("jugador");
                    if (verb.form().endsWith("me") && !verb.form().equals("me") && obj.isEmpty() || aboutMe && OrderParser.has(padded, " conmigo ", " contigo ", " a mi ", " contra mi ") || verb.form().equals("duelo") && aboutMe) {
                        obj = "jugador";
                    }
                    if (obj.isEmpty()) {
                        obj = kill ? "esa criatura" : "amenaza";
                    }
                    JsonObject s = OrderParser.step(kill ? "kill" : "attack", "target", obj);
                    if (obj.equals("jugador")) {
                        s.addProperty("secs", (Number)30);
                    }
                    if (kill && !PlanSpec.demonstrative(obj)) {
                        if (OrderParser.has(flat, " todos ", " todas ")) {
                            s.addProperty("times", (Number)12);
                        } else if (n > 1) {
                            s.addProperty("times", (Number)Math.min(n, 12));
                        }
                    }
                    steps.add((JsonElement)s);
                    names.add(kill ? "matar" : "atacar");
                    lastKill = true;
                    continue block29;
                }
                case 2: {
                    boolean loot;
                    boolean bl = loot = lastKill && (obj.isEmpty() || PlanSpec.allWords(flat) || OrderParser.pronounOnly((List<String>)text));
                    if (loot) {
                        steps.add((JsonElement)OrderParser.step("pickup", "text", "botin"));
                        JsonObject give = OrderParser.step("give", "item", "todo", "times", 0);
                        give.addProperty("target", "jugador");
                        steps.add((JsonElement)give);
                        names.add("traer el bot\u00edn");
                        lastKill = false;
                        continue block29;
                    }
                    if (obj.isEmpty()) continue block29;
                    if (obj.startsWith("vuelta") || obj.startsWith("paseo") || obj.startsWith("viaje")) {
                        JsonObject prev = steps.size() > 0 ? steps.get(steps.size() - 1).getAsJsonObject() : null;
                        String prevOp = prev != null && prev.has("op") ? prev.get("op").getAsString() : "";
                        if (prevOp.equals("ride")) {
                            steps.add((JsonElement)OrderParser.step("drive", "item", carryVehicle));
                            names.add("dar una vuelta");
                        }
                        continue block29;
                    }
                    JsonObject f = OrderParser.step("fetch", "item", PlanSpec.allWords(flat) ? flat : obj);
                    if (n > 0) {
                        f.addProperty("times", (Number)Math.min(n, 64));
                    }
                    f.addProperty("target", "jugador");
                    steps.add((JsonElement)f);
                    names.add(verb.form().startsWith("da") || verb.form().startsWith("entrega") || verb.form().startsWith("regal") ? "dar " + obj : "traer " + obj);
                    lastKill = false;
                    continue block29;
                }
                case 3: 
                case 4: {
                    boolean motion;
                    boolean bl = motion = obj.isEmpty() && more || flat.equals("a") && more;
                    if (motion) continue block29;
                    OrderParser.addGo(steps, names, obj, flat, verb.v() == V.GO);
                    lastKill = false;
                    continue block29;
                }
                case 5: {
                    if (verb.form().equals("sigue") && !text.contains("me") && (!text.contains("a") || text.contains("construyendo") || text.contains("con"))) {
                        if (BuildIntent.kindOf(flat).isEmpty() && !OrderParser.has(flat, " obra ", " constru", " edific ", " lo que ")) continue block29;
                        buildSpec = "";
                        names.add("continuar la obra");
                        continue block29;
                    }
                    singles.add(Order.simple("follow"));
                    names.add("seguirte");
                    continue block29;
                }
                case 6: {
                    boolean me;
                    boolean bl = me = verb.form().endsWith("me") || verb.form().endsWith("nos") || text.contains("me") || text.contains("mi") || text.contains("nos") || obj.isEmpty() || obj.contains("jugador");
                    if (!me) continue block29;
                    singles.add(Order.simple("guard"));
                    names.add("protegerte");
                    continue block29;
                }
                case 7: {
                    singles.add(Order.simple("stop"));
                    names.add("parar");
                    continue block29;
                }
                case 29: {
                    String vq = OrderParser.vehicleObject(text);
                    String first = text.isEmpty() ? "" : (String)text.get(0);
                    boolean prep = first.equals("en") || first.equals("a") || first.equals("al") || first.equals("sobre") || first.equals("encima") || first.equals("dentro");
                    boolean enclitic = verb.form().endsWith("te") || verb.form().endsWith("se") || verb.form().endsWith("lo") || verb.form().endsWith("la");
                    if (!prep && !enclitic && !vq.isEmpty() && !BuildIntent.kindOf(flat).isEmpty()) continue block29;
                    if (!prep && !enclitic && !verb.form().startsWith("sube") && !verb.form().startsWith("subir") && vq.isEmpty() && !more) {
                        if (!verb.form().startsWith("monta") && !verb.form().startsWith("cabalg") && !verb.form().startsWith("abord") && !verb.form().startsWith("embarc")) continue block29;
                    }
                    if (!vq.isEmpty()) {
                        carryVehicle = vq;
                    }
                    steps.add((JsonElement)OrderParser.step("ride", "target", vq));
                    names.add("montarse" + (vq.isEmpty() ? "" : " en " + vq));
                    lastKill = false;
                    continue block29;
                }
                case 30: {
                    steps.add((JsonElement)OrderParser.step("dismount", null, null));
                    names.add("bajarse");
                    lastKill = false;
                    continue block29;
                }
                case 31: {
                    String dq = OrderParser.vehicleObject(text);
                    String first = text.isEmpty() ? "" : (String)text.get(0);
                    boolean toward = first.equals("hacia") || first.equals("hasta") || first.equals("a") || first.equals("al") || first.equals("para");
                    String destination = toward ? dq : "";
                    String vehicle = toward ? "" : dq;
                    String[] split = OrderParser.splitVehicle(toward ? dq : "", toward ? "" : dq);
                    destination = split[0];
                    vehicle = split[1];
                    if (vehicle.isEmpty()) {
                        vehicle = carryVehicle;
                    } else {
                        carryVehicle = vehicle;
                    }
                    int dsecs = OrderParser.secondsIn((List<String>)text);
                    String form = verb.form();
                    String mode = form.startsWith("aterri") ? "aterrizar" : (form.startsWith("vuel") || form.startsWith("volar") || form.startsWith("despeg") ? "vuelo" : "");
                    JsonObject last = steps.size() > 0 ? steps.get(steps.size() - 1).getAsJsonObject() : null;
                    JsonObject drv = last != null && last.has("op") && last.get("op").getAsString().equals("drive") ? last : null;
                    if (drv == null) {
                        drv = OrderParser.step("drive", "item", vehicle);
                        steps.add((JsonElement)drv);
                        names.add(form.startsWith("aterri") ? "aterrizar" : "pilotar" + (vehicle.isEmpty() ? "" : " " + vehicle));
                    } else if (!vehicle.isEmpty() && !drv.has("item")) {
                        drv.addProperty("item", vehicle);
                    }
                    if (!destination.isEmpty()) {
                        drv.addProperty("target", destination);
                    }
                    if (dsecs > 0) {
                        drv.addProperty("secs", (Number)Math.min(dsecs, 180));
                    }
                    if (!mode.isEmpty() && (!drv.has("text") || mode.equals("aterrizar"))) {
                        drv.addProperty("text", mode);
                    }
                    lastKill = false;
                    continue block29;
                }
                case 32: {
                    JsonObject last = steps.size() > 0 ? steps.get(steps.size() - 1).getAsJsonObject() : null;
                    String lastOp = last != null && last.has("op") ? last.get("op").getAsString() : "";
                    if (!lastOp.equals("ride") && !lastOp.equals("drive")) continue block29;
                    String tq = OrderParser.vehicleObject(text);
                    if (tq.isEmpty()) {
                        tq = carryVehicle;
                    } else {
                        carryVehicle = tq;
                    }
                    if (lastOp.equals("ride")) {
                        steps.add((JsonElement)OrderParser.step("drive", "item", tq));
                        names.add("probar " + (tq.isEmpty() ? "el vehiculo" : tq));
                    }
                    continue block29;
                }
                case 33: {
                    String what = obj.replace("todo ", "todo ").trim();
                    steps.add((JsonElement)OrderParser.step("cook", "item", what));
                    names.add("cocinar" + (what.isEmpty() ? "" : " " + what));
                    lastKill = false;
                    continue block29;
                }
                case 34: {
                    steps.add((JsonElement)(n > 0 ? OrderParser.step("fish", null, null, "times", Math.min(n, 12)) : OrderParser.step("fish", null, null)));
                    names.add("pescar");
                    lastKill = false;
                    continue block29;
                }
                case 35: {
                    String[] split = OrderParser.splitVehicle(obj, "");
                    String thing = obj;
                    String where = "";
                    String padded = " " + obj + " ";
                    for (String sep : new String[]{" en ", " sobre ", " cerca de ", " junto a "}) {
                        int at = padded.indexOf(sep);
                        if (at >= 0) {
                            thing = padded.substring(0, at).trim();
                            where = padded.substring(at + sep.length()).trim();
                            break;
                        }
                    }
                    JsonObject pl = OrderParser.step("plant", "item", thing);
                    if (!where.isEmpty()) {
                        pl.addProperty("target", where);
                    }
                    if (n > 0) {
                        pl.addProperty("times", (Number)Math.min(n, 64));
                    }
                    steps.add((JsonElement)pl);
                    names.add("plantar" + (thing.isEmpty() ? "" : " " + thing));
                    lastKill = false;
                    continue block29;
                }
                case 21: {
                    int secs = OrderParser.secondsIn((List<String>)text);
                    if (secs > 0) {
                        steps.add((JsonElement)OrderParser.step("wait", null, null, "secs", Math.min(secs, 60)));
                        names.add("esperar " + secs + " s");
                        continue block29;
                    }
                    if (live.size() != 1) continue block29;
                    singles.add(Order.simple("stop"));
                    names.add("esperar");
                    continue block29;
                }
                case 8: {
                    if (obj.isEmpty()) continue block29;
                    obj = OrderParser.object(raw).replace(" ,", ",");
                    if (OrderParser.materialWord(obj)) {
                        singles.add(new Order("gather", obj, n > 0 ? Math.min(n, 256) : 0, "", "", "", false, "", ""));
                        names.add("recolectar " + obj);
                    } else {
                        steps.add((JsonElement)OrderParser.step("break", "target", obj, "times", n > 0 ? Math.min(n, 48) : 4));
                        names.add("recolectar " + obj);
                    }
                    lastKill = false;
                    continue block29;
                }
                case 9: 
                case 27: {
                    String kind = BuildIntent.kindOf(flat);
                    if (verb.v() == V.CONTINUE) {
                        if (kind.isEmpty() && !OrderParser.has(flat, " obra ", " constru", " edific", " lo que ")) continue block29;
                        buildSpec = "";
                        names.add("continuar la obra");
                    } else {
                        boolean strong;
                        boolean bl = strong = verb.form().startsWith("constru") || verb.form().startsWith("edific") || verb.form().startsWith("erig");
                        if (kind.isEmpty() && !strong) continue block29;
                        buildSpec = obj;
                        names.add("construir" + (String)(obj.isEmpty() ? "" : " " + obj));
                    }
                    buildFresh = (" " + flat + " ").matches(".* (otra|otro|nueva|nuevo|distinta|distinto) .*");
                    continue block29;
                }
                case 10: {
                    if (more && ((Verb)live.get(idx + 1)).v() == V.EQUIP) {
                        carry = obj;
                        continue block29;
                    }
                    singles.add(new Order("pickup", obj, 0, "", "", "", false, "", ""));
                    names.add("recoger " + (obj.isEmpty() ? "lo del suelo" : obj));
                    continue block29;
                }
                case 11: {
                    String padded;
                    if (obj.isEmpty()) continue block29;
                    if (OrderParser.isPower(obj)) {
                        String padded2 = " " + obj + " ";
                        String name = obj;
                        String aim = "";
                        for (String sep : new String[]{" contra ", " sobre ", " hacia ", " a ", " en "}) {
                            int w2 = padded2.indexOf(sep);
                            if (w2 < 1) continue;
                            name = padded2.substring(0, w2).trim();
                            aim = padded2.substring(w2 + sep.length()).trim();
                            break;
                        }
                        JsonObject pw = OrderParser.step("power", "text", name);
                        if (!aim.isEmpty()) {
                            pw.addProperty("target", aim);
                        }
                        steps.add((JsonElement)pw);
                        names.add("usar " + name + (String)(aim.isEmpty() ? "" : " contra " + aim));
                        lastKill = false;
                        continue block29;
                    }
                    String item = "";
                    int w3 = (" " + obj + " ").indexOf(" con ");
                    if (w3 >= 0) {
                        padded = " " + obj + " ";
                        item = padded.substring(w3 + 5).trim();
                        obj = padded.substring(0, w3).trim();
                    }
                    JsonObject u = OrderParser.step("use", "target", obj);
                    if (!item.isEmpty()) {
                        u.addProperty("item", item);
                    }
                    steps.add((JsonElement)u);
                    names.add((verb.form().startsWith("abr") ? "abrir " : (verb.form().startsWith("cierr") || verb.form().startsWith("cerr") ? "cerrar " : "usar ")) + obj);
                    lastKill = false;
                    continue block29;
                }
                case 12: {
                    if (obj.isEmpty()) continue block29;
                    JsonObject b = OrderParser.step("break", "target", obj);
                    if (n > 0) {
                        b.addProperty("times", (Number)Math.min(n, 48));
                    }
                    steps.add((JsonElement)b);
                    names.add("romper " + obj);
                    lastKill = false;
                    continue block29;
                }
                case 13: {
                    if (obj.isEmpty()) continue block29;
                    String item = obj;
                    String target = "";
                    String padded = " " + obj + " ";
                    int cut = -1;
                    String cue = "";
                    for (String sep : PLACE_CUES) {
                        int w4 = padded.indexOf(" " + sep + " ");
                        if (w4 < 1 || cut >= 0 && w4 >= cut) continue;
                        cut = w4;
                        cue = sep;
                    }
                    if (cut >= 1) {
                        item = padded.substring(0, cut).trim();
                        target = (cue + " " + padded.substring(cut + cue.length() + 2)).trim();
                    }
                    if (cut < 0) {
                        for (String adv : new String[]{"aqui", "aca", "ahi", "alli", "alla", "dentro", "adentro", "afuera"}) {
                            if (!padded.startsWith(" " + adv + " ") || padded.length() <= adv.length() + 3) continue;
                            item = padded.substring(adv.length() + 2).trim();
                            target = adv;
                            break;
                        }
                    }
                    JsonObject p = OrderParser.step("place", "item", item);
                    if (!target.isEmpty()) {
                        p.addProperty("target", target);
                    }
                    if (n > 0) {
                        p.addProperty("times", (Number)Math.min(n, 64));
                    }
                    steps.add((JsonElement)p);
                    names.add("poner " + item);
                    lastKill = false;
                    continue block29;
                }
                case 14: {
                    if (verb.form().equals("tira") && text.size() > 1 && ((String)text.get(0)).equals("de")) {
                        steps.add((JsonElement)OrderParser.step("use", "target", OrderParser.object(text.subList(1, text.size()))));
                        names.add("tirar de la palanca");
                        continue block29;
                    }
                    steps.add((JsonElement)(n > 0 ? OrderParser.step("drop", "item", obj.isEmpty() ? "todo" : obj, "times", Math.min(n, 64)) : OrderParser.step("drop", "item", obj.isEmpty() ? "todo" : obj)));
                    names.add("soltar " + (obj.isEmpty() ? "todo" : obj));
                    continue block29;
                }
                case 15: {
                    int secs = OrderParser.secondsIn((List<String>)text);
                    steps.add((JsonElement)OrderParser.step("sleep", "target", "cama", "secs", secs > 0 ? Math.min(secs, 120) : 10));
                    names.add("dormir");
                    continue block29;
                }
                case 28: {
                    String hand;
                    String[] parts = ItemWords.splitHand(obj);
                    String what = parts[0].isEmpty() || ItemWords.pronounOnly(parts[0]) ? (carry.isEmpty() ? "lo que te di" : carry) : parts[0];
                    JsonObject eq = OrderParser.step("equip", "item", what);
                    String string = hand = parts[1].isEmpty() ? ItemWords.splitHand(flat)[1] : parts[1];
                    if (!hand.isEmpty()) {
                        eq.addProperty("text", hand);
                    }
                    steps.add((JsonElement)eq);
                    names.add("equiparse " + what);
                    lastKill = false;
                    continue block29;
                }
                case 16: {
                    steps.add((JsonElement)OrderParser.step("eat", "item", obj));
                    names.add("comer" + (String)(obj.isEmpty() ? "" : " " + obj));
                    continue block29;
                }
                case 17: {
                    boolean me = verb.form().endsWith("me") || text.contains("me") || obj.isEmpty() || PLAYER_TOKENS.contains(obj);
                    steps.add((JsonElement)OrderParser.step("look", "target", me ? "jugador" : obj, "secs", 3));
                    names.add("mirar " + (me ? "al jugador" : obj));
                    continue block29;
                }
                case 18: {
                    steps.add((JsonElement)OrderParser.step("jump", null, null, "times", n > 0 ? Math.min(n, 20) : 1));
                    names.add("saltar");
                    continue block29;
                }
                case 19: {
                    steps.add((JsonElement)OrderParser.step("spin", null, null, "times", n > 0 ? Math.min(n, 10) : 1));
                    names.add("girar");
                    continue block29;
                }
                case 20: {
                    steps.add((JsonElement)OrderParser.step("crouch", null, null, "secs", Math.max(1, Math.min(OrderParser.secondsIn((List<String>)text) > 0 ? OrderParser.secondsIn((List<String>)text) : 4, 60))));
                    names.add("agacharse");
                    continue block29;
                }
                case 22: {
                    if (verb.form().equals("corre") && (text.contains("hacia") || text.contains("a") || text.contains("al")) && !obj.isEmpty()) {
                        OrderParser.addGo(steps, names, obj, flat, false);
                    } else {
                        steps.add((JsonElement)OrderParser.step("flee", "target", obj));
                        names.add("huir");
                    }
                    lastKill = false;
                    continue block29;
                }
                case 23: {
                    steps.add((JsonElement)OrderParser.step("jump", null, null, "times", 2));
                    steps.add((JsonElement)OrderParser.step("spin", null, null, "times", 2));
                    steps.add((JsonElement)OrderParser.step("jump", null, null, "times", 2));
                    steps.add((JsonElement)OrderParser.step("emote", "text", "m\u00fasica", "secs", 3));
                    names.add("bailar");
                    continue block29;
                }
                case 24: {
                    steps.add((JsonElement)OrderParser.step("look", "target", "jugador", "secs", 1));
                    steps.add((JsonElement)OrderParser.step("swing", null, null, "times", 4));
                    steps.add((JsonElement)OrderParser.step("emote", "text", "alegr\u00eda", "secs", 2));
                    names.add("saludar");
                    continue block29;
                }
                case 25: {
                    steps.add((JsonElement)OrderParser.step("jump", null, null, "times", 3));
                    steps.add((JsonElement)OrderParser.step("emote", "text", "fiesta", "secs", 3));
                    steps.add((JsonElement)OrderParser.step("sound", "text", "celebrar"));
                    names.add("celebrar");
                    continue block29;
                }
                case 26: {
                    steps.add((JsonElement)OrderParser.step("swing", null, null, "times", 8));
                    names.add("aplaudir");
                    continue block29;
                }
            }
        }
        if (!carryVehicle.isEmpty()) {
            for (int k = 0; k < steps.size(); ++k) {
                JsonObject st = steps.get(k).getAsJsonObject();
                if (st.has("op") && st.get("op").getAsString().equals("ride") && !st.has("target")) {
                    st.addProperty("target", carryVehicle);
                }
            }
        }
        ArrayList<Order> orders = new ArrayList<Order>();
        String title = String.join((CharSequence)" y ", OrderParser.dedupe(names));
        boolean bl = planFirst = steps.size() > 0;
        if (planFirst) {
            orders.add(Order.plan(title, steps.toString()));
        }
        for (Order s : singles) {
            if (!orders.stream().noneMatch(o -> o.type().equals(s.type()))) continue;
            orders.add(s);
        }
        if (buildSpec != null) {
            boolean merged = false;
            for (int k = 0; k < orders.size() && !merged; ++k) {
                Order o2 = (Order)orders.get(k);
                if (!o2.type().equals("gather") && !o2.type().equals("pickup")) continue;
                orders.set(k, new Order(o2.type(), o2.arg(), o2.amount(), "build", buildSpec, "", buildFresh, "", ""));
                merged = true;
            }
            if (!merged && !planFirst) {
                orders.add(0, new Order("build", "", 0, "", buildSpec, "", buildFresh, "", ""));
            }
        }
        if (orders.size() > 1) {
            orders = new ArrayList<Order>(List.of((Order)orders.get(0)));
        }
        return new Result(orders, true, negated, OrderParser.summarize(orders));
    }

    private static List<String> dedupe(List<String> in) {
        ArrayList<String> out = new ArrayList<String>();
        for (String s : in) {
            if (out.contains(s)) continue;
            out.add(s);
        }
        return out;
    }

    public static String summarize(List<Order> orders) {
        ArrayList<String> parts = new ArrayList<String>();
        block18: for (Order o : orders) {
            switch (o.type()) {
                case "plan": {
                    ArrayList<String> s = new ArrayList<String>();
                    for (PlanSpec.Step st : PlanSpec.parse(o.steps())) {
                        s.add(PlanSpec.describe(st));
                    }
                    parts.add("acci\u00f3n libre [" + String.join((CharSequence)" \u2192 ", s) + "]");
                    continue block18;
                }
                case "gather": {
                    parts.add("recolectar " + o.arg() + (String)(o.amount() > 0 ? " (" + o.amount() + ")" : "") + (o.then().equals("build") ? " y luego construir" : ""));
                    continue block18;
                }
                case "build": {
                    parts.add(o.spec().isEmpty() ? "continuar la obra a medias" : "construir " + o.spec());
                    continue block18;
                }
                case "pickup": {
                    parts.add("recoger del suelo " + (o.arg().isEmpty() ? "lo que haya" : o.arg()) + (o.then().equals("build") ? " y luego construir" : ""));
                    continue block18;
                }
                case "follow": {
                    parts.add("seguir al jugador");
                    continue block18;
                }
                case "guard": {
                    parts.add("escoltar y defender al jugador");
                    continue block18;
                }
                case "stop": {
                    parts.add("parar lo que hace");
                    continue block18;
                }
            }
            parts.add(o.type());
        }
        return String.join((CharSequence)"; ", parts);
    }

    public static String actionJson(Order o) {
        JsonObject a = new JsonObject();
        a.addProperty("do", o.type());
        if (o.type().equals("plan")) {
            a.addProperty("name", o.arg());
            try {
                a.add("steps", JsonParser.parseString((String)o.steps()));
            }
            catch (RuntimeException e) {
                a.add("steps", (JsonElement)new JsonArray());
            }
            return a.toString();
        }
        if (!o.arg().isEmpty()) {
            a.addProperty("what", o.arg());
        }
        if (o.amount() > 0) {
            a.addProperty("amount", (Number)o.amount());
        }
        if (!o.then().isEmpty()) {
            a.addProperty("then", o.then());
        }
        if (!o.spec().isEmpty()) {
            a.addProperty("spec", o.spec());
        }
        if (o.fresh()) {
            a.addProperty("new", Boolean.valueOf(true));
        }
        return a.toString();
    }

    static {
        OrderParser.imp(V.KILL, true, "mata", "asesina", "elimina", "extermina", "liquida", "derrota", "caza");
        OrderParser.req(V.KILL, "matar", "asesinar", "eliminar", "exterminar", "liquidar", "derrotar", "cazar", "mates", "asesines", "elimines", "extermines", "liquides", "derrotes", "caces");
        OrderParser.pres(V.KILL, "matas", "asesinas", "eliminas", "cazas");
        OrderParser.imp(V.ATTACK, true, "ataca", "pelea", "lucha", "golpea", "embiste", "enfrenta", "reta", "desafia");
        OrderParser.exact(V.ATTACK, "duelo", "batete", "pegame", "atacame", "golpeame", "peleame");
        OrderParser.needs("duelo", "contigo", "conmigo", "a", "de", "entre", "ya", "ahora");
        OrderParser.req(V.ATTACK, "atacar", "pelear", "luchar", "golpear", "ataques", "pelees", "luches", "golpees");
        OrderParser.pres(V.ATTACK, "atacas", "peleas", "luchas", "golpeas");
        OrderParser.exact(V.FETCH, "trae", "traeme", "traenos", "traemelo", "traemela", "traemelos", "traemelas", "traelo", "traela", "traelos", "traelas", "consigue", "consigueme", "consiguenos", "consiguelo", "consiguela", "consiguemelo", "obten", "obtenme", "dame", "danos", "damelo", "damela", "damelos", "damelas", "entregame", "entregamelo", "entregalo", "pasame", "pasamelo", "alcanzame", "regalame");
        OrderParser.req(V.FETCH, "traer", "conseguir", "obtener", "entregar", "regalar", "traigas", "consigas", "obtengas", "entregues", "darme", "darnos");
        OrderParser.pres(V.FETCH, "traes", "consigues", "das", "entregas", "obtienes");
        OrderParser.exact(V.GO, "ve", "anda", "camina", "dirigete", "vete");
        OrderParser.req(V.GO, "ir", "caminar", "andar", "irte", "vayas", "andes", "camines");
        OrderParser.pres(V.GO, "vas", "caminas");
        OrderParser.exact(V.COME, "ven", "acercate", "acerca", "vuelve", "regresa");
        OrderParser.req(V.COME, "venir", "acercarte", "volver", "regresar", "vengas", "acerques", "vuelvas", "regreses");
        OrderParser.pres(V.COME, "vienes", "vuelves", "regresas");
        OrderParser.exact(V.FOLLOW, "sigueme", "siguenos", "acompana", "acompaname", "acompananos", "sigue");
        OrderParser.req(V.FOLLOW, "seguirme", "seguirnos", "acompanar", "acompanarme", "sigas", "acompanes");
        OrderParser.pres(V.FOLLOW, "sigues", "acompanas");
        OrderParser.exact(V.GUARD, "protege", "protegeme", "protegenos", "defiende", "defiendeme", "defiendenos", "escolta", "escoltame", "escoltanos", "cuidame", "cuidanos", "resguardame");
        OrderParser.req(V.GUARD, "proteger", "protegerme", "defender", "defenderme", "escoltar", "escoltarme", "cuidarme", "protejas", "defiendas", "escoltes", "cuides");
        OrderParser.pres(V.GUARD, "proteges", "defiendes", "escoltas");
        OrderParser.exact(V.STOP, "para", "detente", "deten", "alto", "basta", "quieto", "quieta", "frena", "cancela", "abandona", "suspende", "quedate");
        OrderParser.req(V.STOP, "detenerte", "parar", "quedarte", "frenar", "pares", "detengas", "quedes");
        OrderParser.exact(V.GATHER, "recolecta", "tala", "talala", "mina", "pica", "cosecha", "extrae", "junta", "recolectame");
        OrderParser.req(V.GATHER, "recolectar", "talar", "minar", "picar", "cosechar", "extraer", "juntar", "recolectes", "tales", "mines", "piques", "coseches", "extraigas", "juntes");
        OrderParser.pres(V.GATHER, "recolectas", "talas", "minas", "cosechas", "juntas");
        OrderParser.exact(V.BUILD, "construye", "construyeme", "edifica", "levanta", "erige", "haz", "hazme", "hazte", "fabrica");
        OrderParser.req(V.BUILD, "construir", "edificar", "levantar", "hacer", "erigir", "construyas", "edifiques", "levantes", "hagas", "erijas");
        OrderParser.pres(V.BUILD, "construyes", "edificas", "haces");
        OrderParser.exact(V.CONTINUE, "continua", "continuala", "termina", "terminala", "reanuda", "retoma", "acaba", "acabala", "prosigue");
        OrderParser.req(V.CONTINUE, "continuar", "terminar", "reanudar", "retomar", "acabar", "proseguir", "continues", "termines", "reanudes", "retomes", "acabes");
        OrderParser.imp(V.PICKUP, true, "recoge", "coge", "agarra", "toma");
        OrderParser.req(V.PICKUP, "recoger", "coger", "agarrar", "tomar", "recojas", "cojas", "agarres", "tomes");
        OrderParser.pres(V.PICKUP, "recoges", "coges", "agarras");
        OrderParser.imp(V.USE, true, "abre", "cierra", "enciende", "apaga", "pulsa", "activa", "desactiva", "usa", "toca", "presiona", "acciona");
        OrderParser.req(V.USE, "abrir", "cerrar", "encender", "apagar", "pulsar", "activar", "desactivar", "usar", "tocar", "presionar", "abras", "cierres", "enciendas", "apagues", "pulses", "actives", "desactives", "uses", "toques", "presiones");
        OrderParser.pres(V.USE, "abres", "cierras", "enciendes", "apagas", "usas");
        OrderParser.imp(V.BREAK, true, "rompe", "destruye", "derriba", "cava", "excava", "remueve", "arranca", "demuele", "quita");
        OrderParser.req(V.BREAK, "romper", "destruir", "derribar", "cavar", "excavar", "quitar", "demoler", "rompas", "destruyas", "derribes", "caves", "excaves", "quites");
        OrderParser.pres(V.BREAK, "rompes", "destruyes", "derribas", "cavas");
        OrderParser.imp(V.PLACE, true, "pon", "coloca", "pone", "planta", "instala", "deposita");
        OrderParser.req(V.PLACE, "poner", "colocar", "plantar", "instalar", "depositar", "pongas", "coloques", "plantes", "instales");
        OrderParser.pres(V.PLACE, "pones", "colocas", "plantas");
        OrderParser.imp(V.DROP, true, "suelta", "tira", "arroja");
        OrderParser.req(V.DROP, "soltar", "tirar", "arrojar", "sueltes", "tires", "arrojes");
        OrderParser.pres(V.DROP, "sueltas", "tiras");
        OrderParser.exact(V.SLEEP, "duerme", "duermete", "acuestate", "echate", "descansa", "siesta");
        OrderParser.req(V.SLEEP, "dormir", "dormirte", "acostarte", "descansar", "duermas", "acuestes", "descanses");
        OrderParser.pres(V.SLEEP, "duermes", "descansas");
        OrderParser.imp(V.EAT, true, "come", "bebe", "consume");
        OrderParser.req(V.EAT, "comer", "beber", "consumir", "comas", "bebas", "consumas");
        OrderParser.pres(V.EAT, "comes", "bebes");
        OrderParser.imp(V.LOOK, true, "mira", "observa", "voltea");
        OrderParser.req(V.LOOK, "mirar", "observar", "mirarme", "mires", "observes");
        OrderParser.pres(V.LOOK, "miras", "observas");
        OrderParser.imp(V.EQUIP, true, "equipa", "empuna", "blande");
        OrderParser.exact(V.EQUIP, "ponte", "pontela", "pontelo", "pontelas", "pontelos", "equipate", "equipatela", "equipatelo");
        OrderParser.req(V.EQUIP, "equipar", "equiparte", "empunar", "equipes", "empunes", "blandir");
        OrderParser.pres(V.EQUIP, "equipas", "empunas");
        OrderParser.exact(V.JUMP, "salta", "brinca", "rebota");
        OrderParser.req(V.JUMP, "saltar", "brincar", "saltes", "brinques");
        OrderParser.pres(V.JUMP, "saltas", "brincas");
        OrderParser.exact(V.SPIN, "gira", "rota");
        OrderParser.req(V.SPIN, "girar", "rotar", "gires", "rotes");
        OrderParser.exact(V.CROUCH, "agachate", "agacha", "arrodillate", "sientate");
        OrderParser.req(V.CROUCH, "agacharte", "agacharse", "sentarte", "agaches", "sientes");
        OrderParser.exact(V.WAIT, "espera", "esperame", "esperanos", "aguarda", "aguardame");
        OrderParser.req(V.WAIT, "esperar", "esperarme", "aguardar", "esperes", "aguardes");
        OrderParser.exact(V.FLEE, "huye", "escapa", "corre");
        OrderParser.req(V.FLEE, "huir", "escapar", "correr", "huyas", "escapes", "corras");
        OrderParser.exact(V.DANCE, "baila");
        OrderParser.req(V.DANCE, "bailar", "bailes");
        OrderParser.imp(V.GREET, true, "saluda");
        OrderParser.req(V.GREET, "saludar", "saludarme", "saludes");
        OrderParser.exact(V.CELEBRATE, "celebra", "festeja");
        OrderParser.req(V.CELEBRATE, "celebrar", "festejar", "celebres", "festejes");
        OrderParser.exact(V.APPLAUD, "aplaude");
        OrderParser.req(V.APPLAUD, "aplaudir", "aplaudas");
        OrderParser.imp(V.RIDE, true, "monta", "sube", "aborda", "cabalga", "embarca");
        OrderParser.req(V.RIDE, "montar", "subir", "abordar", "cabalgar", "embarcar", "montes", "subas", "abordes", "cabalgues");
        OrderParser.pres(V.RIDE, "montas", "subes", "abordas");
        OrderParser.imp(V.DISMOUNT, true, "desmonta", "baja");
        OrderParser.req(V.DISMOUNT, "desmontar", "bajar", "bajes", "desmontes");
        OrderParser.imp(V.DRIVE, true, "vuela", "pilota", "pilotea", "maneja", "conduce", "navega", "despega", "aterriza");
        OrderParser.req(V.DRIVE, "volar", "pilotar", "pilotear", "manejar", "conducir", "navegar", "despegar", "aterrizar", "vueles", "pilotes", "manejes", "conduzcas", "navegues", "despegues", "aterrices");
        OrderParser.pres(V.DRIVE, "vuelas", "pilotas", "manejas", "conduces", "navegas");
        OrderParser.exact(V.TRY, "prueba", "pruebalo", "pruebala", "testea", "testealo", "ensaya", "estrena");
        OrderParser.req(V.TRY, "probar", "testear", "ensayar", "pruebes");
        OrderParser.needs("baja", "de", "del", "ya", "te", "se", "");
        OrderParser.needs("maneja", "el", "la", "tu", "ese", "esa", "eso", "esto", "este", "esta", "un", "una", "lo", "ya");
        OrderParser.needs("conduce", "el", "la", "tu", "ese", "esa", "eso", "esto", "este", "esta", "un", "una", "lo", "ya", "hacia", "a", "hasta");
        OrderParser.needs("navega", "en", "por", "hacia", "a", "el", "la", "hasta", "con");
        OrderParser.needs("prueba", "el", "la", "tu", "ese", "esa", "eso", "esto", "este", "esta", "lo", "un", "una", "con", "su", "a", "ya");
        OrderParser.needs("sube", "a", "al", "en", "te", "se", "sobre", "encima", "dentro");
        OrderParser.needs("monta", "en", "a", "al", "te", "se", "sobre", "encima", "dentro", "el", "la", "tu", "ese", "esa", "eso", "lo");
        OrderParser.imp(V.COOK, true, "cocina", "hornea", "asa", "frie", "funde", "guisa", "tuesta");
        OrderParser.req(V.COOK, "cocinar", "hornear", "asar", "freir", "fundir", "guisar", "cocines", "hornees", "frias");
        OrderParser.pres(V.COOK, "cocinas", "horneas", "asas");
        OrderParser.imp(V.FISH, true, "pesca");
        OrderParser.req(V.FISH, "pescar", "pesques");
        OrderParser.pres(V.FISH, "pescas");
        OrderParser.imp(V.PLANT, true, "siembra", "cultiva");
        OrderParser.req(V.PLANT, "sembrar", "cultivar", "siembres", "cultives");
        OrderParser.pres(V.PLANT, "siembras", "cultivas");
        OrderParser.needs("cocina", "la", "el", "los", "las", "un", "una", "unos", "unas", "eso", "esto", "todo", "toda", "carne", "pollo", "pescado", "patatas", "papas", "pan", "lo", "me", "te", "algo", "para");
        OrderParser.needs("pesca", "algo", "un", "unos", "unas", "peces", "pez", "pescado", "en", "para", "me", "ahi", "aqui", "unas", "tantos", "hasta", "durante", "");
        OrderParser.needs("para", "ya", "de", "eso", "todo", "aqui", "ahi", "ahora", "un", "por", "");
        OrderParser.needs("ve", "a", "al", "hacia", "hasta", "donde", "ahi", "alli", "alla", "aca", "aqui", "ya", "con", "por", "y", "");
        OrderParser.needs("anda", "a", "al", "hacia", "hasta", "ya", "por", "");
        OrderParser.needs("mira", "a", "al", "hacia", "me", "lo", "la", "los", "las", "esa", "ese", "eso", "esto", "esta", "este");
        OrderParser.needs("sigue", "a", "al", "detras", "me", "con", "construyendo", "trabajando", "haciendo", "recolectando", "asi", "adelante", "");
        OrderParser.needs("toma", "eso", "esto", "esos", "esas", "estos", "estas", "el", "la", "los", "las", "lo", "todo");
        OrderParser.needs("quita", "eso", "ese", "esa", "esos", "esas", "el", "la", "los", "las", "lo");
        OrderParser.needs("levanta", "una", "un", "la", "el", "otra", "otro", "me");
        OrderParser.needs("haz", "una", "un", "la", "el", "otra", "otro", "me", "algo", "te");
        OrderParser.needs("pon", "una", "un", "la", "el", "los", "las", "unos", "unas", "ese", "esa", "eso", "esto", "algo", "aqui", "ahi", "alli", "aca", "alla", "otra", "otro", "mas", "tu", "mi", "tus", "mis", "dentro", "adentro", "afuera");
        OrderParser.needs("pone", "una", "un", "la", "el", "los", "las", "unos", "unas", "aqui", "ahi", "alli", "aca", "alla");
        OrderParser.needs("toca", "el", "la", "los", "las", "esa", "ese", "eso", "un", "una", "ahi", "algo");
        OrderParser.needs("corre", "de", "lejos", "rapido", "hacia", "a", "al", "");
        OrderParser.needs("camina", "a", "al", "hacia", "hasta", "conmigo", "por", "");
        OrderParser.needs("acerca", "te", "");
        OrderParser.needs("pica", "la", "el", "los", "las", "un", "una", "piedra", "madera", "hierro", "carbon");
        OrderParser.needs("mina", "la", "el", "los", "las", "un", "una", "piedra", "hierro", "carbon", "oro", "diamantes", "diamante", "cobre");
        OrderParser.needs("come", "la", "el", "los", "las", "un", "una", "eso", "ese", "esa", "algo", "todo");
        OrderParser.needs("ponte", "la", "el", "los", "las", "un", "una", "tu", "esa", "ese", "esos", "esas", "eso", "lo");
        OrderParser.needs("regresa", "conmigo", "aqui", "a", "al", "ya", "y", "");
        OrderParser.needs("vuelve", "conmigo", "aqui", "a", "al", "ya", "y", "");
        OrderParser.needs("alto", "ahi", "ya", "");
        OrderParser.needs("basta", "ya", "de", "");
        OrderParser.needs("cancela", "eso", "la", "el", "todo", "");
        OrderParser.needs("termina", "la", "el", "con", "de", "eso", "lo", "esa", "ese", "");
        OrderParser.needs("acaba", "la", "el", "con", "de", "eso", "lo", "esa", "ese", "");
        OrderParser.needs("continua", "la", "el", "con", "construyendo", "eso", "lo", "esa", "ese", "");
        OrderParser.needs("caza", "a", "al", "la", "el", "esa", "ese", "eso", "los", "las", "un", "una", "algo");
        OrderParser.needs("elimina", "a", "al", "la", "el", "esa", "ese", "eso", "los", "las", "un", "una", "algo");
        OrderParser.needs("liquida", "a", "al", "la", "el", "esa", "ese", "eso", "los", "las", "un", "una", "algo");
        OrderParser.needs("derrota", "a", "al", "la", "el", "esa", "ese", "eso", "los", "las", "un", "una");
        OrderParser.needs("construye", "una", "un", "la", "el", "me", "otra", "otro", "algo", "te", "unas", "unos", "");
        OrderParser.needs("tira", "de", "todo", "eso", "esa", "ese", "la", "el", "los", "las", "un", "una", "tus", "tu");
        OrderParser.needs("suelta", "todo", "eso", "esa", "ese", "la", "el", "los", "las", "un", "una", "tus", "tu", "lo");
        OrderParser.needs("junta", "madera", "piedra", "tierra", "arena", "grava", "hierro", "carbon", "los", "las", "unas", "unos", "el", "la");
        OrderParser.needs("tala", "los", "las", "unos", "unas", "el", "la", "un", "una", "arboles", "arbol", "madera", "algunos");
        OrderParser.needs("cosecha", "el", "la", "los", "las", "trigo", "zanahorias", "patatas", "papas", "remolachas", "cultivos");
        OrderParser.needs("extrae", "el", "la", "los", "las", "piedra", "hierro", "carbon", "madera", "un", "una");
        LEAD_FILLER = OrderParser.set("por", "favor", "porfa", "porfavor", "oye", "hey", "ey", "eh", "anda", "venga", "pues", "bueno", "ok", "vale", "ahora", "primero", "luego", "despues", "entonces", "y", "e", "ya", "tu", "vos", "amigo", "amiga", "compa", "hermano", "hermana", "chico", "chica", "senor", "senora", "escucha", "disculpa", "perdona", "perdon", "gracias", "hola", "pero", "asi", "tambien", "ademas", "mismo", "ahorita", "rapido", "rapidamente", "urgente", "te", "pido", "ruego", "encargo", "cuando", "puedas", "quieras", "si", "importa", "importaria", "molesta", "es", "posible", "un", "momento", "me", "nos", "podrias", "puedes", "quieres", "vas", "a");
        NOT_NAME = OrderParser.set("la", "el", "los", "las", "un", "una", "unos", "unas", "al", "del", "de", "a", "mi", "tu", "su", "este", "ese", "esta", "esa", "que", "como", "cuando", "donde", "porque", "si", "no", "con", "sin", "para", "por", "en", "se", "me", "te", "lo", "le", "nos", "muy", "mas", "menos", "todo", "todos", "nada", "algo", "hay", "es", "son", "ya", "aqui", "ahi", "yo", "el", "ella", "ellos");
        CONNECTORS = OrderParser.set("y", "e", "luego", "despues", "entonces", "ademas", "tambien", "pues", "ahora", "primero");
        CHAIN_SKIP = OrderParser.set("me", "te", "nos", "ya", "se", "lo", "la", "a", "que", "luego", "despues", "entonces", "ahora", "tambien", "ademas", "y", "e");
        TRAIL = OrderParser.set(",", "y", "e", "o", "luego", "despues", "entonces", "ahora", "ya", "por", "favor", "porfa", "porfavor", "gracias", "pues", "anda", "venga", "rapido", "rapidamente", "mismo", "primero", "tambien", "ademas", "que", "para", "si", "me", "te", "nos", "lo", "la");
        ARTICLES = OrderParser.set("a", "al", "del", "de", "la", "las", "el", "los", "lo", "un", "una", "unos", "unas", "uno", "me", "te", "nos", "se", "le", "les", "que", "por", "favor", "ahora", "ya", "mismo", "tambien", "pronto", "rapido", "antes", "algun", "alguna", "algunos", "algunas", "algo", "cuanto", "mas", "porfa", "porfavor", "y", "hacia", "hasta", "poco", "poca", "pocos", "pocas", "bastante", "bastantes", "muchos", "muchas", "cuantos", "cuantas", ",");
        NEGATORS = OrderParser.set("no", "nunca", "jamas", "tampoco", "ni");
        LEAD_INS = List.of("puedes", "podrias", "podes", "quieres", "te animas a", "te atreves a", "serias capaz de", "eres capaz de", "quiero que", "quisiera que", "necesito que", "me gustaria que", "me encantaria que", "deseo que", "tienes que", "tenes que", "debes", "deberias", "has de", "hay que", "vas a", "ve a", "ven a", "anda a", "ponte a", "empieza a", "comienza a", "intenta", "prueba a", "trata de", "te pido que", "te pido", "te ruego que", "te ordeno que", "te mando que", "te encargo que", "te encargo", "encargate de", "atrevete a", "animate a", "favor de", "el favor de", "la molestia de", "haz que", "ojala", "espero que", "a ver si", "si puedes", "si quieres", "cuando puedas", "cuando quieras", "vas y", "ve y", "hazme el favor de", "me haces el favor de", "me harias el favor de", "me puedes", "me podrias", "me harias", "me haces", "necesitamos que", "queremos que", "te toca", "te corresponde", "toca", "ordeno que", "mando que", "pido que", "exijo que", "ruego que");
        LEAD_TOKENS = new ArrayList<List<String>>();
        for (String phrase : LEAD_INS) {
            LEAD_TOKENS.add(OrderParser.tokens(phrase));
        }
        NUMBERS = new HashMap<String, Integer>();
        String[] words = new String[]{"dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve", "veinte"};
        for (int i = 0; i < words.length; ++i) {
            NUMBERS.put(words[i], i + 2);
        }
        NUMBERS.put("treinta", 30);
        NUMBERS.put("cuarenta", 40);
        NUMBERS.put("cincuenta", 50);
        NUMBERS.put("sesenta", 60);
        NUMBERS.put("cien", 100);
        NUMBERS.put("ciento", 100);
        NUMBERS.put("docena", 12);
        NUMBERS.put("par", 2);
        POWER_WORDS = OrderParser.set("poder", "poderes", "habilidad", "habilidades", "tecnica", "tecnicas", "hechizo", "hechizos", "conjuro", "conjuros", "magia", "ataque", "especial");
        PLAYER_TOKENS = OrderParser.set("me", "mi", "mis", "yo", "conmigo", "nosotros", "jugador", "aqui", "aca");
        KNOWN_KEYWORDS = OrderParser.set("coal_ore", "iron_ore", "copper_ore", "gold_ore", "diamond_ore", "clay");
    }

    private record Verb(int at, int len, V v, boolean negated, String form) {
    }

    private static enum V {
        KILL,
        ATTACK,
        FETCH,
        GO,
        COME,
        FOLLOW,
        GUARD,
        STOP,
        GATHER,
        BUILD,
        PICKUP,
        USE,
        BREAK,
        PLACE,
        DROP,
        SLEEP,
        EAT,
        LOOK,
        JUMP,
        SPIN,
        CROUCH,
        WAIT,
        FLEE,
        DANCE,
        GREET,
        CELEBRATE,
        APPLAUD,
        CONTINUE,
        EQUIP,
        RIDE,
        DISMOUNT,
        DRIVE,
        TRY,
        COOK,
        FISH,
        PLANT;

    }

    public record Result(List<Order> orders, boolean order, boolean negated, String summary) {
        public boolean has(String type) {
            for (Order o : this.orders) {
                if (!o.type().equals(type)) continue;
                return true;
            }
            return false;
        }

        public boolean hasStep(String op) {
            for (Order o : this.orders) {
                if (!o.type().equals("plan")) continue;
                for (PlanSpec.Step s : PlanSpec.parse(o.steps())) {
                    if (!s.op().equals(op)) continue;
                    return true;
                }
            }
            return false;
        }

        public Order first() {
            return this.orders.isEmpty() ? null : this.orders.get(0);
        }
    }

    public record Order(String type, String arg, int amount, String then, String spec, String where, boolean fresh, String name, String steps) {
        public static Order plan(String title, String stepsJson) {
            return new Order("plan", title, 0, "", "", "", false, "", stepsJson);
        }

        public static Order simple(String type) {
            return new Order(type, "", 0, "", "", "", false, "", "");
        }
    }
}

