package net.blueva.arcade.modules.blockparty.listener;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Location;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.blockparty.game.BlockPartyGame;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Safety-net movement listener for Block Party (Hytale).
 *
 * This runs on entity ticks and ensures out-of-bounds elimination is still applied
 * during PLAYING even if scheduler-based movement tasks are delayed or cancelled.
 */
public final class BlockPartyMovementListener extends EntityTickingSystem<EntityStore> {

    private final BlockPartyGame game;

    public BlockPartyMovementListener(BlockPartyGame game) {
        this.game = game;
    }

    @Override
    public void tick(float deltaTime,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (ref == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (player == null || playerRef == null || transform == null) {
            return;
        }

        GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context =
                game.getGameContext(player);
        if (context == null) {
            return;
        }

        if (context.getPhase() != GamePhase.PLAYING || !context.isPlayerPlaying(player)) {
            return;
        }

        Vector3d position = transform.getPosition();
        String worldName = ((EntityStore) store.getExternalData()).getWorld().getName();
        Location location = new Location(worldName, position.x, position.y, position.z, 0.0f, 0.0f, 0.0f);

        if (game.shouldEliminate(context, location)) {
            game.handlePlayerElimination(player);
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
