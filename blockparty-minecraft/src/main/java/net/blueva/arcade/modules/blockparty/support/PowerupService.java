package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.ui.Hologram;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import net.blueva.arcade.modules.blockparty.game.BlockPartyGame;
import net.blueva.arcade.modules.blockparty.state.BlockPartyState;
import net.blueva.arcade.modules.blockparty.state.PowerupInstance;
import net.blueva.arcade.modules.blockparty.state.PowerupType;
import net.blueva.arcade.modules.blockparty.state.RoundPhase;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PowerupService {

    private final BlockPartyModule module;
    private final BlockPartyGame game;
    private final Map<Integer, Map<Location, PowerupInstance>> activePowerups = new ConcurrentHashMap<>();

    public PowerupService(BlockPartyModule module, BlockPartyGame game) {
        this.module = module;
        this.game = game;
    }

    public void startPowerupSpawns(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        if (!module.getSettings().isPowerupsEnabled()) {
            return;
        }
        int arenaId = context.getArenaId();
        String taskId = "arena_" + arenaId + "_block_party_powerups";

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (game.isGameEnded(arenaId)) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            Map<Location, PowerupInstance> arenaPowerups = activePowerups.computeIfAbsent(arenaId, id -> new ConcurrentHashMap<>());
            if (arenaPowerups.size() >= module.getSettings().getMaxPowerups()) {
                return;
            }

            spawnPowerup(context);
        }, module.getSettings().getPowerupInterval(), module.getSettings().getPowerupInterval());
    }

    public void clearArenaPowerups(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        Map<Location, PowerupInstance> arenaPowerups = activePowerups.get(arenaId);
        if (arenaPowerups == null || arenaPowerups.isEmpty()) {
            return;
        }
        for (Location location : new ArrayList<>(arenaPowerups.keySet())) {
            removePowerupAt(context, arenaId, location, false);
        }
    }

    private void spawnPowerup(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        BlockPartyState state = game.getArenaState(arenaId);
        if (state == null || state.getCurrentBlocks() == null || state.getCurrentBlocks().isEmpty()) {
            return;
        }

        List<Location> positions = new ArrayList<>();
        for (Map.Entry<Location, Material> entry : state.getCurrentBlocks().entrySet()) {
            if (entry.getValue() != state.getTargetMaterial()) {
                Block supportBlock = entry.getKey().getBlock();
                if (supportBlock.getType() == Material.AIR) {
                    continue;
                }
                positions.add(supportBlock.getLocation());
            }
        }
        if (positions.isEmpty()) {
            return;
        }

        Location support = positions.get(ThreadLocalRandom.current().nextInt(positions.size()));
        World world = support.getWorld();
        if (world == null) {
            return;
        }

        Location powerupLocation = support.clone().add(0, 1, 0);
        Block block = powerupLocation.getBlock();
        org.bukkit.block.data.BlockData originalData = block.getBlockData();
        block.setType(module.getSettings().getPowerupItemMaterial());

        PowerupType type = PowerupType.random();
        Hologram hologram = spawnPowerupHologram(context, block.getLocation(), type);
        String particleTaskId = schedulePowerupParticles(context, arenaId, block.getLocation());

        PowerupInstance instance = new PowerupInstance(type, block.getLocation(), support, originalData,
                hologram, particleTaskId);
        activePowerups.computeIfAbsent(arenaId, id -> new ConcurrentHashMap<>()).put(block.getLocation(), instance);
    }

    public void handlePowerupPickup(Player player, Location location) {
        Integer arenaId = game.getGameContext(player) != null ? game.getGameContext(player).getArenaId() : null;
        if (arenaId == null) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = game.getGameContext(player);
        if (context == null) {
            return;
        }
        if (!context.isPlayerPlaying(player)) {
            return;
        }

        PowerupInstance instance = removePowerupAt(context, arenaId, location, true);
        if (instance == null) {
            return;
        }

        applyPowerupEffect(context, player, instance.getType());
        if (module.getStatsAPI() != null) {
            module.getStatsAPI().addModuleStat(player, module.getModuleInfo().getId(), "powerups_used", 1);
        }

        String message = module.getModuleConfig().getStringFrom("language.yml", "messages.powerups.collected")
                .replace("{player}", player.getName())
                .replace("{powerup}", instance.getType().getPlainName());

        for (Player arenaPlayer : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(arenaPlayer, message);
        }
    }

    private void applyPowerupEffect(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    Player player,
                                    PowerupType type) {
        int arenaId = context.getArenaId();
        BlockPartyState state = game.getArenaState(arenaId);
        if (state == null) {
            return;
        }

        switch (type) {
            case TELEPORT -> {
                Location target = findSafeTarget(state, context);
                if (target != null) {
                    player.teleport(target);
                }
            }
            case PATCH -> applyColorPatch(state, context);
            case SPEED -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                    module.getSettings().getSpeedDurationTicks(),
                    module.getSettings().getSpeedAmplifier(),
                    false,
                    false));
            case BONUS_TIME -> {
                if (state.getPhase() == RoundPhase.SEARCH) {
                    state.setPhaseTicksRemaining(state.getPhaseTicksRemaining() + module.getSettings().getBonusTimeTicks());
                    state.setPhaseTotalTicks(state.getPhaseTotalTicks() + module.getSettings().getBonusTimeTicks());
                }
            }
        }

        if (module.getSettings().isPowerupParticlesEnabled()) {
            player.getWorld().spawnParticle(module.getSettings().getPowerupParticleType(),
                    player.getLocation().add(0, 1, 0),
                    module.getSettings().getPowerupParticleCount(),
                    module.getSettings().getPowerupParticleSpread(),
                    module.getSettings().getPowerupParticleSpread(),
                    module.getSettings().getPowerupParticleSpread(),
                    module.getSettings().getPowerupParticleSpeed());
        }
    }

    public PowerupInstance removePowerupWithSupport(
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
            int arenaId,
            Location supportLocation,
            boolean restoreBlock
    ) {
        Map<Location, PowerupInstance> arenaMap = activePowerups.get(arenaId);
        if (arenaMap == null || supportLocation == null) {
            return null;
        }
        for (Map.Entry<Location, PowerupInstance> entry : new ArrayList<>(arenaMap.entrySet())) {
            PowerupInstance instance = entry.getValue();
            if (instance.getSupportLocation() != null && isSameBlock(instance.getSupportLocation(), supportLocation)) {
                return removePowerupAt(context, arenaId, entry.getKey(), restoreBlock);
            }
        }
        return null;
    }

    public PowerupInstance removePowerupAt(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                           int arenaId,
                                           Location location,
                                           boolean restoreBlock) {
        Map<Location, PowerupInstance> arenaMap = activePowerups.get(arenaId);
        if (arenaMap == null) {
            return null;
        }
        Location key = location.getBlock().getLocation();
        PowerupInstance instance = arenaMap.remove(key);
        if (instance == null) {
            return null;
        }

        if (instance.getParticleTaskId() != null) {
            context.getSchedulerAPI().cancelTask(instance.getParticleTaskId());
        }

        if (instance.getHologram() != null) {
            instance.getHologram().delete();
        }

        Block block = key.getBlock();
        if (restoreBlock && instance.getOriginalData() != null) {
            block.setBlockData(instance.getOriginalData());
        } else {
            block.setType(Material.AIR);
        }

        return instance;
    }

    public boolean isPowerupBlock(Block block) {
        if (block == null) {
            return false;
        }
        Location key = block.getLocation();
        Integer arenaId = getArenaForLocation(key);
        if (arenaId == null) {
            return false;
        }
        Map<Location, PowerupInstance> arenaMap = activePowerups.get(arenaId);
        return arenaMap != null && arenaMap.containsKey(key);
    }

    private Integer getArenaForLocation(Location location) {
        for (Map.Entry<Integer, Map<Location, PowerupInstance>> entry : activePowerups.entrySet()) {
            if (entry.getValue().containsKey(location.getBlock().getLocation())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isSameBlock(Location first, Location second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.getWorld() != null ? !first.getWorld().equals(second.getWorld()) : second.getWorld() != null) {
            return false;
        }
        return first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private Hologram spawnPowerupHologram(
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
            Location location,
            PowerupType type
    ) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        Location hologramLocation = location.clone().add(0.5, 1.35, 0.5);
        List<String> lines = List.of(
                "<gold>POWER UP</gold>",
                "<aqua>" + type.getDisplayName(module.getModuleConfig()) + "</aqua>"
        );
        return context.getHologramAPI().spawn(context.getArenaId(), hologramLocation, lines);
    }

    private String schedulePowerupParticles(
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
            int arenaId,
            Location location
    ) {
        String taskId = "arena_" + arenaId + "_block_party_powerup_particles_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (game.isGameEnded(arenaId)) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }
            World world = location.getWorld();
            if (world == null) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }
            if (location.getBlock().getType() != module.getSettings().getPowerupItemMaterial()) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }
            world.spawnParticle(module.getSettings().getPowerupParticleType(), location.clone().add(0.5, 0.6, 0.5),
                    module.getSettings().getPowerupParticleCount(),
                    module.getSettings().getPowerupParticleSpread(),
                    module.getSettings().getPowerupParticleSpread(),
                    module.getSettings().getPowerupParticleSpread(),
                    module.getSettings().getPowerupParticleSpeed());
        }, 0L, 10L);
        return taskId;
    }

    private Location findSafeTarget(BlockPartyState state,
                                    GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<Location> targets = new ArrayList<>();
        for (Map.Entry<Location, Material> entry : state.getCurrentBlocks().entrySet()) {
            if (entry.getValue() == state.getTargetMaterial()) {
                targets.add(entry.getKey().clone().add(0.5, 1, 0.5));
            }
        }
        if (targets.isEmpty()) {
            Location spawn = context.getArenaAPI().getRandomSpawn();
            return spawn != null ? spawn : null;
        }
        return targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
    }

    private void applyColorPatch(BlockPartyState state,
                                 GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        World world = state.getFloor() != null ? state.getFloor().min().getWorld() : null;
        if (world == null) {
            return;
        }
        int minY = Math.min(state.getFloor().min().getBlockY(), state.getFloor().max().getBlockY());
        List<Player> alive = context.getAlivePlayers();
        if (alive.isEmpty()) {
            return;
        }
        Player player = alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
        Location center = player.getLocation();
        for (int x = -module.getSettings().getPatchRadius(); x <= module.getSettings().getPatchRadius(); x++) {
            for (int z = -module.getSettings().getPatchRadius(); z <= module.getSettings().getPatchRadius(); z++) {
                Location loc = new Location(world, center.getBlockX() + x, minY, center.getBlockZ() + z);
                if (state.getFloor().contains(loc)) {
                    loc.getBlock().setType(state.getTargetMaterial());
                }
            }
        }
    }

    public void removeActivePowerups(int arenaId) {
        Map<Location, PowerupInstance> arenaMap = activePowerups.get(arenaId);
        if (arenaMap == null) {
            return;
        }
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                game.getGameContextFromArena(arenaId);
        for (Location location : new ArrayList<>(arenaMap.keySet())) {
            PowerupInstance instance = arenaMap.get(location);
            if (instance == null) {
                continue;
            }
            if (context != null) {
                removePowerupAt(context, arenaId, location, false);
            } else {
                if (instance.getHologram() != null) {
                    instance.getHologram().delete();
                }
                location.getBlock().setType(Material.AIR);
                arenaMap.remove(location);
            }
        }
        activePowerups.remove(arenaId);
    }

    public void clearAll() {
        activePowerups.clear();
    }

    public Map<Integer, Map<Location, PowerupInstance>> getActivePowerups() {
        return activePowerups;
    }
}
