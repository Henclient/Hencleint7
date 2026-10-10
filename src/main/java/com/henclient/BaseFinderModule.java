package com.henclient;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lists spots where lots of containers are clustered together (likely a base or storage hall). */
public final class BaseFinderModule extends Feature {
    private final Setting minContainers = addNum("Min containers", 6, 2, 40, 1);

    private record Base(int chunkX, int chunkZ, int score, int spawners) {}

    private List<Base> bases = new ArrayList<>();
    private int ticks = 0;

    public BaseFinderModule() {
        super("basefinder", "Base Finder", Category.RENDER,
                "Lists clusters of containers in loaded chunks.");
    }

    @Override
    public boolean needsBlockScan() {
        return true;
    }

    @Override
    public String info() {
        return Integer.toString(bases.size());
    }

    @Override
    public void onDisable() {
        bases = new ArrayList<>();
    }

    @Override
    public void onTick() {
        if (++ticks < 10) return;
        ticks = 0;

        // containers[0], spawners[1] per chunk
        Map<Long, int[]> perChunk = new HashMap<>();
        for (BlockScan.Entry e : BlockScan.entries()) {
            BlockPos p = e.pos();
            long key = (((long) (p.getX() >> 4)) << 32) | ((p.getZ() >> 4) & 0xFFFFFFFFL);
            int[] counts = perChunk.computeIfAbsent(key, k -> new int[2]);
            if (e.kind() == BlockScan.Kind.SPAWNER) {
                counts[1]++;
            } else {
                counts[0]++;
            }
        }

        // Score each chunk by the containers in its 3x3 neighbourhood so a base that
        // straddles a chunk border is still one result.
        List<Base> candidates = new ArrayList<>();
        for (Map.Entry<Long, int[]> entry : perChunk.entrySet()) {
            int cx = (int) (entry.getKey() >> 32);
            int cz = (int) (long) entry.getKey();
            int score = 0;
            int spawners = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long nk = (((long) (cx + dx)) << 32) | ((cz + dz) & 0xFFFFFFFFL);
                    int[] c = perChunk.get(nk);
                    if (c != null) {
                        score += c[0];
                        spawners += c[1];
                    }
                }
            }
            if (score >= minContainers.whole()) {
                candidates.add(new Base(cx, cz, score, spawners));
            }
        }

        candidates.sort(Comparator.comparingInt(Base::score).reversed());

        List<Base> picked = new ArrayList<>();
        for (Base c : candidates) {
            boolean tooClose = false;
            for (Base p : picked) {
                if (Math.abs(p.chunkX() - c.chunkX()) <= 2 && Math.abs(p.chunkZ() - c.chunkZ()) <= 2) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) picked.add(c);
            if (picked.size() >= 4) break;
        }
        bases = picked;
    }

    @Override
    public void hudLines(List<Hud.Line> out) {
        if (bases.isEmpty()) {
            out.add(new Hud.Line("Bases", "none", Theme.MUTED));
            return;
        }
        for (Base b : bases) {
            String where = (b.chunkX() * 16 + 8) + " " + (b.chunkZ() * 16 + 8);
            String what = b.score() + (b.spawners() > 0 ? " +" + b.spawners() + "sp" : "");
            out.add(new Hud.Line("Base", where + " (" + what + ")", Theme.WARN));
        }
    }
}
