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
    private static final String[] TOP_ROW = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
    private static final String[] MIDDLE_ROW = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
    private static final String[] BOTTOM_ROW = {"z", "x", "c", "v", "b", "n", "m"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private final float scaledDensity;

    private int backgroundColor = 0xff18222f;
    private int keyColor = 0xff303d4d;
    private int textColor = 0xfff3f6fa;
    private int accentColor = 0xff57c6d8;
    private String themeLabel = "";
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

        final float contentWidth = Math.max(0f, width - 2f * pad);
        final float labelTop = pad + Math.min(height * 0.13f, 18f * density);
        final float labelSize = clamp(height * 0.075f, 7f * scaledDensity, 11f * scaledDensity);
        if (!themeLabel.isEmpty() && contentWidth > 0f) {
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setTextSize(labelSize);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(withAlpha(textColor, 218));
            drawFittedText(canvas, themeLabel, pad, labelTop, Math.max(0f, contentWidth * 0.72f));
        }

        final float suggestionTop = pad + Math.min(height * 0.16f, 21f * density);
        final float suggestionHeight = clamp(height * 0.14f, 10f * density, 19f * density);
        final float suggestionRadius = Math.min(suggestionHeight * 0.32f, 7f * density);
        rect.set(pad, suggestionTop, width - pad, suggestionTop + suggestionHeight);
        paint.setColor(withAlpha(keyColor, 192));
        canvas.drawRoundRect(rect, suggestionRadius, suggestionRadius, paint);
        drawSuggestion(canvas, rect, suggestionHeight);

        final float bodyTop = suggestionTop + suggestionHeight + Math.max(2f * density, height * 0.035f);
        final float bodyBottom = height - pad;
        final float bodyHeight = Math.max(0f, bodyBottom - bodyTop);
        if (bodyHeight <= 0f || contentWidth <= 0f) {
            return;
        }

        float horizontalGap = clamp(contentWidth * 0.012f, 1f * density, 4f * density);
        float keyWidth = (contentWidth - 9f * horizontalGap) / 10f;
        if (keyWidth <= 0f) {
            return;
        }
        float rowGap = clamp(bodyHeight * 0.055f, 1f * density, 4f * density);
        float keyHeight = Math.max(1f, (bodyHeight - 3f * rowGap) / 4f);
        float keyRadius = Math.min(keyHeight * 0.23f, 6f * density);
        float textSize = clamp(keyHeight * 0.57f, 5f * scaledDensity, 13f * scaledDensity);

        drawLettersRow(canvas, TOP_ROW, pad, bodyTop, keyWidth, keyHeight, horizontalGap,
                keyRadius, textSize);

        float middleWidth = 9f * keyWidth + 8f * horizontalGap;
        drawLettersRow(canvas, MIDDLE_ROW, pad + (contentWidth - middleWidth) / 2f,
                bodyTop + keyHeight + rowGap, keyWidth, keyHeight, horizontalGap,
                keyRadius, textSize);

        float thirdY = bodyTop + 2f * (keyHeight + rowGap);
        drawSpecialKey(canvas, pad, thirdY, keyWidth * 1.42f, keyHeight, keyRadius,
                "↑", textSize * 0.88f, false);
        float thirdStart = pad + keyWidth * 1.42f + horizontalGap;
        for (int i = 0; i < BOTTOM_ROW.length; i++) {
            float x = thirdStart + i * (keyWidth + horizontalGap);
            boolean selected = i == 2;
            drawKey(canvas, x, thirdY, keyWidth, keyHeight, keyRadius,
                    BOTTOM_ROW[i], textSize, selected);
        }
        float enterX = width - pad - keyWidth * 1.42f;
        drawSpecialKey(canvas, enterX, thirdY, keyWidth * 1.42f, keyHeight, keyRadius,
                "↵", textSize * 0.88f, true);

        float utilityY = bodyTop + 3f * (keyHeight + rowGap);
        float utilityGap = horizontalGap;
        float utilityWidths = keyWidth * (1.32f + 1.05f + 4.55f + 1.32f) + 3f * utilityGap;
        float utilityX = pad + Math.max(0f, (contentWidth - utilityWidths) / 2f);
        drawSpecialKey(canvas, utilityX, utilityY, keyWidth * 1.32f, keyHeight,
                keyRadius, "?123", textSize * 0.58f, false);
        utilityX += keyWidth * 1.32f + utilityGap;
        drawSpecialKey(canvas, utilityX, utilityY, keyWidth * 1.05f, keyHeight,
                keyRadius, "◎", textSize * 0.78f, false);
        utilityX += keyWidth * 1.05f + utilityGap;
        drawSpaceKey(canvas, utilityX, utilityY, keyWidth * 4.55f, keyHeight,
                keyRadius, textSize * 0.55f);
        utilityX += keyWidth * 4.55f + utilityGap;
        drawSpecialKey(canvas, utilityX, utilityY, keyWidth * 1.32f, keyHeight,
                keyRadius, "↵", textSize * 0.78f, false);
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
        for (int i = 0; i < letters.length; i++) {
            drawKey(canvas, left + i * (keyWidth + gap), top, keyWidth, keyHeight,
                    radius, letters[i], textSize, false);
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

        if (selected) {
            rect.set(x - density * 1.2f, y - density * 1.2f,
                    x + width + density * 1.2f, y + height + density * 1.2f);
            paint.setColor(withAlpha(accentColor, Math.round(28f + pulse * 38f)));
            canvas.drawRoundRect(rect, radius + density, radius + density, paint);
        }

        rect.set(x, y, x + width, y + height);
        paint.setColor(selected ? blend(keyColor, accentColor, 0.24f + pulse * 0.10f) : keyColor);
        canvas.drawRoundRect(rect, radius, radius, paint);

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
