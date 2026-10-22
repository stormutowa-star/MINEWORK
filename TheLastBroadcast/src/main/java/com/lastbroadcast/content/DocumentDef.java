package com.lastbroadcast.content;

import java.util.ArrayList;
import java.util.List;

/** A physical paper evidence item (letter, log, notice). */
public class DocumentDef {
    public String id;
    public String title = "";
    /** letter | log | notice | clipping | memo */
    public String kind = "letter";
    public String header = "";
    public List<String> pages = new ArrayList<>();
    /** One-paragraph summary written into the case journal. */
    public String note = "";
    public String stamp;
}
