package com.henclient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Saves key binds, on/off states and settings to config/henclient-v2.properties. */
public final class Config {
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("henclient-v2.properties");

    private Config() {}

    public static void load() {
        Properties props = new Properties();
        if (Files.exists(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                props.load(in);
            } catch (IOException e) {
                HenClient.LOGGER.warn("Could not read config", e);
            }
        }

        HenClient.guiKey = getInt(props, "key.gui", HenClient.guiKey);
        Theme.index = Math.max(0, Math.min(Theme.NAMES.length - 1, getInt(props, "theme", Theme.index)));
        Hud.watermark = getBool(props, "hud.watermark", Hud.watermark);
        Hud.arrayList = getBool(props, "hud.arraylist", Hud.arrayList);

        for (Feature f : ModuleManager.all()) {
            f.key = getInt(props, "key." + f.id, f.key);
            if (f.persistent) {
                f.enabled = getBool(props, "enabled." + f.id, f.enabled);
            }
            for (Setting s : f.settings) {
                String raw = props.getProperty("setting." + f.id + "." + s.key());
                if (raw == null) continue;
                if (s.type == Setting.Type.BOOL) {
                    s.flag = Boolean.parseBoolean(raw);
                } else {
                    try {
                        s.setNum(Double.parseDouble(raw));
                    } catch (NumberFormatException ignored) {
                        // keep default
                    }
                }
            }
        }
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("key.gui", Integer.toString(HenClient.guiKey));
        props.setProperty("theme", Integer.toString(Theme.index));
        props.setProperty("hud.watermark", Boolean.toString(Hud.watermark));
        props.setProperty("hud.arraylist", Boolean.toString(Hud.arrayList));

        for (Feature f : ModuleManager.all()) {
            props.setProperty("key." + f.id, Integer.toString(f.key));
            if (f.persistent) {
                props.setProperty("enabled." + f.id, Boolean.toString(f.enabled));
            }
            for (Setting s : f.settings) {
                String k = "setting." + f.id + "." + s.key();
                props.setProperty(k, s.type == Setting.Type.BOOL
                        ? Boolean.toString(s.flag)
                        : Double.toString(s.value));
            }
        }

        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, "Hen Client settings");
            }
        } catch (IOException e) {
            HenClient.LOGGER.warn("Could not write config", e);
        }
    }

    private static int getInt(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean getBool(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }
}
