package com.jamildev.phonemanager.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Stores the PIN as a salted PBKDF2 hash. The PIN itself is never saved. */
public final class PinStore {
    private static final String KEY_HASH = "pin_hash";
    private static final String KEY_SALT = "pin_salt";
    private static final int ITERATIONS = 20000;
    private static final int KEY_BITS = 256;

    private PinStore() {
    }

    public static boolean isSet(Context context) {
        return Prefs.sp(context).contains(KEY_HASH);
    }

    public static void setPin(Context context, String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(pin, salt);
        Prefs.sp(context).edit()
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .apply();
    }

    public static boolean verify(Context context, String pin) {
        SharedPreferences sp = Prefs.sp(context);
        String s = sp.getString(KEY_SALT, null);
        String h = sp.getString(KEY_HASH, null);
        if (s == null || h == null) {
            return false;
        }
        byte[] expected = Base64.decode(h, Base64.NO_WRAP);
        byte[] actual = hash(pin, Base64.decode(s, Base64.NO_WRAP));
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] hash(String pin, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
