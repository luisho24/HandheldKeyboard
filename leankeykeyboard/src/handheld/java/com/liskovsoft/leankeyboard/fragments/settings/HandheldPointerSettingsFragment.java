package com.liskovsoft.leankeyboard.fragments.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.GuidanceStylist.Guidance;

import com.liskovsoft.leankeyboard.utils.HandheldPointerPreferences;
import com.liskovsoft.leankeykeyboard.R;

/** Setup for the optional controller pointer. Kept separate from keyboard layout controls. */
public class HandheldPointerSettingsFragment extends BaseSettingsFragment {
    private static final int[] BUTTON_KEYS = new int[]{
            android.view.KeyEvent.KEYCODE_BUTTON_A, android.view.KeyEvent.KEYCODE_BUTTON_B,
            android.view.KeyEvent.KEYCODE_BUTTON_X, android.view.KeyEvent.KEYCODE_BUTTON_Y,
            android.view.KeyEvent.KEYCODE_BUTTON_L1, android.view.KeyEvent.KEYCODE_BUTTON_R1,
            android.view.KeyEvent.KEYCODE_BUTTON_L2, android.view.KeyEvent.KEYCODE_BUTTON_R2,
            android.view.KeyEvent.KEYCODE_BUTTON_1, android.view.KeyEvent.KEYCODE_BUTTON_2,
            android.view.KeyEvent.KEYCODE_BUTTON_3, android.view.KeyEvent.KEYCODE_BUTTON_4
    };
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context.getApplicationContext();
        addNextAction(R.string.handheld_pointer_enable_android,
                R.string.handheld_pointer_enable_android_desc, this::openAccessibilitySettings);
        addCheckedAction(R.string.handheld_pointer_controls,
                R.string.handheld_pointer_controls_desc,
                () -> HandheldPointerPreferences.isControlsEnabled(mContext),
                enabled -> HandheldPointerPreferences.setControlsEnabled(mContext, enabled));
        addNextAction(R.string.handheld_pointer_chord,
                R.string.handheld_pointer_chord_desc, this::showChordPicker);
        addNextAction(R.string.handheld_pointer_click,
                R.string.handheld_pointer_click_desc,
                () -> showButtonPicker(R.string.handheld_pointer_click,
                        HandheldPointerPreferences.getClickButton(mContext),
                        key -> HandheldPointerPreferences.setClickButton(mContext, key)));
        addNextAction(R.string.handheld_pointer_back,
                R.string.handheld_pointer_back_desc,
                () -> showButtonPicker(R.string.handheld_pointer_back,
                        HandheldPointerPreferences.getBackButton(mContext),
                        key -> HandheldPointerPreferences.setBackButton(mContext, key)));
        addNextAction(R.string.handheld_pointer_scroll_up,
                R.string.handheld_pointer_scroll_up_desc,
                () -> showButtonPicker(R.string.handheld_pointer_scroll_up,
                        HandheldPointerPreferences.getScrollUpButton(mContext),
                        key -> HandheldPointerPreferences.setScrollUpButton(mContext, key)));
        addNextAction(R.string.handheld_pointer_scroll_down,
                R.string.handheld_pointer_scroll_down_desc,
                () -> showButtonPicker(R.string.handheld_pointer_scroll_down,
                        HandheldPointerPreferences.getScrollDownButton(mContext),
                        key -> HandheldPointerPreferences.setScrollDownButton(mContext, key)));
        addNextAction(R.string.handheld_pointer_theme,
                R.string.handheld_pointer_theme_desc, this::showThemePicker);
        addNextAction(R.string.handheld_pointer_speed,
                R.string.handheld_pointer_speed_desc, this::showSpeedPicker);
        addCheckedAction(R.string.handheld_pointer_animations,
                R.string.handheld_pointer_animations_desc,
                () -> HandheldPointerPreferences.areAnimationsEnabled(mContext),
                enabled -> HandheldPointerPreferences.setAnimationsEnabled(mContext, enabled));
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher);
        return new Guidance(getString(R.string.handheld_settings_pointer),
                getString(R.string.handheld_settings_pointer_desc), "", icon);
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {
            // The action is available on supported Android versions; retain settings if OEM removes it.
        }
    }

    private void showChordPicker() {
        showButtonPicker(R.string.handheld_pointer_chord_first,
                HandheldPointerPreferences.getChordFirst(mContext), first ->
                        showButtonPicker(R.string.handheld_pointer_chord_second,
                                HandheldPointerPreferences.getChordSecond(mContext), second -> {
                                    if (first != second) HandheldPointerPreferences.setChord(mContext, first, second);
                                }));
    }

    private void showButtonPicker(int titleRes, int current, OnKeyPicked onPicked) {
        String[] labels = getResources().getStringArray(R.array.handheld_pointer_button_options);
        int selected = indexForKey(current);
        new AlertDialog.Builder(getActivity())
                .setTitle(titleRes)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    onPicked.onKey(BUTTON_KEYS[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showThemePicker() {
        String[] values = new String[]{HandheldPointerPreferences.THEME_KEYBOARD,
                HandheldPointerPreferences.THEME_MINT, HandheldPointerPreferences.THEME_VIOLET,
                HandheldPointerPreferences.THEME_AMBER};
        int current = 0;
        String existing = HandheldPointerPreferences.getTheme(mContext);
        for (int i = 0; i < values.length; i++) if (values[i].equals(existing)) current = i;
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.handheld_pointer_theme)
                .setSingleChoiceItems(R.array.handheld_pointer_theme_options, current, (dialog, which) -> {
                    HandheldPointerPreferences.setTheme(mContext, values[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSpeedPicker() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.handheld_pointer_speed)
                .setSingleChoiceItems(R.array.handheld_pointer_speed_options,
                        HandheldPointerPreferences.getSpeed(mContext), (dialog, which) -> {
                            HandheldPointerPreferences.setSpeed(mContext, which);
                            dialog.dismiss();
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private int indexForKey(int key) {
        for (int i = 0; i < BUTTON_KEYS.length; i++) if (BUTTON_KEYS[i] == key) return i;
        return 0;
    }

    private interface OnKeyPicked { void onKey(int keyCode); }
}
