package com.tighug.difficulty.mixin;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tighug.difficulty.Difficulty;
import com.tighug.difficulty.entity.EnderLightningBoltEntity;
import com.tighug.difficulty.entity.HealthEffect;
import com.tighug.mklapi.entity.DamageAbsorb;
import com.tighug.mklapi.entity.IEntity;
import com.tighug.mklapi.entity.ILivingEntity;
import com.tighug.mklapi.util.Utils;
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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.end.DragonFightManager;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mixin(EnderDragonEntity.class)
public abstract class onEnderDragonEntity extends MobEntity {
    private HealthEffect healthEffect;
    private byte damage;
    @Shadow
    @Final
    private EnderDragonPartEntity body;
    @Shadow
    @Final
    private PhaseManager phaseManager;

    private onEnderDragonEntity(World p_i48576_2_) {
        super(EntityType.ENDER_DRAGON, p_i48576_2_);
    }

    @Inject(at = @At("RETURN"), method = "createAttributes")
    private static void onCreateAttributes(@NotNull CallbackInfoReturnable<AttributeModifierMap.MutableAttribute> cir) {
        AttributeModifierMap.MutableAttribute returnValue = cir.getReturnValue();
        returnValue.add(Attributes.ARMOR, 20);
        returnValue.add(Attributes.ARMOR_TOUGHNESS, 20);
        returnValue.add(Attributes.ATTACK_DAMAGE, 20);
    }

    @Shadow
    @Nullable
    public abstract DragonFightManager getDragonFight();

    @Inject(at = @At("HEAD"), method = "reallyHurt", cancellable = true)
    private void onReallyHurt(@NotNull DamageSource p_82195_1_, float p_82195_2_, CallbackInfoReturnable<Boolean> cir) {
        if (!(p_82195_1_.getEntity() instanceof PlayerEntity) || p_82195_1_.getEntity() instanceof FakePlayer || (this.healthEffect != null && this.healthEffect.isEffective())) {
            cir.setReturnValue(false);
            lightningStrike(p_82195_1_, p_82195_2_);
        }
    }

    @Inject(at = @At("RETURN"), method = "hurt(Lnet/minecraft/entity/boss/dragon/EnderDragonPartEntity;Lnet/minecraft/util/DamageSource;F)Z")
    private void onHurtReturn(EnderDragonPartEntity p_213403_1_, @NotNull DamageSource p_213403_2_, float p_213403_3_, CallbackInfoReturnable<Boolean> cir) {
        if (this.level.isClientSide()) return;
        if (!cir.getReturnValue() && p_213403_2_.getEntity() != null) {
            lightningStrike(p_213403_2_, p_213403_3_);
        } else if (cir.getReturnValue() && this.phaseManager.getCurrentPhase().isSitting()) ++damage;
    }

