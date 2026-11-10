package com.jamildev.phonemanager.manager;

import android.app.Activity;
import android.text.format.Formatter;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.jamildev.phonemanager.R;

/** Shared "Optimize" flow: progress dialog, run, then a result dialog. */
public final class OptimizeUi {
    private OptimizeUi() {
    }

    public static void optimize(Activity activity, Runnable onFinished) {
        CircularProgressIndicator spinner = new CircularProgressIndicator(activity);
        spinner.setIndeterminate(true);
        FrameLayout box = new FrameLayout(activity);
        int pad = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);
        box.addView(spinner, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        final AlertDialog progress = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.optimizing)
                .setView(box)
                .setCancelable(false)
                .show();

        Optimizer.run(activity, result -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            progress.dismiss();
            String msg = activity.getString(R.string.optimize_result,
                    Formatter.formatShortFileSize(activity, result.memoryFreed),
                    Formatter.formatShortFileSize(activity, result.cacheFreed),
                    result.appsStopped);
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.optimize_done)
                    .setMessage(msg)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }
}
