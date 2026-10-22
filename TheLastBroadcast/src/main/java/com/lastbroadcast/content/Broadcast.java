package com.lastbroadcast.content;

import java.util.ArrayList;
import java.util.List;

/**
 * An authored television programme: a timeline of camera shots, spoken lines and sound cues.
 * Loaded from assets/lastbroadcast/broadcasts/*.json. Times are in seconds.
 */
public class Broadcast {
    public String id;
    public String title = "";
    public double duration = 10;
    /** Rendered from desaturated artwork (WKVR has no colour cameras until Wednesday). */
    public boolean mono = false;
    /** Audio-only reel: shows a VU meter instead of a picture during playback. */
    public boolean audioOnly = false;
    /** Programme cannot be recorded (static, dead carrier). */
    public boolean unrecordable = false;
    public String evidenceTitle;
    public String evidenceNote;
    /** Default per-programme effects, merged under each shot's effects. */
    public Fx fx = new Fx();
    public List<Shot> shots = new ArrayList<>();
    public List<Line> lines = new ArrayList<>();
    public List<SoundCue> sounds = new ArrayList<>();
    public Flash flash;

    public int durationTicks() {
        return Math.max(1, (int) Math.round(duration * 20.0));
    }

    public Shot shotAt(double t) {
        Shot current = shots.isEmpty() ? null : shots.get(0);
        for (Shot s : shots) {
            if (s.t <= t) current = s;
            else break;
        }
        return current;
    }

    public Shot previousShot(Shot s) {
        int i = shots.indexOf(s);
        return i > 0 ? shots.get(i - 1) : null;
    }

    public Line lineAt(double t) {
        for (Line l : lines) {
            if (t >= l.t && t < l.t + l.d) return l;
        }
        return null;
    }

    public static class Shot {
        public double t;
        public String bg = "black";
        /** Camera framing in scene units (scene is 160x120): x, y, w, h. */
        public float[] crop;
        /** Framing at the end of the shot (camera move). */
        public float[] cropEnd;
        public List<Actor> actors = new ArrayList<>();
        public List<TextEl> texts = new ArrayList<>();
        public Fx fx;
        /** cut | dissolve | static | roll */
        public String trans = "cut";
        public double transDur = 0.5;
        /** Optional colour multiplier over the whole picture, e.g. "#ffd0a0". */
        public String tint;
    }

    public static class Actor {
        public String sprite;
        public float x, y;
        /** Height in scene units. */
        public float h = 60;
        /** Speaker id whose lines animate this actor's mouth. */
        public String talk;
        /** Optional end position for movement over the shot. */
        public float[] moveTo;
        public boolean flip = false;
    }

    public static class TextEl {
        public String s = "";
        public float x, y;
        public float scale = 1f;
        public String color = "#ffffff";
        public boolean center = true;
        /** Background box colour (ARGB hex) or null. */
        public String box;
        public boolean shadow = true;
        /** Visible window relative to the shot start. */
        public double t0 = 0, t1 = 9999;
        public boolean blink = false;
        /** Text layer is part of the scene (zooms with the camera) instead of a caption. */
        public boolean scene = false;
    }

    public static class Fx {
        public Float stat;
        public Float roll;
        public Float tear;
        public Float ghost;
        public Float flicker;
        public Float bars;
        public Float vignette;
        public Float shake;

        public static float v(Float a, Float b, float def) {
            return a != null ? a : (b != null ? b : def);
        }
    }

    public static class Line {
        public double t, d;
        public String who;
        public String text = "";
        /** Text heard when the tape runs backwards; defaults to reversed letters. */
        public String rev;
        /** Line is only heard when played in reverse (forward shows garble). */
        public boolean reversedSpeech = false;
    }

    public static class SoundCue {
        public double t;
        public String id;
        public float vol = 1f;
        public float pitch = 1f;
    }

    public static class Flash {
        public double t, d;
        public String bg;
        public String evidence;
        public String caption;
    }
}
