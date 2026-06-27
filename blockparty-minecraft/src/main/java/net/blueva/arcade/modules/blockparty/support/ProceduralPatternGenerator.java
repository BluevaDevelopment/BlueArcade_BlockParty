package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.modules.blockparty.state.FloorBounds;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ProceduralPatternGenerator {
    public static final List<String> TEMPLATE_TYPES = List.of(
            "stripes", "diagonal", "rainbow", "checker", "rings", "sectors",
            "islands", "waves", "spiral", "creeper", "mosaic"
    );

    private static final List<Material> CONCRETE_PALETTE = List.of(
            Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.GRAY_CONCRETE,
            Material.BLACK_CONCRETE, Material.BROWN_CONCRETE, Material.RED_CONCRETE,
            Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
            Material.GREEN_CONCRETE, Material.CYAN_CONCRETE, Material.LIGHT_BLUE_CONCRETE,
            Material.BLUE_CONCRETE, Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE,
            Material.PINK_CONCRETE
    );

    private static final int[][] CREEPER = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 1, 0, 0, 1, 1, 0},
            {0, 1, 1, 0, 0, 1, 1, 0},
            {0, 0, 0, 1, 1, 0, 0, 0},
            {0, 0, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0}
    };

    public ProceduralBlockPattern generate(FloorBounds floor, World world, String template, long seed,
                                           int size, int colorCount, int minTargetBlocks) {
        if (floor == null || world == null) {
            throw new IllegalArgumentException("Floor bounds and world are required");
        }
        if (!TEMPLATE_TYPES.contains(template)) {
            throw new IllegalArgumentException("Unknown procedural template: " + template);
        }

        int minX = Math.min(floor.min().getBlockX(), floor.max().getBlockX());
        int maxX = Math.max(floor.min().getBlockX(), floor.max().getBlockX());
        int minY = Math.min(floor.min().getBlockY(), floor.max().getBlockY());
        int maxY = Math.max(floor.min().getBlockY(), floor.max().getBlockY());
        int minZ = Math.min(floor.min().getBlockZ(), floor.max().getBlockZ());
        int maxZ = Math.max(floor.min().getBlockZ(), floor.max().getBlockZ());
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;

        Random random = new Random(seed);
        List<Material> shuffled = new ArrayList<>(CONCRETE_PALETTE);
        Collections.shuffle(shuffled, random);
        int selectedColors = Math.max(2, Math.min(colorCount, shuffled.size()));
        List<Material> palette = new ArrayList<>(shuffled.subList(0, selectedColors));
        PatternContext pattern = new PatternContext(template, width, depth, Math.max(1, size), seed, random, palette);

        Map<Location, Material> blocks = new LinkedHashMap<>();
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                Material material = materialAt(pattern, x, z);
                for (int y = minY; y <= maxY; y++) {
                    blocks.put(new Location(world, minX + x, y, minZ + z), material);
                    counts.merge(material, 1, Integer::sum);
                }
            }
        }

        List<Material> targets = counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= minTargetBlocks)
                .map(Map.Entry::getKey)
                .toList();
        if (targets.isEmpty()) {
            targets = new ArrayList<>(counts.keySet());
        }
        return new ProceduralBlockPattern(blocks, template, seed, targets);
    }

    private Material materialAt(PatternContext context, int x, int z) {
        if (context.template.equals("creeper")) {
            return creeperMaterial(context, x, z);
        }
        int index = switch (context.template) {
            case "stripes" -> context.seed % 2 == 0 ? x / context.size : z / context.size;
            case "diagonal" -> diagonalIndex(context, x, z);
            case "rainbow" -> rainbowIndex(context, x, z);
            case "checker" -> checkerIndex(context, x, z);
            case "rings" -> ringsIndex(context, x, z);
            case "sectors" -> sectorIndex(context, x, z);
            case "islands" -> nearestIsland(context, x, z);
            case "waves" -> wavesIndex(context, x, z);
            case "spiral" -> spiralIndex(context, x, z);
            case "mosaic" -> mosaicIndex(context, x, z);
            default -> 0;
        };
        return context.palette.get(Math.floorMod(index, context.palette.size()));
    }

    private int diagonalIndex(PatternContext context, int x, int z) {
        int offset = seedOffset(context, context.size * context.palette.size(), 11);
        int coordinate = (context.seed & 1L) == 0 ? x + z : x - z;
        return Math.floorDiv(coordinate + offset, context.size);
    }

    private int checkerIndex(PatternContext context, int x, int z) {
        int offsetX = seedOffset(context, context.size * context.palette.size(), 7);
        int offsetZ = seedOffset(context, context.size * context.palette.size(), 19);
        return Math.floorDiv(x + offsetX, context.size)
                + Math.floorDiv(z + offsetZ, context.size) * 5;
    }

    private int rainbowIndex(PatternContext context, int x, int z) {
        int direction = Math.floorMod((int) context.seed, 4);
        int primary = switch (direction) {
            case 0 -> x;
            case 1 -> z;
            case 2 -> x + z;
            default -> x - z;
        };
        int secondary = direction % 2 == 0 ? z : x;
        double bend = Math.sin(secondary / (double) (context.size * 2) + seedPhase(context.seed))
                * context.size * 2;
        int offset = seedOffset(context, context.size * context.palette.size(), 17);
        return (int) Math.floor((primary + offset + bend) / context.size);
    }

    private int wavesIndex(PatternContext context, int x, int z) {
        boolean swapAxes = (context.seed & 1L) != 0;
        int primary = swapAxes ? x : z;
        int secondary = swapAxes ? z : x;
        double phase = seedPhase(context.seed ^ 0x9E3779B97F4A7C15L);
        double wave = Math.sin(secondary / (double) (context.size * 2) + phase) * context.size * 2;
        int offset = seedOffset(context, context.size * context.palette.size(), 23);
        return (int) Math.floor((primary + offset + wave) / context.size);
    }

    private int ringsIndex(PatternContext context, int x, int z) {
        double shiftX = ((context.seed >>> 8) & 7) - 3.5;
        double shiftZ = ((context.seed >>> 16) & 7) - 3.5;
        double dx = x - (context.width - 1) / 2.0 - shiftX;
        double dz = z - (context.depth - 1) / 2.0 - shiftZ;
        return (int) Math.floor(Math.sqrt(dx * dx + dz * dz) / context.size);
    }

    private int sectorIndex(PatternContext context, int x, int z) {
        double angle = centerAngle(context, x, z) + seedPhase(context.seed);
        return (int) Math.floor(angle / (Math.PI * 2.0) * context.palette.size());
    }

    private int spiralIndex(PatternContext context, int x, int z) {
        double angle = centerAngle(context, x, z) + seedPhase(context.seed);
        double direction = (context.seed & 1L) == 0 ? 1.0 : -1.0;
        return (int) Math.floor(distanceFromCenter(context, x, z) / context.size
                + direction * angle / (Math.PI * 2.0) * 4.0);
    }

    private int nearestIsland(PatternContext context, int x, int z) {
        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < context.islands.size(); i++) {
            Point point = context.islands.get(i);
            long dx = x - point.x;
            long dz = z - point.z;
            long distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private Material creeperMaterial(PatternContext context, int x, int z) {
        int px = Math.min(7, x * 8 / context.width);
        int pz = Math.min(7, z * 8 / context.depth);
        if (CREEPER[pz][px] == 1) {
            return Material.BLACK_CONCRETE;
        }
        return context.palette.stream()
                .filter(material -> material != Material.BLACK_CONCRETE)
                .findFirst()
                .orElse(Material.GREEN_CONCRETE);
    }

    private int mosaicIndex(PatternContext context, int x, int z) {
        int cellX = x / context.size;
        int cellZ = z / context.size;
        long value = context.seed ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xC2B2AE3D27D4EB4FL);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return (int) value;
    }

    private int seedOffset(PatternContext context, int bound, int shift) {
        return bound <= 0 ? 0 : Math.floorMod((int) (context.seed >>> shift), bound);
    }

    private double seedPhase(long seed) {
        long mixed = seed ^ (seed >>> 33);
        return Math.floorMod(mixed, 1_000_000L) / 1_000_000.0 * Math.PI * 2.0;
    }

    private double distanceFromCenter(PatternContext context, int x, int z) {
        double dx = x - (context.width - 1) / 2.0;
        double dz = z - (context.depth - 1) / 2.0;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private double centerAngle(PatternContext context, int x, int z) {
        double dx = x - (context.width - 1) / 2.0;
        double dz = z - (context.depth - 1) / 2.0;
        return Math.atan2(dz, dx) + Math.PI;
    }

    private static final class PatternContext {
        private final String template;
        private final int width;
        private final int depth;
        private final int size;
        private final long seed;
        private final List<Material> palette;
        private final List<Point> islands;

        private PatternContext(String template, int width, int depth, int size, long seed, Random random,
                               List<Material> palette) {
            this.template = template;
            this.width = width;
            this.depth = depth;
            this.size = size;
            this.seed = seed;
            this.palette = palette;
            this.islands = createIslands(width, depth, size, random);
        }

        private static List<Point> createIslands(int width, int depth, int size, Random random) {
            int count = Math.max(16, Math.min(128, (width * depth) / Math.max(16, size * size * 4)));
            List<Point> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                points.add(new Point(random.nextInt(width), random.nextInt(depth)));
            }
            return points;
        }
    }

    private record Point(int x, int z) {
    }
}
