package com.henclient;

public enum Category {
    RENDER("Render"),
    MOVEMENT("Movement"),
    COMBAT("Combat"),
    HUD("HUD");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
