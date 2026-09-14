package com.liskovsoft.leankeyboard.fragments.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import com.liskovsoft.leankeyboard.utils.KeyboardSoundPlayer;
import com.liskovsoft.leankeyboard.utils.KeyboardSoundSettings;
import com.liskovsoft.leankeykeyboard.R;

public class KeyboardSoundSettingsFragment extends BaseSettingsFragment {
    private Context mContext;
    private KeyboardSoundSettings mSettings;
    private KeyboardSoundPlayer mPlayer;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context.getApplicationContext();
        mSettings = new KeyboardSoundSettings(mContext);
        mPlayer = KeyboardSoundPlayer.getInstance(mContext);
        addCheckedAction(R.string.keyboard_sound_enabled, R.string.keyboard_sound_enabled_desc,
                mSettings::isEnabled, enabled -> {
                    mSettings.setEnabled(enabled);
                    if (enabled) {
                        mPlayer.prepare();
                    }
                });

        addRadioAction(R.string.keyboard_sound_style_soft,
                () -> mSettings.getStyle() == KeyboardSoundSettings.STYLE_SOFT,
                checked -> setStyle(KeyboardSoundSettings.STYLE_SOFT));
        addRadioAction(R.string.keyboard_sound_style_arcade,
                () -> mSettings.getStyle() == KeyboardSoundSettings.STYLE_ARCADE,
                checked -> setStyle(KeyboardSoundSettings.STYLE_ARCADE));
        addRadioAction(R.string.keyboard_sound_style_glass,
                () -> mSettings.getStyle() == KeyboardSoundSettings.STYLE_GLASS,
                checked -> setStyle(KeyboardSoundSettings.STYLE_GLASS));
        addRadioAction(R.string.keyboard_sound_style_mechanical,
                () -> mSettings.getStyle() == KeyboardSoundSettings.STYLE_MECHANICAL,
                checked -> setStyle(KeyboardSoundSettings.STYLE_MECHANICAL));

        addNextAction(R.string.keyboard_sound_volume, this::showVolumePicker);
        addNextAction(R.string.keyboard_sound_navigation,
                () -> showSoundPicker(KeyboardSoundSettings.EVENT_NAVIGATION, R.string.keyboard_sound_navigation));
        addNextAction(R.string.keyboard_sound_confirm,
                () -> showSoundPicker(KeyboardSoundSettings.EVENT_CONFIRM, R.string.keyboard_sound_confirm));
        addNextAction(R.string.keyboard_sound_delete,
                () -> showSoundPicker(KeyboardSoundSettings.EVENT_DELETE, R.string.keyboard_sound_delete));
        addNextAction(R.string.keyboard_sound_shift,
                () -> showSoundPicker(KeyboardSoundSettings.EVENT_SHIFT, R.string.keyboard_sound_shift));
        addNextAction(R.string.keyboard_sound_preview,
                () -> mPlayer.previewEvent(KeyboardSoundSettings.EVENT_CONFIRM));
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new Guidance(
                getString(R.string.keyboard_sound_settings),
                getString(R.string.keyboard_sound_settings_desc),
                "",
                ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher));
    }

    private void setStyle(int style) {
        mSettings.setStyle(style);
        mPlayer.prepare();
    }

    private void showSoundPicker(int event, int titleId) {
        int selectedIndex = soundToIndex(mSettings.getSound(event));
        new AlertDialog.Builder(getActivity())
                .setTitle(titleId)
                .setSingleChoiceItems(R.array.keyboard_sound_choices, selectedIndex, (dialog, which) -> {
                    int selectedSound = indexToSound(which);
                    mSettings.setSound(event, selectedSound);
                    mPlayer.prepare();
                    mPlayer.previewEvent(event);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.custom_theme_cancel, null)
                .show();
    }

    private void showVolumePicker() {
        LinearLayout content = new LinearLayout(getActivity());
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setOrientation(LinearLayout.HORIZONTAL);
        int padding = dp(20);
        content.setPadding(padding, dp(12), padding, dp(6));

        SeekBar slider = new SeekBar(getActivity());
        slider.setMax(100);
        slider.setProgress(mSettings.getVolume());
        slider.setContentDescription(getString(R.string.keyboard_sound_volume));
        content.addView(slider, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView value = new TextView(getActivity());
        value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setMinWidth(dp(48));
        updateVolumeLabel(value, slider.getProgress());
        content.addView(value, new LinearLayout.LayoutParams(dp(52), dp(48)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateVolumeLabel(value, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.keyboard_sound_volume_dialog)
                .setView(content)
                .setNegativeButton(R.string.custom_theme_cancel, null)
                .setPositiveButton(R.string.keyboard_sound_save, (dialog, which) -> {
                    mSettings.setVolume(slider.getProgress());
                    mPlayer.previewEvent(KeyboardSoundSettings.EVENT_CONFIRM);
                })
                .show();
    }

    private void updateVolumeLabel(TextView value, int progress) {
        value.setText(progress + "%");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int soundToIndex(int sound) {
        switch (sound) {
            case KeyboardSoundSettings.SOUND_TAP:
                return 1;
            case KeyboardSoundSettings.SOUND_DOUBLE:
                return 2;
            case KeyboardSoundSettings.SOUND_CHIME:
                return 3;
            case KeyboardSoundSettings.SOUND_BLEEP:
                return 4;
            case KeyboardSoundSettings.SOUND_SILENT:
                return 5;
            case KeyboardSoundSettings.SOUND_DEFAULT:
            default:
                return 0;
        }
    }

    private static int indexToSound(int index) {
        switch (index) {
            case 1:
                return KeyboardSoundSettings.SOUND_TAP;
            case 2:
                return KeyboardSoundSettings.SOUND_DOUBLE;
            case 3:
                return KeyboardSoundSettings.SOUND_CHIME;
            case 4:
                return KeyboardSoundSettings.SOUND_BLEEP;
            case 5:
                return KeyboardSoundSettings.SOUND_SILENT;
            case 0:
            default:
                return KeyboardSoundSettings.SOUND_DEFAULT;
        }
    }
}
