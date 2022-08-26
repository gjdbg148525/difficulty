package com.tighug.difficulty.entity.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tighug.difficulty.entity.HealthEffect;
import com.tighug.difficulty.entity.IEntity;
import com.tighug.difficulty.entity.ILivingEntity;
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(PlayerEntity.class)
public abstract class onPlayerEntity extends LivingEntity implements IEntity {
    protected List<HealthEffect> healthEffects;
    protected byte multipleHurtInvulnerableTime;

    @Shadow
    public abstract void die(@NotNull DamageSource p_70645_1_);

    @Shadow
    protected abstract SoundEvent getDeathSound();

    @Shadow public abstract boolean isCreative();

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
        if (multipleHurtInvulnerableTime > 0) --multipleHurtInvulnerableTime;
        if (this.isDeadOrDying()) {
            healthEffects = null;
        }
        else if (healthEffects != null && !healthEffects.isEmpty()) {
            float f = 0;
            Iterator<HealthEffect> iterator = healthEffects.iterator();
            while (iterator.hasNext()) {
                HealthEffect effect = iterator.next();
                f += effect.getAmplifier();
                if (!effect.isEffective()) iterator.remove();
            }
            float health = this.getHealth() - f;
            super.setHealth(health);
            if (this.isDeadOrDying()) {
                healthEffects = null;
                DamageSource lastDamageSource = this.getLastDamageSource();
                if (lastDamageSource == null) {
                    super.setHealth(1);
                }
                else if (!((ILivingEntity) this).publicCheckTotemDeathProtection(lastDamageSource)) {
                    SoundEvent soundevent = this.getDeathSound();
                    if (soundevent != null) {
                        this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
                    }
                    this.die(lastDamageSource);
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "die", cancellable = true)
    protected void onDie(DamageSource p_70645_1_, CallbackInfo ci) {
        if (!this.isDeadOrDying() || this.level.isClientSide()) ci.cancel();
    }

    @Override
    public int multipleHurt(@NotNull Map<DamageSource, Float> damageSourceFloatMap) {
        if (multipleHurtInvulnerableTime > 0 || this.isSpectator() || damageSourceFloatMap.isEmpty()) return 0;
        else {
            if (this.isCreative()) {
                Set<DamageSource> set = Sets.newHashSet();
                damageSourceFloatMap.keySet().forEach(damageSource -> {
                    if (damageSource.isBypassInvul()) set.add(damageSource);
                });
                if (!set.isEmpty()) {
                    damageSourceFloatMap = Maps.asMap(set, damageSourceFloatMap::get);
                }
                else return 0;
            }
            multipleHurtInvulnerableTime = 5;
            return IEntity.super.multipleHurt(damageSourceFloatMap);
        }
    }
}
