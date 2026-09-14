package com.liskovsoft.leankeyboard.fragments.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import com.liskovsoft.leankeyboard.addons.keyboards.intkeyboards.CheckedSource;
import com.liskovsoft.leankeyboard.addons.keyboards.intkeyboards.KeyboardInfoAdapter;
import com.liskovsoft.leankeyboard.addons.resize.HandheldDisplayProfiles;
import com.liskovsoft.leankeyboard.utils.KeyboardLayoutPreferences;
import com.liskovsoft.leankeykeyboard.R;

/** Handheld-only layout and sizing controls, grouped on one remote-friendly screen. */
public class HandheldKeyboardSettingsFragment extends BaseSettingsFragment {
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context.getApplicationContext();

        for (CheckedSource.CheckedItem item : new KeyboardInfoAdapter(context).getItems()) {
            addCheckedAction(item.getTitle(), item::getChecked, item::onClick);
        }

        addNextAction(R.string.handheld_display_profile, this::showDisplayProfilePicker);
        addNextAction(R.string.handheld_keyboard_height, this::showKeyboardHeightPicker);
        addCheckedAction(R.string.handheld_floating_keyboard, R.string.handheld_floating_keyboard_desc,
                () -> KeyboardLayoutPreferences.isFloatingKeyboard(mContext),
                floating -> KeyboardLayoutPreferences.setFloatingKeyboard(mContext, floating));
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher);
        return new Guidance(getString(R.string.handheld_settings_keyboard),
                getString(R.string.handheld_settings_keyboard_desc), "", icon);
    }

    private void showDisplayProfilePicker() {
        String[] options = getResources().getStringArray(R.array.handheld_screen_profile_options);
        String[] profileIds = HandheldDisplayProfiles.profileIds();
        int selectedIndex = HandheldDisplayProfiles.profileIndex(KeyboardLayoutPreferences.getProfile(mContext));
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.handheld_profile_dialog_title)
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    KeyboardLayoutPreferences.setProfile(mContext, profileIds[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showKeyboardHeightPicker() {
        int initialPercent = KeyboardLayoutPreferences.getKeyboardHeightPercent(mContext);
        if (initialPercent == KeyboardLayoutPreferences.HEIGHT_AUTO) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            String profile = HandheldDisplayProfiles.resolve(KeyboardLayoutPreferences.getProfile(mContext),
                    metrics.widthPixels, metrics.heightPixels);
            initialPercent = HandheldDisplayProfiles.defaultHeightPercent(profile);
        }

        int padding = dp(20);
        LinearLayout content = new LinearLayout(getActivity());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding / 2, padding, 0);

        TextView value = new TextView(getActivity());
        value.setGravity(Gravity.CENTER);
        value.setText(getString(R.string.handheld_keyboard_height_value, initialPercent));
        content.addView(value, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        SeekBar slider = new SeekBar(getActivity());
        slider.setMax(KeyboardLayoutPreferences.MAX_HEIGHT_PERCENT - KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT);
        slider.setProgress(initialPercent - KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT);
        slider.setContentDescription(getString(R.string.handheld_keyboard_height_title));
        content.addView(slider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText(getString(R.string.handheld_keyboard_height_value,
                        progress + KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.handheld_keyboard_height_title)
                .setMessage(R.string.handheld_keyboard_height_summary)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.handheld_keyboard_height_auto,
                        (dialog, which) -> KeyboardLayoutPreferences.setKeyboardHeightPercent(
                                mContext, KeyboardLayoutPreferences.HEIGHT_AUTO))
                .setPositiveButton(R.string.custom_theme_save, (dialog, which) -> {
                    int percent = slider.getProgress() + KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT;
                    KeyboardLayoutPreferences.setKeyboardHeightPercent(mContext, percent);
                })
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
