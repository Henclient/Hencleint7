package com.henclient;

import net.minecraft.world.entity.ai.attributes.Attributes;

/** Walk up blocks taller than a slab by raising the step-height attribute. */
public final class StepModule extends Feature {
    private final Setting height = addNum("Height", 1.0, 0.6, 3.0, 0.1);

    public StepModule() {
        super("step", "Step", Category.MOVEMENT, "Step up whole blocks without jumping.");
        persistent = false;
    }

    @Override
    public void onTick() {
        var p = MC.player;
        if (p == null) return;
        var attr = p.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null && Math.abs(attr.getBaseValue() - height.num()) > 1.0e-6) {
            attr.setBaseValue(height.num());
        }
    }

    @Override
    public void onDisable() {
        var p = MC.player;
        if (p == null) return;
        var attr = p.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(0.6);
        }
    }
}
