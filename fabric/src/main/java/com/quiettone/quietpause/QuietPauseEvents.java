package com.quiettone.quietpause;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

public final class QuietPauseEvents {
    public static final Event<Freeze> ON_FREEZE = EventFactory.createArrayBacked(Freeze.class, callbacks -> (server, freezerName) -> {
        for (Freeze callback : callbacks) {
            callback.onFreeze(server, freezerName);
        }
    });

    public static final Event<Unfreeze> ON_UNFREEZE = EventFactory.createArrayBacked(Unfreeze.class, callbacks -> server -> {
        for (Unfreeze callback : callbacks) {
            callback.onUnfreeze(server);
        }
    });

    private QuietPauseEvents() {
    }

    @FunctionalInterface
    public interface Freeze {
        void onFreeze(MinecraftServer server, String freezerName);
    }

    @FunctionalInterface
    public interface Unfreeze {
        void onUnfreeze(MinecraftServer server);
    }
}
