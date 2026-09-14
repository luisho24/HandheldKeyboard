package com.liskovsoft.leankeyboard.addons.theme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.core.content.ContextCompat;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardView;
import com.liskovsoft.leankeyboard.utils.KeyboardLayoutPreferences;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

import java.io.File;

public class ThemeManager {
    private static final String TAG = ThemeManager.class.getSimpleName();
    private static final int MAX_BACKGROUND_DIMENSION = 2048;
    private final Context mContext;
    private final RelativeLayout mRootView;
    private final LeanKeyPreferences mPrefs;
    private final int mBasePaddingLeft;
    private final int mBasePaddingTop;
    private final int mBasePaddingRight;
    private final int mBasePaddingBottom;
    private String mLoadedImagePath;
    private long mLoadedImageLastModified;
    private Bitmap mLoadedImage;

    public ThemeManager(Context context, RelativeLayout rootView) {
        mContext = context;
        mRootView = rootView;
        mPrefs = LeanKeyPreferences.instance(mContext);
        mBasePaddingLeft = rootView.getPaddingLeft();
        mBasePaddingTop = rootView.getPaddingTop();
        mBasePaddingRight = rootView.getPaddingRight();
        mBasePaddingBottom = rootView.getPaddingBottom();
    }

    public void updateKeyboardTheme() {
        applyImeWindowSurface();
        String currentThemeId = mPrefs.getCurrentTheme();
        if (LeanKeyPreferences.THEME_CUSTOM.equals(currentThemeId)) {
            applyCustomTheme();
            return;
        }

        if (LeanKeyPreferences.THEME_DEFAULT.equals(currentThemeId)) {
            int background = colorOrDefault(R.color.keyboard_background, R.color.keyboard_background);
            int text = colorOrDefault(R.color.key_text_default, R.color.key_text_default);
            applyKeyboardColors(background,
                    colorOrDefault(R.color.candidate_background, R.color.keyboard_background),
                    colorOrDefault(R.color.enter_key_font_color, R.color.key_text_default),
                    text, blend(background, text, 0.14f), false);
            applyBackgroundImage(null);
            applyShiftDrawable(-1);
            return;
        }

        boolean found = applyForTheme(themeId -> {
            int defaultBackground = colorOrDefault(R.color.keyboard_background, R.color.keyboard_background);
            int defaultText = colorOrDefault(R.color.key_text_default, R.color.key_text_default);
            int background = themeColor(themeId, "keyboard_background_", defaultBackground);
            int text = themeColor(themeId, "key_text_default_", defaultText);
            applyKeyboardColors(background,
                    themeColor(themeId, "candidate_background_", R.color.candidate_background),
                    themeColor(themeId, "enter_key_font_color_", R.color.enter_key_font_color),
                    text, blend(background, text, 0.14f), false);

            int shiftLockOnResId = mContext.getResources().getIdentifier(
                    "ic_ime_shift_lock_on_" + themeId.toLowerCase(java.util.Locale.ROOT),
                    "drawable", mContext.getPackageName());
            applyShiftDrawable(shiftLockOnResId);
            applyBackgroundImage(null);
        });

        if (!found) {
            int background = colorOrDefault(R.color.keyboard_background, R.color.keyboard_background);
            int text = colorOrDefault(R.color.key_text_default, R.color.key_text_default);
            applyKeyboardColors(background,
                    colorOrDefault(R.color.candidate_background, R.color.keyboard_background),
                    colorOrDefault(R.color.enter_key_font_color, R.color.key_text_default),
                    text, blend(background, text, 0.14f), false);
            applyBackgroundImage(null);
            applyShiftDrawable(-1);
        }
    }

    public void updateSuggestionsTheme() {
        String currentTheme = mPrefs.getCurrentTheme();
        int candidateFontColor;

        if (LeanKeyPreferences.THEME_CUSTOM.equals(currentTheme)) {
            candidateFontColor = resolveCustomAccent(mPrefs.isDarkAppearance());
        } else if (LeanKeyPreferences.THEME_DEFAULT.equals(currentTheme)) {
            candidateFontColor = colorOrDefault(R.color.candidate_font_color, R.color.key_text_default);
        } else {
            final int[] selectedColor = {colorOrDefault(R.color.candidate_font_color, R.color.key_text_default)};
            applyForTheme(themeId -> selectedColor[0] = themeColor(themeId, "candidate_font_color_", R.color.candidate_font_color));
            candidateFontColor = selectedColor[0];
        }

        applySuggestionsColors(candidateFontColor);
    }

