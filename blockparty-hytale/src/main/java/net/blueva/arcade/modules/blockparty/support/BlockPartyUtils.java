package net.blueva.arcade.modules.blockparty.support;

import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class BlockPartyUtils {

    /**
     * Empty block ID used for clearing blocks in Hytale.
     */
    private static final String EMPTY_BLOCK_ID = "Empty";

    private BlockPartyUtils() {
    }

    public static String formatMaterialName(String material) {
        if (material == null || material.isBlank()) {
            return "Unknown";
        }
        String name = material.contains(":") ? material.substring(material.indexOf(':') + 1) : material;
        name = name.replace("_", " ").toLowerCase(Locale.ENGLISH);
        if (name.isBlank()) {
            return "Unknown";
        }
        return name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
    }

    public static String normalizeMaterial(String material) {
        if (material == null) {
            return null;
        }
        return material.trim();
    }

    public static boolean isSameMaterial(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return normalizeMaterial(first).equalsIgnoreCase(normalizeMaterial(second));
    }

    public static boolean isAir(String material) {
        if (material == null) {
            return true;
        }
        String normalized = normalizeMaterial(material).toLowerCase(Locale.ENGLISH);
        return normalized.isBlank()
                || "empty".equals(normalized)
                || "hytale:empty".equals(normalized)
                || "0".equals(normalized)
                || "air".equals(normalized)
                || "minecraft:air".equals(normalized)
                || "hytale:air".equals(normalized)
                || "filter_air_block".equals(normalized);
    }

    public static String getAirBlockId() {
        return EMPTY_BLOCK_ID;
    }

    public static Location toBlockLocation(Location location) {
        if (location == null) {
            return null;
        }
        Vector3d pos = location.getPosition();
        double x = Math.floor(pos.x);
        double y = Math.floor(pos.y);
        double z = Math.floor(pos.z);
        return new Location(location.getWorld(), x, y, z);
    }

    public static Location offsetBlockLocation(Location location, int offsetX, int offsetY, int offsetZ) {
        Location base = toBlockLocation(location);
        if (base == null) {
            return null;
        }
        Vector3d pos = base.getPosition();
        return new Location(base.getWorld(), pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ);
    }

    public static Location offsetLocation(Location location, double offsetX, double offsetY, double offsetZ) {
        if (location == null) {
            return null;
        }
        Vector3d pos = location.getPosition();
        return new Location(location.getWorld(), pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ);
    }

    public static Map<Location, String> normalizeBlockMap(Map<Location, String> blocks) {
        Map<Location, String> normalized = new HashMap<>();
        if (blocks == null || blocks.isEmpty()) {
            return normalized;
        }
        for (Map.Entry<Location, String> entry : blocks.entrySet()) {
            Location key = toBlockLocation(entry.getKey());
            if (key != null) {
                normalized.put(key, entry.getValue());
            }
        }
        return normalized;
    }

    public static boolean isSameBlock(Location first, Location second) {
        Location firstBlock = toBlockLocation(first);
        Location secondBlock = toBlockLocation(second);
        if (firstBlock == null || secondBlock == null) {
            return false;
        }
        if (firstBlock.getWorld() == null || secondBlock.getWorld() == null) {
            return false;
        }
        if (!firstBlock.getWorld().equalsIgnoreCase(secondBlock.getWorld())) {
            return false;
        }
        Vector3d firstPos = firstBlock.getPosition();
        Vector3d secondPos = secondBlock.getPosition();
        return (int) firstPos.x == (int) secondPos.x
                && (int) firstPos.y == (int) secondPos.y
                && (int) firstPos.z == (int) secondPos.z;
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
