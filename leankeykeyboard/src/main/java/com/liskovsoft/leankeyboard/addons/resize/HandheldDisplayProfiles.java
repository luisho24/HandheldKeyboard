package com.liskovsoft.leankeyboard.addons.resize;

/** Aspect-ratio profiles for common Android handheld and PC handheld displays. */
public final class HandheldDisplayProfiles {
    public static final String AUTO = "auto";
    public static final String FOUR_THREE = "4_3";
    public static final String SQUARE = "square";
    public static final String SIXTEEN_TEN = "16_10";
    public static final String SIXTEEN_NINE = "16_9";
    public static final String ULTRAWIDE = "20_9";

    private HandheldDisplayProfiles() { }

    public static String[] profileIds() {
        return new String[]{AUTO, FOUR_THREE, SQUARE, SIXTEEN_TEN, SIXTEEN_NINE, ULTRAWIDE};
    }

    public static int profileIndex(String profile) {
        String[] profiles = profileIds();
        for (int i = 0; i < profiles.length; i++) {
            if (profiles[i].equals(profile)) return i;
        }
        return 0;
    }

    public static String resolve(String selectedProfile, int width, int height) {
        if (selectedProfile != null && selectedProfile.length() > 0 && !AUTO.equals(selectedProfile)) {
            return selectedProfile;
        }

        float shortSide = Math.max(1, Math.min(width, height));
        float longSide = Math.max(width, height);
        float ratio = longSide / shortSide;
        if (ratio >= 1.92f) {
            return ULTRAWIDE;
        }
        if (ratio >= 1.70f) {
            return SIXTEEN_NINE;
        }
        if (ratio >= 1.47f) {
            return SIXTEEN_TEN;
        }
        if (ratio >= 1.18f) {
            return FOUR_THREE;
        }
        return SQUARE;
    }

    public static int defaultHeightPercent(String profile) {
        if (ULTRAWIDE.equals(profile)) return 38;
        if (SIXTEEN_TEN.equals(profile)) return 43;
        if (SIXTEEN_NINE.equals(profile)) return 41;
        if (FOUR_THREE.equals(profile)) return 45;
        if (SQUARE.equals(profile)) return 46;
        return 41;
    }

    public static float widthFraction(String profile, boolean portrait, boolean floating) {
        float fraction;
        if (portrait) {
            fraction = SQUARE.equals(profile) ? 0.94f : 0.97f;
        } else if (ULTRAWIDE.equals(profile)) {
            fraction = 0.66f;
        } else if (SIXTEEN_TEN.equals(profile)) {
            fraction = 0.80f;
        } else if (SIXTEEN_NINE.equals(profile)) {
            fraction = 0.74f;
        } else if (FOUR_THREE.equals(profile)) {
            fraction = 0.90f;
        } else {
            fraction = 0.94f;
        }

        if (floating) {
            fraction *= portrait ? 0.87f : 0.91f;
        }
        return fraction;
    }
}
