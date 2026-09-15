package com.liskovsoft.leankeyboard.ime;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.KeyEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import androidx.core.content.ContextCompat;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardManager.KeyboardData;
import com.liskovsoft.leankeyboard.addons.theme.ThemeManager;
import com.liskovsoft.leankeyboard.addons.voice.RecognizerIntentWrapper;
import com.liskovsoft.leankeyboard.helpers.PermissionHelpers;
import com.liskovsoft.leankeyboard.activity.PermissionsActivity;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardController.InputListener;
import com.liskovsoft.leankeyboard.ime.voice.RecognizerView;
import com.liskovsoft.leankeyboard.ime.voice.SpeechLevelSource;
import com.liskovsoft.leankeyboard.activity.settings.KbLayoutActivity;
import com.liskovsoft.leankeyboard.activity.settings.KbSettingsActivity;
import com.liskovsoft.leankeyboard.addons.keyboards.KeyboardManager;
import com.liskovsoft.leankeyboard.helpers.Helpers;
import com.liskovsoft.leankeyboard.helpers.MessageHelpers;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LeanbackKeyboardContainer {
    private static final boolean DEBUG = false;
    public static final double DIRECTION_STEP_MULTIPLIER = 1.25D;
    private static final String IME_PRIVATE_OPTIONS_ESCAPE_NORTH = "EscapeNorth=1";
    private static final String IME_PRIVATE_OPTIONS_VOICE_DISMISS = "VoiceDismiss=1";
    private static final long MOVEMENT_ANIMATION_DURATION = 150L;
    private static final int MSG_START_INPUT_VIEW = 0;
    protected static final float PHYSICAL_HEIGHT_CM = 5.0F;
    protected static final float PHYSICAL_WIDTH_CM = 12.0F;
    private static final String TAG = "LbKbContainer";
    private static final int HANDHELD_MODE_ALPHA = 0;
    private static final int HANDHELD_MODE_SYMBOLS = 1;
    private static final int HANDHELD_MODE_EMOJI = 2;
    private static final int HANDHELD_MODE_TEXTMOJI = 3;
    private static final int HANDHELD_MODE_EDIT = 4;
    private static final String HANDHELD_PACKAGE = "com.handheldkeyboard.ime";
    private static final int HANDHELD_ACTION_GO = 0;
    private static final int HANDHELD_ACTION_HOME = 1;
    private static final int HANDHELD_ACTION_END = 2;
    private static final int HANDHELD_ACTION_PASTE = 3;
    public static final double TOUCH_MOVE_MIN_DISTANCE = 0.1D;
    public static final int TOUCH_STATE_CLICK = 3;
    public static final int TOUCH_STATE_NO_TOUCH = 0;
    public static final int TOUCH_STATE_TOUCH_MOVE = 2;
    public static final int TOUCH_STATE_TOUCH_SNAP = 1;
    private static final boolean VOICE_SUPPORTED = true;
    public static final Interpolator sMovementInterpolator = new DecelerateInterpolator(1.5F);
    public static final int DIRECTION_UP = 8;
    public static final int DIRECTION_DOWN = 2;
    public static final int DIRECTION_LEFT = 1;
    public static final int DIRECTION_RIGHT = 4;
    private Keyboard mAbcKeyboard;
    private Button mActionButtonView;
    private Button mActionHomeButton;
    private Button mActionEndButton;
    private Button mActionPasteButton;
    private View mHandheldActionRail;
    private final float mAlphaIn;
    private final float mAlphaOut;
    private boolean mAutoEnterSpaceEnabled;
    private boolean mCapCharacters;
    private boolean mCapSentences;
    private boolean mCapWords;
    private final int mClickAnimDur;
    private LeanbackImeService mContext;
    private KeyFocus mCurrKeyInfo = new KeyFocus();
    private DismissListener mDismissListener;
    private KeyFocus mDownKeyInfo = new KeyFocus();
    private CharSequence mEnterKeyText;
    private int mEnterKeyTextResId;
    private boolean mEscapeNorthEnabled;
    private Keyboard mInitialMainKeyboard;
    private Keyboard mEmojiKeyboard;
    private Keyboard mTextmojiKeyboard;
    private Keyboard mEditKeyboard;
    private int mHandheldKeyboardMode = HANDHELD_MODE_ALPHA;
    private KeyboardManager mKeyboardManager;
    private View mKeyboardsContainer;
    private LeanbackKeyboardView mMainKeyboardView;
    private int mMiniKbKeyIndex;
    private Keyboard mNumKeyboard;
    private float mOverestimate;
    private PointF mPhysicalSelectPos = new PointF(2.0F, 0.5F);
    private PointF mPhysicalTouchPos = new PointF(2.0F, 0.5F);
    private LeanbackKeyboardView mPrevView;
    private Intent mRecognizerIntent;
    private Rect mRect = new Rect();
    private RelativeLayout mRootView;
    private View mSelector;
    private ImageView mKeySelector;
    private Drawable mKeySelectorSquare;
    private Drawable mKeySelectorStretched;
    private ThemeManager mThemeManager;
    private ScaleAnimation mSelectorAnimation;
    private ValueAnimator mSelectorAnimator;
    private SpeechLevelSource mSpeechLevelSource;
    private SpeechRecognizer mSpeechRecognizer;
    private RecognizerIntentWrapper mRecognizerIntentWrapper;
    private LinearLayout mSuggestions;
    private View mSuggestionsBg;
    private HorizontalScrollView mSuggestionsContainer;
    private boolean mSuggestionsEnabled;
    private boolean mForceDisableSuggestions;
    private Keyboard mSymKeyboard;
    private KeyFocus mTempKeyInfo = new KeyFocus();
    private PointF mTempPoint = new PointF();
    private boolean mTouchDown = false;
    private int mTouchState = TOUCH_STATE_NO_TOUCH;
    private final int mVoiceAnimDur;
    private final VoiceIntroAnimator mVoiceAnimator;
    private RecognizerView mVoiceButtonView;
    private boolean mVoiceEnabled;
    private boolean mVoiceKeyDismissesEnabled;
    private VoiceListener mVoiceListener;
    private boolean mVoiceOn;
    private Float mX;
    private Float mY;
    private String mLabel;

    private AnimatorListener mVoiceEnterListener = new AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
            mSelector.setVisibility(View.INVISIBLE);
            startRecognition(mContext);
        }
    };

    private AnimatorListener mVoiceExitListener = new AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mSelector.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
            mVoiceButtonView.showNotListening();
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.setRecognitionListener(null);
            mVoiceOn = false;
        }
    };

    public LeanbackKeyboardContainer(Context context) {
        mContext = (LeanbackImeService) context;
        final Resources res = mContext.getResources();
        mVoiceAnimDur = res.getInteger(R.integer.voice_anim_duration);
        mAlphaIn = res.getFraction(R.fraction.alpha_in, 1, 1);
        mAlphaOut = res.getFraction(R.fraction.alpha_out, 1, 1);
        mVoiceAnimator = new LeanbackKeyboardContainer.VoiceIntroAnimator(mVoiceEnterListener, mVoiceExitListener);
        mRootView = (RelativeLayout) mContext.getLayoutInflater().inflate(R.layout.root_leanback, null);
        mKeyboardsContainer = mRootView.findViewById(R.id.keyboard);
        mSuggestionsBg = mRootView.findViewById(R.id.candidate_background);
        mSuggestionsContainer = (HorizontalScrollView) mRootView.findViewById(R.id.suggestions_container);
        mSuggestions = (LinearLayout) mSuggestionsContainer.findViewById(R.id.suggestions);
        mMainKeyboardView = (LeanbackKeyboardView) mRootView.findViewById(R.id.main_keyboard);
        mVoiceButtonView = (RecognizerView) mRootView.findViewById(R.id.voice);
        mActionButtonView = (Button) mRootView.findViewById(R.id.enter);
        mActionHomeButton = (Button) mRootView.findViewById(R.id.action_home);
        mActionEndButton = (Button) mRootView.findViewById(R.id.action_end);
        mActionPasteButton = (Button) mRootView.findViewById(R.id.action_paste);
        mHandheldActionRail = mRootView.findViewById(R.id.handheld_action_rail);
        configureHandheldActionRail();
        mSelector = mRootView.findViewById(R.id.selector);
        mKeySelector = mRootView.findViewById(R.id.key_selector);
        mKeySelectorSquare = ContextCompat.getDrawable(mContext, R.drawable.key_selector_square);
        mKeySelectorStretched = ContextCompat.getDrawable(mContext, R.drawable.key_selector);
        mThemeManager = new ThemeManager(mContext, mRootView);
        mSelectorAnimation = new ScaleAnimation((FrameLayout) mSelector);
        mOverestimate = mContext.getResources().getFraction(R.fraction.focused_scale, 1, 1);
        final float scale = context.getResources().getFraction(R.fraction.clicked_scale, 1, 1);
        mClickAnimDur = context.getResources().getInteger(R.integer.clicked_anim_duration);
        mSelectorAnimator = ValueAnimator.ofFloat(1.0F, scale);
        mSelectorAnimator.setDuration(mClickAnimDur);
        mSelectorAnimator.addUpdateListener(animation -> {
            final float value = (Float) animation.getAnimatedValue();
            mSelector.setScaleX(value);
            mSelector.setScaleY(value);
        });
        mSpeechLevelSource = new SpeechLevelSource();
        mVoiceButtonView.setSpeechLevelSource(mSpeechLevelSource);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        mRecognizerIntentWrapper = new RecognizerIntentWrapper(mContext);
        mVoiceButtonView.setCallback(new RecognizerView.Callback() {
            @Override
            public void onCancelRecordingClicked() {
                cancelVoiceRecording();
            }

            @Override
            public void onStartRecordingClicked() {
                startVoiceRecording();
            }

            @Override
            public void onStopRecordingClicked() {
                cancelVoiceRecording();
            }
        });
        mKeyboardManager = new KeyboardManager(mContext);
        initKeyboards();
    }

    private void configureHandheldActionRail() {
        boolean handheld = HANDHELD_PACKAGE.equals(mContext.getPackageName());
        if (mHandheldActionRail != null) {
            mHandheldActionRail.setVisibility(handheld ? View.VISIBLE : View.GONE);
        }
        Button[] extraButtons = {mActionHomeButton, mActionEndButton, mActionPasteButton};
        for (Button button : extraButtons) {
            if (button != null) {
                button.setVisibility(handheld ? View.VISIBLE : View.GONE);
            }
        }
        if (!handheld) {
            return;
        }

        mActionHomeButton.setOnClickListener(v -> executeHandheldRailTouchAction(HANDHELD_ACTION_HOME));
        mActionEndButton.setOnClickListener(v -> executeHandheldRailTouchAction(HANDHELD_ACTION_END));
        mActionPasteButton.setOnClickListener(v -> executeHandheldRailTouchAction(HANDHELD_ACTION_PASTE));
        mActionButtonView.setOnClickListener(v -> executeHandheldRailTouchAction(HANDHELD_ACTION_GO));
    }

    /** The edit deck owns the full keyboard width, so the separate action lane is hidden. */
    private void setHandheldActionRailVisible(boolean visible) {
        if (mHandheldActionRail != null) {
            mHandheldActionRail.setVisibility(visible && isHandheldIme() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isHandheldActionRailVisible() {
        return isHandheldIme() && mHandheldActionRail != null &&
                mHandheldActionRail.getVisibility() == View.VISIBLE;
    }

    /** Executes direct touch actions from the compact handheld action rail. */
    private void executeHandheldRailTouchAction(int action) {
        InputConnection connection = mContext.getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        switch (action) {
            case HANDHELD_ACTION_HOME:
                sendRailKeyEvent(connection, KeyEvent.KEYCODE_MOVE_HOME);
                return;
            case HANDHELD_ACTION_END:
                sendRailKeyEvent(connection, KeyEvent.KEYCODE_MOVE_END);
                return;
            case HANDHELD_ACTION_PASTE:
                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence text = clip.getItemAt(0).coerceToText(mContext);
                        if (!TextUtils.isEmpty(text)) {
                            connection.commitText(text, 1);
                        }
                    }
                }
                return;
            case HANDHELD_ACTION_GO:
            default:
                if (!mContext.sendDefaultEditorAction(true)) {
                    LeanbackUtils.sendEnterKey(connection);
                }
        }
    }

    private static void sendRailKeyEvent(InputConnection connection, int keyCode) {
        long now = System.currentTimeMillis();
        connection.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
        connection.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
    }

    /** Used by the controller so D-pad activation and touch activation agree. */
    public void onHandheldRailAction(int action, InputListener listener) {
        switch (action) {
            case HANDHELD_ACTION_HOME:
                listener.onEntry(InputListener.ENTRY_TYPE_HOME, LeanbackKeyboardView.SHIFT_OFF, null);
                return;
            case HANDHELD_ACTION_END:
                listener.onEntry(InputListener.ENTRY_TYPE_END, LeanbackKeyboardView.SHIFT_OFF, null);
                return;
            case HANDHELD_ACTION_PASTE:
                onClipboardClick(listener);
                return;
            case HANDHELD_ACTION_GO:
            default:
                listener.onEntry(InputListener.ENTRY_TYPE_ACTION, 0, null);
        }
    }

    private void configureFocus(KeyFocus focus, Rect rect, int index, int type) {
        focus.type = type;
        focus.index = index;
        focus.rect.set(rect);
    }

    /**
     * NOTE: Initialize {@link KeyFocus} with values
     * @param focus {@link KeyFocus} to configure
     * @param rect {@link Rect}
     * @param index key index
     * @param key {@link Key}
     * @param type {@link KeyFocus#type} constant
     */
    private void configureFocus(KeyFocus focus, Rect rect, int index, Key key, int type) {
        focus.type = type;
        if (key != null) {
            if (key.codes != null) {
                focus.code = key.codes[0];
            } else {
                focus.code = 0;
            }

            focus.index = index;
            focus.label = key.label;
            focus.rect.left = key.x + rect.left;
            focus.rect.top = key.y + rect.top;
            focus.rect.right = focus.rect.left + key.width;
            focus.rect.bottom = focus.rect.top + key.height;
        }
    }

    private void escapeNorth() {
        mDismissListener.onDismiss(false);
    }

    private PointF getAlignmentPosition(final float posXCm, final float posYCm, final PointF result) {
        final float width = (float) (mRootView.getWidth() - mRootView.getPaddingRight() - mRootView.getPaddingLeft());
        final float height = (float) (mRootView.getHeight() - mRootView.getPaddingTop() - mRootView.getPaddingBottom());
        final float size = mContext.getResources().getDimension(R.dimen.selector_size);
        result.x = posXCm / PHYSICAL_WIDTH_CM * (width - size) + (float) mRootView.getPaddingLeft();
        result.y = posYCm / PHYSICAL_HEIGHT_CM * (height - size) + (float) mRootView.getPaddingTop();
        return result;
    }

    private void getPhysicalPosition(final float x, final float y, final PointF position) {
        float width = (float) (mSelector.getWidth() / 2);
        float height = (float) (mSelector.getHeight() / 2);
        float posXCm = (float) (mRootView.getWidth() - mRootView.getPaddingRight() - mRootView.getPaddingLeft());
        float posYCm = (float) (mRootView.getHeight() - mRootView.getPaddingTop() - mRootView.getPaddingBottom());
        float size = mContext.getResources().getDimension(R.dimen.selector_size);
        position.x = (x - width - (float) mRootView.getPaddingLeft()) * PHYSICAL_WIDTH_CM / (posXCm - size);
        position.y = (y - height - (float) mRootView.getPaddingTop()) * PHYSICAL_HEIGHT_CM / (posYCm - size);
    }

    private PointF getTouchSnapPosition() {
        PointF position = new PointF();
        getPhysicalPosition((float) mCurrKeyInfo.rect.centerX(), (float) mCurrKeyInfo.rect.centerY(), position);
        return position;
    }

    public void initKeyboards() {
        updateAddonKeyboard();
    }

    private boolean isMatch(Locale var1, Locale[] var2) {
        int var4 = var2.length;

        for (int var3 = 0; var3 < var4; ++var3) {
            Locale var5 = var2[var3];
            if ((TextUtils.isEmpty(var5.getLanguage()) || TextUtils.equals(var1.getLanguage(), var5.getLanguage())) && (TextUtils.isEmpty
                    (var5.getCountry()) || TextUtils.equals(var1.getCountry(), var5.getCountry()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * NOTE: Move focus to specified key
     * @param index key index
     * @param type {@link KeyFocus#type} constant
     */
    private void moveFocusToIndex(int index, int type) {
        Key key = mMainKeyboardView.getKey(index);
        configureFocus(mTempKeyInfo, mRect, index, key, type);
        setTouchState(TOUCH_STATE_NO_TOUCH);
        setKbFocus(mTempKeyInfo, true, true);
    }

    private void offsetRect(Rect rect, View view) {
        rect.left = 0;
        rect.top = 0;
        rect.right = view.getWidth();
        rect.bottom = view.getHeight();
        mRootView.offsetDescendantRectToMyCoords(view, rect);
    }

    private void onToggleCapsLock() {
        onShiftDoubleClick(isCapsLockOn());
    }

    /**
     * NOTE: Init currently displayed keyboard<br/>
     * All keyboard settings applied here<br/>
     * This method is called constantly on new field
     * @param res resources (not used)
     * @param info current ime attributes
     */
    private void setImeOptions(Resources res, EditorInfo info) {
        // do not erase last keyboard
        if (mInitialMainKeyboard == null) {
            mInitialMainKeyboard = mAbcKeyboard;
        }

        mSuggestionsEnabled = true;
        mAutoEnterSpaceEnabled = false;
        mVoiceEnabled = true;
        mEscapeNorthEnabled = false;
        mVoiceKeyDismissesEnabled = false;

        switch (LeanbackUtils.getInputTypeClass(info)) {
            case InputType.TYPE_CLASS_TEXT:
                switch (LeanbackUtils.getInputTypeVariation(info)) {
                    case InputType.TYPE_DATETIME_VARIATION_DATE:
                    case InputType.TYPE_DATETIME_VARIATION_TIME:
                    case InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:
                    case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
                        mSuggestionsEnabled = true;
                        mAutoEnterSpaceEnabled = false;
                        mVoiceEnabled = true;
                        mInitialMainKeyboard = mAbcKeyboard;
                        break;
                    case InputType.TYPE_TEXT_VARIATION_PERSON_NAME:
                    case InputType.TYPE_TEXT_VARIATION_PASSWORD:
                    case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
                    case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
                        mSuggestionsEnabled = true; // use suggestion widget as input indicator
                        mVoiceEnabled = false;
                        mInitialMainKeyboard = mAbcKeyboard;
                }
                break;
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_PHONE:
            case InputType.TYPE_CLASS_DATETIME:
                mSuggestionsEnabled = true;
                mVoiceEnabled = false;
                mInitialMainKeyboard = mAbcKeyboard;
        }

        if (mSuggestionsEnabled) {
            if ((info.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                mSuggestionsEnabled = false;
            }
        }

        // NOTE: bug fix: any field: first char in upper case
        //if (!mAutoEnterSpaceEnabled) {
        //    if ((info.inputType & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) {
        //        mCapSentences = true;
        //    }
        //}

        if (mAutoEnterSpaceEnabled && !mSuggestionsEnabled) {
            mAutoEnterSpaceEnabled = false;
        }

        // NOTE: bug fix: any field: first char in upper case
        //if ((info.inputType & InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0 ||
        //    LeanbackUtils.getInputTypeVariation(info) == InputType.TYPE_TEXT_VARIATION_PERSON_NAME) {
        //    mCapWords = true;
        //}

        // NOTE: bug fix: password field: all chars in upper case
        //if ((info.inputType & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
        //    mCapCharacters = true;
        //}

        if (mForceDisableSuggestions) {
            mSuggestionsEnabled = false;
        }

        if (info.privateImeOptions != null) {
            if (info.privateImeOptions.contains(IME_PRIVATE_OPTIONS_ESCAPE_NORTH)) {
                mEscapeNorthEnabled = true;
            }

            if (info.privateImeOptions.contains(IME_PRIVATE_OPTIONS_VOICE_DISMISS)) {
                mVoiceKeyDismissesEnabled = true;
            }
        }

        mEnterKeyText = info.actionLabel;
        if (TextUtils.isEmpty(mEnterKeyText)) {
            switch (LeanbackUtils.getImeAction(info)) {
                case EditorInfo.IME_ACTION_GO:
                    mEnterKeyTextResId = R.string.label_go_key;
                    return;
                case EditorInfo.IME_ACTION_SEARCH:
                    mEnterKeyTextResId = R.string.label_search_key;
                    return;
                case EditorInfo.IME_ACTION_SEND:
                    mEnterKeyTextResId = R.string.label_send_key;
                    return;
                case EditorInfo.IME_ACTION_NEXT:
                    mEnterKeyTextResId = R.string.label_next_key;
                    return;
                default:
                    mEnterKeyTextResId = R.string.label_done_key;
            }
        }

    }

    /**
     * Move focus to specified key
     * @param focus key that will be focused
     * @param forceFocusChange force focus
     * @param animate animate transition
     */
    private void setKbFocus(final KeyFocus focus, final boolean forceFocusChange, final boolean animate) {
        boolean clicked = true;
        if (!focus.equals(mCurrKeyInfo) || forceFocusChange) {
            LeanbackKeyboardView prevView = mPrevView;
            mPrevView = null;
            boolean overestimateWidth = false;
            boolean overestimateHeight = false;
            switch (focus.type) {
                case KeyFocus.TYPE_MAIN:
                    boolean showScale = false;
                    overestimateHeight = true;
                    if (focus.code != LeanbackKeyboardView.ASCII_SPACE) {
                        overestimateWidth = true;
                        showScale = true;
                    }

                    LeanbackKeyboardView mainView = mMainKeyboardView;
                    int index = focus.index;

                    boolean isClicked = false;
                    if (mTouchState == TOUCH_STATE_CLICK) {
                        isClicked = true;
                    }

                    mainView.setFocus(index, isClicked, showScale);
                    mPrevView = mMainKeyboardView;
                    break;
                case KeyFocus.TYPE_VOICE:
                    mVoiceButtonView.setMicFocused(true);
                    dismissMiniKeyboard();
                    break;
                case KeyFocus.TYPE_ACTION:
                    LeanbackUtils.sendAccessibilityEvent(getHandheldActionButton(focus.index), true);
                    dismissMiniKeyboard();
                    break;
                case KeyFocus.TYPE_SUGGESTION:
                    dismissMiniKeyboard();
            }

            if (prevView != null && prevView != mPrevView) {
                if (mTouchState != TOUCH_STATE_CLICK) {
                    clicked = false;
                }

                prevView.setFocus(-1, clicked);
            }

            setSelectorToFocus(focus.rect, overestimateWidth, overestimateHeight, animate);
            mCurrKeyInfo.set(focus);
        }
    }

    /**
     * Set keyboard shift sate
     * @param state one of the
     * {@link LeanbackKeyboardView#SHIFT_ON SHIFT_ON},
     * {@link LeanbackKeyboardView#SHIFT_OFF SHIFT_OFF},
     * {@link LeanbackKeyboardView#SHIFT_LOCKED SHIFT_LOCKED}
     * constants
     */
    private void setShiftState(int state) {
        mMainKeyboardView.setShiftState(state);
    }

    private void setTouchStateInternal(int state) {
        mTouchState = state;
    }

    /**
     * NOTE: Speech recognizer routine
     * @param context context
     */
    private void startRecognition(Context context) {
        // MANAGE_EXTERNAL_STORAGE does not work on Android 14
        if ((PermissionHelpers.hasStoragePermissions(context) || VERSION.SDK_INT >= 34) &&
            PermissionHelpers.hasMicPermissions(context)) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                mRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                mSpeechRecognizer.setRecognitionListener(new MyVoiceRecognitionListener());

                mSpeechRecognizer.startListening(mRecognizerIntent);
            } else {
                mRecognizerIntentWrapper.setListener(searchText -> mVoiceListener.onVoiceResult(searchText));
                mRecognizerIntentWrapper.startListening();

                //String noRecognition = "Seems that the voice recognition is not enabled on your device";
                //
                //MessageHelpers.showLongMessage(context, noRecognition);
                //
                //Log.e(TAG, noRecognition);
            }
        } else {
            Helpers.startActivity(context, PermissionsActivity.class);
        }
    }

    public void alignSelector(final float x, final float y, final boolean playAnimation) {
        final float translatedX = x - (float) (mSelector.getWidth() / 2);
        final float translatedY = y - (float) (mSelector.getHeight() / 2);
        if (!playAnimation) {
            mSelector.setX(translatedX);
            mSelector.setY(translatedY);
        } else {
            mSelector.animate()
                     .x(translatedX)
                     .y(translatedY)
                     .setInterpolator(sMovementInterpolator)
                     .setDuration(MOVEMENT_ANIMATION_DURATION)
                     .start();
        }
    }

    public boolean areSuggestionsEnabled() {
        return mSuggestionsEnabled;
    }

    public void setSuggestionsEnabled(boolean enabled) {
        mSuggestionsEnabled = enabled;
        mForceDisableSuggestions = !enabled;
    }

    public void cancelVoiceRecording() {
        mVoiceAnimator.startExitAnimation();
    }

    public void clearSuggestions() {
        mSuggestions.removeAllViews();
        if (getCurrFocus().type == KeyFocus.TYPE_SUGGESTION) {
            resetFocusCursor();
        }
    }

    public boolean dismissMiniKeyboard() {
        return mMainKeyboardView.dismissMiniKeyboard();
    }

    public boolean enableAutoEnterSpace() {
        return mAutoEnterSpaceEnabled;
    }

    /**
     * Initialize {@link KeyFocus focus} variable based on supplied coordinates
     * @param x x coordinates
     * @param y y coordinates
     * @param focus result focus
     * @return whether focus is found or not
     */
    public boolean getBestFocus(final Float x, final Float y, final KeyFocus focus) {
        int actionLeft = Integer.MAX_VALUE;
        if (isHandheldActionRailVisible()) {
            offsetRect(mRect, mActionButtonView);
            actionLeft = mRect.left;
        }
        offsetRect(mRect, mMainKeyboardView);
        int keyboardTop = mRect.top;
        Float newX = x;
        if (x == null) {
            newX = mX;
        }

        Float newY = y;
        if (y == null) {
            newY = mY;
        }

        int count = mSuggestions.getChildCount();
        if (newY < (float) keyboardTop && count > 0 && mSuggestionsEnabled) {
            for (actionLeft = 0; actionLeft < count; ++actionLeft) {
                View view = mSuggestions.getChildAt(actionLeft);
                offsetRect(mRect, view);
                if (newX < (float) mRect.right || actionLeft + 1 == count) {
                    view.requestFocus();
                    LeanbackUtils.sendAccessibilityEvent(view.findViewById(R.id.text), true);
                    configureFocus(focus, mRect, actionLeft, KeyFocus.TYPE_SUGGESTION);
                    break;
                }
            }

            return true;
        } else if (newY < (float) keyboardTop && mEscapeNorthEnabled) {
            escapeNorth();
            return false;
        } else if (isHandheldActionRailVisible() && newX > (float) actionLeft) {
            configureNearestHandheldActionFocus(focus, newY);
            return true;
        } else {
            mX = newX;
            mY = newY;
            offsetRect(mRect, mMainKeyboardView);
            final float left = (float) mRect.left;
            final float top = (float) mRect.top;
            int keyIdx = mMainKeyboardView.getNearestIndex(newX - left, newY - top);
            Key key = mMainKeyboardView.getKey(keyIdx);
            configureFocus(focus, mRect, keyIdx, key, 0);
            return true;
        }
    }

    public LeanbackKeyboardContainer.KeyFocus getCurrFocus() {
        return mCurrKeyInfo;
    }

    public int getCurrKeyCode() {
        int keyCode = 0;
        Key key = getKey(mCurrKeyInfo.type, mCurrKeyInfo.index);
        if (key != null) {
            keyCode = key.codes[0];
        }

        return keyCode;
    }

    public Button getGoButton() {
        return mActionButtonView;
    }

    public Key getKey(int type, int index) {
        return type == KeyFocus.TYPE_MAIN ? this.mMainKeyboardView.getKey(index) : null;
    }

    public void updateCyclicFocus(int dir, KeyFocus oldFocus, KeyFocus newFocus) {
        if (oldFocus.equals(newFocus) || LeanbackUtils.isSubmitButton(newFocus)) {
            if (LeanKeyPreferences.instance(mContext).isCyclicNavigationEnabled()) {
                if (dir == DIRECTION_RIGHT || dir == DIRECTION_LEFT) {
                    Rect actionRect = new Rect();
                    boolean hasActionRail = isHandheldActionRailVisible();
                    if (hasActionRail) {
                        offsetRect(actionRect, mActionButtonView);
                    }
                    // A handheld action key spans the full height of its lane.
                    // Its bottom still aligns with the cursor/action row, which
                    // is the intended controller entry point for Go/Send.
                    boolean onSameRow = hasActionRail &&
                            Math.abs(oldFocus.rect.bottom - actionRect.bottom) < 20;

                    if (onSameRow && !LeanbackUtils.isSubmitButton(oldFocus)) {
                        // move focus to submit button
                        offsetRect(mRect, mActionButtonView);
                        configureFocus(newFocus, mRect, 0, KeyFocus.TYPE_ACTION);
                    } else {
                        offsetRect(mRect, mMainKeyboardView);
                        float x = dir == DIRECTION_RIGHT ? 0 : mRect.right; // 0 - rightmost position, right - leftmost
                        float sourceY = LeanbackUtils.isSubmitButton(oldFocus)
                                ? mRect.bottom - 1f
                                : oldFocus.rect.top - mRect.top;
                        int keyIdx = mMainKeyboardView.getNearestIndex(x, sourceY);
                        Key key = mMainKeyboardView.getKey(keyIdx);
                        configureFocus(newFocus, mRect, keyIdx, key, 0);
                    }
                } else if (dir == DIRECTION_DOWN || dir == DIRECTION_UP) {
                    if (!LeanbackUtils.isSubmitButton(oldFocus)) {
                        offsetRect(mRect, mMainKeyboardView);
                        float y = dir == DIRECTION_DOWN ? 0 : mRect.bottom; // 0 - topmost position, bottom - downmost
                        int delta = (oldFocus.rect.right - oldFocus.rect.left) / 2; // fix space position
                        int keyIdx = mMainKeyboardView.getNearestIndex(oldFocus.rect.left + delta - mRect.left, y);
                        Key key = mMainKeyboardView.getKey(keyIdx);
                        configureFocus(newFocus, mRect, keyIdx, key, 0);
                    }
                }
            } else if (dir == DIRECTION_UP) {
                // Hide the keyboard when moving focus out of the keyboard
                mContext.hideIme();
            }

            String direction = "UNKNOWN";

            switch (dir) {
                case LeanbackKeyboardContainer.DIRECTION_DOWN:
                    direction = "DOWN";
                    break;
                case LeanbackKeyboardContainer.DIRECTION_LEFT:
                    direction = "LEFT";
                    break;
                case LeanbackKeyboardContainer.DIRECTION_RIGHT:
                    direction = "RIGHT";
                    break;
                case LeanbackKeyboardContainer.DIRECTION_UP:
                    direction = "UP";
                    break;
            }

            Log.d(TAG, "Same key focus found! Direction: " + direction);
        }
    }

    public boolean getNextFocusInDirection(int direction, KeyFocus startFocus, KeyFocus nextFocus) {
        switch (startFocus.type) {
            case KeyFocus.TYPE_MAIN:
                Key currentKey = getKey(startFocus.type, startFocus.index);
                if (mHandheldKeyboardMode == HANDHELD_MODE_EDIT &&
                        moveWithinQuickSettingsDeck(direction, startFocus, nextFocus)) {
                    return true;
                }
                if (mHandheldKeyboardMode == HANDHELD_MODE_EDIT) {
                    // Quick settings is a bounded 2x4 grid. Do not let the generic
                    // keyboard fallback wrap an edge into a stale previous focus.
                    return false;
                }
                // The extra action rail sits outside the XML keyboard grid. A
                // generic nearest-key search can incorrectly choose the
                // cursor-right key below a row (for example Delete -> Right)
                // before it considers that rail. Exit directly into the rail
                // from every right-edge key and preserve the row's Y position.
                if (isHandheldActionRailVisible()
                        && (direction & DIRECTION_RIGHT) != 0
                        && currentKey != null
                        && (currentKey.edgeFlags & Keyboard.EDGE_RIGHT) != 0) {
                    configureNearestHandheldActionFocus(nextFocus, startFocus.rect.centerY());
                    return true;
                }
                if (isHandheldIme()
                        && !isHandheldActionRailVisible()
                        && (direction & DIRECTION_RIGHT) != 0
                        && currentKey != null
                        && (currentKey.edgeFlags & Keyboard.EDGE_RIGHT) != 0) {
                    // The edit deck uses all available width. Do not navigate into the
                    // hidden alpha-keyboard action rail from its right edge.
                    // Returning true would let the controller reuse a stale temporary
                    // focus, causing Esc to jump to a previous key.
                    return false;
                }
                if (moveToDirectionalKey(direction, startFocus, nextFocus)) {
                    return true;
                }

                // Keep the existing transitions out of the keyboard (suggestions, action
                // button, and cyclic wrap handling) when there is no key in this direction.
                Key key = currentKey;
                if (key == null) {
                    return false;
                }
                float centerDelta = (float) startFocus.rect.height() / 2.0F;
                float centerX = (float) startFocus.rect.centerX();
                float centerY = (float) startFocus.rect.centerY();

                if ((direction & DIRECTION_LEFT) != 0) {
                    if ((key.edgeFlags & Keyboard.EDGE_LEFT) == 0) {
                        centerX = (float) startFocus.rect.left - centerDelta;
                    }
                } else if ((direction & DIRECTION_RIGHT) != 0) {
                    if ((key.edgeFlags & Keyboard.EDGE_RIGHT) != 0) {
                        offsetRect(mRect, mActionButtonView);
                        centerX = (float) mRect.centerX();
                    } else {
                        centerX = (float) startFocus.rect.right + centerDelta;
                    }
                }

                if ((direction & DIRECTION_UP) != 0) {
                    centerDelta = (float) ((double) centerY - (double) startFocus.rect.height() * DIRECTION_STEP_MULTIPLIER);
                } else {
                    centerDelta = centerY;
                    if ((direction & DIRECTION_DOWN) != 0) {
                        centerDelta = (float) ((double) centerY + (double) startFocus.rect.height() * DIRECTION_STEP_MULTIPLIER);
                    }
                }

                getPhysicalPosition(centerX, centerDelta, mTempPoint);
                return getBestFocus(centerX, centerDelta, nextFocus);
            case KeyFocus.TYPE_VOICE:
            default:
                break;
            case KeyFocus.TYPE_ACTION:
                if ((direction & (DIRECTION_UP | DIRECTION_DOWN)) != 0) {
                    int nextAction = getAdjacentHandheldAction(startFocus.index, direction);
                    if (nextAction != -1) {
                        configureHandheldActionFocus(nextFocus, nextAction);
                        return true;
                    }
                    if ((direction & DIRECTION_UP) != 0) {
                        offsetRect(mRect, mMainKeyboardView);
                        offsetRect(mRect, mSuggestions);
                        return getBestFocus((float) startFocus.rect.centerX(), (float) mRect.centerY(), nextFocus);
                    }
                    return true;
                }

                if ((direction & DIRECTION_LEFT) != 0) {
                    Button actionButton = getHandheldActionButton(startFocus.index);
                    Rect actionRect = new Rect();
                    offsetRect(actionRect, actionButton);
                    offsetRect(mRect, mMainKeyboardView);
                    return getBestFocus((float) mRect.right,
                            Math.min((float) mRect.bottom - 1f,
                                    (float) actionRect.centerY()), nextFocus);
                }
                break;
            case KeyFocus.TYPE_SUGGESTION:
                if ((direction & DIRECTION_DOWN) != 0) {
                    offsetRect(mRect, mMainKeyboardView);
                    return getBestFocus((float) startFocus.rect.centerX(), (float) mRect.top, nextFocus);
                }

                if ((direction & DIRECTION_UP) != 0) {
                    if (mEscapeNorthEnabled) {
                        escapeNorth();
                        return true;
                    }
                } else {
                    boolean left = (direction & DIRECTION_LEFT) != 0;

                    boolean right = (direction & DIRECTION_RIGHT) != 0;

                    if (left || right) {
                        offsetRect(mRect, mRootView);
                        MarginLayoutParams params = (MarginLayoutParams) mSuggestionsContainer.getLayoutParams();
                        int leftCalc = mRect.left + params.leftMargin;
                        int rightCalc = mRect.right - params.rightMargin;
                        int focusIdx = startFocus.index;
                        byte delta;
                        if (left) {
                            delta = -1;
                        } else {
                            delta = 1;
                        }

                        int suggestIdx = focusIdx + delta;
                        View suggestion = mSuggestions.getChildAt(suggestIdx);
                        if (suggestion != null) {
                            offsetRect(mRect, suggestion);
                            if (mRect.left < leftCalc && mRect.right > rightCalc) {
                                mRect.left = leftCalc;
                                mRect.right = rightCalc;
                            } else if (mRect.left < leftCalc) {
                                mRect.right = mRect.width() + leftCalc;
                                mRect.left = leftCalc;
                            } else if (mRect.right > rightCalc) {
                                mRect.left = rightCalc - mRect.width();
                                mRect.right = rightCalc;
                            }

                            suggestion.requestFocus();
                            LeanbackUtils.sendAccessibilityEvent(suggestion.findViewById(R.id.text), true);
                            configureFocus(nextFocus, mRect, suggestIdx, KeyFocus.TYPE_SUGGESTION);
                            return true;
                        }
                    }
                }
        }

        return true;
    }

    /**
     * The quick-settings deck is an intentional 2x4 controller grid. Use direct
     * row/column navigation rather than the free-form keyboard geometry used by
     * letter rows and the wide space key.
     */
    private boolean moveWithinQuickSettingsDeck(int direction, KeyFocus startFocus,
                                                KeyFocus nextFocus) {
        if (direction != DIRECTION_LEFT && direction != DIRECTION_RIGHT &&
                direction != DIRECTION_UP && direction != DIRECTION_DOWN) {
            return false;
        }
        int source = startFocus.index;
        int keyCount = mMainKeyboardView.getKeyCount();
        if (source < 0 || source >= keyCount || keyCount != 8) {
            return false;
        }

        int row = source / 4;
        int column = source % 4;
        int target = source;
        switch (direction) {
            case DIRECTION_LEFT:
                if (column == 0) return false;
                target--;
                break;
            case DIRECTION_RIGHT:
                if (column == 3) return false;
                target++;
                break;
            case DIRECTION_UP:
                if (row == 0) return false;
                target -= 4;
                break;
            case DIRECTION_DOWN:
                if (row == 1) return false;
                target += 4;
                break;
            default:
                return false;
        }

        Key targetKey = mMainKeyboardView.getKey(target);
        if (targetKey == null) {
            return false;
        }
        Rect keyboardRect = new Rect();
        offsetRect(keyboardRect, mMainKeyboardView);
        configureFocus(nextFocus, keyboardRect, target, targetKey, KeyFocus.TYPE_MAIN);
        return true;
    }

    /**
     * Move to the nearest actual key rectangle in the requested direction.
     * The previous point projection used half a key's height for horizontal movement and
     * then quantized the result into a fixed grid. That could leave focus on the same key
     * on wide handheld layouts, and it did not model the wide Space key or uneven rows.
     */
    private boolean moveToDirectionalKey(int direction, KeyFocus startFocus, KeyFocus nextFocus) {
        int horizontalDirection = direction & (DIRECTION_LEFT | DIRECTION_RIGHT);
        int verticalDirection = direction & (DIRECTION_UP | DIRECTION_DOWN);
        if (horizontalDirection == (DIRECTION_LEFT | DIRECTION_RIGHT) ||
                verticalDirection == (DIRECTION_UP | DIRECTION_DOWN)) {
            return false;
        }

        KeyboardKeyNavigator.Direction navigationDirection;
        if (horizontalDirection == DIRECTION_LEFT && verticalDirection == DIRECTION_UP) {
            navigationDirection = KeyboardKeyNavigator.Direction.UP_LEFT;
        } else if (horizontalDirection == DIRECTION_RIGHT && verticalDirection == DIRECTION_UP) {
            navigationDirection = KeyboardKeyNavigator.Direction.UP_RIGHT;
        } else if (horizontalDirection == DIRECTION_LEFT && verticalDirection == DIRECTION_DOWN) {
            navigationDirection = KeyboardKeyNavigator.Direction.DOWN_LEFT;
        } else if (horizontalDirection == DIRECTION_RIGHT && verticalDirection == DIRECTION_DOWN) {
            navigationDirection = KeyboardKeyNavigator.Direction.DOWN_RIGHT;
        } else if (horizontalDirection == DIRECTION_LEFT) {
            navigationDirection = KeyboardKeyNavigator.Direction.LEFT;
        } else if (horizontalDirection == DIRECTION_RIGHT) {
            navigationDirection = KeyboardKeyNavigator.Direction.RIGHT;
        } else if (verticalDirection == DIRECTION_UP) {
            navigationDirection = KeyboardKeyNavigator.Direction.UP;
        } else if (verticalDirection == DIRECTION_DOWN) {
            navigationDirection = KeyboardKeyNavigator.Direction.DOWN;
        } else {
            return false;
        }

        Rect keyboardRect = new Rect();
        offsetRect(keyboardRect, mMainKeyboardView);
        int keyCount = mMainKeyboardView.getKeyCount();
        List<KeyboardKeyNavigator.KeyBounds> keyBounds = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            Key candidate = mMainKeyboardView.getKey(i);
            keyBounds.add(candidate == null ? null : new KeyboardKeyNavigator.KeyBounds(
                    candidate.x, candidate.y,
                    candidate.x + candidate.width, candidate.y + candidate.height));
        }

        int bestIndex = KeyboardKeyNavigator.findNext(keyBounds, startFocus.index, navigationDirection);
        Key bestKey = mMainKeyboardView.getKey(bestIndex);
        if (bestKey == null) {
            return false;
        }

        configureFocus(nextFocus, keyboardRect, bestIndex, bestKey, KeyFocus.TYPE_MAIN);
        return true;
    }

    public CharSequence getSuggestionText(int idx) {
        CharSequence result = null;
        if (idx >= 0) {
            if (idx < mSuggestions.getChildCount()) {
                Button btn = mSuggestions.getChildAt(idx).findViewById(R.id.text);
                if (btn != null) {
                    result = btn.getText();
                }
            }
        }

        return result;
    }

    public int getTouchState() {
        return mTouchState;
    }

    public RelativeLayout getView() {
        return mRootView;
    }

    public boolean isCapsLockOn() {
        return mMainKeyboardView.getShiftState() == LeanbackKeyboardView.SHIFT_LOCKED;
    }

    public boolean isCurrKeyShifted() {
        return mMainKeyboardView.isShifted();
    }

    public boolean isMiniKeyboardOnScreen() {
        return mMainKeyboardView.isMiniKeyboardOnScreen();
    }

    public boolean isVoiceEnabled() {
        return mVoiceEnabled;
    }

    public boolean isVoiceVisible() {
        return mVoiceButtonView.getVisibility() == View.VISIBLE;
    }

    public void onInitInputView() {
        resetFocusCursor();
        mSelector.setVisibility(View.VISIBLE);
    }

    public boolean onKeyLongPress() {
        int keyCode = mCurrKeyInfo.code;
        if (keyCode == LeanbackKeyboardView.KEYCODE_SHIFT) {
            onToggleCapsLock();
            setTouchState(LeanbackKeyboardContainer.TOUCH_STATE_NO_TOUCH);
            return true;
        } else if (keyCode == LeanbackKeyboardView.ASCII_SPACE) {
            LeanbackUtils.showKeyboardPicker(mContext);
            // Keyboard may stuck on screen. Fixing it...
            mContext.stopSelf();
            // Revert button touch states to normal
            setTouchState(LeanbackKeyboardContainer.TOUCH_STATE_NO_TOUCH);
            return true;
        } else if (keyCode == LeanbackKeyboardView.KEYCODE_LANG_TOGGLE) {
            openKeyboardSettings();
            return true;
        } else {
            if (mCurrKeyInfo.type == KeyFocus.TYPE_MAIN) {
                mMainKeyboardView.onKeyLongPress();
                if (mMainKeyboardView.isMiniKeyboardOnScreen()) {
                    mMiniKbKeyIndex = mCurrKeyInfo.index;
                    moveFocusToIndex(mMainKeyboardView.getBaseMiniKbIndex(), KeyFocus.TYPE_MAIN);
                    return true;
                }
            }

            return false;
        }
    }

    public void openKeyboardSettings() {
        Helpers.startActivity(mContext, KbSettingsActivity.class);
        mContext.hideIme();
    }

    /** Opens one of the handheld settings areas from the keyboard quick-settings deck. */
    public void openHandheldSettingsSection(String section) {
        if (!isHandheldIme()) {
            openKeyboardSettings();
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(mContext.getPackageName(),
                "com.liskovsoft.leankeyboard.activity.settings.HandheldSettingsActivity");
        intent.putExtra("open_section", section);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Helpers.startIntent(mContext, intent)) {
            mContext.hideIme();
        }
    }

    public void onModeChangeClick() {
        dismissMiniKeyboard();
        if (isHandheldIme() && (mHandheldKeyboardMode == HANDHELD_MODE_EMOJI ||
                mHandheldKeyboardMode == HANDHELD_MODE_TEXTMOJI ||
                mHandheldKeyboardMode == HANDHELD_MODE_EDIT)) {
            setHandheldActionRailVisible(true);
            mMainKeyboardView.setKeyboard(mInitialMainKeyboard);
            mHandheldKeyboardMode = HANDHELD_MODE_ALPHA;
        } else if (mMainKeyboardView.getKeyboard().equals(mSymKeyboard)) {
            setHandheldActionRailVisible(true);
            mMainKeyboardView.setKeyboard(mInitialMainKeyboard);
            mHandheldKeyboardMode = HANDHELD_MODE_ALPHA;
        } else {
            setHandheldActionRailVisible(true);
            mMainKeyboardView.setKeyboard(mSymKeyboard);
            mHandheldKeyboardMode = HANDHELD_MODE_SYMBOLS;
        }
    }

    private Button getHandheldActionButton(int action) {
        switch (action) {
            case HANDHELD_ACTION_HOME:
                return mActionHomeButton;
            case HANDHELD_ACTION_END:
                return mActionEndButton;
            case HANDHELD_ACTION_PASTE:
                return mActionPasteButton;
            case HANDHELD_ACTION_GO:
            default:
                return mActionButtonView;
        }
    }

    private void configureHandheldActionFocus(KeyFocus focus, int action) {
        if (!isHandheldActionRailVisible()) {
            return;
        }
        Button button = getHandheldActionButton(action);
        if (button == null || button.getVisibility() != View.VISIBLE) {
            button = mActionButtonView;
            action = HANDHELD_ACTION_GO;
        }
        offsetRect(mRect, button);
        configureFocus(focus, mRect, action, KeyFocus.TYPE_ACTION);
    }

    private void configureNearestHandheldActionFocus(KeyFocus focus, float y) {
        int[] actions = {HANDHELD_ACTION_HOME, HANDHELD_ACTION_END,
                HANDHELD_ACTION_PASTE, HANDHELD_ACTION_GO};
        int nearestAction = HANDHELD_ACTION_GO;
        float nearestDistance = Float.MAX_VALUE;
        for (int action : actions) {
            Button button = getHandheldActionButton(action);
            if (button == null || button.getVisibility() != View.VISIBLE) {
                continue;
            }
            offsetRect(mRect, button);
            float distance = Math.abs(y - mRect.centerY());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestAction = action;
            }
        }
        configureHandheldActionFocus(focus, nearestAction);
    }

    private int getAdjacentHandheldAction(int action, int direction) {
        int[] verticalOrder = {HANDHELD_ACTION_HOME, HANDHELD_ACTION_END,
                HANDHELD_ACTION_PASTE, HANDHELD_ACTION_GO};
        for (int i = 0; i < verticalOrder.length; i++) {
            if (verticalOrder[i] != action) {
                continue;
            }
            int next = i + (direction == DIRECTION_UP ? -1 : 1);
            if (next >= 0 && next < verticalOrder.length) {
                return verticalOrder[next];
            }
        }
        return -1;
    }

    public void onEmojiClick() {
        if (!isHandheldIme()) {
            return;
        }
        dismissMiniKeyboard();
        if (mEmojiKeyboard == null) {
            mEmojiKeyboard = new Keyboard(mContext, R.xml.handheld_emoji);
            fillHandheldRows(mEmojiKeyboard, mEmojiKeyboard.getMinWidth());
        }
        setHandheldActionRailVisible(true);
        mMainKeyboardView.setKeyboard(mEmojiKeyboard);
        mHandheldKeyboardMode = HANDHELD_MODE_EMOJI;
    }

    public void onTextmojiClick() {
        if (!isHandheldIme()) {
            return;
        }
        dismissMiniKeyboard();
        if (mTextmojiKeyboard == null) {
            mTextmojiKeyboard = new Keyboard(mContext, R.xml.handheld_textmoji);
            fillHandheldRows(mTextmojiKeyboard, mTextmojiKeyboard.getMinWidth());
        }
        setHandheldActionRailVisible(true);
        mMainKeyboardView.setKeyboard(mTextmojiKeyboard);
        mHandheldKeyboardMode = HANDHELD_MODE_TEXTMOJI;
    }

    /** Opens the compact settings deck that mirrors the handheld settings home. */
    public void onEditClick() {
        if (!isHandheldIme()) {
            return;
        }
        dismissMiniKeyboard();
        if (mHandheldKeyboardMode == HANDHELD_MODE_EDIT) {
            setHandheldActionRailVisible(true);
            mMainKeyboardView.setKeyboard(mInitialMainKeyboard);
            mHandheldKeyboardMode = HANDHELD_MODE_ALPHA;
            return;
        }
        if (mEditKeyboard == null) {
            mEditKeyboard = new Keyboard(mContext, R.xml.handheld_edit);
            // The command deck has fewer keys per row than the alpha keyboard.
            // Give it a wider source grid so the handheld view reaches its full
            // landscape width before its safety scale limit is applied.
            int renderedWidth = Math.max(mRootView.getWidth(),
                    mContext.getResources().getDisplayMetrics().widthPixels);
            int fullWidthSource = Math.round(renderedWidth * 0.97f / 2.9f);
            fillHandheldRows(mEditKeyboard,
                    Math.max(mEditKeyboard.getMinWidth(), fullWidthSource));
        }
        setHandheldActionRailVisible(false);
        mMainKeyboardView.setKeyboard(mEditKeyboard);
        mHandheldKeyboardMode = HANDHELD_MODE_EDIT;
        // A predictable entry point makes the deck discoverable and avoids
        // carrying a letter-key focus index into this eight-card layout.
        offsetRect(mRect, mMainKeyboardView);
        moveFocusToIndex(0, KeyFocus.TYPE_MAIN);
    }

    private boolean isHandheldIme() {
        return HANDHELD_PACKAGE.equals(mContext.getPackageName());
    }

    private void fillHandheldRows(Keyboard keyboard, int targetWidth) {
        Map<Integer, int[]> rowBounds = new HashMap<>();
        for (Key key : keyboard.getKeys()) {
            int[] bounds = rowBounds.get(key.y);
            if (bounds == null) {
                bounds = new int[]{key.x, key.x + key.width};
                rowBounds.put(key.y, bounds);
            } else {
                bounds[0] = Math.min(bounds[0], key.x);
                bounds[1] = Math.max(bounds[1], key.x + key.width);
            }
        }

        for (Key key : keyboard.getKeys()) {
            int[] bounds = rowBounds.get(key.y);
            int rowWidth = bounds[1] - bounds[0];
            if (rowWidth <= 0 || rowWidth >= targetWidth) {
                continue;
            }

            float scale = (float) targetWidth / rowWidth;
            key.x = bounds[0] + Math.round((key.x - bounds[0]) * scale);
            key.width = Math.round(key.width * scale);
            key.gap = Math.round(key.gap * scale);
        }
    }

    public void onPeriodEntry() {
        if (mMainKeyboardView.isShifted()) {
            if (!isCapsLockOn() && !mCapCharacters && !mCapWords && !mCapSentences) {
                setShiftState(LeanbackKeyboardView.SHIFT_OFF);
            }
        } else if (isCapsLockOn() || mCapCharacters || mCapWords || mCapSentences) {
            setShiftState(LeanbackKeyboardView.SHIFT_ON);
        }
    }

    public void onShiftClick() {
        byte state;
        if (mMainKeyboardView.isShifted()) {
            state = LeanbackKeyboardView.SHIFT_OFF;
        } else {
            state = LeanbackKeyboardView.SHIFT_ON;
        }

        setShiftState(state);
    }

    public void onShiftDoubleClick(boolean wasCapsLockOn) {
        byte state;
        if (wasCapsLockOn) {
            state = LeanbackKeyboardView.SHIFT_OFF;
        } else {
            state = LeanbackKeyboardView.SHIFT_LOCKED;
        }

        setShiftState(state);
    }

    public void onSpaceEntry() {
        if (mMainKeyboardView.isShifted()) {
            if (!isCapsLockOn() && !mCapCharacters && !mCapWords) {
                setShiftState(LeanbackKeyboardView.SHIFT_OFF);
            }
        } else if (isCapsLockOn() || mCapCharacters || mCapWords) {
            setShiftState(LeanbackKeyboardView.SHIFT_ON);
        }
    }

    public void onStartInput(EditorInfo info) {
        setImeOptions(mContext.getResources(), info);
        mVoiceOn = false;
        mLabel = LeanbackUtils.getEditorLabel(info);
    }

    @SuppressLint("NewApi")
    public void onStartInputView() {
        clearSuggestions();
        LayoutParams params = (LayoutParams) mKeyboardsContainer.getLayoutParams();
        if (mSuggestionsEnabled) {
            params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            mSuggestionsContainer.setVisibility(View.VISIBLE);
            mSuggestionsBg.setVisibility(View.VISIBLE);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mSuggestionsContainer.setVisibility(View.GONE);
            mSuggestionsBg.setVisibility(View.GONE);
        }

        mKeyboardsContainer.setLayoutParams(params);
        setHandheldActionRailVisible(true);
        mMainKeyboardView.setKeyboard(mInitialMainKeyboard);
        mHandheldKeyboardMode = HANDHELD_MODE_ALPHA;
        mVoiceButtonView.setMicEnabled(mVoiceEnabled);
        resetVoice();
        dismissMiniKeyboard();
        if (!TextUtils.isEmpty(mEnterKeyText)) {
            mActionButtonView.setText(mEnterKeyText);
            mActionButtonView.setContentDescription(mEnterKeyText);
        } else {
            mActionButtonView.setText(mEnterKeyTextResId);
            mActionButtonView.setContentDescription(mContext.getString(mEnterKeyTextResId));
        }
        sizeHandheldActionKey();

        if (mCapCharacters) {
            setShiftState(LeanbackKeyboardView.SHIFT_LOCKED);
        } else if (!mCapSentences && !mCapWords) {
            setShiftState(LeanbackKeyboardView.SHIFT_OFF);
        } else {
            setShiftState(LeanbackKeyboardView.SHIFT_ON);
        }
    }

    /** Sizes the compact Go key at the base of the handheld action rail. */
    private void sizeHandheldActionKey() {
        if (!"com.handheldkeyboard.ime".equals(mContext.getPackageName())) {
            return;
        }

        Key rightArrow = null;
        for (int i = 0; i < mMainKeyboardView.getKeyCount(); i++) {
            Key key = mMainKeyboardView.getKey(i);
            if (key != null && key.codes != null && key.codes.length > 0 &&
                    key.codes[0] == LeanbackKeyboardView.KEYCODE_RIGHT) {
                rightArrow = key;
                break;
            }
        }
        if (rightArrow == null) {
            return;
        }

        ViewGroup.LayoutParams params = mActionButtonView.getLayoutParams();
        if (!(params instanceof MarginLayoutParams)) {
            return;
        }

        MarginLayoutParams actionParams = (MarginLayoutParams) params;
        Resources resources = mContext.getResources();
        int actionTextSize = Math.round(mMainKeyboardView.mKeyTextSize * 0.64f);
        int minTextSize = Math.round(14 * resources.getDisplayMetrics().scaledDensity);
        int maxTextSize = Math.round(28 * resources.getDisplayMetrics().scaledDensity);
        actionTextSize = Math.max(minTextSize, Math.min(maxTextSize, actionTextSize));
        mActionButtonView.setSingleLine(true);
        mActionButtonView.setEllipsize(TextUtils.TruncateAt.END);
        mActionButtonView.setTextSize(TypedValue.COMPLEX_UNIT_PX, actionTextSize);

        View actionParent = mActionButtonView.getParent() instanceof View
                ? (View) mActionButtonView.getParent() : null;
        int availableWidth = actionParent == null ? 0 : actionParent.getWidth();
        int horizontalPadding = mActionButtonView.getPaddingLeft() + mActionButtonView.getPaddingRight();
        String actionLabel = mActionButtonView.getText().toString().toUpperCase(Locale.ROOT);
        float actionLabelWidth = mActionButtonView.getPaint().measureText(actionLabel);

        // Search and custom action labels can be considerably wider than a cursor key.
        // Shrink only when their pane has a concrete width limit; otherwise preserve a
        // large, readable label and let the next layout pass apply the exact width.
        if (availableWidth > horizontalPadding && actionLabelWidth + horizontalPadding > availableWidth) {
            float fitFactor = (availableWidth - horizontalPadding) / actionLabelWidth;
            int fittedTextSize = Math.max(minTextSize, Math.round(actionTextSize * fitFactor));
            if (fittedTextSize < actionTextSize) {
                actionTextSize = fittedTextSize;
                mActionButtonView.setTextSize(TypedValue.COMPLEX_UNIT_PX, actionTextSize);
            }
        }

        // The parent is exactly the lane to the right of the keyboard grid.
        // Go uses only one keycap; Home, End and Paste use the space above it.
        actionParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        actionParams.height = rightArrow.height;
        mActionButtonView.setLayoutParams(actionParams);
    }

    public void onTextEntry() {
        if (mMainKeyboardView.isShifted()) {
            if (!isCapsLockOn() && !mCapCharacters) {
                setShiftState(LeanbackKeyboardView.SHIFT_OFF);
            }
        } else if (isCapsLockOn() || mCapCharacters) {
            setShiftState(LeanbackKeyboardView.SHIFT_LOCKED);
        }

        if (dismissMiniKeyboard()) {
            moveFocusToIndex(mMiniKbKeyIndex, KeyFocus.TYPE_MAIN);
        }

    }

    public void onVoiceClick() {
        if (mVoiceButtonView != null) {
            mVoiceButtonView.onClick();
        }

    }

    public void resetFocusCursor() {
        offsetRect(mRect, mMainKeyboardView);
        mX = (float) ((double) mRect.left + (double) mRect.width() * 0.45D);
        mY = (float) ((double) mRect.top + (double) mRect.height() * 0.375D);
        getBestFocus(mX, mY, mTempKeyInfo);
        setKbFocus(mTempKeyInfo, true, false);
        setTouchStateInternal(0);
        mSelectorAnimator.reverse();
        mSelectorAnimator.end();
    }

    public void resetVoice() {
        mMainKeyboardView.setAlpha(mAlphaIn);
        mActionButtonView.setAlpha(mAlphaIn);
        mVoiceButtonView.setAlpha(mAlphaOut);
        mMainKeyboardView.setVisibility(View.VISIBLE);
        mActionButtonView.setVisibility(View.VISIBLE);
        mVoiceButtonView.setVisibility(View.INVISIBLE);
    }

    public void setDismissListener(DismissListener listener) {
        mDismissListener = listener;
    }

    public void setFocus(KeyFocus focus) {
        setKbFocus(focus, false, true);
    }

    public void setFocus(KeyFocus focus, final boolean animate) {
        setKbFocus(focus, false, animate);
    }

    /**
     * NOTE: Draw selection over the focused key.<br/>
     * Show selection animation when moving from one button to another.
     */
    public void setSelectorToFocus(Rect rect, boolean overestimateWidth, boolean overestimateHeight, boolean animate) {
        if (mSelector.getWidth() != 0 && mSelector.getHeight() != 0 && rect.width() != 0 && rect.height() != 0) {
            final float width = (float) rect.width();
            final float height = (float) rect.height();
            float heightOver = height;
            if (overestimateHeight) {
                heightOver = height * mOverestimate;
            }

            float widthOver = width;
            if (overestimateWidth) {
                widthOver = width * mOverestimate;
            }

            float deltaY = heightOver;
            float deltaX = widthOver;
            float maxDelta = Math.max(widthOver, heightOver);
            float minDelta = Math.min(widthOver, heightOver);
            if ((double) (maxDelta / minDelta) < 1.1D) {
                deltaY = maxDelta;
                deltaX = maxDelta;
            }

            final float x = rect.exactCenterX() - deltaX / 2.0F;
            final float y = rect.exactCenterY() - deltaY / 2.0F;
            mSelectorAnimation.cancel();

            // The focus drawable is swapped for square and stretched keys. Tint
            // both cached variants on every move so the selected theme's accent
            // remains visible after that swap (especially for Xbox green).
            tintSelectorDrawables();

            // Fix 9-patch stretching for square keys (especially on large keyboard).
            if (Math.abs(deltaX - deltaY) < 1) { // is square
                mKeySelector.setBackground(mKeySelectorSquare);
            } else {
                mKeySelector.setBackground(mKeySelectorStretched);
            }

            if (animate) {
                mSelectorAnimation.reset();
                mSelectorAnimation.setAnimationBounds(x, y, deltaX, deltaY);
                mSelector.startAnimation(mSelectorAnimation);
            } else {
                mSelectorAnimation.setValues(x, y, deltaX, deltaY);
            }
        }
    }

    /**
     * Set touch state
     * @param state state e.g. {@link LeanbackKeyboardContainer#TOUCH_STATE_CLICK LeanbackKeyboardContainer.TOUCH_STATE_CLICK}
     */
    public void setTouchState(final int state) {
        switch (state) {
            case TOUCH_STATE_NO_TOUCH:
                if (mTouchState == TOUCH_STATE_TOUCH_MOVE || mTouchState == TOUCH_STATE_CLICK) {
                    mSelectorAnimator.reverse();
                }
                break;
            case TOUCH_STATE_TOUCH_SNAP:
                if (mTouchState == TOUCH_STATE_CLICK) {
                    mSelectorAnimator.reverse();
                } else if (mTouchState == TOUCH_STATE_TOUCH_MOVE) {
                    mSelectorAnimator.reverse();
                }
                break;
            case TOUCH_STATE_TOUCH_MOVE:
                if (mTouchState == TOUCH_STATE_NO_TOUCH || mTouchState == TOUCH_STATE_TOUCH_SNAP) {
                    mSelectorAnimator.start();
                }
                break;
            case TOUCH_STATE_CLICK:
                if (mTouchState == TOUCH_STATE_NO_TOUCH || mTouchState == TOUCH_STATE_TOUCH_SNAP) {
                    mSelectorAnimator.start();
                }
        }

        setTouchStateInternal(state);
        setKbFocus(mCurrKeyInfo, true, true);
    }

    public void setVoiceListener(VoiceListener listener) {
        mVoiceListener = listener;
    }

    public void startVoiceRecording() {
        if (mVoiceEnabled) {
            if (!mVoiceKeyDismissesEnabled) {
                mVoiceAnimator.startEnterAnimation();
            } else {
                mDismissListener.onDismiss(true);
            }
        }
    }

    /**
     * Switch to next keyboard (looped).
     * {@link KeyboardManager KeyboardManager} is the source behind all keyboard implementations
     */
    public void switchToNextKeyboard() {
        KeyboardData nextKeyboard = mKeyboardManager.next();
        Keyboard currentKeyboard = mMainKeyboardView.getKeyboard();

        if (currentKeyboard != null &&
                currentKeyboard.equals(nextKeyboard.abcKeyboard)) { // one keyboard in the list
            // Prompt user to select layout.
            Helpers.startActivity(mContext, KbLayoutActivity.class);
            mContext.hideIme();
        } else {
            mInitialMainKeyboard = nextKeyboard.abcKeyboard;
            mAbcKeyboard = nextKeyboard.abcKeyboard;
            setHandheldActionRailVisible(true);
            mMainKeyboardView.setKeyboard(nextKeyboard.abcKeyboard);
            mHandheldKeyboardMode = HANDHELD_MODE_ALPHA;

            mSymKeyboard = nextKeyboard.symKeyboard;
            mNumKeyboard = nextKeyboard.numKeyboard;
        }
    }

    public void updateAddonKeyboard() {
        mKeyboardManager.load(); // force reload to fix such errors as invisible kbd
        KeyboardData keyboard = mKeyboardManager.get();
        mInitialMainKeyboard = keyboard.abcKeyboard;
        mAbcKeyboard = keyboard.abcKeyboard;
        setHandheldActionRailVisible(true);
        mMainKeyboardView.setKeyboard(keyboard.abcKeyboard);
        mHandheldKeyboardMode = HANDHELD_MODE_ALPHA;

        mSymKeyboard = keyboard.symKeyboard;
        mNumKeyboard = keyboard.numKeyboard;

        mThemeManager.updateKeyboardTheme();
        tintSelectorDrawables();
    }

    private void tintSelectorDrawables() {
        if (mThemeManager == null) {
            return;
        }
        int color = mThemeManager.getFocusColor();
        if (mKeySelector != null) {
            tintSelectorDrawable(mKeySelector.getBackground(), color);
        }
        tintSelectorDrawable(mKeySelectorSquare, color);
        tintSelectorDrawable(mKeySelectorStretched, color);
    }

    private void tintSelectorDrawable(Drawable drawable, int color) {
        if (drawable != null) {
            drawable.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    public void updateSuggestions(ArrayList<String> suggestions) {
        addUserInputToSuggestions(suggestions);

        int oldCount = mSuggestions.getChildCount();
        int newCount = suggestions.size();
        if (newCount < oldCount) {
            mSuggestions.removeViews(newCount, oldCount - newCount);
        } else if (newCount > oldCount) {
            while (oldCount < newCount) {
                View suggestion = mContext.getLayoutInflater().inflate(R.layout.candidate, null);
                mSuggestions.addView(suggestion);
                ++oldCount;
            }
        }

        for (oldCount = 0; oldCount < newCount; ++oldCount) {
            Button suggestion = mSuggestions.getChildAt(oldCount).findViewById(R.id.text);
            suggestion.setText(suggestions.get(oldCount));
            suggestion.setContentDescription(suggestions.get(oldCount));
        }

        if (getCurrFocus().type == KeyFocus.TYPE_SUGGESTION) {
            resetFocusCursor();
        }

        mThemeManager.updateSuggestionsTheme();
    }

    /**
     * Useful for password fields
     */
    private void addUserInputToSuggestions(ArrayList<String> suggestions) {
        InputConnection connection = mContext.getCurrentInputConnection();

        if (connection != null) {
            String editorText = LeanbackUtils.getEditorText(connection);

            if (editorText.isEmpty()) {
                editorText = mLabel;
            }

            if (suggestions.size() == 0) {
                suggestions.add(editorText);
            } else {
                suggestions.set(0, editorText);
            }
        }
    }

    public void onLangKeyClick() {
        switchToNextKeyboard();
    }

    public void onClipboardClick(InputListener listener) {
        ClipboardManager clipBoard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipBoard != null) {
            ClipData clipData = clipBoard.getPrimaryClip();
            if (clipData != null) {
                ClipData.Item item = clipData.getItemAt(0);
                String text = item.getText().toString();
                if (listener != null) {
                    listener.onEntry(InputListener.ENTRY_TYPE_STRING, LeanbackKeyboardView.NOT_A_KEY, text);
                }
            }
        }
    }

    public interface DismissListener {
        void onDismiss(boolean fromVoice);
    }

    public static class KeyFocus {
        public static final int TYPE_ACTION = 2;
        public static final int TYPE_INVALID = -1;
        public static final int TYPE_MAIN = 0;
        public static final int TYPE_SUGGESTION = 3;
        public static final int TYPE_VOICE = 1;
        int code;
        int index;
        CharSequence label;
        final Rect rect = new Rect();
        int type = TYPE_INVALID;

        @Override
        public boolean equals(Object obj) {
            if (this != obj) {
                if (obj == null || this.getClass() != obj.getClass()) {
                    return false;
                }

                KeyFocus focus = (KeyFocus) obj;

                if (this.code != focus.code) {
                    return false;
                }

                if (this.index != focus.index) {
                    return false;
                }

                if (this.type != focus.type) {
                    return false;
                }

                // equality must be commutative
                if (this.label == null && focus.label != null) {
                    return false;
                }

                if (this.label != null && !this.label.equals(focus.label)) {
                    return false;
                }

                if (!this.rect.equals(focus.rect)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            int hash = this.rect.hashCode();
            int index = this.index;
            int type = this.type;
            int code = this.code;
            int salt;
            if (this.label != null) {
                salt = this.label.hashCode();
            } else {
                salt = 0;
            }

            return (((hash * 31 + index) * 31 + type) * 31 + code) * 31 + salt;
        }

        public void set(KeyFocus focus) {
            this.index = focus.index;
            this.type = focus.type;
            this.code = focus.code;
            this.label = focus.label;
            this.rect.set(focus.rect);
        }

        @Override
        public String toString() {
            return "[type: " + this.type + ", index: " + this.index + ", code: " + this.code + ", label: " + this.label + ", rect: " + this.rect + "]";
        }
    }

    private class ScaleAnimation extends Animation {
        private float mEndHeight;
        private float mEndWidth;
        private float mEndX;
        private float mEndY;
        private final ViewGroup.LayoutParams mParams;
        private float mStartHeight;
        private float mStartWidth;
        private float mStartX;
        private float mStartY;
        private final View mView;

        public ScaleAnimation(FrameLayout view) {
            mView = view;
            mParams = view.getLayoutParams();
            setDuration(MOVEMENT_ANIMATION_DURATION);
            setInterpolator(sMovementInterpolator);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {
            if (interpolatedTime == 0.0F) {
                mStartX = mView.getX();
                mStartY = mView.getY();
                mStartWidth = (float) mParams.width;
                mStartHeight = (float) mParams.height;
            } else {
                setValues((mEndX - mStartX) * interpolatedTime + mStartX,
                            (mEndY - mStartY) * interpolatedTime + mStartY,
                            (float) ((int) ((mEndWidth - mStartWidth) * interpolatedTime + mStartWidth)),
                            (float) ((int) ((mEndHeight - mStartHeight) * interpolatedTime + mStartHeight)));
            }
        }

        public void setAnimationBounds(float x, float y, float width, float height) {
            mEndX = x;
            mEndY = y;
            mEndWidth = width;
            mEndHeight = height;
        }

        public void setValues(float x, float y, float width, float height) {
            mView.setX(x);
            mView.setY(y);
            mParams.width = (int) width;
            mParams.height = (int) height;
            mView.setLayoutParams(mParams);
            mView.requestLayout();
        }
    }

    private class VoiceIntroAnimator {
        private AnimatorListener mEnterListener;
        private AnimatorListener mExitListener;
        private ValueAnimator mValueAnimator;

        public VoiceIntroAnimator(AnimatorListener enterListener, AnimatorListener exitListener) {
            mEnterListener = enterListener;
            mExitListener = exitListener;
            mValueAnimator = ValueAnimator.ofFloat(mAlphaOut, mAlphaIn);
            mValueAnimator.setDuration(mVoiceAnimDur);
            mValueAnimator.setInterpolator(new AccelerateInterpolator());
        }

        private void start(final boolean enterVoice) {
            mValueAnimator.cancel();
            mValueAnimator.removeAllListeners();

            AnimatorListener listener;

            if (enterVoice) {
                listener = mEnterListener;
            } else {
                listener = mExitListener;
            }

            mValueAnimator.addListener(listener);
            mValueAnimator.removeAllUpdateListeners();
            mValueAnimator.addUpdateListener(animation -> {
                float scale = (Float) mValueAnimator.getAnimatedValue();
                float calcOpacity = mAlphaIn + mAlphaOut - scale;
                float opacity;
                if (enterVoice) {
                    opacity = calcOpacity;
                } else {
                    opacity = scale;
                }

                if (enterVoice) {
                    calcOpacity = scale;
                }

                mMainKeyboardView.setAlpha(opacity);
                mActionButtonView.setAlpha(opacity);
                mVoiceButtonView.setAlpha(calcOpacity);
                if (scale == mAlphaOut) {
                    if (!enterVoice) {
                        mMainKeyboardView.setVisibility(View.VISIBLE);
                        mActionButtonView.setVisibility(View.VISIBLE);
                        return;
                    }

                    mVoiceButtonView.setVisibility(View.VISIBLE);
                } else if (scale == mAlphaIn) {
                    if (enterVoice) {
                        mMainKeyboardView.setVisibility(View.INVISIBLE);
                        mActionButtonView.setVisibility(View.INVISIBLE);
                        return;
                    }

                    mVoiceButtonView.setVisibility(View.INVISIBLE);
                }
            });
            mValueAnimator.start();
        }

        void startEnterAnimation() {
            if (!isVoiceVisible() && !mValueAnimator.isRunning()) {
                start(true);
            }
        }

        void startExitAnimation() {
            if (isVoiceVisible() && !mValueAnimator.isRunning()) {
                start(false);
            }
        }
    }

    public interface VoiceListener {
        void onVoiceResult(String result);
    }

    private class MyVoiceRecognitionListener implements RecognitionListener {
        float peakRmsLevel = 0.0F;
        int rmsCounter = 0;

        @Override
        public void onBeginningOfSpeech() {
            mVoiceButtonView.showRecording();
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            // NOP
        }

        @Override
        public void onEndOfSpeech() {
            mVoiceButtonView.showRecognizing();
            mVoiceOn = false;
        }

        @Override
        public void onError(int error) {
            cancelVoiceRecording();

            String errorMsg;

            switch (error) {
                case SpeechRecognizer.ERROR_SERVER:
                    errorMsg = "recognizer error server error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMsg = "recognizer error client error";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMsg = "recognizer error speech timeout";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMsg = "recognizer error no match";
                    break;
                default:
                    errorMsg = "recognizer other error " + error;
            }

            MessageHelpers.showLongMessage(mContext, errorMsg);

            Log.d(TAG, errorMsg);
        }

        @Override
        public void onEvent(int eventType, Bundle bundle) {
            // NOP
        }

        @Override
        public void onPartialResults(Bundle bundle) {
            // NOP
        }

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            mVoiceButtonView.showListening();
        }

        @Override
        public void onResults(Bundle bundle) {
            List<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            if (results != null && mVoiceListener != null) {
                mVoiceListener.onVoiceResult(results.get(0));
            }

            cancelVoiceRecording();
        }

        // TODO: not fully decompiled and may contains bugs
        @Override
        public void onRmsChanged(float rmsdB) {
            synchronized (this) {
                mVoiceOn = true;

                int speechLevel = 0;

                if (rmsdB >= 0) {
                    speechLevel = (int) (rmsdB * 10f);
                }

                mSpeechLevelSource.setSpeechLevel(speechLevel);

                peakRmsLevel = Math.max(peakRmsLevel, rmsdB);
                rmsCounter++;

                if (rmsCounter <= 100) {
                    return;
                }

                if (peakRmsLevel < 0) {
                    return;
                }

                mVoiceButtonView.showNotListening();
            }
        }
    }
}
