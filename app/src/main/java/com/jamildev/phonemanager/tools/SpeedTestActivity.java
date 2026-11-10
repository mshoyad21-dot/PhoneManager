package com.jamildev.phonemanager.tools;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.jamildev.phonemanager.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Download speed test. Downloads up to ~10 seconds of test data from Cloudflare's public
 * speed-test endpoint and shows the average speed in Mbps. Uses your mobile data if you are
 * not on Wi-Fi.
 */
public class SpeedTestActivity extends AppCompatActivity {
    private static final String TEST_URL = "https://speed.cloudflare.com/__down?bytes=50000000";
    private static final long MAX_MS = 10_000L;

    private TextView value;
    private TextView status;
    private TextView ping;
    private MaterialButton btnStart;
    private volatile boolean running;
    private Thread worker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.tool_speed);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        value = findViewById(R.id.speedValue);
        status = findViewById(R.id.speedStatus);
        ping = findViewById(R.id.speedPing);
        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            if (running) {
                stopTest();
            } else {
                startTest();
            }
        });
    }

    @Override
    protected void onStop() {
        stopTest();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void stopTest() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void startTest() {
        running = true;
        btnStart.setText(R.string.speed_stop);
        status.setText(R.string.speed_running);
        ping.setText("");
        value.setText("0.0");

        worker = new Thread(() -> {
            HttpURLConnection conn = null;
            boolean failed = false;
            double lastMbps = 0;
            try {
                long t0 = SystemClock.elapsedRealtime();
                conn = (HttpURLConnection) new URL(TEST_URL).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                InputStream in = conn.getInputStream();
                final long responseMs = SystemClock.elapsedRealtime() - t0;
                runOnUiThread(() -> ping.setText(getString(R.string.speed_response, responseMs)));

                byte[] buf = new byte[64 * 1024];
                long total = 0;
                long start = SystemClock.elapsedRealtime();
                long lastUi = start;
                int n;
                while (running && (n = in.read(buf)) != -1) {
                    total += n;
                    long now = SystemClock.elapsedRealtime();
                    long elapsed = now - start;
                    if (elapsed > 0) {
                        lastMbps = total * 8.0 / (elapsed / 1000.0) / 1_000_000.0;
                    }
                    if (now - lastUi >= 250) {
                        lastUi = now;
                        final double shown = lastMbps;
                        runOnUiThread(() -> value.setText(String.format(Locale.US, "%.1f", shown)));
                    }
                    if (elapsed >= MAX_MS) {
                        break;
                    }
                }
                in.close();
            } catch (Exception e) {
                failed = running; // an interrupt from Stop is not an error
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            final boolean err = failed;
            final double finalMbps = lastMbps;
            runOnUiThread(() -> {
                running = false;
                btnStart.setText(R.string.speed_start);
                if (err) {
                    status.setText(R.string.speed_error);
                } else {
                    value.setText(String.format(Locale.US, "%.1f", finalMbps));
                    status.setText(R.string.speed_done);
                }
            });
        });
        worker.start();
    }
}
