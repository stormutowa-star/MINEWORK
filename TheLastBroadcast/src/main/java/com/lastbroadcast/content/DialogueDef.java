package com.lastbroadcast.content;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Branching conversation tree for one character. Conditions are story flags. */
public class DialogueDef {
    public String name = "";
    public String role = "";
    public List<Root> roots = new ArrayList<>();
    public Map<String, Node> nodes = new LinkedHashMap<>();

    public static class Cond {
        @SerializedName("if")
        public List<String> ifFlags = new ArrayList<>();
        public List<String> ifNot = new ArrayList<>();
        public List<String> ifEvidence = new ArrayList<>();
    }

    public static class Root extends Cond {
        public String node;
    }

    public static class Node {
        public List<Say> lines = new ArrayList<>();
        public List<Action> enter = new ArrayList<>();
        public List<Choice> choices = new ArrayList<>();
        /** Body animation while this node is spoken: idle | gesture | nervous | point | work */
        public String pose = "gesture";
    }

    public static class Say {
        public String who;
        public String text = "";
    }

    public static class Choice extends Cond {
        public String text = "";
        public List<Action> actions = new ArrayList<>();
        public String next;
    }

    public static class Action {
        public String set;
        public String give;
        public int count = 1;
        public String evidence;
    }
}
