package com.quiettone.quietpause.mixin;

import com.quiettone.quietpause.PauseManager;
import com.quiettone.quietpause.QuietPause;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class HungerManagerMixin {
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void quietpause$cancelHunger(PlayerEntity player, CallbackInfo ci) {
        PauseManager manager = QuietPause.getPauseManager();
        if (manager != null && manager.isFrozen()) {
            ci.cancel();
        }
    }
}
