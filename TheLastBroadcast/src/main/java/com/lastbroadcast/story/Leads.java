package com.lastbroadcast.story;

/** The case file's "current lead": a nudge in the investigator's own words, never a spoiler. */
public final class Leads {
    private Leads() {}

    public static String current(CaseState s) {
        if (s.has(Flags.CASE_SOLVED)) return "Case closed. Channel 7-B is dead carrier. File the report.";
        if (s.has(Flags.PENROSE_CONFESSED)) {
            if (s.has(Flags.D4)) return "The placard is wrong and the log says why. Shut the Skyhook down - in the right order.";
            if (s.has(Flags.FRAME_FOUND)) return "One frame of the bulletin shows the console mid-disaster. Compare it with what is posted on the wall.";
            if (s.has(Flags.ARC_FAULT_SEEN)) return "The shutdown went wrong exactly as the bulletin warned. Something in the procedure must be wrong.";
            return "Penrose gave you the console. Before you touch it, study the bulletin on tape - frame by frame if you must.";
        }
        if (s.has(Flags.D3)) return "7-B has a new programme. Record it. Then talk to Penrose - he can't hide behind the log now.";
        if (s.has(Flags.LOCKER_OPEN)) return "Penrose's transmitter log is in your hands. Set it against the dates in Rosa's complaint log.";
        if (s.has(Flags.GOT_REEL)) return "Penrose's reel sounds wrong. Try every way the deck can play it.";
        if (s.has(Flags.D2)) return "WKVR means to break its licence. Ask Rosa who runs the transmitter at night.";
        if (s.has(Flags.D1)) return "7-B is showing programmes that haven't been made yet. Keep watching - and keep recording.";
        if (s.has(Flags.SIGNAL_LOCKED)) return "Watch Channel 7-B. Record what you see and set it against the station's own records.";
        if (s.has(Flags.MET_ROSA)) return "Use the signal analyzer in Master Control to dig the ghost carrier out from under Channel 7.";
        return "Report to the night operator in Master Control (east down the corridor, first door on the right).";
    }
}
