package com.henclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Projects world positions onto the GUI-scaled screen using the current camera,
 * so ESP can be drawn as plain HUD shapes (visible through walls).
 */
public final class Projector {
    private static final Minecraft MC = Minecraft.getInstance();

    private static double ex, ey, ez;
    private static double fx, fy, fz;
    private static double rx, rz;
    private static double ux, uy, uz;
    private static double tanHalf, aspect, sw, sh;
    private static float partial;
    private static boolean ready;

    private Projector() {}

    /** The entity the game is currently rendering from (the fake one while Freecam is on). */
    public static Entity camera() {
        Entity cam = MC.getCameraEntity();
        return cam != null ? cam : MC.player;
    }

    public static void update(float pt) {
        Entity cam = camera();
        if (cam == null || MC.level == null) {
            ready = false;
            return;
        }
        partial = pt;
        Vec3 eye = cam.getEyePosition(pt);
        ex = eye.x;
        ey = eye.y;
        ez = eye.z;

        double yaw = Math.toRadians(cam.getViewYRot(pt));
        double pitch = Math.toRadians(cam.getViewXRot(pt));
        double cp = Math.cos(pitch);

        // Minecraft axes: +X east, +Y up, +Z south.
        fx = -Math.sin(yaw) * cp;
        fy = -Math.sin(pitch);
        fz = Math.cos(yaw) * cp;
        rx = -Math.cos(yaw);
        rz = -Math.sin(yaw);
        ux = -rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy;

        sw = MC.getWindow().getGuiScaledWidth();
        sh = MC.getWindow().getGuiScaledHeight();
        tanHalf = Math.tan(Math.toRadians(MC.options.fov().get()) / 2.0);
        aspect = sw / sh;
        ready = true;
    }

    public static boolean ready() {
        return ready;
    }

    public static float partial() {
        return partial;
    }

    public static double screenW() {
        return sw;
    }

    public static double screenH() {
        return sh;
    }

    public static double distance(double x, double y, double z) {
        double dx = x - ex;
        double dy = y - ey;
        double dz = z - ez;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Returns {screenX, screenY, depth}, or null when the point is behind the camera. */
    public static double[] project(double x, double y, double z) {
        double dx = x - ex;
        double dy = y - ey;
        double dz = z - ez;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.05) return null;
        double px = dx * rx + dz * rz;
        double py = dx * ux + dy * uy + dz * uz;
        double sx = sw / 2.0 * (1.0 + px / (depth * tanHalf * aspect));
        double sy = sh / 2.0 * (1.0 - py / (depth * tanHalf));
        return new double[]{sx, sy, depth};
    }

    /**
     * Screen rectangle {minX,minY,maxX,maxY} around a world-space box, or null if any
     * corner is behind the camera or the rectangle is completely off-screen.
     */
    public static double[] projectBox(double x1, double y1, double z1,
                                      double x2, double y2, double z2) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? x1 : x2;
            double y = (i & 2) == 0 ? y1 : y2;
            double z = (i & 4) == 0 ? z1 : z2;
            double[] p = project(x, y, z);
            if (p == null) return null;
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        if (maxX < 0 || maxY < 0 || minX > sw || minY > sh) return null;
        return new double[]{minX, minY, maxX, maxY};
    }
}
