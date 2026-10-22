package com.lastbroadcast.client;

import com.lastbroadcast.Tx;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DeductionDef;
import net.minecraft.client.resources.language.I18n;

/** Client-side resolution of {@link Tx} text into the player's language. */
public final class ClientText {
    private ClientText() {}

    public static String resolve(String s) {
        if (s == null || s.isEmpty()) return "";
        char c = s.charAt(0);
        if (c == Tx.REF && s.length() > 2) {
            String id = s.substring(2);
            if (s.charAt(1) == 'D') {
                DeductionDef d = ContentLibrary.DEDUCTIONS.get(id);
                return d != null ? d.title : id;
            }
            return ContentLibrary.evidenceTitle(id);
        }
        if (c == Tx.KEY) {
            String[] p = s.substring(1).split(String.valueOf(Tx.SEP), -1);
            Object[] args = new Object[p.length - 1];
            for (int i = 0; i < args.length; i++) args[i] = resolve(p[i + 1]);
            return I18n.get(p[0], args);
        }
        return s;
    }
}
