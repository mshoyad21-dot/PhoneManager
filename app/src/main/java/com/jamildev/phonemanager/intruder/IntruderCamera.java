package com.jamildev.phonemanager.intruder;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.jamildev.phonemanager.data.Repo;
import com.jamildev.phonemanager.security.PermissionHelper;

import java.io.File;

/** Takes a silent front-camera photo after a wrong PIN. Photos stay in app-private storage. */
public final class IntruderCamera {
    private static final long WARMUP_MS = 700L;

    private IntruderCamera() {
    }

    public static void capture(Context context, LifecycleOwner owner, String targetPkg) {
        final Context app = context.getApplicationContext();
        if (!PermissionHelper.hasCamera(app)) {
            return;
        }
        final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(app);
        future.addListener(() -> {
            try {
                final ProcessCameraProvider provider = future.get();
                if (!provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    return;
                }
                final ImageCapture imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                provider.unbindAll();
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture);

                File dir = new File(app.getFilesDir(), "intruders");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                final File out = new File(dir, System.currentTimeMillis() + ".jpg");

                // Give auto-exposure a moment so the photo is not black.
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        imageCapture.takePicture(
                                new ImageCapture.OutputFileOptions.Builder(out).build(),
                                ContextCompat.getMainExecutor(app),
                                new ImageCapture.OnImageSavedCallback() {
                                    @Override
                                    public void onImageSaved(ImageCapture.OutputFileResults r) {
                                        provider.unbind(imageCapture);
                                        new Repo(app).addIntruder(targetPkg, out.getAbsolutePath());
                                    }

                                    @Override
                                    public void onError(ImageCaptureException e) {
                                        provider.unbind(imageCapture);
                                    }
                                }), WARMUP_MS);
            } catch (Exception ignored) {
                // Camera busy / unavailable: skip silently.
            }
        }, ContextCompat.getMainExecutor(app));
    }
}
