package com.henclient;

/** Creative-style flight. Most multiplayer servers will kick or rubber-band this. */
public final class FlightModule extends Feature {
    private final Setting speed = addNum("Speed", 2.0, 0.5, 8.0, 0.5);
    private final Setting autoFly = addBool("Auto fly", true);

    public FlightModule() {
        super("flight", "Flight", Category.MOVEMENT, "Fly like in creative mode.");
        persistent = false;
    }

    @Override
    public void onTick() {
        var p = MC.player;
        if (p == null) return;
        var abilities = p.getAbilities();
        abilities.mayfly = true;
        abilities.setFlyingSpeed(0.05F * (float) speed.num());
        if (autoFly.on() && !abilities.flying) {
            abilities.flying = true;
        }
    }

    @Override
    public void onDisable() {
        var p = MC.player;
        if (p == null) return;
        var abilities = p.getAbilities();
        abilities.setFlyingSpeed(0.05F);
        boolean allowed = p.isCreative() || p.isSpectator();
        abilities.mayfly = allowed;
        if (!allowed) {
            abilities.flying = false;
        }
    }
}
