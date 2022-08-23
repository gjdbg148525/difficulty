package com.tighug.difficulty.entity.mixin;

import com.google.common.collect.Lists;
import com.tighug.difficulty.entity.HealthEffect;
import com.tighug.difficulty.util.Utils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(PlayerEntity.class)
public abstract class onPlayerEntity extends LivingEntity {
    protected List<HealthEffect> healthEffects;

    @Shadow public abstract void die(@NotNull DamageSource p_70645_1_);

    @Shadow protected abstract SoundEvent getDeathSound();

    private onPlayerEntity(EntityType<? extends LivingEntity> p_i48577_1_, World p_i48577_2_) {
        super(p_i48577_1_, p_i48577_2_);
    }

    @Override
    public void setHealth(float health) {
        if (Float.isNaN(health)) {
            throw new ArithmeticException("Health isNaN");
        }
        float f = this.getHealth();
        if (this.level.isClientSide() || Math.abs(f - health) <= 1 || Float.isInfinite(health)) super.setHealth(health);
        else {
            if (healthEffects != null) {
                health = f - health;
                healthEffects.add(new HealthEffect(health / 10f));
            }
            else {
                healthEffects = Lists.newArrayList();
                super.setHealth(health);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "tick")
    protected void onTick(CallbackInfo ci) {
        if (this.level.isClientSide()) return;
        if (this.isDeadOrDying()) {
            healthEffects = null;
            return;
        }
        if (healthEffects != null && !healthEffects.isEmpty()) {
            AtomicReference<Float> f = new AtomicReference<>(0f);
            healthEffects.removeIf(healthEffect -> {
                f.set(f.get() + healthEffect.getAmplifier());
                return !healthEffect.isEffective();
            });
            float health = this.getHealth() - f.get();
            super.setHealth(health);
            if (this.isDeadOrDying()) {
                healthEffects = null;
                if (this.getLastDamageSource() != null && !Utils.checkTotemDeathProtection(this.getLastDamageSource(), this)) {
                    SoundEvent soundevent = this.getDeathSound();
                    if (soundevent != null) {
                        this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
                    }
                    this.die(this.getLastDamageSource());
                }
            }
        }
    }
}
