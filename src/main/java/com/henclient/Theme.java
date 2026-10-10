package com.henclient;

/** Colors and the animated accent. All colors are ARGB. */
public final class Theme {
    private Theme() {}

    public static int index = 0;

    public static final String[] NAMES = {
            "Crimson", "Ocean", "Violet", "Emerald", "Sunset", "Rainbow"
    };

    private static final int[][] PAIRS = {
            {0xFFFF5252, 0xFFC62828},
            {0xFF40C4FF, 0xFF2979FF},
            {0xFFB388FF, 0xFF7C4DFF},
            {0xFF69F0AE, 0xFF00BFA5},
            {0xFFFFB74D, 0xFFFF5252},
            {0xFFFFFFFF, 0xFFFFFFFF}
    };

    public static final int BG = 0xE8111116;
    public static final int PILL = 0xB0000000;
    public static final int ROW = 0xFF1C1C24;
    public static final int ROW_HOVER = 0xFF262630;
    public static final int ROW2 = 0xFF15151B;
    public static final int TEXT = 0xFFEDEDF2;
    public static final int SUBTLE = 0xFFB4B4C0;
    public static final int MUTED = 0xFF8E8E9C;
    public static final int OFF = 0xFF383844;
    public static final int GOOD = 0xFF69F0AE;
    public static final int WARN = 0xFFFFD54F;
    public static final int BAD = 0xFFFF5252;

    public static int accent() {
        return accent(0.0);
    }

    /** Accent color; the offset shifts it along the animation so neighbouring rows differ. */
    public static int accent(double offset) {
        double t = System.currentTimeMillis();
        if (index == NAMES.length - 1) {
            double hue = ((t / 5000.0) + offset * 0.12) % 1.0;
            if (hue < 0) hue += 1.0;
            return hsv((float) hue, 0.55f, 1.0f);
        }
        int[] pair = PAIRS[Math.max(0, Math.min(index, PAIRS.length - 1))];
        double k = 0.5 + 0.5 * Math.sin(t / 650.0 + offset);
        return lerp(pair[0], pair[1], k);
    }

    public static int preview(int i) {
        if (i == NAMES.length - 1) return hsv(0.8f, 0.55f, 1.0f);
        return PAIRS[Math.max(0, Math.min(i, PAIRS.length - 1))][0];
    }

    public static int lerp(int a, int b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    public static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0xFFFFFF);
    }

    public static int hsv(float h, float s, float v) {
        float hh = (h - (float) Math.floor(h)) * 6.0f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000
                | (Math.round(r * 255) << 16)
                | (Math.round(g * 255) << 8)
                | Math.round(b * 255);
    }
}
