package com.lastbroadcast.content;

import java.util.ArrayList;
import java.util.List;

/** A contradiction the player can establish by comparing two pieces of evidence. */
public class DeductionDef {
    public String id;
    public String title = "";
    public List<String> a = new ArrayList<>();
    public List<String> b = new ArrayList<>();
    public String text = "";
    public boolean optional = false;
    public List<String> requires = new ArrayList<>();

    public boolean matches(String x, String y) {
        return (a.contains(x) && b.contains(y)) || (a.contains(y) && b.contains(x));
    }
}
