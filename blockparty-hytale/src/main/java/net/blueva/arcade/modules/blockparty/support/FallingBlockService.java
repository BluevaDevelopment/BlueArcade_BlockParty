package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;

public class FallingBlockService {

    public FallingBlockService(BlockPartyModule module) {
        // Falling blocks are not supported in the Hytale runtime yet.
    }

    public void spawnFallingShard(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                  Location origin,
                                  String material) {
        // Falling blocks are not supported in the Hytale runtime yet.
    }

    public void cleanupFallingBlocks(int arenaId) {
        // Falling blocks are not supported in the Hytale runtime yet.
    }

    public void cleanupAllFallingBlocks() {
        // Falling blocks are not supported in the Hytale runtime yet.
    }
}
