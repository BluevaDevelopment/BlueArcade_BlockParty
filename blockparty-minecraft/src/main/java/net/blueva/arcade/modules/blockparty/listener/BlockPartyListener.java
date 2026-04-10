package net.blueva.arcade.modules.blockparty.listener;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

public class BlockPartyListener implements Listener {

    private final BlockPartyModule module;

    public BlockPartyListener(BlockPartyModule module) {
        this.module = module;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                module.getGame().getGameContext(player);

        if (context == null) {
            return;
        }

        if (!context.isPlayerPlaying(player)) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (context.getPhase() == GamePhase.COUNTDOWN && module.freezePlayersOnCountdown()) {
            player.teleport(event.getFrom());
            return;
        }

        if (context.getPhase() != GamePhase.PLAYING) {
            if (!context.isInsideBounds(to)) {
                Location spawn = context.getArenaAPI().getRandomSpawn();
                if (spawn != null) {
                    player.teleport(spawn);
                }
            }
            return;
        }

        if (module.getGame().shouldEliminate(player, to)) {
            module.getGame().handlePlayerElimination(player);
        }
    }

    @EventHandler
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) {
            return;
        }

        if (!module.getGame().isTrackedFallingBlock(fallingBlock.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        module.getGame().handleFallingBlockLand(fallingBlock);
    }

    @EventHandler
    public void onPowerupInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !module.getGame().isPowerupBlock(clickedBlock)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                module.getGame().getGameContext(player);
        if (context == null || context.getPhase() != GamePhase.PLAYING) {
            return;
        }

        if (context.getSpectators().contains(player) || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        event.setCancelled(true);
        module.getGame().handlePowerupPickup(player, clickedBlock.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                module.getGame().getGameContext(victim);
        if (context == null) {
            return;
        }

        event.setCancelled(true);
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                module.getGame().getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                module.getGame().getGameContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        event.setCancelled(true);
    }
}
