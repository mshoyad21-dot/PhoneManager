package com.jamil.aiagent;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView readiness;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_main);
        readiness = findViewById(R.id.readiness);
        findViewById(R.id.overlay_permission).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        findViewById(R.id.accessibility_permission).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.notification_permission).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && !hasNotification()) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
            else refresh();
        });
        findViewById(R.id.start_overlay).setOnClickListener(v -> startIfReady());
        findViewById(R.id.stop_overlay).setOnClickListener(v -> { stopService(new Intent(this, OverlayService.class)); readiness.setText("ভাসমান বাটন বন্ধ করা হয়েছে।"); });
    }
    private boolean hasNotification() { return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED; }
    private boolean ready() { return Settings.canDrawOverlays(this) && AIAgentAccessibilityService.getInstance() != null && hasNotification(); }
    private void refresh() {
        if (ready()) readiness.setText("অ্যাপ প্রস্তুত! ভাসমান বাটনে ট্যাপ করুন।");
        else readiness.setText("চালু করতে ওভারলে, অ্যাক্সেসিবিলিটি ও নোটিফিকেশন পারমিশন দিন।");
    }
    private void startIfReady() {
        if (!ready()) { refresh(); return; }
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) { readiness.setText("API কী সেট করা নেই। GitHub Secret দিয়ে নতুন APK তৈরি করুন।"); return; }
        try { startForegroundService(new Intent(this, OverlayService.class)); refresh(); }
        catch (RuntimeException e) { readiness.setText("ভাসমান বাটন চালু করা যায়নি।"); }
    }
    @Override protected void onResume() { super.onResume(); refresh(); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) { super.onRequestPermissionsResult(requestCode, permissions, results); refresh(); }
}
