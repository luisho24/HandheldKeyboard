package com.liskovsoft.leankeyboard.utils;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferences for the optional, synthesized keyboard sound effects. */
public final class KeyboardSoundSettings {
    public static final int EVENT_NAVIGATION = 0;
    public static final int EVENT_CONFIRM = 1;
    public static final int EVENT_DELETE = 2;
    public static final int EVENT_SHIFT = 3;
    public static final int EVENT_COUNT = 4;

    public static final int STYLE_SOFT = 0;
    public static final int STYLE_ARCADE = 1;
    public static final int STYLE_GLASS = 2;
    public static final int STYLE_MECHANICAL = 3;

    public static final int SOUND_SILENT = 0;
    public static final int SOUND_DEFAULT = -1;
    public static final int SOUND_TAP = 1;
    public static final int SOUND_DOUBLE = 2;
    public static final int SOUND_CHIME = 3;
    public static final int SOUND_BLEEP = 4;

    private static final String PREFS_NAME = "keyboard_sound_settings";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_STYLE = "style";
    private static final String[] EVENT_KEYS = {
            "navigation_sound", "confirm_sound", "delete_sound", "shift_sound"
    };

    private final SharedPreferences mPreferences;

    public KeyboardSoundSettings(Context context) {
        mPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return mPreferences.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public int getVolume() {
        return clamp(mPreferences.getInt(KEY_VOLUME, 55), 0, 100);
    }

    public void setVolume(int volume) {
        mPreferences.edit().putInt(KEY_VOLUME, clamp(volume, 0, 100)).apply();
    }

    public int getStyle() {
        int style = mPreferences.getInt(KEY_STYLE, STYLE_SOFT);
        return style >= STYLE_SOFT && style <= STYLE_MECHANICAL ? style : STYLE_SOFT;
    }

    public void setStyle(int style) {
        mPreferences.edit().putInt(KEY_STYLE, clamp(style, STYLE_SOFT, STYLE_MECHANICAL)).apply();
    }

    public int getSound(int event) {
        checkEvent(event);
        int sound = mPreferences.getInt(EVENT_KEYS[event], SOUND_DEFAULT);
        return isValidSound(sound) ? sound : SOUND_DEFAULT;
    }

    public void setSound(int event, int sound) {
        checkEvent(event);
        if (!isValidSound(sound)) {
            sound = SOUND_DEFAULT;
        }
        mPreferences.edit().putInt(EVENT_KEYS[event], sound).apply();
    }

    private static boolean isValidSound(int sound) {
        return sound == SOUND_SILENT || sound == SOUND_DEFAULT ||
                sound == SOUND_TAP || sound == SOUND_DOUBLE ||
                sound == SOUND_CHIME || sound == SOUND_BLEEP;
    }

    private static void checkEvent(int event) {
        if (event < 0 || event >= EVENT_COUNT) {
            throw new IllegalArgumentException("Unknown keyboard sound event: " + event);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
