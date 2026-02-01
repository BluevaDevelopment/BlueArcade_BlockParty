package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.world.BlockPattern;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SimpleBlockPattern implements BlockPattern {
    private final Map<Location, Material> blocks;

    public SimpleBlockPattern(Map<Location, Material> blocks) {
        this.blocks = blocks;
    }

    @Override
    public Map<Location, Material> getBlocks() {
        return new HashMap<>(blocks);
    }

    @Override
    public List<Material> getMaterials() {
        return new ArrayList<>(blocks.values().stream().distinct().toList());
    }

    @Override
    public Material getRandomMaterial() {
        List<Material> materials = getMaterials();
        if (materials.isEmpty()) {
            return Material.AIR;
        }
        return materials.get(new Random().nextInt(materials.size()));
    }

    @Override
    public void apply() {
        for (Map.Entry<Location, Material> entry : blocks.entrySet()) {
            Location location = entry.getKey();
            Material material = entry.getValue();
            if (location.getWorld() != null && material != null) {
                location.getBlock().setType(material);
            }
        }
    }
}
