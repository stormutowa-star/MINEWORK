package com.lastbroadcast.client;

import com.lastbroadcast.client.hud.Hud;
import com.lastbroadcast.client.screen.DialogueScreen;
import com.lastbroadcast.client.screen.EndingScreen;
import com.lastbroadcast.client.sound.BroadcastAudio;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.network.NoticePacket;
import com.lastbroadcast.story.CaseState;
import net.minecraft.client.Minecraft;

/** Client-side handling of server packets (only ever loaded on the physical client). */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void onCaseSync(CaseState state) {
        CaseState.CLIENT = state;
    }

    public static void onNotice(int kind, String text) {
        Hud.notice(kind, text);
        switch (kind) {
            case NoticePacket.DEDUCTION -> BroadcastAudio.playUi("deduction", 0.9f, 1f);
            case NoticePacket.EVIDENCE -> BroadcastAudio.playUi("evidence", 0.8f, 1f);
            case NoticePacket.NO_MATCH -> BroadcastAudio.playUi("paper", 0.6f, 0.8f);
            default -> {}
        }
    }

    public static void onFragment(String fragment) {
        Hud.playFragment(fragment, ContentLibrary.FRAGMENTS.get(fragment));
    }

    public static void onEnding() {
        Minecraft.getInstance().setScreen(new EndingScreen());
    }

    public static void onOpenDialogue(int entityId, String character) {
        if (ContentLibrary.DIALOGUES.containsKey(character)) Minecraft.getInstance().setScreen(new DialogueScreen(entityId, character));
    }
}
