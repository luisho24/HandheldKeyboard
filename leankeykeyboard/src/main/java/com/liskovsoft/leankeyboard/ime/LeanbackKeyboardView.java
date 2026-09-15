package com.liskovsoft.leankeyboard.ime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.InputDevice;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import com.liskovsoft.leankeykeyboard.R;
import com.liskovsoft.leankeyboard.utils.TextDrawable;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class LeanbackKeyboardView extends FrameLayout {
    private static final String TAG = "LbKbView";
    /**
     * Space key index (important: wrong value will broke navigation)
     */
    public static final int ASCII_PERIOD = 47;
    /**
     * Keys count among which space key spans (important: wrong value will broke navigation)
     */
    public static final int ASCII_PERIOD_LEN = 5;
    public static final int ASCII_SPACE = 32;
    private static final boolean DEBUG = false;
    public static final int KEYCODE_CAPS_LOCK = -6;
    public static final int KEYCODE_DELETE = -5;
    public static final int KEYCODE_DISMISS_MINI_KEYBOARD = -8;
    public static final int KEYCODE_LEFT = -3;
    public static final int KEYCODE_RIGHT = -4;
    public static final int KEYCODE_UP = -14;
    public static final int KEYCODE_DOWN = -15;
    public static final int KEYCODE_EDIT_TOGGLE = -16;
    public static final int KEYCODE_HOME = -17;
    public static final int KEYCODE_END = -18;
    public static final int KEYCODE_SELECT_LEFT = -19;
    public static final int KEYCODE_SELECT_RIGHT = -20;
    public static final int KEYCODE_SELECT_UP = -21;
    public static final int KEYCODE_SELECT_DOWN = -22;
    public static final int KEYCODE_UNDO = -23;
    public static final int KEYCODE_REDO = -24;
    public static final int KEYCODE_TAB = -25;
    public static final int KEYCODE_ESCAPE = -26;
    public static final int KEYCODE_SELECT_ALL = -27;
    public static final int KEYCODE_QUICK_THEME = -28;
    public static final int KEYCODE_QUICK_LAYOUT = -29;
    public static final int KEYCODE_QUICK_SOUND = -30;
    public static final int KEYCODE_QUICK_MORE = -31;
    public static final int KEYCODE_QUICK_ABOUT = -32;
    public static final int KEYCODE_QUICK_SETUP = -33;
    public static final int KEYCODE_SHIFT = -1;
    public static final int KEYCODE_SYM_TOGGLE = -2;
    public static final int KEYCODE_VOICE = -7;
    public static final int KEYCODE_LANG_TOGGLE = -9;
    public static final int KEYCODE_CLIPBOARD = -10;
    public static final int KEYCODE_SETTINGS = -11;
    public static final int KEYCODE_EMOJI_TOGGLE = -12;
    public static final int KEYCODE_TEXTMOJI_TOGGLE = -13;
    public static final int KEYCODE_CUSTOM_TEXT_BASE = 100000;
    public static final int NOT_A_KEY = -1;
    public static final int SHIFT_LOCKED = 2;
    public static final int SHIFT_OFF = 0;
    public static final int SHIFT_ON = 1;
    private int mBaseMiniKbIndex = -1;
    private final int mClickAnimDur;
    private final float mClickedScale;
    private final float mSquareIconScaleFactor;
    private int mColCount;
    private View mCurrentFocusView;
    private boolean mFocusClicked;
    private int mFocusIndex;
    private final float mFocusedScale;
    private final int mInactiveMiniKbAlpha;
    private ImageView[] mKeyImageViews;
    private int mKeyTextColor;
    private int mKeyBackgroundColor = 0x403A4148;
    private float mKeyCornerRadiusFraction = 0.22f;
    private boolean mKeyGlassEffect;
    private boolean mKeyLiquidEffect;
    private Keyboard mKeyboard;
    private KeyHolder[] mKeys;
    private boolean mMiniKeyboardOnScreen;
    private Rect mPadding;
    private int mRowCount;
    private int mShiftState;
    private final int mUnfocusStartDelay;
    private final KeyConverter mConverter;
    protected Paint mPaint;
    private final Paint mHintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHintBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKeyBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKeyGlassHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKeyGlassStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected int mKeyTextSize;
    protected int mModeChangeTextSize;
    protected float mKeyboardScaleFactor = 1.0f;
    private Drawable mCustomCapsLockDrawable;
    private int mControllerFamily = -1;

    private static final class ControllerHint {
        final int iconResId;
        final String fallbackLabel;

        ControllerHint(int iconResId, String fallbackLabel) {
            this.iconResId = iconResId;
            this.fallbackLabel = fallbackLabel;
        }
    }

    private static class KeyConverter {
        private static final int LOWER_CASE = 0;
        private static final int UPPER_CASE = 1;

        private void init(KeyHolder keyHolder) {
            // store original label
            // in case when two characters are stored in one label (e.g. "A|B")
            if (keyHolder.key.text == null) {
                keyHolder.key.text = keyHolder.key.label;
            }
        }

        public void toLowerCase(KeyHolder keyHolder) {
            extractChar(LOWER_CASE, keyHolder);
        }

        public void toUpperCase(KeyHolder keyHolder) {
            extractChar(UPPER_CASE, keyHolder);
        }

        private void extractChar(int charCase, KeyHolder keyHolder) {
            init(keyHolder);

            CharSequence result = null;
            CharSequence label = keyHolder.key.text;

            String[] labels = splitLabels(label);

            switch (charCase) {
                case LOWER_CASE:
                    result = labels != null ? labels[0] : label.toString().toLowerCase(Locale.ROOT);
                    break;
                case UPPER_CASE:
                    result = labels != null ? labels[1] : label.toString().toUpperCase(Locale.ROOT);
                    break;
            }

            keyHolder.key.label = result;
        }

        private String[] splitLabels(CharSequence label) {
            String realLabel = label.toString();

            String[] labels = realLabel.split("\\|");

            return labels.length == 2 ? labels : null; // remember, we encoding two chars
        }
    }

    public LeanbackKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        TypedArray styledAttrs = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LeanbackKeyboardView, 0, 0);
        mRowCount = styledAttrs.getInteger(R.styleable.LeanbackKeyboardView_rowCount, -1);
        mColCount = styledAttrs.getInteger(R.styleable.LeanbackKeyboardView_columnCount, -1);
        mKeyTextSize = (int) res.getDimension(R.dimen.key_font_size);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mKeyTextSize);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);
        mPadding = new Rect(0, 0, 0, 0);
        mModeChangeTextSize = (int) res.getDimension(R.dimen.function_key_mode_change_font_size);
        mKeyTextColor = ContextCompat.getColor(getContext(), R.color.key_text_default);
        mFocusIndex = -1;
        mShiftState = 0;
        mFocusedScale = res.getFraction(R.fraction.focused_scale, 1, 1);
        mClickedScale = res.getFraction(R.fraction.clicked_scale, 1, 1);
        mSquareIconScaleFactor = res.getFraction(R.fraction.square_icon_scale_factor, 1, 1);
        mClickAnimDur = res.getInteger(R.integer.clicked_anim_duration);
        mUnfocusStartDelay = res.getInteger(R.integer.unfocused_anim_delay);
        mInactiveMiniKbAlpha = res.getInteger(R.integer.inactive_mini_kb_alpha);
        mConverter = new KeyConverter();
    }

    private void adjustCase(KeyHolder keyHolder) {
        if (keyHolder.key.codes != null && keyHolder.key.codes.length > 0 &&
                keyHolder.key.codes[0] >= KEYCODE_CUSTOM_TEXT_BASE) {
            mConverter.init(keyHolder);
            keyHolder.key.label = keyHolder.key.text;
            return;
        }

        boolean flag = keyHolder.isInMiniKb && keyHolder.isInvertible;

        // ^ equals to !=
        if (mKeyboard.isShifted() ^ flag) {
            mConverter.toUpperCase(keyHolder);
        } else {
            mConverter.toLowerCase(keyHolder);
        }
    }

    /**
     * NOTE: Adds key views to root window
     */
    @SuppressLint("NewApi")
    private ImageView createKeyImageView(final int keyIndex) {
        Rect padding = mPadding;
        int kbdPaddingLeft = getPaddingLeft();
        int kbdPaddingTop = getPaddingTop();
        KeyHolder keyHolder = mKeys[keyIndex];
        Key key = keyHolder.key;
        adjustCase(keyHolder);
        String label;
        if (key.label == null) {
            label = null;
        } else {
            label = key.label.toString();
        }

        Bitmap bitmap = Bitmap.createBitmap(key.width, key.height, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = mPaint;
        paint.setColor(mKeyTextColor);
        canvas.drawARGB(0, 0, 0, 0);
        float keyInset = Math.max(1.0f, Math.min(key.width, key.height) * 0.045f);
        RectF keyShape = new RectF(keyInset, keyInset, key.width - keyInset, key.height - keyInset);
        mKeyBackgroundPaint.setColor(mKeyBackgroundColor);
        float keyRadius = Math.min(key.width, key.height) * mKeyCornerRadiusFraction;
        canvas.drawRoundRect(keyShape, keyRadius, keyRadius, mKeyBackgroundPaint);
        if (mKeyGlassEffect) {
            // Android exposes backdrop blur to an IME at window scope only. A
            // key-bounded treatment avoids blurring the full editor behind it.
            float glossHeight = keyShape.height() * (mKeyLiquidEffect ? 0.62f : 0.48f);
            int topAlpha = mKeyLiquidEffect ? 44 : 30;
            mKeyGlassHighlightPaint.setStyle(Paint.Style.FILL);
            mKeyGlassHighlightPaint.setShader(new LinearGradient(
                    0f, keyShape.top, 0f, keyShape.top + glossHeight,
                    new int[]{Color.argb(topAlpha, 255, 255, 255), Color.argb(0, 255, 255, 255)},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(keyShape, keyRadius, keyRadius, mKeyGlassHighlightPaint);
            mKeyGlassHighlightPaint.setShader(null);

            mKeyGlassStrokePaint.setStyle(Paint.Style.STROKE);
            mKeyGlassStrokePaint.setStrokeWidth(Math.max(1f, Math.min(key.width, key.height) * 0.014f));
            mKeyGlassStrokePaint.setColor(Color.argb(mKeyLiquidEffect ? 88 : 62, 255, 255, 255));
            canvas.drawRoundRect(keyShape, keyRadius, keyRadius, mKeyGlassStrokePaint);
        }

        if (key.codes != null && key.codes.length > 0 && key.codes[0] == ASCII_SPACE
                && isHandheldKeyboard()) {
            drawHandheldSpacebar(canvas, key, keyShape, paint, label);
        } else if (key.icon != null) {
            if (key.codes[0] == NOT_A_KEY) {
                switch (mShiftState) {
                    case SHIFT_OFF:
                        key.icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_ime_shift_off);
                        break;
                    case SHIFT_ON:
                        key.icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_ime_shift_on);
                        break;
                    case SHIFT_LOCKED:
                        if (mCustomCapsLockDrawable != null) {
                            key.icon = mCustomCapsLockDrawable;
                        } else {
                            key.icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_ime_shift_lock_on);
                        }
                }
            }

            // NOTE: Fix non proper scale of space key on low dpi

            int iconWidth = key.width; // originally used key.icon.getIntrinsicWidth();
            int iconHeight = key.height; // originally used key.icon.getIntrinsicHeight();

            if (key.width == key.height) { // square key proper fit
                int newSize = Math.round(key.width * mSquareIconScaleFactor);
                iconWidth = newSize;
                iconHeight = newSize;
            }

            // Most command icons are authored on a square canvas. The
            // handheld layout can make their keys rectangular after fitting
            // the keyboard to the display. Stretching that 72x72 artwork to
            // fill the key made the globe and settings glyphs look wide.
            // Keep their original aspect ratio and size them from the key's
            // shortest side instead.
            int intrinsicWidth = key.icon.getIntrinsicWidth();
            int intrinsicHeight = key.icon.getIntrinsicHeight();
            boolean squareCommandIcon = key.codes[0] != ASCII_SPACE
                    && intrinsicWidth > 0
                    && intrinsicHeight > 0
                    && Math.abs(intrinsicWidth - intrinsicHeight) <= 1;
            if (squareCommandIcon) {
                int newSize = Math.round(Math.min(key.width, key.height)
                        * mSquareIconScaleFactor);
                iconWidth = newSize;
                iconHeight = newSize;
            }

            if (key.codes[0] == ASCII_SPACE && mKeyboardScaleFactor > 1.0f) {
                // space fix for large interface
                float gap = getResources().getDimension(R.dimen.keyboard_horizontal_gap);
                float gapDelta = (gap * mKeyboardScaleFactor) - gap;
                iconWidth -= gapDelta * (ASCII_PERIOD_LEN - 1);
            }

            int dx = (key.width - padding.left - padding.right - iconWidth) / 2 + padding.left;
            int dy = (key.height - padding.top - padding.bottom - iconHeight) / 2 + padding.top;

            canvas.translate((float) dx, (float) dy);
            key.icon.setBounds(0, 0, iconWidth, iconHeight);
            if (key.icon instanceof TextDrawable) {
                // The localised Space label is drawn by TextDrawable rather
                // than the icon bitmap, so a Drawable color filter alone does
                // not change its text color.
                ((TextDrawable) key.icon).setTextColor(mKeyTextColor);
            }
            // Function glyphs used to be authored in white. Tint them with the
            // active label color so Space, Shift, language and cursor controls
            // remain readable on a light palette as well as a dark one.
            key.icon.mutate().setColorFilter(mKeyTextColor, PorterDuff.Mode.SRC_IN);
            key.icon.draw(canvas);
            key.icon.setColorFilter(null);
            if (key.codes[0] == ASCII_SPACE && key.icon instanceof TextDrawable) {
                // Draw the language name ourselves after the legacy space-bar
                // artwork. StaticLayout keeps its own white paint on some
                // Android versions, so this guarantees the light variant has
                // a readable language label.
                TextDrawable spaceDrawable = (TextDrawable) key.icon;
                paint.setColor(mKeyTextColor);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                paint.setTextSize(Math.max(12f, iconHeight * 0.28f));
                float baseline = iconHeight / 2f - (paint.ascent() + paint.descent()) / 2f;
                canvas.drawText(spaceDrawable.getText().toString(), iconWidth / 2f, baseline, paint);
            }
            canvas.translate((float) (-dx), (float) (-dy));
        } else if (label != null) {
            if (label.length() > 1) {
                paint.setTextSize((float) mModeChangeTextSize);
                paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            } else {
                paint.setTextSize((float) mKeyTextSize);
                paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            }

            float availableTextWidth = key.width - padding.left - padding.right - keyInset * 2;
            float measuredTextWidth = paint.measureText(label);
            if (measuredTextWidth > availableTextWidth && measuredTextWidth > 0) {
                paint.setTextSize(paint.getTextSize() * availableTextWidth / measuredTextWidth);
            }

            canvas.drawText(
                    label,
                    (float) ((key.width - padding.left - padding.right) / 2 + padding.left),
                    (float) ((key.height - padding.top - padding.bottom) / 2) + (paint.getTextSize() - paint.descent()) / 2.0F + (float) padding.top,
                    paint
            );
            paint.setShadowLayer(0.0F, 0.0F, 0.0F, 0);
        }

        drawControllerHint(canvas, key);

        ImageView image = new ImageView(getContext());
        image.setImageBitmap(bitmap);
        image.setContentDescription(label);
        // Adds key views to root window
        addView(image, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        // Set position manually for each key
        image.setX((float) (key.x + kbdPaddingLeft));
        image.setY((float) (key.y + kbdPaddingTop));
        int opacity;
        if (mMiniKeyboardOnScreen && !keyHolder.isInMiniKb) {
            opacity = mInactiveMiniKbAlpha;
        } else {
            opacity = 255;
        }

        image.setImageAlpha(opacity);
        image.setVisibility(View.VISIBLE);

        return image;
    }

    /** Draws a calm language control instead of stretching the legacy space artwork. */
    private void drawHandheldSpacebar(Canvas canvas, Key key, RectF keyShape, Paint paint,
                                      String fallbackLabel) {
        String language = fallbackLabel;
        if (key.icon instanceof TextDrawable) {
            CharSequence localized = ((TextDrawable) key.icon).getText();
            if (localized != null && localized.length() > 0) {
                language = localized.toString();
            }
        }
        if (language == null || language.length() == 0) {
            language = "Space";
        }

        float centerX = keyShape.centerX();
        float labelSize = Math.max(14f, keyShape.height() * 0.27f);
        paint.setColor(mKeyTextColor);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(labelSize);
        float labelBaseline = keyShape.centerY() - keyShape.height() * 0.10f
                - (paint.ascent() + paint.descent()) / 2f;
        canvas.drawText(language, centerX, labelBaseline, paint);

        // A slim centred rail expresses the wide key's purpose without the
        // old bright inset bar that competed with the language name.
        float maxRailWidth = 140f * getResources().getDisplayMetrics().density;
        float railWidth = Math.min(keyShape.width() * 0.34f, maxRailWidth);
        float railHeight = Math.max(2f, keyShape.height() * 0.045f);
        float railY = keyShape.centerY() + keyShape.height() * 0.24f;
        mKeyGlassHighlightPaint.setShader(null);
        mKeyGlassHighlightPaint.setStyle(Paint.Style.FILL);
        mKeyGlassHighlightPaint.setColor(Color.argb(92, Color.red(mKeyTextColor),
                Color.green(mKeyTextColor), Color.blue(mKeyTextColor)));
        RectF rail = new RectF(centerX - railWidth / 2f, railY - railHeight / 2f,
                centerX + railWidth / 2f, railY + railHeight / 2f);
        canvas.drawRoundRect(rail, railHeight, railHeight, mKeyGlassHighlightPaint);

        float notchWidth = Math.max(12f, railHeight * 5f);
        mKeyGlassHighlightPaint.setColor(mKeyTextColor);
        RectF notch = new RectF(centerX - notchWidth / 2f, railY - railHeight / 2f,
                centerX + notchWidth / 2f, railY + railHeight / 2f);
        canvas.drawRoundRect(notch, railHeight, railHeight, mKeyGlassHighlightPaint);
    }

    private boolean isHandheldKeyboard() {
        return "com.handheldkeyboard.ime".equals(getContext().getPackageName());
    }

    private void drawControllerHint(Canvas canvas, Key key) {
        if (key.codes == null || key.codes.length == 0) {
            return;
        }

        ControllerHint[] hints = getControllerHints(key.codes[0]);
        if (hints == null || hints.length == 0) {
            return;
        }

        float shortSide = Math.min(key.width, key.height);
        // These are instructions, not decoration. The original TV-sized
        // badges became unreadable after the handheld layout condensed the
        // key rows. Give them a usable minimum size on 1080p handhelds while
        // keeping a little separation from the main key glyph.
        float margin = Math.max(3f, shortSide * 0.06f);
        float iconHeight = Math.max(20f, shortSide * (hints.length > 1 ? 0.29f : 0.38f));
        float iconGap = Math.max(3f, iconHeight * 0.12f);
        float textSize = Math.max(13f, shortSide * (hints.length > 1 ? 0.22f : 0.26f));
        mHintPaint.setTextSize(textSize);
        mHintPaint.setTextAlign(Paint.Align.CENTER);
        mHintPaint.setColor(0xFFFFFFFF);
        mHintBackgroundPaint.setColor(0xCC1B2A3B);

        for (int i = 0; i < hints.length; i++) {
            ControllerHint hint = hints[i];
            Drawable icon = hint.iconResId != 0 ? ContextCompat.getDrawable(getContext(), hint.iconResId) : null;
            float badgeHeight = icon != null ? iconHeight : textSize + textSize * 0.32f;
            float badgeWidth;
            if (icon != null && icon.getIntrinsicWidth() > 0 && icon.getIntrinsicHeight() > 0) {
                float aspect = (float) icon.getIntrinsicWidth() / (float) icon.getIntrinsicHeight();
                badgeWidth = badgeHeight * aspect;
                // Keep wide shoulder-button glyphs inside compact keycaps.
                float maxWidth = Math.max(badgeHeight, key.width * 0.48f);
                if (badgeWidth > maxWidth) {
                    badgeWidth = maxWidth;
                    badgeHeight = badgeWidth / aspect;
                }
            } else {
                String label = hint.fallbackLabel == null ? "" : hint.fallbackLabel;
                float horizontalPadding = textSize * 0.34f;
                badgeWidth = mHintPaint.measureText(label) + horizontalPadding * 2;
            }
            float left = key.width - badgeWidth - margin;
            float top = margin + i * (badgeHeight + iconGap);
            RectF badge = new RectF(left, top, left + badgeWidth, top + badgeHeight);

            if (icon != null) {
                icon.setBounds(Math.round(left), Math.round(top), Math.round(left + badgeWidth), Math.round(top + badgeHeight));
                icon.draw(canvas);
            } else {
                canvas.drawRoundRect(badge, badgeHeight * 0.35f, badgeHeight * 0.35f, mHintBackgroundPaint);
                canvas.drawText(hint.fallbackLabel, left + badgeWidth / 2,
                        top + badgeHeight / 2 - (mHintPaint.ascent() + mHintPaint.descent()) / 2, mHintPaint);
            }
        }
    }

    private ControllerHint[] getControllerHints(int keyCode) {
        int family = getControllerFamily();
        switch (keyCode) {
            case KEYCODE_DELETE:
                return new ControllerHint[] {family == 2
                        ? new ControllerHint(R.drawable.controller_ps_square, "□")
                        : family == 3
                        ? new ControllerHint(R.drawable.controller_switch_y, "Y")
                        : new ControllerHint(R.drawable.controller_xb_x, "X")};
            case ASCII_SPACE:
                return new ControllerHint[] {family == 2
                        ? new ControllerHint(R.drawable.controller_ps_triangle, "△")
                        : family == 3
                        ? new ControllerHint(R.drawable.controller_switch_x, "X")
                        : new ControllerHint(R.drawable.controller_xb_y, "Y")};
            case KEYCODE_SHIFT:
                return new ControllerHint[] {
                        family == 2
                                ? new ControllerHint(R.drawable.controller_ps_l1, "L1")
                                : family == 3
                                ? new ControllerHint(R.drawable.controller_switch_l, "L")
                                : new ControllerHint(R.drawable.controller_xb_lb, "LB"),
                        new ControllerHint(R.drawable.controller_generic_r3, family == 2 ? "R3" : "RS")
                };
            case KEYCODE_SYM_TOGGLE:
                return new ControllerHint[] {family == 2
                        ? new ControllerHint(R.drawable.controller_ps_r1, "R1")
                        : family == 3
                        ? new ControllerHint(R.drawable.controller_switch_r, "R")
                        : new ControllerHint(R.drawable.controller_xb_rb, "RB")};
            case KEYCODE_CAPS_LOCK:
                return new ControllerHint[] {
                        new ControllerHint(R.drawable.controller_generic_r3, family == 2 ? "R3" : "RS")
                };
            case KEYCODE_LANG_TOGGLE:
                return new ControllerHint[] {family == 2
                        ? new ControllerHint(R.drawable.controller_ps_super, "SHARE")
                        : family == 3
                        ? new ControllerHint(R.drawable.controller_switch_minus, "−")
                        : new ControllerHint(R.drawable.controller_xb_select, "VIEW")};
            default:
                return null;
        }
    }

    /** 1 = Xbox/generic, 2 = PlayStation, 3 = Nintendo. */
    private int getControllerFamily() {
        if (mControllerFamily != -1) {
            return mControllerFamily;
        }

        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (device == null || (device.getSources() & InputDevice.SOURCE_CLASS_JOYSTICK) != InputDevice.SOURCE_CLASS_JOYSTICK) {
                continue;
            }

            String name = device.getName().toLowerCase(java.util.Locale.ROOT);
            boolean isSony = name.contains("dualshock") || name.contains("dualsense") || name.contains("playstation") || name.contains("sony");
            boolean isNintendo = name.contains("nintendo") || name.contains("joy-con") || name.contains("joycon") || name.contains("switch pro");
            if (Build.VERSION.SDK_INT >= 19) {
                isSony |= device.getVendorId() == 0x054C;
                isNintendo |= device.getVendorId() == 0x057E;
            }

            if (isSony) {
                mControllerFamily = 2;
                return mControllerFamily;
            }
            if (isNintendo) {
                mControllerFamily = 3;
                return mControllerFamily;
            }
        }
        mControllerFamily = 1;
        return mControllerFamily;
    }

    private void createKeyImageViews(KeyHolder[] keys) {
        if (mKeyImageViews != null) {
            ImageView[] images = mKeyImageViews;
            int totalImages = images.length;

            for (int i = 0; i < totalImages; ++i) {
                images[i].animate().cancel();
                resetKeyTransform(images[i]);
                removeView(images[i]);
            }

            mKeyImageViews = null;
        }

        // Image views are recreated when a theme, case, or layout changes.
        // Never retain a focus animation target from the previous set: it may
        // have been removed from this FrameLayout already.
        mCurrentFocusView = null;
        mFocusIndex = -1;
        mFocusClicked = false;

        int totalKeys = keys.length;
        for (int i = 0; i < totalKeys; ++i) {
            if (mKeyImageViews == null) {
                mKeyImageViews = new ImageView[totalKeys];
            } else if (mKeyImageViews[i] != null) {
                removeView(mKeyImageViews[i]);
            }

            mKeyImageViews[i] = createKeyImageView(i);
        }

    }

    private void removeMessages() {
        // TODO: not implemented
        Log.w(TAG, "method 'removeMessages()' not implemented");
    }

    /**
     * NOTE: Keys initialization routine.<br/>
     * Any manipulations with keys should be done here.
     */
    private void setKeys(List<Key> keys) {
        mKeys = new KeyHolder[keys.size()];
        Iterator<Key> iterator = keys.iterator();

        for (int i = 0; i < mKeys.length && iterator.hasNext(); ++i) {
            Key key = iterator.next();
            mKeys[i] = new KeyHolder(key);
        }
    }

    public boolean dismissMiniKeyboard() {
        boolean dismiss = false;
        if (mMiniKeyboardOnScreen) {
            mMiniKeyboardOnScreen = false;
            setKeys(mKeyboard.getKeys());
            invalidateAllKeys();
            dismiss = true;
        }

        return dismiss;
    }

    public int getBaseMiniKbIndex() {
        return mBaseMiniKbIndex;
    }

    public int getColCount() {
        return mColCount;
    }

    public Key getFocusedKey() {
        return mFocusIndex == -1 ? null : mKeys[mFocusIndex].key;
    }

    public Key getKey(int index) {
        return mKeys != null && index >= 0 && index < mKeys.length ? mKeys[index].key : null;
    }

    public int getKeyCount() {
        return mKeys == null ? 0 : mKeys.length;
    }

    public Keyboard getKeyboard() {
        return mKeyboard;
    }

    /**
     * Get index of the key under cursor
     * <br/>
     * Resulted index depends on the space key position
     * @param x x position
     * @param y y position
     * @return index of the key
     */
    public int getNearestIndex(final float x, final float y) {
        if (mKeys == null || mKeys.length == 0) {
            return 0;
        }

        // Use the rendered key bounds instead of dividing the view into a uniform grid.
        // Handheld layouts scale rows and columns independently, and wide keys (especially
        // Space) make grid-based hit testing select the wrong key near row boundaries.
        int nearestIndex = 0;
        float nearestDistance = Float.MAX_VALUE;
        for (int i = 0; i < mKeys.length; i++) {
            Key key = mKeys[i].key;
            float dx = x < key.x ? key.x - x : x > key.x + key.width ? x - (key.x + key.width) : 0f;
            float dy = y < key.y ? key.y - y : y > key.y + key.height ? y - (key.y + key.height) : 0f;
            float distance = dx * dx + dy * dy;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
                if (distance == 0f) {
                    break;
                }
            }
        }

        return nearestIndex;
    }

    public int getRowCount() {
        return mRowCount;
    }

    public int getShiftState() {
        return mShiftState;
    }

    public void invalidateAllKeys() {
        createKeyImageViews(mKeys);
    }

    public void invalidateKey(int keyIndex) {
        if (mKeys != null && keyIndex >= 0 && keyIndex < mKeys.length) {
            if (mKeyImageViews[keyIndex] != null) {
                removeView(mKeyImageViews[keyIndex]);
            }

            mKeyImageViews[keyIndex] = createKeyImageView(keyIndex);
        }
    }

    public boolean isMiniKeyboardOnScreen() {
        return mMiniKeyboardOnScreen;
    }

    public boolean isShifted() {
        return mShiftState == SHIFT_ON || mShiftState == SHIFT_LOCKED;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void onKeyLongPress() {
        int popupResId = mKeys[mFocusIndex].key.popupResId;

        if (popupResId != 0) {
            dismissMiniKeyboard();
            mMiniKeyboardOnScreen = true;
            List<Key> accentKeys = (new Keyboard(getContext(), popupResId)).getKeys();
            int totalAccentKeys = accentKeys.size();
            int baseIndex = mFocusIndex;
            int currentRow = mFocusIndex / mColCount;
            int nextRow = (mFocusIndex + totalAccentKeys) / mColCount;
            if (currentRow != nextRow) {
                baseIndex = mColCount * nextRow - totalAccentKeys;
            }

            mBaseMiniKbIndex = baseIndex;

            for (int i = 0; i < totalAccentKeys; ++i) {
                Key accentKey = accentKeys.get(i);
                accentKey.x = mKeys[baseIndex + i].key.x;
                accentKey.y = mKeys[baseIndex + i].key.y;
                accentKey.edgeFlags = mKeys[baseIndex + i].key.edgeFlags;
                mKeys[baseIndex + i].key = accentKey;
                mKeys[baseIndex + i].isInMiniKb = true;
                KeyHolder holder = mKeys[baseIndex + i];

                holder.isInvertible = i == 0; // uppercase first char
            }

            invalidateAllKeys();
        } else {
            boolean isSpecialKey = mKeys[mFocusIndex].key.icon != null; // space, paste, voice input etc

            if (!isSpecialKey) { // simply use the same char in uppercase
                dismissMiniKeyboard();
                mMiniKeyboardOnScreen = true;
                mBaseMiniKbIndex = mFocusIndex;

                mKeys[mFocusIndex].isInMiniKb = true;
                mKeys[mFocusIndex].isInvertible = true;

                invalidateAllKeys();
            }
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mKeyboard == null) {
            setMeasuredDimension(getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom());
        } else {
            int heightFull = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
            heightMeasureSpec = heightFull;
            if (MeasureSpec.getSize(widthMeasureSpec) < heightFull + 10) {
                heightMeasureSpec = MeasureSpec.getSize(widthMeasureSpec);
            }

            setMeasuredDimension(heightMeasureSpec, mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom());
        }
    }

    public void setFocus(int row, int col, boolean clicked) {
        setFocus(mColCount * row + col, clicked);
    }

    public void setFocus(int index, boolean clicked) {
        setFocus(index, clicked, true);
    }

    /**
     * NOTE: Increase size of currently focused or clicked key
     * @param index index of the key
     * @param clicked key state
     * @param showFocusScale increase size
     */
    public void setFocus(final int index, final boolean clicked, final boolean showFocusScale) {
        float scale = 1.0F;
        if (mKeyImageViews != null && mKeyImageViews.length != 0) {
            int indexFull;

            if (index >= 0 && index < mKeyImageViews.length) {
                indexFull = index;
            } else {
                indexFull = -1;
            }

            if (indexFull != mFocusIndex || clicked != mFocusClicked) {
                if (indexFull != mFocusIndex) {
                    if (mFocusIndex != -1) {
                        LeanbackUtils.sendAccessibilityEvent(mKeyImageViews[mFocusIndex], false);
                    }

                    if (indexFull != -1) {
                        LeanbackUtils.sendAccessibilityEvent(mKeyImageViews[indexFull], true);
                    }
                }

                if (mCurrentFocusView != null) {
                    resetKeyTransform(mCurrentFocusView);
                }

                if (indexFull != -1) {
                    mCurrentFocusView = mKeyImageViews[indexFull];
                    if (clicked) {
                        playKeyPressAnimation(mCurrentFocusView);
                    } else {
                        // The selector communicates focus. Scaling the bitmap
                        // itself made fast controller navigation leave enlarged
                        // key artwork over neighbouring rows on some devices.
                        resetKeyTransform(mCurrentFocusView);
                    }
                }

                mFocusIndex = indexFull;
                mFocusClicked = clicked;
                if (-1 != indexFull && !mKeys[indexFull].isInMiniKb) {
                    dismissMiniKeyboard();
                }
            }
        }

    }

    public void setKeyboard(Keyboard keyboard) {
        removeMessages();
        mKeyboard = keyboard;
        setKeys(mKeyboard.getKeys());
        int state = mShiftState;
        mShiftState = -1;
        setShiftState(state);
        requestLayout();
        invalidateAllKeys();
    }

    /**
     * Set keyboard shift sate
     * @param state one of the
     * {@link LeanbackKeyboardView#SHIFT_ON SHIFT_ON},
     * {@link LeanbackKeyboardView#SHIFT_OFF SHIFT_OFF},
     * {@link LeanbackKeyboardView#SHIFT_LOCKED SHIFT_LOCKED}
     * constants
     */
    public void setShiftState(int state) {
        if (mShiftState != state) {
            switch (state) {
                case SHIFT_OFF:
                    mKeyboard.setShifted(false);
                    break;
                case SHIFT_ON:
                case SHIFT_LOCKED:
                    mKeyboard.setShifted(true);
            }

            mShiftState = state;
            invalidateAllKeys();
        }
    }

    private static class KeyHolder {
        public boolean isInMiniKb = false;
        public boolean isInvertible = false;
        public Key key;

        public KeyHolder(Key key) {
            this.key = key;
        }
    }

    public void setCapsLockDrawable(Drawable drawable) {
        mCustomCapsLockDrawable = drawable;
    }

    public void setKeyTextColor(int color) {
        mKeyTextColor = color;
        invalidateAllKeys();
    }

    /** A bounded press pulse that never alters the key's position or dimensions. */
    private void playKeyPressAnimation(View keyView) {
        keyView.animate().cancel();
        keyView.setScaleX(1f);
        keyView.setScaleY(1f);
        keyView.setAlpha(0.72f);
        keyView.animate()
                .alpha(1f)
                .setInterpolator(LeanbackKeyboardContainer.sMovementInterpolator)
                .setDuration(mClickAnimDur)
                .start();
    }

    private void resetKeyTransform(View keyView) {
        keyView.animate().cancel();
        keyView.setScaleX(1f);
        keyView.setScaleY(1f);
        keyView.setAlpha(1f);
    }

    public void setKeyBackgroundColor(int color) {
        mKeyBackgroundColor = color;
        invalidateAllKeys();
    }

    /** Updates the keycap shape for a theme (Xbox uses flatter rectangular caps). */
    public void setKeyCornerRadiusFraction(float fraction) {
        mKeyCornerRadiusFraction = Math.max(0f, Math.min(0.5f, fraction));
        invalidateAllKeys();
    }

    /** Adds a glass rim within each key without applying a full IME-window blur. */
    public void setKeyGlassEffect(boolean enabled, boolean liquid) {
        if (mKeyGlassEffect == enabled && mKeyLiquidEffect == liquid) {
            return;
        }
        mKeyGlassEffect = enabled;
        mKeyLiquidEffect = enabled && liquid;
        if (mKeys != null) {
            invalidateAllKeys();
        }
    }
}
