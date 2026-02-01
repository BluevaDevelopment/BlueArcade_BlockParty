package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.ui.ItemAPI;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
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
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    int slot = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;

                    ItemStack item = new ItemStack(material, amount);

                    if (slot >= 0 && slot < 36) {
                        player.getInventory().setItem(slot, item);
                    } else {
                        player.getInventory().addItem(item);
                    }
                }
            } catch (Exception e) {
                // Ignore malformed entries
            }
        }
    }

    public void applyStartingEffects(Player player) {
        List<String> startingEffects = module.getModuleConfig().getStringList("effects.starting_effects");

        if (startingEffects == null || startingEffects.isEmpty()) {
            return;
        }

        for (String effectString : startingEffects) {
            applyEffectString(player, effectString);
        }
    }

    public void handleRespawnEffects(Player player) {
        List<String> respawnEffects = module.getModuleConfig().getStringList("effects.respawn_effects");

        if (respawnEffects == null || respawnEffects.isEmpty()) {
            return;
        }

        for (String effectString : respawnEffects) {
            applyEffectString(player, effectString);
        }
    }

    public void giveTargetItem(Player player, Material target) {
        if (!module.getSettings().isGiveTargetItem() || target == null || target == Material.AIR) {
            return;
        }

        ItemStack item = new ItemStack(target);
        String name = module.getModuleConfig().getStringFrom("language.yml", "items.target_name");
        ItemAPI<Player, ItemStack, Material> itemAPI = module.getItemAPI();
        if (itemAPI != null && name != null) {
            item = itemAPI.decorate(item, name, Collections.emptyList());
        }

        if (module.getSettings().getTargetItemSlot() >= 0 && module.getSettings().getTargetItemSlot() < player.getInventory().getSize()) {
            player.getInventory().setItem(module.getSettings().getTargetItemSlot(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }

    public void clearPlayerInventories(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getAlivePlayers()) {
            player.getInventory().clear();
        }
    }

    private void applyEffectString(Player player, String effectString) {
        try {
            String[] parts = effectString.split(":");
            if (parts.length >= 3) {
                PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);

                if (effectType != null) {
                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, false));
                }
            }
        } catch (Exception e) {
            // Ignore malformed entries
        }
    }
}
