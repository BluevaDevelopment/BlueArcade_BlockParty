package net.blueva.arcade.modules.blockparty.game;

import net.blueva.arcade.api.arena.FloorRegion;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import net.blueva.arcade.modules.blockparty.state.BlockPartyState;
import net.blueva.arcade.modules.blockparty.state.FloorBounds;
import net.blueva.arcade.modules.blockparty.state.RoundPhase;
import net.blueva.arcade.modules.blockparty.support.BlockPartyUtils;
import net.blueva.arcade.modules.blockparty.support.FallingBlockService;
import net.blueva.arcade.modules.blockparty.support.PatternService;
import net.blueva.arcade.modules.blockparty.support.PlayerEffectsService;
import net.blueva.arcade.modules.blockparty.support.PowerupService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class BlockPartyGame {

    private final BlockPartyModule module;
    private final PatternService patternService;
    private final PowerupService powerupService;
    private final PlayerEffectsService playerEffectsService;
    private final FallingBlockService fallingBlockService;

    private final Map<Integer, GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity>> activeGames =
            new ConcurrentHashMap<>();
    private final Map<Player, Integer> playerArenas = new ConcurrentHashMap<>();
    private final Map<Integer, FloorBounds> arenaFloors = new ConcurrentHashMap<>();
    private final Map<Integer, FloorBounds> arenaBounds = new ConcurrentHashMap<>();
    private final Map<Integer, BlockPartyState> arenaStates = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> gameEnded = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> arenaWinners = new ConcurrentHashMap<>();
    private final Map<Integer, Set<UUID>> eliminatedPlayers = new ConcurrentHashMap<>();

    public BlockPartyGame(BlockPartyModule module) {
        this.module = module;
        this.patternService = new PatternService(module);
        this.powerupService = new PowerupService(module, this);
        this.playerEffectsService = new PlayerEffectsService(module);
        this.fallingBlockService = new FallingBlockService(module);
    }

    public void onStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        activeGames.put(arenaId, context);
        gameEnded.put(arenaId, false);
        arenaWinners.remove(arenaId);
        eliminatedPlayers.put(arenaId, ConcurrentHashMap.newKeySet());

        FloorBounds floor = cacheFloorBounds(context);
        if (floor != null) {
            arenaFloors.put(arenaId, floor);
        }
        FloorBounds bounds = cacheArenaBounds(context, floor);
        if (bounds != null) {
            arenaBounds.put(arenaId, bounds);
        }

        BlockPartyState state = createState(context, floor);
        arenaStates.put(arenaId, state);

        for (Player player : context.getPlayers()) {
            playerArenas.put(player, arenaId);
        }

        sendDescription(context);
    }

    public void onCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                int secondsLeft) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            context.getSoundsAPI().play(player, module.getCoreConfig().getSound("sounds.starting_game.countdown"));

            String title = module.getCoreConfig().getLanguage("titles.starting_game.title")
                    .replace("{game_display_name}", module.getModuleInfo().getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            String subtitle = module.getCoreConfig().getLanguage("titles.starting_game.subtitle")
                    .replace("{game_display_name}", module.getModuleInfo().getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 5);
        }
    }

    public void onCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            String title = module.getCoreConfig().getLanguage("titles.game_started.title")
                    .replace("{game_display_name}", module.getModuleInfo().getName());

            String subtitle = module.getCoreConfig().getLanguage("titles.game_started.subtitle")
                    .replace("{game_display_name}", module.getModuleInfo().getName());

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 20);
            context.getSoundsAPI().play(player, module.getCoreConfig().getSound("sounds.starting_game.start"));
        }
    }

    public void onGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        BlockPartyState state = arenaStates.get(arenaId);
        if (state == null) {
            state = createState(context, arenaFloors.get(arenaId));
            arenaStates.put(arenaId, state);
        }

        resetFloor(context);
        sendStartTitle(context);
        startRegionParticles(context);
        powerupService.startPowerupSpawns(context);
        startGameLoop(context, state);
        scheduleMaxGameTime(context);

        for (Player player : context.getPlayers()) {
            player.setGameMode(GameMode.valueOf(GameMode.class, module.getModuleConfig().getStringFrom("settings.yml", "gameplay.game_mode", "SURVIVAL").toUpperCase()));
            playerEffectsService.giveStartingItems(player);
            playerEffectsService.applyStartingEffects(player);
            context.getScoreboardAPI().showScoreboard(player, getScoreboardPath());
        }
    }

    public void onEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                      GameResult<Player> result) {
        int arenaId = context.getArenaId();

        context.getSchedulerAPI().cancelArenaTasks(arenaId);
        resetFloor(context);
        fallingBlockService.cleanupFallingBlocks(arenaId);
        powerupService.removeActivePowerups(arenaId);
        context.getHologramAPI().deleteArenaHolograms(arenaId);

        activeGames.remove(arenaId);
        gameEnded.remove(arenaId);
        arenaWinners.remove(arenaId);
        arenaFloors.remove(arenaId);
        arenaStates.remove(arenaId);
        eliminatedPlayers.remove(arenaId);
        for (Player player : context.getPlayers()) {
            playerArenas.remove(player);
        }

        if (module.getStatsAPI() != null) {
            for (Player player : context.getPlayers()) {
                module.getStatsAPI().addModuleStat(player, module.getModuleInfo().getId(), "games_played", 1);
            }
        }

        for (Player player : context.getPlayers()) {
            context.getSoundsAPI().stopMusic(player);
        }
    }

    public void onDisable() {
        if (!activeGames.isEmpty()) {
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> anyContext =
                    activeGames.values().iterator().next();
            anyContext.getSchedulerAPI().cancelModuleTasks(module.getModuleInfo().getId());
        }

        activeGames.clear();
        playerArenas.clear();
        gameEnded.clear();
        arenaFloors.clear();
        arenaStates.clear();
        eliminatedPlayers.clear();
        powerupService.clearAll();
        fallingBlockService.cleanupAllFallingBlocks();
    }

    public Map<String, String> getCustomPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        if (context != null) {
            placeholders.put("alive", String.valueOf(context.getAlivePlayers().size()));
            placeholders.put("spectators", String.valueOf(context.getSpectators().size()));
            BlockPartyState state = arenaStates.get(context.getArenaId());
            if (state != null) {
                placeholders.put("bp_round", String.valueOf(state.getRound()));
                placeholders.put("bp_time", state.getPhase() == RoundPhase.SEARCH
                        ? BlockPartyUtils.formatSeconds(state.getDisplayedTime())
                        : "-");
            } else {
                placeholders.put("bp_round", "-");
                placeholders.put("bp_time", "-");
            }
        }

        return placeholders;
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContext(Player player) {
        Integer arenaId = playerArenas.get(player);
        if (arenaId == null) {
            return null;
        }
        return activeGames.get(arenaId);
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContextFromArena(int arenaId) {
        return activeGames.get(arenaId);
    }

    public void handleRespawnEffects(Player player) {
        playerEffectsService.handleRespawnEffects(player);
    }

    public boolean shouldEliminate(Player player, Location to) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        if (context == null) {
            return false;
        }

        if (!context.isInsideBounds(to)) {
            return true;
        }

        FloorBounds floor = arenaFloors.get(context.getArenaId());
        if (floor == null) {
            return false;
        }

        double minY = Math.min(floor.min().getY(), floor.max().getY());
        return to.getY() < minY - module.getSettings().getEliminationMargin();
    }

    public void handlePlayerElimination(Player player) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        if (context == null) {
            return;
        }

        // Don't eliminate spectators
        if (context.getSpectators().contains(player)) {
            return;
        }

        int arenaId = context.getArenaId();
        Set<UUID> arenaEliminations = eliminatedPlayers.computeIfAbsent(arenaId, id -> ConcurrentHashMap.newKeySet());
        if (!arenaEliminations.add(player.getUniqueId())) {
            return;
        }

        broadcastDeathMessage(context, player);
        context.eliminatePlayer(player, module.getModuleConfig().getStringFrom("language.yml", "messages.eliminated"));
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        context.getSoundsAPI().play(player, module.getCoreConfig().getSound("sounds.in_game.respawn"));

        if (context.getAlivePlayers().size() <= 1) {
            endGameOnce(context);
        }
    }

    public void handlePowerupPickup(Player player, Location location) {
        powerupService.handlePowerupPickup(player, location);
    }

    public boolean isPowerupBlock(org.bukkit.block.Block block) {
        return powerupService.isPowerupBlock(block);
    }

    public boolean isTrackedFallingBlock(UUID uuid) {
        return fallingBlockService.isTrackedFallingBlock(uuid);
    }

    public void handleFallingBlockLand(org.bukkit.entity.FallingBlock fallingBlock) {
        fallingBlockService.handleFallingBlockLand(fallingBlock);
    }

    public FloorBounds getArenaFloor(int arenaId) {
        return arenaFloors.get(arenaId);
    }

    public BlockPartyState getArenaState(int arenaId) {
        return arenaStates.get(arenaId);
    }

    public boolean isGameEnded(int arenaId) {
        return gameEnded.getOrDefault(arenaId, false);
    }

    public void removePlayerArena(Player player) {
        playerArenas.remove(player);
    }

    public void trackFallingBlock(int arenaId, UUID uuid) {
        fallingBlockService.trackFallingBlock(arenaId, uuid);
    }

    private BlockPartyState createState(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                        FloorBounds floor) {
        Boolean configuredProcedural = context.getDataAccess().getGameData("game.procedural.enabled", Boolean.class);
        boolean procedural = Boolean.TRUE.equals(configuredProcedural);
        Map<String, net.blueva.arcade.api.world.BlockPattern> patterns = procedural
                ? new HashMap<>()
                : patternService.loadPatterns(context, floor);
        List<String> order = new ArrayList<>(patterns.keySet());
        String initialPatternKey = context.getDataAccess().getGameData("game.patterns.initial", String.class);
        if (initialPatternKey == null || !patterns.containsKey(initialPatternKey)) {
            initialPatternKey = order.isEmpty() ? null : order.get(0);
        }
        double startingSearchTime = module.getSettings().getInitialSearchTime();
        Double configuredSearch = context.getDataAccess().getGameData("basic.search_time", Double.class);
        if (configuredSearch != null && configuredSearch > 0) {
            startingSearchTime = configuredSearch;
        }

        Double configuredMusic = context.getDataAccess().getGameData("basic.initial_music_time", Double.class);
        if (configuredMusic != null && configuredMusic > 0) {
            module.getSettings().setInitialMusicTime(configuredMusic);
        }

        Double configuredDecrease = context.getDataAccess().getGameData("basic.decrease_time", Double.class);
        if (configuredDecrease != null && configuredDecrease > 0) {
            module.getSettings().setDecreaseTime(configuredDecrease);
        }

        Double configuredMin = context.getDataAccess().getGameData("basic.min_search_time", Double.class);
        if (configuredMin != null && configuredMin > 0) {
            module.getSettings().setMinSearchTime(configuredMin);
        }

        List<String> proceduralTemplates = readProceduralTemplates(context);
        long matchSeed = ThreadLocalRandom.current().nextLong();
        return new BlockPartyState(context.getArenaId(), floor, patterns, order, initialPatternKey, startingSearchTime,
                procedural, proceduralTemplates, matchSeed);
    }

    private List<String> readProceduralTemplates(
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        String configured = context.getDataAccess().getGameData("game.procedural.templates", String.class);
        if (configured == null || configured.isBlank()) {
            return module.getSettings().getProceduralTemplates();
        }
        List<String> templates = new ArrayList<>();
        for (String value : configured.split(",")) {
            String template = value.trim().toLowerCase(Locale.ROOT);
            if (!template.isEmpty()) {
                templates.add(template);
            }
        }
        return templates.isEmpty() ? module.getSettings().getProceduralTemplates() : templates;
    }

    private void sendDescription(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<String> description = module.getModuleConfig().getStringListFrom("language.yml", "description.default");
        for (Player player : context.getPlayers()) {
            for (String line : description) {
                context.getMessagesAPI().sendRaw(player, line);
            }
        }
    }

    private void sendStartTitle(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        String title = module.getCoreConfig().getLanguage("titles.game_started.title")
                .replace("{game_display_name}", module.getModuleInfo().getName());
        String subtitle = module.getCoreConfig().getLanguage("titles.game_started.subtitle")
                .replace("{game_display_name}", module.getModuleInfo().getName());

        for (Player player : context.getPlayers()) {
            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 10);
        }
    }

    private void startGameLoop(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                               BlockPartyState state) {
        int arenaId = context.getArenaId();
        String taskId = "arena_" + arenaId + "_block_party_loop";

        startRound(context, state, true);

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (gameEnded.getOrDefault(arenaId, false)) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            if (state.getPhaseTicksRemaining() > 0) {
                state.setPhaseTicksRemaining(state.getPhaseTicksRemaining() - 1);
            }

            updateActionBars(context, state);

            if (state.getPhaseTicksRemaining() <= 0) {
                advancePhase(context, state);
            }

        }, 1L, 1L);
    }

    private void startRound(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                            BlockPartyState state,
                            boolean firstRound) {
        if (state.isEnded() || gameEnded.getOrDefault(state.getArenaId(), false)) {
            return;
        }

        powerupService.clearArenaPowerups(context);

        state.setRound(state.getRound() + 1);
        net.blueva.arcade.api.world.BlockPattern pattern = null;
        if (state.usesProceduralPatterns()) {
            try {
                pattern = patternService.createProceduralPattern(context, state);
            } catch (RuntimeException exception) {
                Bukkit.getLogger().warning("[BlockParty] Failed to generate a procedural pattern for arena "
                        + state.getArenaId() + ": " + exception.getMessage());
            }
        } else {
            String patternKey = patternService.selectPatternKey(state, firstRound);
            pattern = state.getPatterns().get(patternKey);
        }
        if (pattern == null && !state.getPatterns().isEmpty()) {
            pattern = state.getPatterns().values().iterator().next();
        }
        if (pattern == null) {
            pattern = patternService.createFallbackPattern(state.getFloor());
        }

        state.setCurrentPattern(pattern);
        state.setCurrentBlocks(pattern.getBlocks());
        state.setTargetMaterial(patternService.selectTargetMaterial(pattern));
        state.setPhase(RoundPhase.MUSIC);
        state.setPhaseTicksRemaining(BlockPartyUtils.secondsToTicks(module.getSettings().getInitialMusicTime()));
        state.setPhaseTotalTicks(state.getPhaseTicksRemaining());
        state.setDisplayedTime(BlockPartyUtils.ticksToSeconds(state.getPhaseTicksRemaining()));

        applyPattern(context, pattern);

        String musicTrack = null;
        if (!module.getSettings().getMusicPlaylist().isEmpty()) {
            musicTrack = state.getMusicTrack();
            if (musicTrack == null) {
                musicTrack = selectMusicTrack(module.getSettings().getMusicPlaylist());
                state.setMusicTrack(musicTrack);
            }
        }

        for (Player player : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(player,
                    module.getModuleConfig().getStringFrom("language.yml", "messages.round.starting")
                            .replace("{bp_round}", String.valueOf(state.getRound())));

            if (musicTrack != null) {
                if (state.isMusicPaused()) {
                    context.getSoundsAPI().resumeMusic(player);
                } else {
                    context.getSoundsAPI().play(player, musicTrack);
                }
            }
        }

        state.setMusicPaused(false);
    }

    private void advancePhase(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              BlockPartyState state) {
        if (state.getPhase() == RoundPhase.MUSIC) {
            revealTarget(context, state);
        } else if (state.getPhase() == RoundPhase.SEARCH) {
            collapseFloor(context, state);
            state.setPhase(RoundPhase.PAUSE);
            state.setPhaseTicksRemaining(module.getSettings().getRoundPauseTicks());
            state.setPhaseTotalTicks(state.getPhaseTicksRemaining());
            state.setDisplayedTime(BlockPartyUtils.ticksToSeconds(state.getPhaseTicksRemaining()));
        } else if (state.getPhase() == RoundPhase.PAUSE) {
            List<Player> alive = context.getAlivePlayers();
            if (alive.size() <= 1) {
                endGameOnce(context);
                return;
            }
            startRound(context, state, false);
        }
    }

    private void revealTarget(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              BlockPartyState state) {
        state.setPhase(RoundPhase.SEARCH);
        state.setPhaseTicksRemaining(BlockPartyUtils.secondsToTicks(state.getSearchSeconds()));
        state.setPhaseTotalTicks(state.getPhaseTicksRemaining());
        state.setDisplayedTime(BlockPartyUtils.ticksToSeconds(state.getPhaseTicksRemaining()));

        for (Player player : context.getPlayers()) {
            String message = module.getModuleConfig().getStringFrom("language.yml", "messages.round.reveal")
                    .replace("{block}", BlockPartyUtils.formatMaterialName(state.getTargetMaterial()));
            context.getMessagesAPI().sendRaw(player, message);
            playerEffectsService.giveTargetItem(player, state.getTargetMaterial());
        }
    }

    private void collapseFloor(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                               BlockPartyState state) {
        if (state.getCurrentBlocks() == null || state.getCurrentBlocks().isEmpty()) {
            return;
        }

        World world = resolveWorld(state.getFloor());
        if (world == null) {
            return;
        }

        for (Map.Entry<Location, org.bukkit.Material> entry : state.getCurrentBlocks().entrySet()) {
            Location loc = entry.getKey();
            org.bukkit.Material material = entry.getValue();

            if (material == null || material == org.bukkit.Material.AIR || material == state.getTargetMaterial()) {
                continue;
            }

            org.bukkit.block.Block block = loc.getBlock();
            if (block.getType() != org.bukkit.Material.AIR) {
                org.bukkit.block.data.BlockData data = block.getBlockData();
                block.setType(org.bukkit.Material.AIR);

                powerupService.removePowerupWithSupport(context, context.getArenaId(), loc, false);

                if (module.getSettings().isFallingBlocksEnabled() && data.getMaterial() != org.bukkit.Material.AIR) {
                    fallingBlockService.spawnFallingShard(context, loc, data);
                }
                if (module.getSettings().isCollapseParticlesEnabled()) {
                    world.spawnParticle(module.getSettings().getCollapseParticleType(), loc.clone().add(0.5, 0.5, 0.5),
                            module.getSettings().getCollapseParticleCount(),
                            module.getSettings().getCollapseParticleSpread(),
                            module.getSettings().getCollapseParticleSpread() * 0.6,
                            module.getSettings().getCollapseParticleSpread(),
                            module.getSettings().getCollapseParticleSpeed());
                }
            }
        }

        for (Player player : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(player, module.getModuleConfig().getStringFrom("language.yml", "messages.round.collapsing"));
        }

        playerEffectsService.clearPlayerInventories(context);
        evaluateSurvival(context, state);

        if (state.getMusicTrack() != null) {
            for (Player player : context.getPlayers()) {
                context.getSoundsAPI().pauseMusic(player);
            }
            state.setMusicPaused(true);
        }

        state.setSearchSeconds(Math.max(module.getSettings().getMinSearchTime(), state.getSearchSeconds() - module.getSettings().getDecreaseTime()));
    }

    private void evaluateSurvival(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  BlockPartyState state) {
        List<Player> alivePlayers = new ArrayList<>(context.getAlivePlayers());
        for (Player player : alivePlayers) {
            if (!player.isOnline()) {
                continue;
            }
            org.bukkit.block.Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
            if (blockBelow.getType() == state.getTargetMaterial() && module.getStatsAPI() != null) {
                module.getStatsAPI().addModuleStat(player, module.getModuleInfo().getId(), "correct_blocks", 1);
            }
        }

        if (context.getAlivePlayers().size() <= 1) {
            endGameOnce(context);
        }
    }

    private void applyPattern(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              net.blueva.arcade.api.world.BlockPattern pattern) {
        if (pattern == null) {
            return;
        }
        context.getBlocksAPI().applyPattern(pattern);
    }

    private String selectMusicTrack(List<String> playlist) {
        return playlist.get(ThreadLocalRandom.current().nextInt(playlist.size()));
    }

    private void startRegionParticles(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        if (!module.getSettings().isRegionParticlesEnabled()) {
            return;
        }
        int arenaId = context.getArenaId();

        String taskId = "arena_" + arenaId + "_block_party_particles";
        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (gameEnded.getOrDefault(arenaId, false)) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }
            spawnRegionParticles(arenaId);
        }, 20L, 20L);
    }

    private void spawnRegionParticles(int arenaId) {
        FloorBounds bounds = arenaBounds.getOrDefault(arenaId, arenaFloors.get(arenaId));
        if (bounds == null) {
            return;
        }

        World world = resolveWorld(bounds);
        if (world == null) {
            return;
        }

        double minX = Math.min(bounds.min().getX(), bounds.max().getX());
        double maxX = Math.max(bounds.min().getX(), bounds.max().getX());
        double minZ = Math.min(bounds.min().getZ(), bounds.max().getZ());
        double maxZ = Math.max(bounds.min().getZ(), bounds.max().getZ());
        double minY = Math.min(bounds.min().getY(), bounds.max().getY()) + 0.5;
        double maxY = Math.max(bounds.min().getY(), bounds.max().getY()) + 1.5;

        double spacing = Math.max(0.5, module.getSettings().getRegionParticleSpacing());
        double perimeter = 2 * ((maxX - minX) + (maxZ - minZ));
        int samples = Math.max(8, (int) Math.round(perimeter / spacing));
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < samples; i++) {
            boolean verticalEdge = random.nextBoolean();
            double x = verticalEdge
                    ? (random.nextBoolean() ? minX + 0.5 : maxX + 0.5)
                    : random.nextDouble(minX + 0.5, maxX + 0.5);
            double z = verticalEdge
                    ? random.nextDouble(minZ + 0.5, maxZ + 0.5)
                    : (random.nextBoolean() ? minZ + 0.5 : maxZ + 0.5);
            double y = random.nextDouble(minY, maxY + 0.0001);
            world.spawnParticle(module.getSettings().getRegionParticleType(), new Location(world, x, y, z),
                    module.getSettings().getRegionParticleCount(), 0, 0, 0, module.getSettings().getRegionParticleSpeed());
        }
    }

    private void updateActionBars(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  BlockPartyState state) {
        List<Player> players = context.getPlayers();
        boolean showCountdown = state.getPhase() == RoundPhase.SEARCH;
        String actionBarTemplate = showCountdown
                ? module.getModuleConfig().getStringFrom("language.yml", "action_bar.search")
                : null;
        state.setDisplayedTime(showCountdown ? Math.max(0, BlockPartyUtils.ticksToSeconds(state.getPhaseTicksRemaining())) : 0);

        for (Player player : players) {
            if (!player.isOnline()) {
                continue;
            }

            Map<String, String> placeholders = getCustomPlaceholders(player);
            placeholders.put("bp_time", showCountdown ? BlockPartyUtils.formatSeconds(state.getDisplayedTime()) : "-");
            placeholders.put("bp_round", String.valueOf(state.getRound()));

            if (showCountdown && actionBarTemplate != null) {
                String actionBarMessage = actionBarTemplate
                        .replace("{bp_time}", BlockPartyUtils.formatSeconds(state.getDisplayedTime()))
                        .replace("{bp_round}", String.valueOf(state.getRound()));
                context.getMessagesAPI().sendActionBar(player, actionBarMessage);
            } else {
                context.getMessagesAPI().sendActionBar(player, "");
            }

            updateExperienceBar(player, state);
            context.getScoreboardAPI().update(player, getScoreboardPath(), placeholders);
        }
    }

    private void broadcastDeathMessage(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       Player victim) {
        // Don't broadcast death messages for spectators
        if (context.getSpectators().contains(victim)) {
            return;
        }

        String message = getRandomMessage("messages.deaths.generic");
        if (message == null) {
            return;
        }

        message = message.replace("{victim}", victim.getName());

        for (Player player : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(player, message);
        }
    }

    private String getRandomMessage(String path) {
        List<String> messages = module.getModuleConfig().getStringListFrom("language.yml", path);
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(messages.size());
        return messages.get(index);
    }

    private void endGameOnce(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        Boolean wasEnded = gameEnded.put(arenaId, true);
        if (wasEnded != null && wasEnded) {
            return;
        }

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        List<Player> alivePlayers = new ArrayList<>(context.getAlivePlayers());
        if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.get(0);
            context.setWinner(winner);
            handleWin(winner);
        }

        context.endGame();
    }

    public void handleWin(Player player) {
        if (module.getStatsAPI() == null) {
            return;
        }

        Integer arenaId = playerArenas.get(player);
        if (arenaId == null) {
            return;
        }

        if (!arenaWinners.containsKey(arenaId)) {
            arenaWinners.put(arenaId, player.getUniqueId());
            module.getStatsAPI().addModuleStat(player, module.getModuleInfo().getId(), "wins", 1);
            module.getStatsAPI().addGlobalStat(player, "wins", 1);
        }
    }

    private FloorBounds cacheFloorBounds(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        FloorBounds bounds = findFloorBounds(context);
        if (bounds != null) {
            arenaFloors.put(arenaId, bounds);
        }
        return bounds;
    }

    private FloorBounds cacheArenaBounds(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                         FloorBounds fallback) {
        int arenaId = context.getArenaId();
        FloorBounds bounds = findArenaBounds(context);
        if (bounds == null) {
            bounds = fallback;
        }
        if (bounds != null) {
            arenaBounds.put(arenaId, bounds);
        }
        return bounds;
    }

    private FloorBounds findFloorBounds(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        Location storedMin = context.getDataAccess().getGameLocation("game.floor.bounds.min");
        Location storedMax = context.getDataAccess().getGameLocation("game.floor.bounds.max");

        if (storedMin != null && storedMax != null) {
            return new FloorBounds(storedMin, storedMax);
        }

        List<FloorRegion<Location>> floors = context.getArenaAPI().getFloors();
        if (floors != null && !floors.isEmpty()) {
            FloorRegion<Location> region = floors.get(0);
            return new FloorBounds(region.getMin(), region.getMax());
        }

        return null;
    }

    private FloorBounds findArenaBounds(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        Location min = context.getDataAccess().getArenaLocation("bounds.min");
        Location max = context.getDataAccess().getArenaLocation("bounds.max");
        if (min != null && max != null) {
            return new FloorBounds(min, max);
        }
        return null;
    }

    private void resetFloor(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        BlockPartyState state = arenaStates.get(arenaId);
        if (state == null || state.getCurrentPattern() == null) {
            return;
        }
        context.getBlocksAPI().applyPattern(state.getCurrentPattern());
    }

    private String getScoreboardPath() {
        return "scoreboard.main";
    }

    private void scheduleMaxGameTime(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        if (!module.getSettings().isRespectMaxGameTime()) {
            return;
        }
        Integer maxTimeSeconds = context.getDataAccess().getGameData("basic.time", Integer.class);
        if (maxTimeSeconds == null || maxTimeSeconds <= 0) {
            return;
        }
        int arenaId = context.getArenaId();
        String taskId = "arena_" + arenaId + "_block_party_max_time";
        context.getSchedulerAPI().runLater(taskId, () -> {
            if (!gameEnded.getOrDefault(arenaId, false)) {
                endGameOnce(context);
            }
        }, BlockPartyUtils.secondsToTicks(maxTimeSeconds));
    }

    public long resolveCurrentTick(int arenaId) {
        FloorBounds bounds = arenaFloors.get(arenaId);
        if (bounds != null && bounds.min().getWorld() != null) {
            return bounds.min().getWorld().getFullTime();
        }
        List<World> worlds = Bukkit.getWorlds();
        if (!worlds.isEmpty()) {
            return worlds.get(0).getFullTime();
        }
        return System.currentTimeMillis() / 50L;
    }

    private World resolveWorld(FloorBounds floor) {
        return floor != null ? floor.min().getWorld() : null;
    }

    private void updateExperienceBar(Player player, BlockPartyState state) {
        if (state.getPhaseTotalTicks() <= 0 || state.getPhase() != RoundPhase.SEARCH) {
            player.setExp(0f);
            player.setLevel(0);
            return;
        }

        float progress = (float) state.getPhaseTicksRemaining() / (float) state.getPhaseTotalTicks();
        progress = Math.max(0f, Math.min(1f, progress));
        player.setExp(progress);
        player.setLevel((int) Math.ceil(state.getDisplayedTime()));
    }

    public BlockPartyModule getModule() {
        return module;
    }

    public Map<Integer, Boolean> getGameEndedMap() {
        return gameEnded;
    }

    public BlockPartyState getStateForPlayer(Player player) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        if (context == null) {
            return null;
        }
        return arenaStates.get(context.getArenaId());
    }
}
