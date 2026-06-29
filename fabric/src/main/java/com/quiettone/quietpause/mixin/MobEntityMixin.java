package com.quiettone.quietpause.mixin;

import com.quiettone.quietpause.PauseManager;
import com.quiettone.quietpause.QuietPause;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void quietpause$freezeMobTick(CallbackInfo ci) {
        PauseManager manager = QuietPause.getPauseManager();
        if (manager != null && manager.isFrozen()) {
            ci.cancel();
        }
    }
}
