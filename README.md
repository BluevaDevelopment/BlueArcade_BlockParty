# BlueArcade - Block Party

This resource is a **BlueArcade 3 module** and requires the core plugin to run.
Get BlueArcade 3 here: https://store.blueva.net/resources/resource/1-blue-arcade/

## Description
Match the correct color before the floor disappears. Quick reactions win the round.

## Game type notes
This is a **Minigame**: it is designed for standalone arenas, but it can also be used inside party rotations. Minigames usually provide longer, feature-rich rounds.

## What you get with BlueArcade 3 + this module
- Party system (lobbies, queues, and shared party flow).
- Store-ready menu integration and vote menus.
- Victory effects and end-game celebrations.
- Scoreboards, timers, and game lifecycle management.
- Player stats tracking and placeholders.
- XP system, leaderboards, and achievements.
- Arena management tools and setup commands.

## Features
- Pattern system for color sequences.
- Flexible timing controls for music and search phases.
- Floor region setup with the magic stick tool.

## Arena setup
### Common steps
Use these steps to register the arena and attach the module:

- `/baa create [id] <standalone|party>` — Create a new arena in standalone or party mode.
- `/baa arena [id] setname [name]` — Give the arena a friendly display name.
- `/baa arena [id] setlobby` — Set the lobby spawn for the arena.
- `/baa arena [id] minplayers [amount]` — Define the minimum players required to start.
- `/baa arena [id] maxplayers [amount]` — Define the maximum players allowed.
- `/baa game [arena_id] add [minigame]` — Attach this minigame module to the arena.
- `/baa stick` — Get the setup tool to select regions.
- `/baa game [arena_id] [minigame] bounds set` — Save the game bounds for this arena.
- `/baa game [arena_id] [minigame] spawn add` — Add spawn points for players.
- `/baa game [arena_id] [minigame] time [minutes]` — Set the match duration.

### Module-specific steps
Finish the setup with the commands below:
- `/baa game [arena_id] block_party floor set` — Select and save the floor area.
- `/baa game [arena_id] block_party pattern add <name>` — Create a new color pattern.
- `/baa game [arena_id] block_party pattern remove <name>` — Remove a pattern.
- `/baa game [arena_id] block_party pattern list` — List existing patterns.
- `/baa game [arena_id] block_party pattern initial <name>` — Set the initial pattern.
- `/baa game [arena_id] block_party musictime <seconds>` — Set the music phase length (optional).
- `/baa game [arena_id] block_party searchtime <seconds>` — Set the time to find the color (optional).
- `/baa game [arena_id] block_party decreasetime <seconds>` — Decrease the search time per round (optional).
- `/baa game [arena_id] block_party mintime <seconds>` — Set the minimum search time (optional).

## Technical details
- **Minigame ID:** `block_party`
- **Module Type:** `MINIGAME`

## Building individual editions
If you only need one edition, you can build it on its own:
- `mvn clean package -pl blockparty-minecraft -am`
- `mvn clean package -pl blockparty-hytale -am`

## Links & Support
- Website: https://www.blueva.net
- Documentation: https://docs.blueva.net/books/blue-arcade
- Support: https://discord.com/invite/CRFJ32NdcK
