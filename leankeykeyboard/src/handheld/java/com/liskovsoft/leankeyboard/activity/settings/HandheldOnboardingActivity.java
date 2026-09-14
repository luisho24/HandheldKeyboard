package com.liskovsoft.leankeyboard.activity.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOverlay;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.liskovsoft.leankeyboard.addons.resize.HandheldDisplayProfiles;
import com.liskovsoft.leankeyboard.ime.LeanbackImeService;
import com.liskovsoft.leankeyboard.utils.KeyboardLayoutPreferences;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Touch-first, four-step first-run setup for the handheld product flavor. */
public class HandheldOnboardingActivity extends Activity {
    private static final int STEP_COUNT = 4;
    private static final int REQUEST_IME_SETTINGS = 71;
    private static final int COLOR_BACKGROUND = Color.rgb(8, 20, 30);
    private static final int COLOR_PANEL = Color.rgb(18, 34, 47);
    private static final int COLOR_PANEL_RAISED = Color.rgb(25, 45, 60);
    private static final int COLOR_TEXT = Color.rgb(240, 247, 250);
    private static final int COLOR_MUTED = Color.rgb(156, 180, 191);
    private static final int COLOR_ACCENT = Color.rgb(104, 235, 210);
    private static final int COLOR_ACCENT_DARK = Color.rgb(20, 91, 91);

