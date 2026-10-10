package com.henclient;

/** Narrows the field of view. Restores your real FOV when turned off or when the game closes. */
public final class ZoomModule extends Feature {
    private final Setting zoomFov = addNum("Zoom FOV", 25, 5, 60, 1);
    private int savedFov = -1;

    public ZoomModule() {
        super("zoom", "Zoom", Category.RENDER, "Telescope-style zoom.");
        persistent = false;
    }

    @Override
    public void onEnable() {
        savedFov = MC.options.fov().get();
        MC.options.fov().set(zoomFov.whole());
    }

    @Override
    public void onTick() {
        if (savedFov < 0) return;
        if (MC.options.fov().get() != zoomFov.whole()) {
            MC.options.fov().set(zoomFov.whole());
        }
    }

    @Override
    public void onDisable() {
        if (savedFov >= 0) {
            MC.options.fov().set(savedFov);
            savedFov = -1;
        }
    }
}
