package com.liskovsoft.leankeyboard.utils;

/** Tracks a two-button chord and emits exactly one toggle per complete press. */
public final class PointerChord {
    private boolean mFirstHeld;
    private boolean mSecondHeld;
    private boolean mConsumed;

    /** @return true exactly once when both chord buttons are held. */
    public boolean onKeyEvent(int keyCode, boolean down, int firstKey, int secondKey) {
        if (keyCode != firstKey && keyCode != secondKey) return false;
        if (keyCode == firstKey) mFirstHeld = down;
        if (keyCode == secondKey) mSecondHeld = down;
        boolean toggled = down && mFirstHeld && mSecondHeld && !mConsumed;
        if (toggled) mConsumed = true;
        if (!mFirstHeld && !mSecondHeld) mConsumed = false;
        return toggled;
    }

    public boolean handles(int keyCode, int firstKey, int secondKey) {
        return keyCode == firstKey || keyCode == secondKey;
    }
}
