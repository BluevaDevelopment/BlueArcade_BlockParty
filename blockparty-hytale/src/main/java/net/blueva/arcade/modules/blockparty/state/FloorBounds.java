package net.blueva.arcade.modules.blockparty.state;

import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3d;

public record FloorBounds(Location min, Location max) {
    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null || min.getWorld() == null) {
            return false;
        }
        if (!loc.getWorld().equalsIgnoreCase(min.getWorld())) {
            return false;
        }
        Vector3d pos = loc.getPosition();
        Vector3d minPos = min.getPosition();
        Vector3d maxPos = max.getPosition();

        int x = (int) Math.floor(pos.x);
        int y = (int) Math.floor(pos.y);
        int z = (int) Math.floor(pos.z);

        int minX = (int) Math.floor(Math.min(minPos.x, maxPos.x));
        int maxX = (int) Math.floor(Math.max(minPos.x, maxPos.x));
        int minY = (int) Math.floor(Math.min(minPos.y, maxPos.y));
        int maxY = (int) Math.floor(Math.max(minPos.y, maxPos.y));
        int minZ = (int) Math.floor(Math.min(minPos.z, maxPos.z));
        int maxZ = (int) Math.floor(Math.max(minPos.z, maxPos.z));

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
