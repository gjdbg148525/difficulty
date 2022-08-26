package com.tighug.difficulty.entity.mixin;

import com.google.common.collect.Multimap;
import com.tighug.difficulty.Difficulty;
import com.tighug.difficulty.enchantment.ModEnchantments;
import com.tighug.difficulty.entity.ILivingEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;

@Mixin(LivingEntity.class)
public abstract class onLivingEntity extends Entity implements ILivingEntity {

    @Shadow
    public float animationSpeed;
    @Shadow
    public int hurtDuration;
    @Shadow
    public int hurtTime;
    @Shadow
    public float hurtDir;
    @Shadow
    protected int noActionTime;
    @Shadow
    @javax.annotation.Nullable
    protected PlayerEntity lastHurtByPlayer;
    @Shadow
    protected int lastHurtByPlayerTime;
    @Shadow
    private long lastDamageStamp;
    @Shadow
    private DamageSource lastDamageSource;

    private onLivingEntity(World p_i48580_2_) {
        super(EntityType.AREA_EFFECT_CLOUD, p_i48580_2_);
    }

    @Shadow
    protected abstract void hurtArmor(DamageSource p_230294_1_, float p_230294_2_);

    @Shadow
    public abstract int getArmorValue();

    @Shadow
    public abstract double getAttributeValue(Attribute p_233637_1_);

    @Shadow
    public abstract float getArmorCoverPercentage();

    @Shadow
    public abstract boolean isDeadOrDying();

    @Shadow
    protected abstract boolean checkTotemDeathProtection(DamageSource p_190628_1_);

    @Shadow
    public abstract boolean hurt(@NotNull DamageSource p_70097_1_, float p_70097_2_);

    @Shadow
    public abstract boolean hasEffect(Effect p_70644_1_);

    @Shadow
    public abstract boolean isSleeping();

    @Shadow
    public abstract void stopSleeping();

    @Shadow
    protected abstract float getDamageAfterMagicAbsorb(DamageSource p_70672_1_, float p_70672_2_);

    @Shadow
    public abstract float getAbsorptionAmount();

    @Shadow
    public abstract void setAbsorptionAmount(float p_110149_1_);

    @Shadow
    public abstract CombatTracker getCombatTracker();

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract void setHealth(float p_70606_1_);

    @Shadow
    public abstract void setLastHurtByMob(@Nullable LivingEntity p_70604_1_);

    @Shadow
    @javax.annotation.Nullable
    protected abstract SoundEvent getDeathSound();

    @Shadow
    protected abstract float getSoundVolume();

    @Shadow
    protected abstract float getVoicePitch();

    @Shadow
    public abstract void die(DamageSource p_70645_1_);

    @Shadow
    protected abstract void playHurtSound(DamageSource p_184581_1_);

