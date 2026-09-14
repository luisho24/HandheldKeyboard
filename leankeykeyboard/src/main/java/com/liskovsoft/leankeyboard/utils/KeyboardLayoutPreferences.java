package com.liskovsoft.leankeyboard.utils;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferences for the handheld keyboard's display-aware layout. */
public final class KeyboardLayoutPreferences {
    public static final String PROFILE_AUTO = "auto";
    public static final int HEIGHT_AUTO = 0;
    public static final int MIN_HEIGHT_PERCENT = 24;
    public static final int MAX_HEIGHT_PERCENT = 60;

    private static final String FILE_NAME = "handheld_keyboard_layout";
    private static final String KEY_PROFILE = "displayProfile";
    private static final String KEY_HEIGHT_PERCENT = "keyboardHeightPercent";
    private static final String KEY_FLOATING = "floatingKeyboard";

    private KeyboardLayoutPreferences() { }

    public static String getProfile(Context context) {
        return preferences(context).getString(KEY_PROFILE, PROFILE_AUTO);
    }

    public static void setProfile(Context context, String profile) {
        preferences(context).edit().putString(KEY_PROFILE, profile).apply();
    }

    /** Returns 0 when the height should follow the detected device profile. */
    public static int getKeyboardHeightPercent(Context context) {
        return preferences(context).getInt(KEY_HEIGHT_PERCENT, HEIGHT_AUTO);
    }

    public static void setKeyboardHeightPercent(Context context, int percent) {
        int value = percent == HEIGHT_AUTO ? HEIGHT_AUTO
                : Math.max(MIN_HEIGHT_PERCENT, Math.min(MAX_HEIGHT_PERCENT, percent));
        preferences(context).edit().putInt(KEY_HEIGHT_PERCENT, value).apply();
    }

    public static boolean isFloatingKeyboard(Context context) {
        return preferences(context).getBoolean(KEY_FLOATING, false);
    }

    public static void setFloatingKeyboard(Context context, boolean floating) {
        preferences(context).edit().putBoolean(KEY_FLOATING, floating).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
