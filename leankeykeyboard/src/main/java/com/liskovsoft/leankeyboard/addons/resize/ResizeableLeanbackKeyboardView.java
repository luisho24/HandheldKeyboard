package com.liskovsoft.leankeyboard.addons.resize;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardView;
import com.liskovsoft.leankeyboard.utils.KeyboardLayoutPreferences;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.List;

public class ResizeableLeanbackKeyboardView extends LeanbackKeyboardView {
    private static final String HANDHELD_PACKAGE = "com.handheldkeyboard.ime";
    private static final float HANDHELD_KEY_LABEL_SCALE = 1.10f;
    // The source keyboard XML was designed for television remotes, where broad
    // gaps make individual keys easier to isolate. Handheld screens use the
    // same layouts but reserve a dedicated editor-action lane, so compact the
    // source gaps before scaling the key grid to the available width.
    private static final float HANDHELD_HORIZONTAL_GAP_FACTOR = 0.36f;
    private static final float HANDHELD_VERTICAL_GAP_FACTOR = 0.45f;
    private final LeanKeyPreferences mPrefs;
    private final int mKeyTextSizeOrigin;
    private final int mModeChangeTextSizeOrigin;
    private final Map<Keyboard, KeyboardGeometrySnapshot> mOriginalGeometry =
            Collections.synchronizedMap(new WeakHashMap<>());
    private float mWidthFactor = -1.0f;
    private float mHeightFactor = -1.0f;

