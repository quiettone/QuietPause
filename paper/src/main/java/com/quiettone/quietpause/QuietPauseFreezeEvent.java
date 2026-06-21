package com.quiettone.quietpause;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuietPauseFreezeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final String freezerName;

    public QuietPauseFreezeEvent(@Nullable String freezerName) {
        this.freezerName = freezerName;
    }

    /** null means the server triggered the freeze */
    @Nullable
    public String getFreezerName() {
        return freezerName;
    }

    public boolean isServerInitiated() {
        return freezerName == null;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
