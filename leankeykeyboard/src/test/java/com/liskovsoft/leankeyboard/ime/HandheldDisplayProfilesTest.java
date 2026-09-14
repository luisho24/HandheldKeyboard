package com.liskovsoft.leankeyboard.ime;

import com.liskovsoft.leankeyboard.addons.resize.HandheldDisplayProfiles;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HandheldDisplayProfilesTest {
    @Test
    public void landscapeKeyboardUsesNearlyTheFullDisplayWidth() {
        for (String profile : HandheldDisplayProfiles.profileIds()) {
            assertEquals(profile, 0.97f,
                    HandheldDisplayProfiles.widthFraction(profile, false, false), 0.001f);
        }
    }

    @Test
    public void floatingKeyboardKeepsAnInsetWhileUsingMostOfTheWidth() {
        for (String profile : HandheldDisplayProfiles.profileIds()) {
            float full = HandheldDisplayProfiles.widthFraction(profile, false, false);
            float floating = HandheldDisplayProfiles.widthFraction(profile, false, true);
            assertTrue(profile, floating < full);
            assertTrue(profile, floating > 0.85f);
        }
    }
}
