package net.blueva.arcade.modules.blockparty;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.achievements.AchievementsAPI;
import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.events.CustomEventRegistry;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameModule;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.api.ui.ItemAPI;
import net.blueva.arcade.api.ui.VoteMenuAPI;
import net.blueva.arcade.modules.blockparty.game.BlockPartyGame;
import net.blueva.arcade.modules.blockparty.listener.BlockPartyListener;
import net.blueva.arcade.modules.blockparty.setup.BlockPartySetup;
import net.blueva.arcade.modules.blockparty.support.BlockPartySettings;
import net.blueva.arcade.modules.blockparty.support.BlockPartyStats;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class BlockPartyModule implements GameModule<Player, Location, World, Material, ItemStack, Sound, Block, Entity, Listener, EventPriority> {

    private ModuleConfigAPI moduleConfig;
    private CoreConfigAPI coreConfig;
    private ModuleInfo moduleInfo;
    private StatsAPI statsAPI;
    private ItemAPI<Player, ItemStack, Material> itemAPI;
    private BlockPartySettings settings;
    private BlockPartyGame game;

    @Override
    public void onLoad() {
        moduleInfo = ModuleAPI.getModuleInfo("block_party");

        if (moduleInfo == null) {
            throw new IllegalStateException("ModuleInfo not available for Block Party module");
        }

        moduleConfig = ModuleAPI.getModuleConfig(moduleInfo.getId());
        coreConfig = ModuleAPI.getCoreConfig();
        statsAPI = ModuleAPI.getStatsAPI();
        itemAPI = castItemApi(ModuleAPI.getItemAPI());
        VoteMenuAPI voteMenu = ModuleAPI.getVoteMenuAPI();
        AchievementsAPI achievementsAPI = ModuleAPI.getAchievementsAPI();

        moduleConfig.register("language.yml", 1);
        moduleConfig.register("settings.yml", 1);
        moduleConfig.register("achievements.yml", 1);

        settings = new BlockPartySettings();
        settings.load(moduleConfig, moduleInfo.getId());

        BlockPartyStats blockPartyStats = new BlockPartyStats(statsAPI, moduleInfo);
        blockPartyStats.register();

        if (achievementsAPI != null) {
            achievementsAPI.registerModuleAchievements(moduleInfo.getId(), "achievements.yml");
        }

        ModuleAPI.getSetupAPI().registerHandler(moduleInfo.getId(), new BlockPartySetup(this));

        if (moduleConfig != null && voteMenu != null) {
            voteMenu.registerGame(
                    moduleInfo.getId(),
                    Material.valueOf(moduleConfig.getString("menus.vote.item")),
                    moduleConfig.getStringFrom("language.yml", "vote_menu.name"),
                    moduleConfig.getStringListFrom("language.yml", "vote_menu.lore")
            );
        }

        game = new BlockPartyGame(this);
    }

    @Override
    public void onStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.onStart(context);
    }

    @Override
    public void onCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                int secondsLeft) {
        game.onCountdownTick(context, secondsLeft);
    }

    @Override
    public void onCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.onCountdownFinish(context);
    }

    @Override
    public boolean freezePlayersOnCountdown() {
        return false;
    }

    @Override
    public void onGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.onGameStart(context);
    }

    @Override
    public void onEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                      GameResult<Player> result) {
        game.onEnd(context, result);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.onDisable();
        }
    }

    @Override
    public void registerEvents(CustomEventRegistry<Listener, EventPriority> registry) {
        registry.register(new BlockPartyListener(this));
    }

    @Override
    public Map<String, String> getCustomPlaceholders(Player player) {
        return game.getCustomPlaceholders(player);
    }

    public ModuleConfigAPI getModuleConfig() {
        return moduleConfig;
    }

    public CoreConfigAPI getCoreConfig() {
        return coreConfig;
    }

    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public StatsAPI getStatsAPI() {
        return statsAPI;
    }

    public ItemAPI<Player, ItemStack, Material> getItemAPI() {
        return itemAPI;
    }

    public BlockPartySettings getSettings() {
        return settings;
    }

    public BlockPartyGame getGame() {
        return game;
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContext(Player player) {
        return game.getGameContext(player);
    }

    public void handleRespawnEffects(Player player) {
        game.handleRespawnEffects(player);
    }

    public boolean shouldEliminate(Player player, Location to) {
        return game.shouldEliminate(player, to);
    }

    public void handlePlayerElimination(Player player) {
        game.handlePlayerElimination(player);
    }

    public void handlePowerupPickup(Player player, Location location) {
        game.handlePowerupPickup(player, location);
    }

    public boolean isPowerupBlock(Block block) {
        return game.isPowerupBlock(block);
    }

    public boolean isTrackedFallingBlock(UUID uuid) {
        return game.isTrackedFallingBlock(uuid);
    }

    public void handleFallingBlockLand(FallingBlock fallingBlock) {
        game.handleFallingBlockLand(fallingBlock);
    }

    public void handleWin(Player player) {
        game.handleWin(player);
    }

    @SuppressWarnings("unchecked")
    private ItemAPI<Player, ItemStack, Material> castItemApi(ItemAPI<?, ?, ?> api) {
        return (ItemAPI<Player, ItemStack, Material>) api;
    }
}
