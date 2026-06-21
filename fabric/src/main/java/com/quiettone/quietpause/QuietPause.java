package com.quiettone.quietpause;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import net.minecraft.util.ActionResult;

public class QuietPause implements ModInitializer {
    public static final String MOD_ID = "quietpause";
    private static PauseManager pauseManager;

    public static PauseManager getPauseManager() {
        return pauseManager;
    }

    @Override
    public void onInitialize() {
        QuietPauseMessages.init();
        pauseManager = new PauseManager();

        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> pauseManager.restoreTrackedEntity(entity));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> pauseManager.unpauseQuiet());
        ServerTickEvents.END_SERVER_TICK.register(server -> pauseManager.tick(server));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> pauseManager.applyTo(handler.player));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> pauseManager.isFrozen() ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) -> pauseManager.isFrozen() ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> pauseManager.isFrozen() ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> pauseManager.isFrozen() ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> pauseManager.isFrozen() ? ActionResult.FAIL : ActionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !pauseManager.isFrozen());
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("quiet")
                .then(CommandManager.literal("f")
                        .executes(context -> {
                            pauseManager.toggle(context.getSource());
                            return 1;
                        })
                        .then(CommandManager.literal("public")
                                .executes(context -> {
                                    pauseManager.setAccessMode(context.getSource(), PauseManager.AccessMode.PUBLIC);
                                    return 1;
                                }))
                        .then(CommandManager.literal("admin")
                                .executes(context -> {
                                    pauseManager.setAccessMode(context.getSource(), PauseManager.AccessMode.ADMIN);
                                    return 1;
                                }))));

        dispatcher.register(CommandManager.literal("f")
                .executes(context -> {
                    pauseManager.toggle(context.getSource());
                    return 1;
                }));

        dispatcher.register(CommandManager.literal("p")
                .executes(context -> {
                    pauseManager.toggle(context.getSource());
                    return 1;
                }));


    }
}
