package com.siuzu.paradox.mixin;

import com.siuzu.paradox.time.TimeFreeze;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    private int paradoxTickSkip = 0;

    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true)
    private void paradox$slowTicks(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        paradoxTickSkip++;
        if (paradoxTickSkip % 2 != 0) { // skip every other tick → half speed
            ci.cancel();
            return;
        }
    }
}
