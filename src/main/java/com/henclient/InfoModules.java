package com.henclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/** All the information-only HUD modules. */
public final class InfoModules {
    private InfoModules() {}

    // ------------------------------------------------------------------

    public static final class Coordinates extends Feature {
        public Coordinates() {
            super("coords", "Coordinates", Category.HUD, "Your position, plus the Nether/Overworld equivalent.");
            enabled = true;
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            var p = MC.player;
            if (p == null) return;
            int x = (int) Math.floor(p.getX());
            int y = (int) Math.floor(p.getY());
            int z = (int) Math.floor(p.getZ());
            out.add(new Hud.Line("XYZ", x + " " + y + " " + z, Theme.TEXT));

            if (MC.level != null) {
                var dim = MC.level.dimension();
                if (dim.equals(Level.OVERWORLD)) {
                    out.add(new Hud.Line("Nether", Math.floorDiv(x, 8) + " " + Math.floorDiv(z, 8), Theme.SUBTLE));
                } else if (dim.equals(Level.NETHER)) {
                    out.add(new Hud.Line("Overworld", (x * 8) + " " + (z * 8), Theme.SUBTLE));
                }
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Fps extends Feature {
        public Fps() {
            super("fps", "FPS", Category.HUD, "Frames per second.");
            enabled = true;
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            int fps = MC.getFps();
            int color = fps >= 60 ? Theme.GOOD : (fps >= 30 ? Theme.WARN : Theme.BAD);
            out.add(new Hud.Line("FPS", Integer.toString(fps), color));
        }
    }

    // ------------------------------------------------------------------

    public static final class Ping extends Feature {
        public Ping() {
            super("ping", "Ping", Category.HUD, "Latency to the server.");
            enabled = true;
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            var p = MC.player;
            var connection = MC.getConnection();
            if (p == null || connection == null) return;
            var info = connection.getPlayerInfo(p.getUUID());
            if (info == null) return;
            int ms = info.getLatency();
            int color = ms <= 80 ? Theme.GOOD : (ms <= 200 ? Theme.WARN : Theme.BAD);
            out.add(new Hud.Line("Ping", ms + " ms", color));
        }
    }

    // ------------------------------------------------------------------

    public static final class Direction extends Feature {
        private static final String[] NAMES = {
                "South", "South-West", "West", "North-West",
                "North", "North-East", "East", "South-East"};
        private static final String[] AXES = {
                "+Z", "+Z -X", "-X", "-X -Z", "-Z", "-Z +X", "+X", "+X +Z"};

        public Direction() {
            super("direction", "Direction", Category.HUD, "Compass heading and axis.");
            enabled = true;
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            var p = MC.player;
            if (p == null) return;
            int index = Math.floorMod(Math.round(p.getYRot() / 45.0f), 8);
            out.add(new Hud.Line("Facing", NAMES[index] + " (" + AXES[index] + ")", Theme.TEXT));
        }
    }

    // ------------------------------------------------------------------

    public static final class Biome extends Feature {
        public Biome() {
            super("biome", "Biome", Category.HUD, "Name of the biome you are in.");
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            var p = MC.player;
            if (p == null || MC.level == null) return;
            var biome = MC.level.getBiome(p.blockPosition());
            String raw = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("unknown");
            out.add(new Hud.Line("Biome", prettify(raw), Theme.TEXT));
        }

        private static String prettify(String raw) {
            StringBuilder sb = new StringBuilder();
            for (String word : raw.split("_")) {
                if (word.isEmpty()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------

    public static final class SpeedMeter extends Feature {
        private double lastX, lastZ;
        private boolean haveLast = false;
        private double bps = 0.0;

        public SpeedMeter() {
            super("speedmeter", "Speed Meter", Category.HUD, "Horizontal speed in blocks per second.");
        }

        @Override
        public void onTick() {
            var p = MC.player;
            if (p == null) return;
            double x = p.getX();
            double z = p.getZ();
            if (haveLast) {
                double dx = x - lastX;
                double dz = z - lastZ;
                double now = Math.sqrt(dx * dx + dz * dz) * 20.0;
                bps += (now - bps) * 0.3; // smooth it
            }
            lastX = x;
            lastZ = z;
            haveLast = true;
        }

        @Override
        public void onWorldLeave() {
            haveLast = false;
            bps = 0.0;
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            out.add(new Hud.Line("Speed", String.format(Locale.ROOT, "%.1f b/s", bps), Theme.TEXT));
        }
    }

    // ------------------------------------------------------------------

    public static final class Armor extends Feature {
        private static final EquipmentSlot[] SLOTS = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        private static final String[] NAMES = {"Helmet", "Chest", "Legs", "Boots"};

        public Armor() {
            super("armor", "Armor", Category.HUD, "Durability of worn armor.");
            enabled = true;
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            var p = MC.player;
            if (p == null) return;
            for (int i = 0; i < SLOTS.length; i++) {
                ItemStack stack = p.getItemBySlot(SLOTS[i]);
                if (stack.isEmpty()) continue;
                if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                    int max = stack.getMaxDamage();
                    int pct = (int) Math.round((max - stack.getDamageValue()) * 100.0 / max);
                    int color = pct > 50 ? Theme.GOOD : (pct > 20 ? Theme.WARN : Theme.BAD);
                    out.add(new Hud.Line(NAMES[i], pct + "%", color));
                } else {
                    out.add(new Hud.Line(NAMES[i], "OK", Theme.GOOD));
                }
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Potions extends Feature {
        public Potions() {
            super("potions", "Potion Effects", Category.HUD, "Active effects with time left.");
            enabled = true;
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            var p = MC.player;
            if (p == null) return;
            int shown = 0;
            for (MobEffectInstance inst : p.getActiveEffects()) {
                if (shown >= 6) break;
                String name = inst.getEffect().value().getDisplayName().getString();
                if (inst.getAmplifier() > 0) name += " " + (inst.getAmplifier() + 1);
                String time;
                if (inst.isInfiniteDuration()) {
                    time = "inf";
                } else {
                    int secs = Math.max(0, inst.getDuration() / 20);
                    time = (secs / 60) + ":" + String.format(Locale.ROOT, "%02d", secs % 60);
                }
                int color = inst.getEffect().value().isBeneficial() ? Theme.GOOD : Theme.BAD;
                out.add(new Hud.Line(name, time, color));
                shown++;
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class ItemCounter extends Feature {
        public ItemCounter() {
            super("itemcounter", "Item Counter", Category.HUD,
                    "Totems, gapples, crystals, pearls, XP bottles.");
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            var p = MC.player;
            if (p == null) return;

            int totems = 0, gapples = 0, crystals = 0, pearls = 0, xp = 0;
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty()) continue;
                int n = s.getCount();
                if (s.is(Items.TOTEM_OF_UNDYING)) totems += n;
                else if (s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE)) gapples += n;
                else if (s.is(Items.END_CRYSTAL)) crystals += n;
                else if (s.is(Items.ENDER_PEARL)) pearls += n;
                else if (s.is(Items.EXPERIENCE_BOTTLE)) xp += n;
            }

            if (totems > 0) out.add(new Hud.Line("Totems", Integer.toString(totems), Theme.WARN));
            if (gapples > 0) out.add(new Hud.Line("Gapples", Integer.toString(gapples), Theme.WARN));
            if (crystals > 0) out.add(new Hud.Line("Crystals", Integer.toString(crystals), Theme.TEXT));
            if (pearls > 0) out.add(new Hud.Line("Pearls", Integer.toString(pearls), Theme.TEXT));
            if (xp > 0) out.add(new Hud.Line("XP bottles", Integer.toString(xp), Theme.TEXT));
        }
    }

    // ------------------------------------------------------------------

    public static final class Health extends Feature {
        public Health() {
            super("health", "Health", Category.HUD, "Health, absorption and food.");
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            var p = MC.player;
            if (p == null) return;
            float hp = p.getHealth();
            float max = Math.max(1.0f, p.getMaxHealth());
            String value = String.format(Locale.ROOT, "%.1f", hp);
            if (p.getAbsorptionAmount() > 0.0f) {
                value += String.format(Locale.ROOT, " +%.1f", p.getAbsorptionAmount());
            }
            out.add(new Hud.Line("Health", value, Gfx.healthColor(hp / max)));
            out.add(new Hud.Line("Food", Integer.toString(p.getFoodData().getFoodLevel()), Theme.TEXT));
        }
    }

    // ------------------------------------------------------------------

    public static final class Clock extends Feature {
        private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

        public Clock() {
            super("clock", "Clock", Category.HUD, "Your real-world time.");
        }

        @Override
        public void hudLines(List<Hud.Line> out) {
            out.add(new Hud.Line("Time", LocalTime.now().format(FORMAT), Theme.TEXT));
        }
    }

    // ------------------------------------------------------------------

    /** WASD / mouse button display with clicks per second, bottom-right of the screen. */
    public static final class Keystrokes extends Feature {
        private final Deque<Long> leftClicks = new ArrayDeque<>();
        private final Deque<Long> rightClicks = new ArrayDeque<>();
        private boolean leftWas = false;
        private boolean rightWas = false;

        public Keystrokes() {
            super("keystrokes", "Keystrokes", Category.HUD, "WASD, mouse buttons and CPS.");
        }

        @Override
        public void onFrame(GuiGraphics g, float partial) {
            long window = MC.getWindow().handle();
            long now = System.currentTimeMillis();

            boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (MC.screen == null) {
                if (left && !leftWas) leftClicks.addLast(now);
                if (right && !rightWas) rightClicks.addLast(now);
            }
            leftWas = left;
            rightWas = right;
            while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000L) leftClicks.removeFirst();
            while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000L) rightClicks.removeFirst();

            var o = MC.options;
            int sw = MC.getWindow().getGuiScaledWidth();
            int sh = MC.getWindow().getGuiScaledHeight();

            int size = 20;
            int gap = 2;
            int blockW = size * 3 + gap * 2;
            int x0 = sw - 8 - blockW;
            int y0 = sh - 8 - (size * 3 + gap * 2 + 12 + gap);

            key(g, x0 + size + gap, y0, size, "W", o.keyUp.isDown());
            key(g, x0, y0 + size + gap, size, "A", o.keyLeft.isDown());
            key(g, x0 + size + gap, y0 + size + gap, size, "S", o.keyDown.isDown());
            key(g, x0 + (size + gap) * 2, y0 + size + gap, size, "D", o.keyRight.isDown());

            int mouseY = y0 + (size + gap) * 2;
            int halfW = (blockW - gap) / 2;
            wide(g, x0, mouseY, halfW, size, "LMB " + leftClicks.size(), left);
            wide(g, x0 + halfW + gap, mouseY, halfW, size, "RMB " + rightClicks.size(), right);

            int spaceY = mouseY + size + gap;
            boolean space = o.keyJump.isDown();
            Gfx.roundRect(g, x0, spaceY, x0 + blockW, spaceY + 12, 3,
                    space ? Theme.accent() : Theme.PILL);
        }

        private static void key(GuiGraphics g, int x, int y, int size, String label, boolean down) {
            Gfx.roundRect(g, x, y, x + size, y + size, 3, down ? Theme.accent() : Theme.PILL);
            Gfx.textCentered(g, label, x + size / 2, y + size / 2 - 4, down ? 0xFF101014 : Theme.TEXT);
        }

        private static void wide(GuiGraphics g, int x, int y, int w, int h, String label, boolean down) {
            Gfx.roundRect(g, x, y, x + w, y + h, 3, down ? Theme.accent() : Theme.PILL);
            Gfx.textCentered(g, label, x + w / 2, y + h / 2 - 4, down ? 0xFF101014 : Theme.TEXT);
        }
    }
}
