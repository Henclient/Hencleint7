package com.henclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

public final class MobEspModule extends Feature {
    private final Setting hostile = addBool("Hostile", true);
    private final Setting passive = addBool("Passive", false);
    private final Setting range = addNum("Range", 96, 16, 192, 8);
    private final Setting names = addBool("Names", true);
    private final Setting corners = addBool("Corner style", true);
    private int shown;

    public MobEspModule() {
        super("mobesp", "Mob ESP", Category.RENDER,
                "Boxes hostile (and optionally passive) mobs.");
    }

    @Override
    public String info() {
        return Integer.toString(shown);
    }

    @Override
    public void onFrame(GuiGraphics g, float partial) {
        shown = 0;
        if (!Projector.ready() || MC.level == null) return;

        float pt = Projector.partial();
        double maxDist = range.num();

        for (Entity e : MC.level.entitiesForRendering()) {
            if (!(e instanceof Mob mob) || !mob.isAlive()) continue;

            boolean isHostile = mob instanceof Enemy;
            if (isHostile ? !hostile.on() : !passive.on()) continue;
            if (shown >= 120) break;

            double px = Gfx.lerp(pt, mob.xo, mob.getX());
            double py = Gfx.lerp(pt, mob.yo, mob.getY());
            double pz = Gfx.lerp(pt, mob.zo, mob.getZ());
            double hw = mob.getBbWidth() / 2.0 + 0.05;
            double h = mob.getBbHeight() + 0.05;

            double d = Projector.distance(px, py + h / 2.0, pz);
            if (d > maxDist) continue;

            double[] b = Projector.projectBox(px - hw, py, pz - hw, px + hw, py + h, pz + hw);
            if (b == null) continue;
            shown++;

            int color = isHostile ? 0xFFFF5252 : 0xFF69F0AE;
            int[] r = Gfx.espBox(g, b, color, corners.on());
            if (names.on() && d <= 64.0) {
                Gfx.label(g, mob.getName().getString() + " " + (int) d + "m",
                        (r[0] + r[2]) / 2, r[1] - 13, color);
            }
        }
    }
}
