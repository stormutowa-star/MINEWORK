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

    public static synchronized void load() {
        if (loaded) return;
        try {
            Index index = read("index.json", Index.class);
            SPEAKERS.putAll(index.speakers);
            EXTRA_EVIDENCE.putAll(index.evidence);
            FRAGMENTS.putAll(index.fragments);
            for (String b : index.broadcasts) {
                Broadcast bc = read("broadcasts/" + b + ".json", Broadcast.class);
                if (bc.id == null) bc.id = b;
                bc.shots.sort(Comparator.comparingDouble(s -> s.t));
                bc.lines.sort(Comparator.comparingDouble(l -> l.t));
                bc.sounds.sort(Comparator.comparingDouble(s -> s.t));
                BROADCASTS.put(bc.id, bc);
            }
            List<DocumentDef> docs = read("documents.json", new TypeToken<List<DocumentDef>>() {}.getType());
            for (DocumentDef d : docs) DOCUMENTS.put(d.id, d);
            List<DeductionDef> deds = read("deductions.json", new TypeToken<List<DeductionDef>>() {}.getType());
            for (DeductionDef d : deds) DEDUCTIONS.put(d.id, d);
            for (String c : index.dialogues) DIALOGUES.put(c, read("dialogue/" + c + ".json", DialogueDef.class));
            loaded = true;
            LastBroadcast.LOG.info("Loaded {} broadcasts, {} documents, {} deductions, {} dialogue trees",
                    BROADCASTS.size(), DOCUMENTS.size(), DEDUCTIONS.size(), DIALOGUES.size());
        } catch (Exception e) {
            LastBroadcast.LOG.error("Failed to load The Last Broadcast story content", e);
            throw new IllegalStateException("The Last Broadcast: story content failed to load", e);
        }
    }

    private static <T> T read(String file, java.lang.reflect.Type type) throws IOException {
        try (Reader r = open(file)) {
            T t = GSON.fromJson(r, type);
            if (t == null) throw new IOException("Empty content file " + file);
            return t;
        }
    }

    private static Reader open(String file) throws IOException {
        String path = ROOT + file;
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
        if (in == null) throw new IOException("Missing content file " + path);
        return new InputStreamReader(in, StandardCharsets.UTF_8);
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
