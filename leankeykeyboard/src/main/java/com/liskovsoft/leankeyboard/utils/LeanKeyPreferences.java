package com.liskovsoft.leankeyboard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class LeanKeyPreferences {
    private static final String APP_RUN_ONCE = "appRunOnce";
    private static final String BOOTSTRAP_SELECTED_LANGUAGE = "bootstrapSelectedLanguage";
    private static final String APP_KEYBOARD_INDEX = "appKeyboardIndex";
    private static final String FORCE_SHOW_KEYBOARD = "forceShowKeyboard";
    private static final String ENLARGE_KEYBOARD = "enlargeKeyboard";
    private static final String KEYBOARD_THEME = "keyboardTheme";
    private static final String CUSTOM_THEME_BACKGROUND = "customThemeBackground";
    private static final String CUSTOM_THEME_CANDIDATE = "customThemeCandidate";
    private static final String CUSTOM_THEME_KEY = "customThemeKey";
    private static final String CUSTOM_THEME_TEXT = "customThemeText";
    private static final String CUSTOM_THEME_ACCENT = "customThemeAccent";
    private static final String CUSTOM_THEME_IMAGE = "customThemeImage";
    private static final String CUSTOM_THEME_LIGHT_PREFIX = "customThemeLight";
    private static final String CUSTOM_THEME_DARK_PREFIX = "customThemeDark";
    private static final String CUSTOM_THEME_EDIT_DARK = "customThemeEditDark";
    private static final String CUSTOM_THEME_SYSTEM_COLORS = "customThemeSystemColors";
    private static final String KEYBOARD_APPEARANCE = "keyboardAppearance";
    private static final String THEME_SURFACE_MODE = "themeSurfaceMode";
    private static final String THEME_SURFACE_OPACITY = "themeSurfaceOpacity";
    public static final String APPEARANCE_SYSTEM = "system";
    public static final String APPEARANCE_LIGHT = "light";
    public static final String APPEARANCE_DARK = "dark";
    public static final String SURFACE_SOLID = "solid";
    public static final String SURFACE_TRANSLUCENT = "translucent";
    public static final String SURFACE_GLASS = "glass";
    public static final String SURFACE_LIQUID = "liquid";
    public static final String THEME_DEFAULT = "Default";
    public static final String THEME_DARK = "Dark";
    public static final String THEME_DARK2 = "Dark2";
    public static final String THEME_DARK3 = "Dark3";
    public static final String THEME_CUSTOM = "Custom";
    private static final String SUGGESTIONS_ENABLED = "suggestionsEnabled";
    private static final String CYCLIC_NAVIGATION_ENABLED = "cyclicNavigationEnabled";
    private static final String AUTODETECT_LAYOUT = "autodetectLayout";
    private static LeanKeyPreferences sInstance;
    private final Context mContext;
    private SharedPreferences mPrefs;

    public static LeanKeyPreferences instance(Context ctx) {
        if (sInstance == null)
            sInstance = new LeanKeyPreferences(ctx);
        return sInstance;
    }

    public LeanKeyPreferences(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public boolean isRunOnce() {
        return mPrefs.getBoolean(APP_RUN_ONCE, false);
    }

    public boolean isHandheldOnboardingComplete() {
        return mPrefs.getBoolean("handheldOnboardingComplete", false);
    }

    public void setHandheldOnboardingComplete(boolean complete) {
        mPrefs.edit().putBoolean("handheldOnboardingComplete", complete).apply();
    }

    public void setRunOnce(boolean runOnce) {
        mPrefs.edit()
                .putBoolean(APP_RUN_ONCE, runOnce)
                .apply();
    }

    public void setPreferredLanguage(String name) {
        mPrefs.edit()
                .putString(BOOTSTRAP_SELECTED_LANGUAGE, name)
                .apply();
    }

    public String getPreferredLanguage() {
        return mPrefs.getString(BOOTSTRAP_SELECTED_LANGUAGE, "");
    }

    public int getKeyboardIndex() {
        return mPrefs.getInt(APP_KEYBOARD_INDEX, 0);
    }

    public void setKeyboardIndex(int idx) {
        mPrefs.edit()
                .putInt(APP_KEYBOARD_INDEX, idx)
                .apply();
    }

    public boolean getForceShowKeyboard() {
        return mPrefs.getBoolean(FORCE_SHOW_KEYBOARD, true);
    }

    public void setForceShowKeyboard(boolean force) {
        mPrefs.edit()
                .putBoolean(FORCE_SHOW_KEYBOARD, force)
                .apply();
    }

    public boolean getEnlargeKeyboard() {
        return mPrefs.getBoolean(ENLARGE_KEYBOARD, false);
    }

    public void setEnlargeKeyboard(boolean enlarge) {
        mPrefs.edit()
                .putBoolean(ENLARGE_KEYBOARD, enlarge)
                .apply();
    }

    public void setCurrentTheme(String theme) {
        mPrefs.edit()
                .putString(KEYBOARD_THEME, theme)
                .apply();
    }

    public String getCurrentTheme() {
        return mPrefs.getString(KEYBOARD_THEME, THEME_DARK3);
    }

    public void setKeyboardAppearanceMode(String mode) {
        mPrefs.edit().putString(KEYBOARD_APPEARANCE, normalizeAppearance(mode)).apply();
    }

    public String getKeyboardAppearanceMode() {
        return normalizeAppearance(mPrefs.getString(KEYBOARD_APPEARANCE, APPEARANCE_SYSTEM));
    }

    /** Resolves the saved appearance choice against the current Android night-mode setting. */
    public boolean isDarkAppearance() {
        String mode = getKeyboardAppearanceMode();
        if (APPEARANCE_DARK.equals(mode)) {
            return true;
        }
        if (APPEARANCE_LIGHT.equals(mode)) {
            return false;
        }
        int nightMode = mContext.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public void setCustomThemeEditingDarkVariant(boolean dark) {
        mPrefs.edit().putBoolean(CUSTOM_THEME_EDIT_DARK, dark).apply();
    }

    public boolean isCustomThemeEditingDarkVariant() {
        return mPrefs.getBoolean(CUSTOM_THEME_EDIT_DARK, isDarkAppearance());
    }

    public void setCustomThemeUsingSystemColors(boolean enabled) {
        mPrefs.edit().putBoolean(CUSTOM_THEME_SYSTEM_COLORS, enabled).apply();
    }

    public boolean isCustomThemeUsingSystemColors() {
        return mPrefs.getBoolean(CUSTOM_THEME_SYSTEM_COLORS, false);
    }

    public void setThemeSurfaceMode(String mode) {
        mPrefs.edit().putString(THEME_SURFACE_MODE, normalizeSurfaceMode(mode)).apply();
    }

    public String getThemeSurfaceMode() {
        return normalizeSurfaceMode(mPrefs.getString(THEME_SURFACE_MODE, SURFACE_SOLID));
    }

    /** Percentage from 0 to 100. A conservative default keeps key legends readable. */
    public int getThemeSurfaceOpacity() {
        return Math.max(0, Math.min(100, mPrefs.getInt(THEME_SURFACE_OPACITY, 88)));
    }

    public void setThemeSurfaceOpacity(int opacity) {
        mPrefs.edit().putInt(THEME_SURFACE_OPACITY, Math.max(0, Math.min(100, opacity))).apply();
    }

    private static String normalizeAppearance(String mode) {
        if (APPEARANCE_LIGHT.equals(mode) || APPEARANCE_DARK.equals(mode)) {
            return mode;
        }
        return APPEARANCE_SYSTEM;
    }

    private static String normalizeSurfaceMode(String mode) {
        if (SURFACE_TRANSLUCENT.equals(mode) || SURFACE_GLASS.equals(mode) || SURFACE_LIQUID.equals(mode)) {
            return mode;
        }
        return SURFACE_SOLID;
    }

    public int getCustomThemeBackground() {
        return getCustomThemeBackground(isDarkAppearance());
    }

    public void setCustomThemeBackground(int color) {
        setCustomThemeBackground(isCustomThemeEditingDarkVariant(), color);
    }

    public int getCustomThemeBackground(boolean darkVariant) {
        return getCustomThemeColor(darkVariant, "Background", CUSTOM_THEME_BACKGROUND,
                darkVariant ? 0xFF14202B : 0xFFE8EDF3);
    }

    public void setCustomThemeBackground(boolean darkVariant, int color) {
        setCustomThemeColor(darkVariant, "Background", color);
    }

    public int getCustomThemeCandidate() {
        return getCustomThemeCandidate(isDarkAppearance());
    }

    public void setCustomThemeCandidate(int color) {
        setCustomThemeCandidate(isCustomThemeEditingDarkVariant(), color);
    }

    public int getCustomThemeCandidate(boolean darkVariant) {
        return getCustomThemeColor(darkVariant, "Candidate", CUSTOM_THEME_CANDIDATE,
                darkVariant ? 0xFF1C2C3A : 0xFFF4F6F9);
    }

    public void setCustomThemeCandidate(boolean darkVariant, int color) {
        setCustomThemeColor(darkVariant, "Candidate", color);
    }

    public int getCustomThemeKey() {
        return getCustomThemeKey(isDarkAppearance());
    }

    public void setCustomThemeKey(int color) {
        setCustomThemeKey(isCustomThemeEditingDarkVariant(), color);
    }

    public int getCustomThemeKey(boolean darkVariant) {
        return getCustomThemeColor(darkVariant, "Key", CUSTOM_THEME_KEY,
                darkVariant ? 0xFF344656 : 0xFFFFFFFF);
    }

    public void setCustomThemeKey(boolean darkVariant, int color) {
        setCustomThemeColor(darkVariant, "Key", color);
    }

    public int getCustomThemeText() {
        return getCustomThemeText(isDarkAppearance());
    }

    public void setCustomThemeText(int color) {
        setCustomThemeText(isCustomThemeEditingDarkVariant(), color);
    }

    public int getCustomThemeText(boolean darkVariant) {
        return getCustomThemeColor(darkVariant, "Text", CUSTOM_THEME_TEXT,
                darkVariant ? 0xFFF4F7FA : 0xFF1D2732);
    }

    public void setCustomThemeText(boolean darkVariant, int color) {
        setCustomThemeColor(darkVariant, "Text", color);
    }

    public int getCustomThemeAccent() {
        return getCustomThemeAccent(isDarkAppearance());
    }

    public void setCustomThemeAccent(int color) {
        setCustomThemeAccent(isCustomThemeEditingDarkVariant(), color);
    }

    public int getCustomThemeAccent(boolean darkVariant) {
        return getCustomThemeColor(darkVariant, "Accent", CUSTOM_THEME_ACCENT,
                darkVariant ? 0xFF55D6C2 : 0xFF246BFD);
    }

    public void setCustomThemeAccent(boolean darkVariant, int color) {
        setCustomThemeColor(darkVariant, "Accent", color);
    }

    private int getCustomThemeColor(boolean darkVariant, String role, String legacyKey, int fallback) {
        String variantKey = (darkVariant ? CUSTOM_THEME_DARK_PREFIX : CUSTOM_THEME_LIGHT_PREFIX) + role;
        int variantDefault = darkVariant ? mPrefs.getInt(legacyKey, fallback) : fallback;
        return mPrefs.getInt(variantKey, variantDefault);
    }

    private void setCustomThemeColor(boolean darkVariant, String role, int color) {
        String variantKey = (darkVariant ? CUSTOM_THEME_DARK_PREFIX : CUSTOM_THEME_LIGHT_PREFIX) + role;
        mPrefs.edit().putInt(variantKey, color).apply();
    }

    public String getCustomThemeImagePath() {
        return mPrefs.getString(CUSTOM_THEME_IMAGE, null);
    }

    public void setCustomThemeImagePath(String path) {
        mPrefs.edit().putString(CUSTOM_THEME_IMAGE, path).apply();
    }

    /** Variant paths override the legacy shared path; an empty value means explicitly no image. */
    public String getCustomThemeImagePath(boolean darkVariant) {
        String variantKey = darkVariant ? CUSTOM_THEME_DARK_PREFIX + "Image" : CUSTOM_THEME_LIGHT_PREFIX + "Image";
        if (mPrefs.contains(variantKey)) {
            String path = mPrefs.getString(variantKey, "");
            return path.length() == 0 ? null : path;
        }
        return getCustomThemeImagePath();
    }

    public void setCustomThemeImagePath(boolean darkVariant, String path) {
        String variantKey = darkVariant ? CUSTOM_THEME_DARK_PREFIX + "Image" : CUSTOM_THEME_LIGHT_PREFIX + "Image";
        mPrefs.edit().putString(variantKey, path == null ? "" : path).apply();
    }

    public void setSuggestionsEnabled(boolean enabled) {
        mPrefs.edit()
                .putBoolean(SUGGESTIONS_ENABLED, enabled)
                .apply();
    }

    public boolean getSuggestionsEnabled() {
        return mPrefs.getBoolean(SUGGESTIONS_ENABLED, true);
    }

    public void setCyclicNavigationEnabled(boolean enabled) {
        mPrefs.edit()
                .putBoolean(CYCLIC_NAVIGATION_ENABLED, enabled)
                .apply();
    }

    public boolean isCyclicNavigationEnabled() {
        return mPrefs.getBoolean(CYCLIC_NAVIGATION_ENABLED, false);
    }

    public boolean getAutodetectLayout() {
        return mPrefs.getBoolean(AUTODETECT_LAYOUT, false);
    }
}
