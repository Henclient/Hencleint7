package com.henclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class StorageEspModule extends Feature {
    private final Setting range = addNum("Range", 128, 16, 192, 8);
    private final Setting utility = addBool("Utility blocks", false);
    private final Setting labels = addBool("Labels", true);
    private final Setting corners = addBool("Corner style", false);
    private int total;

    public StorageEspModule() {
        super("storageesp", "Storage ESP", Category.RENDER,
                "Boxes chests, barrels and shulkers through walls.");
        key = GLFW.GLFW_KEY_X;
    }

    @Override
    public boolean needsBlockScan() {
        return true;
    }

    @Override
    public String info() {
        return Integer.toString(total);
    }

    @Override
    public void onFrame(GuiGraphics g, float partial) {
        total = 0;
        if (!Projector.ready()) return;

        double maxDist = range.num();
        for (BlockScan.Entry e : BlockScan.entries()) {
            if (e.kind() == BlockScan.Kind.SPAWNER) continue;
            if (e.kind() == BlockScan.Kind.UTILITY && !utility.on()) continue;

            BlockPos p = e.pos();
            double d = Projector.distance(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            if (d > maxDist) continue;
            total++;

            double[] b = Projector.projectBox(
                    p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1);
            if (b == null) continue;

            int[] r = Gfx.espBox(g, b, e.color(), corners.on());
            if (labels.on() && d <= 48.0) {
                Gfx.label(g, e.label() + " " + (int) d + "m", (r[0] + r[2]) / 2, r[1] - 13, e.color());
            }
        }
    }
}
