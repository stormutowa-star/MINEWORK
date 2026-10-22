package com.lastbroadcast.client.screen;

import com.lastbroadcast.client.ClientDialogueState;
import com.lastbroadcast.client.sound.BroadcastAudio;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DialogueDef;
import com.lastbroadcast.network.DialogueActionPacket;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.story.CaseState;
import com.lastbroadcast.story.DialogueLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversation panel along the bottom of the screen, leaving the character visible in the world
 * (where they turn to face you, gesture and move their lips while speaking).
 */
public class DialogueScreen extends RetroScreen {
    private final int entityId;
    private final String character;
    private final DialogueDef def;
    private String nodeId;
    private DialogueDef.Node node;
    private int line = 0;
    private float typed = 0;
    private long lastNanos = System.nanoTime();
    private double nextBlip = 0;
    private final List<Integer> visibleChoices = new ArrayList<>();

    public DialogueScreen(int entityId, String character) {
        super(Component.literal("Conversation"), 380, 140);
        this.entityId = entityId;
        this.character = character;
        this.def = ContentLibrary.DIALOGUES.get(character);
        ClientDialogueState.entityId = entityId;
        if (def != null) enter(DialogueLogic.rootNode(CaseState.CLIENT, def));
    }

    @Override
    protected void init() {
        super.init();
        top = Math.round(height / scale - panelH - 8);
    }

    private void enter(String id) {
        nodeId = id;
        node = id == null || def == null ? null : def.nodes.get(id);
        line = 0;
        typed = 0;
        if (node == null) {
            onClose();
            return;
        }
        ClientDialogueState.pose = node.pose;
        Net.toServer(new DialogueActionPacket(entityId, id, DialogueActionPacket.ENTER));
    }

    private DialogueDef.Say currentLine() {
        return node != null && line < node.lines.size() ? node.lines.get(line) : null;
    }

    private boolean typing() {
        DialogueDef.Say s = currentLine();
        return s != null && typed < s.text.length();
    }

    private boolean showingChoices() {
        return node != null && line >= node.lines.size() - 1 && !typing();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(entityId);
        if (e == null || mc.player == null || e.distanceToSqr(mc.player) > 12 * 12) onClose();
    }

    @Override
    protected void layout() {
    }

    private void advance() {
        if (node == null) return;
        DialogueDef.Say s = currentLine();
        if (s != null && typed < s.text.length()) {
            typed = s.text.length();
            return;
        }
        if (line < node.lines.size() - 1) {
            line++;
            typed = 0;
        } else if (node.choices.isEmpty()) {
            onClose();
        }
    }

    private void choose(int index) {
        DialogueDef.Choice c = node.choices.get(index);
        Net.toServer(new DialogueActionPacket(entityId, nodeId, index));
        BroadcastAudio.playUi("paper", 0.4f, 1.3f);
        if (c.next == null) onClose();
        else enter(c.next);
    }

    @Override
    protected boolean panelClicked(int mx, int my, int button) {
        if (showingChoices() && !node.choices.isEmpty()) {
            for (int i = 0; i < visibleChoices.size(); i++) {
                int y = 50 + i * 12;
                if (my >= y && my < y + 12 && mx >= 100 && mx < panelW - 8) {
                    choose(visibleChoices.get(i));
                    return true;
                }
            }
            return true;
        }
        advance();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (!showingChoices() || node.choices.isEmpty()) advance();
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9 && showingChoices()) {
            int i = key - GLFW.GLFW_KEY_1;
            if (i < visibleChoices.size()) choose(visibleChoices.get(i));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        super.removed();
        Net.toServer(new DialogueActionPacket(entityId, nodeId == null ? "" : nodeId, DialogueActionPacket.CLOSE));
        ClientDialogueState.clear();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, height - 150, width, height, 0x00000000, 0xC0000000);
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        if (def == null || node == null) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        DialogueDef.Say s = currentLine();
        if (s != null && typed < s.text.length()) {
            typed = Math.min(s.text.length(), typed + dt * 48f);
            if (now / 1e9 >= nextBlip && s.who != null && !"player".equals(s.who)) {
                Entity e = Minecraft.getInstance().level.getEntity(entityId);
                ContentLibrary.Speaker sp = ContentLibrary.speaker(s.who);
                if (e != null) BroadcastAudio.playAt(sp.voice, e.getEyePosition(), 0.6f, sp.pitch * (0.9f + (float) Math.random() * 0.2f));
                nextBlip = now / 1e9 + 0.09 + Math.random() * 0.07;
            }
        }
        ClientDialogueState.speaking = s != null && typed < s.text.length() && character.equals(s.who);

        cabinet(g, 0, 0, panelW, panelH);
        // name plate
        plate(g, 8, 8, 86, 30);
        boolean playerLine = s != null && "player".equals(s.who);
        String name = playerLine ? "You" : def.name;
        labelCentered(g, font().plainSubstrByWidth(name, 82), 51, 12, INK);
        smallLabel(g, playerLine ? "FCC Field Engineer" : def.role, 51, 26, 0xFF5A4E3A);
        // text
        recess(g, 100, 8, panelW - 108, 124, 0xFFF4ECD4);
        if (s != null) {
            String shown = s.text.substring(0, (int) typed);
            int color = playerLine ? 0xFF2A4A6A : INK;
            wrapped(g, shown, 106, 13, panelW - 120, color, showingChoices() && !node.choices.isEmpty() ? 3 : 8);
        }
        visibleChoices.clear();
        if (showingChoices()) {
            if (node.choices.isEmpty()) {
                smallLabel(g, "[click to leave]", panelW - 40, 124, 0xFF8A7A5A);
            } else {
                for (int i = 0; i < node.choices.size(); i++) {
                    if (DialogueLogic.test(CaseState.CLIENT, node.choices.get(i))) visibleChoices.add(i);
                }
                g.fill(104, 47, panelW - 12, 48, 0x40403020);
                for (int i = 0; i < visibleChoices.size() && i < 6; i++) {
                    int y = 50 + i * 12;
                    boolean hover = my >= y && my < y + 12 && mx >= 100 && mx < panelW - 8;
                    if (hover) g.fill(104, y - 1, panelW - 12, y + 10, 0x30A06020);
                    String text = (i + 1) + ". " + node.choices.get(visibleChoices.get(i)).text;
                    label(g, font().plainSubstrByWidth(text, panelW - 120), 108, y, hover ? RED : 0xFF3A2A1A);
                }
            }
        } else if (s != null && !typing()) {
            smallLabel(g, "[click / space]", panelW - 36, 124, 0xFF8A7A5A);
        }
    }
}
