package com.liskovsoft.leankeyboard.ime;

import java.util.List;

/** Selects a keyboard key by its real rectangle rather than a uniform row/column grid. */
final class KeyboardKeyNavigator {
    enum Direction {
        LEFT, RIGHT, UP, DOWN, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
    }

    static final class KeyBounds {
        final float left;
        final float top;
        final float right;
        final float bottom;

        KeyBounds(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        float centerX() {
            return (left + right) / 2f;
        }

        float centerY() {
            return (top + bottom) / 2f;
        }
    }

    private KeyboardKeyNavigator() {
    }

    static int findNext(List<KeyBounds> keys, int sourceIndex, Direction direction) {
        if (keys == null || sourceIndex < 0 || sourceIndex >= keys.size() || direction == null) {
            return -1;
        }

        KeyBounds source = keys.get(sourceIndex);
        if (source == null) {
            return -1;
        }

        boolean left = direction == Direction.LEFT || direction == Direction.UP_LEFT || direction == Direction.DOWN_LEFT;
        boolean right = direction == Direction.RIGHT || direction == Direction.UP_RIGHT || direction == Direction.DOWN_RIGHT;
        boolean up = direction == Direction.UP || direction == Direction.UP_LEFT || direction == Direction.UP_RIGHT;
        boolean down = direction == Direction.DOWN || direction == Direction.DOWN_LEFT || direction == Direction.DOWN_RIGHT;
        boolean horizontal = left || right;
        boolean vertical = up || down;
        boolean diagonal = horizontal && vertical;

        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        int bestAlignedIndex = -1;
        float bestAlignedScore = Float.MAX_VALUE;
        boolean cardinal = !diagonal;
        for (int i = 0; i < keys.size(); i++) {
            KeyBounds candidate = keys.get(i);
            if (i == sourceIndex || candidate == null || candidate.right <= candidate.left || candidate.bottom <= candidate.top) {
                continue;
            }

            float dx = candidate.centerX() - source.centerX();
            float dy = candidate.centerY() - source.centerY();
            if ((left && dx >= 0f) || (right && dx <= 0f) || (up && dy >= 0f) || (down && dy <= 0f)) {
                continue;
            }

            float primaryDistance;
            float crossDistance;
            float overlap;
            boolean aligned = false;
            if (diagonal) {
                primaryDistance = Math.abs(dx) + Math.abs(dy);
                crossDistance = 0f;
                overlap = 0f;
            } else if (horizontal) {
                primaryDistance = Math.abs(dx);
                crossDistance = Math.abs(dy);
                overlap = Math.max(0f, Math.min(source.bottom, candidate.bottom) - Math.max(source.top, candidate.top));
                aligned = candidate.top <= source.centerY() && candidate.bottom >= source.centerY();
            } else {
                primaryDistance = Math.abs(dy);
                crossDistance = Math.abs(dx);
                overlap = Math.max(0f, Math.min(source.right, candidate.right) - Math.max(source.left, candidate.left));
                aligned = candidate.left <= source.centerX() && candidate.right >= source.centerX();
            }

            // Favor close forward keys and penalize diagonal jumps in the fallback case.
            float score = primaryDistance + crossDistance * 1.5f - Math.min(20f, overlap * 0.15f);
            if (score < bestScore) {
                bestIndex = i;
                bestScore = score;
            }

            // A wide key such as Space can have a closer key diagonally above it (for
            // example M) even though a control key sits directly beside it in the same
            // row. Prefer candidates crossed by the source centerline whenever one exists;
            // retain the weighted fallback for staggered rows with no aligned neighbor.
            if (cardinal && aligned && primaryDistance < bestAlignedScore) {
                bestAlignedIndex = i;
                bestAlignedScore = primaryDistance;
            }
        }

        return bestAlignedIndex >= 0 ? bestAlignedIndex : bestIndex;
    }
}
