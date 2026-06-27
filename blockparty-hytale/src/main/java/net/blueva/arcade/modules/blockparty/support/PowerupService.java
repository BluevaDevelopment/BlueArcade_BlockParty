package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.ui.Hologram;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import net.blueva.arcade.modules.blockparty.game.BlockPartyGame;
import net.blueva.arcade.modules.blockparty.state.BlockPartyState;
import net.blueva.arcade.modules.blockparty.state.PowerupInstance;
import net.blueva.arcade.modules.blockparty.state.PowerupType;
import net.blueva.arcade.modules.blockparty.state.RoundPhase;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;

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

    public void startPowerupSpawns(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
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

    public void clearArenaPowerups(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        int arenaId = context.getArenaId();
        Map<Location, PowerupInstance> arenaPowerups = activePowerups.get(arenaId);
        if (arenaPowerups == null || arenaPowerups.isEmpty()) {
            return;
        }
        for (Location location : new ArrayList<>(arenaPowerups.keySet())) {
            removePowerupAt(context, arenaId, location, false);
        }
    }

    private void spawnPowerup(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        int arenaId = context.getArenaId();
        BlockPartyState state = game.getArenaState(arenaId);
        if (state == null || state.getCurrentBlocks() == null || state.getCurrentBlocks().isEmpty()) {
            return;
        }

        List<Location> positions = new ArrayList<>();
        for (Map.Entry<Location, String> entry : state.getCurrentBlocks().entrySet()) {
            if (!BlockPartyUtils.isSameMaterial(entry.getValue(), state.getTargetMaterial())) {
                positions.add(entry.getKey());
            }
        }
        if (positions.isEmpty()) {
            return;
        }

        Location support = positions.get(ThreadLocalRandom.current().nextInt(positions.size()));
        Location powerupLocation = BlockPartyUtils.offsetBlockLocation(support, 0, 1, 0);

        context.getBlocksAPI().setBlock(powerupLocation, module.getSettings().getPowerupItemMaterial());

        PowerupType type = PowerupType.random();
        Hologram<Location> hologram = spawnPowerupHologram(context, powerupLocation, type);
        String particleTaskId = schedulePowerupParticles(context, arenaId, powerupLocation);

        PowerupInstance instance = new PowerupInstance(type, powerupLocation, support, BlockPartyUtils.getAirBlockId(),
                hologram, particleTaskId);
        activePowerups.computeIfAbsent(arenaId, id -> new ConcurrentHashMap<>()).put(BlockPartyUtils.toBlockLocation(powerupLocation), instance);
    }

    public void handlePowerupPickup(Player player, Location location) {
        Integer arenaId = game.getGameContext(player) != null ? game.getGameContext(player).getArenaId() : null;
        if (arenaId == null || location == null) {
            return;
        }

        GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context = game.getGameContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
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
                .replace("{player}", player.getPlayerRef().getUsername())
                .replace("{powerup}", instance.getType().getPlainName());

        for (Player arenaPlayer : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(arenaPlayer, message);
        }
    }

    private void applyPowerupEffect(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
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
                    teleportPlayer(context, player, target);
                }
            }
            case PATCH -> applyColorPatch(state, context);
            case SPEED -> {
                // Status effects are not yet available in the Hytale runtime.
            }
            case BONUS_TIME -> {
                if (state.getPhase() == RoundPhase.SEARCH) {
                    state.setPhaseTicksRemaining(state.getPhaseTicksRemaining() + module.getSettings().getBonusTimeTicks());
                    state.setPhaseTotalTicks(state.getPhaseTotalTicks() + module.getSettings().getBonusTimeTicks());
                }
            }
        }
    }

    public PowerupInstance removePowerupWithSupport(
            GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
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
            if (instance.getSupportLocation() != null && BlockPartyUtils.isSameBlock(instance.getSupportLocation(), supportLocation)) {
                return removePowerupAt(context, arenaId, entry.getKey(), restoreBlock);
            }
        }
        return null;
    }

    public PowerupInstance removePowerupAt(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                           int arenaId,
                                           Location location,
                                           boolean restoreBlock) {
        Map<Location, PowerupInstance> arenaMap = activePowerups.get(arenaId);
        if (arenaMap == null || location == null) {
            return null;
        }
        Location key = BlockPartyUtils.toBlockLocation(location);
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

        if (restoreBlock && instance.getOriginalBlockId() != null) {
            context.getBlocksAPI().setBlock(key, instance.getOriginalBlockId());
        } else {
            context.getBlocksAPI().setBlock(key, BlockPartyUtils.getAirBlockId());
        }

        return instance;
    }

    private Hologram<Location> spawnPowerupHologram(
            GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
            Location location,
            PowerupType type
    ) {
        Location hologramLocation = BlockPartyUtils.offsetLocation(location, 0.5, 1.35, 0.5);
        List<String> lines = List.of(
                "<gold>POWER UP</gold>",
                "<aqua>" + type.getDisplayName(module.getModuleConfig()) + "</aqua>"
        );
        return context.getHologramAPI().spawn(context.getArenaId(), hologramLocation, lines);
    }

    private String schedulePowerupParticles(
            GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
            int arenaId,
            Location location
    ) {
        // Hytale runtime does not yet expose particle effects.
        return null;
    }

    private Location findSafeTarget(BlockPartyState state,
                                    GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        List<Location> targets = new ArrayList<>();
        for (Map.Entry<Location, String> entry : state.getCurrentBlocks().entrySet()) {
            if (BlockPartyUtils.isSameMaterial(entry.getValue(), state.getTargetMaterial())) {
                targets.add(BlockPartyUtils.offsetLocation(entry.getKey(), 0.5, 1, 0.5));
            }
        }
        if (targets.isEmpty()) {
            return context.getArenaAPI().getRandomSpawn();
        }
        return targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
    }

    private void applyColorPatch(BlockPartyState state,
                                 GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        if (state.getFloor() == null) {
            return;
        }
        int minY = (int) Math.floor(Math.min(state.getFloor().min().getPosition().y, state.getFloor().max().getPosition().y));
        List<Player> alive = context.getAlivePlayers();
        if (alive.isEmpty()) {
            return;
        }
        Player player = alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
        context.getSchedulerAPI().runAtEntity(player, () -> {
            Location center = resolvePlayerLocation(player);
            if (center == null) {
                center = context.getArenaAPI().getRandomSpawn();
            }
            if (center == null) {
                return;
            }
            Location base = BlockPartyUtils.toBlockLocation(center);
            for (int x = -module.getSettings().getPatchRadius(); x <= module.getSettings().getPatchRadius(); x++) {
                for (int z = -module.getSettings().getPatchRadius(); z <= module.getSettings().getPatchRadius(); z++) {
                    Location loc = new Location(base.getWorld(), base.getPosition().x + x, minY, base.getPosition().z + z);
                    if (state.getFloor().contains(loc)) {
                        context.getBlocksAPI().setBlock(loc, state.getTargetMaterial());
                        state.getCurrentBlocks().put(BlockPartyUtils.toBlockLocation(loc), state.getTargetMaterial());
                    }
                }
            }
        });
    }

    private Location resolvePlayerLocation(Player player) {
        if (player == null || player.getWorld() == null || player.getReference() == null) {
            return null;
        }
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return null;
        }
        Vector3d position = transform.getPosition();
        Rotation3f rotation = transform.getRotation();
        return new Location(player.getWorld().getName(), position.x, position.y, position.z, rotation.pitch(), rotation.yaw(), rotation.roll());
    }

    private void teleportPlayer(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                Player player,
                                Location location) {
        if (player == null || location == null) {
            return;
        }
        context.getSchedulerAPI().runAtEntity(player, () -> {
            Vector3d position = location.getPosition();
            Rotation3f rotation = location.getRotation();
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            transform.setPosition(position);
            if (rotation != null) {
                transform.setRotation(rotation);
            }
        });
    }

    public void removeActivePowerups(int arenaId) {
        Map<Location, PowerupInstance> arenaMap = activePowerups.get(arenaId);
        if (arenaMap == null) {
            return;
        }
        GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context =
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
