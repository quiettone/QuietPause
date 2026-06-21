package com.quiettone.quietpause.mixin;

import com.quiettone.quietpause.PauseManager;
import com.quiettone.quietpause.QuietPause;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void quietpause$cancelDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        PauseManager manager = QuietPause.getPauseManager();
        if (manager != null && manager.isFrozen()) {
            cir.setReturnValue(null);
        }
    }
}
