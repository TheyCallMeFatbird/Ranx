# Ranx

## Setup

- Run `gradlew genSources` once (optional, helps IDE).
- Run `gradlew runServer` or `gradlew runClient` and open a singleplayer world.

## Ranks

- Ranks are stored in `config/ranx.json` (auto-created on first run).
- To "hardcode" staff, add your username (lowercase) under `defaultNameRanks`, for example:
  - `"tcmfatbird": "owner"`

### Commands

- `/rank <player> <rank>`: sets a player's rank (rank IDs: `owner`, `admin`, `mod`, `helper`, `player`)
- `/mute <player>`: toggles mute
- `/kick <player> [reason]`
- `/ban <player> [reason]`
- `/ip-ban <player> [reason]`

## License

MIT
