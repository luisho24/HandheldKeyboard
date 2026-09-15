package com.liskovsoft.leankeyboard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

/** Persistent, handheld-specific controls for the optional accessibility pointer. */
public final class HandheldPointerPreferences {
    public static final String THEME_KEYBOARD = "keyboard";
    public static final String THEME_MINT = "mint";
    public static final String THEME_VIOLET = "violet";
    public static final String THEME_AMBER = "amber";

    private static final String FILE_NAME = "handheld_pointer";
    private static final String KEY_CONTROLS_ENABLED = "controlsEnabled";
    private static final String KEY_POINTER_ACTIVE = "pointerActive";
    private static final String KEY_CHORD_FIRST = "chordFirst";
    private static final String KEY_CHORD_SECOND = "chordSecond";
    private static final String KEY_CLICK = "clickButton";
    private static final String KEY_BACK = "backButton";
    private static final String KEY_SCROLL_UP = "scrollUpButton";
    private static final String KEY_SCROLL_DOWN = "scrollDownButton";
    private static final String KEY_THEME = "theme";
    private static final String KEY_ANIMATIONS = "animations";
    private static final String KEY_SPEED = "speed";

    private HandheldPointerPreferences() { }

    public static boolean isControlsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_CONTROLS_ENABLED, true);
    }

    public static void setControlsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_CONTROLS_ENABLED, enabled).apply();
        if (!enabled) setPointerActive(context, false);
    }

    public static boolean isPointerActive(Context context) {
        return prefs(context).getBoolean(KEY_POINTER_ACTIVE, false);
    }

    public static void setPointerActive(Context context, boolean active) {
        prefs(context).edit().putBoolean(KEY_POINTER_ACTIVE, active).apply();
    }

    public static int getChordFirst(Context context) {
        return prefs(context).getInt(KEY_CHORD_FIRST, KeyEvent.KEYCODE_BUTTON_1);
    }

    public static int getChordSecond(Context context) {
        return prefs(context).getInt(KEY_CHORD_SECOND, KeyEvent.KEYCODE_BUTTON_4);
    }

    public static void setChord(Context context, int first, int second) {
        if (first == second) return;
        prefs(context).edit().putInt(KEY_CHORD_FIRST, first).putInt(KEY_CHORD_SECOND, second).apply();
    }

    public static int getClickButton(Context context) {
        return prefs(context).getInt(KEY_CLICK, KeyEvent.KEYCODE_BUTTON_A);
    }

    public static void setClickButton(Context context, int keyCode) {
        prefs(context).edit().putInt(KEY_CLICK, keyCode).apply();
    }

    public static int getBackButton(Context context) {
        return prefs(context).getInt(KEY_BACK, KeyEvent.KEYCODE_BUTTON_B);
    }

    public static void setBackButton(Context context, int keyCode) {
        prefs(context).edit().putInt(KEY_BACK, keyCode).apply();
    }

    public static int getScrollUpButton(Context context) {
        return prefs(context).getInt(KEY_SCROLL_UP, KeyEvent.KEYCODE_BUTTON_L1);
    }

    public static void setScrollUpButton(Context context, int keyCode) {
        prefs(context).edit().putInt(KEY_SCROLL_UP, keyCode).apply();
    }

    public static int getScrollDownButton(Context context) {
        return prefs(context).getInt(KEY_SCROLL_DOWN, KeyEvent.KEYCODE_BUTTON_R1);
    }

    public static void setScrollDownButton(Context context, int keyCode) {
        prefs(context).edit().putInt(KEY_SCROLL_DOWN, keyCode).apply();
    }

    public static String getTheme(Context context) {
        return prefs(context).getString(KEY_THEME, THEME_KEYBOARD);
    }

    public static void setTheme(Context context, String theme) {
        prefs(context).edit().putString(KEY_THEME, theme).apply();
    }

    public static boolean areAnimationsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ANIMATIONS, true);
    }

    public static void setAnimationsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ANIMATIONS, enabled).apply();
    }

    /** 0, 1 and 2 map to careful, balanced and fast pointer movement. */
    public static int getSpeed(Context context) {
        return prefs(context).getInt(KEY_SPEED, 1);
    }

    public static void setSpeed(Context context, int speed) {
        prefs(context).edit().putInt(KEY_SPEED, Math.max(0, Math.min(2, speed))).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
