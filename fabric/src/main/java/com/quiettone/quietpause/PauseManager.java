package com.quiettone.quietpause;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PauseManager {
    public enum AccessMode {
        PUBLIC,
        ADMIN
    }

    private boolean frozen;
    private boolean inCountdown;
    private String freezerName;
    private AccessMode accessMode = AccessMode.PUBLIC;
    private int countdownTicks;
    private int nextCountdownNumber;
    private MinecraftServer server;
    private final Map<UUID, Vec3d> frozenEntityVelocities = new HashMap<>();
    private final Map<UUID, Vec3d> frozenPlayerPositions = new HashMap<>();
    private final Map<RegistryKey<World>, Long> frozenWorldTimes = new HashMap<>();
    private final Map<UUID, StatusEffectInstance> savedWaterBreathing = new HashMap<>();

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isInCountdown() {
        return inCountdown;
    }

    public void tick(MinecraftServer server) {
        this.server = server;

        if (inCountdown) {
            tickCountdown();
        }

        if (!frozen) {
            return;
        }

        freezeEntities(server);
        freezePlayers(server);
        freezeWorldTimes(server);
    }

    public void setAccessMode(ServerCommandSource source, AccessMode requestedMode) {
        if (!isAdmin(source)) {
            source.sendError(QuietPauseMessages.text(source, "quietpause.error.no_permission", QuietPauseMessages.noPlaceholders()));
            return;
        }

        accessMode = requestedMode;
    }

    public void toggle(ServerCommandSource source) {
        server = source.getServer();

        if (inCountdown) {
            return;
        }

        if (frozen) {
            if (!canToggleFrozen(source)) {
                source.sendError(QuietPauseMessages.text(source, "quietpause.error.no_permission", QuietPauseMessages.noPlaceholders()));
                return;
            }

            startCountdown();
            return;
        }

        if (!canToggleFrozen(source)) {
            source.sendError(QuietPauseMessages.text(source, "quietpause.error.no_permission", QuietPauseMessages.noPlaceholders()));
            return;
        }
        freezerName = source.getName();
        freeze(freezerName);
    }

    private boolean canToggleFrozen(ServerCommandSource source) {
        if (isAdmin(source)) {
            return true;
        }
        if (accessMode == AccessMode.ADMIN) {
            return false;
        }
        // PUBLIC mode: only the freezer can unfreeze
        if (frozen) {
            return source.getName().equals(freezerName);
        }
        return true;
    }

    private boolean isAdmin(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        return player == null || source.getServer().getPlayerManager().isOperator(new PlayerConfigEntry(player.getGameProfile()));
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

    public void pauseQuiet() {
        if (frozen || inCountdown || server == null) {
            return;
        }
        frozen = true;
        freezeWorlds(server);
        freezeEntities(server);
        QuietPauseEvents.ON_FREEZE.invoker().onFreeze(server, "Server");
    }

    public void unpauseQuiet() {
        if (!frozen || server == null) {
            return;
        }
        inCountdown = false;
        frozen = false;
        freezerName = null;
        unfreezeEntities(server);
        restoreWorlds(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.removeStatusEffect(StatusEffects.WATER_BREATHING);
            StatusEffectInstance saved = savedWaterBreathing.remove(player.getUuid());
            if (saved != null) {
                player.addStatusEffect(saved);
            }
        }
        savedWaterBreathing.clear();
        frozenPlayerPositions.clear();
        QuietPauseEvents.ON_UNFREEZE.invoker().onUnfreeze(server);
    }

    public void applyTo(ServerPlayerEntity player) {
        if (!frozen || inCountdown) return;

        StatusEffectInstance existing = player.getStatusEffect(StatusEffects.WATER_BREATHING);
        if (existing != null) {
            savedWaterBreathing.put(player.getUuid(), existing);
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, -1, 0, false, false, false));
        frozenPlayerPositions.put(player.getUuid(), pos(player));
    }

    private void freeze(String callerName) {
        if (server == null) {
            return;
        }

        frozen = true;
        freezeWorlds(server);
        freezeEntities(server);
        frozenPlayerPositions.clear();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            applyTo(player);
            playSound(player, SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("§8[§5QuietTone§8] ").append(QuietPauseMessages.text(player, "quietpause.pause.started", QuietPauseMessages.placeholders("player", callerName))), false);
        }
        QuietPauseEvents.ON_FREEZE.invoker().onFreeze(server, callerName);
    }

    private void freezeFromServer() {
        if (server == null) {
            return;
        }

        frozen = true;
        freezeWorlds(server);
        freezeEntities(server);
        frozenPlayerPositions.clear();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            applyTo(player);
            playSound(player, SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("§8[§5QuietTone§8] ").append(QuietPauseMessages.text(player, "quietpause.pause.started.server", QuietPauseMessages.noPlaceholders())), false);
        }
        QuietPauseEvents.ON_FREEZE.invoker().onFreeze(server, "Server");
    }

    private void startCountdown() {
        inCountdown = true;
        countdownTicks = 0;
        nextCountdownNumber = 5;
    }

    private void tickCountdown() {
        if (countdownTicks % 20 == 0 && nextCountdownNumber >= 1) {
            broadcast("quietpause.pause.countdown", QuietPauseMessages.placeholders("seconds", nextCountdownNumber));
            if (server != null) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    playSound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.2f);
                }
            }
            nextCountdownNumber--;
        }

        countdownTicks++;
        if (countdownTicks >= 100) {
            inCountdown = false;
            unfreeze();
        }
    }

    private void unfreeze() {
        if (server == null) {
            return;
        }

        frozen = false;
        freezerName = null;
        unfreezeEntities(server);
        restoreWorlds(server);
        frozenPlayerPositions.clear();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.removeStatusEffect(StatusEffects.WATER_BREATHING);
            StatusEffectInstance saved = savedWaterBreathing.remove(player.getUuid());
            if (saved != null) {
                player.addStatusEffect(saved);
            }
            playSound(player, SoundEvents.BLOCK_GLASS_PLACE, 1.0f, 1.2f);
        }
        savedWaterBreathing.clear();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("§8[§5QuietTone§8] ").append(QuietPauseMessages.text(player, "quietpause.pause.resumed", QuietPauseMessages.noPlaceholders())), false);
        }
        QuietPauseEvents.ON_UNFREEZE.invoker().onUnfreeze(server);
    }

    private void freezeWorlds(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            frozenWorldTimes.putIfAbsent(world.getRegistryKey(), world.getTimeOfDay());
        }
    }

    private void freezeWorldTimes(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            Long frozenTime = frozenWorldTimes.get(world.getRegistryKey());
            if (frozenTime != null) {
                world.setTimeOfDay(frozenTime);
            }
        }
    }

    private void restoreWorlds(MinecraftServer server) {
        frozenWorldTimes.clear();
    }

    private void freezePlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Vec3d frozenPos = frozenPlayerPositions.computeIfAbsent(player.getUuid(), id -> pos(player));
            if (player.squaredDistanceTo(frozenPos) > 0.0001D) {
                player.teleport(player.getEntityWorld(), frozenPos.x, frozenPos.y, frozenPos.z, Set.of(), player.getYaw(), player.getPitch(), true);
            }
            player.setVelocity(Vec3d.ZERO);
            player.fallDistance = 0.0f;
            if (player.currentScreenHandler != player.playerScreenHandler) {
                player.closeHandledScreen();
            }
        }
    }

    private void freezeEntities(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                freezeNewEntity(entity);
            }
        }
    }

    private void unfreezeEntities(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                UUID id = entity.getUuid();
                if (!frozenEntityVelocities.containsKey(id)) {
                    continue;
                }

                restoreTrackedEntity(entity);
            }
        }
        frozenEntityVelocities.clear();
    }

    public void freezeNewEntity(Entity entity) {
        if (!frozen || !isFreezable(entity) || frozenEntityVelocities.containsKey(entity.getUuid())) {
            return;
        }

        frozenEntityVelocities.put(entity.getUuid(), entity.getVelocity());
        entity.setVelocity(Vec3d.ZERO);
    }

    public void restoreTrackedEntity(Entity entity) {
        Vec3d velocity = frozenEntityVelocities.remove(entity.getUuid());
        if (velocity != null) {
            entity.setVelocity(velocity);
        }
    }

    public boolean shouldFreezeEntityTick(Entity entity) {
        return frozen && isFreezable(entity);
    }

    private boolean isFreezable(Entity entity) {
        return !(entity instanceof PlayerEntity);
    }

    private void broadcast(String key, Map<String, String> placeholders) {
        if (server == null) {
            return;
        }

        QuietPauseMessages.broadcast(server, key, placeholders);
    }

    private Vec3d pos(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    private void playSound(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                RegistryEntry.of(sound),
                SoundCategory.MASTER,
                player.getX(),
                player.getY(),
                player.getZ(),
                volume,
                pitch,
                ThreadLocalRandom.current().nextLong()
        ));
    }
}
