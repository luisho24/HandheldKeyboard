package com.liskovsoft.leankeyboard.activity.settings;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.app.GuidedStepSupportFragment;

import com.liskovsoft.leankeyboard.activity.settings.KbActivationActivity;
import com.liskovsoft.leankeyboard.fragments.settings.KeyboardSoundSettingsFragment;
import com.liskovsoft.leankeyboard.fragments.settings.HandheldPointerSettingsFragment;
import com.liskovsoft.leankeyboard.fragments.settings.MiscFragment;
import com.liskovsoft.leankeyboard.receiver.RestartServiceReceiver;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Handheld settings home. It deliberately does not use the stock GuidedStep root:
 * the larger cards make the major controls discoverable on a small touchscreen while
 * retaining predictable D-pad focus movement for handhelds and TV-style controllers.
 */
public class HandheldSettingsActivity extends FragmentActivity {
    private static final String EXTRA_OPEN_SECTION = "open_section";
    private static final int COLOR_BACKGROUND = Color.rgb(7, 18, 27);
    private static final int COLOR_PANEL = Color.rgb(17, 35, 47);
    private static final int COLOR_PANEL_FOCUSED = Color.rgb(31, 67, 73);
    private static final int COLOR_BORDER = Color.rgb(46, 75, 88);
    private static final int COLOR_ACCENT = Color.rgb(104, 235, 210);
    private static final int COLOR_TEXT = Color.rgb(241, 248, 249);
    private static final int COLOR_MUTED = Color.rgb(159, 187, 194);
    private static final int FIRST_CARD_ID = 0x70010000;
    private static final String PAYPAL_DONATE_URL = "https://paypal.me/lucabarcas";

