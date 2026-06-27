package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.world.BlockPattern;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import net.blueva.arcade.modules.blockparty.state.BlockPartyState;
import net.blueva.arcade.modules.blockparty.state.FloorBounds;
import com.hypixel.hytale.math.vector.Location;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;

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

    public Map<String, BlockPattern<Location, String>> loadPatterns(
            GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
            FloorBounds floor
    ) {
        Map<String, BlockPattern<Location, String>> patterns = new LinkedHashMap<>();
        String index = context.getDataAccess().getGameData("game.patterns.index", String.class);
        List<String> names = parseIndex(index);

        for (String name : names) {
            List<String> raw = context.getDataAccess().getGameData("game.patterns." + name, List.class);
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            BlockPattern<Location, String> pattern = context.getBlocksAPI().parsePattern(raw);
            if (pattern != null) {
                patterns.put(name, pattern);
            }
        }

        if (patterns.isEmpty()) {
            FloorBounds fallback = floor != null ? floor : findFloorBounds(context);
            if (fallback != null) {
                patterns.put("fallback", createFallbackPattern(context, fallback));
                for (Player player : context.getPlayers()) {
                    context.getMessagesAPI().sendRaw(player,
                            "<yellow>[BlockParty]</yellow> <gray>Fallback pattern generated because none were configured.</gray>");
                }
            }
        }

        return patterns;
    }

    public BlockPattern<Location, String> createFallbackPattern(
            GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
            FloorBounds floor
    ) {
        Map<Location, String> map = new HashMap<>();
        Location min = floor.min();
        Location max = floor.max();
        if (min == null || max == null) {
            return new SimpleBlockPattern(context.getBlocksAPI(), map);
        }

        Vector3d minPos = min.getPosition();
        Vector3d maxPos = max.getPosition();

        int minX = (int) Math.floor(Math.min(minPos.x, maxPos.x));
        int minY = (int) Math.floor(Math.min(minPos.y, maxPos.y));
        int minZ = (int) Math.floor(Math.min(minPos.z, maxPos.z));
        int maxX = (int) Math.floor(Math.max(minPos.x, maxPos.x));
        int maxY = (int) Math.floor(Math.max(minPos.y, maxPos.y));
        int maxZ = (int) Math.floor(Math.max(minPos.z, maxPos.z));

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(min.getWorld(), x, y, z);
                    String material = module.getSettings().getFallbackMaterials()
                            .get(random.nextInt(module.getSettings().getFallbackMaterials().size()));
                    map.put(loc, material);
                }
            }
        }
        return new SimpleBlockPattern(context.getBlocksAPI(), map);
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

    public String selectTargetMaterial(BlockPattern<Location, String> pattern) {
        List<String> materials = pattern.getMaterials();
        List<String> valid = new ArrayList<>();
        for (String material : materials) {
            if (!BlockPartyUtils.isAir(material)) {
                valid.add(material);
            }
        }
        if (valid.isEmpty()) {
            return module.getSettings().getFallbackMaterials().isEmpty()
                    ? BlockPartyUtils.getAirBlockId()
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

    private FloorBounds findFloorBounds(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
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
