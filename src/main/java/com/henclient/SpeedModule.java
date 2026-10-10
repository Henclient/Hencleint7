package com.henclient;

import net.minecraft.client.Options;
import net.minecraft.world.phys.Vec3;

/** Sets your horizontal velocity while you hold a movement key. */
public final class SpeedModule extends Feature {
    private final Setting speed = addNum("Speed", 0.4, 0.1, 1.5, 0.05); // blocks per tick

    public SpeedModule() {
        super("speed", "Speed", Category.MOVEMENT, "Move faster on the ground and in the air.");
        persistent = false;
    }

    @Override
    public void onTick() {
        var p = MC.player;
        if (p == null || MC.screen != null) return;
        if (p.isFallFlying() || p.isPassenger()) return;

        Options o = MC.options;
        double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double left = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        if (forward == 0 && left == 0) return;

        double yaw = Math.toRadians(p.getYRot());
        double mx = -Math.sin(yaw) * forward + Math.cos(yaw) * left;
        double mz = Math.cos(yaw) * forward + Math.sin(yaw) * left;
        double len = Math.sqrt(mx * mx + mz * mz);
        if (len < 1.0e-4) return;
        mx /= len;
        mz /= len;

        Vec3 dm = p.getDeltaMovement();
        p.setDeltaMovement(mx * speed.num(), dm.y, mz * speed.num());
    }
}
