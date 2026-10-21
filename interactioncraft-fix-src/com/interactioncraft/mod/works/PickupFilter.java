/*
 * Decompiled with CFR 0.152.
 */
package com.interactioncraft.mod.works;

import com.interactioncraft.mod.works.Materials;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class PickupFilter {
    private static final Set<String> STOP = Set.of("de", "del", "la", "las", "el", "los", "lo", "a", "al", "en", "con", "para", "por", "que", "mi", "mis", "tu", "tus", "su", "sus", "te", "me", "le", "les", "se", "nos", "un", "una", "unos", "unas", "uno", "este", "esta", "estos", "estas", "ese", "esa", "esos", "esas", "eso", "esto", "aquel", "aquella", "aquellos", "aquellas", "ahi", "aqui", "alli", "alla", "cerca", "suelo", "piso", "tirado", "tirados", "tirada", "tiradas", "tire", "tiro", "deje", "dejo", "dejado", "dejados", "puse", "todo", "todos", "toda", "todas", "cosa", "cosas", "objeto", "objetos", "item", "items", "recoge", "recoger", "recogelo", "recogelos", "coge", "coger", "agarra", "junta", "hay", "ya", "favor", "porfa", "porfavor", "tambien", "mas", "demas", "resto", "please", "the", "all", "everything", "stuff", "things", "of", "and", "those", "these", "that", "it", "them", "on", "ground", "floor", "pick", "up", "some", "any");
    private static final Set<String> OR = Set.of("y", "e", "o", "u", "and", "or");
    private static final Map<String, List<String>> WORDS = new HashMap<String, List<String>>();
    private static final Pattern MARKS;
    private static final Pattern NOT_ALNUM;

    private PickupFilter() {
    }

    private static Map<String, String> ES;

    /** Nombres en espanol (todas las variantes de Minecraft) del objeto/bloque/criatura de un id; asi "cubo", "tijeras" o "vaca" casan con minecraft:bucket, shears o cow. */
    public static synchronized String spanish(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        if (ES == null) {
            ES = new HashMap<String, String>();
            try (InputStream in = PickupFilter.class.getResourceAsStream("/assets/interactioncraft/es_names.txt")) {
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    while ((line = r.readLine()) != null) {
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            ES.put(line.substring(0, eq), line.substring(eq + 1).replace('|', ' '));
                        }
                    }
                }
            }
            catch (Exception | LinkageError e) {
                // sin diccionario: solo nombres del juego
            }
        }
        int colon = id.indexOf(':');
        String ns = colon > 0 ? id.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        String own = "minecraft".equals(ns) ? ES.getOrDefault(path, "") : "";
        String live = "";
        try {
            net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
            for (String kind : new String[]{"item", "block", "entity"}) {
                String key = kind + "." + ns + "." + path.replace('/', '.');
                if (lang.has(key)) {
                    live = live + " " + lang.getOrDefault(key);
                }
            }
        }
        catch (Exception | LinkageError e) {
            // sin idioma cargado
        }
        return (own + " " + live).trim();
    }

    private static void w(List<String> frags, String ... words) {
        for (String word : words) {
            WORDS.put(word, frags);
        }
    }

    public static int score(String query, String id, String name) {
        List<List<String>> alts = PickupFilter.parse(query);
        if (alts.isEmpty()) {
            return 0;
        }
        String nq = PickupFilter.norm(query);
        String nn = PickupFilter.norm(name);
        if (!nn.isEmpty() && nn.equals(nq)) {
            return 100;
        }
        Item it = new Item(id == null ? "" : id, name == null ? "" : name, false);
        int best = 0;
        for (List<String> group : alts) {
            int hits = 0;
            boolean allKnownHit = true;
            boolean headHit = false;
            boolean sawKnown = false;
            for (String word : group) {
                if (PickupFilter.category(word)) continue;
                boolean known = PickupFilter.keyword(word) != null;
                boolean hit = PickupFilter.matches(word, it);
                if (known) {
                    if (!sawKnown) {
                        headHit = hit;
                    }
                    sawKnown = true;
                    if (!hit) {
                        allKnownHit = false;
                    }
                }
                if (!hit) continue;
                ++hits;
            }
            if (!sawKnown) {
                if (hits <= 0) continue;
                best = Math.max(best, 30 + Math.min(20, hits * 5));
                continue;
            }
            if (allKnownHit && hits > 0) {
                best = Math.max(best, 40 + Math.min(20, hits * 5) + (!nn.isEmpty() && nn.contains(String.join((CharSequence)" ", group)) ? 20 : 0));
                continue;
            }
            if (!headHit) continue;
            best = Math.max(best, 25 + hits * 3);
        }
        return best;
    }

    public static String norm(String s) {
        if (s == null) {
            return "";
        }
        String d = MARKS.matcher(Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("");
        return NOT_ALNUM.matcher(d).replaceAll(" ").trim();
    }

    private static String keyword(String word) {
        if (WORDS.containsKey(word)) {
            return word;
        }
        if (word.endsWith("s") && WORDS.containsKey(word.substring(0, word.length() - 1))) {
            return word.substring(0, word.length() - 1);
        }
        if (word.endsWith("es") && WORDS.containsKey(word.substring(0, word.length() - 2))) {
            return word.substring(0, word.length() - 2);
        }
        return null;
    }

    private static boolean category(String word) {
        return word.startsWith("bloque") || word.equals("block") || word.equals("blocks") || word.startsWith("material");
    }

    private static boolean matches(String word, Item it) {
        String path;
        String string = path = it.id().contains(":") ? it.id().substring(it.id().indexOf(58) + 1) : it.id();
        if (word.startsWith("bloque") || word.equals("block") || word.equals("blocks")) {
            return it.block();
        }
        if (word.startsWith("material")) {
            return it.block() || Materials.looksLikeMaterial(it.id());
        }
        String k = PickupFilter.keyword(word);
        if (k != null) {
            for (String f : WORDS.get(k)) {
                if (!path.contains(f)) continue;
                return true;
            }
        }
        String name = " " + PickupFilter.norm(it.name() + " " + PickupFilter.spanish(it.id())) + " ";
        for (String w : PickupFilter.variants(word)) {
            if (w.length() < 3 || !name.contains(" " + w + " ") && !name.contains(" " + w) && !path.contains(w)) continue;
            return true;
        }
        return false;
    }

    private static List<String> variants(String word) {
        ArrayList<String> out = new ArrayList<String>();
        out.add(word);
        if (word.length() > 4 && word.endsWith("es")) {
            out.add(word.substring(0, word.length() - 2));
        }
        if (word.length() > 3 && word.endsWith("s")) {
            out.add(word.substring(0, word.length() - 1));
        }
        return out;
    }

    static List<List<String>> parse(String what) {
        ArrayList<List<String>> alts = new ArrayList<List<String>>();
        ArrayList<String> cur = new ArrayList<String>();
        for (String word : PickupFilter.norm(what).split(" ")) {
            if (word.isEmpty()) continue;
            if (OR.contains(word)) {
                if (!cur.isEmpty()) {
                    alts.add(cur);
                }
                cur = new ArrayList();
                continue;
            }
            if (STOP.contains(word)) continue;
            cur.add(word);
        }
        if (!cur.isEmpty()) {
            alts.add(cur);
        }
        return alts;
    }

    public static boolean all(String what) {
        return PickupFilter.parse(what).isEmpty();
    }

    public static List<Integer> select(List<Item> items, String what) {
        List<List<String>> alts = PickupFilter.parse(what);
        ArrayList<Integer> every = new ArrayList<Integer>();
        for (int i = 0; i < items.size(); ++i) {
            every.add(i);
        }
        if (alts.isEmpty()) {
            return every;
        }
        LinkedHashSet<Integer> out = new LinkedHashSet<Integer>();
        boolean concrete = false;
        for (List<String> group : alts) {
            ArrayList<String> useful = new ArrayList<String>();
            for (String word : group) {
                boolean known = PickupFilter.keyword(word) != null || PickupFilter.category(word);
                boolean hits = false;
                for (Item it : items) {
                    if (!PickupFilter.matches(word, it)) continue;
                    hits = true;
                    break;
                }
                if (!known && !hits) continue;
                useful.add(word);
            }
            if (useful.isEmpty()) continue;
            concrete = true;
            for (int i = 0; i < items.size(); ++i) {
                boolean ok = true;
                for (String word : useful) {
                    if (PickupFilter.matches(word, items.get(i))) continue;
                    ok = false;
                    break;
                }
                if (!ok) continue;
                out.add(i);
            }
        }
        return concrete ? new ArrayList<Integer>(out) : every;
    }

    static {
        PickupFilter.w(List.of("log", "wood", "planks", "stick", "_stem", "hyphae", "bamboo"), "madera", "wood");
        PickupFilter.w(List.of("log", "_wood", "_stem", "hyphae"), "tronco", "leno");
        PickupFilter.w(List.of("planks"), "tablon", "tabla", "tablero");
        PickupFilter.w(List.of("stick"), "palo", "palito");
        PickupFilter.w(List.of("stone", "cobble", "andesite", "diorite", "granite", "deepslate", "tuff", "blackstone"), "piedra", "roca");
        PickupFilter.w(List.of("cobblestone", "cobbled"), "adoquin", "guijarro");
        PickupFilter.w(List.of("brick"), "ladrillo");
        PickupFilter.w(List.of("glass"), "cristal", "vidrio");
        PickupFilter.w(List.of("glass"), "ventana");
        PickupFilter.w(List.of("dirt", "grass_block", "podzol", "mud", "farmland"), "tierra", "barro");
        PickupFilter.w(List.of("farmland", "wheat", "carrots", "potatoes", "beetroots"), "cultivo", "sembrado", "huerto");
        PickupFilter.w(List.of("sand"), "arena");
        PickupFilter.w(List.of("gravel"), "grava");
        PickupFilter.w(List.of("wool"), "lana");
        PickupFilter.w(List.of("door"), "puerta");
        PickupFilter.w(List.of("torch"), "antorcha");
        PickupFilter.w(List.of("coal"), "carbon");
        PickupFilter.w(List.of("iron"), "hierro");
        PickupFilter.w(List.of("gold"), "oro");
        PickupFilter.w(List.of("diamond"), "diamante");
        PickupFilter.w(List.of("stairs", "ladder"), "escalera", "escalon");
        PickupFilter.w(List.of("slab"), "losa");
        PickupFilter.w(List.of("fence"), "valla");
        PickupFilter.w(List.of("_bed"), "cama");
        PickupFilter.w(List.of("leaves"), "hoja");
        PickupFilter.w(List.of("clay"), "arcilla");
        PickupFilter.w(List.of("terracotta"), "terracota");
        PickupFilter.w(List.of("concrete"), "hormigon", "cemento");
        PickupFilter.w(List.of("quartz"), "cuarzo");
        PickupFilter.w(List.of("obsidian"), "obsidiana");
        PickupFilter.w(List.of("snow"), "nieve");
        PickupFilter.w(List.of("ice"), "hielo");
        PickupFilter.w(List.of("lantern"), "farol", "linterna");
        PickupFilter.w(List.of("chest"), "cofre", "baul");
        PickupFilter.w(List.of("furnace"), "horno");
        PickupFilter.w(List.of("oak"), "roble");
        PickupFilter.w(List.of("spruce"), "abeto", "pino");
        PickupFilter.w(List.of("birch"), "abedul");
        PickupFilter.w(List.of("jungle"), "jungla");
        PickupFilter.w(List.of("acacia"), "acacia");
        PickupFilter.w(List.of("mangrove"), "mangle");
        PickupFilter.w(List.of("cherry"), "cerezo");
        PickupFilter.w(List.of("dark_"), "oscuro", "oscura");
        PickupFilter.w(List.of("crimson"), "carmesi");
        PickupFilter.w(List.of("warped"), "distorsionado", "distorsionada");
        PickupFilter.w(List.of("campfire"), "fogata", "hoguera", "fuego");
        PickupFilter.w(List.of("button"), "boton");
        PickupFilter.w(List.of("lever"), "palanca");
        PickupFilter.w(List.of("trapdoor"), "trampilla");
        PickupFilter.w(List.of("fence_gate"), "portillo", "cancela", "tranquera");
        PickupFilter.w(List.of("crafting_table"), "crafteo", "trabajo");
        PickupFilter.w(List.of("anvil"), "yunque");
        PickupFilter.w(List.of("bell"), "campana");
        PickupFilter.w(List.of("barrel"), "barril");
        PickupFilter.w(List.of("note_block"), "musical");
        PickupFilter.w(List.of("jukebox"), "tocadiscos", "rocola");
        PickupFilter.w(List.of("lectern"), "atril");
        PickupFilter.w(List.of("composter"), "compostador", "compostera");
        PickupFilter.w(List.of("cauldron"), "caldero");
        PickupFilter.w(List.of("poppy", "dandelion", "tulip", "orchid", "allium", "azure", "oxeye", "cornflower", "lily_of", "sunflower", "lilac", "rose", "peony", "torchflower", "flower"), "flor", "ramo");
        PickupFilter.w(List.of("wheat"), "trigo");
        PickupFilter.w(List.of("carrot"), "zanahoria");
        PickupFilter.w(List.of("potato"), "patata", "papa");
        PickupFilter.w(List.of("beetroot"), "remolacha");
        PickupFilter.w(List.of("pumpkin"), "calabaza");
        PickupFilter.w(List.of("melon"), "sandia", "melon");
        PickupFilter.w(List.of("seeds"), "semilla");
        PickupFilter.w(List.of("water"), "agua");
        PickupFilter.w(List.of("lava"), "lava");
        PickupFilter.w(List.of("log", "leaves"), "arbol");
        PickupFilter.w(List.of("short_grass", "tall_grass", "grass"), "hierba", "cesped", "pasto");
        PickupFilter.w(List.of("cow", "mooshroom"), "vaca");
        PickupFilter.w(List.of("sheep"), "oveja");
        PickupFilter.w(List.of("pig"), "cerdo", "chancho");
        PickupFilter.w(List.of("chicken"), "gallina", "pollo");
        PickupFilter.w(List.of("horse", "donkey", "mule"), "caballo", "burro", "mula");
        PickupFilter.w(List.of("wolf"), "lobo", "perro");
        PickupFilter.w(List.of("cat", "ocelot"), "gato", "gata");
        PickupFilter.w(List.of("villager"), "aldeano", "aldeana");
        PickupFilter.w(List.of("zombie"), "zombi", "zombie");
        PickupFilter.w(List.of("skeleton"), "esqueleto");
        PickupFilter.w(List.of("spider"), "arana");
        PickupFilter.w(List.of("creeper"), "creeper");
        PickupFilter.w(List.of("car", "vehicle", "truck", "kart", "jeep"), "coche", "auto", "carro", "vehiculo", "camion");
        PickupFilter.w(List.of("boat", "raft"), "barca", "bote", "barco");
        PickupFilter.w(List.of("minecart"), "vagoneta");
        PickupFilter.w(List.of("beef", "porkchop", "mutton", "chicken", "rabbit", "rotten_flesh", "steak"), "carne");
        PickupFilter.w(List.of("rotten_flesh"), "podrida");
        PickupFilter.w(List.of("leather"), "cuero", "piel");
        PickupFilter.w(List.of("feather"), "pluma");
        PickupFilter.w(List.of("bone"), "hueso");
        PickupFilter.w(List.of("gunpowder"), "polvora");
        PickupFilter.w(List.of("string", "cobweb"), "hilo", "cuerda", "telarana");
        PickupFilter.w(List.of("arrow"), "flecha");
        PickupFilter.w(List.of("_egg"), "huevo");
        PickupFilter.w(List.of("apple"), "manzana");
        PickupFilter.w(List.of("bread"), "pan");
        PickupFilter.w(List.of("cod", "salmon", "tropical_fish", "pufferfish"), "pescado");
        PickupFilter.w(List.of("ender_pearl"), "perla");
        PickupFilter.w(List.of("slime_ball", "slime"), "baba");
        PickupFilter.w(List.of("witch"), "bruja", "brujo");
        PickupFilter.w(List.of("enderman"), "enderman");
        PickupFilter.w(List.of("slime"), "slime");
        PickupFilter.w(List.of("bat"), "murcielago");
        PickupFilter.w(List.of("rabbit"), "conejo");
        PickupFilter.w(List.of("turtle"), "tortuga");
        PickupFilter.w(List.of("parrot"), "loro");
        PickupFilter.w(List.of("fox"), "zorro");
        PickupFilter.w(List.of("bee"), "abeja");
        PickupFilter.w(List.of("goat"), "cabra");
        PickupFilter.w(List.of("llama"), "llama");
        PickupFilter.w(List.of("squid"), "calamar");
        PickupFilter.w(List.of("dolphin"), "delfin");
        PickupFilter.w(List.of("golem"), "golem");
        PickupFilter.w(List.of("pillager", "vindicator", "evoker", "ravager"), "saqueador", "vindicador", "invocador");
        PickupFilter.w(List.of("drowned"), "ahogado");
        PickupFilter.w(List.of("husk"), "husk");
        PickupFilter.w(List.of("piglin"), "piglin");
        PickupFilter.w(List.of("hoglin"), "hoglin");
        PickupFilter.w(List.of("blaze"), "blaze");
        PickupFilter.w(List.of("ghast"), "ghast");
        PickupFilter.w(List.of("magma_cube"), "magma");
        PickupFilter.w(List.of("silverfish"), "lepisma");
        PickupFilter.w(List.of("cod", "salmon", "tropical_fish"), "pez");
        PickupFilter.w(List.of("strider"), "strider");
        PickupFilter.w(List.of("panda"), "panda");
        PickupFilter.w(List.of("polar_bear"), "oso");
        PickupFilter.w(List.of("phantom"), "fantasma");
        MARKS = Pattern.compile("\\p{M}+");
        NOT_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]+");
    }

    public record Item(String id, String name, boolean block) {
    }
}

