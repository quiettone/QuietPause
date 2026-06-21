package com.quiettone.quietpause.mixin;

import com.quiettone.quietpause.PauseManager;
import com.quiettone.quietpause.QuietPause;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowableFluid.class)
public class FluidTickMixin {
    @Inject(method = "onScheduledTick", at = @At("HEAD"), cancellable = true)
    private void quietpause$cancelFluidTick(ServerWorld world, BlockPos pos, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        PauseManager manager = QuietPause.getPauseManager();
        if (manager != null && manager.isFrozen()) {
            ci.cancel();
        }
    }
}
