package com.henclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Base class for every module (cheat or HUD element). */
public abstract class Feature {
    protected static final Minecraft MC = Minecraft.getInstance();

    public final String id;
    public final String name;
    public final String description;
    public final Category category;

    public int key = -1;                // GLFW key code, -1 = unbound
    public boolean enabled = false;
    public boolean persistent = true;   // remember on/off state between launches
    public boolean expanded = false;    // settings folded open in the GUI
    public final List<Setting> settings = new ArrayList<>();

    protected Feature(String id, String name, Category category, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    protected Setting addBool(String name, boolean def) {
        Setting s = Setting.bool(name, def);
        settings.add(s);
        return s;
    }

    protected Setting addNum(String name, double def, double min, double max, double step) {
        Setting s = Setting.num(name, def, min, max, step);
        settings.add(s);
        return s;
    }

    // ---- lifecycle -------------------------------------------------

    /** Called when the player switches the feature on. */
    public void onEnable() {}

    /** Called when switched off, on world leave failures and on game exit. Must be safe to call twice. */
    public void onDisable() {}

    /** Every client tick while in a world and enabled. */
    public void onTick() {}

    /** Every rendered frame while in a world and enabled (drawn over the world, under the HUD panels). */
    public void onFrame(GuiGraphics g, float partial) {}

    /** The player left the world while this feature was on. */
    public void onWorldLeave() {}

    /** True if the feature wants the real player to ignore the keyboard/mouse. */
    public boolean blocksInput() {
        return false;
    }

    /** True if the feature reads {@link BlockScan}. */
    public boolean needsBlockScan() {
        return false;
    }

    /** Small suffix shown after the name in the module list, or null. */
    public String info() {
        return null;
    }

    /** Rows for the info panel (top-left). */
    public void hudLines(List<Hud.Line> out) {}

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        ModuleManager.guard(this, value ? this::onEnable : this::onDisable);
    }
}
