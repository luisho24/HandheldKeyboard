package com.liskovsoft.leankeyboard.fragments.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class KbThemeFragment extends BaseSettingsFragment {
    private static final int REQUEST_BACKGROUND_IMAGE = 73;
    private static final int MAX_BACKGROUND_FILE_BYTES = 20 * 1024 * 1024;
    private static final int APPEARANCE_CHECK_SET_ID = 1001;
    private static final int VARIANT_CHECK_SET_ID = 1002;
    private static final int COLOR_SOURCE_CHECK_SET_ID = 1003;
    private static final int SURFACE_CHECK_SET_ID = 1004;
    private Context mContext;
    private LeanKeyPreferences mPrefs;
    private boolean mEditingDarkVariant;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mPrefs = LeanKeyPreferences.instance(context);
        mEditingDarkVariant = mPrefs.isCustomThemeEditingDarkVariant();
        initRadioItems();
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getActivity().getResources().getString(R.string.kb_theme);
        String desc = getActivity().getResources().getString(R.string.kb_theme_desc);
        return new Guidance(title, desc, "", ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher));
    }

    private void initRadioItems() {
        String[] themes = mContext.getResources().getStringArray(R.array.keyboard_themes);

        addRadioActionInGroup(APPEARANCE_CHECK_SET_ID, R.string.theme_appearance_system, R.string.theme_appearance_system_desc,
                () -> LeanKeyPreferences.APPEARANCE_SYSTEM.equals(mPrefs.getKeyboardAppearanceMode()),
                checked -> mPrefs.setKeyboardAppearanceMode(LeanKeyPreferences.APPEARANCE_SYSTEM));
        addRadioActionInGroup(APPEARANCE_CHECK_SET_ID, R.string.theme_appearance_light, R.string.theme_appearance_light_desc,
                () -> LeanKeyPreferences.APPEARANCE_LIGHT.equals(mPrefs.getKeyboardAppearanceMode()),
                checked -> mPrefs.setKeyboardAppearanceMode(LeanKeyPreferences.APPEARANCE_LIGHT));
        addRadioActionInGroup(APPEARANCE_CHECK_SET_ID, R.string.theme_appearance_dark, R.string.theme_appearance_dark_desc,
                () -> LeanKeyPreferences.APPEARANCE_DARK.equals(mPrefs.getKeyboardAppearanceMode()),
                checked -> mPrefs.setKeyboardAppearanceMode(LeanKeyPreferences.APPEARANCE_DARK));

        for (String theme : themes) {
            String[] split = theme.split("\\|");
            if (split.length < 2) {
                continue;
            }
            String themeName = split[0];
            String themeId = split[1];
            addRadioAction(themeName, () -> mPrefs.getCurrentTheme().equals(themeId),
                    checked -> mPrefs.setCurrentTheme(themeId));
        }

        addRadioAction(R.string.custom_theme, () -> LeanKeyPreferences.THEME_CUSTOM.equals(mPrefs.getCurrentTheme()),
                checked -> mPrefs.setCurrentTheme(LeanKeyPreferences.THEME_CUSTOM));

        addRadioActionInGroup(VARIANT_CHECK_SET_ID, R.string.custom_theme_edit_light,
                () -> !mEditingDarkVariant,
                checked -> setEditingVariant(false));
        addRadioActionInGroup(VARIANT_CHECK_SET_ID, R.string.custom_theme_edit_dark,
                () -> mEditingDarkVariant,
                checked -> setEditingVariant(true));

        addRadioActionInGroup(COLOR_SOURCE_CHECK_SET_ID, R.string.custom_theme_color_source_custom, R.string.custom_theme_color_source_custom_desc,
                () -> !mPrefs.isCustomThemeUsingSystemColors(),
                checked -> mPrefs.setCustomThemeUsingSystemColors(false));
        addRadioActionInGroup(COLOR_SOURCE_CHECK_SET_ID, R.string.custom_theme_color_source_system, R.string.custom_theme_color_source_system_desc,
                () -> mPrefs.isCustomThemeUsingSystemColors(),
                checked -> mPrefs.setCustomThemeUsingSystemColors(true));

        addColorAction(R.string.custom_theme_background_color,
                dark -> mPrefs.getCustomThemeBackground(dark),
                (dark, color) -> saveColor(mPrefs::setCustomThemeBackground, dark, color));
        addColorAction(R.string.custom_theme_candidate_color,
                dark -> mPrefs.getCustomThemeCandidate(dark),
                (dark, color) -> saveColor(mPrefs::setCustomThemeCandidate, dark, color));
        addColorAction(R.string.custom_theme_key_color,
                dark -> mPrefs.getCustomThemeKey(dark),
                (dark, color) -> saveColor(mPrefs::setCustomThemeKey, dark, color));
        addColorAction(R.string.custom_theme_text_color,
                dark -> mPrefs.getCustomThemeText(dark),
                (dark, color) -> saveColor(mPrefs::setCustomThemeText, dark, color));
        addColorAction(R.string.custom_theme_accent_color,
                dark -> mPrefs.getCustomThemeAccent(dark),
                (dark, color) -> saveColor(mPrefs::setCustomThemeAccent, dark, color));
        addNextAction(R.string.custom_theme_choose_image, this::chooseBackgroundImage);
        addNextAction(R.string.custom_theme_remove_image, this::removeBackgroundImage);

        addRadioActionInGroup(SURFACE_CHECK_SET_ID, R.string.theme_surface_solid, R.string.theme_surface_solid_desc,
                () -> LeanKeyPreferences.SURFACE_SOLID.equals(mPrefs.getThemeSurfaceMode()),
                checked -> mPrefs.setThemeSurfaceMode(LeanKeyPreferences.SURFACE_SOLID));
        addRadioActionInGroup(SURFACE_CHECK_SET_ID, R.string.theme_surface_translucent, R.string.theme_surface_translucent_desc,
                () -> LeanKeyPreferences.SURFACE_TRANSLUCENT.equals(mPrefs.getThemeSurfaceMode()),
                checked -> mPrefs.setThemeSurfaceMode(LeanKeyPreferences.SURFACE_TRANSLUCENT));
        addRadioActionInGroup(SURFACE_CHECK_SET_ID, R.string.theme_surface_glass, R.string.theme_surface_glass_desc,
                () -> LeanKeyPreferences.SURFACE_GLASS.equals(mPrefs.getThemeSurfaceMode()),
                checked -> mPrefs.setThemeSurfaceMode(LeanKeyPreferences.SURFACE_GLASS));
        addRadioActionInGroup(SURFACE_CHECK_SET_ID, R.string.theme_surface_liquid, R.string.theme_surface_liquid_desc,
                () -> LeanKeyPreferences.SURFACE_LIQUID.equals(mPrefs.getThemeSurfaceMode()),
                checked -> mPrefs.setThemeSurfaceMode(LeanKeyPreferences.SURFACE_LIQUID));
        addNextAction(R.string.theme_surface_opacity, this::showOpacityPicker);
    }

    private void setEditingVariant(boolean dark) {
        mEditingDarkVariant = dark;
        mPrefs.setCustomThemeEditingDarkVariant(dark);
    }

    private void saveColor(VariantColorSave save, boolean dark, int color) {
        save.save(dark, color);
        mPrefs.setCustomThemeUsingSystemColors(false);
        mPrefs.setCurrentTheme(LeanKeyPreferences.THEME_CUSTOM);
    }

    private void addColorAction(int titleId, VariantColorValue value, VariantColorSave save) {
        addNextAction(titleId, () -> showColorPicker(getString(titleId), value.get(mEditingDarkVariant),
                color -> save.save(mEditingDarkVariant, color)));
    }

    private void showOpacityPicker() {
        LinearLayout content = new LinearLayout(getActivity());
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(8));

        SeekBar opacity = new SeekBar(getActivity());
        opacity.setMax(100);
        opacity.setProgress(mPrefs.getThemeSurfaceOpacity());
        content.addView(opacity, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView value = new TextView(getActivity());
        value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        value.setMinWidth(dp(56));
        updateOpacityLabel(value, opacity.getProgress());
        content.addView(value, new LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT));
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateOpacityLabel(value, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.theme_surface_opacity)
                .setView(content)
                .setNegativeButton(R.string.custom_theme_cancel, null)
                .setPositiveButton(R.string.custom_theme_save, (dialog, which) -> mPrefs.setThemeSurfaceOpacity(opacity.getProgress()))
                .show();
    }

    private void updateOpacityLabel(TextView label, int progress) {
        label.setText(progress + "%");
    }

    private void showColorPicker(String title, int initialColor, ColorSave onSave) {
        LinearLayout content = new LinearLayout(getActivity());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(8), padding, dp(4));

        TextView preview = new TextView(getActivity());
        preview.setGravity(Gravity.CENTER);
        preview.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        preview.setTextSize(16);
        preview.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        previewParams.bottomMargin = dp(12);
        content.addView(preview, previewParams);

        final int[] channels = {Color.red(initialColor), Color.green(initialColor), Color.blue(initialColor)};
        final TextView[] values = new TextView[channels.length];
        final int[] selectedColor = {Color.rgb(channels[0], channels[1], channels[2])};
        updateColorPreview(preview, selectedColor[0]);

        String[] channelNames = {
                getString(R.string.custom_theme_red),
                getString(R.string.custom_theme_green),
                getString(R.string.custom_theme_blue)
        };

        for (int index = 0; index < channels.length; index++) {
            final int channelIndex = index;
            LinearLayout row = new LinearLayout(getActivity());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView label = new TextView(getActivity());
            label.setText(channelNames[index]);
            label.setTextSize(14);
            row.addView(label, new LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT));

            SeekBar slider = new SeekBar(getActivity());
            slider.setMax(255);
            slider.setProgress(channels[index]);
            slider.setContentDescription(channelNames[index]);
            row.addView(slider, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            values[index] = new TextView(getActivity());
            values[index].setText(String.valueOf(channels[index]));
            values[index].setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            row.addView(values[index], new LinearLayout.LayoutParams(dp(38), LinearLayout.LayoutParams.WRAP_CONTENT));
            content.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    channels[channelIndex] = progress;
                    values[channelIndex].setText(String.valueOf(progress));
                    selectedColor[0] = Color.rgb(channels[0], channels[1], channels[2]);
                    updateColorPreview(preview, selectedColor[0]);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(content)
                .setNegativeButton(R.string.custom_theme_cancel, null)
                .setPositiveButton(R.string.custom_theme_save, (dialog, which) -> onSave.save(selectedColor[0]))
                .show();
    }

    private void updateColorPreview(TextView preview, int color) {
        preview.setBackgroundColor(color);
        int brightness = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
        preview.setTextColor(brightness > 145 ? Color.BLACK : Color.WHITE);
        preview.setText(String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF));
    }

    private void chooseBackgroundImage() {
        Intent picker;
        if (Build.VERSION.SDK_INT >= 19) {
            picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
        } else {
            picker = new Intent(Intent.ACTION_GET_CONTENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
        }
        picker.setType("image/*");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(picker, getString(R.string.custom_theme_choose_image)), REQUEST_BACKGROUND_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_BACKGROUND_IMAGE || resultCode != android.app.Activity.RESULT_OK || data == null) {
            return;
        }

        Uri imageUri = data.getData();
        if (imageUri == null) {
            return;
        }

        copyBackgroundImage(imageUri);
    }

    private void copyBackgroundImage(Uri imageUri) {
        String variantName = mEditingDarkVariant ? "dark" : "light";
        File target = new File(mContext.getFilesDir(), "keyboard-theme-background-" + variantName + ".img");
        File temporary = new File(mContext.getFilesDir(), "keyboard-theme-background-" + variantName + ".tmp");
        int totalBytes = 0;

        try (InputStream input = mContext.getContentResolver().openInputStream(imageUri);
             OutputStream output = new FileOutputStream(temporary)) {
            if (input == null) {
                throw new IOException("Unable to open the selected image");
            }
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                totalBytes += count;
                if (totalBytes > MAX_BACKGROUND_FILE_BYTES) {
                    throw new IOException("Image is larger than 20 MB");
                }
                output.write(buffer, 0, count);
            }
            output.flush();

            BitmapFactory.Options imageInfo = new BitmapFactory.Options();
            imageInfo.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(temporary.getAbsolutePath(), imageInfo);
            if (imageInfo.outWidth <= 0 || imageInfo.outHeight <= 0) {
                throw new IOException("The selected file is not a supported image");
            }

            if (target.exists() && !target.delete()) {
                throw new IOException("Unable to replace the previous image");
            }
            if (!temporary.renameTo(target)) {
                throw new IOException("Unable to save the selected image");
            }

            mPrefs.setCustomThemeImagePath(mEditingDarkVariant, target.getAbsolutePath());
            mPrefs.setCurrentTheme(LeanKeyPreferences.THEME_CUSTOM);
            Toast.makeText(mContext, R.string.custom_theme_image_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            temporary.delete();
            Toast.makeText(mContext, R.string.custom_theme_image_error, Toast.LENGTH_LONG).show();
        }
    }

    private void removeBackgroundImage() {
        String path = mPrefs.getCustomThemeImagePath(mEditingDarkVariant);
        if (path != null && path.contains("keyboard-theme-background-")) {
            new File(path).delete();
        }
        mPrefs.setCustomThemeImagePath(mEditingDarkVariant, null);
        mPrefs.setCurrentTheme(LeanKeyPreferences.THEME_CUSTOM);
        Toast.makeText(mContext, R.string.custom_theme_image_removed, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface VariantColorValue {
        int get(boolean darkVariant);
    }

    private interface VariantColorSave {
        void save(boolean darkVariant, int color);
    }

    private interface ColorSave {
        void save(int color);
    }
}
