package com.tighug.difficulty.entity.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class onClientPlayerEntity extends AbstractClientPlayerEntity {

    public onClientPlayerEntity(ClientWorld p_i50991_1_, GameProfile p_i50991_2_) {
        super(p_i50991_1_, p_i50991_2_);
    }

    @Inject(at = @At("HEAD"), method = "hurtTo", cancellable = true)
    protected void onHurtTo(float p_71150_1_, CallbackInfo ci) {
        if (this.invulnerableTime >= 10) {
            this.setHealth(p_71150_1_);
            ci.cancel();
        }
    }
}
