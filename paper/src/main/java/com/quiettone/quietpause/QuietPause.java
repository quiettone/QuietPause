package com.quiettone.quietpause;

import org.bukkit.plugin.java.JavaPlugin;

public class QuietPause extends JavaPlugin {

    private PauseManager manager;

    @Override
    public void onEnable() {
        QuietPauseMessages.init(this);
        manager = new PauseManager(this);
        PauseCommand pauseCommand = new PauseCommand(manager);
        getCommand("quiet").setExecutor(pauseCommand);
        getCommand("quiet").setTabCompleter(pauseCommand);
        getCommand("f").setExecutor(new PToggleCommand(manager));
        getCommand("p").setExecutor(new PToggleCommand(manager));

        getServer().getPluginManager().registerEvents(new PauseListener(manager, this), this);
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.unpauseQuiet();
        }
    }

    public PauseManager getPauseManager() { return manager; }
}