    private void applyCustomTheme() {
        boolean darkVariant = mPrefs.isDarkAppearance();
        String imagePath = mPrefs.getCustomThemeImagePath(darkVariant);
        Bitmap image = loadBackgroundImage(imagePath);
        boolean hasImage = image != null;
        int background = resolveCustomColor(darkVariant, "background");
        int candidateBackground = resolveCustomColor(darkVariant, "candidate");
        int keyBackground = resolveCustomColor(darkVariant, "key");
        int text = resolveCustomColor(darkVariant, "text");
        int accent = resolveCustomAccent(darkVariant);

        applyKeyboardColors(background, candidateBackground, accent, text, keyBackground, hasImage);
        applyBackgroundImage(image);
        applyShiftDrawable(-1);
    }

    private void applyBackgroundImage(Bitmap image) {
        if (image == null) {
            if (mLoadedImage != null && !mLoadedImage.isRecycled()) {
                mLoadedImage.recycle();
            }
            mLoadedImage = null;
            mLoadedImagePath = null;
            mLoadedImageLastModified = 0;
            return;
        }

        boolean floating = KeyboardLayoutPreferences.isFloatingKeyboard(mContext);
        float cornerRadius = floating ? dp(22) : 0;
        Drawable photo = new CenterCropDrawable(image, cornerRadius);
        int surfaceAlpha = getSurfaceAlpha();
        photo.setAlpha(surfaceAlpha);
        int overlayAlpha = LeanKeyPreferences.SURFACE_SOLID.equals(mPrefs.getThemeSurfaceMode()) ? 0x50 : 0x20;
        Drawable dimLayer = new ColorDrawable((overlayAlpha << 24));
        if (floating) {
            GradientDrawable border = createSurfaceDrawable(Color.TRANSPARENT, 22);
            Drawable[] layers = {photo, dimLayer, border};
            mRootView.setBackgroundDrawable(withFloatingInsets(new LayerDrawable(layers), true));
        } else {
            mRootView.setBackgroundDrawable(new LayerDrawable(new Drawable[]{photo, dimLayer}));
        }
    }

    private void applyKeyboardColors(int keyboardBackground, int candidateBackground,
                                    int enterFontColor, int keyTextColor,
                                    int keyBackgroundColor, boolean hasImage) {
        RelativeLayout rootLayout = mRootView.findViewById(R.id.root_ime);
        boolean floating = KeyboardLayoutPreferences.isFloatingKeyboard(mContext);
        applyRootPadding(floating);
        if (rootLayout != null) {
            int surfaceColor = withAlpha(keyboardBackground, getSurfaceAlpha());
            boolean liquid = LeanKeyPreferences.SURFACE_LIQUID.equals(mPrefs.getThemeSurfaceMode());
            Drawable surface = floating || liquid
                    ? createSurfaceDrawable(surfaceColor, floating ? 22 : 0)
                    : new ColorDrawable(surfaceColor);
            rootLayout.setBackgroundDrawable(withFloatingInsets(surface, floating));
        }

        View candidateLayout = mRootView.findViewById(R.id.candidate_background);
        if (candidateLayout != null) {
            int candidateColor = withAlpha(candidateBackground, getSurfaceAlpha());
            boolean liquid = LeanKeyPreferences.SURFACE_LIQUID.equals(mPrefs.getThemeSurfaceMode());
            candidateLayout.setBackgroundDrawable(floating || liquid
                    ? createSurfaceDrawable(candidateColor, floating ? 10 : 0)
                    : new ColorDrawable(candidateColor));
        }

        Button enterButton = mRootView.findViewById(R.id.enter);
        if (enterButton != null) {
            enterButton.setTextColor(enterFontColor);
        }

        LeanbackKeyboardView keyboardView = mRootView.findViewById(R.id.main_keyboard);
        if (keyboardView != null) {
            keyboardView.setKeyBackgroundColor(withAlpha(keyBackgroundColor, getSurfaceAlpha()));
            keyboardView.setKeyTextColor(keyTextColor);
        }
    }

    /** Keeps the full-width IME window transparent while drawing the keyboard as a padded card. */
    private void applyRootPadding(boolean floating) {
        int sideInset = floating ? dp(14) : 0;
        int topInset = floating ? dp(8) : 0;
        int bottomInset = floating ? dp(14) : 0;
        mRootView.setPadding(mBasePaddingLeft + sideInset,
                mBasePaddingTop + topInset,
                mBasePaddingRight + sideInset,
                mBasePaddingBottom + bottomInset);
    }

    private Drawable withFloatingInsets(Drawable drawable, boolean floating) {
        if (!floating) {
            return drawable;
        }
        int sideInset = dp(14);
        return new InsetDrawable(drawable, sideInset, dp(8), sideInset, dp(14));
    }

