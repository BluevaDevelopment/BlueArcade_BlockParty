package net.blueva.arcade.modules.blockparty.state;

import net.blueva.arcade.api.config.ModuleConfigAPI;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public enum PowerupType {
    TELEPORT,
    PATCH,
    SPEED,
    BONUS_TIME;

    public static PowerupType random() {
        PowerupType[] values = values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    public String getDisplayName(ModuleConfigAPI moduleConfig) {
        String key = "powerup_names." + name();
        return moduleConfig.getStringFrom("language.yml", key, name().toLowerCase(Locale.ENGLISH));
    }

    public String getLore(ModuleConfigAPI moduleConfig) {
        return moduleConfig.getStringFrom("language.yml", "powerup_lore." + name(), "");
    }

    public String getPlainName() {
        return name().toLowerCase(Locale.ENGLISH).replace("_", " ");
    }
}
