package com.tighug.difficulty.entity.mixin;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tighug.difficulty.entity.EnderLightningBoltEntity;
import com.tighug.difficulty.entity.HealthEffect;
import com.tighug.difficulty.entity.IEntity;
import com.tighug.difficulty.entity.ILivingEntity;
import com.tighug.difficulty.util.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseManager;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(EnderDragonEntity.class)
public abstract class onEnderDragonEntity extends MobEntity {
    protected AtomicReference<HealthEffect> healthEffect;

    @Shadow @Final private EnderDragonPartEntity body;

    @Shadow @Final private PhaseManager phaseManager;

    protected byte damage;

    private onEnderDragonEntity(World p_i48576_2_) {
        super(EntityType.ENDER_DRAGON, p_i48576_2_);
    }

    @Inject(at = @At("HEAD"), method = "reallyHurt", cancellable = true)
    protected void onReallyHurt(@NotNull DamageSource p_82195_1_, float p_82195_2_, CallbackInfoReturnable<Boolean> cir) {
        if (!(p_82195_1_.getEntity() instanceof PlayerEntity) || p_82195_1_.getEntity() instanceof FakePlayer || (healthEffect != null && healthEffect.get().isEffective())) {
            cir.setReturnValue(false);
            lightningStrike(p_82195_1_, p_82195_2_);
        }
    }

    @Inject(at = @At("RETURN"), method = "hurt(Lnet/minecraft/entity/boss/dragon/EnderDragonPartEntity;Lnet/minecraft/util/DamageSource;F)Z")
    protected void onHurtReturn(EnderDragonPartEntity p_213403_1_, @NotNull DamageSource p_213403_2_, float p_213403_3_, CallbackInfoReturnable<Boolean> cir) {
        if (this.level.isClientSide()) return;
        if (!cir.getReturnValue() && p_213403_2_.getEntity() != null) {
            lightningStrike(p_213403_2_, p_213403_3_);
        }
        else if (cir.getReturnValue() && this.phaseManager.getCurrentPhase().isSitting()) ++damage;
    }

