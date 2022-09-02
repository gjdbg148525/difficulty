package com.tighug.difficulty.mixin;

import com.tighug.difficulty.enchantment.ModEnchantments;
import com.tighug.mklapi.entity.ILivingEntity;
import com.tighug.mklapi.util.Utils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.stats.Stats;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class onLivingEntity extends Entity implements ILivingEntity {

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
    public abstract boolean hurt(@NotNull DamageSource p_70097_1_, float p_70097_2_);

    @Shadow
    public abstract boolean hasEffect(Effect p_70644_1_);

    @Shadow
    @javax.annotation.Nullable
    public abstract EffectInstance getEffect(Effect p_70660_1_);

    /**
     * @author tighug
     * @reason getDamageAfterArmorAbsorb
     */
    @Overwrite
    protected float getDamageAfterArmorAbsorb(@NotNull DamageSource damageSource, float damage) {
        damage = Utils.clamp(damage, 0, Float.MAX_VALUE);
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
                    else {
                        damageSource.bypassMagic();
                        return damage;
                    }
                }
            }
            if (i == 15) {
                damageSource.bypassMagic();
                damage *= 2f;
            }
        } else if (!damageSource.isBypassArmor()) {
            float damageAfterAbsorb = CombatRules.getDamageAfterAbsorb(damage, (float) this.getArmorValue(), (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            this.hurtArmor(damageSource, (float) ((damage - damageAfterAbsorb) / this.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
            damage = damageAfterAbsorb;
        }
        return damage;
    }

    /**
     * @author tighug
     * @reason getDamageAfterMagicAbsorb
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    protected float getDamageAfterMagicAbsorb(@NotNull DamageSource damageSource, float damage) {
        if (damageSource.isBypassMagic()) {
            return damage;
        } else {
            if (this.hasEffect(Effects.DAMAGE_RESISTANCE) && damageSource != DamageSource.OUT_OF_WORLD) {
                EffectInstance effect = this.getEffect(Effects.DAMAGE_RESISTANCE);
                assert effect != null;
                int i = (effect.getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f = damage * (float) j;
                float f1 = damage;
                damage = Math.max(f / 25F, damage / 5F);
                float f2 = f1 - damage;
                if (f2 > 0.0F && f2 < 3.4028235E37F) {
                    if (ServerPlayerEntity.class.isInstance(this)) {
                        ServerPlayerEntity.class.cast(this).awardStat(Stats.DAMAGE_RESISTED, Math.round(f2 * 10.0F));
                    } else if (damageSource.getEntity() instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) damageSource.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(f2 * 10.0F));
                    }
                }
            }

            if (damage <= 0.0F) {
                return 0.0F;
            } else {
                int k = EnchantmentHelper.getDamageProtection(this.getArmorSlots(), damageSource);
                if (k > 0) {
                    damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float) k);
                }

                return damage;
            }
        }
    }
}
