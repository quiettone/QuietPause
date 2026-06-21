package com.quiettone.quietpause;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PauseCommand implements CommandExecutor, TabCompleter {

    private final PauseManager manager;

    public PauseCommand(PauseManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2 || !args[0].equalsIgnoreCase("f")) {
            sender.sendMessage("Usage: /quiet f <public|admin>");
            return true;
        }

        PauseManager.AccessMode mode;
        String modeArg = args[1].toLowerCase(Locale.ROOT);
        if (modeArg.equals("public")) {
            mode = PauseManager.AccessMode.PUBLIC;
        } else if (modeArg.equals("admin")) {
            mode = PauseManager.AccessMode.ADMIN;
        } else {
            sender.sendMessage("Usage: /quiet f <public|admin>");
            return true;
        }

        manager.setAccessMode(sender, mode);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && "f".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            completions.add("f");
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("f")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if ("public".startsWith(prefix)) completions.add("public");
            if ("admin".startsWith(prefix)) completions.add("admin");
        }
        return completions;
    }
}
