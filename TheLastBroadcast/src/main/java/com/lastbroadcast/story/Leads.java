package com.lastbroadcast.story;

/** The case file's "current lead": a nudge in the investigator's own words, never a spoiler. */
public final class Leads {
    private Leads() {}

    public static String current(CaseState s) {
        if (s.has(Flags.CASE_SOLVED)) return "lastbroadcast.lead.closed";
        if (s.has(Flags.PENROSE_CONFESSED)) {
            if (s.has(Flags.D4)) return "lastbroadcast.lead.placard";
            if (s.has(Flags.FRAME_FOUND)) return "lastbroadcast.lead.frame";
            if (s.has(Flags.ARC_FAULT_SEEN)) return "lastbroadcast.lead.arc";
            return "lastbroadcast.lead.console";
        }
        if (s.has(Flags.D3)) return "lastbroadcast.lead.d3";
        if (s.has(Flags.LOCKER_OPEN)) return "lastbroadcast.lead.locker";
        if (s.has(Flags.GOT_REEL)) return "lastbroadcast.lead.reel";
        if (s.has(Flags.D2)) return "lastbroadcast.lead.d2";
        if (s.has(Flags.D1)) return "lastbroadcast.lead.d1";
        if (s.has(Flags.SIGNAL_LOCKED)) return "lastbroadcast.lead.signal";
        if (s.has(Flags.MET_ROSA)) return "lastbroadcast.lead.met_rosa";
        return "lastbroadcast.lead.start";
    }
}
