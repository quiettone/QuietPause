package com.quiettone.quietpause;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PToggleCommand implements CommandExecutor {

    private final PauseManager manager;

    public PToggleCommand(PauseManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        manager.toggle(sender);
        return true;
    }
}
