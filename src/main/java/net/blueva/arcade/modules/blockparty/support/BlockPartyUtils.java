package net.blueva.arcade.modules.blockparty.support;

import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.Locale;

public final class BlockPartyUtils {

    private BlockPartyUtils() {
    }

    public static String formatMaterialName(Material material) {
        if (material == null) {
            return "Unknown";
        }
        String name = material.name().toLowerCase(Locale.ENGLISH).replace("_", " ");
        return name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
    }

    public static Particle parseParticle(String name) {
        try {
            String upper = name.toUpperCase(Locale.ENGLISH);
            if ("SPELL_WITCH".equals(upper)) {
                upper = "WITCH";
            }
            return Particle.valueOf(upper);
        } catch (Exception e) {
            return Particle.FLAME;
        }
    }

    public static Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return fallback;
        }
    }

    public static long secondsToTicks(double seconds) {
        return Math.max(1L, (long) Math.round(seconds * 20.0));
    }

    public static double ticksToSeconds(long ticks) {
        return Math.max(0D, ticks / 20.0D);
    }

    public static String formatSeconds(double seconds) {
        return String.format(Locale.ENGLISH, "%.1f", seconds);
    }
}
