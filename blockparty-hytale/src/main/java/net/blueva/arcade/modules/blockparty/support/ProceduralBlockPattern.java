package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.world.BlockPattern;
import net.blueva.arcade.api.world.BlocksAPI;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Location;

import java.util.List;
import java.util.Map;

public class ProceduralBlockPattern extends SimpleBlockPattern {
    private final String template;
    private final long seed;
    private final List<String> targetMaterials;

    public ProceduralBlockPattern(BlocksAPI<Location, String, Holder> blocksAPI,
                                  Map<Location, String> blocks, String template, long seed,
                                  List<String> targetMaterials) {
        super(blocksAPI, blocks);
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

    public List<String> getTargetMaterials() {
        return targetMaterials;
    }
}
