package com.henclient;

import java.util.Locale;

/** A single tweakable value on a feature: either an on/off flag or a number. */
public final class Setting {
    public enum Type { BOOL, NUM }

    public final Type type;
    public final String name;
    public final double min;
    public final double max;
    public final double step;
    public boolean flag;
    public double value;

    private Setting(Type type, String name, boolean flag, double value,
                    double min, double max, double step) {
        this.type = type;
        this.name = name;
        this.flag = flag;
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public static Setting bool(String name, boolean def) {
        return new Setting(Type.BOOL, name, def, 0, 0, 1, 1);
    }

    public static Setting num(String name, double def, double min, double max, double step) {
        return new Setting(Type.NUM, name, false, def, min, max, step);
    }

    public boolean on() {
        return flag;
    }

    public double num() {
        return value;
    }

    public int whole() {
        return (int) Math.round(value);
    }

    public void setNum(double v) {
        double snapped = Math.round((v - min) / step) * step + min;
        snapped = Math.round(snapped * 10000.0) / 10000.0;
        value = Math.max(min, Math.min(max, snapped));
    }

    /** Stable config key, e.g. "Utility blocks" -> "utility_blocks". */
    public String key() {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public String display() {
        if (type == Type.BOOL) {
            return flag ? "ON" : "OFF";
        }
        int decimals = step >= 1.0 ? 0 : (step >= 0.1 ? 1 : 2);
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }
}
