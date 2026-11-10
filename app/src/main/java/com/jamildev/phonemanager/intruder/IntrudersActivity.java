package com.jamildev.phonemanager.intruder;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.lock.PinGate;
import com.jamildev.phonemanager.lock.PinProtected;

import com.jamildev.phonemanager.data.IntruderLog;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IntrudersActivity extends AppCompatActivity implements PinProtected {
    private IntrudersViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PinGate.redirectIfNoPin(this)) {
            return;
        }
        setContentView(R.layout.activity_intruders);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.intruders_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final LogAdapter adapter = new LogAdapter();
        RecyclerView list = findViewById(R.id.intruderList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        final View empty = findViewById(R.id.intruderEmpty);

        vm = new ViewModelProvider(this).get(IntrudersViewModel.class);
        vm.getLogs().observe(this, logs -> {
            adapter.submitList(logs);
            empty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.intruders_clear);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 1) {
            vm.clear();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class LogAdapter extends ListAdapter<IntruderLog, LogAdapter.VH> {
        private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();
        private static final Handler MAIN = new Handler(Looper.getMainLooper());

        private static final DiffUtil.ItemCallback<IntruderLog> DIFF =
                new DiffUtil.ItemCallback<IntruderLog>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull IntruderLog a, @NonNull IntruderLog b) {
                        return a.id == b.id;
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull IntruderLog a, @NonNull IntruderLog b) {
                        return a.id == b.id;
                    }
                };

        LogAdapter() {
            super(DIFF);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_intruder, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final IntruderLog log = getItem(position);
            h.title.setText(h.itemView.getContext().getString(
                    R.string.intruder_attempt, labelFor(h.itemView, log.packageName)));
            h.time.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(log.timestamp)));

            h.photo.setImageDrawable(null);
            h.photo.setTag(log.photoPath);
            LOADER.execute(() -> {
                final Bitmap bmp = decode(log.photoPath);
                MAIN.post(() -> {
                    if (bmp != null && log.photoPath.equals(h.photo.getTag())) {
                        h.photo.setImageBitmap(bmp);
                    }
                });
            });
        }

        private static String labelFor(View v, String pkg) {
            PackageManager pm = v.getContext().getPackageManager();
            if (pkg == null || pkg.equals(v.getContext().getPackageName())) {
                return v.getContext().getString(R.string.app_name);
            }
            try {
                return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
            } catch (PackageManager.NameNotFoundException e) {
                return pkg;
            }
        }

        private static Bitmap decode(String path) {
            if (path == null) {
                return null;
            }
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 4;
                Bitmap bmp = BitmapFactory.decodeFile(path, o);
                if (bmp == null) {
                    return null;
                }
                int orientation = new ExifInterface(path).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int degrees = orientation == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                        : orientation == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                        : orientation == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
                if (degrees == 0) {
                    return bmp;
                }
                Matrix m = new Matrix();
                m.postRotate(degrees);
                return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            } catch (Exception e) {
                return null;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView photo;
            final TextView title;
            final TextView time;

            VH(@NonNull View v) {
                super(v);
                photo = v.findViewById(R.id.photo);
                title = v.findViewById(R.id.title);
                time = v.findViewById(R.id.time);
            }
        }
    }
}
