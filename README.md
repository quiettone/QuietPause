# QuietPause

Freezes your entire Minecraft server without shutting it down. Available for both Paper and Fabric.

## Features

- Disables all mob AI
- Freezes projectiles, falling blocks, TNT and vehicles
- Stops the daylight cycle
- Gives players water breathing while frozen
- 5-second countdown before resuming

## Commands

| Command | Description |
|---|---|
| `/f` or `/p` | Toggle pause |
| `/quiet f public` | Allow anyone to pause |
| `/quiet f admin` | Allow only admins to pause |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `quiet.pause.admin` | OP | Can use access mode commands |

## Installation

### Paper
Build from the `paper/` folder and drop the `.jar` into your `plugins/` folder.

### Fabric
Build from the `fabric/` folder and drop the `.jar` into your `mods/` folder.

## License

Apache 2.0 — see [LICENSE](LICENSE)
