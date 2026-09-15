package com.liskovsoft.leankeyboard.activity.settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import com.liskovsoft.leankeyboard.fragments.settings.KbSettingsFragment;
import com.liskovsoft.leankeyboard.receiver.RestartServiceReceiver;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;

public class KbSettingsActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ("com.handheldkeyboard.ime".equals(getPackageName()) &&
                !LeanKeyPreferences.instance(this).isHandheldOnboardingComplete()) {
            Intent onboarding = new Intent();
            onboarding.setClassName(getPackageName(),
                    "com.liskovsoft.leankeyboard.activity.settings.HandheldOnboardingActivity");
            startActivity(onboarding);
            finish();
            return;
        }

        if ("com.handheldkeyboard.ime".equals(getPackageName())) {
            // The handheld flavor has its own card-based settings home. Keep the
            // shared guided settings screen for the TV and Play Store variants.
            Intent handheldSettings = new Intent();
            handheldSettings.setClassName(getPackageName(),
                    "com.liskovsoft.leankeyboard.activity.settings.HandheldSettingsActivity");
            startActivity(handheldSettings);
            finish();
            return;
        }

        GuidedStepSupportFragment.addAsRoot(this, new KbSettingsFragment(), android.R.id.content);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // restart kbd service
        Intent intent = new Intent(this, RestartServiceReceiver.class);
        sendBroadcast(intent);
    }
}
