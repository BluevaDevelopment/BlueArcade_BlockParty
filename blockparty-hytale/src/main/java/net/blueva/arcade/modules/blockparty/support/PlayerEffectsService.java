package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.List;

public class PlayerEffectsService {

    private final BlockPartyModule module;

    public PlayerEffectsService(BlockPartyModule module) {
        this.module = module;
    }

    public void giveStartingItems(Player player) {
        List<String> startingItems = module.getModuleConfig().getStringList("items.starting_items");

        if (startingItems == null || startingItems.isEmpty()) {
            return;
        }

        for (String itemString : startingItems) {
            try {
                String[] parts = itemString.split(":");
                if (parts.length >= 2) {
                    String itemId = parts[0];
                    int amount = Integer.parseInt(parts[1]);
                    int slot = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;

                    ItemStack item = new ItemStack(itemId, amount);
                    addItem(player, item, slot);
                }
            } catch (Exception ignored) {
                // Ignore malformed entries.
            }
        }
    }

    public void applyStartingEffects(Player player) {
        List<String> startingEffects = module.getModuleConfig().getStringList("effects.starting_effects");

        if (startingEffects == null || startingEffects.isEmpty()) {
            return;
        }
        // Status effects are not yet available in the Hytale runtime.
    }

    public void handleRespawnEffects(Player player) {
        List<String> respawnEffects = module.getModuleConfig().getStringList("effects.respawn_effects");

        if (respawnEffects == null || respawnEffects.isEmpty()) {
            return;
        }
        // Status effects are not yet available in the Hytale runtime.
    }

    public void giveTargetItem(Player player, String target) {
        if (!module.getSettings().isGiveTargetItem() || target == null || BlockPartyUtils.isAir(target)) {
            return;
        }

        try {
            ItemStack item = new ItemStack(target, 1);
            int slot = module.getSettings().getTargetItemSlot();
            addItem(player, item, slot);
        } catch (IllegalArgumentException e) {
            // Cannot create ItemStack for this block type (e.g., Empty blocks)
        }
    }

    public void clearPlayerInventories(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context) {
        for (Player player : context.getAlivePlayers()) {
            clearInventory(player);
        }
    }

    public void clearInventory(Player player) {
        if (player == null || player.getInventory() == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        clearContainer(inventory.getArmor());
        clearContainer(inventory.getHotbar());
        clearContainer(inventory.getStorage());
        clearContainer(inventory.getUtility());
        clearContainer(inventory.getTools());
        clearContainer(inventory.getBackpack());
    }

    private void addItem(Player player, ItemStack item, int slot) {
        if (player == null || item == null || player.getInventory() == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (slot >= 0) {
            ItemContainer hotbar = inventory.getHotbar();
            if (slot < 9 && hotbar != null) {
                hotbar.addItemStackToSlot((short) slot, item);
                return;
            }

            ItemContainer storage = inventory.getStorage();
            if (storage != null) {
                short storageSlot = (short) Math.max(0, slot - 9);
                if (storageSlot < storage.getCapacity()) {
                    storage.addItemStackToSlot(storageSlot, item);
                } else {
                    storage.addItemStack(item);
                }
            }
            return;
        }

        ItemContainer storage = inventory.getStorage();
        if (storage != null) {
            storage.addItemStack(item);
        }
    }

    private void clearContainer(ItemContainer container) {
        if (container != null) {
            container.clear();
        }
    }
}
