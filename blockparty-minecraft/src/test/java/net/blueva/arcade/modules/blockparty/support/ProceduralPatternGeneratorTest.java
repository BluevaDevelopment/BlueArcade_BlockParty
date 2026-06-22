package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.modules.blockparty.state.FloorBounds;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ProceduralPatternGeneratorTest {
    private final ProceduralPatternGenerator generator = new ProceduralPatternGenerator();
    private final World world = worldStub();
    private final FloorBounds floor = new FloorBounds(new Location(world, 10, 64, 20),
            new Location(world, 41, 64, 51));

    @Test
    public void everyTemplateFillsTheRuntimeFloor() {
        for (String template : ProceduralPatternGenerator.TEMPLATE_TYPES) {
            ProceduralBlockPattern pattern = generator.generate(floor, world, template, 12345L, 4, 8, 16);

            assertEquals(template, 32 * 32, pattern.getBlocks().size());
            assertFalse(template, pattern.getTargetMaterials().isEmpty());
            assertTrue(template, pattern.getBlocks().values().stream().noneMatch(material -> material == Material.AIR));
            assertTrue(template, pattern.getBlocks().keySet().stream().allMatch(location -> location.getWorld() == world));
        }
    }

    @Test
    public void seedMovesWavesAndRainbowGeometry() {
        for (String template : new String[]{"waves", "rainbow"}) {
            Map<Location, Material> first = generator.generate(floor, world, template, 11L, 4, 8, 16).getBlocks();
            Map<Location, Material> second = generator.generate(floor, world, template, 12L, 4, 8, 16).getBlocks();

            assertNotEquals(template, canonicalLayout(first), canonicalLayout(second));
        }
    }

    @Test
    public void creeperKeepsAContrastingBlackFace() {
        ProceduralBlockPattern pattern = generator.generate(floor, world, "creeper", 9876L, 4, 8, 16);

        assertTrue(pattern.getMaterials().contains(Material.BLACK_CONCRETE));
        assertTrue(pattern.getMaterials().stream().anyMatch(material -> material != Material.BLACK_CONCRETE));
        assertEquals(2, pattern.getMaterials().size());
    }

    @Test
    public void generatedLocationsUseTheDynamicArenaWorld() {
        ProceduralBlockPattern pattern = generator.generate(floor, world, "checker", 42L, 4, 6, 16);
        assertSame(world, pattern.getBlocks().keySet().iterator().next().getWorld());
    }

    private static World worldStub() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "dynamic_block_party_test";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "WorldStub";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static List<Integer> canonicalLayout(Map<Location, Material> blocks) {
        List<Map.Entry<Location, Material>> entries = new ArrayList<>(blocks.entrySet());
        entries.sort(Comparator
                .comparingInt((Map.Entry<Location, Material> entry) -> entry.getKey().getBlockX())
                .thenComparingInt(entry -> entry.getKey().getBlockZ()));
        Map<Material, Integer> ids = new EnumMap<>(Material.class);
        List<Integer> layout = new ArrayList<>(entries.size());
        for (Map.Entry<Location, Material> entry : entries) {
            layout.add(ids.computeIfAbsent(entry.getValue(), ignored -> ids.size()));
        }
        return layout;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
