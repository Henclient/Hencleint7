package com.henclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Cheap scan of the block entities in chunks the server has already sent us.
 * Runs a few times per second and is shared by all block-based modules.
 */
public final class BlockScan {
    public enum Kind { STORAGE, UTILITY, SPAWNER }

    public record Entry(BlockPos pos, Kind kind, int color, String label) {}

    public static final int RANGE = 192;
    private static final int MAX_ENTRIES = 3000;

    private static final Minecraft MC = Minecraft.getInstance();
    private static List<Entry> entries = List.of();

    private BlockScan() {}

    public static List<Entry> entries() {
        return entries;
    }

    public static void clear() {
        entries = List.of();
    }

    public static void scan() {
        Entity cam = Projector.camera();
        if (MC.level == null || cam == null) {
            entries = List.of();
            return;
        }

        double cx = cam.getX();
        double cy = cam.getY();
        double cz = cam.getZ();
        int chunkX = ((int) Math.floor(cx)) >> 4;
        int chunkZ = ((int) Math.floor(cz)) >> 4;
        int radius = (RANGE >> 4) + 1;
        double maxSq = (double) RANGE * RANGE;

        List<Entry> out = new ArrayList<>();
        outer:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                var chunk = MC.level.getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk levelChunk)) continue;

                for (BlockEntity be : levelChunk.getBlockEntities().values()) {
                    Entry entry = classify(be);
                    if (entry == null) continue;

                    BlockPos p = entry.pos();
                    double ddx = p.getX() + 0.5 - cx;
                    double ddy = p.getY() + 0.5 - cy;
                    double ddz = p.getZ() + 0.5 - cz;
                    if (ddx * ddx + ddy * ddy + ddz * ddz > maxSq) continue;

                    out.add(entry);
                    if (out.size() >= MAX_ENTRIES) break outer;
                }
            }
        }
        entries = out;
    }

    private static Entry classify(BlockEntity be) {
        BlockPos p = be.getBlockPos();
        if (be instanceof TrappedChestBlockEntity) {
            return new Entry(p, Kind.STORAGE, 0xFFFF5252, "Trapped Chest");
        }
        if (be instanceof ChestBlockEntity) {
            return new Entry(p, Kind.STORAGE, 0xFFFFA726, "Chest");
        }
        if (be instanceof BarrelBlockEntity) {
            return new Entry(p, Kind.STORAGE, 0xFFC68642, "Barrel");
        }
        if (be instanceof ShulkerBoxBlockEntity) {
            return new Entry(p, Kind.STORAGE, 0xFFCE93D8, "Shulker");
        }
        if (be instanceof EnderChestBlockEntity) {
            return new Entry(p, Kind.STORAGE, 0xFF26C6DA, "Ender Chest");
        }
        if (be instanceof SpawnerBlockEntity) {
            return new Entry(p, Kind.SPAWNER, 0xFFE040FB, "Spawner");
        }
        if (be instanceof HopperBlockEntity) {
            return new Entry(p, Kind.UTILITY, 0xFF90A4AE, "Hopper");
        }
        if (be instanceof AbstractFurnaceBlockEntity) {
            return new Entry(p, Kind.UTILITY, 0xFFFF8A65, "Furnace");
        }
        if (be instanceof DispenserBlockEntity) {
            return new Entry(p, Kind.UTILITY, 0xFFB0BEC5, "Dispenser");
        }
        return null;
    }
}
