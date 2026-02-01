package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class BlockPartySettings {

    private double eliminationMargin = 1.0;
    private double initialMusicTime = 3.0;
    private double initialSearchTime = 12.0;
    private double decreaseTime = 1.0;
    private double minSearchTime = 0.5;
    private long roundPauseTicks = 60L;
    private boolean giveTargetItem = true;
    private int targetItemSlot = 4;
    private boolean respectMaxGameTime = false;

    private boolean fallingBlocksEnabled = true;
    private double fallingDownwardVelocity = 0.3;
    private double fallingHorizontalRandomness = 0.08;

    private boolean regionParticlesEnabled = true;
    private Particle regionParticleType = Particle.NOTE;
    private int regionParticleCount = 6;
    private double regionParticleSpacing = 3.5;
    private double regionParticleSpeed = 0.02;

    private boolean collapseParticlesEnabled = false;
    private Particle collapseParticleType = Particle.FLAME;
    private int collapseParticleCount = 10;
    private double collapseParticleSpread = 0.35;
    private double collapseParticleSpeed = 0.1;

    private boolean powerupsEnabled = true;
    private long powerupInterval = 200L;
    private int maxPowerups = 2;
    private Material powerupItemMaterial = Material.BEACON;
    private long bonusTimeTicks = 40L;
    private int speedDurationTicks = 160;
    private int speedAmplifier = 1;
    private int patchRadius = 2;
    private boolean powerupParticlesEnabled = true;
    private Particle powerupParticleType = Particle.WITCH;
    private int powerupParticleCount = 12;
    private double powerupParticleSpread = 0.35;
    private double powerupParticleSpeed = 0.12;

    private final List<Material> fallbackMaterials = new ArrayList<>();
    private final List<String> musicPlaylist = new ArrayList<>();

    public void load(ModuleConfigAPI moduleConfig, String moduleId) {
        eliminationMargin = moduleConfig.getDouble("gameplay.elimination_margin", 1.0);
        initialMusicTime = moduleConfig.getDouble("gameplay.initial_music_time", 3.0);
        initialSearchTime = moduleConfig.getDouble("gameplay.search_time", 12.0);
        decreaseTime = moduleConfig.getDouble("gameplay.decrease_time", 1.0);
        minSearchTime = moduleConfig.getDouble("gameplay.min_search_time", 0.5);
        roundPauseTicks = Math.max(0L, moduleConfig.getInt("gameplay.round_pause_ticks", 60));
        giveTargetItem = moduleConfig.getBoolean("gameplay.give_target_item", true);
        targetItemSlot = moduleConfig.getInt("gameplay.target_item_slot", 4);
        respectMaxGameTime = moduleConfig.getBoolean("gameplay.respect_max_game_time", false);

        fallingBlocksEnabled = moduleConfig.getBoolean("gameplay.falling_blocks.enabled", true);
        fallingDownwardVelocity = moduleConfig.getDouble("gameplay.falling_blocks.downward_velocity", 0.3);
        fallingHorizontalRandomness = moduleConfig.getDouble("gameplay.falling_blocks.horizontal_randomness", 0.08);

        regionParticlesEnabled = moduleConfig.getBoolean("particles.region.enabled", true);
        regionParticleType = BlockPartyUtils.parseParticle(moduleConfig.getString("particles.region.type", "NOTE"));
        regionParticleCount = moduleConfig.getInt("particles.region.count", 6);
        regionParticleSpacing = moduleConfig.getDouble("particles.region.spacing", 3.5);
        regionParticleSpeed = moduleConfig.getDouble("particles.region.speed", 0.02);

        collapseParticlesEnabled = moduleConfig.getBoolean("particles.collapse.enabled", false);
        collapseParticleType = BlockPartyUtils.parseParticle(moduleConfig.getString("particles.collapse.type", "FLAME"));
        collapseParticleCount = moduleConfig.getInt("particles.collapse.count", 10);
        collapseParticleSpread = moduleConfig.getDouble("particles.collapse.spread", 0.35);
        collapseParticleSpeed = moduleConfig.getDouble("particles.collapse.speed", 0.1);

        powerupsEnabled = moduleConfig.getBoolean("powerups.enabled", true);
        powerupInterval = Math.max(20L, moduleConfig.getInt("powerups.spawn_interval_ticks", 200));
        maxPowerups = Math.max(0, moduleConfig.getInt("powerups.max_active", 2));
        powerupItemMaterial = BlockPartyUtils.parseMaterial(moduleConfig.getString("powerups.item", "BEACON"), Material.BEACON);
        bonusTimeTicks = Math.max(0L, moduleConfig.getInt("powerups.bonus_time_ticks", 40));
        speedDurationTicks = Math.max(1, moduleConfig.getInt("powerups.speed.duration_ticks", 160));
        speedAmplifier = Math.max(0, moduleConfig.getInt("powerups.speed.amplifier", 1));
        patchRadius = Math.max(1, moduleConfig.getInt("powerups.patch.radius", 2));
        powerupParticlesEnabled = moduleConfig.getBoolean("powerups.particles.enabled", true);
        powerupParticleType = BlockPartyUtils.parseParticle(moduleConfig.getString("powerups.particles.type", "WITCH"));
        powerupParticleCount = moduleConfig.getInt("powerups.particles.count", 12);
        powerupParticleSpread = moduleConfig.getDouble("powerups.particles.spread", 0.35);
        powerupParticleSpeed = moduleConfig.getDouble("powerups.particles.speed", 0.12);

        fallbackMaterials.clear();
        for (String value : moduleConfig.getStringList("fallback_pattern.materials")) {
            Material material = BlockPartyUtils.parseMaterial(value, null);
            if (material != null) {
                fallbackMaterials.add(material);
            }
        }
        if (fallbackMaterials.isEmpty()) {
            fallbackMaterials.add(Material.WHITE_CONCRETE);
            fallbackMaterials.add(Material.LIGHT_BLUE_CONCRETE);
            fallbackMaterials.add(Material.PINK_CONCRETE);
        }

        loadMusicPlaylist(moduleConfig, moduleId);
    }

    public double getEliminationMargin() {
        return eliminationMargin;
    }

    public double getInitialMusicTime() {
        return initialMusicTime;
    }

    public void setInitialMusicTime(double initialMusicTime) {
        this.initialMusicTime = initialMusicTime;
    }

    public double getInitialSearchTime() {
        return initialSearchTime;
    }

    public double getDecreaseTime() {
        return decreaseTime;
    }

    public void setDecreaseTime(double decreaseTime) {
        this.decreaseTime = decreaseTime;
    }

    public double getMinSearchTime() {
        return minSearchTime;
    }

    public void setMinSearchTime(double minSearchTime) {
        this.minSearchTime = minSearchTime;
    }

    public long getRoundPauseTicks() {
        return roundPauseTicks;
    }

    public boolean isGiveTargetItem() {
        return giveTargetItem;
    }

    public int getTargetItemSlot() {
        return targetItemSlot;
    }

    public boolean isRespectMaxGameTime() {
        return respectMaxGameTime;
    }

    public boolean isFallingBlocksEnabled() {
        return fallingBlocksEnabled;
    }

    public double getFallingDownwardVelocity() {
        return fallingDownwardVelocity;
    }

    public double getFallingHorizontalRandomness() {
        return fallingHorizontalRandomness;
    }

    public boolean isRegionParticlesEnabled() {
        return regionParticlesEnabled;
    }

    public Particle getRegionParticleType() {
        return regionParticleType;
    }

    public int getRegionParticleCount() {
        return regionParticleCount;
    }

    public double getRegionParticleSpacing() {
        return regionParticleSpacing;
    }

    public double getRegionParticleSpeed() {
        return regionParticleSpeed;
    }

    public boolean isCollapseParticlesEnabled() {
        return collapseParticlesEnabled;
    }

    public Particle getCollapseParticleType() {
        return collapseParticleType;
    }

    public int getCollapseParticleCount() {
        return collapseParticleCount;
    }

    public double getCollapseParticleSpread() {
        return collapseParticleSpread;
    }

    public double getCollapseParticleSpeed() {
        return collapseParticleSpeed;
    }

    public boolean isPowerupsEnabled() {
        return powerupsEnabled;
    }

    public long getPowerupInterval() {
        return powerupInterval;
    }

    public int getMaxPowerups() {
        return maxPowerups;
    }

    public Material getPowerupItemMaterial() {
        return powerupItemMaterial;
    }

    public long getBonusTimeTicks() {
        return bonusTimeTicks;
    }

    public int getSpeedDurationTicks() {
        return speedDurationTicks;
    }

    public int getSpeedAmplifier() {
        return speedAmplifier;
    }

    public int getPatchRadius() {
        return patchRadius;
    }

    public boolean isPowerupParticlesEnabled() {
        return powerupParticlesEnabled;
    }

    public Particle getPowerupParticleType() {
        return powerupParticleType;
    }

    public int getPowerupParticleCount() {
        return powerupParticleCount;
    }

    public double getPowerupParticleSpread() {
        return powerupParticleSpread;
    }

    public double getPowerupParticleSpeed() {
        return powerupParticleSpeed;
    }

    public List<Material> getFallbackMaterials() {
        return fallbackMaterials;
    }

    public List<String> getMusicPlaylist() {
        return musicPlaylist;
    }

    private void loadMusicPlaylist(ModuleConfigAPI moduleConfig, String moduleId) {
        musicPlaylist.clear();

        File moduleFolder = moduleConfig.getDataFolder();
        if (moduleFolder == null) {
            return;
        }

        File musicFolder = new File(moduleFolder, "music");
        if (!musicFolder.exists() && !musicFolder.mkdirs()) {
            return;
        }
        if (!musicFolder.isDirectory()) {
            return;
        }

        File pluginDataFolder = resolvePluginDataFolder(moduleFolder);
        if (pluginDataFolder == null) {
            return;
        }
        Path pluginDataPath = pluginDataFolder.toPath();
        try (Stream<Path> paths = Files.walk(musicFolder.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".nbs"))
                    .forEach(path -> {
                        String relative = pluginDataPath.relativize(path).toString().replace(File.separatorChar, '/');
                        musicPlaylist.add("[NBS] " + relative);
                    });
        } catch (IOException ignored) {
            // Ignore read errors and keep playlist empty.
        }
    }

    private File resolvePluginDataFolder(File moduleFolder) {
        File modulesFolder = moduleFolder.getParentFile();
        if (modulesFolder == null) {
            return null;
        }
        return modulesFolder.getParentFile();
    }
}