    private void lightningStrike(@NotNull DamageSource p_213403_2_, float f) {
        Entity entity = p_213403_2_.getEntity();
        if (entity == null || this.level.isClientSide() || (!p_213403_2_.isProjectile() && p_213403_2_.getEntity() instanceof PlayerEntity && f < 5) || !(f >= 0))
            return;
        lightningStrike(entity, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) + f);
        this.heal(f);
    }

    private void lightningStrike(@NotNull Entity entity, float f) {
        if (!(f >= 0)) return;
        EnderLightningBoltEntity lightningBolt = new EnderLightningBoltEntity(EntityType.LIGHTNING_BOLT, this.level);
        lightningBolt.setDamage(f);
        lightningBolt.setPosRaw(entity.getX(), entity.getY(), entity.getZ());
        this.level.addFreshEntity(lightningBolt);
    }

    @Inject(at = @At("HEAD"), method = "hurt(Lnet/minecraft/entity/boss/dragon/EnderDragonPartEntity;Lnet/minecraft/util/DamageSource;F)Z", cancellable = true)
    private void onHurt(EnderDragonPartEntity p_213403_1_, @NotNull DamageSource p_213403_2_, float p_213403_3_, CallbackInfoReturnable<Boolean> cir) {
        if (!(p_213403_2_.getEntity() instanceof PlayerEntity) || p_213403_2_.getEntity() instanceof FakePlayer || (this.healthEffect != null && this.healthEffect.isEffective())) {
            cir.setReturnValue(false);
            lightningStrike(p_213403_2_, p_213403_3_);
        }
    }

    @Override
    protected void onEffectAdded(@NotNull EffectInstance p_70670_1_) {
    }

    @Override
    protected void onEffectUpdated(@NotNull EffectInstance p_70695_1_, boolean p_70695_2_) {
    }

    @Override
    protected void onEffectRemoved(@NotNull EffectInstance p_70688_1_) {
    }

    @Override
    public void heal(float p_70691_1_) {
        if (!(p_70691_1_ > 0) || this.isDeadOrDying() || this.phaseManager.getCurrentPhase().getPhase() == PhaseType.DYING)
            return;
        super.setHealth(this.getHealth() + p_70691_1_);
        if (!this.level.isClientSide()) this.healthEffect = new HealthEffect(0, (byte) 100);
    }

    @Override
    public void setHealth(float health) {
        if (Float.isNaN(health)) {
            throw new ArithmeticException("Health isNaN");
        }
        health = Utils.clamp(health, -Float.MAX_VALUE, Float.MAX_VALUE);
        float f = this.getHealth();
        if (this.level.isClientSide() || f - health < 0 || this.isDeadOrDying() || this.phaseManager == null || this.phaseManager.getCurrentPhase().getPhase() == PhaseType.DYING) {
            super.setHealth(health);
        } else {
            if (this.healthEffect == null || !this.healthEffect.isEffective()) {
                health = Math.min(f - health, this.getMaxHealth() * 0.02f);
                if (this.getDragonFight() != null && this.getDragonFight().hasPreviouslyKilledDragon()) health /= 2f;
                this.healthEffect = new HealthEffect(health / 20f, (byte) 20);
            }
        }
    }

    @Override
    public double getAttributeValue(@NotNull Attribute p_233637_1_) {
        double attributeValue = super.getAttributeValue(p_233637_1_);
        if (Double.isNaN(attributeValue)) attributeValue = p_233637_1_.getDefaultValue();
        if (p_233637_1_ == Attributes.ATTACK_DAMAGE) {
            if (attributeValue < 20) attributeValue = 20;
            attributeValue *= 5d - this.getHealth() / this.getMaxHealth() * 4d;
        } else if (p_233637_1_ == Attributes.MAX_HEALTH) {
            if (attributeValue < 200) attributeValue = 200;
        } else if (p_233637_1_ == Attributes.ARMOR || p_233637_1_ == Attributes.ARMOR_TOUGHNESS) {
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
        float f = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) / 2d);
        for (Entity entity : p_70970_1_) {
            if (entity instanceof LivingEntity) {
                double d2 = entity.getX() - d0;
                double d3 = entity.getZ() - d1;
                double d4 = Math.max(d2 * d2 + d3 * d3, 0.1D);
                entity.push(d2 / d4 * 4.0D, 0.2D, d3 / d4 * 4.0D);
                if (!this.phaseManager.getCurrentPhase().isSitting() && ((LivingEntity) entity).getLastHurtByMobTimestamp() < entity.tickCount - 2) {
                    DamageAbsorb[] damageAbsorbs = ((ILivingEntity) entity).getDefaultDamageAbsorb();
                    int length = damageAbsorbs.length;
                    damageAbsorbs = (DamageAbsorb[]) Utils.modifyArrayLength(damageAbsorbs, length + 1);
                    damageAbsorbs[length] = DamageAbsorb.of(Difficulty.Event::advancedProtection, entity);
                    ((IEntity) entity).multipleHurt(Maps.asMap(Sets.newHashSet(DamageSource.mobAttack(this), DamageSource.indirectMagic(this, this)), ds -> f), damageAbsorbs);
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
        for (Entity entity : p_70971_1_) {
            if (entity instanceof LivingEntity) {
                DamageAbsorb[] damageAbsorbs = ((ILivingEntity) entity).getDefaultDamageAbsorb();
                int length = damageAbsorbs.length;
                damageAbsorbs = (DamageAbsorb[]) Utils.modifyArrayLength(damageAbsorbs, length + 1);
                damageAbsorbs[length] = DamageAbsorb.of(Difficulty.Event::advancedProtection, entity);
                ((IEntity) entity).multipleHurt(Maps.asMap(Sets.newHashSet(DamageSource.mobAttack(this), DamageSource.indirectMagic(this, this)), ds -> f), damageAbsorbs);
                this.doEnchantDamageEffects(this, entity);
            }
        }

    }

    private void onTick() {
        if (this.level.isClientSide()) return;
        AttributeModifierManager modifierManager = this.getAttributes();
        Consumer<ModifiableAttributeInstance> modifiableAttributeInstanceConsumer = modifiableAttributeInstance -> modifiableAttributeInstance.getModifiers().stream().filter(a -> !(a.getAmount() >= 0)).collect(Collectors.toSet()).forEach(modifiableAttributeInstance::removeModifier);
        modifierManager.getDirtyAttributes().forEach(modifiableAttributeInstanceConsumer);
        modifierManager.getSyncableAttributes().forEach(modifiableAttributeInstanceConsumer);
        if (this.isDeadOrDying() || this.phaseManager.getCurrentPhase().getPhase() == PhaseType.DYING) {
            this.healthEffect = null;
        } else if (this.healthEffect != null && this.healthEffect.isEffective()) {
            float f = this.healthEffect.getAmplifier();
            float health = this.getHealth() - f;
            super.setHealth(health);
            if (this.isDeadOrDying()) {
                this.healthEffect = null;
                DamageSource lastDamageSource = this.getLastDamageSource();
                if (lastDamageSource != null && this.checkTotemDeathProtection(lastDamageSource)) {
                    Optional.ofNullable(this.getDeathSound()).ifPresent(s -> this.playSound(s, this.getSoundVolume(), this.getVoicePitch()));
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

    @Inject(at = @At("RETURN"), method = "readAdditionalSaveData")
    private void onReadAdditionalSaveData(CompoundNBT p_70037_1_, CallbackInfo ci) {
        super.readAdditionalSaveData(p_70037_1_);
        if (!this.level.isClientSide() && p_70037_1_.contains("Health", 99)) {
            super.setHealth(p_70037_1_.getFloat("Health"));
            this.healthEffect = HealthEffect.EMPTY;
        }
    }
}
