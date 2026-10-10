package com.henclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClickGui extends Screen {
    private static final Minecraft MC = Minecraft.getInstance();

    private static final int MAX_W = 500;
    private static final int ROW_H = 30;
    private static final int SET_H = 20;
    private static final Category[] TABS = Category.values();

    private interface Click {
        void on(double mx, double my, int button);
    }

    private record Hit(int x1, int y1, int x2, int y2, Click click) {}

    private final List<Hit> hits = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();

    private int tab = 0;               // 0..TABS.length-1 = categories, TABS.length = settings tab
    private int scroll = 0;
    private Feature binding = null;
    private boolean bindingGui = false;
    private Setting dragging = null;
    private int dragX1 = 0;
    private int dragX2 = 1;

    public ClickGui() {
        super(Component.literal("Hen Client"));
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        hits.clear();

        // Sliders are dragged by polling the mouse button, so we need no drag/release callbacks.
        boolean mouseDown = GLFW.glfwGetMouseButton(
                MC.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (dragging != null) {
            if (mouseDown) {
                applyDrag(mouseX);
            } else {
                dragging = null;
                Config.save();
            }
        }

        int pw = Math.min(MAX_W, width - 16);
        int ph = Math.min(height - 20, 380);
        int left = (width - pw) / 2;
        int top = (height - ph) / 2;

        // frame
        Gfx.roundRect(g, left - 1, top - 1, left + pw + 1, top + ph + 1, 6, 0x66000000);
        Gfx.roundRect(g, left, top, left + pw, top + ph, 5, Theme.BG);

        // header
        Gfx.gradientText(g, "HEN CLIENT", left + 14, top + 11, 0.5);
        Gfx.text(g, "v" + HenClient.VERSION + "  |  Fabric 1.21.11", left + 100, top + 11, Theme.MUTED);
        Gfx.textRight(g, "click = toggle   right-click = settings", left + pw - 14, top + 11, Theme.MUTED);
        Gfx.accentBar(g, left + 10, left + pw - 10, top + 28, 2);

        drawSidebar(g, left, top, mouseX, mouseY);

        int cx1 = left + 124;
        int cx2 = left + pw - 12;
        int cTop = top + 38;
        int cBottom = top + ph - 10;

        int y = cTop - scroll;
        if (tab < TABS.length) {
            for (Feature f : ModuleManager.in(TABS[tab])) {
                y = drawFeature(g, f, cx1, cx2, y, cTop, cBottom, mouseX, mouseY);
            }
        } else {
            y = drawSettingsTab(g, cx1, cx2, y, cTop, cBottom, mouseX, mouseY);
        }

        int contentH = y + scroll - cTop;
        int maxScroll = Math.max(0, contentH - (cBottom - cTop));
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
    }

    private void drawSidebar(GuiGraphics g, int left, int top, int mouseX, int mouseY) {
        int x1 = left + 10;
        int x2 = left + 114;
        int y = top + 38;

        for (int i = 0; i <= TABS.length; i++) {
            final int index = i;
            String name = i < TABS.length ? TABS[i].label : "Settings";
            boolean active = tab == i;
            boolean hover = inside(mouseX, mouseY, x1, y, x2, y + 22);

            int bg = active ? Theme.withAlpha(Theme.accent(), 0x40)
                    : (hover ? Theme.ROW_HOVER : Theme.ROW2);
            Gfx.roundRect(g, x1, y, x2, y + 22, 4, bg);
            if (active) {
                Gfx.roundRect(g, x1, y + 4, x1 + 3, y + 18, 1, Theme.accent());
            }
            Gfx.text(g, name, x1 + 12, y + 7, active ? Theme.TEXT : Theme.SUBTLE);

            if (i < TABS.length) {
                int on = 0;
                for (Feature f : ModuleManager.in(TABS[i])) {
                    if (f.enabled) on++;
                }
                if (on > 0) {
                    Gfx.textRight(g, Integer.toString(on), x2 - 8, y + 7, Theme.accent());
                }
            }

            hits.add(new Hit(x1, y, x2, y + 22, (lx, ly, lb) -> {
                tab = index;
                scroll = 0;
            }));
            y += 26;
        }
    }

    private int drawFeature(GuiGraphics g, Feature f, int x1, int x2, int y,
                            int cTop, int cBottom, int mx, int my) {
        if (visible(y, ROW_H, cTop, cBottom)) {
            boolean hover = inside(mx, my, x1, y, x2, y + ROW_H);
            Gfx.roundRect(g, x1, y, x2, y + ROW_H, 4, hover ? Theme.ROW_HOVER : Theme.ROW);
            if (f.enabled) {
                Gfx.roundRect(g, x1, y + 6, x1 + 3, y + ROW_H - 6, 1, Theme.accent());
            }

            int sx2 = x2 - 10;
            int sx1 = sx2 - 26;
            int kx2 = sx1 - 8;
            int kx1 = kx2 - 58;
            int bx2 = kx1 - 6;
            int bx1 = bx2 - 18;
            boolean hasSettings = !f.settings.isEmpty();
            int textRight = (hasSettings ? bx1 : kx1) - 8;

            Gfx.text(g, Gfx.fit(f.name, textRight - (x1 + 10)), x1 + 10, y + 6,
                    f.enabled ? Theme.TEXT : Theme.SUBTLE);
            Gfx.text(g, Gfx.fit(f.description, textRight - (x1 + 10)), x1 + 10, y + 17, Theme.MUTED);

            Gfx.toggle(g, sx1, y + 9, f.enabled);

            boolean bindingThis = binding == f;
            Gfx.roundRect(g, kx1, y + 6, kx2, y + 24, 3, bindingThis ? Theme.accent() : Theme.ROW2);
            Gfx.textCentered(g, bindingThis ? "..." : keyName(f.key), (kx1 + kx2) / 2, y + 11,
                    bindingThis ? 0xFF101014 : Theme.MUTED);

            if (hasSettings) {
                Gfx.roundRect(g, bx1, y + 6, bx2, y + 24, 3, f.expanded ? Theme.accent() : Theme.ROW2);
                Gfx.textCentered(g, f.expanded ? "-" : "+", (bx1 + bx2) / 2, y + 11,
                        f.expanded ? 0xFF101014 : Theme.SUBTLE);
            }

            // Whole row first, specific controls after (later hits are tested first).
            hits.add(new Hit(x1, y, x2, y + ROW_H, (lx, ly, lb) -> {
                if (lb == 0) {
                    f.toggle();
                    Config.save();
                } else if (lb == 1 && !f.settings.isEmpty()) {
                    f.expanded = !f.expanded;
                }
            }));
            hits.add(new Hit(kx1, y + 6, kx2, y + 24, (lx, ly, lb) -> {
                if (lb == 0) {
                    binding = f;
                    bindingGui = false;
                }
            }));
            if (hasSettings) {
                hits.add(new Hit(bx1, y + 6, bx2, y + 24, (lx, ly, lb) -> f.expanded = !f.expanded));
            }
        }
        y += ROW_H + 4;

        if (f.expanded) {
            for (Setting s : f.settings) {
                if (visible(y, SET_H, cTop, cBottom)) {
                    drawSetting(g, s, x1 + 12, x2, y, mx, my);
                }
                y += SET_H + 2;
            }
            y += 4;
        }
        return y;
    }

    private void drawSetting(GuiGraphics g, Setting s, int x1, int x2, int y, int mx, int my) {
        Gfx.roundRect(g, x1, y, x2, y + SET_H, 3, Theme.ROW2);
        Gfx.text(g, s.name, x1 + 8, y + 6, Theme.SUBTLE);

        if (s.type == Setting.Type.BOOL) {
            Gfx.toggle(g, x2 - 36, y + 4, s.flag);
            hits.add(new Hit(x1, y, x2, y + SET_H, (lx, ly, lb) -> {
                s.flag = !s.flag;
                Config.save();
            }));
            return;
        }

        int sx1 = x1 + 150;
        int sx2 = x2 - 56;
        double frac = (s.value - s.min) / (s.max - s.min);
        frac = Math.max(0.0, Math.min(1.0, frac));
        int fillX = sx1 + (int) Math.round((sx2 - sx1) * frac);

        Gfx.roundRect(g, sx1, y + 8, sx2, y + 12, 2, Theme.OFF);
        if (fillX > sx1) {
            Gfx.roundRect(g, sx1, y + 8, fillX, y + 12, 2, Theme.accent());
        }
        Gfx.roundRect(g, fillX - 3, y + 4, fillX + 3, y + 16, 2, 0xFFFFFFFF);
        Gfx.textRight(g, s.display(), x2 - 8, y + 6, Theme.TEXT);

        hits.add(new Hit(sx1 - 4, y, sx2 + 4, y + SET_H, (lx, ly, lb) -> {
            if (lb == 0) {
                dragging = s;
                dragX1 = sx1;
                dragX2 = sx2;
                applyDrag(lx);
            }
        }));
    }

    private int drawSettingsTab(GuiGraphics g, int x1, int x2, int y,
                                int cTop, int cBottom, int mx, int my) {
        // Accent colour
        if (visible(y, ROW_H, cTop, cBottom)) {
            Gfx.roundRect(g, x1, y, x2, y + ROW_H, 4, Theme.ROW);
            Gfx.text(g, "Accent color", x1 + 10, y + 11, Theme.TEXT);

            int swW = 26;
            int gap = 4;
            int total = Theme.NAMES.length * (swW + gap) - gap;
            int sx = x2 - 10 - total;
            for (int i = 0; i < Theme.NAMES.length; i++) {
                final int index = i;
                int bx1 = sx + i * (swW + gap);
                int bx2 = bx1 + swW;
                Gfx.roundRect(g, bx1, y + 7, bx2, y + 23, 3, Theme.preview(i));
                if (Theme.index == i) {
                    Gfx.outline(g, bx1 - 1, y + 6, bx2 + 1, y + 24, 0xFFFFFFFF);
                }
                hits.add(new Hit(bx1, y + 7, bx2, y + 23, (lx, ly, lb) -> {
                    Theme.index = index;
                    Config.save();
                }));
            }
            Gfx.textRight(g, Theme.NAMES[Theme.index], sx - 10, y + 11, Theme.MUTED);
        }
        y += ROW_H + 4;

        // GUI key
        if (visible(y, ROW_H, cTop, cBottom)) {
            Gfx.roundRect(g, x1, y, x2, y + ROW_H, 4, Theme.ROW);
            Gfx.text(g, "Menu key", x1 + 10, y + 11, Theme.TEXT);
            int kx2 = x2 - 10;
            int kx1 = kx2 - 80;
            Gfx.roundRect(g, kx1, y + 6, kx2, y + 24, 3, bindingGui ? Theme.accent() : Theme.ROW2);
            Gfx.textCentered(g, bindingGui ? "..." : keyName(HenClient.guiKey), (kx1 + kx2) / 2, y + 11,
                    bindingGui ? 0xFF101014 : Theme.MUTED);
            hits.add(new Hit(kx1, y + 6, kx2, y + 24, (lx, ly, lb) -> {
                if (lb == 0) {
                    bindingGui = true;
                    binding = null;
                }
            }));
        }
        y += ROW_H + 4;

        y = switchRow(g, "Watermark", Hud.watermark, x1, x2, y, cTop, cBottom,
                () -> Hud.watermark = !Hud.watermark);
        y = switchRow(g, "Module list", Hud.arrayList, x1, x2, y, cTop, cBottom,
                () -> Hud.arrayList = !Hud.arrayList);

        if (visible(y, ROW_H, cTop, cBottom)) {
            Gfx.text(g, "Hen Client " + HenClient.VERSION + " - Backspace unbinds a key while binding.",
                    x1 + 4, y + 10, Theme.MUTED);
        }
        y += ROW_H;
        return y;
    }

    private int switchRow(GuiGraphics g, String label, boolean value, int x1, int x2, int y,
                          int cTop, int cBottom, Runnable flip) {
        if (visible(y, ROW_H, cTop, cBottom)) {
            Gfx.roundRect(g, x1, y, x2, y + ROW_H, 4, Theme.ROW);
            Gfx.text(g, label, x1 + 10, y + 11, Theme.TEXT);
            Gfx.toggle(g, x2 - 36, y + 9, value);
            hits.add(new Hit(x1, y, x2, y + ROW_H, (lx, ly, lb) -> {
                flip.run();
                Config.save();
            }));
        }
        return y + ROW_H + 4;
    }

    private void applyDrag(double mouseX) {
        if (dragging == null || dragX2 <= dragX1) return;
        double frac = (mouseX - dragX1) / (double) (dragX2 - dragX1);
        frac = Math.max(0.0, Math.min(1.0, frac));
        dragging.setNum(dragging.min + frac * (dragging.max - dragging.min));
    }

    private static boolean visible(int y, int h, int cTop, int cBottom) {
        return y >= cTop && y + h <= cBottom;
    }

    private static boolean inside(int mx, int my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (binding != null || bindingGui) {
            binding = null;
            bindingGui = false;
            return true;
        }

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x1() && mx < h.x2() && my >= h.y1() && my < h.y2()) {
                h.click().on(mx, my, button);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** No @Override on purpose: if a game update changes this signature we just lose wheel scrolling. */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.round(scrollY * 34));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (binding != null || bindingGui) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                binding = null;
                bindingGui = false;
                return true;
            }
            if (binding != null && (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE)) {
                binding.key = -1;
            } else if (key != GLFW.GLFW_KEY_UNKNOWN) {
                if (bindingGui) {
                    HenClient.guiKey = key;
                } else {
                    binding.key = key;
                }
            }
            binding = null;
            bindingGui = false;
            Config.save();
            return true;
        }

        // Same key that opened the menu closes it (ignore the repeat events right after opening).
        if (key == HenClient.guiKey && System.currentTimeMillis() - openedAt > 400L) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            scroll = Math.max(0, scroll - 34);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            scroll += 34;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Config.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------

    private static String keyName(int key) {
        if (key <= 0) return "NONE";
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) {
            return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        }
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            return "NUM " + (key - GLFW.GLFW_KEY_KP_0);
        }
        switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_PAGE_UP: return "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PGDN";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            default: break;
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase(Locale.ROOT);
        return "KEY " + key;
    }
}
