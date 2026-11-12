package com.jamil.aiagent;

import org.junit.Test;
import static org.junit.Assert.*;

public class ActionParserTest {
    @Test public void parsesFencedJson() {
        assertEquals("click", ActionParser.parse("```json\n{\"action\":\"click\",\"x\":3,\"y\":4}\n```").type);
        assertEquals(4, ActionParser.parse("{\"action\":\"click\",\"x\":3,\"y\":4}").y);
    }
    @Test public void malformedOrUnknownIsWait() {
        assertEquals("wait", ActionParser.parse(null).type);
        assertEquals("wait", ActionParser.parse("not json").type);
        assertEquals("wait", ActionParser.parse("{\"action\":\"launch_missiles\"}").type);
        assertEquals("wait", ActionParser.parse("{\"action\":\"click\"}").type);
        assertEquals("wait", ActionParser.parse("{\"action\":\"open_app\",\"package_name\":\"not a package\"}").type);
    }
    @Test public void parsesOpenAppAndType() {
        assertEquals("com.example.app", ActionParser.parse("{\"action\":\"open_app\",\"package_name\":\"com.example.app\"}").packageName);
        assertEquals("হ্যালো", ActionParser.parse("{\"action\":\"type\",\"text\":\"হ্যালো\"}").text);
    }
}
