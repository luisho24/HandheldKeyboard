package com.liskovsoft.leankeyboard.utils;

/** Unicode helpers for text editing actions issued by the keyboard. */
public final class UnicodeTextUtils {
    private static final int ZERO_WIDTH_JOINER = 0x200D;

    private UnicodeTextUtils() {
    }

    /**
     * Returns the UTF-16 length of the final user-perceived character in a text prefix.
     * This keeps backspace from splitting surrogate pairs, combining sequences, flags,
     * skin-tone emoji, keycaps, emoji tag sequences, and emoji joined with a ZWJ.
     */
    public static int previousGraphemeLength(CharSequence text) {
        if (text == null || text.length() == 0) {
            return 0;
        }

        int end = text.length();
        int cursor = end;
        int current = Character.codePointBefore(text, cursor);
        cursor -= Character.charCount(current);

        while (cursor > 0) {
            int previous = Character.codePointBefore(text, cursor);

            if (isGraphemeExtend(current) && current != ZERO_WIDTH_JOINER) {
                cursor -= Character.charCount(previous);
                current = previous;
                continue;
            }

            if (current == ZERO_WIDTH_JOINER) {
                cursor -= Character.charCount(previous);
                current = previous;
                continue;
            }

            if (previous == ZERO_WIDTH_JOINER) {
                cursor -= Character.charCount(previous);
                if (cursor == 0) {
                    break;
                }
                current = Character.codePointBefore(text, cursor);
                cursor -= Character.charCount(current);
                continue;
            }

            if (current == '\n' && previous == '\r') {
                cursor -= Character.charCount(previous);
                break;
            }

            if (isRegionalIndicator(current) && isRegionalIndicator(previous)) {
                int runStart = cursor;
                int runLength = 1;
                while (runStart > 0) {
                    int preceding = Character.codePointBefore(text, runStart);
                    if (!isRegionalIndicator(preceding)) {
                        break;
                    }
                    runStart -= Character.charCount(preceding);
                    runLength++;
                }

                // Regional indicators pair from the start of their contiguous run. If
                // that run has an odd length, its final grapheme is a single indicator.
                int lastGraphemeLength = runLength % 2 == 0 ? 4 : 2;
                int suffixLength = end - (cursor + Character.charCount(current));
                return suffixLength + lastGraphemeLength;
            }

            break;
        }

        return end - cursor;
    }

    private static boolean isGraphemeExtend(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK ||
                type == Character.COMBINING_SPACING_MARK ||
                type == Character.ENCLOSING_MARK ||
                codePoint == 0x200C ||
                (codePoint >= 0xFE00 && codePoint <= 0xFE0F) ||
                (codePoint >= 0xE0100 && codePoint <= 0xE01EF) ||
                (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF) ||
                (codePoint >= 0xE0020 && codePoint <= 0xE007F);
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }
}
