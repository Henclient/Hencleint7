package com.henclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerEspModule extends Feature {
    private final Setting range = addNum("Range", 160, 16, 256, 8);
    private final Setting names = addBool("Names", true);
    private final Setting health = addBool("Health bar", true);
    private final Setting corners = addBool("Corner style", true);
    private int shown;

    public PlayerEspModule() {
        super("playeresp", "Player ESP", Category.RENDER,
                "Boxes, names and health of other players.");
    }

    @Override
    public String info() {
        return Integer.toString(shown);
    }

    @Override
    public void onFrame(GuiGraphics g, float partial) {
        shown = 0;
        if (!Projector.ready() || MC.level == null) return;

        Entity cam = Projector.camera();
        float pt = Projector.partial();
        double maxDist = range.num();

        for (Player p : MC.level.players()) {
            if (p == cam || !p.isAlive()) continue; // never box the camera's own entity

            double px = Gfx.lerp(pt, p.xo, p.getX());
            double py = Gfx.lerp(pt, p.yo, p.getY());
            double pz = Gfx.lerp(pt, p.zo, p.getZ());
            double hw = p.getBbWidth() / 2.0 + 0.05;
            double h = p.getBbHeight() + 0.1;

            double d = Projector.distance(px, py + h / 2.0, pz);
            if (d > maxDist) continue;

            double[] b = Projector.projectBox(px - hw, py, pz - hw, px + hw, py + h, pz + hw);
            if (b == null) continue;
            shown++;

            int color = Theme.accent(shown * 0.7);
            int[] r = Gfx.espBox(g, b, color, corners.on());

            if (health.on()) {
                float max = Math.max(1.0f, p.getMaxHealth());
                Gfx.healthBar(g, r[0], r[1], r[3], p.getHealth() / max);
            }
            if (names.on()) {
                String text = p.getName().getString() + " " + (int) d + "m";
                Gfx.label(g, text, (r[0] + r[2]) / 2, r[1] - 13, Theme.TEXT);
            }
        }
    }
}