    private final List<ThemeCard> mThemeCards = new ArrayList<>();
    private LeanKeyPreferences mPrefs;
    private FrameLayout mRoot;
    private LinearLayout mPageContent;
    private TextView mStepCounter;
    private TextView mActivationStatus;
    private View[] mProgressSegments;
    private Button mBackButton;
    private Button mNextButton;
    private Button mSkipButton;
    private int mStep;
    private boolean mOpeningThemeEditor;
    private boolean mReturnToSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = LeanKeyPreferences.instance(this);
        mReturnToSettings = getIntent().getBooleanExtra("return_to_settings", false);
        mStep = savedInstanceState == null ? 0 : savedInstanceState.getInt("onboardingStep", 0);
        mStep = Math.max(0, Math.min(STEP_COUNT - 1, mStep));
        styleSystemBars();
        buildShell();
        renderStep(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshActivationStatus();
        if (mOpeningThemeEditor && mStep == 1) {
            mOpeningThemeEditor = false;
            renderStep(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("onboardingStep", mStep);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (mStep > 0) {
            mStep--;
            renderStep(true);
        } else {
            super.onBackPressed();
        }
    }

    private void styleSystemBars() {
        Window window = getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        if (Build.VERSION.SDK_INT >= 21) {
            window.setStatusBarColor(COLOR_BACKGROUND);
            window.setNavigationBarColor(COLOR_BACKGROUND);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
        }
    }

    private void buildShell() {
        mRoot = new FrameLayout(this);
        mRoot.setBackgroundColor(COLOR_BACKGROUND);
        mRoot.addView(new AmbientGlowView(this), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(22), dp(14), dp(22), dp(12));
        mRoot.addView(shell, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = text(R.string.onboarding_brand, 12, COLOR_ACCENT, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= 21) brand.setLetterSpacing(0.13f);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(26), 1f));
        mStepCounter = text("", 12, COLOR_MUTED, Typeface.BOLD);
        header.addView(mStepCounter, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(26)));
        shell.addView(header);

        LinearLayout progress = new LinearLayout(this);
        progress.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(3));
        progressParams.topMargin = dp(11);
        progressParams.bottomMargin = dp(9);
        shell.addView(progress, progressParams);
        mProgressSegments = new View[STEP_COUNT];
        for (int i = 0; i < STEP_COUNT; i++) {
            View segment = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(3), 1f);
            if (i > 0) params.leftMargin = dp(5);
            progress.addView(segment, params);
            mProgressSegments[i] = segment;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        shell.addView(scroll, scrollParams);
        FrameLayout pageFrame = new FrameLayout(this);
        pageFrame.setClipChildren(false);
        pageFrame.setClipToPadding(false);
        scroll.addView(pageFrame, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        mPageContent = new LinearLayout(this);
        mPageContent.setOrientation(LinearLayout.VERTICAL);
        mPageContent.setClipChildren(false);
        mPageContent.setClipToPadding(false);
        mPageContent.setPadding(0, dp(13), 0, dp(24));
        pageFrame.addView(mPageContent, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        shell.addView(footer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        mBackButton = new Button(this);
        mBackButton.setText(R.string.onboarding_back);
        styleButton(mBackButton, false);
        footer.addView(mBackButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        mBackButton.setOnClickListener(v -> {
            if (mStep > 0) {
                mStep--;
                renderStep(true);
            }
        });

        mSkipButton = new Button(this);
        mSkipButton.setText(R.string.onboarding_skip);
        mSkipButton.setAllCaps(false);
        mSkipButton.setTextColor(COLOR_MUTED);
        mSkipButton.setBackgroundColor(Color.TRANSPARENT);
        applyCustomFocus(mSkipButton, dp(15));
        footer.addView(mSkipButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)));
        mSkipButton.setOnClickListener(v -> finishSetup());

        mNextButton = new Button(this);
        styleButton(mNextButton, true);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(0, dp(48), 1.25f);
        nextParams.leftMargin = dp(6);
        footer.addView(mNextButton, nextParams);
        mNextButton.setOnClickListener(v -> {
            if (mStep < STEP_COUNT - 1) {
                mStep++;
                renderStep(true);
            } else {
                finishSetup();
            }
        });

        setContentView(mRoot);
        if (Build.VERSION.SDK_INT >= 20) {
            mRoot.setOnApplyWindowInsetsListener((view, insets) -> {
                shell.setPadding(dp(22), dp(14) + insets.getSystemWindowInsetTop(),
                        dp(22), dp(12) + insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            });
        }
    }

    private void renderStep(boolean animate) {
        if (mPageContent == null) return;
        mStepCounter.setText(getString(R.string.onboarding_step_counter, mStep + 1, STEP_COUNT));
        for (int i = 0; i < mProgressSegments.length; i++) {
            mProgressSegments[i].setBackground(makeRounded(i <= mStep ? COLOR_ACCENT : 0x443d5a67, dp(3)));
        }
        mBackButton.setVisibility(mStep == 0 ? View.INVISIBLE : View.VISIBLE);
        mSkipButton.setVisibility(View.VISIBLE);
        mSkipButton.setText(mStep == STEP_COUNT - 1 ? R.string.onboarding_skip : R.string.onboarding_skip);
        mNextButton.setText(mStep == STEP_COUNT - 1 ? R.string.onboarding_finish : R.string.onboarding_continue);
        mPageContent.removeAllViews();
        mThemeCards.clear();

        View page;
        if (mStep == 0) page = buildWelcomeStep();
        else if (mStep == 1) page = buildThemeStep();
        else if (mStep == 2) page = buildComfortStep();
        else page = buildReadyStep();
        mPageContent.addView(page, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (animate && animationsEnabled()) {
            page.setAlpha(0f);
            page.setTranslationY(dp(13));
            page.animate().alpha(1f).translationY(0f).setDuration(260L)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        // Start controller/keyboard navigation on a real action. This also
        // makes the onboarding's custom focus treatment visible immediately.
        mNextButton.post(() -> {
            if (mNextButton.getVisibility() == View.VISIBLE && !mNextButton.hasFocus()) {
                mNextButton.requestFocus();
            }
        });
        if (Build.VERSION.SDK_INT >= 16) {
            mRoot.announceForAccessibility(getString(R.string.onboarding_step_counter, mStep + 1, STEP_COUNT));
        }
    }

    private View buildWelcomeStep() {
        LinearLayout page = pageColumn();
        addEyebrow(page, R.string.onboarding_feature_controller);
        addTitle(page, R.string.onboarding_welcome_title, 31);
        addBody(page, R.string.onboarding_welcome_body);

        FrameLayout previewPanel = panel();
        LinearLayout.LayoutParams panelParams = matchWrap();
        panelParams.topMargin = dp(22);
        page.addView(previewPanel, panelParams);
        KeyboardThemePreviewView preview = new KeyboardThemePreviewView(this);
        Palette palette = resolvePalette(mPrefs.getCurrentTheme());
        preview.setPalette(palette.background, palette.key, palette.text, palette.accent);
        preview.setThemeLabel(getString(R.string.onboarding_brand));
        previewPanel.addView(preview, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(190)));

        LinearLayout features = new LinearLayout(this);
        features.setOrientation(LinearLayout.HORIZONTAL);
        features.setGravity(Gravity.CENTER_VERTICAL);
        features.setPadding(0, dp(17), 0, 0);
        page.addView(features, matchWrap());
        addFeature(features, R.string.onboarding_feature_touch);
        addFeature(features, R.string.onboarding_feature_layouts);
        TextView note = text(R.string.onboarding_welcome_tip, 13, COLOR_MUTED, Typeface.NORMAL);
        note.setPadding(0, dp(20), 0, 0);
        page.addView(note, matchWrap());
        return page;
    }

    private View buildThemeStep() {
        LinearLayout page = pageColumn();
        addEyebrow(page, R.string.onboarding_step_themes);
        addTitle(page, R.string.onboarding_theme_title, 30);
        addBody(page, R.string.onboarding_theme_body);

        HorizontalScrollView carousel = new HorizontalScrollView(this);
        carousel.setHorizontalScrollBarEnabled(false);
        carousel.setClipToPadding(false);
        carousel.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        carousel.setPadding(dp(1), dp(17), dp(18), dp(8));
        LinearLayout.LayoutParams carouselParams = matchWrap();
        carouselParams.topMargin = dp(4);
        page.addView(carousel, carouselParams);
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setClipChildren(false);
        track.setClipToPadding(false);
        carousel.addView(track, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT));

        String[] names = {getString(R.string.onboarding_theme_default), getString(R.string.onboarding_theme_steam),
                getString(R.string.onboarding_theme_xbox), getString(R.string.onboarding_theme_switch),
                getString(R.string.onboarding_theme_night), getString(R.string.onboarding_theme_carbon),
                getString(R.string.onboarding_theme_midnight), getString(R.string.onboarding_theme_custom)};
        String[] descriptions = {getString(R.string.onboarding_theme_default_desc),
                getString(R.string.onboarding_theme_steam_desc), getString(R.string.onboarding_theme_xbox_desc),
                getString(R.string.onboarding_theme_switch_desc), getString(R.string.onboarding_theme_night_desc),
                getString(R.string.onboarding_theme_carbon_desc), getString(R.string.onboarding_theme_midnight_desc),
                getString(R.string.onboarding_theme_custom_desc)};
        String[] ids = {LeanKeyPreferences.THEME_DEFAULT, "SteamDeck", "Xbox", "Switch",
                LeanKeyPreferences.THEME_DARK, LeanKeyPreferences.THEME_DARK2,
                LeanKeyPreferences.THEME_DARK3, LeanKeyPreferences.THEME_CUSTOM};
        int width = Math.min(dp(270), Math.max(dp(226), getResources().getDisplayMetrics().widthPixels - dp(70)));
        for (int i = 0; i < ids.length; i++) {
            ThemeCard card = createThemeCard(ids[i], names[i], descriptions[i], width);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(width,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) cardParams.leftMargin = dp(12);
            track.addView(card.container, cardParams);
            mThemeCards.add(card);
        }
        updateThemeCardSelection();

        Button edit = new Button(this);
        edit.setText(R.string.onboarding_theme_edit);
        styleButton(edit, false);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
        editParams.topMargin = dp(12);
        page.addView(edit, editParams);
        edit.setOnClickListener(v -> openThemeEditor());

        addSectionLabel(page, R.string.onboarding_appearance_title);
        addBody(page, R.string.onboarding_appearance_note);
        LinearLayout appearance = new LinearLayout(this);
        appearance.setOrientation(LinearLayout.HORIZONTAL);
        appearance.setPadding(0, dp(11), 0, 0);
        page.addView(appearance, matchWrap());
        addAppearanceChip(appearance, LeanKeyPreferences.APPEARANCE_SYSTEM, R.string.onboarding_system);
        addAppearanceChip(appearance, LeanKeyPreferences.APPEARANCE_LIGHT, R.string.onboarding_light);
        addAppearanceChip(appearance, LeanKeyPreferences.APPEARANCE_DARK, R.string.onboarding_dark);
        return page;
    }

    private ThemeCard createThemeCard(String themeId, String name, String description, int width) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(12), dp(12), dp(12), dp(14));
        container.setBackground(makeRounded(COLOR_PANEL, dp(18)));
        container.setFocusable(true);
        container.setClickable(true);
        container.setContentDescription(name + ". " + description);
        applyCustomFocus(container, dp(18));

        KeyboardThemePreviewView preview = new KeyboardThemePreviewView(this);
        Palette palette = resolvePalette(themeId);
        preview.setPulseEnabled(false);
        preview.setPalette(palette.background, palette.key, palette.text, palette.accent);
        preview.setThemeLabel(name);
        if (LeanKeyPreferences.THEME_CUSTOM.equals(themeId)) {
            preview.setBackgroundImage(loadThemeImage(mPrefs.getCustomThemeImagePath(mPrefs.isDarkAppearance())));
        }
        LinearLayout.LayoutParams previewParams = matchWrap();
        previewParams.height = dp(126);
        container.addView(preview, previewParams);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(12);
        container.addView(titleRow, titleParams);
        TextView title = text(name, 17, COLOR_TEXT, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(26), 1f));
        TextView selected = text(R.string.onboarding_theme_selected, 9, COLOR_ACCENT, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= 21) selected.setLetterSpacing(0.08f);
        titleRow.addView(selected, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(22)));

        TextView detail = text(description, 12, COLOR_MUTED, Typeface.NORMAL);
        detail.setGravity(Gravity.TOP | Gravity.LEFT);
        LinearLayout.LayoutParams detailParams = matchWrap();
        detailParams.topMargin = dp(3);
        container.addView(detail, detailParams);

        ThemeCard card = new ThemeCard(themeId, container, preview, selected);
        container.setOnClickListener(v -> {
            mPrefs.setCurrentTheme(themeId);
            updateThemeCardSelection();
            v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(75L)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(110L).start()).start();
        });
        return card;
    }

    private void updateThemeCardSelection() {
        String selected = mPrefs.getCurrentTheme();
        for (ThemeCard card : mThemeCards) {
            boolean active = card.themeId.equals(selected);
            card.preview.setPulseEnabled(active);
            card.selected.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            card.container.setBackground(makeRounded(active ? 0xff17343e : COLOR_PANEL,
                    dp(18), active ? COLOR_ACCENT : 0x333a6474, active ? dp(1.4f) : dp(1)));
            card.container.setSelected(active);
            if (active && card.container.hasFocus()) {
                card.container.setScaleX(1f);
                card.container.setScaleY(1f);
            }
        }
    }

    private View buildComfortStep() {
        LinearLayout page = pageColumn();
        addEyebrow(page, R.string.onboarding_screen_shape);
        addTitle(page, R.string.onboarding_comfort_title, 30);
        addBody(page, R.string.onboarding_comfort_body);

        LinearLayout heightPanel = panelColumn();
        LinearLayout.LayoutParams heightParams = matchWrap();
        heightParams.topMargin = dp(18);
        page.addView(heightPanel, heightParams);
        final int savedHeight = KeyboardLayoutPreferences.getKeyboardHeightPercent(this);
        final int initialHeight = savedHeight == KeyboardLayoutPreferences.HEIGHT_AUTO
                ? 41 : savedHeight;
        TextView heightLabel = text(getString(R.string.onboarding_height_value, initialHeight),
                15, COLOR_TEXT, Typeface.BOLD);
        heightPanel.addView(heightLabel, matchWrap());
        SeekBar height = new SeekBar(this);
        applyCustomFocus(height, dp(12));
        height.setMax(KeyboardLayoutPreferences.MAX_HEIGHT_PERCENT - KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT);
        height.setProgress(initialHeight - KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT);
        height.setEnabled(savedHeight != KeyboardLayoutPreferences.HEIGHT_AUTO);
        LinearLayout.LayoutParams seekParams = matchWrap();
        seekParams.topMargin = dp(4);
        heightPanel.addView(height, seekParams);
        CheckBox autoHeight = new CheckBox(this);
        applyCustomFocus(autoHeight, dp(12));
        autoHeight.setText(R.string.onboarding_height_auto);
        autoHeight.setTextColor(COLOR_MUTED);
        if (Build.VERSION.SDK_INT >= 21) {
            autoHeight.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
        }
        autoHeight.setChecked(savedHeight == KeyboardLayoutPreferences.HEIGHT_AUTO);
        heightPanel.addView(autoHeight, matchWrap());
        height.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT;
                heightLabel.setText(getString(R.string.onboarding_height_value, value));
                if (fromUser) {
                    autoHeight.setChecked(false);
                    KeyboardLayoutPreferences.setKeyboardHeightPercent(HandheldOnboardingActivity.this, value);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        autoHeight.setOnCheckedChangeListener((button, checked) -> {
            height.setEnabled(!checked);
            if (checked) {
                heightLabel.setText(R.string.onboarding_height_auto);
                KeyboardLayoutPreferences.setKeyboardHeightPercent(HandheldOnboardingActivity.this,
                        KeyboardLayoutPreferences.HEIGHT_AUTO);
            } else {
                int value = height.getProgress() + KeyboardLayoutPreferences.MIN_HEIGHT_PERCENT;
                heightLabel.setText(getString(R.string.onboarding_height_value, value));
                KeyboardLayoutPreferences.setKeyboardHeightPercent(HandheldOnboardingActivity.this, value);
            }
        });

        LinearLayout floatingPanel = new LinearLayout(this);
        floatingPanel.setGravity(Gravity.CENTER_VERTICAL);
        floatingPanel.setOrientation(LinearLayout.HORIZONTAL);
        floatingPanel.setPadding(dp(16), dp(12), dp(12), dp(12));
        floatingPanel.setBackground(makeRounded(COLOR_PANEL, dp(16)));
        LinearLayout.LayoutParams floatingParams = matchWrap();
        floatingParams.topMargin = dp(12);
        page.addView(floatingPanel, floatingParams);
        LinearLayout floatingText = new LinearLayout(this);
        floatingText.setOrientation(LinearLayout.VERTICAL);
        floatingPanel.addView(floatingText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        floatingText.addView(text(R.string.onboarding_floating, 15, COLOR_TEXT, Typeface.BOLD), matchWrap());
        TextView floatingDesc = text(R.string.onboarding_floating_desc, 12, COLOR_MUTED, Typeface.NORMAL);
        floatingDesc.setPadding(0, dp(3), 0, 0);
        floatingText.addView(floatingDesc, matchWrap());
        Switch floating = new Switch(this);
        applyCustomFocus(floating, dp(18));
        floating.setChecked(KeyboardLayoutPreferences.isFloatingKeyboard(this));
        floating.setContentDescription(getString(R.string.onboarding_floating));
        floatingPanel.addView(floating, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        floating.setOnCheckedChangeListener((button, checked) ->
                KeyboardLayoutPreferences.setFloatingKeyboard(HandheldOnboardingActivity.this, checked));

        addSectionLabel(page, R.string.onboarding_screen_shape);
        HorizontalScrollView profileScroll = new HorizontalScrollView(this);
        profileScroll.setHorizontalScrollBarEnabled(false);
        profileScroll.setClipToPadding(false);
        LinearLayout profileRow = new LinearLayout(this);
        profileRow.setOrientation(LinearLayout.HORIZONTAL);
        profileScroll.addView(profileRow, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        String[] profiles = HandheldDisplayProfiles.profileIds();
        String[] labels = getResources().getStringArray(R.array.handheld_screen_profile_options);
        for (int i = 0; i < profiles.length; i++) {
            final String id = profiles[i];
            addSelectionChip(profileRow, labels[Math.min(i, labels.length - 1)],
                    id.equals(KeyboardLayoutPreferences.getProfile(this)),
                    () -> {
                        KeyboardLayoutPreferences.setProfile(this, id);
                        refreshProfileChips(profileRow, profiles, labels);
                    });
        }
        LinearLayout.LayoutParams profileParams = matchWrap();
        profileParams.topMargin = dp(7);
        page.addView(profileScroll, profileParams);

        addSectionLabel(page, R.string.onboarding_surface_title);
        addBody(page, R.string.onboarding_surface_desc);
        HorizontalScrollView surfaceScroll = new HorizontalScrollView(this);
        surfaceScroll.setHorizontalScrollBarEnabled(false);
        surfaceScroll.setClipToPadding(false);
        LinearLayout surfaceRow = new LinearLayout(this);
        surfaceRow.setOrientation(LinearLayout.HORIZONTAL);
        surfaceScroll.addView(surfaceRow, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        addSurfaceChip(surfaceRow, R.string.onboarding_surface_solid, LeanKeyPreferences.SURFACE_SOLID);
        addSurfaceChip(surfaceRow, R.string.onboarding_surface_translucent, LeanKeyPreferences.SURFACE_TRANSLUCENT);
        addSurfaceChip(surfaceRow, R.string.onboarding_surface_glass, LeanKeyPreferences.SURFACE_GLASS);
        addSurfaceChip(surfaceRow, R.string.onboarding_surface_liquid, LeanKeyPreferences.SURFACE_LIQUID);
        LinearLayout.LayoutParams surfaceParams = matchWrap();
        surfaceParams.topMargin = dp(7);
        page.addView(surfaceScroll, surfaceParams);
        return page;
    }

    private void refreshProfileChips(LinearLayout row, String[] profiles, String[] labels) {
        row.removeAllViews();
        String selected = KeyboardLayoutPreferences.getProfile(this);
        for (int i = 0; i < profiles.length; i++) {
            final String id = profiles[i];
            addSelectionChip(row, labels[Math.min(i, labels.length - 1)], id.equals(selected), () -> {
                KeyboardLayoutPreferences.setProfile(this, id);
                refreshProfileChips(row, profiles, labels);
            });
        }
    }

    private void addSurfaceChip(LinearLayout row, int labelRes, String mode) {
        addSelectionChip(row, getString(labelRes), mode.equals(mPrefs.getThemeSurfaceMode()), () -> {
            mPrefs.setThemeSurfaceMode(mode);
            ViewGroup parent = (ViewGroup) row.getParent();
            if (parent != null) {
                row.removeAllViews();
                addSurfaceChip(row, R.string.onboarding_surface_solid, LeanKeyPreferences.SURFACE_SOLID);
                addSurfaceChip(row, R.string.onboarding_surface_translucent, LeanKeyPreferences.SURFACE_TRANSLUCENT);
                addSurfaceChip(row, R.string.onboarding_surface_glass, LeanKeyPreferences.SURFACE_GLASS);
                addSurfaceChip(row, R.string.onboarding_surface_liquid, LeanKeyPreferences.SURFACE_LIQUID);
            }
        });
    }

    private View buildReadyStep() {
        LinearLayout page = pageColumn();
        addEyebrow(page, R.string.onboarding_step_ready);
        addTitle(page, R.string.onboarding_ready_title, 31);
        addBody(page, R.string.onboarding_ready_body);

        LinearLayout statusPanel = panelColumn();
        LinearLayout.LayoutParams panelParams = matchWrap();
        panelParams.topMargin = dp(22);
        page.addView(statusPanel, panelParams);
        View statusDot = new View(this);
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusPanel.addView(statusRow, matchWrap());
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotParams.rightMargin = dp(11);
        statusRow.addView(statusDot, dotParams);
        mActivationStatus = text("", 16, COLOR_TEXT, Typeface.BOLD);
        statusRow.addView(mActivationStatus, new LinearLayout.LayoutParams(
                0, dp(28), 1f));
        statusDot.setBackground(makeRounded(COLOR_MUTED, dp(8)));
        refreshActivationStatus(statusDot);

        Button settings = new Button(this);
        settings.setText(R.string.onboarding_open_ime_settings);
        styleButton(settings, true);
        LinearLayout.LayoutParams settingsParams = matchWrap();
        settingsParams.topMargin = dp(18);
        statusPanel.addView(settings, settingsParams);
        settings.setOnClickListener(v -> openImeSettings());

        Button picker = new Button(this);
        picker.setText(R.string.onboarding_choose_keyboard);
        styleButton(picker, false);
        LinearLayout.LayoutParams pickerParams = matchWrap();
        pickerParams.topMargin = dp(8);
        statusPanel.addView(picker, pickerParams);
        picker.setEnabled(isKeyboardEnabled());
        picker.setAlpha(picker.isEnabled() ? 1f : 0.45f);
        picker.setOnClickListener(v -> showInputMethodPicker());

        TextView finishNote = text(R.string.onboarding_ready_saved, 13, COLOR_ACCENT, Typeface.BOLD);
        finishNote.setGravity(Gravity.CENTER);
        finishNote.setPadding(0, dp(22), 0, 0);
        page.addView(finishNote, matchWrap());
        return page;
    }

    private void addAppearanceChip(LinearLayout row, String mode, int labelRes) {
        boolean selected = mode.equals(mPrefs.getKeyboardAppearanceMode());
        addSelectionChip(row, getString(labelRes), selected, () -> {
            mPrefs.setKeyboardAppearanceMode(mode);
            renderStep(true);
        });
    }

    private void addSelectionChip(LinearLayout row, String label, boolean selected, Runnable action) {
        TextView chip = text(label, 12, selected ? COLOR_BACKGROUND : COLOR_TEXT, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), dp(9), dp(14), dp(9));
        chip.setBackground(makeRounded(selected ? COLOR_ACCENT : 0xff203541, dp(18),
                selected ? COLOR_ACCENT : 0x555a7a86, dp(1)));
        chip.setClickable(true);
        chip.setFocusable(true);
        applyCustomFocus(chip, dp(18));
        chip.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
        params.rightMargin = dp(7);
        row.addView(chip, params);
    }

    private void openThemeEditor() {
        mOpeningThemeEditor = true;
        Intent intent = new Intent();
        intent.setClassName(getPackageName(),
                "com.liskovsoft.leankeyboard.activity.settings.HandheldThemeEditorActivity");
        try {
            startActivity(intent);
        } catch (RuntimeException error) {
            mOpeningThemeEditor = false;
            Toast.makeText(this, R.string.onboarding_theme_saved, Toast.LENGTH_SHORT).show();
        }
    }

    private void openImeSettings() {
        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
        try {
            startActivityForResult(intent, REQUEST_IME_SETTINGS);
        } catch (RuntimeException error) {
            try {
                startActivityForResult(new Intent(Settings.ACTION_SETTINGS), REQUEST_IME_SETTINGS);
            } catch (RuntimeException ignored) {
                Toast.makeText(this, R.string.onboarding_open_ime_settings, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showInputMethodPicker() {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null && isKeyboardEnabled()) {
            manager.showInputMethodPicker();
        }
    }

    private boolean isKeyboardEnabled() {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager == null) return false;
        for (InputMethodInfo info : manager.getEnabledInputMethodList()) {
            if (getPackageName().equals(info.getPackageName()) &&
                    LeanbackImeService.class.getName().equals(info.getServiceName())) {
                return true;
            }
        }
        return false;
    }

    private void refreshActivationStatus() {
        if (mActivationStatus != null) refreshActivationStatus(null);
    }

    private void refreshActivationStatus(View dot) {
        if (mActivationStatus == null) return;
        boolean enabled = isKeyboardEnabled();
        mActivationStatus.setText(enabled ? R.string.onboarding_ime_enabled : R.string.onboarding_ime_not_enabled);
        mActivationStatus.setTextColor(enabled ? COLOR_ACCENT : COLOR_TEXT);
        if (dot != null) dot.setBackground(makeRounded(enabled ? COLOR_ACCENT : 0xffdba75b, dp(8)));
        ViewGroup parent = (ViewGroup) mActivationStatus.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) parent;
            if (row.getChildCount() > 0) {
                row.getChildAt(0).setBackground(makeRounded(enabled ? COLOR_ACCENT : 0xffdba75b, dp(8)));
            }
        }
        View page = mActivationStatus;
        while (page != null && !(page instanceof LinearLayout && page.getParent() instanceof ScrollView)) {
            if (!(page.getParent() instanceof View)) break;
            page = (View) page.getParent();
        }
        if (page != null) updatePickerEnabled(page, enabled);
    }

    private void updatePickerEnabled(View root, boolean enabled) {
        if (root instanceof Button) {
            Button button = (Button) root;
            if (getString(R.string.onboarding_choose_keyboard).contentEquals(button.getText())) {
                button.setEnabled(enabled);
                button.setAlpha(enabled ? 1f : 0.45f);
            }
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                updatePickerEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    private void finishSetup() {
        mPrefs.setHandheldOnboardingComplete(true);
        if (!mReturnToSettings) {
            startActivity(new Intent(this, KbSettingsActivity.class));
        }
        finish();
    }

    private void addFeature(LinearLayout row, int textRes) {
        TextView badge = text(textRes, 11, COLOR_TEXT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(7), dp(10), dp(7));
        badge.setBackground(makeRounded(0x4423c5b2, dp(14)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
        params.rightMargin = dp(7);
        row.addView(badge, params);
    }

    private void addEyebrow(LinearLayout parent, int textRes) {
        TextView eyebrow = text(textRes, 11, COLOR_ACCENT, Typeface.BOLD);
        eyebrow.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(8);
        parent.addView(eyebrow, params);
    }

    private void addTitle(LinearLayout parent, int textRes, float size) {
        TextView title = text(textRes, size, COLOR_TEXT, Typeface.BOLD);
        title.setLineSpacing(dp(1), 0.96f);
        title.setMaxWidth(dp(620));
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(8);
        parent.addView(title, params);
    }

    private void addBody(LinearLayout parent, int textRes) {
        TextView body = text(textRes, 15, COLOR_MUTED, Typeface.NORMAL);
        body.setLineSpacing(dp(4), 1.0f);
        body.setMaxWidth(dp(620));
        parent.addView(body, matchWrap());
    }

    private void addSectionLabel(LinearLayout parent, int textRes) {
        TextView label = text(textRes, 16, COLOR_TEXT, Typeface.BOLD);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(22);
        params.bottomMargin = dp(4);
        parent.addView(label, params);
    }

    private LinearLayout pageColumn() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipChildren(false);
        page.setClipToPadding(false);
        return page;
    }

    private FrameLayout panel() {
        FrameLayout panel = new FrameLayout(this);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(makeRounded(COLOR_PANEL, dp(22), 0x6675afa9, dp(1)));
        return panel;
    }

    private LinearLayout panelColumn() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(15), dp(16), dp(15));
        panel.setBackground(makeRounded(COLOR_PANEL, dp(18), 0x335c7b86, dp(1)));
        return panel;
    }

    private TextView text(int textRes, float size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(textRes);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(false);
        return view;
    }

    private void styleButton(Button button, boolean primary) {
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(primary ? COLOR_BACKGROUND : COLOR_TEXT);
        button.setBackground(makeRounded(primary ? COLOR_ACCENT : COLOR_PANEL_RAISED, dp(16),
                primary ? COLOR_ACCENT : 0x555a7782, dp(1)));
        button.setMinHeight(dp(48));
        button.setPadding(dp(15), 0, dp(15), 0);
        applyCustomFocus(button, dp(16));
    }

    private void applyCustomFocus(View view, float cornerRadius) {
        if (Build.VERSION.SDK_INT >= 26) {
            view.setDefaultFocusHighlightEnabled(false);
        }

        if (Build.VERSION.SDK_INT >= 18) {
            FocusRingDrawable ring = new FocusRingDrawable(cornerRadius,
                    getResources().getDisplayMetrics().density);
            ViewOverlay overlay = view.getOverlay();
            overlay.add(ring);
            view.addOnLayoutChangeListener((changed, left, top, right, bottom,
                                           oldLeft, oldTop, oldRight, oldBottom) ->
                    ring.setBounds(0, 0, changed.getWidth(), changed.getHeight()));
            view.setOnFocusChangeListener((focusedView, hasFocus) -> {
                ring.setFocused(hasFocus);
                focusedView.animate().cancel();
                focusedView.animate()
                        .scaleX(hasFocus ? 1.018f : 1f)
                        .scaleY(hasFocus ? 1.018f : 1f)
                        .setDuration(145L)
                        .setInterpolator(new DecelerateInterpolator(1.6f))
                        .start();
                if (Build.VERSION.SDK_INT >= 21) {
                    focusedView.animate().translationZ(hasFocus ? dp(4) : 0f).start();
                }
            });
        } else {
            view.setOnFocusChangeListener((focusedView, hasFocus) -> {
                focusedView.animate().cancel();
                focusedView.animate().scaleX(hasFocus ? 1.018f : 1f)
                        .scaleY(hasFocus ? 1.018f : 1f).setDuration(145L)
                        .setInterpolator(new DecelerateInterpolator(1.6f)).start();
            });
        }
    }

    private static final class FocusRingDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radius;
        private final float density;
        private boolean focused;

        FocusRingDrawable(float radius, float density) {
            this.radius = radius;
            this.density = density;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        void setFocused(boolean value) {
            if (focused == value) return;
            focused = value;
            setVisible(value, false);
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            if (!focused) return;
            RectF bounds = new RectF(getBounds());
            float inset = 2.5f * density;
            bounds.inset(inset, inset);
            float round = Math.min(radius, Math.min(bounds.width(), bounds.height()) / 2f);

            paint.setStrokeWidth(7f * density);
            paint.setColor(0x3868ebd2);
            canvas.drawRoundRect(bounds, round, round, paint);
            paint.setStrokeWidth(3.5f * density);
            paint.setColor(0x9968ebd2);
            canvas.drawRoundRect(bounds, round, round, paint);
            paint.setStrokeWidth(1.5f * density);
            paint.setColor(COLOR_ACCENT);
            canvas.drawRoundRect(bounds, round, round, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private GradientDrawable makeRounded(int color, float radius) {
        return makeRounded(color, radius, Color.TRANSPARENT, 0f);
    }

    private GradientDrawable makeRounded(int color, float radius, int strokeColor, float strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0f) drawable.setStroke(Math.max(1, Math.round(strokeWidth)), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Palette resolvePalette(String id) {
        boolean dark = mPrefs.isDarkAppearance();
        if (LeanKeyPreferences.THEME_CUSTOM.equals(id)) {
            int background = mPrefs.getCustomThemeBackground(dark);
            int key = mPrefs.getCustomThemeKey(dark);
            int text = mPrefs.getCustomThemeText(dark);
            int accent = mPrefs.getCustomThemeAccent(dark);
            if (mPrefs.isCustomThemeUsingSystemColors() && Build.VERSION.SDK_INT >= 31) {
                background = androidColor(dark ? "system_neutral1_900" : "system_neutral1_10", background);
                key = androidColor(dark ? "system_neutral2_700" : "system_neutral2_100", key);
                text = androidColor(dark ? "system_neutral1_100" : "system_neutral1_900", text);
                accent = androidColor(dark ? "system_accent1_200" : "system_accent1_600", accent);
            }
            return new Palette(background, key, text, accent);
        }

        String suffix = id.toLowerCase(java.util.Locale.ROOT);
        int fallbackBackground = color(R.color.keyboard_background);
        int fallbackText = color(R.color.key_text_default);
        int background = id.equals(LeanKeyPreferences.THEME_DEFAULT) ? fallbackBackground
                : namedColor("keyboard_background_" + suffix, fallbackBackground);
        int text = id.equals(LeanKeyPreferences.THEME_DEFAULT) ? fallbackText
                : namedColor("key_text_default_" + suffix, fallbackText);
        int accent = id.equals(LeanKeyPreferences.THEME_DEFAULT) ? color(R.color.candidate_font_color)
                : namedColor("candidate_font_color_" + suffix, color(R.color.candidate_font_color));
        return new Palette(background, blend(background, text, 0.14f), text, accent);
    }

    private int namedColor(String name, int fallback) {
        int id = getResources().getIdentifier(name, "color", getPackageName());
        return id == 0 ? fallback : color(id);
    }

    private int androidColor(String name, int fallback) {
        int id = getResources().getIdentifier(name, "color", "android");
        return id == 0 ? fallback : color(id);
    }

    private int color(int id) {
        return ContextCompat.getColor(this, id);
    }

    private int blend(int background, int foreground, float amount) {
        float inverse = 1f - amount;
        return Color.rgb(Math.round(Color.red(background) * inverse + Color.red(foreground) * amount),
                Math.round(Color.green(background) * inverse + Color.green(foreground) * amount),
                Math.round(Color.blue(background) * inverse + Color.blue(foreground) * amount));
    }

    private Bitmap loadThemeImage(String path) {
        if (path == null || path.length() == 0) return null;
        File file = new File(path);
        if (!file.isFile()) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        int sample = 1;
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(path, options);
    }

    private void refreshThemeChips() {
        updateThemeCardSelection();
    }

    private boolean animationsEnabled() {
        if (Build.VERSION.SDK_INT >= 26) return android.animation.ValueAnimator.areAnimatorsEnabled();
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                return android.provider.Settings.Global.getFloat(getContentResolver(),
                        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
            } catch (RuntimeException ignored) {
                return true;
            }
        }
        return true;
    }

    private class AmbientGlowView extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private float phase = 0.22f;
        private android.animation.ValueAnimator animator;

        AmbientGlowView(Context context) {
            super(context);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            setFocusable(false);
            setClickable(false);
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(COLOR_BACKGROUND);
            float x = getWidth() * phase;
            float y = getHeight() * 0.16f;
            float radius = Math.max(getWidth(), getHeight()) * 0.66f;
            android.graphics.RadialGradient glow = new android.graphics.RadialGradient(x, y, radius,
                    new int[]{0x3723b8a8, 0x14246578, Color.TRANSPARENT}, null,
                    android.graphics.Shader.TileMode.CLAMP);
            paint.setShader(glow);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (!animationsEnabled()) return;
            animator = android.animation.ValueAnimator.ofFloat(0.20f, 0.80f);
            animator.setDuration(8400L);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            animator.addUpdateListener(a -> {
                phase = (Float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDetachedFromWindow() {
            if (animator != null) animator.cancel();
            animator = null;
            super.onDetachedFromWindow();
        }
    }

    private static final class Palette {
        final int background;
        final int key;
        final int text;
        final int accent;
        Palette(int background, int key, int text, int accent) {
            this.background = background;
            this.key = key;
            this.text = text;
            this.accent = accent;
        }
    }

    private static final class ThemeCard {
        final String themeId;
        final LinearLayout container;
        final KeyboardThemePreviewView preview;
        final TextView selected;
        ThemeCard(String themeId, LinearLayout container, KeyboardThemePreviewView preview, TextView selected) {
            this.themeId = themeId;
            this.container = container;
            this.preview = preview;
            this.selected = selected;
        }
    }
}
