package me.otter.mintaddon.mixin;


import me.otter.mintaddon.MintAddon;
import dev.boze.api.BozeInstance;
import dev.boze.api.exception.AddonInitializationException;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(RunArgs args, CallbackInfo ci) {
        try {
            BozeInstance.INSTANCE.registerAddon(MintAddon.INSTANCE);
        } catch (AddonInitializationException e) {
            MintAddon.LOG.fatal("Failed to initialize " + MintAddon.INSTANCE.id, e);
        }

    }
}
