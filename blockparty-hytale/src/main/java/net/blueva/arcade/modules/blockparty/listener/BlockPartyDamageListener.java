package net.blueva.arcade.modules.blockparty.listener;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.blockparty.game.BlockPartyGame;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Damage listener for Block Party minigame (Hytale edition).
 * Cancels all player-vs-player damage inside Block Party arenas.
 */
public class BlockPartyDamageListener extends EntityEventSystem<EntityStore, Damage> {

    private final BlockPartyGame game;

    public BlockPartyDamageListener(BlockPartyGame game) {
        super(Damage.class);
        this.game = game;
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
        if (victimRef == null) {
            return;
        }

        PlayerRef victimPlayerRef = store.getComponent(victimRef, PlayerRef.getComponentType());
        Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
        if (victimPlayerRef == null || victimPlayer == null) {
            return;
        }

        GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context =
                game.getGameContext(victimPlayer);
        if (context == null) {
            return;
        }

        Player attacker = resolveAttacker(damage, store);
        if (attacker == null) {
            return;
        }

        damage.setCancelled(true);
    }

    @Nullable
    private Player resolveAttacker(@Nonnull Damage damage, @Nonnull Store<EntityStore> store) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }

        Store<EntityStore> attackerStore = attackerRef.getStore();
        if (attackerStore == null) {
            return null;
        }

        return attackerStore.getComponent(attackerRef, Player.getComponentType());
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
