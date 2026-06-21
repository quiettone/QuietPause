# ⏸️ QuietPause

Give your multiplayer server a real pause button.

QuietPause pauses gameplay across your Minecraft server without shutting it down. Players, mobs, projectiles, vehicles, world time, and other gameplay systems remain frozen until the game is resumed.

Available for **Paper** and **Fabric**.

> QuietPause does not suspend the server process or stop the global server tick loop. The server remains online while normal gameplay progression is prevented.

## ✨ Features

While the game is paused, QuietPause:

* Freezes all players in place
* Prevents player movement and interactions
* Blocks inventory usage, block breaking, block placement, and item dropping
* Disables mob AI
* Freezes newly spawned mobs
* Freezes projectiles, falling blocks, primed TNT, and vehicles
* Preserves entity velocity and gravity state
* Prevents damage and hunger loss
* Prevents explosions and fire spread
* Stops world time
* Protects underwater players with Water Breathing
* Preserves and restores existing Water Breathing effects
* Applies the pause to players who join while the game is paused
* Resumes gameplay with a safe 5-second countdown
* Includes English and Turkish messages
* Provides APIs for plugin and mod integrations

## 🎮 Commands

| Command           | Description                        |
| ----------------- | ---------------------------------- |
| `/f`              | Pause or resume gameplay           |
| `/p`              | Pause or resume gameplay           |
| `/quiet f public` | Allow every player to pause        |
| `/quiet f admin`  | Restrict pausing to administrators |

### Public mode

```text
/quiet f public
```

Every player can pause the game.

Only the player who started the pause or an administrator can resume it.

### Admin mode

```text
/quiet f admin
```

Only operators and players with the required permission can pause or resume the game.

When the game is resumed, QuietPause starts a 5-second countdown before gameplay continues.

## 🔐 Permissions

### Paper

| Permission          | Default | Description                                                   |
| ------------------- | ------- | ------------------------------------------------------------- |
| `quiet.pause.admin` | OP      | Change the access mode and override normal pause restrictions |

Fabric uses the server's operator system.

## 📦 Installation

Download the latest release and choose the JAR for your platform.

### Paper

1. Download the Paper version of QuietPause.
2. Place the JAR inside your server's `plugins/` folder.
3. Restart the server.

```text
plugins/
└── quietpause-paper-1.0.0.jar
```

### Fabric

1. Install Fabric Loader and Fabric API.
2. Download the Fabric version of QuietPause.
3. Place the JAR inside your server's `mods/` folder.
4. Restart the server.

```text
mods/
├── fabric-api.jar
└── quietpause-fabric-1.0.0.jar
```

No client-side installation is required when joining a dedicated server.

## ✅ Requirements

### Paper

* Minecraft 1.21 or newer
* A compatible Paper server or Paper fork

Compatible Paper forks such as Purpur may also work.

### Fabric

* Minecraft 1.21.11
* Fabric Loader 0.19.3 or newer
* Fabric API 0.141.4 or newer

## 🌍 Languages and configuration

QuietPause includes:

* English messages
* Turkish messages
* Configurable server messages
* Public and administrator-only access modes

Configuration and language files are generated when the plugin or mod is started for the first time.

## ⚙️ How does the pause work?

QuietPause creates a server-wide gameplay pause by freezing entities and blocking actions that could progress the game.

The server itself remains online, which means:

* Players remain connected
* Commands can still be processed
* Plugins and mods can still communicate with QuietPause
* Administrators can manage the server during the pause
* The game can resume without restarting the server

## 🧩 Developer API

QuietPause provides pause state controls and lifecycle events for integrations with other plugins and mods.

## Paper API

Add the QuietPause Paper JAR to your project's compile classpath.

Declare QuietPause inside your `plugin.yml`:

```yaml
softdepend:
  - QuietPause
```

Use `depend` instead of `softdepend` when your plugin cannot function without QuietPause.

### Listen for pause and resume events

```java
import com.quiettone.quietpause.QuietPauseFreezeEvent;
import com.quiettone.quietpause.QuietPauseUnfreezeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class QuietPauseListener implements Listener {

    @EventHandler
    public void onFreeze(QuietPauseFreezeEvent event) {
        String freezerName = event.getFreezerName();

        if (event.isServerInitiated()) {
            // The pause was triggered programmatically.
        } else {
            // The pause was triggered by a player.
            System.out.println("Gameplay paused by " + freezerName);
        }

        // Prevent the pause:
        // event.setCancelled(true);
    }

    @EventHandler
    public void onUnfreeze(QuietPauseUnfreezeEvent event) {
        // Gameplay is about to resume.

        // Prevent the resume:
        // event.setCancelled(true);
    }
}
```

Register the listener from your plugin's `onEnable()` method:

```java
@Override
public void onEnable() {
    getServer().getPluginManager().registerEvents(
            new QuietPauseListener(),
            this
    );
}
```

Both Paper events are cancellable.

### Check or control the pause state

```java
import com.quiettone.quietpause.PauseManager;
import com.quiettone.quietpause.QuietPause;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

Plugin plugin = Bukkit.getPluginManager().getPlugin("QuietPause");

if (plugin instanceof QuietPause quietPause) {
    PauseManager pauseManager = quietPause.getPauseManager();

    boolean frozen = pauseManager.isFrozen();

    pauseManager.forceFreeze();
    pauseManager.forceUnfreeze();
}
```

`forceUnfreeze()` starts the normal 5-second resume countdown.

## Fabric API

Add the QuietPause Fabric JAR to your mod's compile classpath.

When QuietPause is required by your mod, add it to `fabric.mod.json`:

```json
{
  "depends": {
    "quietpause": "*"
  }
}
```

### Listen for pause and resume events

```java
import com.quiettone.quietpause.QuietPauseEvents;

QuietPauseEvents.ON_FREEZE.register((server, freezerName) -> {
    // freezerName is "Server" when triggered programmatically.
    System.out.println("Gameplay paused by " + freezerName);
});

QuietPauseEvents.ON_UNFREEZE.register(server -> {
    // Fired after the countdown finishes.
    System.out.println("Gameplay resumed");
});
```

Fabric events are notification callbacks and cannot be cancelled.

### Check or control the pause state

```java
import com.quiettone.quietpause.QuietPauseAPI;

boolean frozen = QuietPauseAPI.isFrozen();
boolean countingDown = QuietPauseAPI.isInCountdown();

QuietPauseAPI.freeze();
QuietPauseAPI.unfreeze();
```

`QuietPauseAPI.unfreeze()` starts the normal 5-second resume countdown.

## 💡 Useful for

QuietPause is useful when:

* Someone needs a quick AFK break
* A player temporarily disconnects
* Someone needs to answer the door
* A player needs to change their settings
* Your group wants to discuss a plan
* You want to pause before an important fight
* You are playing a manhunt or minigame with friends
* You do not want anyone progressing while another player is away
* You want to take a break without restarting the server

## 📄 License

QuietPause is licensed under the [Apache License 2.0](LICENSE).

---

**Someone said “BRB”? Pause the game, not the server.**
