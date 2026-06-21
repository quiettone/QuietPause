package com.quiettone.quietpause;

/**
 * Static API for other mods to interact with QuietPause.
 *
 * Listen to freeze/unfreeze via {@link QuietPauseEvents}:
 *   QuietPauseEvents.ON_FREEZE.register((server, freezerName) -> ...);
 *   QuietPauseEvents.ON_UNFREEZE.register(server -> ...);
 */
public final class QuietPauseAPI {
    private QuietPauseAPI() {}

    public static boolean isFrozen() {
        PauseManager manager = QuietPause.getPauseManager();
        return manager != null && manager.isFrozen();
    }

    public static boolean isInCountdown() {
        PauseManager manager = QuietPause.getPauseManager();
        return manager != null && manager.isInCountdown();
    }

    /**
     * Programmatically freeze the server (no-op if already frozen or in countdown).
     * Fires {@link QuietPauseEvents#ON_FREEZE} with freezerName = "Server".
     */
    public static void freeze() {
        PauseManager manager = QuietPause.getPauseManager();
        if (manager != null) manager.forceFreeze();
    }

    /**
     * Programmatically unfreeze the server (triggers the 5-second countdown).
     * No-op if not frozen or already counting down.
     * Fires {@link QuietPauseEvents#ON_UNFREEZE} when the countdown ends.
     */
    public static void unfreeze() {
        PauseManager manager = QuietPause.getPauseManager();
        if (manager != null) manager.forceUnfreeze();
    }
}
