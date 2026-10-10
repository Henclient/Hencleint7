package com.henclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemEspModule extends Feature {
    private final Setting range = addNum("Range", 48, 8, 128, 4);
    private final Setting labels = addBool("Labels", true);
    private int shown;

    public ItemEspModule() {
        super("itemesp", "Item ESP", Category.RENDER,
                "Boxes dropped items through walls.");
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
            if (!(e instanceof ItemEntity item) || !item.isAlive()) continue;
            if (shown >= 80) break;

            double px = Gfx.lerp(pt, item.xo, item.getX());
            double py = Gfx.lerp(pt, item.yo, item.getY());
            double pz = Gfx.lerp(pt, item.zo, item.getZ());
            double d = Projector.distance(px, py + 0.125, pz);
            if (d > maxDist) continue;

            double[] b = Projector.projectBox(px - 0.2, py, pz - 0.2, px + 0.2, py + 0.4, pz + 0.2);
            if (b == null) continue;
            shown++;

            int[] r = Gfx.espBox(g, b, 0xFFFFD54F, true);
            if (labels.on() && d <= 24.0) {
                var stack = item.getItem();
                String text = stack.getHoverName().getString()
                        + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
                Gfx.label(g, text, (r[0] + r[2]) / 2, r[1] - 13, 0xFFFFD54F);
            }
        }
    }
}