    private int resolveCustomColor(boolean darkVariant, String role) {
        if (mPrefs.isCustomThemeUsingSystemColors()) {
            int fallback = getManualCustomColor(darkVariant, role);
            if ("background".equals(role)) {
                return systemColor(darkVariant ? "system_neutral1_900" : "system_neutral1_10", fallback);
            }
            if ("candidate".equals(role)) {
                return systemColor(darkVariant ? "system_neutral2_800" : "system_neutral2_50", fallback);
            }
            if ("key".equals(role)) {
                return systemColor(darkVariant ? "system_neutral2_700" : "system_neutral2_100", fallback);
            }
            if ("text".equals(role)) {
                return systemColor(darkVariant ? "system_neutral1_100" : "system_neutral1_900", fallback);
            }
        }
        return getManualCustomColor(darkVariant, role);
    }

    private int resolveCustomAccent(boolean darkVariant) {
        int fallback = mPrefs.getCustomThemeAccent(darkVariant);
        if (mPrefs.isCustomThemeUsingSystemColors()) {
            return systemColor(darkVariant ? "system_accent1_200" : "system_accent1_600", fallback);
        }
        return fallback;
    }

    private int getManualCustomColor(boolean darkVariant, String role) {
        if ("background".equals(role)) return mPrefs.getCustomThemeBackground(darkVariant);
        if ("candidate".equals(role)) return mPrefs.getCustomThemeCandidate(darkVariant);
        if ("key".equals(role)) return mPrefs.getCustomThemeKey(darkVariant);
        return mPrefs.getCustomThemeText(darkVariant);
    }

