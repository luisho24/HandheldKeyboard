package com.liskovsoft.leankeyboard.fragments.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import androidx.leanback.widget.GuidedAction;
import com.liskovsoft.leankeyboard.helpers.AppInfoHelpers;
import com.liskovsoft.leankeykeyboard.R;

import java.util.List;

/** Handheld about screen with direct access to source, releases, and privacy notes. */
public class HandheldAboutFragment extends GuidedStepSupportFragment {
    private static final String[] LINKS = {
            "https://github.com/luisho24/HandheldKeyboard/blob/main/PRIVACY.md",
            "https://github.com/luisho24/HandheldKeyboard",
            "https://github.com/luisho24/HandheldKeyboard/releases"
    };

    @NonNull
    @Override
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new Guidance(getString(R.string.handheld_settings_about),
                getString(R.string.handheld_settings_about_desc), "",
                ContextCompat.getDrawable(getActivity(), R.drawable.ic_launcher));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        addLinkAction(actions, 0, R.string.handheld_privacy_policy,
                R.string.handheld_privacy_policy_desc);
        addLinkAction(actions, 1, R.string.handheld_source_code,
                R.string.handheld_source_code_desc);
        addLinkAction(actions, 2, R.string.handheld_releases,
                R.string.handheld_releases_desc);

        String appName = AppInfoHelpers.getApplicationName(getActivity());
        String appVersion = AppInfoHelpers.getAppVersionName(getActivity());
        GuidedAction version = new GuidedAction.Builder(getActivity())
                .id(3)
                .title(String.format("%s · %s", appName, appVersion))
                .description(getString(R.string.handheld_about_version_desc))
                .build();
        version.setFocusable(false);
        actions.add(version);
    }

    private void addLinkAction(List<GuidedAction> actions, long id, int titleId, int descriptionId) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .id(id)
                .title(titleId)
                .description(getString(descriptionId))
                .build();
        actions.add(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        int index = (int) action.getId();
        if (index < 0 || index >= LINKS.length) {
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LINKS[index])));
    }
}
