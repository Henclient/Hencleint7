package com.henclient;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Full brightness via a client-side Night Vision effect (the server never hears about it). */
public final class FullbrightModule extends Feature {
    private boolean ours = false;

    public FullbrightModule() {
        super("fullbright", "Fullbright", Category.RENDER,
                "See clearly in caves and at night.");
    }

    @Override
    public void onTick() {
        var p = MC.player;
        if (p == null) return;

        MobEffectInstance current = p.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || (ours && current.getDuration() < 400)) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20000, 0, false, false));
            ours = true;
        }
    }

    @Override
    public void onDisable() {
        var p = MC.player;
        if (ours && p != null) {
            p.removeEffect(MobEffects.NIGHT_VISION);
        }
        ours = false;
    }

    @Override
    public void onWorldLeave() {
        ours = false;
    }
}