    /** Android's wallpaper-derived system palettes are public from Android 12 (API 31). */
    private int systemColor(String resourceName, int fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return fallback;
        }
        try {
            int resourceId = mContext.getResources().getIdentifier(resourceName, "color", "android");
            return resourceId == 0 ? fallback : mContext.getResources().getColor(resourceId);
        } catch (RuntimeException error) {
            return fallback;
        }
    }

    private int getSurfaceAlpha() {
        String mode = mPrefs.getThemeSurfaceMode();
        if (LeanKeyPreferences.SURFACE_SOLID.equals(mode)) {
            return 255;
        }
        return Math.round(255 * (mPrefs.getThemeSurfaceOpacity() / 100f));
    }

    private GradientDrawable createSurfaceDrawable(int color, int radiusDp) {
        String mode = mPrefs.getThemeSurfaceMode();
        if (LeanKeyPreferences.SURFACE_LIQUID.equals(mode)) {
            return new LiquidGlassSurfaceDrawable(mContext, color, radiusDp);
        }
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(color);
        surface.setCornerRadius(dp(radiusDp));
        if (LeanKeyPreferences.SURFACE_GLASS.equals(mode) ||
                LeanKeyPreferences.SURFACE_TRANSLUCENT.equals(mode)) {
            int borderAlpha = LeanKeyPreferences.SURFACE_GLASS.equals(mode) ? 96 : 56;
            surface.setStroke(Math.max(1, dp(1)), withAlpha(Color.WHITE, borderAlpha));
        }
        return surface;
    }

    private int dp(int value) {
        return Math.round(value * mContext.getResources().getDisplayMetrics().density);
    }

    /** Keeps glass styling on keyboard views instead of blurring the full-screen IME window. */
    private void applyImeWindowSurface() {
        if (!(mContext instanceof InputMethodService)) {
            return;
        }
        try {
            Window window = ((InputMethodService) mContext).getWindow().getWindow();
            if (window == null) return;

            boolean glass = LeanKeyPreferences.SURFACE_GLASS.equals(mPrefs.getThemeSurfaceMode())
                    || LeanKeyPreferences.SURFACE_LIQUID.equals(mPrefs.getThemeSurfaceMode());
            boolean translucent = LeanKeyPreferences.SURFACE_TRANSLUCENT.equals(mPrefs.getThemeSurfaceMode());
            boolean floating = KeyboardLayoutPreferences.isFloatingKeyboard(mContext);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // An IME window can cover the whole app even when its keyboard
                // content is only at the bottom. Window blur therefore softens
                // the host editor as well. Keep the window sharp and render the
                // glass treatment on the keyboard's own background drawables.
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                window.setBackgroundBlurRadius(0);
            }
            if (glass || translucent || floating) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            } else {
                window.setBackgroundDrawable(new ColorDrawable(resolveKeyboardBackgroundColor()));
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "IME window does not support the selected translucent surface", error);
        }
    }

    private int resolveKeyboardBackgroundColor() {
        String themeId = mPrefs.getCurrentTheme();
        if (LeanKeyPreferences.THEME_CUSTOM.equals(themeId)) {
            return resolveCustomColor(mPrefs.isDarkAppearance(), "background");
        }
        if (LeanKeyPreferences.THEME_DEFAULT.equals(themeId)) {
            return colorOrDefault(R.color.keyboard_background, R.color.keyboard_background);
        }

        final int[] result = {colorOrDefault(R.color.keyboard_background, R.color.keyboard_background)};
        applyForTheme(id -> result[0] = themeColor(id, "keyboard_background_", R.color.keyboard_background));
        return result[0];
    }

    private void applySuggestionsColors(int candidateFontColor) {
        LinearLayout suggestions = mRootView.findViewById(R.id.suggestions);
        if (suggestions == null) {
            return;
        }

        int childCount = suggestions.getChildCount();
        Log.d(TAG, "Number of suggestions: " + childCount);
        for (int i = 0; i < childCount; i++) {
            View child = suggestions.getChildAt(i);
            Button candidateButton = child.findViewById(R.id.text);
            if (candidateButton != null) {
                candidateButton.setTextColor(candidateFontColor);
            }
        }
    }

    private void applyShiftDrawable(int resId) {
        LeanbackKeyboardView keyboardView = mRootView.findViewById(R.id.main_keyboard);
        if (keyboardView == null) {
            return;
        }

        Drawable drawable = resId > 0 ? ContextCompat.getDrawable(mContext, resId) : null;
        keyboardView.setCapsLockDrawable(drawable);
    }

    private int themeColor(String themeId, String prefix, int defaultResId) {
        int resourceId = mContext.getResources().getIdentifier(
                prefix + themeId.toLowerCase(java.util.Locale.ROOT), "color", mContext.getPackageName());
        return colorOrDefault(resourceId, defaultResId);
    }

    private int colorOrDefault(int resourceId, int defaultResId) {
        int safeResourceId = resourceId != 0 ? resourceId : defaultResId;
        return ContextCompat.getColor(mContext, safeResourceId);
    }

    private Bitmap loadBackgroundImage(String imagePath) {
        if (imagePath == null || imagePath.length() == 0) {
            return null;
        }
        File imageFile = new File(imagePath);
        if (!imageFile.isFile()) {
            return null;
        }
        if (imagePath.equals(mLoadedImagePath) && mLoadedImage != null && !mLoadedImage.isRecycled() &&
                imageFile.lastModified() == mLoadedImageLastModified) {
            return mLoadedImage;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int sampleSize = 1;
        while (bounds.outWidth / sampleSize > MAX_BACKGROUND_DIMENSION ||
                bounds.outHeight / sampleSize > MAX_BACKGROUND_DIMENSION) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        if (bitmap != null) {
            mLoadedImagePath = imagePath;
            mLoadedImageLastModified = imageFile.lastModified();
            mLoadedImage = bitmap;
        }
        return bitmap;
    }

    private boolean applyForTheme(ThemeCallback callback) {
        String currentThemeId = mPrefs.getCurrentTheme();
        Resources resources = mContext.getResources();
        String[] themes = resources.getStringArray(R.array.keyboard_themes);

        for (String theme : themes) {
            String[] split = theme.split("\\|");
            if (split.length >= 2 && currentThemeId.equals(split[1])) {
                callback.onThemeFound(split[1]);
                return true;
            }
        }
        return false;
    }

    private static int blend(int base, int overlay, float amount) {
        float inverse = 1.0f - amount;
        return Color.rgb(
                Math.round(Color.red(base) * inverse + Color.red(overlay) * amount),
                Math.round(Color.green(base) * inverse + Color.green(overlay) * amount),
                Math.round(Color.blue(base) * inverse + Color.blue(overlay) * amount));
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static class CenterCropDrawable extends Drawable {
        private final Bitmap mBitmap;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        private final RectF mDestination = new RectF();
        private final float mCornerRadius;
        private int mAlpha = 255;

        CenterCropDrawable(Bitmap bitmap, float cornerRadius) {
            mBitmap = bitmap;
            mCornerRadius = cornerRadius;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.width() <= 0 || bounds.height() <= 0 || mBitmap.isRecycled()) {
                return;
            }

            float scale = Math.max(bounds.width() / (float) mBitmap.getWidth(),
                    bounds.height() / (float) mBitmap.getHeight());
            float width = mBitmap.getWidth() * scale;
            float height = mBitmap.getHeight() * scale;
            float left = bounds.left + (bounds.width() - width) / 2.0f;
            float top = bounds.top + (bounds.height() - height) / 2.0f;
            mDestination.set(left, top, left + width, top + height);
            mPaint.setAlpha(mAlpha);

            int saveCount = canvas.save();
            if (mCornerRadius > 0) {
                Path clip = new Path();
                clip.addRoundRect(new RectF(bounds), mCornerRadius, mCornerRadius, Path.Direction.CW);
                canvas.clipPath(clip);
            } else {
                canvas.clipRect(bounds);
            }
            canvas.drawBitmap(mBitmap, null, mDestination, mPaint);
            canvas.restoreToCount(saveCount);
        }

        @Override
        public void setAlpha(int alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return mAlpha == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
        }
    }

    private interface ThemeCallback {
        void onThemeFound(String themeId);
    }
}
