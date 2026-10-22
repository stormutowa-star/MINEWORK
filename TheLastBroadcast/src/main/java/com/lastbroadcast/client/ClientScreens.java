package com.lastbroadcast.client;

import com.lastbroadcast.client.screen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientScreens {
    private ClientScreens() {}

    private static void open(net.minecraft.client.gui.screens.Screen s) {
        Minecraft.getInstance().setScreen(s);
    }

    public static void openTelevision(BlockPos pos) { open(new TelevisionScreen(pos)); }
    public static void openAnalyzer(BlockPos pos) { open(new AnalyzerScreen(pos)); }
    public static void openRecorder(BlockPos pos) { open(new RecorderScreen(pos)); }
    public static void openJournal() { open(new JournalScreen()); }
    public static void openKeypad(BlockPos pos) { open(new KeypadScreen(pos)); }
    public static void openConsole(BlockPos pos) { open(new ConsoleScreen(pos)); }
    public static void openDocument(String id) { open(new DocumentScreen(id, null)); }
}