    private final List<View> mCards = new ArrayList<>();
    private int mNextId = FIRST_CARD_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        styleSystemBars();
        setContentView(buildContent());
        String requestedSection = getIntent().getStringExtra(EXTRA_OPEN_SECTION);
        if (savedInstanceState == null && requestedSection != null && !requestedSection.isEmpty()) {
            getWindow().getDecorView().post(() -> openRequestedSection(requestedSection));
            return;
        }
        if (savedInstanceState == null && !mCards.isEmpty()) {
            mCards.get(0).post(() -> {
                View firstCard = mCards.get(0);
                firstCard.requestFocus();
                if (!firstCard.hasFocus() && Build.VERSION.SDK_INT >= 19) {
                    firstCard.requestFocusFromTouch();
                }
            });
        }
    }

    /** Supports direct entry from the keyboard quick-settings deck. */
    private void openRequestedSection(String section) {
        switch (section) {
            case "theme":
                openThemeGallery();
                return;
            case "layout":
                openHandheldKeyboardSettings();
                return;
            case "sound":
                openGuided(new KeyboardSoundSettingsFragment());
                return;
            case "pointer":
                openGuided(new HandheldPointerSettingsFragment());
                return;
            case "more":
                openGuided(new MiscFragment());
                return;
            case "about":
                openHandheldAbout();
                return;
            case "setup":
                openSetup();
                return;
            default:
                if (!mCards.isEmpty()) {
                    mCards.get(0).requestFocus();
                }
        }
    }

    private View buildContent() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BACKGROUND);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, dp(8), 0, dp(18));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(18), dp(22), 0);
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GridLayout cards = new GridLayout(this);
        cards.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        cards.setUseDefaultMargins(false);
        int columns = getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 2 : 1;
        cards.setColumnCount(columns);
        cards.setRowCount((CARD_SPECS.length + columns - 1) / columns);
        cards.setPadding(0, dp(14), 0, 0);
        page.addView(cards, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int index = 0; index < CARD_SPECS.length; index++) {
            CardSpec spec = CARD_SPECS[index];
            View card = createCard(spec);
            mCards.add(card);
            int row = index / columns;
            int column = index % columns;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(row), GridLayout.spec(column, 1f));
            params.width = 0;
            params.height = dp(116);
            params.setMargins(column == 0 ? 0 : dp(7), dp(7),
                    column == columns - 1 ? 0 : dp(7), dp(7));
            cards.addView(card, params);
        }
        wireFocusOrder(columns);

        TextView hint = text(getString(R.string.handheld_settings_navigation_hint), 12, COLOR_MUTED);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(12), 0, dp(4));
        page.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher);
        icon.setContentDescription(getString(R.string.ime_name));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        header.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(14), 0, 0, 0);
        TextView eyebrow = text(getString(R.string.handheld_settings_eyebrow), 11, COLOR_ACCENT);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= 21) {
            eyebrow.setLetterSpacing(.12f);
        }
        copy.addView(eyebrow);
        TextView title = text(getString(R.string.handheld_settings_title), 27, COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        copy.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView subtitle = text(getString(R.string.handheld_settings_subtitle), 13, COLOR_MUTED);
        copy.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        header.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = text(getString(R.string.handheld_settings_badge), 11, COLOR_ACCENT);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setPadding(dp(10), dp(6), dp(10), dp(6));
        badge.setBackground(roundDrawable(Color.TRANSPARENT, COLOR_ACCENT, dp(1), dp(18)));
        header.addView(badge, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return header;
    }

    private View createCard(final CardSpec spec) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(12), dp(16), dp(12));
        card.setFocusable(true);
        card.setClickable(true);
        card.setId(mNextId++);
        card.setContentDescription(getString(spec.titleRes));
        card.setBackground(cardBackground());
        if (Build.VERSION.SDK_INT >= 26) {
            card.setDefaultFocusHighlightEnabled(false);
        }
        card.setOnFocusChangeListener((view, focused) -> {
            float scale = focused ? 1.025f : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(130).start();
            if (Build.VERSION.SDK_INT >= 21) {
                view.animate().translationZ(focused ? dp(5) : 0).setDuration(130).start();
            }
        });
        card.setOnClickListener(v -> spec.action.open(this));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView marker = text(spec.marker, 22, COLOR_ACCENT);
        marker.setGravity(Gravity.CENTER);
        marker.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(marker, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView title = text(getString(spec.titleRes), 17, COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(dp(10), 0, 0, 0);
        top.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = text("›", 24, COLOR_MUTED);
        arrow.setGravity(Gravity.CENTER);
        top.addView(arrow, new LinearLayout.LayoutParams(dp(20), dp(34)));
        card.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView description = text(getString(spec.descriptionRes), 12, COLOR_MUTED);
        description.setPadding(dp(44), dp(4), 0, 0);
        card.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private void wireFocusOrder(int columns) {
        for (int index = 0; index < mCards.size(); index++) {
            View card = mCards.get(index);
            int row = index / columns;
            int column = index % columns;
            card.setNextFocusLeftId(mCards.get(column > 0 ? index - 1 : index).getId());
            int right = column + 1 < columns && index + 1 < mCards.size() ? index + 1 : index;
            card.setNextFocusRightId(mCards.get(right).getId());
            int up = row > 0 ? Math.min(index - columns, mCards.size() - 1) : index;
            int down = index + columns < mCards.size() ? index + columns : index;
            card.setNextFocusUpId(mCards.get(up).getId());
            card.setNextFocusDownId(mCards.get(down).getId());
        }
    }

    private StateListDrawable cardBackground() {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},
                roundDrawable(COLOR_PANEL_FOCUSED, COLOR_ACCENT, dp(2), dp(15)));
        states.addState(new int[]{android.R.attr.state_pressed},
                roundDrawable(COLOR_PANEL_FOCUSED, COLOR_ACCENT, dp(2), dp(15)));
        states.addState(new int[]{}, roundDrawable(COLOR_PANEL, COLOR_BORDER, dp(1), dp(15)));
        return states;
    }

    private GradientDrawable roundDrawable(int fill, int stroke, int strokeWidth, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(strokeWidth, stroke);
        return drawable;
    }

    private TextView text(String value, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        return view;
    }

    private void styleSystemBars() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 21) {
            window.setStatusBarColor(COLOR_BACKGROUND);
            window.setNavigationBarColor(Color.BLACK);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openGuided(GuidedStepSupportFragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        GuidedStepSupportFragment.add(manager, fragment);
        // The custom home remains underneath the guided page. Explicitly move
        // focus into the newly opened action list so a controller never keeps
        // navigating invisible home cards.
        try {
            manager.executePendingTransactions();
        } catch (IllegalStateException ignored) {
            // The normal asynchronous transaction below will still complete.
        }
        Runnable focusGuided = () -> focusFirstVisibleChild(fragment.getView());
        if (fragment.getView() != null) {
            fragment.getView().post(focusGuided);
        } else {
            getWindow().getDecorView().post(focusGuided);
        }
    }

    private void focusFirstVisibleChild(View root) {
        if (root == null) return;
        View target = findFocusableChild(root);
        if (target != null) {
            target.requestFocus();
            if (!target.hasFocus() && Build.VERSION.SDK_INT >= 19) {
                target.requestFocusFromTouch();
            }
        }
    }

    private View findFocusableChild(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                View childTarget = findFocusableChild(group.getChildAt(index));
                if (childTarget != null) return childTarget;
            }
        }
        if (root.isShown() && root.getVisibility() == View.VISIBLE &&
                root.getWidth() > 0 && root.getHeight() > 0 && root.isFocusable()) {
            return root;
        }
        return null;
    }

    private void openSetup() {
        Intent intent = new Intent();
        intent.setClassName(getPackageName(),
                "com.liskovsoft.leankeyboard.activity.settings.HandheldOnboardingActivity");
        intent.putExtra("return_to_settings", true);
        startActivity(intent);
    }

    /** Opens the touch-friendly gallery instead of Leanback's legacy theme action list. */
    private void openThemeGallery() {
        Intent intent = new Intent();
        intent.setClassName(getPackageName(),
                "com.liskovsoft.leankeyboard.activity.settings.HandheldOnboardingActivity");
        intent.putExtra("return_to_settings", true);
        intent.putExtra("theme_only", true);
        intent.putExtra("start_step", 1);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendBroadcast(new Intent(this, RestartServiceReceiver.class));
    }

    private static final CardSpec[] CARD_SPECS = new CardSpec[]{
            new CardSpec("✦", R.string.handheld_settings_appearance,
                    R.string.handheld_settings_appearance_desc,
                    HandheldSettingsActivity::openThemeGallery),
            new CardSpec("⌨", R.string.handheld_settings_keyboard,
                    R.string.handheld_settings_keyboard_desc,
                    activity -> activity.openHandheldKeyboardSettings()),
            new CardSpec("♪", R.string.handheld_settings_feedback,
                    R.string.handheld_settings_feedback_desc,
                    activity -> activity.openGuided(new KeyboardSoundSettingsFragment())),
            new CardSpec("◉", R.string.handheld_settings_pointer,
                    R.string.handheld_settings_pointer_desc,
                    activity -> activity.openGuided(new HandheldPointerSettingsFragment())),
            new CardSpec("ⓘ", R.string.handheld_settings_about,
                    R.string.handheld_settings_about_desc,
                    activity -> activity.openHandheldAbout()),
            new CardSpec("⋯", R.string.handheld_settings_more,
                    R.string.handheld_settings_more_desc,
                    activity -> activity.openGuided(new MiscFragment())),
            new CardSpec("✓", R.string.activate_keyboard,
                    R.string.handheld_settings_activate_desc,
                    activity -> activity.startActivity(new Intent(activity, KbActivationActivity.class))),
            new CardSpec("↻", R.string.handheld_onboarding_restart,
                    R.string.handheld_settings_setup_desc,
                    HandheldSettingsActivity::openSetup),
            new CardSpec("♥", R.string.handheld_settings_support,
                    R.string.handheld_settings_support_desc,
                    HandheldSettingsActivity::openDonation)
    };

    /** Opens the voluntary project-support page outside the keyboard settings flow. */
    private void openDonation() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_DONATE_URL));
        startActivity(intent);
    }

    private void openHandheldKeyboardSettings() {
        openHandheldFragment("com.liskovsoft.leankeyboard.fragments.settings.HandheldKeyboardSettingsFragment",
                new com.liskovsoft.leankeyboard.fragments.settings.KbLayoutFragment());
    }

    private void openHandheldAbout() {
        openHandheldFragment("com.liskovsoft.leankeyboard.fragments.settings.HandheldAboutFragment",
                new com.liskovsoft.leankeyboard.fragments.settings.AboutFragment());
    }

    private void openHandheldFragment(String className, GuidedStepSupportFragment fallback) {
        try {
            Class<?> fragmentClass = Class.forName(className);
            openGuided((GuidedStepSupportFragment) fragmentClass.getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException | ClassCastException e) {
            openGuided(fallback);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            manager.popBackStackImmediate();
            if (!mCards.isEmpty()) {
                mCards.get(0).post(() -> mCards.get(0).requestFocus());
            }
            return;
        }
        super.onBackPressed();
    }

    private interface CardAction {
        void open(HandheldSettingsActivity activity);
    }

    private static final class CardSpec {
        final String marker;
        final int titleRes;
        final int descriptionRes;
        final CardAction action;

        CardSpec(String marker, int titleRes, int descriptionRes, CardAction action) {
            this.marker = marker;
            this.titleRes = titleRes;
            this.descriptionRes = descriptionRes;
            this.action = action;
        }

    }
}
