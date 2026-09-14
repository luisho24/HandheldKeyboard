package com.liskovsoft.leankeyboard.fragments.settings;

import android.content.Context;
import android.app.AlertDialog;
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
import com.liskovsoft.leankeyboard.addons.resize.HandheldDisplayProfiles;
import com.liskovsoft.leankeyboard.activity.settings.KbSettingsActivity2;
import com.liskovsoft.leankeyboard.helpers.Helpers;
import com.liskovsoft.leankeyboard.utils.KeyboardLayoutPreferences;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

public class MiscFragment extends BaseSettingsFragment {
    private LeanKeyPreferences mPrefs;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
        mPrefs = LeanKeyPreferences.instance(getActivity());
        addCheckedAction(R.string.keep_on_screen, R.string.keep_on_screen_desc, mPrefs::getForceShowKeyboard, mPrefs::setForceShowKeyboard);
        if (!"com.handheldkeyboard.ime".equals(context.getPackageName())) {
            addCheckedAction(R.string.increase_kbd_size, R.string.increase_kbd_size_desc, mPrefs::getEnlargeKeyboard, mPrefs::setEnlargeKeyboard);
        }
        addCheckedAction(R.string.enable_suggestions, R.string.enable_suggestions_desc, mPrefs::getSuggestionsEnabled, mPrefs::setSuggestionsEnabled);
        addCheckedAction(R.string.show_launcher_icon, R.string.show_launcher_icon_desc, this::getLauncherIconShown, this::setLauncherIconShown);
        addCheckedAction(R.string.enable_cyclic_navigation, R.string.enable_cyclic_navigation_desc, mPrefs::isCyclicNavigationEnabled, mPrefs::setCyclicNavigationEnabled);
        if ("com.handheldkeyboard.ime".equals(context.getPackageName())) {
            addNextAction(R.string.handheld_display_profile, this::showDisplayProfilePicker);
            addNextAction(R.string.handheld_keyboard_height, this::showKeyboardHeightPicker);
            addCheckedAction(R.string.handheld_floating_keyboard, R.string.handheld_floating_keyboard_desc,
                    () -> KeyboardLayoutPreferences.isFloatingKeyboard(mContext),
                    floating -> KeyboardLayoutPreferences.setFloatingKeyboard(mContext, floating));
        }
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getActivity().getResources().getString(R.string.misc);
        String desc = getActivity().getResources().getString(R.string.misc_desc);
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher);

        return new Guidance(
                title,
                desc,
                "",
                icon
        );
    }

    private void setLauncherIconShown(boolean shown) {
        Helpers.setLauncherIconShown(mContext, KbSettingsActivity2.class, shown);
    }

    private boolean getLauncherIconShown() {
        return Helpers.getLauncherIconShown(mContext, KbSettingsActivity2.class);
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

        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(getActivity());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding / 2, padding, 0);

        TextView value = new TextView(getActivity());
        value.setGravity(Gravity.CENTER);
        value.setText(getString(R.string.handheld_keyboard_height_value, initialPercent));
        content.addView(value, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        SeekBar heightSlider = new SeekBar(getActivity());
        heightSlider.setMax(KeyboardLayoutPreferences.MAX_HEIGHT_PERCENT - KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT);
        heightSlider.setProgress(initialPercent - KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT);
        heightSlider.setContentDescription(getString(R.string.handheld_keyboard_height_title));
        content.addView(heightSlider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        heightSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int percent = progress + KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT;
                value.setText(getString(R.string.handheld_keyboard_height_value, percent));
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
                .setNeutralButton(R.string.handheld_keyboard_height_auto, (dialog, which) -> {
                    KeyboardLayoutPreferences.setKeyboardHeightPercent(mContext, KeyboardLayoutPreferences.HEIGHT_AUTO);
                })
                .setPositiveButton(R.string.custom_theme_save, (dialog, which) -> {
                    int percent = heightSlider.getProgress() + KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT;
                    KeyboardLayoutPreferences.setKeyboardHeightPercent(mContext, percent);
                })
                .show();
    }
}
