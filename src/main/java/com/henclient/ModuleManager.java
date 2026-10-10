package com.henclient;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Owns the feature list and runs each feature inside an error guard. */
public final class ModuleManager {
    private static final List<Feature> ALL = new ArrayList<>();
    private static int scanTicks = 0;

    private ModuleManager() {}

    public static List<Feature> all() {
        return ALL;
    }

    public static List<Feature> in(Category category) {
        List<Feature> out = new ArrayList<>();
        for (Feature f : ALL) {
            if (f.category == category) out.add(f);
        }
        return out;
    }

    private static void add(Feature f) {
        ALL.add(f);
    }

    public static void init() {
        // Render
        add(new StorageEspModule());
        add(new SpawnerEspModule());
        add(new BaseFinderModule());
        add(new PlayerEspModule());
        add(new MobEspModule());
        add(new ItemEspModule());
        add(new TracersModule());
        add(new FullbrightModule());
        add(new ZoomModule());
        add(new FreecamModule());
        // Movement
        add(new FlightModule());
        add(new SpeedModule());
        add(new SprintModule());
        add(new StepModule());
        // Combat
        add(new MobAuraModule());
        // HUD
        add(new InfoModules.Coordinates());
        add(new InfoModules.Fps());
        add(new InfoModules.Ping());
        add(new InfoModules.Direction());
        add(new InfoModules.Biome());
        add(new InfoModules.SpeedMeter());
        add(new InfoModules.Armor());
        add(new InfoModules.Potions());
        add(new InfoModules.ItemCounter());
        add(new InfoModules.Health());
        add(new InfoModules.Clock());
        add(new InfoModules.Keystrokes());
    }

    /**
     * Runs code belonging to a feature. If it throws (for example because a game API
     * differs from what the code expects), the feature is switched off and logged
     * instead of crashing the game.
     */
    public static void guard(Feature f, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            HenClient.LOGGER.error("Feature '" + f.id + "' failed and was disabled", t);
            f.enabled = false;
            try {
                f.onDisable();
            } catch (Throwable ignored) {
                // nothing more we can do
            }
        }
    }

    public static void tick() {
        for (Feature f : ALL) {
            if (f.enabled) guard(f, f::onTick);
        }

        boolean scan = false;
        for (Feature f : ALL) {
            if (f.enabled && f.needsBlockScan()) {
                scan = true;
                break;
            }
        }
        if (scan) {
            if (++scanTicks >= 10) {
                scanTicks = 0;
                try {
                    BlockScan.scan();
                } catch (Throwable t) {
                    HenClient.LOGGER.error("Block scan failed", t);
                    BlockScan.clear();
                }
            }
        } else {
            scanTicks = 9; // scan right away the next time something needs it
            BlockScan.clear();
        }
    }

    public static void frame(GuiGraphics g, float partial) {
        Projector.update(partial);
        for (Feature f : ALL) {
            if (f.enabled) guard(f, () -> f.onFrame(g, partial));
        }
    }

    public static void worldLeft() {
        for (Feature f : ALL) {
            if (f.enabled) guard(f, f::onWorldLeave);
        }
        BlockScan.clear();
    }

    public static void shutdown() {
        for (Feature f : ALL) {
            if (!f.enabled) continue;
            try {
                f.onDisable();
            } catch (Throwable ignored) {
                // closing anyway
            }
        }
    }

    public static boolean blocksInput() {
        for (Feature f : ALL) {
            if (f.enabled && f.blocksInput()) return true;
        }
        return false;
    }
}
