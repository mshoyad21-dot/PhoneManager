package com.jamildev.phonemanager.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.jamildev.phonemanager.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Numeric keypad with PIN dots. Digits can be shuffled (random keypad). */
public class PinPadView extends LinearLayout {

    public interface Listener {
        void onSubmit(String pin);
    }

    public static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 8;

    private final StringBuilder pin = new StringBuilder();
    private final TextView dots;
    private final List<MaterialButton> digitButtons = new ArrayList<>();
    private final List<MaterialButton> allButtons = new ArrayList<>();
    private boolean randomized;
    private Listener listener;

    public PinPadView(Context context) {
        this(context, null);
    }

    public PinPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        dots = new TextView(context);
        dots.setTextSize(28f);
        dots.setGravity(Gravity.CENTER);
        dots.setMinHeight(dp(56));
        dots.setLetterSpacing(0.2f);
        addView(dots, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        // Rows 1-3: nine digit slots. Row 4: OK, digit, delete.
        for (int r = 0; r < 3; r++) {
            LinearLayout row = newRow();
            for (int c = 0; c < 3; c++) {
                row.addView(newDigitButton());
            }
            addView(row);
        }
        LinearLayout last = newRow();
        MaterialButton ok = newButton(getContext().getString(R.string.pad_ok));
        ok.setTextSize(16f);
        ok.setOnClickListener(v -> submit());
        last.addView(ok);
        last.addView(newDigitButton());
        MaterialButton del = newButton("⌫");
        del.setContentDescription(getContext().getString(R.string.pad_delete));
        del.setOnClickListener(v -> {
            if (pin.length() > 0) {
                pin.deleteCharAt(pin.length() - 1);
                updateDots();
            }
        });
        last.addView(del);
        addView(last);

        layoutDigits();
        updateDots();
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setRandomized(boolean random) {
        this.randomized = random;
        layoutDigits();
    }

    /** Clears the entered PIN (and reshuffles when random keypad is on). */
    public void clear() {
        pin.setLength(0);
        updateDots();
        layoutDigits();
    }

    public void setPadEnabled(boolean enabled) {
        for (MaterialButton b : allButtons) {
            b.setEnabled(enabled);
        }
    }

    private void submit() {
        if (pin.length() < MIN_LENGTH || listener == null) {
            return;
        }
        listener.onSubmit(pin.toString());
    }

    private void layoutDigits() {
        List<String> digits = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            digits.add(String.valueOf(i));
        }
        digits.add("0");
        if (randomized) {
            Collections.shuffle(digits);
        }
        for (int i = 0; i < digitButtons.size(); i++) {
            digitButtons.get(i).setText(digits.get(i));
        }
    }

    private void updateDots() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pin.length(); i++) {
            sb.append('●');
        }
        dots.setText(sb.length() == 0 ? " " : sb.toString());
    }

    private MaterialButton newDigitButton() {
        MaterialButton b = newButton("");
        b.setOnClickListener((View v) -> {
            if (pin.length() < MAX_LENGTH) {
                pin.append(((MaterialButton) v).getText());
                updateDots();
            }
        });
        digitButtons.add(b);
        return b;
    }

    private MaterialButton newButton(CharSequence text) {
        MaterialButton b = new MaterialButton(getContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(text);
        b.setTextSize(22f);
        b.setInsetTop(0);
        b.setInsetBottom(0);
        LayoutParams lp = new LayoutParams(dp(80), dp(64));
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        b.setLayoutParams(lp);
        allButtons.add(b);
        return b;
    }

    private LinearLayout newRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        // WRAP_CONTENT: a MATCH_PARENT row inside a wrap_content pad collapses to the width of the dots.
        row.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        return row;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
