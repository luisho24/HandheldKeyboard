package com.liskovsoft.leankeyboard.ime;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class KeyboardKeyNavigatorTest {
    private final List<String> labels = new ArrayList<>();
    private final List<KeyboardKeyNavigator.KeyBounds> keys = new ArrayList<>();

    @Before
    public void createHandheldLayout() {
        addRow("1234567890⌫", 0);
        addRow("qwertyuiop@", 80);
        addRow("asdfghjkl;&", 160);
        addRow("zxcvbnm,.-/", 240);
        addKey("symbols", 0, 320, 90, 70);
        addKey("shift", 100, 320, 90, 70);
        addKey("language", 200, 320, 90, 70);
        addKey("settings", 300, 320, 90, 70);
        addKey("space", 400, 320, 490, 70);
        addKey("cursor-left", 900, 320, 90, 70);
        addKey("cursor-right", 1000, 320, 90, 70);
    }

    @Test
    public void horizontalNeighborsUseActualKeyBounds() {
        assertMove("4", KeyboardKeyNavigator.Direction.LEFT, "3");
        assertMove("3", KeyboardKeyNavigator.Direction.RIGHT, "4");
        assertMove("f", KeyboardKeyNavigator.Direction.LEFT, "d");
        assertMove("d", KeyboardKeyNavigator.Direction.RIGHT, "f");
    }

    @Test
    public void verticalAndDiagonalRoutesReachReportedKeys() {
        int fromR = move("r", KeyboardKeyNavigator.Direction.UP);
        assertEquals("4", labels.get(fromR));
        assertEquals("3", labels.get(move(fromR, KeyboardKeyNavigator.Direction.LEFT)));

        int fromV = move("v", KeyboardKeyNavigator.Direction.UP);
        assertEquals("f", labels.get(fromV));
        int fromF = move(fromV, KeyboardKeyNavigator.Direction.UP);
        assertEquals("r", labels.get(fromF));
        assertEquals("e", labels.get(move(fromF, KeyboardKeyNavigator.Direction.LEFT)));
    }

    @Test
    public void cursorArrowKeysCanBeReachedFromOneAnother() {
        assertMove("cursor-left", KeyboardKeyNavigator.Direction.RIGHT, "cursor-right");
        assertMove("cursor-right", KeyboardKeyNavigator.Direction.LEFT, "cursor-left");
        assertMove("cursor-left", KeyboardKeyNavigator.Direction.LEFT, "space");
    }

    @Test
    public void handheldBottomRowMatchesTheVoiceFreeKeyboardLayout() {
        assertMove("space", KeyboardKeyNavigator.Direction.RIGHT, "cursor-left");
        assertMove("space", KeyboardKeyNavigator.Direction.LEFT, "settings");
        assertMove("settings", KeyboardKeyNavigator.Direction.RIGHT, "space");
    }

    @Test
    public void diagonalMoveSelectsTheNearestKeyInThatQuadrant() {
        assertMove("r", KeyboardKeyNavigator.Direction.UP_LEFT, "3");
    }

    private void assertMove(String source, KeyboardKeyNavigator.Direction direction, String target) {
        assertEquals(target, labels.get(move(source, direction)));
    }

    private int move(String source, KeyboardKeyNavigator.Direction direction) {
        return move(labels.indexOf(source), direction);
    }

    private int move(int sourceIndex, KeyboardKeyNavigator.Direction direction) {
        int target = KeyboardKeyNavigator.findNext(keys, sourceIndex, direction);
        if (target < 0) {
            throw new AssertionError("No target from " + labels.get(sourceIndex) + " toward " + direction);
        }
        return target;
    }

    private void addRow(String row, int top) {
        for (int i = 0; i < row.length(); i++) {
            addKey(String.valueOf(row.charAt(i)), i * 100, top, 90, 70);
        }
    }

    private void addKey(String label, int left, int top, int width, int height) {
        labels.add(label);
        keys.add(new KeyboardKeyNavigator.KeyBounds(left, top, left + width, top + height));
    }
}
