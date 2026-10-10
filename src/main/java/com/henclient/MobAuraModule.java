package com.henclient;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/** Automatically attacks the nearest hostile mob in reach when your weapon is charged. */
public final class MobAuraModule extends Feature {
    private final Setting reach = addNum("Reach", 3.5, 2.0, 4.5, 0.1);
    private final Setting needLineOfSight = addBool("Need line of sight", true);

    public MobAuraModule() {
        super("mobaura", "Mob Aura", Category.COMBAT, "Auto-attacks nearby hostile mobs.");
        persistent = false;
    }

    @Override
    public void onTick() {
        var p = MC.player;
        if (p == null || MC.level == null || MC.gameMode == null || MC.screen != null) return;
        if (!p.isAlive() || p.getAttackStrengthScale(0.5F) < 1.0F) return;

        double maxSq = reach.num() * reach.num();
        Entity best = null;
        double bestSq = maxSq;

        for (Entity e : MC.level.entitiesForRendering()) {
            if (!(e instanceof Enemy) || !(e instanceof LivingEntity living) || !living.isAlive()) continue;
            double d = p.distanceToSqr(e);
            if (d > bestSq) continue;
            if (needLineOfSight.on() && !p.hasLineOfSight(e)) continue;
            best = e;
            bestSq = d;
        }

        if (best != null) {
            MC.gameMode.attack(p, best);
            p.swing(InteractionHand.MAIN_HAND);
        }
    }
}
