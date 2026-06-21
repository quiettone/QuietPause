package com.quiettone.quietpause.mixin;

import com.quiettone.quietpause.PauseManager;
import com.quiettone.quietpause.QuietPause;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.class)
public class FireBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void quietpause$cancelFireSpread(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!(((Object) this) instanceof AbstractFireBlock)) return;
        PauseManager manager = QuietPause.getPauseManager();
        if (manager != null && manager.isFrozen()) {
            ci.cancel();
        }
    }
}
