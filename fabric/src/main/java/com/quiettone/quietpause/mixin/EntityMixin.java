package com.quiettone.quietpause.mixin;

import com.quiettone.quietpause.PauseManager;
import com.quiettone.quietpause.QuietPause;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void quietpause$freezeEntityTick(CallbackInfo ci) {
        PauseManager manager = QuietPause.getPauseManager();
        if (manager == null || !manager.isFrozen()) {
            return;
        }

        Entity entity = (Entity) (Object) this;
        manager.freezeNewEntity(entity);
        if (manager.shouldFreezeEntityTick(entity)) {
            entity.setVelocity(Vec3d.ZERO);
            ci.cancel();
        }
    }
}
