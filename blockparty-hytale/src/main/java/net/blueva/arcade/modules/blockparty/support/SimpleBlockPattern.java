package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.world.BlockPattern;
import net.blueva.arcade.api.world.BlocksAPI;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SimpleBlockPattern implements BlockPattern<Location, String> {
    private final Map<Location, String> blocks;
    private final BlocksAPI<Location, String, BlockState> blocksAPI;

    public SimpleBlockPattern(BlocksAPI<Location, String, BlockState> blocksAPI, Map<Location, String> blocks) {
        this.blocksAPI = blocksAPI;
        this.blocks = blocks == null ? new HashMap<>() : new HashMap<>(blocks);
    }

    @Override
    public Map<Location, String> getBlocks() {
        return new HashMap<>(blocks);
    }

    @Override
    public List<String> getMaterials() {
        return new ArrayList<>(blocks.values().stream().distinct().toList());
    }

    @Override
    public String getRandomMaterial() {
        List<String> materials = getMaterials();
        if (materials.isEmpty()) {
            return BlockPartyUtils.getAirBlockId();
        }
        return materials.get(new Random().nextInt(materials.size()));
    }

    @Override
    public void apply() {
        if (blocksAPI == null) {
            return;
        }
        for (Map.Entry<Location, String> entry : blocks.entrySet()) {
            Location location = entry.getKey();
            String material = entry.getValue();
            if (location != null && material != null) {
                blocksAPI.setBlock(location, material);
            }
        }
    }
}
