package net.blueva.arcade.modules.blockparty.setup;

import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
import net.blueva.arcade.api.setup.SetupDataAPI;
import net.blueva.arcade.api.setup.SetupSelectionAPI;
import net.blueva.arcade.api.setup.TabCompleteContext;
import net.blueva.arcade.api.setup.TabCompleteResult;
import net.blueva.arcade.modules.blockparty.BlockPartyModule;
import com.hypixel.hytale.math.vector.Location;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BlockPartySetup implements GameSetupHandler {

    private final BlockPartyModule module;

    public BlockPartySetup(BlockPartyModule module) {
        this.module = module;
    }

    @Override
    public boolean handle(SetupContext context) {
        return handleInternal(castSetupContext(context));
    }

    private boolean handleInternal(SetupContext<Player, CommandSender, Location> context) {
        String subcommand = context.getArg(context.getStartIndex() - 1).toLowerCase(Locale.ENGLISH);

        return switch (subcommand) {
            case "floor" -> handleFloor(context);
            case "pattern" -> handlePattern(context);
            case "musictime" -> handleTime(context, "basic.initial_music_time",
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_music_time"));
            case "searchtime" -> handleTime(context, "basic.search_time",
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_search_time"));
            case "decreasetime" -> handleTime(context, "basic.decrease_time",
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_decrease_time"));
            case "mintime" -> handleTime(context, "basic.min_search_time",
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_min_time"));
            default -> {
                context.getMessagesAPI().sendRaw(context.getPlayer(),
                        module.getCoreConfig().getLanguage("admin_commands.errors.unknown_subcommand"));
                yield true;
            }
        };
    }

    @Override
    public TabCompleteResult tabComplete(TabCompleteContext context) {
        return tabCompleteInternal(castTabContext(context));
    }

    private TabCompleteResult tabCompleteInternal(TabCompleteContext<Player, CommandSender> context) {
        String sub = context.getArg(context.getStartIndex() - 1).toLowerCase(Locale.ENGLISH);
        int relIndex = context.getRelativeArgIndex();

        if ("pattern".equals(sub)) {
            if (relIndex == 0) {
                return TabCompleteResult.of("add", "remove", "list", "initial");
            }
            if (relIndex == 1 && ("remove".equals(context.getArg(context.getStartIndex())) ||
                    "initial".equals(context.getArg(context.getStartIndex())))) {
                return TabCompleteResult.of();
            }
        }

        if (relIndex == 0) {
            switch (sub) {
                case "floor" -> {
                    return TabCompleteResult.of("set");
                }
                case "musictime", "searchtime", "decreasetime", "mintime" -> {
                    return TabCompleteResult.of("<seconds>");
                }
                default -> {
                }
            }
        }

        return TabCompleteResult.empty();
    }

    @Override
    public List<String> getSubcommands() {
        return Arrays.asList("floor", "pattern", "musictime", "searchtime", "decreasetime", "mintime");
    }

    @Override
    public boolean validateConfig(SetupContext context) {
        return validateConfigInternal(castSetupContext(context));
    }

    private boolean validateConfigInternal(SetupContext<Player, CommandSender, Location> context) {
        SetupDataAPI data = context.getData();

        boolean hasFloor = data.has("game.floor.bounds.min.x") && data.has("game.floor.bounds.max.x");
        boolean hasPatterns = data.getString("game.patterns.index") != null;

        if (!hasFloor || !hasPatterns) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.not_configured")
                            .replace("{arena_id}", String.valueOf(context.getArenaId())));
        }

        return hasFloor && hasPatterns;
    }

    private boolean handleFloor(SetupContext<Player, CommandSender, Location> context) {
        if (!context.isPlayer()) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getCoreConfig().getLanguage("admin_commands.errors.must_be_player"));
            return true;
        }

        if (!context.hasHandlerArgs(1)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_floor"));
            return true;
        }

        String action = context.getHandlerArg(0).toLowerCase(Locale.ENGLISH);
        if (!action.equals("set")) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_floor"));
            return true;
        }

        Player player = context.getPlayer();
        SetupSelectionAPI<Player, Location> selection = context.getSelection();

        if (!selection.hasCompleteSelection(player)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.must_use_stick"));
            return true;
        }

        Location pos1 = selection.getPosition1(player);
        Location pos2 = selection.getPosition2(player);

        SetupDataAPI data = context.getData();
        data.setRegionBounds("game.floor.bounds", pos1, pos2);
        data.remove("game.floor.blocks");
        data.save();

        Vector3d pos1Vec = pos1.getPosition();
        Vector3d pos2Vec = pos2.getPosition();
        int x = (int) Math.abs(pos2Vec.x - pos1Vec.x) + 1;
        int y = (int) Math.abs(pos2Vec.y - pos1Vec.y) + 1;
        int z = (int) Math.abs(pos2Vec.z - pos1Vec.z) + 1;
        int blocks = x * y * z;

        context.getMessagesAPI().sendRaw(context.getPlayer(),
                module.getModuleConfig().getStringFrom("language.yml", "setup_messages.set_success")
                        .replace("{blocks}", String.valueOf(blocks)).replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y)).replace("{z}", String.valueOf(z)));

        return true;
    }

    private boolean handlePattern(SetupContext<Player, CommandSender, Location> context) {
        if (!context.hasHandlerArgs(1)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_pattern_add"));
            return true;
        }

        String action = context.getHandlerArg(0).toLowerCase(Locale.ENGLISH);
        return switch (action) {
            case "add" -> handlePatternAdd(context);
            case "remove" -> handlePatternRemove(context);
            case "list" -> handlePatternList(context);
            case "initial" -> handlePatternInitial(context);
            default -> {
                context.getMessagesAPI().sendRaw(context.getPlayer(),
                        module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_pattern_add"));
                yield true;
            }
        };
    }

    private boolean handlePatternAdd(SetupContext<Player, CommandSender, Location> context) {
        if (!context.isPlayer()) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getCoreConfig().getLanguage("admin_commands.errors.must_be_player"));
            return true;
        }

        SetupDataAPI data = context.getData();
        List<String> index = parseIndex(data.getString("game.patterns.index"));

        String name;
        if (context.hasHandlerArgs(2)) {
            name = context.getHandlerArg(1).toLowerCase(Locale.ENGLISH);
        } else {
            name = generateNextPatternName(index);
        }

        if (index.contains(name)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.pattern_exists")
                            .replace("{name}", name));
            return true;
        }

        Player player = context.getPlayer();
        if (!context.getSelection().hasCompleteSelection(player)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.must_use_stick"));
            return true;
        }

        Location pos1 = context.getSelection().getPosition1(player);
        Location pos2 = context.getSelection().getPosition2(player);

        data.saveRegionBlocks("game.patterns." + name, pos1, pos2);
        index.add(name);
        saveIndex(data, index);
        if (data.getString("game.patterns.initial") == null) {
            data.setString("game.patterns.initial", name);
        }
        data.save();

        Vector3d pos1Vec = pos1.getPosition();
        Vector3d pos2Vec = pos2.getPosition();
        int x = (int) Math.abs(pos2Vec.x - pos1Vec.x) + 1;
        int y = (int) Math.abs(pos2Vec.y - pos1Vec.y) + 1;
        int z = (int) Math.abs(pos2Vec.z - pos1Vec.z) + 1;
        int blocks = x * y * z;

        context.getMessagesAPI().sendRaw(context.getPlayer(),
                module.getModuleConfig().getStringFrom("language.yml", "setup_messages.pattern_added")
                        .replace("{name}", name).replace("{blocks}", String.valueOf(blocks)));
        return true;
    }

    private boolean handlePatternRemove(SetupContext<Player, CommandSender, Location> context) {
        if (!context.hasHandlerArgs(2)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_pattern_remove"));
            return true;
        }

        String name = context.getHandlerArg(1).toLowerCase(Locale.ENGLISH);

        SetupDataAPI data = context.getData();
        List<String> index = parseIndex(data.getString("game.patterns.index"));
        if (!index.remove(name)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.pattern_missing")
                            .replace("{name}", name));
            return true;
        }

        data.remove("game.patterns." + name);
        saveIndex(data, index);

        if (name.equalsIgnoreCase(data.getString("game.patterns.initial"))) {
            data.remove("game.patterns.initial");
        }

        data.save();
        context.getMessagesAPI().sendRaw(context.getPlayer(),
                module.getModuleConfig().getStringFrom("language.yml", "setup_messages.pattern_removed")
                        .replace("{name}", name));
        return true;
    }

    private boolean handlePatternList(SetupContext<Player, CommandSender, Location> context) {
        SetupDataAPI data = context.getData();
        List<String> index = parseIndex(data.getString("game.patterns.index"));

        context.getMessagesAPI().sendRaw(context.getPlayer(),
                module.getModuleConfig().getStringFrom("language.yml", "setup_messages.pattern_list_header"));
        for (String name : index) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.pattern_list_entry")
                            .replace("{name}", name));
        }

        return true;
    }

    private boolean handlePatternInitial(SetupContext<Player, CommandSender, Location> context) {
        if (!context.hasHandlerArgs(2)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_pattern_initial"));
            return true;
        }

        String name = context.getHandlerArg(1).toLowerCase(Locale.ENGLISH);
        SetupDataAPI data = context.getData();
        List<String> index = parseIndex(data.getString("game.patterns.index"));
        if (!index.contains(name)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.pattern_missing")
                            .replace("{name}", name));
            return true;
        }
        data.setString("game.patterns.initial", name);
        data.save();

        context.getMessagesAPI().sendRaw(context.getPlayer(),
                module.getModuleConfig().getStringFrom("language.yml", "setup_messages.pattern_initial_set")
                        .replace("{name}", name));
        return true;
    }

    private boolean handleTime(SetupContext<Player, CommandSender, Location> context, String path, String usage) {
        if (!context.hasHandlerArgs(1)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), usage);
            return true;
        }

        try {
            double value = Double.parseDouble(context.getHandlerArg(0));
            context.getData().setDouble(path, value);
            context.getData().save();
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.time_updated")
                            .replace("{key}", path).replace("{value}", context.getHandlerArg(0)));
        } catch (NumberFormatException e) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), usage);
        }

        return true;
    }

    private List<String> parseIndex(String index) {
        if (index == null || index.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> names = new ArrayList<>();
        for (String part : index.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names;
    }

    private void saveIndex(SetupDataAPI data, List<String> names) {
        if (names.isEmpty()) {
            data.setString("game.patterns.index", "");
        } else {
            data.setString("game.patterns.index", String.join(",", names));
        }
    }

    private String generateNextPatternName(List<String> existing) {
        int maxNumber = 0;
        for (String entry : existing) {
            try {
                maxNumber = Math.max(maxNumber, Integer.parseInt(entry));
            } catch (NumberFormatException ignored) {
                // Non-numeric names are ignored when picking the next automatic id.
            }
        }
        return String.valueOf(maxNumber + 1);
    }

    @SuppressWarnings("unchecked")
    private SetupContext<Player, CommandSender, Location> castSetupContext(SetupContext context) {
        return (SetupContext<Player, CommandSender, Location>) context;
    }

    @SuppressWarnings("unchecked")
    private TabCompleteContext<Player, CommandSender> castTabContext(TabCompleteContext context) {
        return (TabCompleteContext<Player, CommandSender>) context;
    }
}
