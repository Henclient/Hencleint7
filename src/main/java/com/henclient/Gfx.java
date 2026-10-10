package com.henclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Small 2D drawing helpers built only on GuiGraphics.fill / drawString. */
public final class Gfx {
    private static final Minecraft MC = Minecraft.getInstance();

    private Gfx() {}

    // ---- text --------------------------------------------------------

    public static int width(String s) {
        return MC.font.width(s);
    }

    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(MC.font, s, x, y, color);
    }

    public static void textRight(GuiGraphics g, String s, int xRight, int y, int color) {
        g.drawString(MC.font, s, xRight - width(s), y, color);
    }

    public static void textCentered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(MC.font, s, cx - width(s) / 2, y, color);
    }

    /** Draws text letter by letter, each in the animated accent. */
    public static void gradientText(GuiGraphics g, String s, int x, int y, double step) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            g.drawString(MC.font, ch, cx, y, Theme.accent(i * step));
            cx += width(ch);
        }
    }

    /** Shortens a string with ".." so it fits in maxWidth pixels. */
    public static String fit(String s, int maxWidth) {
        if (width(s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && width(t + "..") > maxWidth) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "..";
    }

    // ---- shapes ------------------------------------------------------

    public static void rect(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    /** Rectangle with softly cut corners. */
    public static void roundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }
        g.fill(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            g.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            g.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    public static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        g.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    /** Horizontal strip that runs through the accent animation. */
    public static void accentBar(GuiGraphics g, int x1, int x2, int y, int thickness) {
        for (int x = x1; x < x2; x += 2) {
            g.fill(x, y, Math.min(x + 2, x2), y + thickness, Theme.accent((x - x1) / 40.0));
        }
    }

    public static void toggle(GuiGraphics g, int x, int y, boolean on) {
        roundRect(g, x, y, x + 26, y + 12, 6, on ? Theme.accent() : Theme.OFF);
        int kx = on ? x + 16 : x + 2;
        roundRect(g, kx, y + 2, kx + 8, y + 10, 4, 0xFFFFFFFF);
    }

    /** Text on a dark pill, centered on cx. */
    public static void label(GuiGraphics g, String s, int cx, int y, int color) {
        int w = width(s);
        roundRect(g, cx - w / 2 - 3, y - 2, cx - w / 2 + w + 4, y + 10, 2, Theme.PILL);
        g.drawString(MC.font, s, cx - w / 2, y, color);
    }

    // ---- lines -------------------------------------------------------

    /** Line clipped to the screen, drawn as a string of small squares. */
    public static void line(GuiGraphics g, double x1, double y1, double x2, double y2, int color) {
        double sw = MC.getWindow().getGuiScaledWidth();
        double sh = MC.getWindow().getGuiScaledHeight();
        double[] c = clip(x1, y1, x2, y2, 0, 0, sw, sh);
        if (c == null) return;
        double dx = c[2] - c[0];
        double dy = c[3] - c[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, (int) Math.ceil(len / 2.0));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int px = (int) Math.round(c[0] + dx * t);
            int py = (int) Math.round(c[1] + dy * t);
            g.fill(px, py, px + 2, py + 2, color);
        }
    }

    /** Liang-Barsky line clipping. Returns {x1,y1,x2,y2} or null if fully outside. */
    private static double[] clip(double x0, double y0, double x1, double y1,
                                 double xmin, double ymin, double xmax, double ymax) {
        double t0 = 0.0;
        double t1 = 1.0;
        double dx = x1 - x0;
        double dy = y1 - y0;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x0 - xmin, xmax - x0, y0 - ymin, ymax - y0};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0.0) {
                if (q[i] < 0.0) return null;
            } else {
                double r = q[i] / p[i];
                if (p[i] < 0.0) {
                    if (r > t1) return null;
                    if (r > t0) t0 = r;
                } else {
                    if (r < t0) return null;
                    if (r < t1) t1 = r;
                }
            }
        }
        return new double[]{x0 + t0 * dx, y0 + t0 * dy, x0 + t1 * dx, y0 + t1 * dy};
    }

    // ---- ESP ---------------------------------------------------------

    private static int clampI(double v) {
        return (int) Math.max(-4000.0, Math.min(4000.0, v));
    }

    /** Draws an ESP box for a projected rectangle {minX,minY,maxX,maxY}; returns the final int rect. */
    public static int[] espBox(GuiGraphics g, double[] b, int color, boolean corners) {
        int x1 = clampI(b[0]);
        int y1 = clampI(b[1]);
        int x2 = clampI(b[2]);
        int y2 = clampI(b[3]);
        if (x2 - x1 < 4) {
            int cx = (x1 + x2) / 2;
            x1 = cx - 2;
            x2 = cx + 2;
        }
        if (y2 - y1 < 4) {
            int cy = (y1 + y2) / 2;
            y1 = cy - 2;
            y2 = cy + 2;
        }

        if (corners) {
            int len = Math.max(4, Math.min(x2 - x1, y2 - y1) / 3);
            cornerSet(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, len + 1, 0xC0000000);
            cornerSet(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, len - 1, 0xC0000000);
            cornerSet(g, x1, y1, x2, y2, len, color);
        } else {
            g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Theme.withAlpha(color, 0x18));
            outline(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xC0000000);
            outline(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xC0000000);
            outline(g, x1, y1, x2, y2, color);
        }
        return new int[]{x1, y1, x2, y2};
    }

    private static void cornerSet(GuiGraphics g, int x1, int y1, int x2, int y2, int len, int color) {
        len = Math.max(2, len);
        g.fill(x1, y1, x1 + len, y1 + 1, color);
        g.fill(x1, y1 + 1, x1 + 1, y1 + len, color);
        g.fill(x2 - len, y1, x2, y1 + 1, color);
        g.fill(x2 - 1, y1 + 1, x2, y1 + len, color);
        g.fill(x1, y2 - 1, x1 + len, y2, color);
        g.fill(x1, y2 - len, x1 + 1, y2 - 1, color);
        g.fill(x2 - len, y2 - 1, x2, y2, color);
        g.fill(x2 - 1, y2 - len, x2, y2 - 1, color);
    }

    public static int healthColor(float frac) {
        return Theme.lerp(0xFFFF5252, 0xFF69F0AE, frac);
    }

    /** Vertical health bar hugging the left side of a box whose left edge is x. */
    public static void healthBar(GuiGraphics g, int x, int y1, int y2, float frac) {
        frac = Math.max(0.0f, Math.min(1.0f, frac));
        g.fill(x - 5, y1 - 1, x - 1, y2 + 1, 0xC0000000);
        int h = Math.round((y2 - y1) * frac);
        g.fill(x - 4, y2 - h, x - 2, y2, healthColor(frac));
    }

    public static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }
}
