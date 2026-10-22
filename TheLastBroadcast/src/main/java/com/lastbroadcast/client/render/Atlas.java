package com.lastbroadcast.client.render;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lastbroadcast.LastBroadcast;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Index of the single broadcast texture atlas (backgrounds, presenters, noise, UI sprites).
 * Everything a screen draws lives in one texture so all layers render in submission order.
 */
public final class Atlas {
    public static final ResourceLocation TEXTURE = LastBroadcast.id("textures/broadcast/atlas.png");
    private static final ResourceLocation INDEX = LastBroadcast.id("textures/broadcast/atlas.json");
    public static final float SIZE = 2048f;

    public record Region(float x, float y, float w, float h) {
        public float u0() { return x / SIZE; }
        public float v0() { return y / SIZE; }
        public float u1() { return (x + w) / SIZE; }
        public float v1() { return (y + h) / SIZE; }
    }

    private static Map<String, Region> regions;

    private Atlas() {}

    public static Region get(String name) {
        if (regions == null) load();
        Region r = regions.get(name);
        return r != null ? r : regions.get("missing");
    }

    public static boolean has(String name) {
        if (regions == null) load();
        return regions.containsKey(name);
    }

    public static synchronized void load() {
        Map<String, Region> out = new HashMap<>();
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(INDEX);
            if (res.isPresent()) {
                try (Reader r = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                    Map<String, float[]> raw = new Gson().fromJson(r, new TypeToken<Map<String, float[]>>() {}.getType());
                    raw.forEach((k, v) -> out.put(k, new Region(v[0], v[1], v[2], v[3])));
                }
            } else {
                LastBroadcast.LOG.error("Broadcast atlas index missing: {}", INDEX);
            }
        } catch (Exception e) {
            LastBroadcast.LOG.error("Failed to read broadcast atlas index", e);
        }
        out.putIfAbsent("missing", new Region(0, 0, 4, 4));
        out.putIfAbsent("white", new Region(0, 0, 4, 4));
        regions = out;
    }

    public static void invalidate() {
        regions = null;
    }
}