    protected void lightningStrike(@NotNull DamageSource p_213403_2_, float f) {
        Entity entity = p_213403_2_.getEntity();
        if (entity == null || this.level.isClientSide() || (!p_213403_2_.isProjectile() && p_213403_2_.getEntity() instanceof PlayerEntity && f < 5) || !(f >= 0)) return;
        lightningStrike(entity, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) + f);
        this.heal(f);
    }

    protected void lightningStrike(@NotNull Entity entity, float f) {
        if (!(f >= 0)) return;
        EnderLightningBoltEntity lightningBolt = new EnderLightningBoltEntity(EntityType.LIGHTNING_BOLT, this.level);
        lightningBolt.setDamage(f);
        lightningBolt.setPosRaw(entity.getX(), entity.getY(), entity.getZ());
        this.level.addFreshEntity(lightningBolt);
    }

    @Inject(at = @At("HEAD"), method = "hurt(Lnet/minecraft/entity/boss/dragon/EnderDragonPartEntity;Lnet/minecraft/util/DamageSource;F)Z", cancellable = true)
    protected void onHurt(EnderDragonPartEntity p_213403_1_, @NotNull DamageSource p_213403_2_, float p_213403_3_, CallbackInfoReturnable<Boolean> cir) {
        if (!(p_213403_2_.getEntity() instanceof PlayerEntity) || p_213403_2_.getEntity() instanceof FakePlayer || (healthEffect != null && healthEffect.get().isEffective())) {
            cir.setReturnValue(false);
            lightningStrike(p_213403_2_, p_213403_3_);
        }
    }

    @Inject(at = @At("RETURN"), method = "createAttributes")
    private static void onCreateAttributes(@NotNull CallbackInfoReturnable<AttributeModifierMap.MutableAttribute> cir) {
        AttributeModifierMap.MutableAttribute returnValue = cir.getReturnValue();
        returnValue.add(Attributes.ARMOR, 20);
        returnValue.add(Attributes.ARMOR_TOUGHNESS, 20);
        returnValue.add(Attributes.ATTACK_DAMAGE, 20);
    }

    @Override
    public boolean addEffect(@NotNull EffectInstance p_195064_1_) {
        return false;
    }

    @Override
    protected void onEffectAdded(@NotNull EffectInstance p_70670_1_) {}

    @Override
    protected void onEffectUpdated(@NotNull EffectInstance p_70695_1_, boolean p_70695_2_) {}

    @Override
    protected void onEffectRemoved(@NotNull EffectInstance p_70688_1_) {}

    @Override
    public void heal(float p_70691_1_) {
        if (!(p_70691_1_ > 0) || this.isDeadOrDying() || this.phaseManager.getCurrentPhase().getPhase() == PhaseType.DYING) return;
        super.setHealth(this.getHealth() + p_70691_1_);
        if (!this.level.isClientSide() && this.healthEffect != null) healthEffect.set(new HealthEffect(0) {
            @Override
            public float getAmplifier() {
                ++count;
                return 0;
            }

            @Override
            public boolean isEffective() {
                return count <= 100;
            }
        });
    }

    @Override
    public void setHealth(float health) {
        if (Float.isNaN(health)) {
            throw new ArithmeticException("Health isNaN");
        }
        float f = this.getHealth();
        if (this.level.isClientSide() || f - health <= 0) {
            super.setHealth(health);
        }
        else if (healthEffect == null) {
            healthEffect = new AtomicReference<>(HealthEffect.EMPTY);
            super.setHealth(health);
        }
        else if (!healthEffect.get().isEffective()) {
            health = Math.min(f - health, this.getMaxHealth() * 0.02f);
            healthEffect.set(new HealthEffect(health / 20f) {

                @Override
                public float getAmplifier() {
                    if (++count <= 20) return amplifier;
                    return 0;
                }

                @Override
                public boolean isEffective() {
                    return count <= 20;
                }
            });
        }
    }

    @Override
    public double getAttributeValue(@NotNull Attribute p_233637_1_) {
        double attributeValue = super.getAttributeValue(p_233637_1_);
        if (Double.isNaN(attributeValue)) attributeValue = p_233637_1_.getDefaultValue();
        if (p_233637_1_ == Attributes.ATTACK_DAMAGE) {
            if (attributeValue < 20 ) attributeValue = 20;
            attributeValue *= 5d - this.getHealth() / this.getMaxHealth() * 4d ;
        }
        else if (p_233637_1_ == Attributes.MAX_HEALTH) {
            if (attributeValue < 200) attributeValue = 200;
        }
        else if (p_233637_1_ == Attributes.ARMOR || p_233637_1_ == Attributes.ARMOR_TOUGHNESS) {
            if (attributeValue < 20) attributeValue = 20;
            attributeValue *= this.getHealth() / this.getMaxHealth();
        }
        return attributeValue * 3d;
    }

    /**
     * @author tighug
     * @reason knockBack
     */
    @Overwrite
    private void knockBack(@NotNull List<Entity> p_70970_1_) {
        double d0 = (this.body.getBoundingBox().minX + this.body.getBoundingBox().maxX) / 2.0D;
        double d1 = (this.body.getBoundingBox().minZ + this.body.getBoundingBox().maxZ) / 2.0D;

        for(Entity entity : p_70970_1_) {
            if (entity instanceof LivingEntity) {
                double d2 = entity.getX() - d0;
                double d3 = entity.getZ() - d1;
                double d4 = Math.max(d2 * d2 + d3 * d3, 0.1D);
                entity.push(d2 / d4 * 4.0D, 0.2D, d3 / d4 * 4.0D);
                if (!this.phaseManager.getCurrentPhase().isSitting() && ((LivingEntity)entity).getLastHurtByMobTimestamp() < entity.tickCount - 2) {
                    float f = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) / 2d);
                    ((IEntity) entity).multipleHurt(Maps.asMap(Sets.newHashSet(DamageSource.mobAttack(this), DamageSource.indirectMagic(this, this)), ds -> f));
                    this.doEnchantDamageEffects(this, entity);
                }
            }
        }

    }

    /**
     * @author tighug
     * @reason hurt
     */
    @Overwrite
    private void hurt(@NotNull List<Entity> p_70971_1_) {
        float f = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        for(Entity entity : p_70971_1_) {
            if (entity instanceof LivingEntity) {
                ((IEntity) entity).multipleHurt(Maps.asMap(Sets.newHashSet(DamageSource.mobAttack(this), DamageSource.indirectMagic(this, this)), ds -> f));
                this.doEnchantDamageEffects(this, entity);
            }
        }

    }

    protected void onTick() {
        if (this.level.isClientSide()) return;
        AttributeModifierManager modifierManager = this.getAttributes();
        modifierManager.getDirtyAttributes().forEach(Utils.REMOVE_ADVERSE_MODIFIER);
        modifierManager.getSyncableAttributes().forEach(Utils.REMOVE_ADVERSE_MODIFIER);
        if (this.isDeadOrDying() || this.phaseManager.getCurrentPhase().getPhase() == PhaseType.DYING) {
            healthEffect = null;
        }
        else if (healthEffect != null && healthEffect.get().isEffective()) {
            float f = healthEffect.get().getAmplifier();
            float health = this.getHealth() - f;
            super.setHealth(health);
            if (this.isDeadOrDying()) {
                healthEffect = null;
                DamageSource lastDamageSource = this.getLastDamageSource();
                if (lastDamageSource != null && !((ILivingEntity) this).publicCheckTotemDeathProtection(lastDamageSource)) {
                    SoundEvent soundevent = this.getDeathSound();
                    if (soundevent != null) {
                        this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
                    }
                    this.die(lastDamageSource);
                    if (!this.phaseManager.getCurrentPhase().isSitting()) {
                        super.setHealth(1);
                        this.phaseManager.setPhase(PhaseType.DYING);
                    }
                }
            }
            if (this.phaseManager.getCurrentPhase().isSitting()) {
                if (this.damage > 5) {
                    this.damage = 0;
                    this.phaseManager.setPhase(PhaseType.TAKEOFF);
                }
            }
        }
    }

    @Override
    public void tick() {
        this.onTick();
        super.tick();
    }
}
