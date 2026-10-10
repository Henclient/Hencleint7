package com.henclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class HenClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("henclient");
    public static final String VERSION = "1.1.0";

    public static int guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private static final Set<Integer> HELD = new HashSet<>();
    private static boolean wasInWorld = false;
    private static int hudErrors = 0;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        // Freecam & co: make the real player ignore the keyboard before it reads it.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.screen == null && ModuleManager.blocksInput()) {
                KeyMapping.releaseAll();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> endTick(client));

        HudRenderCallback.EVENT.register((graphics, tickCounter) ->
                renderHud(graphics, tickCounter.getGameTimeDeltaPartialTick(true)));

        // Put FOV etc. back before the game writes its options file.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.shutdown());

        LOGGER.info("Hen Client {} loaded", VERSION);
    }

    private static void endTick(Minecraft client) {
        boolean inWorld = client.player != null && client.level != null;
        if (!inWorld) {
            if (wasInWorld) {
                ModuleManager.worldLeft();
                HELD.clear();
            }
            wasInWorld = false;
            return;
        }
        wasInWorld = true;

        ModuleManager.tick();

        long window = client.getWindow().handle();

        // While a screen is open, only remember which keys are down so that closing the
        // screen doesn't count a still-held key as a fresh press.
        if (client.screen != null) {
            trackHeld(window);
            return;
        }

        Set<Integer> fresh = freshPresses(window);

        if (fresh.contains(guiKey)) {
            client.setScreen(new ClickGui());
            return;
        }

        boolean changed = false;
        for (Feature f : ModuleManager.all()) {
            if (f.key > 0 && fresh.contains(f.key)) {
                f.toggle();
                changed = true;
            }
        }
        if (changed) {
            Config.save();
        }
    }

    private static boolean isDown(long window, int key) {
        return key > 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    private static void trackHeld(long window) {
        updateHeld(window, guiKey);
        for (Feature f : ModuleManager.all()) {
            updateHeld(window, f.key);
        }
    }

    private static void updateHeld(long window, int key) {
        if (key <= 0) return;
        if (isDown(window, key)) {
            HELD.add(key);
        } else {
            HELD.remove(key);
        }
    }

    /** Keys that went from up to down since the last tick. */
    private static Set<Integer> freshPresses(long window) {
        Set<Integer> fresh = new HashSet<>();
        collect(window, guiKey, fresh);
        for (Feature f : ModuleManager.all()) {
            collect(window, f.key, fresh);
        }
        return fresh;
    }

    private static void collect(long window, int key, Set<Integer> fresh) {
        if (key <= 0) return;
        if (isDown(window, key)) {
            if (HELD.add(key)) fresh.add(key);
        } else {
            HELD.remove(key);
        }
    }

    private static void renderHud(GuiGraphics graphics, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        try {
            ModuleManager.frame(graphics, partial);
            Hud.render(graphics);
        } catch (Throwable t) {
            if (hudErrors++ < 5) {
                LOGGER.error("HUD rendering failed", t);
            }
        }
    }
}
