package com.henclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lines from the bottom of the screen to nearby targets. */
public final class TracersModule extends Feature {
    private final Setting players = addBool("Players", true);
    private final Setting hostiles = addBool("Hostile mobs", false);
    private final Setting storage = addBool("Storage", false);
    private final Setting fromCrosshair = addBool("From crosshair", false);
    private final Setting range = addNum("Range", 128, 16, 192, 8);
    private final Setting maxTargets = addNum("Max targets", 12, 1, 30, 1);

    private record Target(double x, double y, double z, double dist, int color) {}

    public TracersModule() {
        super("tracers", "Tracers", Category.RENDER,
                "Lines pointing at players, mobs and storage.");
    }

    @Override
    public boolean needsBlockScan() {
        return storage.on();
    }

    @Override
    public void onFrame(GuiGraphics g, float partial) {
        if (!Projector.ready() || MC.level == null) return;

        Entity cam = Projector.camera();
        float pt = Projector.partial();
        double maxDist = range.num();
        List<Target> targets = new ArrayList<>();

        if (players.on()) {
            for (Player p : MC.level.players()) {
                if (p == cam || !p.isAlive()) continue;
                double x = Gfx.lerp(pt, p.xo, p.getX());
                double y = Gfx.lerp(pt, p.yo, p.getY()) + p.getBbHeight() / 2.0;
                double z = Gfx.lerp(pt, p.zo, p.getZ());
                double d = Projector.distance(x, y, z);
                if (d <= maxDist) targets.add(new Target(x, y, z, d, 0xFF4FC3F7));
            }
        }

        if (hostiles.on()) {
            for (Entity e : MC.level.entitiesForRendering()) {
                if (!(e instanceof Mob mob) || !(mob instanceof Enemy) || !mob.isAlive()) continue;
                double x = Gfx.lerp(pt, mob.xo, mob.getX());
                double y = Gfx.lerp(pt, mob.yo, mob.getY()) + mob.getBbHeight() / 2.0;
                double z = Gfx.lerp(pt, mob.zo, mob.getZ());
                double d = Projector.distance(x, y, z);
                if (d <= maxDist) targets.add(new Target(x, y, z, d, 0xFFFF5252));
            }
        }

        if (storage.on()) {
            for (BlockScan.Entry e : BlockScan.entries()) {
                if (e.kind() != BlockScan.Kind.STORAGE) continue;
                BlockPos p = e.pos();
                double x = p.getX() + 0.5;
                double y = p.getY() + 0.5;
                double z = p.getZ() + 0.5;
                double d = Projector.distance(x, y, z);
                if (d <= maxDist) targets.add(new Target(x, y, z, d, e.color()));
            }
        }

        targets.sort(Comparator.comparingDouble(Target::dist));

        double startX = Projector.screenW() / 2.0;
        double startY = fromCrosshair.on() ? Projector.screenH() / 2.0 : Projector.screenH();

        int drawn = 0;
        for (Target t : targets) {
            if (drawn >= maxTargets.whole()) break;
            double[] p = Projector.project(t.x(), t.y(), t.z());
            if (p == null) continue;
            Gfx.line(g, startX, startY, p[0], p[1], Theme.withAlpha(t.color(), 0xD0));
            drawn++;
        }
    }
}
