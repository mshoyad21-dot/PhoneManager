package com.jamil.aiagent.model;

public final class AgentAction {
    public final String type, text, packageName, reasoning;
    public final int x, y, endX, endY;
    public AgentAction(String type, int x, int y, int endX, int endY, String text, String packageName, String reasoning) {
        this.type = type; this.x = x; this.y = y; this.endX = endX; this.endY = endY;
        this.text = text; this.packageName = packageName; this.reasoning = reasoning;
    }
    public static AgentAction waitAction() { return new AgentAction("wait", 0, 0, 0, 0, "", "", "নিরাপদ উত্তর পাওয়া যায়নি, অপেক্ষা করছি"); }
}
