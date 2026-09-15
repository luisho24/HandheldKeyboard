package com.liskovsoft.leankeyboard.activity.settings;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Small, self-contained keyboard miniature for theme selection cards.
 * It deliberately draws its contents on a Canvas so it can scale to any card size.
 */
public class KeyboardThemePreviewView extends View {
    private static final String[] NUMBER_ROW = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "⌫"};
    private static final String[] TOP_ROW = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "@"};
    private static final String[] MIDDLE_ROW = {"a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ", "&"};
    private static final String[] BOTTOM_ROW = {"z", "x", "c", "v", "b", "n", "m", ",", ".", "-", "?"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private final float scaledDensity;

    private int backgroundColor = 0xff18222f;
    private int keyColor = 0xff303d4d;
    private int textColor = 0xfff3f6fa;
    private int accentColor = 0xff57c6d8;
    private String themeLabel = "";
    private boolean xboxStyle;
    private Bitmap backgroundImage;
    private float pulse = 0.5f;
    private ValueAnimator pulseAnimator;
    private boolean pulseEnabled = true;

    public KeyboardThemePreviewView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        scaledDensity = density * context.getResources().getConfiguration().fontScale;
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setFocusable(false);
        setWillNotDraw(false);
    }

    public void setPalette(int background, int key, int text, int accent) {
        backgroundColor = background;
        keyColor = key;
        textColor = text;
        accentColor = accent;
        invalidate();
    }

    public void setThemeLabel(String label) {
        themeLabel = label == null ? "" : label.trim();
        xboxStyle = "xbox".equalsIgnoreCase(themeLabel);
        invalidate();
    }

    public void setBackgroundImage(Bitmap image) {
        backgroundImage = image;
        invalidate();
    }

    public void setPulseEnabled(boolean enabled) {
        pulseEnabled = enabled;
        if (!enabled && pulseAnimator != null && pulseAnimator.isStarted()) {
            pulseAnimator.cancel();
            pulse = 0.5f;
            invalidate();
        } else if (enabled && getWindowToken() != null) {
            ensureAnimator();
            if (pulseAnimator != null && !pulseAnimator.isStarted()) pulseAnimator.start();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(220f * density);
        int desiredHeight = Math.round(126f * density);
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float width = getWidth();
        final float height = getHeight();
        if (width <= 0f || height <= 0f) {
            return;
        }

        final float minDimension = Math.min(width, height);
        final float pad = clamp(minDimension * 0.055f, 2f * density, 12f * density);
        final float cardRadius = Math.min(minDimension * 0.12f, 15f * density);

        rect.set(0f, 0f, width, height);
        paint.setStyle(Paint.Style.FILL);
        if (backgroundImage != null && !backgroundImage.isRecycled()) {
            int save = canvas.save();
            canvas.clipPath(roundRectPath(rect, cardRadius));
            drawCenterCrop(canvas, backgroundImage, rect);
            paint.setColor(withAlpha(backgroundColor, 190));
            canvas.drawRoundRect(rect, cardRadius, cardRadius, paint);
            canvas.restoreToCount(save);
        } else {
            paint.setColor(backgroundColor);
            canvas.drawRoundRect(rect, cardRadius, cardRadius, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, density));
        paint.setColor(withAlpha(accentColor, 46));
        rect.inset(paint.getStrokeWidth() / 2f, paint.getStrokeWidth() / 2f);
        canvas.drawRoundRect(rect, cardRadius, cardRadius, paint);
        paint.setStyle(Paint.Style.FILL);

        // The preview uses the exact handheld structure: a numeric row, three
        // character rows, a utility row and the action rail at the right.
        // It intentionally does not draw a generic phone-keyboard suggestion bar.
        final float contentWidth = Math.max(0f, width - 2f * pad);
        final float bodyTop = pad;
        final float bodyBottom = height - pad;
        final float bodyHeight = Math.max(0f, bodyBottom - bodyTop);
        if (bodyHeight <= 0f || contentWidth <= 0f) return;

        float horizontalGap = clamp(contentWidth * 0.010f, 1f * density, 3f * density);
        float rowGap = clamp(bodyHeight * 0.038f, 1f * density, 3f * density);
        float railWidth = Math.max(contentWidth * 0.095f, 16f * density);
        float mainWidth = contentWidth - railWidth - horizontalGap;
        float keyWidth = (mainWidth - 10f * horizontalGap) / 11f;
        if (keyWidth <= 0f) return;
        float keyHeight = Math.max(1f, (bodyHeight - 4f * rowGap) / 5f);
        float keyRadius = xboxStyle
                ? Math.min(keyHeight * 0.08f, 4f * density)
                : Math.min(keyHeight * 0.23f, 6f * density);
        float textSize = clamp(keyHeight * 0.60f, 4.5f * scaledDensity, 12f * scaledDensity);

        drawLettersRow(canvas, NUMBER_ROW, pad, bodyTop, keyWidth, keyHeight, horizontalGap,
                keyRadius, textSize * 0.88f, false);
        drawLettersRow(canvas, TOP_ROW, pad, bodyTop + keyHeight + rowGap, keyWidth, keyHeight,
                horizontalGap, keyRadius, textSize, true);
        drawLettersRow(canvas, MIDDLE_ROW, pad, bodyTop + 2f * (keyHeight + rowGap), keyWidth,
                keyHeight, horizontalGap, keyRadius, textSize, false);
        drawLettersRow(canvas, BOTTOM_ROW, pad, bodyTop + 3f * (keyHeight + rowGap), keyWidth,
                keyHeight, horizontalGap, keyRadius, textSize, false);

        drawUtilityRow(canvas, pad, bodyTop + 4f * (keyHeight + rowGap), mainWidth,
                keyHeight, horizontalGap, keyRadius, textSize);
        drawActionRail(canvas, pad + mainWidth + horizontalGap, bodyTop, railWidth, keyHeight,
                rowGap, keyRadius, textSize);
    }

    private void drawUtilityRow(Canvas canvas, float left, float top, float width, float height,
                                float gap, float radius, float textSize) {
        float unit = (width - 7f * gap) / 11f;
        float x = left;
        String[] shortcuts = {"?123", "⇧", "◎", "⚙"};
        for (String shortcut : shortcuts) {
            drawSpecialKey(canvas, x, top, unit, height, radius, shortcut, textSize * 0.72f, false);
            x += unit + gap;
        }
        float spaceWidth = unit * 4f + 3f * gap;
        drawSpaceKey(canvas, x, top, spaceWidth, height, radius, textSize * 0.64f);
        x += spaceWidth + gap;
        drawSpecialKey(canvas, x, top, unit, height, radius, "☺", textSize * 0.76f, false);
        x += unit + gap;
        drawSpecialKey(canvas, x, top, unit, height, radius, "edit", textSize * 0.48f, false);
        x += unit + gap;
        drawSpecialKey(canvas, x, top, unit, height, radius, "↕", textSize * 0.72f, false);
    }

    private void drawActionRail(Canvas canvas, float left, float top, float width, float keyHeight,
                                float gap, float radius, float textSize) {
        String[] actions = {"|◀", "▶|", "▣", "Go"};
        int[] actionRows = {0, 1, 2, 4};
        for (int i = 0; i < actions.length; i++) {
            float y = top + actionRows[i] * (keyHeight + gap);
            drawSpecialKey(canvas, left, y, width, keyHeight, radius, actions[i],
                    actions[i].equals("Go") ? textSize * 0.65f : textSize * 0.66f, i == 3);
        }
    }

    private void drawSuggestion(Canvas canvas, RectF bounds, float height) {
        String[] words = {"the", "hello", "world"};
        float slotWidth = bounds.width() / words.length;
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(clamp(height * 0.54f, 6f * scaledDensity, 10f * scaledDensity));
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = bounds.centerY() - (metrics.ascent + metrics.descent) / 2f;
        for (int i = 0; i < words.length; i++) {
            float centerX = bounds.left + (i + 0.5f) * slotWidth;
            if (i == 1) {
                rect.set(centerX - slotWidth * 0.36f, bounds.top + height * 0.12f,
                        centerX + slotWidth * 0.36f, bounds.bottom - height * 0.12f);
                paint.setColor(withAlpha(accentColor, 188));
                canvas.drawRoundRect(rect, height * 0.2f, height * 0.2f, paint);
            }
            paint.setColor(textColor);
            canvas.drawText(words[i], centerX, baseline, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawLettersRow(Canvas canvas, String[] letters, float left, float top,
                                float keyWidth, float keyHeight, float gap, float radius,
                                float textSize) {
        drawLettersRow(canvas, letters, left, top, keyWidth, keyHeight, gap, radius, textSize, false);
    }

    private void drawLettersRow(Canvas canvas, String[] letters, float left, float top,
                                float keyWidth, float keyHeight, float gap, float radius,
                                float textSize, boolean highlightFirst) {
        for (int i = 0; i < letters.length; i++) {
            drawKey(canvas, left + i * (keyWidth + gap), top, keyWidth, keyHeight,
                    radius, letters[i], textSize, highlightFirst && i == 0);
        }
    }

    private void drawKey(Canvas canvas, float x, float y, float width, float height,
                         float radius, String label, float textSize, boolean selected) {
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float scale = selected ? 1f + (pulse - 0.5f) * 0.045f : 1f;
        int save = canvas.save();
        canvas.scale(scale, scale, centerX, centerY);

        rect.set(x, y + Math.max(1f, density * 0.8f), x + width, y + height + Math.max(1f, density * 0.8f));
        paint.setColor(withAlpha(0xff000000, 34));
        canvas.drawRoundRect(rect, radius, radius, paint);

        if (selected && !xboxStyle) {
            rect.set(x - density * 1.2f, y - density * 1.2f,
                    x + width + density * 1.2f, y + height + density * 1.2f);
            paint.setColor(withAlpha(accentColor, Math.round(28f + pulse * 38f)));
            canvas.drawRoundRect(rect, radius + density, radius + density, paint);
        }

        rect.set(x, y, x + width, y + height);
        paint.setColor(selected && !xboxStyle
                ? blend(keyColor, accentColor, 0.24f + pulse * 0.10f) : keyColor);
        canvas.drawRoundRect(rect, radius, radius, paint);

        if (selected && xboxStyle) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, density * 2f));
            paint.setColor(accentColor);
            rect.inset(paint.getStrokeWidth() / 2f, paint.getStrokeWidth() / 2f);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        paint.setColor(textColor);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2f;
        drawFittedText(canvas, label, centerX, baseline, Math.max(0f, width - 2f * density));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.restoreToCount(save);
    }

    private void drawSpecialKey(Canvas canvas, float x, float y, float width, float height,
                                float radius, String label, float textSize, boolean selected) {
        drawKey(canvas, x, y, width, height, radius, label, textSize, selected);
    }

    private void drawSpaceKey(Canvas canvas, float x, float y, float width, float height,
                              float radius, float textSize) {
        drawKey(canvas, x, y, width, height, radius, "English", textSize, false);
    }

    private void drawFittedText(Canvas canvas, String text, float x, float baseline, float maxWidth) {
        if (text == null || text.length() == 0 || maxWidth <= 0f) {
            return;
        }
        float measured = paint.measureText(text);
        if (measured > maxWidth && measured > 0f) {
            int save = canvas.save();
            canvas.translate(x, baseline);
            canvas.scale(maxWidth / measured, 1f);
            canvas.drawText(text, 0f, 0f, paint);
            canvas.restoreToCount(save);
        } else {
            canvas.drawText(text, x, baseline, paint);
        }
    }

    private void drawCenterCrop(Canvas canvas, Bitmap image, RectF destination) {
        float scale = Math.max(destination.width() / image.getWidth(),
                destination.height() / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        float left = destination.centerX() - width / 2f;
        float top = destination.centerY() - height / 2f;
        canvas.drawBitmap(image, null, new RectF(left, top, left + width, top + height), paint);
    }

    private android.graphics.Path roundRectPath(RectF bounds, float radius) {
        android.graphics.Path path = new android.graphics.Path();
        path.addRoundRect(bounds, radius, radius, android.graphics.Path.Direction.CW);
        return path;
    }

    private void ensureAnimator() {
        if (pulseAnimator != null || !areAnimationsEnabled()) {
            return;
        }
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1250L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            Object value = animation.getAnimatedValue();
            if (value instanceof Float) {
                pulse = (Float) value;
                if (Build.VERSION.SDK_INT >= 16) {
                    postInvalidateOnAnimation();
                } else {
                    invalidate();
                }
            }
        });
    }

    private boolean areAnimationsEnabled() {
        try {
            float scale;
            if (Build.VERSION.SDK_INT >= 17) {
                scale = Settings.Global.getFloat(getContext().getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            } else {
                scale = Settings.System.getFloat(getContext().getContentResolver(),
                        "animator_duration_scale", 1f);
            }
            return scale > 0f;
        } catch (RuntimeException ignored) {
            // Some vendor builds restrict reading animation scale. Prefer the lightweight pulse.
            return true;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (pulseEnabled) ensureAnimator();
        if (pulseAnimator != null && !pulseAnimator.isStarted()) {
            pulseAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (clamp(alpha, 0, 255) << 24);
    }

    private static int blend(int from, int to, float amount) {
        float t = clamp(amount, 0f, 1f);
        int a = Math.round(((from >>> 24) & 0xff) * (1f - t) + ((to >>> 24) & 0xff) * t);
        int r = Math.round(((from >>> 16) & 0xff) * (1f - t) + ((to >>> 16) & 0xff) * t);
        int g = Math.round(((from >>> 8) & 0xff) * (1f - t) + ((to >>> 8) & 0xff) * t);
        int b = Math.round((from & 0xff) * (1f - t) + (to & 0xff) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
