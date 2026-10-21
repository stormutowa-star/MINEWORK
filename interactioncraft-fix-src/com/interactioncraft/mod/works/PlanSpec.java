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
import com.interactioncraft.mod.works.PickupFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlanSpec {
    public static final int MAX_STEPS = 80;
    public static final List<String> OPS = List.of("go", "look", "follow", "flee", "jump", "spin", "crouch", "swing", "wait", "say", "break", "place", "use", "give", "drop", "pickup", "attack", "kill", "fetch", "sleep", "eat", "emote", "sound", "interact", "ride", "dismount", "teleport", "command", "equip", "power", "learn", "train", "godir", "back", "observe", "drive", "cook", "fish", "plant");
    private static final Map<String, String> ALIAS = new HashMap<String, String>();
    private static final Set<String> HOSTILE_WORDS;
    private static final Set<String> ANIMAL_WORDS;
    private static final Set<String> ANY_WORDS;
    private static final Set<String> FILLER_WORDS;
    private static final Set<String> DEMONSTRATIVES;
    private static final Set<String> ALL_WORDS;
    private static final List<String> LOOT_PHRASES;
    private static final List<String> PACK_PHRASES;

    private PlanSpec() {
    }

    private static void a(String op, String ... names) {
        ALIAS.put(op, op);
        for (String n : names) {
            ALIAS.put(n, op);
        }
    }

    public static String op(String raw) {
        String k = PickupFilter.norm(raw).replace(' ', '_');
        String op = ALIAS.get(k);
        return op == null ? "" : op;
    }

    private static String str(JsonObject o, String ... keys) {
        for (String k : keys) {
            String v;
            if (!o.has(k) || !o.get(k).isJsonPrimitive() || (v = o.get(k).getAsString().trim()).isEmpty()) continue;
            return v.length() > 180 ? v.substring(0, 180) : v;
        }
        return "";
    }

    private static int num(JsonObject o, int def, String ... keys) {
        for (String k : keys) {
            if (!o.has(k) || !o.get(k).isJsonPrimitive()) continue;
            try {
                return (int)Math.round(o.get(k).getAsDouble());
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
        }
        return def;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int qty(int times) {
        return times <= 0 ? 0 : PlanSpec.clamp(times, 1, 64);
    }

    private static Step read(JsonObject o, String op) {
        String target = PlanSpec.str(o, "target", "to", "at", "on", "from", "objetivo", "a", "hacia", "de", "donde", "who", "quien", "dir", "direction", "direccion", "sentido");
        String item = PlanSpec.str(o, "item", "objeto", "block", "bloque", "with", "con");
        String text = PlanSpec.str(o, "text", "texto", "say", "type", "tipo", "id", "what", "que", "emotion", "sound", "mensaje", "message", "command", "cmd", "comando", "slot", "ranura", "name", "nombre", "power", "poder");
        if (target.isEmpty() && o.has("x") && o.has("z") && o.get("x").isJsonPrimitive() && o.get("z").isJsonPrimitive()) {
            try {
                target = Math.round(o.get("x").getAsDouble()) + " " + Math.round(o.has("y") && o.get("y").isJsonPrimitive() ? o.get("y").getAsDouble() : -9999) + " " + Math.round(o.get("z").getAsDouble());
            }
            catch (RuntimeException e) {
                // coordenadas ilegibles
            }
        }
        String vehicle = PlanSpec.str(o, "vehicle", "vehiculo", "nave", "montura", "mount", "ride", "in", "en", "into");
        int times = PlanSpec.num(o, -1, "times", "n", "count", "turns", "veces", "vueltas", "cantidad", "amount", "blocks", "bloques", "distance", "distancia");
        int secs = PlanSpec.num(o, -1, "secs", "seconds", "segundos", "s", "duration", "duracion");
        return switch (op) {
            case "jump" -> new Step(op, "", "", PlanSpec.clamp(times < 0 ? 1 : times, 1, 20), 0, "");
            case "spin" -> new Step(op, "", "", PlanSpec.clamp(times < 0 ? 1 : times, 1, 10), 0, "");
            case "swing" -> new Step(op, "", "", PlanSpec.clamp(times < 0 ? 3 : times, 1, 20), 0, "");
            case "crouch" -> new Step(op, "", "", 1, PlanSpec.clamp(secs < 0 ? 3 : secs, 1, 60), "");
            case "wait" -> new Step(op, target, "", 1, PlanSpec.clamp(secs < 0 ? 2 : secs, 1, 60), "");
            case "sleep" -> new Step(op, target, "", 1, PlanSpec.clamp(secs < 0 ? 10 : secs, 2, 120), "");
            case "follow" -> new Step(op, target.isEmpty() ? "jugador" : target, "", 1, PlanSpec.clamp(secs < 0 ? 15 : secs, 1, 120), "");
            case "flee" -> new Step(op, target, "", 1, PlanSpec.clamp(secs < 0 ? 6 : secs, 1, 30), "");
            case "emote" -> new Step(op, "", "", 1, PlanSpec.clamp(secs < 0 ? 2 : secs, 1, 10), text.isEmpty() ? item : text);
            case "say" -> new Step(op, "", "", 1, 0, text.isEmpty() ? PlanSpec.str(o, "linea", "line", "frase") : text);
            case "give", "drop" -> new Step(op, op.equals("give") && target.isEmpty() ? "jugador" : target, item.isEmpty() ? text : item, PlanSpec.qty(times), 0, "");
            case "pickup" -> new Step(op, "", "", 1, 0, text.isEmpty() ? item : text);
            case "attack" -> new Step(op, target, "", PlanSpec.clamp(times < 0 ? 1 : times, 1, 12), PlanSpec.clamp(secs < 0 ? 40 : secs, 5, 240), "");
            case "kill" -> new Step(op, target, "", PlanSpec.clamp(times < 0 ? 1 : times, 1, 12), PlanSpec.clamp(secs < 0 ? 90 : secs, 10, 300), "");
            case "fetch" -> new Step(op, target.isEmpty() ? "jugador" : target, item.isEmpty() ? text : item, PlanSpec.qty(times), 0, "");
            case "eat" -> new Step(op, "", item.isEmpty() ? text : item, 1, 0, "");
            case "sound" -> new Step(op, "", "", 1, 0, text.isEmpty() ? item : text);
            case "command" -> new Step(op, "", "", 1, 0, text.isEmpty() ? item : text);
            case "equip" -> new Step(op, "", item.isEmpty() ? target : item, 1, 0, text);
            case "power" -> new Step(op, target, item, PlanSpec.clamp(times < 0 ? 1 : times, 1, 12), 0, text);
            case "learn" -> {
                String json = o.toString();
                yield new Step(op, PlanSpec.str(o, "name", "nombre", "power", "poder", "arg"), "", 1, 0, json.length() > 900 ? json.substring(0, 900) : json);
            }
            case "train" -> new Step(op, target, "", PlanSpec.clamp(times < 0 ? 5 : times, 1, 20), 0, text);
            case "godir" -> new Step(op, target, "", PlanSpec.clamp(times < 0 ? 8 : times, 1, 64), 0, "");
            case "dismount", "back" -> new Step(op, "", "", 1, 0, "");
            case "observe" -> new Step(op, target.isEmpty() ? text : target, "", 1, PlanSpec.clamp(secs < 0 ? 3 : secs, 1, 20), "");
            case "ride" -> new Step(op, target.isEmpty() ? (vehicle.isEmpty() ? (item.isEmpty() ? text : item) : vehicle) : target, "", 1, 0, "");
            case "drive" -> new Step(op, target, item.isEmpty() ? vehicle : item, 1, PlanSpec.clamp(secs < 0 ? 25 : secs, 3, 180), text);
            case "teleport", "interact" -> new Step(op, target, item, 1, 0, text);
            default -> new Step(op, target, item, PlanSpec.clamp(times < 0 ? 1 : times, 1, 64), PlanSpec.clamp(secs < 0 ? 0 : secs, 0, 60), text);
        };
    }


    private static final Set<String> FLY_ALIASES = Set.of("volar", "vuela", "vuelalo", "volando", "vuelo", "fly", "flying", "despegar", "despega", "takeoff", "take_off");

    private static int commonPrefix(String a, String b) {
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) {
            ++i;
        }
        return i;
    }

    /** Operacion mas parecida a un nombre de paso desconocido ("montando_nave", "volar_en_circulos"): por palabras o por raiz comun. */
    public static String fuzzyOp(String raw) {
        String k = PickupFilter.norm(raw).replace(' ', '_');
        if (k.isEmpty()) {
            return "";
        }
        String[] toks = k.split("_");
        for (String token : toks) {
            String direct = token.length() >= 2 ? ALIAS.get(token) : null;
            if (direct != null) {
                return direct;
            }
        }
        String best = "";
        int bestScore = 0;
        String first = toks[0];
        if (first.length() >= 4) {
            for (Map.Entry<String, String> en : ALIAS.entrySet()) {
                String key = en.getKey();
                int cp = PlanSpec.commonPrefix(first, key);
                if (cp < 4 || cp < Math.min(first.length(), key.length()) - 2) {
                    continue;
                }
                int score = cp * 2 - Math.abs(first.length() - key.length());
                if (score > bestScore) {
                    bestScore = score;
                    best = en.getValue();
                }
            }
        }
        return best;
    }


    private static final Set<String> RAW_STOP = Set.of("en", "a", "al", "el", "la", "los", "las", "un", "una", "de", "del", "con", "hacia", "hasta", "por", "para", "y", "me", "te", "se", "lo", "circulos", "circulo", "vuelta", "vueltas", "rapido", "lento", "lejos", "cerca", "alrededor", "libre", "libremente", "rato", "bien", "mucho");

    /** Palabras sobrantes del nombre de un paso desconocido ("montar_tie_fighter" -> "tie fighter"): suelen ser el objetivo. */
    static String fuzzyRest(String raw, String op) {
        StringBuilder sb = new StringBuilder();
        for (String token : PickupFilter.norm(raw).replace(' ', '_').split("_")) {
            if (token.isEmpty() || RAW_STOP.contains(token) || op.equals(ALIAS.get(token)) || token.equals(PickupFilter.norm(raw).replace(' ', '_').split("_")[0])) {
                continue;
            }
            sb.append(sb.length() == 0 ? "" : " ").append(token);
        }
        return sb.toString();
    }

    private static void expand(JsonArray arr, List<Step> out, int depth) {
        for (JsonElement el : arr) {
            if (out.size() >= 80) {
                return;
            }
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                // paso escrito como frase ("ve a la puerta"): se entiende con la gramatica del juego
                String sentence = el.getAsString().trim();
                boolean spliced = false;
                for (String lead : new String[]{"", "puedes "}) {
                    OrderParser.Order first = OrderParser.parse(lead + sentence).first();
                    if (!sentence.isEmpty() && first != null && first.type().equals("plan")) {
                        for (Step st : PlanSpec.parse(first.steps())) {
                            if (out.size() < 80) {
                                out.add(st);
                            }
                        }
                        spliced = true;
                        break;
                    }
                }
                if (!spliced && !sentence.isEmpty()) {
                    JsonObject asOp = new JsonObject();
                    asOp.addProperty("op", sentence);
                    JsonArray one = new JsonArray();
                    one.add((JsonElement)asOp);
                    PlanSpec.expand(one, out, depth + 1);
                }
                continue;
            }
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String rawOp = PlanSpec.str(o, "op", "do", "accion", "action", "paso", "step", "type_op");
            String op = PlanSpec.op(rawOp);
            if (op.isEmpty()) {
                for (String k : o.keySet()) {
                    String alt = PlanSpec.op(k);
                    if (alt.isEmpty()) continue;
                    op = alt;
                    JsonObject copy = o.deepCopy();
                    JsonElement v = o.get(k);
                    if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
                        copy.add(alt.equals("wait") || alt.equals("crouch") || alt.equals("sleep") ? "secs" : "times", v);
                    } else if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                        String field = switch (alt) {
                            case "say", "emote", "sound", "pickup", "command", "power", "train" -> "text";
                            case "give", "drop", "eat", "place", "fetch", "equip" -> "item";
                            default -> "target";
                        };
                        copy.add(field, v);
                    } else if (v.isJsonArray() && alt.equals("repeat")) {
                        copy.add("steps", v);
                    }
                    o = copy;
                    break;
                }
            }
            if (op.isEmpty() && !rawOp.isEmpty()) {
                op = PlanSpec.fuzzyOp(rawOp);
                String obj = PlanSpec.str(o, "target", "to", "at", "on", "objetivo", "a", "hacia");
                String item = PlanSpec.str(o, "item", "objeto", "block", "bloque", "with", "con");
                String txt = PlanSpec.str(o, "text", "texto", "say", "mensaje", "message");
                if (!op.isEmpty() && obj.isEmpty() && item.isEmpty() && txt.isEmpty() && !PlanSpec.str(o, "vehicle", "vehiculo", "nave").isEmpty() == false) {
                    String rest = PlanSpec.fuzzyRest(rawOp, op);
                    if (!rest.isEmpty()) {
                        JsonObject copy = o.deepCopy();
                        copy.addProperty(op.equals("give") || op.equals("drop") || op.equals("eat") || op.equals("place") || op.equals("fetch") || op.equals("equip") ? "item" : "target", rest);
                        o = copy;
                    }
                }
                if (op.isEmpty()) {
                    // un nombre libre ("bailar", "saludar", "celebrar"...): lo que el juego ya entiende de esa frase
                    boolean spliced = false;
                    String phrase = PickupFilter.norm(rawOp) + " " + PickupFilter.norm(obj.isEmpty() ? item : obj);
                    for (String lead : new String[]{"puedes ", ""}) {
                        OrderParser.Result lit = OrderParser.parse(lead + phrase.trim());
                        OrderParser.Order first = lit.first();
                        if (first != null && first.type().equals("plan")) {
                            for (Step st : PlanSpec.parse(first.steps())) {
                                if (out.size() < 80) {
                                    out.add(st);
                                }
                            }
                            spliced = true;
                            break;
                        }
                    }
                    if (spliced) {
                        continue;
                    }
                    JsonObject copy = o.deepCopy();
                    if (!obj.isEmpty() || !item.isEmpty()) {
                        op = "use";
                        if (obj.isEmpty()) {
                            copy.addProperty("target", item);
                            copy.remove("item");
                        }
                    } else if (!txt.isEmpty()) {
                        op = "emote";
                    }
                    o = copy;
                }
            }
            if (op.equals("drive") && FLY_ALIASES.contains(PickupFilter.norm(rawOp).replace(' ', '_')) && !o.has("text")) {
                JsonObject copy = o.deepCopy();
                copy.addProperty("text", "vuelo");
                o = copy;
            }
            if (op.equals("repeat")) {
                if (depth >= 2 || !o.has("steps") || !o.get("steps").isJsonArray()) continue;
                int times = PlanSpec.clamp(PlanSpec.num(o, 2, "times", "n", "count", "veces"), 1, 20);
                for (int i = 0; i < times && out.size() < 80; ++i) {
                    PlanSpec.expand(o.getAsJsonArray("steps"), out, depth + 1);
                }
                continue;
            }
            if (op.isEmpty()) continue;
            out.add(PlanSpec.read(o, op));
        }
    }

    public static List<Step> parse(String json) {
        ArrayList<Step> out = new ArrayList<Step>();
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            String trimmed = json.trim();
            JsonElement root;
            try {
                root = JsonParser.parseString(trimmed);
            }
            catch (RuntimeException notJson) {
                root = new com.google.gson.JsonPrimitive(trimmed);
            }
            if (root.isJsonObject() && root.getAsJsonObject().has("steps") && root.getAsJsonObject().get("steps").isJsonArray()) {
                root = root.getAsJsonObject().get("steps");
            }
            if (root.isJsonPrimitive() && root.getAsJsonPrimitive().isString()) {
                JsonArray one = new JsonArray();
                one.add(root);
                PlanSpec.expand(one, out, 0);
            } else if (root.isJsonArray()) {
                PlanSpec.expand(root.getAsJsonArray(), out, 0);
            } else if (root.isJsonObject()) {
                JsonArray one = new JsonArray();
                one.add(root);
                PlanSpec.expand(one, out, 0);
            }
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
        return out;
    }

    public static boolean allWords(String text) {
        boolean any = false;
        for (String w : PickupFilter.norm(text).split(" ")) {
            if (w.isEmpty()) continue;
            if (!(ALL_WORDS.contains(w) || FILLER_WORDS.contains(w) || DEMONSTRATIVES.contains(w))) {
                return false;
            }
            any = true;
        }
        return any;
    }

    public static boolean lootText(String text) {
        String n = " " + PickupFilter.norm(text) + " ";
        for (String p : LOOT_PHRASES) {
            if (!n.contains(" " + p + " ")) continue;
            return true;
        }
        return false;
    }

    public static boolean wholePack(String text) {
        String n = " " + PickupFilter.norm(text) + " ";
        for (String p : PACK_PHRASES) {
            if (!n.contains(" " + p + " ")) continue;
            return true;
        }
        return false;
    }

    public static String targetKind(String text) {
        boolean hostile = false;
        boolean animal = false;
        boolean any = false;
        boolean other = false;
        for (String w : PickupFilter.norm(text).split(" ")) {
            if (w.isEmpty() || FILLER_WORDS.contains(w) || DEMONSTRATIVES.contains(w)) continue;
            if (HOSTILE_WORDS.contains(w)) {
                hostile = true;
                continue;
            }
            if (ANIMAL_WORDS.contains(w)) {
                animal = true;
                continue;
            }
            if (ANY_WORDS.contains(w)) {
                any = true;
                continue;
            }
            other = true;
        }
        if (other || !hostile && !animal && !any) {
            return "";
        }
        return hostile ? "hostile" : (animal ? "animal" : "any");
    }

    public static boolean demonstrative(String text) {
        for (String w : PickupFilter.norm(text).split(" ")) {
            if (!DEMONSTRATIVES.contains(w)) continue;
            return true;
        }
        return false;
    }

    public static String toJson(List<Step> steps) {
        JsonArray arr = new JsonArray();
        for (Step s : steps) {
            JsonObject o = new JsonObject();
            o.addProperty("op", s.op());
            if (!s.target().isEmpty()) {
                o.addProperty("target", s.target());
            }
            if (!s.item().isEmpty()) {
                o.addProperty("item", s.item());
            }
            o.addProperty("times", (Number)s.n());
            if (s.secs() > 0) {
                o.addProperty("secs", (Number)s.secs());
            }
            if (!s.text().isEmpty()) {
                o.addProperty("text", s.text());
            }
            arr.add((JsonElement)o);
        }
        return arr.toString();
    }

    public static boolean notable(List<Step> steps) {
        for (Step s : steps) {
            switch (s.op()) {
                case "break": 
                case "place": 
                case "use": 
                case "give": 
                case "drop": 
                case "pickup": 
                case "attack": 
                case "kill": 
                case "fetch": 
                case "eat": 
                case "sleep": 
                case "go": 
                case "follow": 
                case "interact": 
                case "ride": 
                case "teleport": 
                case "command": 
                case "equip": 
                case "power": 
                case "learn": 
                case "train": 
                case "godir": 
                case "back":
                case "cook":
                case "fish":
                case "plant":
                case "drive": {
                    return true;
                }
            }
        }
        return false;
    }

    public static String describe(Step s) {
        String t = s.target().isEmpty() ? "" : " " + s.target();
        String i = s.item().isEmpty() ? "" : " " + s.item();
        return switch (s.op()) {
            case "go" -> "va a" + t;
            case "look" -> "mira a" + t;
            case "follow" -> "sigue a" + t;
            case "flee" -> "huye" + (String)(t.isEmpty() ? "" : " de" + t);
            case "jump" -> "salta " + s.n() + (s.n() == 1 ? " vez" : " veces");
            case "spin" -> "gira " + s.n() + (s.n() == 1 ? " vuelta" : " vueltas");
            case "crouch" -> "se agacha";
            case "swing" -> "mueve el brazo";
            case "wait" -> "espera " + s.secs() + " s";
            case "say" -> "dice \"" + s.text() + "\"";
            case "break" -> "rompe" + t;
            case "place" -> "coloca" + i + (String)(t.isEmpty() ? "" : " en" + t);
            case "use" -> "usa" + i + (String)(t.isEmpty() ? "" : (i.isEmpty() ? "" : " en") + t);
            case "give" -> "da" + (i.isEmpty() ? " todo" : i) + (String)(s.n() > 1 ? " x" + s.n() : "") + " a" + (t.isEmpty() ? " el jugador" : t);
            case "drop" -> "suelta" + (i.isEmpty() ? " todo" : i) + (String)(s.n() > 1 ? " x" + s.n() : "");
            case "pickup" -> "recoge del suelo " + (s.text().isEmpty() ? "lo que hay" : s.text());
            case "attack" -> "ataca a" + t;
            case "kill" -> "mata a" + (t.isEmpty() ? " una criatura" : t) + (String)(s.n() > 1 ? " (" + s.n() + ")" : "");
            case "fetch" -> "trae" + (i.isEmpty() ? " lo pedido" : i) + (String)(s.n() > 1 ? " x" + s.n() : "") + " a" + (t.isEmpty() ? " el jugador" : t);
            case "sleep" -> "se acuesta" + (String)(t.isEmpty() ? "" : " en" + t);
            case "eat" -> "come" + i;
            case "emote" -> "muestra " + (s.text().isEmpty() ? "una emoci\u00f3n" : s.text());
            case "sound" -> "hace un sonido";
            case "interact" -> "interact\u00faa con" + t;
            case "ride" -> "se monta en" + t;
            case "dismount" -> "se baja";
            case "cook" -> "cocina" + (i.isEmpty() ? " lo que lleva" : i);
            case "fish" -> "pesca" + (s.n() > 1 ? " (" + s.n() + ")" : "");
            case "plant" -> "planta" + (i.isEmpty() ? " lo que lleva" : i) + (t.isEmpty() ? "" : " en" + t);
            case "drive" -> "pilota" + (i.isEmpty() ? " el vehículo" : i) + (t.isEmpty() ? "" : " hacia" + t) + " (" + s.secs() + " s)";
            case "teleport" -> "se teletransporta a" + t;
            case "command" -> "ejecuta " + (String)(s.text().length() > 40 ? s.text().substring(0, 40) + "\u2026" : s.text());
            case "equip" -> "se equipa" + i;
            case "power" -> "usa su poder" + (String)(s.text().isEmpty() ? i : " \"" + s.text() + "\"") + (String)(t.isEmpty() ? "" : " sobre" + t);
            case "learn" -> "aprende el poder \"" + s.target() + "\"";
            case "train" -> "entrena " + (String)(s.text().isEmpty() ? "su poder" : "\"" + s.text() + "\"") + " (" + s.n() + " veces)";
            case "godir" -> "avanza " + s.n() + " bloques hacia" + t;
            case "back" -> "vuelve al punto de partida";
            case "observe" -> "observa los alrededores" + (String)(t.isEmpty() ? "" : " buscando" + t);
            default -> s.op();
        };
    }

    static {
        PlanSpec.a("go", "ir", "ve", "caminar", "andar", "moverse", "mover", "acercarse", "acercate", "walk", "move", "goto", "go_to", "travel");
        PlanSpec.a("look", "mirar", "mira", "observar", "look_at", "lookat", "watch");
        PlanSpec.a("follow", "seguir", "sigue", "acompanar");
        PlanSpec.a("flee", "huir", "huye", "escapar", "correr", "run", "run_away", "escape");
        PlanSpec.a("jump", "saltar", "salta", "brincar", "hop");
        PlanSpec.a("spin", "girar", "gira", "dar_vueltas", "vueltas", "turn", "rotate", "twirl", "voltear");
        PlanSpec.a("crouch", "agacharse", "agachate", "sneak", "sentarse", "sit", "arrodillarse", "kneel");
        PlanSpec.a("swing", "saludar", "agitar", "brazo", "wave", "arm", "hit_air", "gesticular", "aplaudir", "clap");
        PlanSpec.a("wait", "esperar", "espera", "pausa", "idle", "quieto", "pause");
        PlanSpec.a("say", "decir", "di", "hablar", "gritar", "shout", "talk", "speak");
        PlanSpec.a("break", "romper", "rompe", "picar", "talar", "cavar", "excavar", "cortar", "cosechar", "mine", "dig", "chop", "harvest", "destroy");
        PlanSpec.a("place", "colocar", "coloca", "poner", "pon", "put", "set");
        PlanSpec.a("use", "usar", "usa", "abrir", "abre", "cerrar", "cierra", "pulsar", "pulsa", "activar", "interactuar", "encender", "enciende", "plantar", "sembrar", "tocar", "lanzar", "arrojar", "interact", "open", "close", "press", "activate", "light", "plant", "throw", "toggle", "ring");
        PlanSpec.a("give", "dar", "da", "entregar", "regalar", "hand", "offer");
        PlanSpec.a("drop", "soltar", "suelta", "dejar", "deja", "tirar");
        PlanSpec.a("pickup", "recoger", "recoge", "coger", "agarrar", "collect", "pick_up", "pick", "grab", "take");
        PlanSpec.a("attack", "atacar", "ataca", "pelear", "pelea", "luchar", "lucha", "golpear", "golpea", "fight", "hit", "strike");
        PlanSpec.a("kill", "matar", "mata", "matalo", "matala", "asesinar", "asesina", "eliminar", "elimina", "liquidar", "liquida", "exterminar", "extermina", "cazar", "caza", "derrotar", "derrota", "slay", "hunt", "murder", "acabar_con", "acaba_con");
        PlanSpec.a("fetch", "traer", "trae", "traeme", "traenos", "trae_me", "bring", "fetch", "conseguir", "consigue", "consigueme", "conseguime", "obtener", "obten", "get", "buscar_y_traer", "busca_y_trae", "ir_a_por", "ve_a_por", "a_por");
        PlanSpec.a("sleep", "dormir", "duerme", "acostarse", "descansar", "siesta", "lie", "lie_down", "rest", "nap");
        PlanSpec.a("eat", "comer", "come", "consume", "beber", "drink");
        PlanSpec.a("emote", "emocion", "expresar", "particulas", "particles", "emotion", "feel", "sentir");
        PlanSpec.a("sound", "sonido", "play_sound", "sfx", "ruido", "audio");
        PlanSpec.a("interact", "interactuar_con", "hablar_con", "trade", "comerciar", "entity_interact", "interact_entity", "right_click_entity", "clic_derecho", "interactua_con");
        PlanSpec.a("ride", "montar", "montarse", "cabalgar", "mount", "monta", "subirse", "subete", "montate", "montar_en", "montarse_en", "subir_a", "subirse_a", "sube_a", "embarcar", "embarca", "abordar", "aborda", "board", "hop_on", "get_on", "ride_vehicle", "montarse_a", "sentarse_en");
        PlanSpec.a("cook", "cocinar", "cocina", "cocinalo", "hornear", "hornea", "asar", "asa", "freir", "fríe", "frie", "fundir", "funde", "fundelo", "calentar", "smelt", "bake", "roast", "grill", "preparar_comida", "guisar", "guisa");
        PlanSpec.a("fish", "pescar", "pesca", "pescando", "fishing", "angling", "cana", "lanzar_cana");
        PlanSpec.a("plant", "sembrar", "siembra", "plantar", "planta", "sow", "cultivar", "cultiva", "reforestar", "reforesta", "plantar_arbol", "sembrar_semillas");
        PlanSpec.a("dismount", "desmontar", "desmonta", "bajarse", "bajate", "unmount", "get_off", "desmontate", "bajar_de", "baja_de", "bajarse_de", "descender", "apearse", "apeate", "hop_off", "get_down", "salir_del_vehiculo", "bajar");
        PlanSpec.a("drive", "pilotar", "pilota", "pilotear", "pilotea", "volar", "vuela", "vuelalo", "volando", "vuelo", "fly", "flying", "pilot", "conducir", "conduce", "conducelo", "manejar", "maneja", "manejalo", "navegar", "navega", "despegar", "despega", "aterrizar", "aterriza", "land", "takeoff", "take_off", "test_drive", "probar_vehiculo", "conduccion", "maniobrar", "maniobra", "circular");
        PlanSpec.a("teleport", "teletransportar", "teletransportarse", "teletransporta", "tp", "warp", "aparecer", "blink");
        PlanSpec.a("command", "comando", "cmd", "run_command", "ejecutar", "execute");
        PlanSpec.a("equip", "equipar", "equipate", "ponerse", "vestir", "vestirse", "wear", "armar", "armarse", "empunar", "empuna", "wield", "sostener", "sostiene", "hold");
        PlanSpec.a("power", "useitem", "use_item", "usar_objeto", "usar_item", "usa_objeto", "poder", "habilidad", "skill", "cast", "ability", "activar_poder", "usar_poder", "lanzar_poder", "use_power", "usa_poder", "tecnica", "hechizo", "conjuro", "lanzar_hechizo", "lanza_poder", "desatar", "desata");
        PlanSpec.a("learn", "aprender", "aprende", "ensenar", "ensena", "learn_power", "aprender_poder", "teach", "nuevo_poder", "new_power", "domina");
        PlanSpec.a("train", "entrenar", "entrena", "practicar", "practica", "ejercitar", "ejercita", "train_power", "entrenar_poder", "practice", "drill");
        PlanSpec.a("godir", "go_dir", "ir_hacia", "avanzar", "avanza", "advance", "walk_dir", "caminar_hacia", "dirigirse", "head");
        PlanSpec.a("back", "volver", "return", "regresar", "volver_al_inicio", "vuelve_al_inicio", "return_back", "go_back", "retroceder_al_inicio");
        PlanSpec.a("observe", "observe", "explorar", "explora", "escanear", "escanea", "scan", "survey", "inspect", "inspeccionar", "reconocer", "reconoce", "recon");
        PlanSpec.a("repeat", "repetir", "repite", "loop", "bucle", "veces");
        HOSTILE_WORDS = Set.of("monstruo", "monstruos", "hostil", "hostiles", "enemigo", "enemigos", "amenaza", "amenazas", "malo", "mala", "malos", "malas", "peligroso", "peligrosos", "peligro");
        ANIMAL_WORDS = Set.of("animal", "animales", "ganado", "presa", "bestia", "bestias", "res");
        ANY_WORDS = Set.of("criatura", "criaturas", "bicho", "bichos", "mob", "mobs", "ser", "seres", "cosa", "cosas", "algo", "cualquiera", "cualquier", "quieras", "quiera", "sea", "alguna", "alguno", "viva", "vivo", "creature", "creatures", "thing", "anything", "any", "primero", "primera", "veas", "vea", "encuentres", "encuentre", "halles", "hayas", "ves", "aparezca", "aparezcas", "salga", "salgas");
        FILLER_WORDS = Set.of("de", "del", "la", "las", "el", "los", "lo", "a", "al", "un", "una", "unos", "unas", "uno", "que", "por", "favor", "solo", "sola", "mas", "cercano", "cercana", "cerca", "proximo", "proxima", "the", "some", "nearest", "closest", "near");
        DEMONSTRATIVES = Set.of("este", "esta", "esto", "estos", "estas", "ese", "esa", "eso", "esos", "esas", "aquel", "aquella", "aquello", "aquellos", "aquellas", "this", "that", "those", "these");
        ALL_WORDS = Set.of("todo", "toda", "todos", "todas", "botin", "loot", "drops", "drop", "despojos", "lo", "que", "solto", "suelte", "soltara", "solte", "dejo", "deje", "dejara", "cayo", "caiga", "caera", "consiguio", "consiga", "consiguiera", "recogio", "recoja", "tiene", "tenga", "lleva", "lleve", "llevaba", "sus", "su", "cosas", "cosa", "objetos", "objeto", "items", "item", "dropee", "dropeo", "sea", "ello", "ellos", "ellas", "everything", "all", "of", "it", "whatever", "recogido", "conseguido", "cual", "cuales", "haya", "hubiera", "hubiese", "tirado", "tirada", "tirados", "tiradas");
        LOOT_PHRASES = List.of("botin", "loot", "drops", "drop", "despojos", "lo que solto", "lo que suelte", "lo que soltara", "lo que dropee", "lo que dropeo", "lo que cayo", "lo que caiga", "lo que dejo", "lo que deje", "lo que consiguio", "lo que consiga", "sus cosas", "sus drops", "lo que tenga", "lo que llevaba", "lo que lleve");
        PACK_PHRASES = List.of("lo que llevas", "lo que tienes", "lo que cargas", "tu mochila", "tus cosas", "tus materiales", "tu inventario", "todo lo tuyo", "todo lo que tengas", "todo lo que lleves", "lo que lleves", "lo que tengas");
    }

    public record Step(String op, String target, String item, int n, int secs, String text) {
    }
}

