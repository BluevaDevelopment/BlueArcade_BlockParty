package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.world.BlocksAPI;
import net.blueva.arcade.modules.blockparty.state.FloorBounds;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Location;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ProceduralPatternGenerator {
    public static final List<String> TEMPLATE_TYPES = List.of(
            "stripes", "diagonal", "rainbow", "checker", "rings", "sectors",
            "islands", "waves", "spiral", "creeper", "mosaic"
    );

    // Default Hytale wool palette.
    private static final List<String> COLOR_PALETTE = List.of(
            "Cloth_Block_Wool_White", "Cloth_Block_Wool_Blue", "Cloth_Block_Wool_Cyan",
            "Cloth_Block_Wool_Green", "Cloth_Block_Wool_Orange", "Cloth_Block_Wool_Pink",
            "Cloth_Block_Wool_Purple", "Cloth_Block_Wool_Red", "Cloth_Block_Wool_Black",
            "Cloth_Block_Wool_Yellow"
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

    private final BlocksAPI<Location, String, Holder> blocksAPI;

    public ProceduralPatternGenerator(BlocksAPI<Location, String, Holder> blocksAPI) {
        this.blocksAPI = blocksAPI;
    }

    public ProceduralBlockPattern generate(FloorBounds floor, String worldName, String template, long seed,
                                           int size, int colorCount, int minTargetBlocks) {
        if (floor == null || worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("Floor bounds and a valid world are required");
        }
        if (!TEMPLATE_TYPES.contains(template)) {
            throw new IllegalArgumentException("Unknown procedural template: " + template);
        }

        Vector3d minPos = floor.min().getPosition();
        Vector3d maxPos = floor.max().getPosition();

        int minX = (int) Math.floor(Math.min(minPos.x, maxPos.x));
        int maxX = (int) Math.floor(Math.max(minPos.x, maxPos.x));
        int minY = (int) Math.floor(Math.min(minPos.y, maxPos.y));
        int maxY = (int) Math.floor(Math.max(minPos.y, maxPos.y));
        int minZ = (int) Math.floor(Math.min(minPos.z, maxPos.z));
        int maxZ = (int) Math.floor(Math.max(minPos.z, maxPos.z));
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;

        Random random = new Random(seed);
        List<String> shuffled = new ArrayList<>(COLOR_PALETTE);
        Collections.shuffle(shuffled, random);
        int selectedColors = Math.max(2, Math.min(colorCount, shuffled.size()));
        List<String> palette = new ArrayList<>(shuffled.subList(0, selectedColors));
        if ("creeper".equals(template)) {
            ensureCreeperColors(palette, random);
        }
        PatternContext pattern = new PatternContext(template, width, depth, Math.max(1, size), seed, random, palette);

        Map<Location, String> blocks = new LinkedHashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        // Generate only the top layer to avoid overwhelming the Hytale block task queue.
        int topY = Math.max(minY, maxY);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                String material = materialAt(pattern, x, z);
                Location loc = new Location(worldName, minX + x, topY, minZ + z);
                blocks.put(loc, material);
                counts.merge(material, 1, Integer::sum);
            }
        }

        List<String> targets = counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= minTargetBlocks)
                .map(Map.Entry::getKey)
                .toList();
        if (targets.isEmpty()) {
            targets = new ArrayList<>(counts.keySet());
        }
        return new ProceduralBlockPattern(blocksAPI, blocks, template, seed, targets);
    }

    private String materialAt(PatternContext context, int x, int z) {
        if ("creeper".equals(context.template)) {
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

    private String creeperMaterial(PatternContext context, int x, int z) {
        int px = Math.min(7, x * 8 / context.width);
        int pz = Math.min(7, z * 8 / context.depth);
        // Classic creeper: green background, black face pixels.
        return CREEPER[pz][px] == 1 ? "Cloth_Block_Wool_Black" : "Cloth_Block_Wool_Green";
    }

    private void ensureCreeperColors(List<String> palette, Random random) {
        boolean hasGreen = palette.contains("Cloth_Block_Wool_Green");
        boolean hasBlack = palette.contains("Cloth_Block_Wool_Black");
        if (!hasGreen) {
            palette.set(random.nextInt(palette.size()), "Cloth_Block_Wool_Green");
        }
        if (!hasBlack) {
            int index = random.nextInt(palette.size());
            // Keep green if it was just added; overwrite another slot when possible.
            while (palette.size() > 1 && "Cloth_Block_Wool_Green".equals(palette.get(index))) {
                index = random.nextInt(palette.size());
            }
            palette.set(index, "Cloth_Block_Wool_Black");
        }
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
        private final List<String> palette;
        private final List<Point> islands;

        private PatternContext(String template, int width, int depth, int size, long seed, Random random,
                               List<String> palette) {
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
