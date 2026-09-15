package com.liskovsoft.leankeyboard.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PointerChordTest {
    private static final int M1 = 188;
    private static final int M4 = 191;

    @Test
    public void togglesOnceForEachCompleteChord() {
        PointerChord chord = new PointerChord();
        assertFalse(chord.onKeyEvent(M1, true, M1, M4));
        assertTrue(chord.onKeyEvent(M4, true, M1, M4));
        assertFalse(chord.onKeyEvent(M4, true, M1, M4));
        assertFalse(chord.onKeyEvent(M4, false, M1, M4));
        assertFalse(chord.onKeyEvent(M1, false, M1, M4));
        assertFalse(chord.onKeyEvent(M4, true, M1, M4));
        assertTrue(chord.onKeyEvent(M1, true, M1, M4));
    }

    @Test
    public void ignoresButtonsOutsideTheConfiguredChord() {
        PointerChord chord = new PointerChord();
        assertFalse(chord.handles(96, M1, M4));
        assertFalse(chord.onKeyEvent(96, true, M1, M4));
        assertTrue(chord.handles(M1, M1, M4));
    }
}
