package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.world.BlockPattern;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import net.blueva.arcade.modules.blockparty.state.BlockPartyState;
import net.blueva.arcade.modules.blockparty.state.FloorBounds;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PatternService {

    private final BlockPartyModule module;

    public PatternService(BlockPartyModule module) {
        this.module = module;
    }

    public Map<String, BlockPattern> loadPatterns(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                                  FloorBounds floor) {
        Map<String, BlockPattern> patterns = new LinkedHashMap<>();
        String index = context.getDataAccess().getGameData("game.patterns.index", String.class);
        List<String> names = parseIndex(index);

        for (String name : names) {
            List<String> raw = context.getDataAccess().getGameData("game.patterns." + name, List.class);
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            BlockPattern pattern = context.getBlocksAPI().parsePattern(raw);
            if (pattern != null) {
                // Temporary workaround until API 3.4 makes BlocksAPI resolve dynamic arena worlds contextually.
                patterns.put(name, normalizePatternWorld(context, floor, pattern));
            }
        }

        if (patterns.isEmpty()) {
            FloorBounds fallback = floor != null ? floor : findFloorBounds(context);
            if (fallback != null) {
                patterns.put("fallback", createFallbackPattern(fallback));
                for (Player player : context.getPlayers()) {
                    context.getMessagesAPI().sendRaw(player, "<yellow>[BlockParty]</yellow> <gray>Fallback pattern generated because none were configured.</gray>");
                }
            }
        }

        return patterns;
    }

    private BlockPattern normalizePatternWorld(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                               FloorBounds floor,
                                               BlockPattern pattern) {
        World world = resolvePatternWorld(context, floor);
        if (world == null) {
            return pattern;
        }

        Map<Location, Material> normalized = new LinkedHashMap<>();
        for (Object rawEntry : pattern.getBlocks().entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) rawEntry;
            if (!(entry.getKey() instanceof Location location) || !(entry.getValue() instanceof Material material)) {
                continue;
            }
            normalized.put(new Location(world, location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch()), material);
        }
        return new SimpleBlockPattern(normalized);
    }

    private World resolvePatternWorld(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                      FloorBounds floor) {
        World world = context.getArenaAPI().getWorld();
        if (world != null) {
            return world;
        }
        if (floor != null && floor.min() != null) {
            return floor.min().getWorld();
        }
        return null;
    }

    public BlockPattern createFallbackPattern(FloorBounds floor) {
        Map<Location, Material> map = new HashMap<>();
        Location min = floor.min();
        Location max = floor.max();
        World world = min.getWorld();
        if (world == null) {
            return new SimpleBlockPattern(map);
        }

        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Material material = module.getSettings().getFallbackMaterials()
                            .get(random.nextInt(module.getSettings().getFallbackMaterials().size()));
                    map.put(loc, material);
                }
            }
        }
        return new SimpleBlockPattern(map);
    }

    public String selectPatternKey(BlockPartyState state, boolean firstRound) {
        if (firstRound && state.getInitialPatternKey() != null && state.getPatterns().containsKey(state.getInitialPatternKey())) {
            return state.getInitialPatternKey();
        }
        if (state.getOrder().isEmpty()) {
            return null;
        }
        return state.getOrder().get(ThreadLocalRandom.current().nextInt(state.getOrder().size()));
    }

    public Material selectTargetMaterial(BlockPattern pattern) {
        List<Material> materials = pattern.getMaterials();
        List<Material> valid = new ArrayList<>();
        for (Material material : materials) {
            if (material != null && material != Material.AIR && material != Material.BARRIER) {
                valid.add(material);
            }
        }
        if (valid.isEmpty()) {
            return module.getSettings().getFallbackMaterials().isEmpty()
                    ? Material.AIR
                    : module.getSettings().getFallbackMaterials().get(0);
        }
        return valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
    }

    private List<String> parseIndex(String index) {
        if (index == null || index.isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = index.split(",");
        List<String> names = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names;
    }

    private FloorBounds findFloorBounds(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        Location storedMin = context.getDataAccess().getGameLocation("game.floor.bounds.min");
        Location storedMax = context.getDataAccess().getGameLocation("game.floor.bounds.max");

        if (storedMin != null && storedMax != null) {
            return new FloorBounds(storedMin, storedMax);
        }

        List<net.blueva.arcade.api.arena.FloorRegion<Location>> floors = context.getArenaAPI().getFloors();
        if (floors != null && !floors.isEmpty()) {
            net.blueva.arcade.api.arena.FloorRegion<Location> region = floors.get(0);
            return new FloorBounds(region.getMin(), region.getMax());
        }

        return null;
    }
}
