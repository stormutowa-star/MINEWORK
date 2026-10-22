package com.lastbroadcast.content;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lastbroadcast.LastBroadcast;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads all authored story content (broadcasts, documents, deductions, dialogue) from the mod jar.
 * Both logical sides read the same files, so server-side validation and client presentation agree.
 */
public final class ContentLibrary {
    private static final Gson GSON = new Gson();
    private static final String ROOT = "assets/lastbroadcast/content/";

    public static final Map<String, Broadcast> BROADCASTS = new LinkedHashMap<>();
    public static final Map<String, DocumentDef> DOCUMENTS = new LinkedHashMap<>();
    public static final Map<String, DeductionDef> DEDUCTIONS = new LinkedHashMap<>();
    public static final Map<String, DialogueDef> DIALOGUES = new LinkedHashMap<>();
    public static final Map<String, Speaker> SPEAKERS = new LinkedHashMap<>();
    public static final Map<String, EvidenceInfo> EXTRA_EVIDENCE = new LinkedHashMap<>();
    public static final Map<String, List<Broadcast.Line>> FRAGMENTS = new LinkedHashMap<>();
    private static boolean loaded = false;

    public static class Speaker {
        public String name = "";
        public String voice = "voice_dot";
        public String color = "#ffffff";
        public float pitch = 1f;
    }

    public static class EvidenceInfo {
        public String title = "";
        public String note = "";
        public String kind = "misc";
    }

    public static class Index {
        List<String> broadcasts = new ArrayList<>();
        List<String> dialogues = new ArrayList<>();
        Map<String, Speaker> speakers = new LinkedHashMap<>();
        Map<String, EvidenceInfo> evidence = new LinkedHashMap<>();
        Map<String, List<Broadcast.Line>> fragments = new LinkedHashMap<>();
    }

    private ContentLibrary() {}

    /** Language the story content is currently loaded in: "en" or "es". */
    private static String language = "en";

    /** Loads the English story content once (server and common setup). */
    public static synchronized void load() {
        if (loaded) return;
        loadFor("en");
    }

    /**
     * Reloads the story content for a Minecraft language code (any Spanish variant uses the Spanish
     * files under content/es/). Called on the client at start-up and whenever the language changes.
     */
    public static synchronized void reloadFor(String languageCode) {
        String lang = languageCode != null && languageCode.toLowerCase(Locale.ROOT).startsWith("es") ? "es" : "en";
        if (loaded && lang.equals(language)) return;
        try {
            loadFor(lang);
        } catch (RuntimeException e) {
            LastBroadcast.LOG.error("Could not load '{}' story content, falling back to English", lang, e);
            loadFor("en");
        }
    }

    private static void loadFor(String lang) {
        try {
            Map<String, Broadcast> broadcasts = new LinkedHashMap<>();
            Map<String, DocumentDef> documents = new LinkedHashMap<>();
            Map<String, DeductionDef> deductions = new LinkedHashMap<>();
            Map<String, DialogueDef> dialogues = new LinkedHashMap<>();
            Index index = read("index.json", Index.class, lang);
            for (String b : index.broadcasts) {
                Broadcast bc = read("broadcasts/" + b + ".json", Broadcast.class, lang);
                if (bc.id == null) bc.id = b;
                bc.shots.sort(Comparator.comparingDouble(s -> s.t));
                bc.lines.sort(Comparator.comparingDouble(l -> l.t));
                bc.sounds.sort(Comparator.comparingDouble(s -> s.t));
                broadcasts.put(bc.id, bc);
            }
            List<DocumentDef> docs = read("documents.json", new TypeToken<List<DocumentDef>>() {}.getType(), lang);
            for (DocumentDef d : docs) documents.put(d.id, d);
            List<DeductionDef> deds = read("deductions.json", new TypeToken<List<DeductionDef>>() {}.getType(), lang);
            for (DeductionDef d : deds) deductions.put(d.id, d);
            for (String c : index.dialogues) dialogues.put(c, read("dialogue/" + c + ".json", DialogueDef.class, lang));
            replace(SPEAKERS, index.speakers);
            replace(EXTRA_EVIDENCE, index.evidence);
            replace(FRAGMENTS, index.fragments);
            replace(BROADCASTS, broadcasts);
            replace(DOCUMENTS, documents);
            replace(DEDUCTIONS, deductions);
            replace(DIALOGUES, dialogues);
            language = lang;
            loaded = true;
            LastBroadcast.LOG.info("Loaded {} broadcasts, {} documents, {} deductions, {} dialogue trees ({})",
                    BROADCASTS.size(), DOCUMENTS.size(), DEDUCTIONS.size(), DIALOGUES.size(), lang);
        } catch (Exception e) {
            LastBroadcast.LOG.error("Failed to load The Last Broadcast story content", e);
            throw new IllegalStateException("The Last Broadcast: story content failed to load", e);
        }
    }

