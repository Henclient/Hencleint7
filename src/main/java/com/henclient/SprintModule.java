package com.henclient;

import net.minecraft.client.Options;

/** Holds the sprint key down for you while you walk forward. */
public final class SprintModule extends Feature {
    private boolean forced = false;

    public SprintModule() {
        super("sprint", "Auto Sprint", Category.MOVEMENT, "Always sprint while walking forward.");
    }

    @Override
    public void onTick() {
        var p = MC.player;
        if (p == null || MC.screen != null) return;

        Options o = MC.options;
        boolean want = o.keyUp.isDown()
                && !p.isShiftKeyDown()
                && !p.horizontalCollision
                && !p.isUsingItem()
                && p.getFoodData().getFoodLevel() > 6;

        if (want && !o.keySprint.isDown()) {
            o.keySprint.setDown(true);
            forced = true;
        } else if (!want && forced) {
            o.keySprint.setDown(false);
            forced = false;
        }
    }

    @Override
    public void onDisable() {
        if (forced) {
            MC.options.keySprint.setDown(false);
            forced = false;
        }
    }
}
