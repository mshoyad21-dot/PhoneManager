package com.jamil.aiagent;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.jamil.aiagent.model.AgentAction;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AIAgentAccessibilityService extends AccessibilityService {
    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
        "com.google.android.apps.nbu.paisa.user", "com.phonepe.app", "net.one97.paytm",
        "com.dreamplug.androidapp", "com.bd.mobileBanking", "com.sslwireless.android"));
    private static volatile AIAgentAccessibilityService instance;
    private volatile String lastError = "স্ক্রিন পাওয়া যায়নি";
    private volatile String lastPackage = "";
    public static AIAgentAccessibilityService getInstance() { return instance; }
    public static boolean isBlocked(String pkg) { return pkg != null && BLOCKED.contains(pkg); }
    public String getLastError() { return lastError; }
    @Override protected void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName() != null && isBlocked(event.getPackageName().toString())) {
            OverlayService.stopForSafety("ব্যাংকিং অ্যাপ শনাক্ত হয়েছে; কাজ বন্ধ করা হয়েছে।");
        }
    }
    @Override public void onInterrupt() { OverlayService.stopForSafety("অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ হয়েছে।"); }
    @Override public void onDestroy() { instance = null; OverlayService.stopForSafety("অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ হয়েছে।"); super.onDestroy(); }

    // Scan the entire accessible tree for passwords, even after the 50-node output cap.
    // If traversal cannot finish safely, fail closed instead of sending partial data.
    public String captureScreenStructure() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { lastError = "স্ক্রিন পড়া যাচ্ছে না; কাজ বন্ধ হয়েছে।"; return null; }
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        try {
            CharSequence packageName = root.getPackageName();
            String pkg = packageName == null ? "" : packageName.toString();
            if (pkg.isEmpty() || isBlocked(pkg)) { lastError = "নিরাপত্তার জন্য এই স্ক্রিনে কাজ বন্ধ হয়েছে।"; return null; }
            JSONArray nodes = new JSONArray();
            int visited = 0;
            while (!queue.isEmpty()) {
                AccessibilityNodeInfo node = queue.removeFirst();
                try {
                    if (++visited > 5000 || queue.size() > 5000) { lastError = "স্ক্রিন সম্পূর্ণ যাচাই করা যায়নি; কাজ বন্ধ হয়েছে।"; return null; }
                    if (node.isPassword()) { lastError = "পাসওয়ার্ড ফিল্ড পাওয়া গেছে; কাজ বন্ধ হয়েছে।"; return null; }
                    if (nodes.length() < 50) {
                        Rect r = new Rect(); node.getBoundsInScreen(r);
                        JSONObject n = new JSONObject();
                        n.put("text", string(node.getText())); n.put("contentDescription", string(node.getContentDescription()));
                        n.put("className", string(node.getClassName())); n.put("left", r.left); n.put("top", r.top);
                        n.put("right", r.right); n.put("bottom", r.bottom); n.put("x", r.centerX()); n.put("y", r.centerY());
                        n.put("clickable", node.isClickable()); n.put("editable", node.isEditable()); n.put("scrollable", node.isScrollable());
                        nodes.put(n);
                    }
                    for (int i = 0; i < node.getChildCount(); i++) {
                        AccessibilityNodeInfo child = node.getChild(i);
                        if (child != null) queue.addLast(child);
                    }
                } finally { node.recycle(); }
            }
            JSONObject output = new JSONObject(); output.put("package", pkg); output.put("nodes", nodes);
            lastPackage = pkg; return output.toString();
        } catch (Exception e) { lastError = "স্ক্রিন যাচাই করা যায়নি; কাজ বন্ধ হয়েছে।"; return null; }
        finally { while (!queue.isEmpty()) queue.removeFirst().recycle(); }
    }
    private static String string(CharSequence value) { return value == null ? "" : value.toString(); }

    // Recheck the whole tree and foreground package immediately before every action.
    public boolean executeAction(AgentAction action, String expectedScreen) throws InterruptedException {
        String current = captureScreenStructure();
        if (action == null || current == null) return false;
        if (!current.equals(expectedScreen)) {
            lastError = "স্ক্রিন পরিবর্তিত হয়েছে; কাজ বন্ধ হয়েছে।";
            return false;
        }
        if (action.type.equals("open_app")) {
            if (isBlocked(action.packageName)) { lastError = "নিষিদ্ধ ব্যাংকিং অ্যাপ খোলা যাবে না।"; return false; }
            Intent intent = getPackageManager().getLaunchIntentForPackage(action.packageName);
            if (intent == null) { lastError = "অ্যাপটি খুঁজে পাওয়া যায়নি।"; return false; }
            try { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); return true; }
            catch (RuntimeException e) { lastError = "অ্যাপ খোলা যায়নি।"; return false; }
        }
        switch (action.type) {
            case "click": return gesture(action.x, action.y, action.x, action.y, 80);
            case "swipe": return gesture(action.x, action.y, action.endX, action.endY, 400);
            case "back": return performGlobalAction(GLOBAL_ACTION_BACK);
            case "home": return performGlobalAction(GLOBAL_ACTION_HOME);
            case "type": return nodeAction(true, action.text);
            case "scroll_down": return nodeAction(false, "");
            case "wait": Thread.sleep(1500); return true;
            case "done": return true;
            default: return false;
        }
    }
    private boolean gesture(int x, int y, int endX, int endY, long duration) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        if (x < 0 || y < 0 || endX < 0 || endY < 0 || x >= dm.widthPixels || endX >= dm.widthPixels || y >= dm.heightPixels || endY >= dm.heightPixels) { lastError = "স্ক্রিনের বাইরের অবস্থান বাতিল করা হয়েছে।"; return false; }
        Path path = new Path(); path.moveTo(x, y); if (x != endX || y != endY) path.lineTo(endX, endY);
        return dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path, 0, duration)).build(), null, null);
    }
    private boolean nodeAction(boolean typing, String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow(); if (root == null) return false;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>(); queue.add(root);
        try {
            while (!queue.isEmpty()) {
                AccessibilityNodeInfo n = queue.removeFirst();
                try {
                    boolean match = typing ? n.isEditable() && n.isFocused() && !n.isPassword() : n.isScrollable();
                    if (match) {
                        Bundle args = new Bundle(); if (typing) args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                        if (n.performAction(typing ? AccessibilityNodeInfo.ACTION_SET_TEXT : AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, args)) return true;
                    }
                    for (int i = 0; i < n.getChildCount(); i++) { AccessibilityNodeInfo child = n.getChild(i); if (child != null) queue.add(child); }
                } finally { n.recycle(); }
            }
            lastError = typing ? "নির্বাচিত লেখার ঘর পাওয়া যায়নি।" : "স্ক্রল করা যায়নি।"; return false;
        } finally { while (!queue.isEmpty()) queue.removeFirst().recycle(); }
    }
}
