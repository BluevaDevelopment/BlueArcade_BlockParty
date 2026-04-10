package net.blueva.arcade.modules.blockparty.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;

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
    private String regionParticleType = "NOTE";
    private int regionParticleCount = 6;
    private double regionParticleSpacing = 3.5;
    private double regionParticleSpeed = 0.02;

    private boolean collapseParticlesEnabled = false;
    private String collapseParticleType = "FLAME";
    private int collapseParticleCount = 10;
    private double collapseParticleSpread = 0.35;
    private double collapseParticleSpeed = 0.1;

    private boolean powerupsEnabled = true;
    private long powerupInterval = 200L;
    private int maxPowerups = 2;
    private String powerupItemMaterial = "Stone";
    private long bonusTimeTicks = 40L;
    private int speedDurationTicks = 160;
    private int speedAmplifier = 1;
    private int patchRadius = 2;
    private boolean powerupParticlesEnabled = true;
    private String powerupParticleType = "WITCH";
    private int powerupParticleCount = 12;
    private double powerupParticleSpread = 0.35;
    private double powerupParticleSpeed = 0.12;

    private final List<String> fallbackMaterials = new ArrayList<>();
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
        regionParticleType = moduleConfig.getString("particles.region.type", "NOTE");
        regionParticleCount = moduleConfig.getInt("particles.region.count", 6);
        regionParticleSpacing = moduleConfig.getDouble("particles.region.spacing", 3.5);
        regionParticleSpeed = moduleConfig.getDouble("particles.region.speed", 0.02);

        collapseParticlesEnabled = moduleConfig.getBoolean("particles.collapse.enabled", false);
        collapseParticleType = moduleConfig.getString("particles.collapse.type", "FLAME");
        collapseParticleCount = moduleConfig.getInt("particles.collapse.count", 10);
        collapseParticleSpread = moduleConfig.getDouble("particles.collapse.spread", 0.35);
        collapseParticleSpeed = moduleConfig.getDouble("particles.collapse.speed", 0.1);

        powerupsEnabled = moduleConfig.getBoolean("powerups.enabled", true);
        powerupInterval = Math.max(20L, moduleConfig.getInt("powerups.spawn_interval_ticks", 200));
        maxPowerups = Math.max(0, moduleConfig.getInt("powerups.max_active", 2));
        powerupItemMaterial = BlockPartyUtils.normalizeMaterial(moduleConfig.getString("powerups.item", "Wool_Black"));
        bonusTimeTicks = Math.max(0L, moduleConfig.getInt("powerups.bonus_time_ticks", 40));
        speedDurationTicks = Math.max(1, moduleConfig.getInt("powerups.speed.duration_ticks", 160));
        speedAmplifier = Math.max(0, moduleConfig.getInt("powerups.speed.amplifier", 1));
        patchRadius = Math.max(1, moduleConfig.getInt("powerups.patch.radius", 2));
        powerupParticlesEnabled = moduleConfig.getBoolean("powerups.particles.enabled", true);
        powerupParticleType = moduleConfig.getString("powerups.particles.type", "WITCH");
        powerupParticleCount = moduleConfig.getInt("powerups.particles.count", 12);
        powerupParticleSpread = moduleConfig.getDouble("powerups.particles.spread", 0.35);
        powerupParticleSpeed = moduleConfig.getDouble("powerups.particles.speed", 0.12);

        fallbackMaterials.clear();
        for (String value : moduleConfig.getStringList("fallback_pattern.materials")) {
            String material = BlockPartyUtils.normalizeMaterial(value);
            if (material != null && !material.isBlank()) {
                fallbackMaterials.add(material);
            }
        }
        if (fallbackMaterials.isEmpty()) {
            fallbackMaterials.add("Stone");
            fallbackMaterials.add("Sand");
            fallbackMaterials.add("Dirt");
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

    public String getRegionParticleType() {
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

    public String getCollapseParticleType() {
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

    public String getPowerupItemMaterial() {
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

    public String getPowerupParticleType() {
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

    public List<String> getFallbackMaterials() {
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

        try (Stream<Path> paths = Files.walk(musicFolder.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".midi"))
                    .forEach(path -> {
                        // Store the absolute path without the .midi extension.
                        // MidiLibrary resolves melody IDs against the sounds directory using
                        // Path.resolve(), which returns an absolute path unchanged, so passing
                        // the absolute path here lets MidiLibrary load files from the module's
                        // own music folder without requiring them to live inside sounds/.
                        String absPath = path.toAbsolutePath().toString();
                        if (absPath.endsWith(".midi")) {
                            absPath = absPath.substring(0, absPath.length() - ".midi".length());
                        }
                        musicPlaylist.add(absPath);
                    });
        } catch (IOException ignored) {
            // Ignore read errors and keep playlist empty.
        }
    }
}
