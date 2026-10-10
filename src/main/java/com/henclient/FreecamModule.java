package com.henclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import org.lwjgl.glfw.GLFW;

/**
 * Detached camera. The camera entity is moved every rendered frame (not every tick)
 * and its "old" position is kept equal to the new one, so the game never interpolates
 * between two different states. That removes the shaking the tick-based version had.
 */
public final class FreecamModule extends Feature {
    private final Setting speed = addNum("Speed", 14, 2, 80, 1);          // blocks per second
    private final Setting boost = addNum("Boost", 3, 1, 8, 0.5);          // multiplier while holding Ctrl
    private final Setting smoothing = addNum("Smoothing", 14, 2, 40, 1);  // higher = snappier

    private RemotePlayer cam;
    private double x, y, z;
    private double vx, vy, vz;
    private float origYaw, origPitch;
    private long lastNanos;

    public FreecamModule() {
        super("freecam", "Freecam", Category.RENDER,
                "Detach the camera. WASD, Space, Shift, Ctrl = boost.");
        key = GLFW.GLFW_KEY_Z;
        persistent = false;
    }

    @Override
    public void onEnable() {
        var p = MC.player;
        if (p == null || MC.level == null) {
            enabled = false;
            return;
        }
        cam = new RemotePlayer(MC.level, p.getGameProfile());
        x = p.getX();
        y = p.getY();
        z = p.getZ();
        vx = 0;
        vy = 0;
        vz = 0;
        origYaw = p.getYRot();
        origPitch = p.getXRot();
        lastNanos = System.nanoTime();
        apply();
        MC.setCameraEntity(cam);
    }

    @Override
    public void onDisable() {
        if (cam == null) return;
        cam = null;
        var p = MC.player;
        if (p != null) {
            MC.setCameraEntity(p);
            // Mouse movement turned the real player while we were detached; put it back.
            p.setYRot(origYaw);
            p.setXRot(origPitch);
        }
    }

    @Override
    public void onWorldLeave() {
        cam = null;
        enabled = false;
    }

    @Override
    public boolean blocksInput() {
        return cam != null;
    }

    @Override
    public void onTick() {
        if (cam == null) return;

        // New dimension / world object: our camera entity belongs to the old one.
        if (cam.level() != MC.level) {
            cam = null;
            enabled = false;
            if (MC.player != null) MC.setCameraEntity(MC.player);
            return;
        }

        // The game resets the camera entity on respawn; take it back.
        if (MC.getCameraEntity() != cam) {
            MC.setCameraEntity(cam);
        }

        // With the HUD hidden (F1) there is no frame callback, so move from the tick instead.
        if (MC.options.hideGui) {
            step();
        }
    }

    @Override
    public void onFrame(GuiGraphics g, float partial) {
        step();
    }

    private void step() {
        if (cam == null || MC.player == null) return;

        long now = System.nanoTime();
        double dt = Math.min(0.1, Math.max(0.0, (now - lastNanos) / 1.0e9));
        lastNanos = now;

        double fwd = 0;
        double strafe = 0;
        double vert = 0;
        double mult = 1;

        if (MC.screen == null) {
            long w = MC.getWindow().handle();
            fwd = axis(w, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_S);
            strafe = axis(w, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_A);
            vert = axis(w, GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_LEFT_SHIFT);
            if (GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) {
                mult = boost.num();
            }
        }

        double yaw = Math.toRadians(MC.player.getYRot());
        double mx = -Math.sin(yaw) * fwd - Math.cos(yaw) * strafe;
        double mz = Math.cos(yaw) * fwd - Math.sin(yaw) * strafe;
        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 1.0) {
            mx /= len;
            mz /= len;
        }

        double top = speed.num() * mult;
        double k = 1.0 - Math.exp(-smoothing.num() * dt);
        vx += (mx * top - vx) * k;
        vy += (vert * top - vy) * k;
        vz += (mz * top - vz) * k;

        x += vx * dt;
        y += vy * dt;
        z += vz * dt;
        apply();
    }

    private static double axis(long window, int plus, int minus) {
        double v = 0;
        if (GLFW.glfwGetKey(window, plus) == GLFW.GLFW_PRESS) v += 1;
        if (GLFW.glfwGetKey(window, minus) == GLFW.GLFW_PRESS) v -= 1;
        return v;
    }

    /** Writes position and rotation so that "previous" equals "current" (no interpolation). */
    private void apply() {
        cam.setPos(x, y, z);
        cam.xo = x;
        cam.yo = y;
        cam.zo = z;
        cam.xOld = x;
        cam.yOld = y;
        cam.zOld = z;

        var p = MC.player;
        if (p != null) {
            float yaw = p.getYRot();
            float pitch = p.getXRot();
            cam.setYRot(yaw);
            cam.setXRot(pitch);
            cam.yRotO = yaw;
            cam.xRotO = pitch;
        }
    }
}