    private static <T> void replace(Map<String, T> target, Map<String, T> source) {
        target.clear();
        target.putAll(source);
    }

    private static <T> T read(String file, java.lang.reflect.Type type, String lang) throws IOException {
        try (Reader r = open(file, lang)) {
            T t = GSON.fromJson(r, type);
            if (t == null) throw new IOException("Empty content file " + file);
            return t;
        }
    }

    private static Reader open(String file, String lang) throws IOException {
        InputStream in = null;
        if (!"en".equals(lang)) in = openStream(ROOT + lang + "/" + file);
        if (in == null) in = openStream(ROOT + file);
        if (in == null) throw new IOException("Missing content file " + ROOT + file);
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    private static InputStream openStream(String path) {
        InputStream in = null;
        try {
            var mod = ModList.get() == null ? null : ModList.get().getModFileById(LastBroadcast.MODID);
            if (mod != null) {
                Path p = mod.getFile().findResource(path.split("/"));
                if (p != null && Files.exists(p)) in = Files.newInputStream(p);
            }
        } catch (Exception e) {
            LastBroadcast.LOG.debug("Mod file lookup failed for {}, falling back to classpath", path, e);
        }
        if (in == null) in = LastBroadcast.class.getResourceAsStream("/" + path);
        return in;
    }

    // ---------------------------------------------------------------- lookups

    public static Broadcast broadcast(String id) {
        return id == null ? null : BROADCASTS.get(id);
    }

    public static Speaker speaker(String id) {
        Speaker s = id == null ? null : SPEAKERS.get(id);
        if (s == null) {
            s = new Speaker();
            s.name = id == null ? "" : id;
        }
        return s;
    }

    /** Current story language: "en" or "es". */
    public static String language() {
        return language;
    }

    /** Label printed on a recorded reel, in the current language. */
    public static String tapeLabel(String broadcastId) {
        Broadcast b = broadcast(broadcastId);
        if (b == null) return null;
        if ("memo_penrose".equals(broadcastId) && b.evidenceTitle != null) return b.evidenceTitle;
        return b.title;
    }

    /** Human readable title of an evidence id (doc:x, tape:x, frame:x ...). */
    public static String evidenceTitle(String evidenceId) {
        EvidenceInfo info = evidenceInfo(evidenceId);
        return info.title;
    }

    public static EvidenceInfo evidenceInfo(String evidenceId) {
        EvidenceInfo extra = EXTRA_EVIDENCE.get(evidenceId);
        if (extra != null) return extra;
        EvidenceInfo info = new EvidenceInfo();
        if (evidenceId.startsWith("doc:")) {
            DocumentDef d = DOCUMENTS.get(evidenceId.substring(4));
            info.kind = "document";
            info.title = d != null ? d.title : evidenceId;
            info.note = d != null ? d.note : "";
        } else if (evidenceId.startsWith("tape:")) {
            Broadcast b = BROADCASTS.get(evidenceId.substring(5));
            info.kind = "tape";
            info.title = b != null ? (b.evidenceTitle != null ? b.evidenceTitle : "Recording: " + b.title) : evidenceId;
            info.note = b != null && b.evidenceNote != null ? b.evidenceNote : "";
        } else {
            info.title = evidenceId;
        }
        return info;
    }
}
