package com.jamil.aiagent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.jamil.aiagent.model.AgentAction;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OverlayService extends Service {
    private static volatile OverlayService instance;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private WindowManager windows;
    private WindowManager.LayoutParams params;
    private View overlay, panel;
    private EditText goal;
    private TextView status;
    private volatile long generation;
    private volatile Future<?> task;
    private volatile GeminiApiClient client;
    private static final String CHANNEL = "agent_status";
    @Override public void onCreate() {
        super.onCreate(); instance = this;
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "এজেন্টের অবস্থা", NotificationManager.IMPORTANCE_LOW));
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_info_details).setContentTitle("AI Agent চালু আছে").setContentText("ভাসমান বাটন থেকে নিয়ন্ত্রণ করুন").setContentIntent(pi).setOngoing(true).build();
        startForeground(101, notification);
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null);
        panel = overlay.findViewById(R.id.panel); goal = overlay.findViewById(R.id.goal); status = overlay.findViewById(R.id.status);
        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START; params.x = 20; params.y = 120;
        try { windows.addView(overlay, params); } catch (RuntimeException e) { stopSelf(); return; }
        overlay.findViewById(R.id.start).setOnClickListener(v -> startTask());
        overlay.findViewById(R.id.cancel).setOnClickListener(v -> cancel("কাজ বন্ধ করা হয়েছে।"));
        overlay.findViewById(R.id.bubble).setOnTouchListener(new View.OnTouchListener() {
            float downX, downY; int initialX, initialY; boolean dragged;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: downX = event.getRawX(); downY = event.getRawY(); initialX = params.x; initialY = params.y; dragged = false; return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(event.getRawX() - downX), dy = (int)(event.getRawY() - downY);
                        if (Math.abs(dx) + Math.abs(dy) > 10) dragged = true;
                        if (dragged) { params.x = Math.max(0, Math.min(getResources().getDisplayMetrics().widthPixels - v.getWidth(), initialX + dx)); params.y = Math.max(0, Math.min(getResources().getDisplayMetrics().heightPixels - v.getHeight(), initialY + dy)); windows.updateViewLayout(overlay, params); }
                        return true;
                    case MotionEvent.ACTION_UP: if (!dragged) { v.performClick(); togglePanel(); } return true;
                    default: return true;
                }
            }
        });
    }
    private void togglePanel() {
        boolean show = panel.getVisibility() != View.VISIBLE;
        panel.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            int panelWidth = (int)(300 * getResources().getDisplayMetrics().density);
            params.x = Math.max(0, Math.min(params.x, getResources().getDisplayMetrics().widthPixels - panelWidth));
            int panelHeight = (int)(400 * getResources().getDisplayMetrics().density);
            params.y = Math.max(0, Math.min(params.y, getResources().getDisplayMetrics().heightPixels - panelHeight));
        } else params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        windows.updateViewLayout(overlay, params);
    }
    private void startTask() {
        String command = goal.getText().toString().trim();
        if (command.isEmpty()) { status.setText("প্রথমে নির্দেশ লিখুন।"); return; }
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) { status.setText("API কী সেট করা নেই।"); return; }
        if (AIAgentAccessibilityService.getInstance() == null) { status.setText("অ্যাক্সেসিবিলিটি চালু করুন।"); return; }
        cancel(null); long run = ++generation; client = new GeminiApiClient(); GeminiApiClient thisClient = client;
        togglePanel();
        task = worker.submit(() -> runAgent(run, command, thisClient));
    }
    private void runAgent(long run, String command, GeminiApiClient api) {
        try {
            for (int step = 1; step <= 15 && active(run); step++) {
                update(run, "স্টেপ " + step + "/১৫: বিশ্লেষণ করছি...");
                AIAgentAccessibilityService service = AIAgentAccessibilityService.getInstance();
                if (service == null) { update(run, "অ্যাক্সেসিবিলিটি বন্ধ হয়েছে।"); return; }
                String screen = service.captureScreenStructure();
                if (screen == null) { update(run, "⚠️ " + service.getLastError()); return; }
                if (!active(run)) return;
                String pkg = new JSONObject(screen).getString("package");
                String response = api.sendSync(screen, command, step);
                if (!active(run)) return;
                AgentAction action = ActionParser.parse(response);
                update(run, "স্টেপ " + step + "/১৫: " + action.reasoning);
                if (action.type.equals("done")) {
                    String current = service.captureScreenStructure();
                    if (!active(run)) return;
                    if (current == null || !pkg.equals(new JSONObject(current).optString("package"))) { update(run, "⚠️ স্ক্রিন পরিবর্তিত হয়েছে বা নিরাপদ নয়; কাজ বন্ধ হয়েছে।"); return; }
                    update(run, "✅ কাজ সম্পন্ন হয়েছে!"); return;
                }
                if (!active(run) || !service.executeAction(action, screen)) { update(run, "⚠️ " + service.getLastError()); return; }
                Thread.sleep(1200);
            }
            if (active(run)) update(run, "সর্বোচ্চ ১৫ ধাপ শেষ হয়েছে; কাজ বন্ধ করা হয়েছে।");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (Exception e) { update(run, "Gemini বা স্ক্রিনে সমস্যা হয়েছে; কাজ বন্ধ। কী, নেটওয়ার্ক ও মডেল যাচাই করুন।"); }
        finally { api.close(); }
    }
    private boolean active(long run) { return run == generation && !Thread.currentThread().isInterrupted(); }
    private void update(long run, String message) { main.post(() -> { if (run == generation && status != null) status.setText(message); }); }
    private void cancel(String message) {
        generation++;
        GeminiApiClient c = client; if (c != null) c.cancel();
        Future<?> f = task; if (f != null) f.cancel(true);
        if (message != null && status != null) status.setText(message);
    }
    public static void stopForSafety(String message) {
        OverlayService service = instance;
        if (service != null) {
            service.generation++;
            GeminiApiClient c = service.client; if (c != null) c.cancel();
            Future<?> f = service.task; if (f != null) f.cancel(true);
            service.main.post(() -> { if (service.status != null) service.status.setText("⚠️ " + message); if (service.panel != null && service.panel.getVisibility() != View.VISIBLE) service.togglePanel(); });
        }
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_NOT_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        instance = null; cancel(null); worker.shutdownNow();
        if (overlay != null && overlay.isAttachedToWindow()) windows.removeView(overlay);
        super.onDestroy();
    }
}
