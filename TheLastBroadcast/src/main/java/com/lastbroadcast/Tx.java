package com.lastbroadcast;

/**
 * Language-neutral text for packets and server messages. The server sends a translation key (plus
 * arguments) or a reference to story content, and the client resolves it in the player's language.
 */
public final class Tx {
    public static final char SEP = '\u0001', KEY = '\u0002', REF = '\u0004';

    private Tx() {}

    /** Encodes a translation key with optional arguments (arguments may themselves be keys or refs). */
    public static String key(String key, Object... args) {
        StringBuilder sb = new StringBuilder().append(KEY).append(key);
        for (Object a : args) sb.append(SEP).append(a);
        return sb.toString();
    }

    /** Reference to story content resolved on the client: kind 'E' = evidence title, 'D' = deduction title. */
    public static String ref(char kind, String id) {
        return "" + REF + kind + id;
    }
}
