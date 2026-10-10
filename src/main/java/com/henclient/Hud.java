package com.henclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Draws the watermark, the info panel (top-left) and the module list (top-right). */
public final class Hud {
    public static boolean watermark = true;
    public static boolean arrayList = true;

    public record Line(String label, String value, int color) {}

    private static final Minecraft MC = Minecraft.getInstance();

    private Hud() {}

    public static void render(GuiGraphics g) {
        int sw = MC.getWindow().getGuiScaledWidth();

        int y = 6;
        if (watermark) {
            y = drawWatermark(g, 6, y) + 4;
        }
        drawInfoPanel(g, 6, y);
        if (arrayList) {
            drawModuleList(g, sw);
        }
    }

    private static int drawWatermark(GuiGraphics g, int x, int y) {
        String a = "HEN";
        String b = " CLIENT";
        int w = Gfx.width(a + b) + 14;
        int h = 15;
        Gfx.roundRect(g, x, y, x + w, y + h, 3, Theme.BG);
        Gfx.accentBar(g, x + 3, x + w - 3, y, 1);
        Gfx.gradientText(g, a, x + 7, y + 4, 0.6);
        Gfx.text(g, b, x + 7 + Gfx.width(a), y + 4, Theme.TEXT);
        return y + h;
    }

    private static void drawInfoPanel(GuiGraphics g, int x, int y) {
        List<Line> lines = new ArrayList<>();
        for (Feature f : ModuleManager.all()) {
            if (f.enabled) {
                ModuleManager.guard(f, () -> f.hudLines(lines));
            }
        }
        if (lines.isEmpty()) return;

        int rowH = 11;
        int widest = 0;
        for (Line l : lines) {
            widest = Math.max(widest, Gfx.width(l.label()) + 10 + Gfx.width(l.value()));
        }
        int w = widest + 16;
        int h = lines.size() * rowH + 7;

        Gfx.roundRect(g, x, y, x + w, y + h, 3, Theme.BG);
        // accent strip down the left edge, animated per row
        for (int i = 0; i < lines.size(); i++) {
            int top = y + 3 + i * rowH;
            g.fill(x, top, x + 2, top + rowH, Theme.accent(i * 0.5));
        }

        int ty = y + 4;
        for (Line l : lines) {
            Gfx.text(g, l.label(), x + 7, ty, Theme.MUTED);
            Gfx.textRight(g, l.value(), x + w - 6, ty, l.color());
            ty += rowH;
        }
    }

    private static void drawModuleList(GuiGraphics g, int sw) {
        List<Feature> active = new ArrayList<>();
        for (Feature f : ModuleManager.all()) {
            if (f.enabled && f.category != Category.HUD) active.add(f);
        }
        if (active.isEmpty()) return;

        active.sort(Comparator.comparingInt((Feature f) -> -textWidth(f)));

        // Vanilla draws effect icons in the top-right corner; stay below them.
        int y = 4;
        if (MC.player != null && !MC.player.getActiveEffects().isEmpty()) {
            y = 56;
        }

        int i = 0;
        for (Feature f : active) {
            String info = f.info();
            int tw = textWidth(f);
            int x2 = sw - 4;
            int x1 = x2 - tw - 12;
            int rowH = 12;

            Gfx.roundRect(g, x1, y, x2, y + rowH, 2, Theme.PILL);
            int accent = Theme.accent(i * 0.45);
            g.fill(x2 - 2, y, x2, y + rowH, accent);

            Gfx.text(g, f.name, x1 + 5, y + 2, accent);
            if (info != null) {
                Gfx.text(g, info, x1 + 5 + Gfx.width(f.name) + 4, y + 2, Theme.MUTED);
            }
            y += rowH + 1;
            i++;
        }
    }

    private static int textWidth(Feature f) {
        String info = f.info();
        return Gfx.width(f.name) + (info == null ? 0 : 4 + Gfx.width(info));
    }
}
