package net.blueva.arcade.modules.blockparty.state;

import net.blueva.arcade.api.ui.Hologram;
import org.bukkit.Location;

public class PowerupInstance {
    private final PowerupType type;
    private final Location location;
    private final Location supportLocation;
    private final org.bukkit.block.data.BlockData originalData;
    private final Hologram hologram;
    private final String particleTaskId;

    public PowerupInstance(PowerupType type, Location location, Location supportLocation,
                           org.bukkit.block.data.BlockData originalData, Hologram hologram, String particleTaskId) {
        this.type = type;
        this.location = location.getBlock().getLocation();
        this.supportLocation = supportLocation != null ? supportLocation.getBlock().getLocation() : null;
        this.originalData = originalData;
        this.hologram = hologram;
        this.particleTaskId = particleTaskId;
    }

    public PowerupType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public Location getSupportLocation() {
        return supportLocation;
    }

    public org.bukkit.block.data.BlockData getOriginalData() {
        return originalData;
    }

    public Hologram getHologram() {
        return hologram;
    }

    public String getParticleTaskId() {
        return particleTaskId;
    }
}
