package net.blueva.arcade.modules.blockparty.support;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class ProceduralBlockPattern extends SimpleBlockPattern {
    private final String template;
    private final long seed;
    private final List<Material> targetMaterials;

    public ProceduralBlockPattern(Map<Location, Material> blocks, String template, long seed,
                                  List<Material> targetMaterials) {
        super(blocks);
        this.template = template;
        this.seed = seed;
        this.targetMaterials = List.copyOf(targetMaterials);
    }

    public String getTemplate() {
        return template;
    }

    public long getSeed() {
        return seed;
    }

    public List<Material> getTargetMaterials() {
        return targetMaterials;
    }
}