    public ResizeableLeanbackKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPrefs = LeanKeyPreferences.instance(getContext());
        mKeyTextSizeOrigin = mKeyTextSize;
        mModeChangeTextSizeOrigin = mModeChangeTextSize;
    }

    @Override
    public void setKeyboard(Keyboard keyboard) {
        if (keyboard instanceof KeyboardWrapper) {
            keyboard = ((KeyboardWrapper) keyboard).getWrappedKeyboard();
        }

        KeyboardGeometrySnapshot geometry = mOriginalGeometry.get(keyboard);
        if (geometry == null) {
            geometry = new KeyboardGeometrySnapshot(keyboard.getKeys());
            mOriginalGeometry.put(keyboard, geometry);
        } else {
            geometry.restore();
        }

        if (HANDHELD_PACKAGE.equals(getContext().getPackageName())) {
            geometry.compactHandheldSpacing();
        }
        calculateSizeFactors(keyboard);
        float keyLabelScale = HANDHELD_PACKAGE.equals(getContext().getPackageName())
                ? HANDHELD_KEY_LABEL_SCALE : 1.0f;
        mKeyTextSize = Math.round(mKeyTextSizeOrigin * mHeightFactor * keyLabelScale);
        mModeChangeTextSize = Math.round(mModeChangeTextSizeOrigin * mHeightFactor);
        mKeyboardScaleFactor = mWidthFactor;

        if (Math.abs(mWidthFactor - 1.0f) > 0.001f || Math.abs(mHeightFactor - 1.0f) > 0.001f) {
            geometry.scale(mWidthFactor, mHeightFactor);
            KeyboardWrapper wrapper = KeyboardWrapper.from(keyboard, getContext());
            wrapper.setHeightFactor(mHeightFactor);
            wrapper.setWidthFactor(mWidthFactor);
            wrapper.setContentSize(getKeyboardContentWidth(keyboard), getKeyboardContentHeight(keyboard));
            keyboard = wrapper;
        }

        mPaint.setTextSize(mKeyTextSize);

        super.setKeyboard(keyboard);
    }

    private void calculateSizeFactors(Keyboard keyboard) {
        if (!HANDHELD_PACKAGE.equals(getContext().getPackageName())) {
            mWidthFactor = mHeightFactor = mPrefs.getEnlargeKeyboard() ? 1.3f : 1.0f;
            return;
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float availableWidth = metrics.widthPixels;
        float availableHeight = metrics.heightPixels;
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            Point displaySize = new Point();
            if (Build.VERSION.SDK_INT >= 17) {
                display.getRealSize(displaySize);
            } else {
                display.getSize(displaySize);
            }
            if (displaySize.x > 0 && displaySize.y > 0) {
                availableWidth = displaySize.x;
                availableHeight = displaySize.y;
            }
        }

        boolean portrait = availableWidth < availableHeight;
        boolean floating = KeyboardLayoutPreferences.isFloatingKeyboard(getContext());
        String profile = HandheldDisplayProfiles.resolve(
                KeyboardLayoutPreferences.getProfile(getContext()),
                Math.round(availableWidth), Math.round(availableHeight));
        float widthFraction = HandheldDisplayProfiles.widthFraction(profile, portrait, floating);
        int customHeight = KeyboardLayoutPreferences.getKeyboardHeightPercent(getContext());
        int heightPercent = customHeight == KeyboardLayoutPreferences.HEIGHT_AUTO
                ? HandheldDisplayProfiles.defaultHeightPercent(profile) : customHeight;

        // The alpha keyboard reserves a separate action lane in landscape. The
        // edit deck replaces that lane with editing actions and may use the full width.
        float widthBudget = availableWidth * widthFraction;
        float heightBudget = availableHeight * heightPercent / 100.0f;
        float keyboardWidth = Math.max(1, getKeyboardContentWidth(keyboard));
        float widthFactor;
        if (HANDHELD_PACKAGE.equals(getContext().getPackageName()) &&
                shouldReserveHandheldActionRail(keyboard)) {
            float actionSpacing = getResources().getDimension(R.dimen.handheld_action_key_spacing);
            float minimumActionWidth = getResources().getDimension(R.dimen.handheld_action_key_width);
            float actionKeyWidth = getRightArrowWidth(keyboard);
            float scaledActionWidthFactor = actionKeyWidth * 1.25f;
            widthFactor = (widthBudget - actionSpacing) /
                    (keyboardWidth + scaledActionWidthFactor);
            if (scaledActionWidthFactor * widthFactor < minimumActionWidth) {
                widthFactor = (widthBudget - actionSpacing - minimumActionWidth) / keyboardWidth;
            }
        } else {
            widthFactor = widthBudget / keyboardWidth;
        }
        float heightFactor = heightBudget / Math.max(1, getKeyboardContentHeight(keyboard));

        mWidthFactor = clamp(widthFactor, 0.72f, 2.9f);
        mHeightFactor = clamp(heightFactor, 0.72f, 2.1f);
    }

    private boolean shouldReserveHandheldActionRail(Keyboard keyboard) {
        boolean hasEditToggle = false;
        for (Key key : keyboard.getKeys()) {
            if (key.codes == null || key.codes.length == 0) {
                continue;
            }
            int code = key.codes[0];
            if (code >= 0) {
                return true;
            }
            if (code == -16) {
                hasEditToggle = true;
            }
        }
        // handheld_edit.xml contains only command keys and begins with the
        // ABC toggle; every other keyboard keeps the normal action lane.
        return !hasEditToggle;
    }

    private int getKeyboardContentWidth(Keyboard keyboard) {
        int contentWidth = 0;
        for (Key key : keyboard.getKeys()) {
            contentWidth = Math.max(contentWidth, key.x + key.width);
        }
        return contentWidth > 0 ? contentWidth : keyboard.getMinWidth();
    }

    private int getKeyboardContentHeight(Keyboard keyboard) {
        int contentHeight = 0;
        for (Key key : keyboard.getKeys()) {
            contentHeight = Math.max(contentHeight, key.y + key.height);
        }
        return contentHeight > 0 ? contentHeight : keyboard.getHeight();
    }

    private int getRightArrowWidth(Keyboard keyboard) {
        for (Key key : keyboard.getKeys()) {
            if (key.codes != null && key.codes.length > 0 &&
                    key.codes[0] == LeanbackKeyboardView.KEYCODE_RIGHT) {
                return key.width;
            }
        }
        return Math.round(getResources().getDimension(R.dimen.key_width));
    }

    private float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static class KeyboardGeometrySnapshot {
        private final List<Key> keys;
        private final int[] originalX;
        private final int[] originalY;
        private final int[] originalWidth;
        private final int[] originalHeight;
        private final int[] originalGap;

        KeyboardGeometrySnapshot(List<Key> source) {
            keys = new ArrayList<>(source);
            originalX = new int[keys.size()];
            originalY = new int[keys.size()];
            originalWidth = new int[keys.size()];
            originalHeight = new int[keys.size()];
            originalGap = new int[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                Key key = keys.get(i);
                originalX[i] = key.x;
                originalY[i] = key.y;
                originalWidth[i] = key.width;
                originalHeight[i] = key.height;
                originalGap[i] = key.gap;
            }
        }

        void restore() {
            for (int i = 0; i < keys.size(); i++) {
                Key key = keys.get(i);
                key.x = originalX[i];
                key.y = originalY[i];
                key.width = originalWidth[i];
                key.height = originalHeight[i];
                key.gap = originalGap[i];
            }
        }

        void scale(float widthFactor, float heightFactor) {
            for (Key key : keys) {
                key.width = Math.round(key.width * widthFactor);
                key.height = Math.round(key.height * heightFactor);
                key.gap = Math.round(key.gap * widthFactor);
                key.x = Math.round(key.x * widthFactor);
                key.y = Math.round(key.y * heightFactor);
            }
        }

        /**
         * Reflows each source row using smaller visual gutters. Scaling then
         * gives the recovered width to the keys themselves, keeping the grid
         * full width instead of simply making the keyboard narrower.
         */
        void compactHandheldSpacing() {
            List<KeyboardRow> rows = new ArrayList<>();
            for (Key key : keys) {
                KeyboardRow row = null;
                for (KeyboardRow candidate : rows) {
                    if (candidate.originalY == key.y) {
                        row = candidate;
                        break;
                    }
                }
                if (row == null) {
                    row = new KeyboardRow(key.y);
                    rows.add(row);
                }
                row.keys.add(key);
                row.originalBottom = Math.max(row.originalBottom, key.y + key.height);
            }

            int previousOriginalBottom = 0;
            int previousCompactedBottom = 0;
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                KeyboardRow row = rows.get(rowIndex);
                int compactedY;
                if (rowIndex == 0) {
                    compactedY = row.originalY;
                } else {
                    int sourceGap = Math.max(0, row.originalY - previousOriginalBottom);
                    compactedY = previousCompactedBottom + Math.round(sourceGap * HANDHELD_VERTICAL_GAP_FACTOR);
                }

                Key previous = null;
                int previousOriginalRight = 0;
                int previousCompactedRight = 0;
                int rowBottom = compactedY;
                for (Key key : row.keys) {
                    if (previous != null) {
                        int sourceGap = Math.max(0, key.x - previousOriginalRight);
                        int compactedGap = Math.round(sourceGap * HANDHELD_HORIZONTAL_GAP_FACTOR);
                        key.x = previousCompactedRight + compactedGap;
                        key.gap = compactedGap;
                    } else {
                        key.gap = Math.round(key.gap * HANDHELD_HORIZONTAL_GAP_FACTOR);
                    }
                    key.y = compactedY;
                    previous = key;
                    previousOriginalRight = originalX[indexOf(key)] + originalWidth[indexOf(key)];
                    previousCompactedRight = key.x + key.width;
                    rowBottom = Math.max(rowBottom, key.y + key.height);
                }
                previousOriginalBottom = row.originalBottom;
                previousCompactedBottom = rowBottom;
            }
        }

        private int indexOf(Key target) {
            for (int i = 0; i < keys.size(); i++) {
                if (keys.get(i) == target) return i;
            }
            return 0;
        }

        private static final class KeyboardRow {
            final int originalY;
            final List<Key> keys = new ArrayList<>();
            int originalBottom;

            KeyboardRow(int originalY) {
                this.originalY = originalY;
                this.originalBottom = originalY;
            }
        }
    }
}
