package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FallingBlockService {

    private final BlockPartyModule module;
    private final ConcurrentHashMap<Integer, Set<UUID>> arenaFallingBlocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> fallingBlockArena = new ConcurrentHashMap<>();

    public FallingBlockService(BlockPartyModule module) {
        this.module = module;
    }

    public void spawnFallingShard(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  Location origin,
                                  org.bukkit.block.data.BlockData blockData) {
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        org.bukkit.entity.FallingBlock fallingBlock = world.spawnFallingBlock(origin.clone().add(0.5, 0.1, 0.5), blockData);
        fallingBlock.setDropItem(false);
        fallingBlock.setHurtEntities(false);
        double horizontalX = ThreadLocalRandom.current().nextDouble(-module.getSettings().getFallingHorizontalRandomness(),
                module.getSettings().getFallingHorizontalRandomness());
        double horizontalZ = ThreadLocalRandom.current().nextDouble(-module.getSettings().getFallingHorizontalRandomness(),
                module.getSettings().getFallingHorizontalRandomness());
        fallingBlock.setVelocity(new Vector(horizontalX, -Math.abs(module.getSettings().getFallingDownwardVelocity()), horizontalZ));

        int arenaId = context.getArenaId();
        trackFallingBlock(arenaId, fallingBlock.getUniqueId());

        String taskId = "arena_" + arenaId + "_block_party_falling_" + fallingBlock.getUniqueId();
        context.getSchedulerAPI().runLater(taskId, () -> removeTrackedFallingBlock(fallingBlock), 40L);
    }

    public boolean isTrackedFallingBlock(UUID uuid) {
        return fallingBlockArena.containsKey(uuid);
    }

    public void handleFallingBlockLand(org.bukkit.entity.FallingBlock fallingBlock) {
        removeTrackedFallingBlock(fallingBlock);
    }

    public void trackFallingBlock(int arenaId, UUID uuid) {
        arenaFallingBlocks.computeIfAbsent(arenaId, id -> ConcurrentHashMap.newKeySet()).add(uuid);
        fallingBlockArena.put(uuid, arenaId);
    }

    private void removeTrackedFallingBlock(org.bukkit.entity.FallingBlock fallingBlock) {
        if (fallingBlock == null) {
            return;
        }
        UUID uuid = fallingBlock.getUniqueId();
        fallingBlock.remove();
        untrackFallingBlock(uuid);
    }

    private void untrackFallingBlock(UUID uuid) {
        Integer arenaId = fallingBlockArena.remove(uuid);
        if (arenaId == null) {
            return;
        }

        Set<UUID> arenaSet = arenaFallingBlocks.get(arenaId);
        if (arenaSet != null) {
            arenaSet.remove(uuid);
            if (arenaSet.isEmpty()) {
                arenaFallingBlocks.remove(arenaId);
            }
        }
    }

    public void cleanupFallingBlocks(int arenaId) {
        Set<UUID> fallingBlocks = arenaFallingBlocks.remove(arenaId);
        if (fallingBlocks == null) {
            return;
        }

        for (UUID uuid : fallingBlocks) {
            fallingBlockArena.remove(uuid);
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    public void cleanupAllFallingBlocks() {
        for (Integer arenaId : new HashSet<>(arenaFallingBlocks.keySet())) {
            cleanupFallingBlocks(arenaId);
        }
        fallingBlockArena.clear();
        arenaFallingBlocks.clear();
    }
}