    /**
     * @author tighug
     * @reason getDamageAfterArmorAbsorb
     */
    @Overwrite
    protected float getDamageAfterArmorAbsorb(@NotNull DamageSource damageSource, float damage) {
        if (!damageSource.isExplosion() && (damageSource.getEntity() instanceof LivingEntity || damageSource.isProjectile())) {
            int enchantmentLevel = damageSource.getEntity() instanceof LivingEntity ? damageSource.getEntity() instanceof EnderDragonEntity ? 15 : EnchantmentHelper.getEnchantmentLevel(ModEnchantments.PRECISION.get(), (LivingEntity) damageSource.getEntity()) : 0;
            int i = enchantmentLevel >= 15 ? 15 : Math.min(15, this.random.nextInt(16) + enchantmentLevel);
            if (i == 0) return 0;
            else if (!damageSource.isBypassArmor()) {
                double d = this.getArmorValue() - damage / (1d + this.getAttributeValue(Attributes.ARMOR_TOUGHNESS) / 8d);
                d = Math.max(0, d) + this.getArmorCoverPercentage() * 3d;
                if (Math.min(14, i) <= d) {
                    this.hurtArmor(damageSource, damage / this.getArmorCoverPercentage());
                    if (i != 15) return 0;
                    else return damage;
                }
            }
            if (i == 15) damage *= 2f;
        } else if (!damageSource.isBypassArmor()) {
            float damageAfterAbsorb = CombatRules.getDamageAfterAbsorb(damage, (float) this.getArmorValue(), (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            this.hurtArmor(damageSource, (float) ((damage - damageAfterAbsorb) / this.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
            damage = damageAfterAbsorb;
        }
        return damage;
    }

    @Override
    public boolean publicCheckTotemDeathProtection(DamageSource damageSource) {
        if (!this.isDeadOrDying()) return true;
        return this.checkTotemDeathProtection(damageSource);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int multipleHurt(@NotNull Multimap<DamageSource, Float> damageSourceFloatMap) {
        if (this.level.isClientSide() || damageSourceFloatMap.isEmpty()) return 0;
        if (this.hasEffect(Effects.FIRE_RESISTANCE)) {
            Iterator<DamageSource> iterator = damageSourceFloatMap.keySet().iterator();
            iterator.forEachRemaining(ds -> {
                if (ds.isFire()) iterator.remove();
            });
            if (damageSourceFloatMap.isEmpty()) return 0;
        }
        int i = 0;
        boolean flag1 = true;
        DamageSource damageSource = null;
        for (DamageSource ds : damageSourceFloatMap.keySet()) {
            for (float de : damageSourceFloatMap.get(ds)) {
                double damage = de;
                if (this.isDeadOrDying() || !(damage > 0))
                    continue;
                damage = this.getDamageAfterMagicAbsorb(ds, this.getDamageAfterArmorAbsorb(ds, (float) damage)) / 2d;
                double f = Math.max(0, damage - this.getAbsorptionAmount());
                this.setAbsorptionAmount((float) (this.getAbsorptionAmount() - damage + f));
                float v = (float) (damage - f);
                boolean instance = ServerPlayerEntity.class.isInstance(this);
                if (v > 0.0F && v < 3.4028235E37F) {
                    if (ds.getEntity() instanceof ServerPlayerEntity)
                        ((ServerPlayerEntity) ds.getEntity()).awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(v * 10.0F));
                    if (instance)
                        ServerPlayerEntity.class.cast(this).awardStat(Stats.DAMAGE_ABSORBED, Math.round(v * 10.0F));
                }
                float f2 = Difficulty.Event.advancedProtection(this, (float) (f + damage));
                float f1 = this.getHealth();
                this.getCombatTracker().recordDamage(ds, f1, f2);
                this.setHealth(f1 - f2);
                if (instance) {
                    ServerPlayerEntity cast = ServerPlayerEntity.class.cast(this);
                    cast.causeFoodExhaustion(ds.getFoodExhaustion());
                    CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(cast, ds, de, f2, false);
                    if (f2 < 3.4028235E37F) {
                        cast.awardStat(Stats.DAMAGE_TAKEN, Math.round(f2 * 10.0F));
                    }
                    if (f1 > 0.0F && f1 < 3.4028235E37F) {
                        cast.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f1 * 10.0F));
                    }
                }
                Entity entity1 = ds.getEntity();
                if (entity1 != null) {
                    if (entity1 instanceof LivingEntity) {
                        this.setLastHurtByMob((LivingEntity) entity1);
                    }

                    if (entity1 instanceof ServerPlayerEntity) {
                        this.lastHurtByPlayerTime = 100;
                        this.lastHurtByPlayer = (PlayerEntity) entity1;
                        CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayerEntity) entity1, this, ds, de, f2, false);
                    } else if (entity1 instanceof TameableEntity) {
                        TameableEntity wolfentity = (TameableEntity) entity1;
                        if (wolfentity.isTame()) {
                            this.lastHurtByPlayerTime = 100;
                            LivingEntity livingentity = wolfentity.getOwner();
                            if (livingentity != null && livingentity.getType() == EntityType.PLAYER) {
                                this.lastHurtByPlayer = (PlayerEntity) livingentity;
                            } else {
                                this.lastHurtByPlayer = null;
                            }
                        }
                    }
                }
                if (ds instanceof EntityDamageSource && ((EntityDamageSource) ds).isThorns()) {
                    this.level.broadcastEntityEvent(this, (byte) 33);
                }
                else {
                    byte b0;
                    if (ds == DamageSource.DROWN) {
                        b0 = 36;
                    } else if (ds.isFire()) {
                        b0 = 37;
                    } else if (ds == DamageSource.SWEET_BERRY_BUSH) {
                        b0 = 44;
                    } else {
                        b0 = 2;
                    }

                    this.level.broadcastEntityEvent(this, b0);
                }
                if (this.isDeadOrDying()) {
                    if (!this.checkTotemDeathProtection(ds)) {
                        SoundEvent soundevent = this.getDeathSound();
                        if (flag1 && soundevent != null) {
                            flag1 = false;
                            this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
                        }
                        this.die(ds);
                    }
                }
                else if (flag1) {
                    flag1 = false;
                    this.playHurtSound(ds);
                }
                if (damageSource == null || ds.isBypassInvul()) damageSource = ds;
                ++i;
            }
        }
        if (i != 0) {
            if (this.isSleeping()) {
                this.stopSleeping();
            }
            if (this.lastDamageStamp != this.level.getGameTime()) {
                this.lastDamageStamp = this.level.getGameTime();
                this.lastDamageSource = damageSource;
            }
            this.noActionTime = 0;
            this.animationSpeed = 1.5F;
            this.hurtDir = 0.0F;
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
        }
        return i;
    }
}
