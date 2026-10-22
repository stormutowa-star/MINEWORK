package com.lastbroadcast.client;

/** Which NPC the local player is talking to, and whether that NPC is mid-sentence. */
public final class ClientDialogueState {
    public static int entityId = -1;
    public static boolean speaking = false;
    public static String pose = "gesture";

    private ClientDialogueState() {}

    public static void clear() {
        entityId = -1;
        speaking = false;
        pose = "gesture";
    }
}
