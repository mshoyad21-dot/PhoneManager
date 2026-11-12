package com.jamil.aiagent;

import com.jamil.aiagent.model.AgentAction;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ActionParser {
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("click", "type", "swipe", "open_app", "back", "home", "scroll_down", "wait", "done"));
    private ActionParser() {}
    public static AgentAction parse(String response) {
        try {
            if (response == null) return AgentAction.waitAction();
            String value = response.trim();
            if (value.startsWith("```")) {
                int first = value.indexOf('\n'), last = value.lastIndexOf("```");
                if (first < 0 || last <= first) return AgentAction.waitAction();
                value = value.substring(first + 1, last).trim();
            }
            JSONObject o = new JSONObject(value);
            String type = o.getString("action");
            if (!TYPES.contains(type)) return AgentAction.waitAction();
            if (type.equals("click") || type.equals("swipe")) { o.getInt("x"); o.getInt("y"); }
            if (type.equals("swipe")) { o.getInt("end_x"); o.getInt("end_y"); }
            if (type.equals("type") && !o.has("text")) return AgentAction.waitAction();
            String pkg = o.optString("package_name", "").trim();
            if (type.equals("open_app") && pkg.isEmpty()) pkg = o.optString("text", "").trim();
            if (type.equals("open_app") && !pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) return AgentAction.waitAction();
            return new AgentAction(type, o.optInt("x"), o.optInt("y"), o.optInt("end_x"), o.optInt("end_y"), o.optString("text", ""), pkg, o.optString("reasoning", "পরবর্তী ধাপ চলছে"));
        } catch (Exception ignored) { return AgentAction.waitAction(); }
    }
}
