package com.lastbroadcast;

import net.minecraft.core.BlockPos;

/**
 * Common-side entry points into client-only code. Only ever invoked when running on the logical
 * client; the client classes are resolved lazily so dedicated servers never load them.
 */
public final class ClientBridge {
    private ClientBridge() {}

    public static void openTelevision(BlockPos pos) {
        com.lastbroadcast.client.ClientScreens.openTelevision(pos);
    }

    public static void openAnalyzer(BlockPos pos) {
        com.lastbroadcast.client.ClientScreens.openAnalyzer(pos);
    }

    public static void openRecorder(BlockPos pos) {
        com.lastbroadcast.client.ClientScreens.openRecorder(pos);
    }

    public static void openJournal() {
        com.lastbroadcast.client.ClientScreens.openJournal();
    }

    public static void openKeypad(BlockPos pos) {
        com.lastbroadcast.client.ClientScreens.openKeypad(pos);
    }

    public static void openConsole(BlockPos pos) {
        com.lastbroadcast.client.ClientScreens.openConsole(pos);
    }

    public static void openDocument(String docId) {
        com.lastbroadcast.client.ClientScreens.openDocument(docId);
    }
}
