package com.liskovsoft.leankeyboard.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnicodeTextUtilsTest {
    @Test
    public void removesSupplementaryEmojiAsOneCharacter() {
        assertEquals(2, UnicodeTextUtils.previousGraphemeLength("text😀"));
    }

    @Test
    public void removesCombiningAccentWithItsBase() {
        assertEquals(2, UnicodeTextUtils.previousGraphemeLength("cafe\u0301"));
    }

    @Test
    public void doesNotAttachAnEarlierCombiningMarkToTheFollowingCharacter() {
        assertEquals(1, UnicodeTextUtils.previousGraphemeLength("cafe\u0301x"));
    }

    @Test
    public void removesSkinToneAndKeycapSequencesWhole() {
        assertEquals("👍🏽".length(), UnicodeTextUtils.previousGraphemeLength("👍🏽"));
        assertEquals("1️⃣".length(), UnicodeTextUtils.previousGraphemeLength("1️⃣"));
    }

    @Test
    public void removesJoinedFamilyEmojiWhole() {
        String family = "👨‍👩‍👧‍👦";
        assertEquals(family.length(), UnicodeTextUtils.previousGraphemeLength("x" + family));
    }

    @Test
    public void removesRegionalIndicatorFlagsInPairs() {
        String flag = "🇺🇸";
        assertEquals(flag.length(), UnicodeTextUtils.previousGraphemeLength("x" + flag));
        assertEquals(2, UnicodeTextUtils.previousGraphemeLength("🇺🇸🇨"));
        assertEquals(4, UnicodeTextUtils.previousGraphemeLength("🇺🇸🇨🇦"));
    }
}
