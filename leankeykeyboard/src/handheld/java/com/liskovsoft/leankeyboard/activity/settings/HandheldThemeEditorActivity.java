package com.liskovsoft.leankeyboard.activity.settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import com.liskovsoft.leankeyboard.fragments.settings.KbThemeFragment;
import com.liskovsoft.leankeyboard.receiver.RestartServiceReceiver;

/** Opens the existing full custom-theme editor directly from the onboarding gallery. */
public class HandheldThemeEditorActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, new KbThemeFragment(), android.R.id.content);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendBroadcast(new Intent(this, RestartServiceReceiver.class));
    }
}
