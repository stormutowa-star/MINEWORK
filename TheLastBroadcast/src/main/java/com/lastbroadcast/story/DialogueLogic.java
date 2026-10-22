package com.lastbroadcast.story;

import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DialogueDef;
import com.lastbroadcast.network.DialogueActionPacket;
import net.minecraft.server.level.ServerPlayer;

/** Shared dialogue evaluation; the server re-checks every condition before applying actions. */
public final class DialogueLogic {
    private DialogueLogic() {}

    public static boolean test(CaseState state, DialogueDef.Cond c) {
        if (c == null) return true;
        for (String f : c.ifFlags) if (!state.has(f)) return false;
        for (String f : c.ifNot) if (state.has(f)) return false;
        for (String e : c.ifEvidence) if (!state.evidence.contains(e)) return false;
        return true;
    }

    public static String rootNode(CaseState state, DialogueDef def) {
        for (DialogueDef.Root r : def.roots) {
            if (test(state, r)) return r.node;
        }
        return def.roots.isEmpty() ? null : def.roots.get(def.roots.size() - 1).node;
    }

    public static void apply(ServerPlayer player, String character, String nodeId, int choice) {
        DialogueDef def = ContentLibrary.DIALOGUES.get(character);
        if (def == null) return;
        DialogueDef.Node node = def.nodes.get(nodeId);
        if (node == null) return;
        CaseState state = CaseData.get(player.server).state;
        if (choice == DialogueActionPacket.ENTER) {
            for (DialogueDef.Action a : node.enter) run(player, a);
            return;
        }
        if (choice < 0 || choice >= node.choices.size()) return;
        DialogueDef.Choice c = node.choices.get(choice);
        if (!test(state, c)) return;
        for (DialogueDef.Action a : c.actions) run(player, a);
    }

    private static void run(ServerPlayer player, DialogueDef.Action a) {
        if (a.set != null) StoryDirector.setFlag(player.server, a.set);
        if (a.evidence != null) StoryDirector.logEvidence(player.server, a.evidence);
        if (a.give != null) {
            if (a.give.startsWith("doc:") && StoryDirector.playerHasDocument(player, a.give.substring(4))) return;
            StoryDirector.give(player, a.give, a.count);
        }
    }
}
