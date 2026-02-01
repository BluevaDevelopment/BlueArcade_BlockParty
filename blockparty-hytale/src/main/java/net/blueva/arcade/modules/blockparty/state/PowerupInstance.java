package net.blueva.arcade.modules.blockparty.state;

import net.blueva.arcade.api.ui.Hologram;
import com.hypixel.hytale.math.vector.Location;

public class PowerupInstance {
    private final PowerupType type;
    private final Location location;
    private final Location supportLocation;
    private final String originalBlockId;
    private final Hologram<Location> hologram;
    private final String particleTaskId;

    public PowerupInstance(PowerupType type, Location location, Location supportLocation,
                           String originalBlockId, Hologram<Location> hologram, String particleTaskId) {
        this.type = type;
        this.location = location;
        this.supportLocation = supportLocation;
        this.originalBlockId = originalBlockId;
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

    public String getOriginalBlockId() {
        return originalBlockId;
    }

    public Hologram<Location> getHologram() {
        return hologram;
    }

    public String getParticleTaskId() {
        return particleTaskId;
    }
}
