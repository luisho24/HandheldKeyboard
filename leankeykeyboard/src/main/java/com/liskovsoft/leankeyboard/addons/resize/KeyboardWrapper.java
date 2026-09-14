package com.liskovsoft.leankeyboard.addons.resize;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import androidx.annotation.Nullable;
import com.liskovsoft.leankeyboard.ime.LeanbackKeyboardContainer;
import com.liskovsoft.leankeykeyboard.R;

import java.util.List;

public class KeyboardWrapper extends Keyboard {
    private Keyboard mKeyboard;
    private int mHeight = -1;
    private float mHeightFactor = 1.0f;
    private float mWidthFactor = 1.0f;

    public KeyboardWrapper(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    public KeyboardWrapper(Context context, int xmlLayoutResId, int modeId, int width, int height) {
        super(context, xmlLayoutResId, modeId, width, height);
    }

    public KeyboardWrapper(Context context, int xmlLayoutResId, int modeId) {
        super(context, xmlLayoutResId, modeId);
    }

    public KeyboardWrapper(Context context, int layoutTemplateResId, CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
    }

    public static KeyboardWrapper from(Keyboard keyboard, Context context) {
        KeyboardWrapper wrapper = new KeyboardWrapper(context, R.xml.empty_kbd);
        wrapper.mKeyboard = keyboard;

        return wrapper;
    }

    Keyboard getWrappedKeyboard() {
        return mKeyboard;
    }

    @Override
    public List<Key> getKeys() {
        return mKeyboard.getKeys();
    }

    @Override
    public List<Key> getModifierKeys() {
        return mKeyboard.getModifierKeys();
    }

    @Override
    public int getHeight() {
        int contentHeight = 0;
        for (Key key : mKeyboard.getKeys()) {
            contentHeight = Math.max(contentHeight, key.y + key.height);
        }
        return Math.max((int)(mKeyboard.getHeight() * mHeightFactor), contentHeight);
    }

    @Override
    public int getMinWidth() {
        int contentWidth = 0;
        for (Key key : mKeyboard.getKeys()) {
            contentWidth = Math.max(contentWidth, key.x + key.width);
        }
        return Math.max((int)(mKeyboard.getMinWidth() * mWidthFactor), contentWidth);
    }

    @Override
    public boolean setShifted(boolean shiftState) {
        return mKeyboard.setShifted(shiftState);
    }

    @Override
    public boolean isShifted() {
        return mKeyboard.isShifted();
    }

    @Override
    public int getShiftKeyIndex() {
        return mKeyboard.getShiftKeyIndex();
    }

    @Override
    public int[] getNearestKeys(int x, int y) {
        // Keyboard builds its proximity grid before the wrapped key geometry is scaled.
        // Query that grid in its original coordinate space; callers still validate the
        // returned candidates against the scaled Key bounds exposed by this wrapper.
        int originalX = (int) Math.floor(x / Math.max(0.01f, mWidthFactor));
        int originalY = (int) Math.floor(y / Math.max(0.01f, mHeightFactor));
        return mKeyboard.getNearestKeys(originalX, originalY);
    }

    public void setHeightFactor(float factor) {
        mHeightFactor = factor;
    }

    public void setWidthFactor(float factor) {
        mWidthFactor = factor;
    }

    /**
     * Wrapper fix: {@link LeanbackKeyboardContainer#onModeChangeClick}
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Keyboard) {
            return mKeyboard.equals(obj);
        }

        return false;
    }
}
