package com.quiettone.quietpause;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Vehicle;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class PauseManager {
    public enum AccessMode {
        PUBLIC,
        ADMIN
    }

    private boolean frozen = false;
    private boolean inCountdown = false;
    private String freezerName = null;
    private UUID freezerUUID = null;
    private AccessMode accessMode = AccessMode.PUBLIC;
    private final Plugin plugin;
    private final Map<UUID, Vector> frozenEntityVelocities = new HashMap<>();
    private final Map<UUID, Boolean> frozenEntityGravity = new HashMap<>();
    private final Map<UUID, Boolean> frozenMobAI = new HashMap<>();

    public PauseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void forceFreeze() {
        if (frozen || inCountdown) {
            return;
        }
        freezeFromServer();
    }

    public void forceUnfreeze() {
        if (!frozen || inCountdown) {
            return;
        }
        startCountdown();
    }

    public void setAccessMode(CommandSender sender, AccessMode requestedMode) {
        if (!isAdmin(sender)) {
            sender.sendMessage(QuietPauseMessages.text(
                    sender,
                    "quietpause.error.no_permission",
                    QuietPauseMessages.noPlaceholders()
            ));
            return;
        }
        accessMode = requestedMode;
    }

    public void toggle(CommandSender sender) {
        if (inCountdown) {
            return;
        }

        if (frozen) {
            if (!canUnfreeze(sender)) {
                sender.sendMessage(QuietPauseMessages.text(
                        sender,
                        "quietpause.error.no_permission",
                        QuietPauseMessages.noPlaceholders()
                ));
                return;
            }
            startCountdown();
        } else {
            if (!canFreeze(sender)) {
                sender.sendMessage(QuietPauseMessages.text(
                        sender,
                        "quietpause.error.no_permission",
                        QuietPauseMessages.noPlaceholders()
                ));
                return;
            }
            freezerName = sender.getName();
            freezerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
            freeze(freezerName);
        }
    }

    private boolean canFreeze(CommandSender sender) {
        if (accessMode == AccessMode.PUBLIC) {
            return true;
        }
        return isAdmin(sender);
    }

    private boolean canUnfreeze(CommandSender sender) {
        if (isAdmin(sender)) {
            return true;
        }
        if (freezerUUID != null && sender instanceof Player) {
            return ((Player) sender).getUniqueId().equals(freezerUUID);
        }
        return freezerUUID == null;
    }

    private boolean isAdmin(CommandSender sender) {
        return !(sender instanceof Player) || ((Player) sender).isOp() || sender.hasPermission("quiet.pause.admin");
    }

    private void freeze(String callerName) {
        frozen = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
        }
        setMobAI(false);
        freezeEntities();
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }
        QuietPauseMessages.broadcast(
                "quietpause.pause.started",
                QuietPauseMessages.placeholders("player", callerName)
        );
        notifyAbilityManager("onGameFreeze");
    }

    private void freezeFromServer() {
        frozen = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
        }
        setMobAI(false);
        freezeEntities();
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }
        QuietPauseMessages.broadcast("quietpause.pause.started.server", QuietPauseMessages.noPlaceholders());
        notifyAbilityManager("onGameFreeze");
    }

    private void startCountdown() {
        inCountdown = true;

        for (int i = 5; i >= 1; i--) {
            final int count = i;
            long delay = (long) (5 - i) * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                QuietPauseMessages.broadcast(
                        "quietpause.pause.countdown",
                        QuietPauseMessages.placeholders("seconds", count)
                );
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                }
            }, delay);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            inCountdown = false;
            unfreeze();
        }, 5 * 20L);
    }

    private void unfreeze() {
        frozen = false;
        freezerName = null;
        freezerUUID = null;
        unfreezeEntities();
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        }
        notifyAbilityManager("onGameUnfreeze");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 1.2f);
        }
        QuietPauseMessages.broadcast("quietpause.pause.resumed", QuietPauseMessages.noPlaceholders());
    }

    private void freezeEntities() {
        frozenEntityVelocities.clear();
        frozenEntityGravity.clear();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!isFreezable(entity)) {
                    continue;
                }
                trackEntity(entity);
                entity.setVelocity(new Vector(0, 0, 0));
                entity.setGravity(false);
            }
        }
    }

    private void unfreezeEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                UUID id = entity.getUniqueId();
                if (!frozenEntityVelocities.containsKey(id)
                        && !frozenEntityGravity.containsKey(id)
                        && !frozenMobAI.containsKey(id)) {
                    continue;
                }
                restoreTrackedEntity(entity);
            }
        }
        frozenEntityVelocities.clear();
        frozenEntityGravity.clear();
        frozenMobAI.clear();
    }

    public void freezeNewEntity(Entity entity) {
        if (!frozen || !isFreezable(entity)) {
            return;
        }
        trackEntity(entity);
        entity.setVelocity(new Vector(0, 0, 0));
        entity.setGravity(false);
    }

    public void restoreTrackedEntity(Entity entity) {
        UUID id = entity.getUniqueId();
        Vector velocity = frozenEntityVelocities.remove(id);
        Boolean gravity = frozenEntityGravity.remove(id);
        if (gravity != null) {
            entity.setGravity(gravity);
        }
        if (velocity != null) {
            entity.setVelocity(velocity);
        }
        if (entity instanceof LivingEntity && !(entity instanceof Player)) {
            Boolean ai = frozenMobAI.remove(id);
            if (ai != null) {
                ((LivingEntity) entity).setAI(ai);
            }
        }
    }

    private void trackEntity(Entity entity) {
        UUID id = entity.getUniqueId();
        frozenEntityVelocities.putIfAbsent(id, entity.getVelocity().clone());
        frozenEntityGravity.putIfAbsent(id, entity.hasGravity());
        if (entity instanceof LivingEntity && !(entity instanceof Player)) {
            frozenMobAI.putIfAbsent(id, ((LivingEntity) entity).hasAI());
        }
    }

    private boolean isFreezable(Entity entity) {
        return entity instanceof Projectile || entity instanceof FallingBlock
                || entity instanceof TNTPrimed || entity instanceof Vehicle;
    }

    private void setMobAI(boolean enabled) {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                if (!frozenMobAI.containsKey(entity.getUniqueId())) {
                    frozenMobAI.put(entity.getUniqueId(), entity.hasAI());
                }
                entity.setAI(enabled);
            }
        }
    }

    private void notifyAbilityManager(String methodName) {
        org.bukkit.plugin.Plugin speedrun = Bukkit.getPluginManager().getPlugin("QuietSpeedrun");
        if (speedrun == null || !speedrun.isEnabled()) {
            return;
        }
        try {
            Object abilityManager = speedrun.getClass().getMethod("getAbilityManager").invoke(speedrun);
            abilityManager.getClass().getMethod(methodName).invoke(abilityManager);
        } catch (Exception ignored) {
        }
    }

    public void pauseQuiet() {
        if (frozen || inCountdown) {
            return;
        }
        frozen = true;
        setMobAI(false);
        freezeEntities();
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }
        notifyAbilityManager("onGameFreeze");
    }

    public void unpauseQuiet() {
        if (!frozen) {
            return;
        }
        inCountdown = false;
        frozen = false;
        freezerName = null;
        accessMode = AccessMode.PUBLIC;
        unfreezeEntities();
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        }
        notifyAbilityManager("onGameUnfreeze");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        }
    }

    public void applyTo(Player player) {
        if (!frozen || inCountdown) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
    }
}
