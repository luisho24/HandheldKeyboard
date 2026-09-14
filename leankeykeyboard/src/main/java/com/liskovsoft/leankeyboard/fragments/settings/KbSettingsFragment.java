package com.liskovsoft.leankeyboard.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import com.liskovsoft.leankeyboard.activity.settings.KbActivationActivity;
import com.liskovsoft.leankeykeyboard.R;

public class KbSettingsFragment extends BaseSettingsFragment {
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        addNextAction(R.string.activate_keyboard, () -> {
            Intent intent = new Intent(getActivity(), KbActivationActivity.class);
            startActivity(intent);
        });

        if ("com.handheldkeyboard.ime".equals(context.getPackageName())) {
            addNextAction(R.string.handheld_onboarding_restart, () -> {
                Intent intent = new Intent();
                intent.setClassName(context.getPackageName(),
                        "com.liskovsoft.leankeyboard.activity.settings.HandheldOnboardingActivity");
                intent.putExtra("return_to_settings", true);
                startActivity(intent);
            });
        }

        if ("com.handheldkeyboard.ime".equals(context.getPackageName())) {
            addNextAction(R.string.handheld_settings_appearance,
                    R.string.handheld_settings_appearance_desc,
                    () -> startGuidedFragment(new KbThemeFragment()));
            addNextAction(R.string.handheld_settings_keyboard,
                    R.string.handheld_settings_keyboard_desc,
                    this::startHandheldKeyboardSettingsFragment);
            addNextAction(R.string.handheld_settings_feedback,
                    R.string.handheld_settings_feedback_desc,
                    () -> startGuidedFragment(new KeyboardSoundSettingsFragment()));
            addNextAction(R.string.handheld_settings_about,
                    R.string.handheld_settings_about_desc,
                    this::startHandheldAboutFragment);
            addNextAction(R.string.handheld_settings_more,
                    R.string.handheld_settings_more_desc,
                    () -> startGuidedFragment(new MiscFragment()));
        } else {
            addNextAction(R.string.change_layout, () -> startGuidedFragment(new KbLayoutFragment()));
            addNextAction(R.string.change_theme, () -> startGuidedFragment(new KbThemeFragment()));
            addNextAction(R.string.keyboard_sound_settings,
                    () -> startGuidedFragment(new KeyboardSoundSettingsFragment()));
            addNextAction(R.string.misc, () -> startGuidedFragment(new MiscFragment()));
            addNextAction(R.string.about_desc, () -> startGuidedFragment(new AboutFragment()));
        }
    }

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getActivity().getResources().getString(R.string.ime_name);
        String desc = getActivity().getResources().getString(R.string.kb_settings_desc);
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher);

        return new Guidance(
                title,
                desc,
                "",
                icon
        );
    }

    private void startGuidedFragment(GuidedStepSupportFragment fragment) {
        if (getFragmentManager() != null) {
            GuidedStepSupportFragment.add(getFragmentManager(), fragment);
        }
    }

    private void startHandheldKeyboardSettingsFragment() {
        startHandheldFragment("com.liskovsoft.leankeyboard.fragments.settings.HandheldKeyboardSettingsFragment",
                new KbLayoutFragment());
    }

    private void startHandheldAboutFragment() {
        startHandheldFragment("com.liskovsoft.leankeyboard.fragments.settings.HandheldAboutFragment",
                new AboutFragment());
    }

    private void startHandheldFragment(String className, GuidedStepSupportFragment fallback) {
        try {
            Class<?> fragmentClass = Class.forName(className);
            GuidedStepSupportFragment fragment = (GuidedStepSupportFragment)
                    fragmentClass.getDeclaredConstructor().newInstance();
            startGuidedFragment(fragment);
        } catch (ReflectiveOperationException e) {
            // Keep the shared app variants resilient if this handheld-only screen is absent.
            startGuidedFragment(fallback);
        }
    }
}
