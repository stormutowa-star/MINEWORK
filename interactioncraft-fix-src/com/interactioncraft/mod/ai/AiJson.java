/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package com.interactioncraft.mod.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.interactioncraft.mod.works.PlanSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AiJson {
    private static final Set<String> ACTION_TYPES = Set.of("follow", "guard", "attack", "stop", "gather", "build", "pickup", "plan");
    private static final Set<String> NO_ACTION = Set.of("none", "null", "nada", "ninguna", "ninguno", "no", "nothing", "no_action", "ninguna_accion", "-", "n/a");
    private static final Set<String> PLAN_ALIASES = Set.of("accion", "accion_libre", "libre", "custom", "free", "act", "action", "acciones", "pasos", "steps");

    private AiJson() {
    }

    private static String actionType(String raw) {
        String t;
        String string = t = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (PLAN_ALIASES.contains(t)) {
            return "plan";
        }
        return ACTION_TYPES.contains(t) ? t : "";
    }

    private static String thenOf(JsonObject o) {
        String t = AiJson.str(o, "then").toLowerCase(Locale.ROOT);
        if (t.startsWith("build") || t.startsWith("constru")) {
            return "build";
        }
        if (t.startsWith("give") || t.startsWith("dar") || t.startsWith("entreg") || t.startsWith("bring") || t.startsWith("trae")) {
            return "give";
        }
        return "";
    }

    private static boolean flag(JsonObject o, String ... keys) {
        for (String k : keys) {
            String v;
            JsonElement e = o.get(k);
            if (e == null || !e.isJsonPrimitive() || !(v = e.getAsString().trim().toLowerCase(Locale.ROOT)).equals("true") && !v.equals("si") && !v.equals("s\u00ed") && !v.equals("yes") && !v.equals("1") && !v.equals("nueva") && !v.equals("nuevo") && !v.equals("otra") && !v.equals("otro")) continue;
            return true;
        }
        return false;
    }

    private static String stepsOf(JsonObject o) {
        for (String k : new String[]{"steps", "pasos", "plan", "secuencia", "sequence"}) {
            JsonElement e = o.get(k);
            if (e == null) continue;
            if (e.isJsonArray() || e.isJsonObject()) {
                return e.toString();
            }
            if (!e.isJsonPrimitive() || !e.getAsString().trim().startsWith("[")) continue;
            return e.getAsString().trim();
        }
        return "";
    }

    public static JsonObject parseObject(String raw) {
        if (raw == null) {
            return null;
        }
        String t = AiJson.stripFences(raw);
        int start = t.indexOf(123);
        int end = t.lastIndexOf(125);
        if (start < 0 || end <= start) {
            return null;
        }
        String slice = t.substring(start, end + 1);
        JsonObject o = AiJson.tryObject(slice);
        if (o == null) {
            o = AiJson.tryObject(AiJson.relax(slice));
        }
        return o != null ? o : AiJson.parseRepaired(raw);
    }

    private static JsonObject tryObject(String s) {
        if (s == null) {
            return null;
        }
        try {
            JsonElement el = JsonParser.parseString((String)s);
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    private static JsonObject parseRepaired(String raw) {
        String fixed = AiJson.repairTruncated(raw);
        if (fixed == null) {
            return null;
        }
        JsonObject o = AiJson.tryObject(fixed);
        return o != null ? o : AiJson.tryObject(AiJson.relax(fixed));
    }

    public static String relax(String s) {
        if (s == null) {
            return null;
        }
        String in = s.replace('\u201c', '\"').replace('\u201d', '\"');
        StringBuilder out = new StringBuilder(in.length() + 16);
        boolean inStr = false;
        boolean esc = false;
        int n = in.length();
        for (int i = 0; i < n; ++i) {
            int k;
            char c = in.charAt(i);
            if (inStr) {
                if (esc) {
                    esc = false;
                    out.append(c);
                    continue;
                }
                if (c == '\\') {
                    esc = true;
                    out.append(c);
                    continue;
                }
                if (c == '\"') {
                    if (AiJson.closesString(in, i + 1)) {
                        inStr = false;
                        out.append(c);
                        continue;
                    }
                    out.append("\\\"");
                    continue;
                }
                if (c == '\n') {
                    out.append("\\n");
                    continue;
                }
                out.append(c);
                continue;
            }
            if (c == '\"') {
                inStr = true;
                out.append(c);
                continue;
            }
            if (c == ',') {
                k = AiJson.skipSpace(in, i + 1);
                if (k < n && (in.charAt(k) == '}' || in.charAt(k) == ']')) continue;
                out.append(c);
                continue;
            }
            if (Character.isLetter(c) && (i == 0 || !Character.isLetterOrDigit(in.charAt(i - 1)))) {
                String word;
                for (k = i; k < n && Character.isLetter(in.charAt(k)); ++k) {
                }
                out.append(switch (word = in.substring(i, k)) {
                    case "True" -> "true";
                    case "False" -> "false";
                    case "None", "undefined" -> "null";
                    default -> word;
                });
                i = k - 1;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean closesString(String s, int from) {
        int k = AiJson.skipSpace(s, from);
        if (k >= s.length()) {
            return true;
        }
        char c = s.charAt(k);
        if (c == '}' || c == ']') {
            return true;
        }
        if (c == ':' || c == ',') {
            int v = AiJson.skipSpace(s, k + 1);
            if (v >= s.length()) {
                return true;
            }
            char d = s.charAt(v);
            return d == '\"' || d == '{' || d == '[' || d == '}' || d == ']' || d == '-' || Character.isDigit(d) || s.startsWith("true", v) || s.startsWith("false", v) || s.startsWith("null", v) || s.startsWith("True", v) || s.startsWith("False", v) || s.startsWith("None", v);
        }
        return false;
    }

    private static int skipSpace(String s, int from) {
        int k;
        for (k = from; k < s.length() && Character.isWhitespace(s.charAt(k)); ++k) {
        }
        return k;
    }

    public static String repairTruncated(String raw) {
        if (raw == null) {
            return null;
        }
        String t = AiJson.stripFences(raw);
        int start = t.indexOf(123);
        if (start < 0) {
            return null;
        }
        String s = t.substring(start);
        StringBuilder stack = new StringBuilder();
        boolean inStr = false;
        boolean esc = false;
        int safeLen = -1;
        String safeStack = "";
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (inStr) {
                if (esc) {
                    esc = false;
                    continue;
                }
                if (c == '\\') {
                    esc = true;
                    continue;
                }
                if (c != '\"') continue;
                inStr = false;
                continue;
            }
            if (c == '\"') {
                inStr = true;
                continue;
            }
            if (c == '{' || c == '[') {
                stack.append(c);
                safeLen = i + 1;
                safeStack = stack.toString();
                continue;
            }
            if (c == '}' || c == ']') {
                if (stack.length() == 0) {
                    return s.substring(0, i + 1);
                }
                stack.setLength(stack.length() - 1);
                if (stack.length() == 0) {
                    return s.substring(0, i + 1);
                }
                safeLen = i + 1;
                safeStack = stack.toString();
                continue;
            }
            if (c != ',') continue;
            safeLen = i;
            safeStack = stack.toString();
        }
        if (safeLen <= 0) {
            return null;
        }
        StringBuilder out = new StringBuilder(s.substring(0, safeLen));
        for (int k = safeStack.length() - 1; k >= 0; --k) {
            out.append(safeStack.charAt(k) == '{' ? (char)'}' : ']');
        }
        return out.toString();
    }

    public static String stripFences(String s) {
        int nl;
        int close;
        String t = s.trim();
        if (t.startsWith("```") && (close = (t = (nl = t.indexOf(10)) >= 0 ? t.substring(nl + 1) : t.substring(3)).lastIndexOf("```")) >= 0) {
            t = t.substring(0, close);
        }
        return t.trim();
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString().trim() : "";
    }

    public static String clean(String s, int max) {
        if (s == null) {
            return "";
        }
        Object t = s.replaceAll("\\*[^*\\n]{0,80}\\*", " ").replace('\n', ' ').replace('\r', ' ').trim();
        while (((String)t).length() > 1 && (((String)t).startsWith("\"") && ((String)t).endsWith("\"") || ((String)t).startsWith("\u00ab") && ((String)t).endsWith("\u00bb"))) {
            t = ((String)t).substring(1, ((String)t).length() - 1).trim();
        }
        if (((String)(t = ((String)t).replaceAll("\\s+", " "))).length() > max) {
            int cut = ((String)t).lastIndexOf(32, max - 1);
            t = ((String)t).substring(0, cut > max / 2 ? cut : max - 1).trim() + "\u2026";
        }
        return (String)t;
    }

    public static SceneReply parseScene(String raw, Set<String> ids, String fallbackId) {
        ArrayList<Line> lines = new ArrayList<Line>();
        ArrayList<Mem> mems = new ArrayList<Mem>();
        LinkedHashMap<String, Act> acts = new LinkedHashMap<String, Act>();
        JsonObject obj = AiJson.parseObject(raw);
        if (obj != null) {
            String npc;
            JsonObject o;
            if (obj.has("lines") && obj.get("lines").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("lines")) {
                    if (!el.isJsonObject()) continue;
                    o = el.getAsJsonObject();
                    npc = AiJson.str(o, "npc");
                    String say = AiJson.clean(AiJson.str(o, "say"), 260);
                    String inline = AiJson.actionType(AiJson.str(o, "action"));
                    if (!inline.isEmpty() && ids.contains(npc)) {
                        acts.put(npc, new Act(npc, inline));
                    }
                    if (say.isEmpty() || !ids.contains(npc)) continue;
                    lines.add(new Line(npc, say));
                }
            }
            if (obj.has("actions") && obj.get("actions").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("actions")) {
                    if (!el.isJsonObject()) continue;
                    o = el.getAsJsonObject();
                    npc = AiJson.str(o, "npc");
                    String rawType = AiJson.str(o, "do").isEmpty() ? (AiJson.str(o, "action").isEmpty() ? AiJson.str(o, "type") : AiJson.str(o, "action")) : AiJson.str(o, "do");
                    String type = AiJson.actionType(rawType);
                    String name = AiJson.text(o, "name", 40);
                    if (name.isEmpty()) {
                        name = AiJson.text(o, "nombre", 40);
                    }
                    if (type.isEmpty() && !AiJson.stepsOf(o).isEmpty()) {
                        type = "plan";
                    }
                    String forcedSteps = "";
                    if (type.isEmpty() && !rawType.isBlank() && !NO_ACTION.contains(rawType.trim().toLowerCase(Locale.ROOT)) && AiJson.stepsOf(o).isEmpty()) {
                        String known = PlanSpec.op(rawType);
                        if (known.isEmpty()) {
                            known = PlanSpec.fuzzyOp(rawType);
                        }
                        if (!known.isEmpty()) {
                            JsonObject one = o.deepCopy();
                            for (String drop : new String[]{"npc", "do", "action", "type", "name", "nombre"}) {
                                one.remove(drop);
                            }
                            one.addProperty("op", rawType);
                            JsonArray arr = new JsonArray();
                            arr.add((JsonElement)one);
                            forcedSteps = arr.toString();
                            type = "plan";
                            if (name.isEmpty()) {
                                name = AiJson.clean(rawType, 40);
                            }
                        }
                    }
                    boolean weak = false;
                    if (type.isEmpty() && !rawType.isBlank() && !NO_ACTION.contains(rawType.trim().toLowerCase(Locale.ROOT))) {
                        type = "plan";
                        weak = AiJson.stepsOf(o).isEmpty();
                        if (name.isEmpty()) {
                            name = AiJson.clean(rawType, 40);
                        }
                    }
                    if (type.isEmpty() || !ids.contains(npc)) continue;
                    int amount = 0;
                    try {
                        amount = o.has("amount") ? Math.max(0, Math.min(256, o.get("amount").getAsInt())) : 0;
                    }
                    catch (RuntimeException runtimeException) {
                        // empty catch block
                    }
                    String arg = AiJson.text(o, "what", 60);
                    String where = AiJson.text(o, "where", 60);
                    if (where.isEmpty()) {
                        where = AiJson.text(o, "donde", 60);
                    }
                    Act act = new Act(npc, type, arg, amount, AiJson.thenOf(o), AiJson.text(o, "spec", 200), where, AiJson.flag(o, "new", "nueva", "nuevo", "fresh", "otra"), name, forcedSteps.isEmpty() ? AiJson.stepsOf(o) : forcedSteps);
                    if (weak) {
                        acts.putIfAbsent(npc, act);
                        continue;
                    }
                    acts.put(npc, act);
                }
            }
            if (obj.has("memories") && obj.get("memories").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("memories")) {
                    if (!el.isJsonObject()) continue;
                    o = el.getAsJsonObject();
                    npc = AiJson.str(o, "npc");
                    String text = AiJson.clean(AiJson.str(o, "text"), 200);
                    if (text.isEmpty() || !ids.contains(npc)) continue;
                    int imp = 3;
                    try {
                        imp = Math.max(1, Math.min(5, o.get("importance").getAsInt()));
                    }
                    catch (RuntimeException runtimeException) {
                        // empty catch block
                    }
                    mems.add(new Mem(npc, text, imp));
                }
            }
        }
        if (lines.isEmpty() && obj == null && fallbackId != null) {
            String text;
            String stripped = AiJson.stripFences(raw == null ? "" : raw);
            boolean jsonish = stripped.startsWith("{") || stripped.contains("\"lines\"") || stripped.contains("\"actions\"");
            String string = text = jsonish ? "" : AiJson.clean(stripped, 260);
            if (!text.isEmpty()) {
                lines.add(new Line(fallbackId, text));
            }
        }
        ArrayList<Act> actions = new ArrayList<Act>(acts.values());
        return new SceneReply(lines, mems, actions);
    }

    public static List<String> strings(JsonObject o, String key, int maxItems, int maxLen) {
        String c;
        ArrayList<String> out = new ArrayList<String>();
        if (o == null || !o.has(key)) {
            return out;
        }
        JsonElement e = o.get(key);
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (JsonElement x : a) {
                String c2;
                if (!x.isJsonPrimitive() || out.size() >= maxItems || (c2 = AiJson.clean(x.getAsString(), maxLen)).isEmpty()) continue;
                out.add(c2);
            }
        } else if (e.isJsonPrimitive() && !(c = AiJson.clean(e.getAsString(), maxLen)).isEmpty()) {
            out.add(c);
        }
        return out;
    }

    public static String text(JsonObject o, String key, int maxLen) {
        return o == null ? "" : AiJson.clean(AiJson.str(o, key), maxLen);
    }

    public record Act(String npc, String type, String arg, int amount, String then, String spec, String where, boolean fresh, String name, String steps) {
        public Act(String npc, String type) {
            this(npc, type, "", 0, "", "", "", false, "", "");
        }

        public Act(String npc, String type, String arg, int amount, String then, String spec) {
            this(npc, type, arg, amount, then, spec, "", false, "", "");
        }
    }

    public record Line(String npc, String say) {
    }

    public record Mem(String npc, String text, int importance) {
    }

    public record SceneReply(List<Line> lines, List<Mem> memories, List<Act> actions) {
    }
}

